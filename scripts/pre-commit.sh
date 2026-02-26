#!/usr/bin/env bash
# pre-commit hook: blocks forbidden files and secret patterns in staged changes
set -euo pipefail

RED='\033[0;31m'
NC='\033[0m'

FORBIDDEN_FILES=(
  "gradle.properties"
  "local.properties"
  ".env"
  "credentials"
  ".pem"
  ".p12"
  ".key"
  ".keystore"
  ".jks"
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
STAGED_DIFF=$(git diff --cached -- . ':(exclude)scripts/')
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

exit 0
