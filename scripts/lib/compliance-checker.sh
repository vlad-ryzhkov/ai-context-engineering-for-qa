#!/usr/bin/env bash
# Compliance Checker — validates skill output against BANNED/REQUIRED patterns.
#
# Two-layer model:
#   Layer 1 (Definition Compliance): Runs skill-rules (Go) against SKILL.md → extracts rules
#   Layer 2 (Output Compliance): Runs extracted rules against skill output directory
#
# Usage:
#   bash scripts/lib/compliance-checker.sh --skill api-tests --output-dir src/test/kotlin/
#   bash scripts/lib/compliance-checker.sh --skill api-tests --rules-only  # Extract rules, don't check output
#   bash scripts/lib/compliance-checker.sh --skill golang-tester --output-dir tests/golden/golang-tester/
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILLS_DIR=".claude/skills"
ERRORS=0
WARNINGS=0
SKILL_FILTER=""
OUTPUT_DIR=""
RULES_ONLY=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
    --rules-only) RULES_ONLY=true; shift ;;
    -h|--help)
      echo "Usage: bash scripts/lib/compliance-checker.sh --skill <name> --output-dir <path>"
      echo ""
      echo "Options:"
      echo "  --skill <name>       Target skill name (required)"
      echo "  --output-dir <path>  Directory containing skill output to check"
      echo "  --rules-only         Only extract rules from SKILL.md (don't check output)"
      exit 0
      ;;
    *) shift ;;
  esac
done

if [[ -z "$SKILL_FILTER" ]]; then
  echo -e "${RED}ERROR: --skill is required${NC}"
  exit 1
fi

SKILL_DIR="${SKILLS_DIR}/${SKILL_FILTER}"
SKILL_FILE="${SKILL_DIR}/SKILL.md"

if [[ ! -f "$SKILL_FILE" ]]; then
  echo -e "${RED}ERROR: SKILL.md not found: ${SKILL_FILE}${NC}"
  exit 1
fi

# Resolve skill-rules binary: pre-built > go run > grep fallback
run_skill_rules() {
  local skill_file="$1"
  local bin="${SCRIPT_DIR}/../bin/skill-rules"
  local project_root
  project_root="$(cd "${SCRIPT_DIR}/../.." && pwd)"

  if [[ -x "$bin" ]]; then
    "$bin" "$skill_file"
  elif command -v go >/dev/null 2>&1; then
    (cd "${project_root}/tools" && go run ./cmd/skill-rules "$skill_file")
  else
    return 1
  fi
}

# Extract rules using Go skill-rules binary, else use grep fallback
extract_rules() {
  local skill_file="$1"

  if run_skill_rules "$skill_file" 2>/dev/null; then
    return 0
  else
    # Grep fallback: extract BANNED and REQUIRED patterns
    extract_rules_grep "$skill_file"
  fi
}

# Grep-based fallback for rule extraction
extract_rules_grep() {
  local skill_file="$1"
  local banned=()
  local required=()

  # Extract BANNED patterns: lines containing BANNED, prohibited, forbidden, NEVER use
  while IFS= read -r line; do
    # Extract pattern from backtick-quoted content
    local patterns
    patterns=$(echo "$line" | grep -oE '`[^`]+`' | tr -d '`' || true)
    while IFS= read -r p; do
      [[ -n "$p" ]] && banned+=("$p")
    done <<< "$patterns"
  done < <(grep -iE '(BANNED|prohibited|forbidden|NEVER use|NEVER import)' "$skill_file" 2>/dev/null || true)

  # Extract REQUIRED patterns: lines containing REQUIRED, must have, mandatory
  while IFS= read -r line; do
    local patterns
    patterns=$(echo "$line" | grep -oE '`[^`]+`' | tr -d '`' || true)
    while IFS= read -r p; do
      [[ -n "$p" ]] && required+=("$p")
    done <<< "$patterns"
  done < <(grep -iE '(REQUIRED|must have|mandatory|must contain|must include)' "$skill_file" 2>/dev/null || true)

  # Output as JSON
  local json='{"banned":['
  local first=true
  for b in "${banned[@]}"; do
    [[ -z "$b" ]] && continue
    if [[ "$first" == true ]]; then first=false; else json+=','; fi
    # Escape quotes in pattern
    b=$(echo "$b" | sed 's/"/\\"/g')
    json+='"'"$b"'"'
  done
  json+='],"required":['
  first=true
  for r in "${required[@]}"; do
    [[ -z "$r" ]] && continue
    if [[ "$first" == true ]]; then first=false; else json+=','; fi
    r=$(echo "$r" | sed 's/"/\\"/g')
    json+='"'"$r"'"'
  done
  json+=']}'
  echo "$json"
}

# Check BANNED patterns in output directory
check_banned() {
  local output_dir="$1" rules_json="$2"
  local banned_count=0
  local found_count=0

  local patterns
  patterns=$(echo "$rules_json" | jq -r '.banned[]' 2>/dev/null || true)

  if [[ -z "$patterns" ]]; then
    echo -e "  ${GREEN}✓${NC} [C1] No BANNED patterns defined"
    return 0
  fi

  while IFS= read -r pattern; do
    [[ -z "$pattern" ]] && continue
    ((banned_count++)) || true

    # Search for pattern in output files
    local matches
    matches=$(grep -rl --include="*.kt" --include="*.java" --include="*.md" --include="*.go" --include="*.sql" \
      -F "$pattern" "$output_dir" 2>/dev/null || true)

    if [[ -n "$matches" ]]; then
      ((found_count++)) || true
      while IFS= read -r match_file; do
        local line_num
        line_num=$(grep -n -F "$pattern" "$match_file" 2>/dev/null | head -1 | cut -d: -f1)
        echo -e "  ${RED}ERROR${NC}  [C1] BANNED pattern found: \`${pattern}\` in ${match_file}:${line_num}"
        ((ERRORS++)) || true
      done <<< "$matches"
    fi
  done <<< "$patterns"

  local passed=$((banned_count - found_count))
  echo -e "  [C1] BANNED: ${passed}/${banned_count} clean"
  return "$found_count"
}

# Check REQUIRED patterns in output directory
check_required() {
  local output_dir="$1" rules_json="$2"
  local required_count=0
  local missing_count=0

  local patterns
  patterns=$(echo "$rules_json" | jq -r '.required[]' 2>/dev/null || true)

  if [[ -z "$patterns" ]]; then
    echo -e "  ${GREEN}✓${NC} [C2] No REQUIRED patterns defined"
    return 0
  fi

  while IFS= read -r pattern; do
    [[ -z "$pattern" ]] && continue
    ((required_count++)) || true

    local matches
    matches=$(grep -rl --include="*.kt" --include="*.java" --include="*.md" --include="*.go" --include="*.sql" \
      -F "$pattern" "$output_dir" 2>/dev/null || true)

    if [[ -z "$matches" ]]; then
      ((missing_count++)) || true
      echo -e "  ${YELLOW}WARN${NC}   [C2] REQUIRED pattern not found: \`${pattern}\`"
      ((WARNINGS++)) || true
    fi
  done <<< "$patterns"

  local found=$((required_count - missing_count))
  echo -e "  [C2] REQUIRED: ${found}/${required_count} present"
  return "$missing_count"
}

# Check prompt cache ordering (C4) — $ARGUMENTS before Algorithm section
check_cache_ordering() {
  local skill_file="$1"

  # Find line numbers for Algorithm section and first dynamic reference
  local algo_line
  algo_line=$(grep -n -iE '^#+.*algorithm|^#+.*workflow|^#+.*execution' "$skill_file" 2>/dev/null | head -1 | cut -d: -f1 || echo 0)

  local dynamic_line
  dynamic_line=$(grep -n -E '\$ARGUMENTS|\{input\}|\$INPUT|<user' "$skill_file" 2>/dev/null | head -1 | cut -d: -f1 || echo 0)

  if [[ "$dynamic_line" -gt 0 ]] && [[ "$algo_line" -gt 0 ]] && [[ "$dynamic_line" -lt "$algo_line" ]]; then
    echo -e "  ${YELLOW}WARN${NC}   [C4] Dynamic reference (\$ARGUMENTS) at line ${dynamic_line} before Algorithm at line ${algo_line} — breaks prompt cache"
    ((WARNINGS++)) || true
  else
    echo -e "  ${GREEN}✓${NC} [C4] Prompt cache ordering valid"
  fi
}

# Main
echo -e "${CYAN}═══ Compliance Checker: ${SKILL_FILTER} ═══${NC}\n"

# Step 1: Extract rules
echo -e "${CYAN}Extracting rules from SKILL.md...${NC}"
rules_json=$(extract_rules "$SKILL_FILE")

if [[ "$RULES_ONLY" == true ]]; then
  echo "$rules_json" | jq . 2>/dev/null || echo "$rules_json"
  exit 0
fi

# Show extracted rules summary
banned_n=$(echo "$rules_json" | jq '.banned | length' 2>/dev/null || echo 0)
required_n=$(echo "$rules_json" | jq '.required | length' 2>/dev/null || echo 0)
echo -e "  Found: ${banned_n} BANNED, ${required_n} REQUIRED patterns\n"

# Step 2: Check definition compliance (always)
echo -e "${CYAN}Definition Compliance${NC}"
check_cache_ordering "$SKILL_FILE"

# Step 3: Check output compliance (if output dir provided)
if [[ -n "$OUTPUT_DIR" ]]; then
  if [[ ! -d "$OUTPUT_DIR" ]]; then
    echo -e "\n${RED}ERROR: Output directory not found: ${OUTPUT_DIR}${NC}"
    exit 1
  fi

  echo -e "\n${CYAN}Output Compliance (${OUTPUT_DIR})${NC}"
  check_banned "$OUTPUT_DIR" "$rules_json" || true
  check_required "$OUTPUT_DIR" "$rules_json" || true
fi

# Summary
echo ""
echo -e "Results: ${RED}${ERRORS} error(s)${NC}, ${YELLOW}${WARNINGS} warning(s)${NC}"

if [[ "$ERRORS" -gt 0 ]]; then
  exit 1
fi
exit 0
