#!/usr/bin/env bash
# pre-push hook: branch validation + secrets check + compile + markdownlint
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# 1. Branch name validation: main/master always allowed; feature branches — Latin + digits + /_.- , 7–45 chars
if [[ "$CURRENT_BRANCH" != "main" && "$CURRENT_BRANCH" != "master" ]]; then
  if ! echo "$CURRENT_BRANCH" | grep -qE '^[a-zA-Z0-9/_.\-]{7,45}$'; then
    echo -e "${RED}[pre-push] BLOCKED: branch name '$CURRENT_BRANCH' violates CI naming convention (Latin/digits//_.-,  7–45 chars).${NC}"
    exit 1
  fi
fi

FORBIDDEN_FILES=(
  "gradle.properties"
  ".env"
  "local.properties"
  "-credentials.toml"
)

# 2. Forbidden files in diff vs remote
REMOTE_BRANCH="origin/$CURRENT_BRANCH"
if git rev-parse --verify "$REMOTE_BRANCH" >/dev/null 2>&1; then
  DIFF_FILES=$(git diff --name-only "$REMOTE_BRANCH"..HEAD)
else
  DIFF_FILES=$(git diff --name-only HEAD)
fi

FOUND_FORBIDDEN=0
for file in $DIFF_FILES; do
  for pattern in "${FORBIDDEN_FILES[@]}"; do
    if echo "$file" | grep -qiE -- "$pattern"; then
      echo -e "${RED}[pre-push] BLOCKED: forbidden file in diff: $file${NC}"
      FOUND_FORBIDDEN=1
    fi
  done
done

if [ "$FOUND_FORBIDDEN" -eq 1 ]; then
  exit 1
fi

# 3. Secret patterns in diff (warn only — no push block to avoid false positives)
SECRET_PATTERNS=(
  "aws_access_key_id"
  "aws_secret_access_key"
  "ghp_[a-zA-Z0-9]+"
  "password\s*=\s*\S{4,}"
  "secret\s*=\s*\S{4,}"
  "api[_-]?key\s*=\s*\S{4,}"
  "AKIA[0-9A-Z]{16}"
)

if git rev-parse --verify "$REMOTE_BRANCH" >/dev/null 2>&1; then
  DIFF_CONTENT=$(git diff "$REMOTE_BRANCH"..HEAD)
else
  DIFF_CONTENT=$(git show HEAD)
fi

for pattern in "${SECRET_PATTERNS[@]}"; do
  if echo "$DIFF_CONTENT" | grep -qiE "^\+.*$pattern"; then
    echo -e "${YELLOW}[pre-push] WARNING: possible secret pattern detected: $pattern — review before pushing.${NC}"
  fi
done

# 4. Kotlin compile check
echo "[pre-push] Running Kotlin compilation check..."
if ! ./gradlew compileTestKotlin -q; then
  echo -e "${RED}[pre-push] BLOCKED: Kotlin compilation failed. Fix errors before pushing.${NC}"
  exit 1
fi

# 5. Markdownlint (check only — never modify files during push)
if command -v npx >/dev/null 2>&1; then
  echo "[pre-push] Running markdownlint..."
  if ! npx markdownlint-cli "**/*.md" --ignore node_modules --ignore audit 2>/dev/null; then
    echo -e "${YELLOW}[pre-push] WARNING: markdownlint found issues. Run 'npx markdownlint-cli --fix **/*.md' to fix.${NC}"
  fi
else
  echo -e "${YELLOW}[pre-push] WARNING: npx not found — markdownlint skipped.${NC}"
fi

# 6. Regression warning for .claude/ changes (non-blocking)
CLAUDE_CHANGES=$(echo "$DIFF_FILES" | grep -E '^\.(claude)/' || true)
if [ -n "$CLAUDE_CHANGES" ]; then
  if [ -f "scripts/lib/regression-detect.sh" ] && [ -f ".claude/baselines/skill-snapshot.json" ]; then
    if ! bash scripts/lib/regression-detect.sh >/dev/null 2>&1; then
      echo -e "${YELLOW}[pre-push] WARNING: Regression detected in .claude/ files. Run 'bash scripts/skill-quality.sh --check regression' for details.${NC}"
    fi
  fi
fi

echo "[pre-push] All checks passed."
exit 0
