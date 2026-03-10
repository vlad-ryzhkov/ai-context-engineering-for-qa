#!/usr/bin/env bash
# pre-commit hook: blocks forbidden files and secret patterns in staged changes
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

FORBIDDEN_FILES=(
  "gradle\.properties"
  "local\.properties"
  "\.env($|[^a-z])"
  "(^|/)credentials(\.[^/]+)?$"
  "\.pem$"
  "\.p12$"
  "\.key$"
  "\.keystore$"
  "\.jks$"
)

SECRET_PATTERNS=(
  "aws_access_key_id"
  "aws_secret_access_key"
  "ghp_[a-zA-Z0-9]+"
  "password\s*=\s*\S{4,}"
  "secret\s*=\s*\S{4,}"
  "api[_-]?key\s*=\s*\S{4,}"
  "AKIA[0-9A-Z]{16}"
)

STAGED_FILES=$(git diff --cached --name-only)

if [ -z "$STAGED_FILES" ]; then
  exit 0
fi

# Check forbidden files
FOUND_FORBIDDEN=0
for file in $STAGED_FILES; do
  for pattern in "${FORBIDDEN_FILES[@]}"; do
    if echo "$file" | grep -qiE "$pattern"; then
      echo -e "${RED}[pre-commit] BLOCKED: forbidden file staged: $file${NC}"
      FOUND_FORBIDDEN=1
    fi
  done
done

# Check secret patterns in staged diff (exclude scripts/ — they contain pattern definitions)
FOUND_SECRET=0
STAGED_DIFF=$(git diff --cached -- . ':(exclude)scripts/' ':(exclude).claude/' ':(exclude)docs/' ':(exclude).ai-lessons/' ':(exclude)package-lock.json')
for pattern in "${SECRET_PATTERNS[@]}"; do
  if echo "$STAGED_DIFF" | grep -qiE "^\+.*$pattern"; then
    echo -e "${RED}[pre-commit] BLOCKED: secret pattern detected: $pattern${NC}"
    FOUND_SECRET=1
  fi
done

if [ "$FOUND_FORBIDDEN" -eq 1 ] || [ "$FOUND_SECRET" -eq 1 ]; then
  echo -e "${RED}[pre-commit] Commit aborted. Remove sensitive data and try again.${NC}"
  exit 1
fi

# Check staged SKILL.md files: agnix + Tier 1 Baseline structure
STAGED_SKILLS=$(echo "$STAGED_FILES" | grep -E '\.claude/skills/.*/SKILL\.md$' || true)
if [ -n "$STAGED_SKILLS" ]; then
  # agnix quick check (if available)
  if command -v npx >/dev/null 2>&1 && [ -f "node_modules/.bin/agnix" ]; then
    if ! npx agnix --target claude-code .claude/ >/dev/null 2>&1; then
      echo -e "${YELLOW}[pre-commit] WARNING: agnix found issues in .claude/ — run 'npx agnix --target claude-code .' for details.${NC}"
    fi
  fi

  # Tier 1 Baseline structure check for each staged skill
  for skill_file in $STAGED_SKILLS; do
    skill_name=$(echo "$skill_file" | sed 's|.*skills/\([^/]*\)/.*|\1|')
    if [ -f "scripts/lib/skill-structure.sh" ]; then
      if ! bash scripts/lib/skill-structure.sh --skill "$skill_name" >/dev/null 2>&1; then
        echo -e "${YELLOW}[pre-commit] WARNING: Tier 1 Baseline issues in ${skill_name} — run 'bash scripts/skill-quality.sh --skill ${skill_name}' for details.${NC}"
      fi
    fi
  done
fi

exit 0
