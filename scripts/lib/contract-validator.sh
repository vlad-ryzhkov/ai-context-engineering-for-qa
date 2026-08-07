#!/usr/bin/env bash
# Contract Validator — validates SKILL COMPLETE blocks and cross-skill contracts.
#
# Part A: Completion Contract (K1-K4) — checks skill output for valid SKILL COMPLETE block.
# Part B: Cross-Skill Contracts (X1-X2) — checks pipeline artifact existence and format.
#
# Enforcement levels:
#   QA skills (api-tests, api-test-review, repo-scout, etc.) → FAIL on missing
#   Go skills (golang-tester, golang-codereviewer, mysql-designer) → WARN only
#
# Usage:
#   bash scripts/lib/contract-validator.sh                              # All skills
#   bash scripts/lib/contract-validator.sh --skill api-tests            # Single skill
#   bash scripts/lib/contract-validator.sh --check cross-skill          # Cross-skill only
#   bash scripts/lib/contract-validator.sh --output audit/report.md     # Check output file
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

SKILLS_DIR=".claude/skills"
CONTRACTS_FILE="cross-skill-contracts.yaml"
ERRORS=0
WARNINGS=0
SKILL_FILTER=""
CHECK_MODE="all"  # all | completion | cross-skill
OUTPUT_FILE=""

# Go/infrastructure skills → WARN only (not yet adopted SKILL COMPLETE)
GO_SKILLS=("golang-tester" "golang-codereviewer" "mysql-designer")

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    --check) CHECK_MODE="$2"; shift 2 ;;
    --output) OUTPUT_FILE="$2"; shift 2 ;;
    -h|--help)
      echo "Usage: bash scripts/lib/contract-validator.sh [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --skill <name>       Scope to a single skill"
      echo "  --check completion   Check SKILL COMPLETE blocks only"
      echo "  --check cross-skill  Check cross-skill contracts only"
      echo "  --output <file>      Check specific output file"
      exit 0
      ;;
    *) shift ;;
  esac
done

# Determine enforcement level: FAIL for QA skills, WARN for Go skills
is_go_skill() {
  local name="$1"
  for go_skill in "${GO_SKILLS[@]}"; do
    [[ "$name" == "$go_skill" ]] && return 0
  done
  return 1
}

severity_for_skill() {
  local name="$1"
  if is_go_skill "$name"; then
    echo "WARN"
  else
    echo "FAIL"
  fi
}

# K1: SKILL COMPLETE block exists
check_k1() {
  local output_file="$1" skill_name="$2"
  local sev
  sev=$(severity_for_skill "$skill_name")

  if ! grep -qiE '(SKILL COMPLETE|SKILL_COMPLETE|✅.*COMPLETE)' "$output_file" 2>/dev/null; then
    if [[ "$sev" == "FAIL" ]]; then
      echo -e "  ${RED}ERROR${NC}  [K1] Missing SKILL COMPLETE block"
      ((ERRORS++)) || true
    else
      echo -e "  ${YELLOW}WARN${NC}   [K1] Missing SKILL COMPLETE block (recommended for $skill_name)"
      ((WARNINGS++)) || true
    fi
    return 1
  fi
  return 0
}

# K2: Required fields in SKILL COMPLETE block
check_k2() {
  local output_file="$1" skill_name="$2"
  local sev
  sev=$(severity_for_skill "$skill_name")

  # Extract SKILL COMPLETE block (from marker to end or next heading)
  local block
  block=$(sed -n '/SKILL COMPLETE/,/^#/p' "$output_file" 2>/dev/null | head -30 || true)

  if [[ -z "$block" ]]; then
    # Try alternative: extract from SKILL COMPLETE to end of file
    block=$(sed -n '/SKILL COMPLETE/,$p' "$output_file" 2>/dev/null | head -30 || true)
  fi

  local missing_fields=()

  # Check for status indicator
  if ! echo "$block" | grep -qiE '(status|✅|❌|⚠️|DONE|PARTIAL|FAILED)'; then
    missing_fields+=("status")
  fi

  # Check for skill name or skill reference
  if ! echo "$block" | grep -qiE "(skill|name|${skill_name})"; then
    missing_fields+=("skill_name")
  fi

  if [[ ${#missing_fields[@]} -gt 0 ]]; then
    local fields_str
    fields_str=$(IFS=', '; echo "${missing_fields[*]}")
    if [[ "$sev" == "FAIL" ]]; then
      echo -e "  ${RED}ERROR${NC}  [K2] Missing required fields in SKILL COMPLETE: ${fields_str}"
      ((ERRORS++)) || true
    else
      echo -e "  ${YELLOW}WARN${NC}   [K2] Missing required fields in SKILL COMPLETE: ${fields_str}"
      ((WARNINGS++)) || true
    fi
    return 1
  fi
  return 0
}

# K3: Artifacts referenced in SKILL COMPLETE exist on disk
check_k3() {
  local output_file="$1" skill_name="$2"
  local sev
  sev=$(severity_for_skill "$skill_name")

  # Extract file paths from SKILL COMPLETE block (├─, └─, or plain paths)
  local block
  block=$(sed -n '/SKILL COMPLETE/,$p' "$output_file" 2>/dev/null | head -40 || true)

  local artifacts
  artifacts=$(echo "$block" | grep -oE '[a-zA-Z0-9_./-]+\.(md|kt|java|yaml|yml|json|sql|sh|py)' 2>/dev/null | sort -u || true)

  if [[ -z "$artifacts" ]]; then
    return 0  # No artifacts referenced — pass vacuously
  fi

  local missing=0
  while IFS= read -r artifact; do
    [[ -z "$artifact" ]] && continue
    # Check relative to project root and common output dirs
    if [[ ! -f "$artifact" ]] && [[ ! -f "audit/$artifact" ]] && [[ ! -f "src/test/$artifact" ]]; then
      if [[ "$sev" == "FAIL" ]]; then
        echo -e "  ${RED}ERROR${NC}  [K3] Referenced artifact not found: ${artifact}"
        ((ERRORS++)) || true
      else
        echo -e "  ${YELLOW}WARN${NC}   [K3] Referenced artifact not found: ${artifact}"
        ((WARNINGS++)) || true
      fi
      ((missing++)) || true
    fi
  done <<< "$artifacts"

  return "$missing"
}

# K4: No tail noise after SKILL COMPLETE block
check_k4() {
  local output_file="$1" skill_name="$2"

  # Count lines after SKILL COMPLETE block
  local total_lines complete_line tail_lines
  total_lines=$(wc -l < "$output_file" | tr -d ' ')
  complete_line=$(grep -n -iE '(SKILL COMPLETE|SKILL_COMPLETE|✅.*COMPLETE)' "$output_file" 2>/dev/null | tail -1 | cut -d: -f1 || echo 0)

  if [[ "$complete_line" -eq 0 ]]; then
    return 0  # No SKILL COMPLETE — K1 already flagged
  fi

  tail_lines=$((total_lines - complete_line))
  # Allow up to 15 lines after SKILL COMPLETE (tree structure + blank lines)
  if [[ "$tail_lines" -gt 15 ]]; then
    echo -e "  ${YELLOW}WARN${NC}   [K4] ${tail_lines} lines after SKILL COMPLETE (expected ≤15) — possible tail noise"
    ((WARNINGS++)) || true
    return 1
  fi
  return 0
}

# Part B: Cross-Skill Contract Validation
validate_cross_skill_contracts() {
  if [[ ! -f "$CONTRACTS_FILE" ]]; then
    echo -e "${GREEN}✓${NC} No cross-skill contracts file — vacuous pass"
    return 0
  fi

  # Check if file has any contracts (non-comment, non-empty lines)
  local contract_count
  contract_count=$(grep -cvE '^\s*(#|$)' "$CONTRACTS_FILE" 2>/dev/null || echo 0)

  if [[ "$contract_count" -eq 0 ]]; then
    echo -e "${GREEN}✓${NC} Cross-skill contracts: 0 contracts defined — vacuous pass"
    return 0
  fi

  echo -e "\n${CYAN}Cross-Skill Contracts${NC}"

  local contract_errors=0
  while IFS='|' read -r upstream artifact downstream format_check; do
    # Skip comments and empty lines
    [[ "$upstream" =~ ^[[:space:]]*# ]] && continue
    [[ -z "$upstream" ]] && continue

    # Trim whitespace
    upstream=$(echo "$upstream" | xargs)
    artifact=$(echo "$artifact" | xargs)
    downstream=$(echo "$downstream" | xargs)
    format_check=$(echo "$format_check" | xargs)

    # X1: Artifact exists
    if [[ -n "$artifact" ]] && [[ ! -f "$artifact" ]] && [[ ! -d "$artifact" ]]; then
      echo -e "  ${YELLOW}WARN${NC}   [X1] Artifact not found: ${artifact} (${upstream} → ${downstream})"
      ((WARNINGS++)) || true
      ((contract_errors++)) || true
      continue
    fi

    # X2: Format validation (if format_check command provided)
    if [[ -n "$format_check" ]] && [[ -f "$artifact" ]]; then
      if ! eval "$format_check" "$artifact" >/dev/null 2>&1; then
        echo -e "  ${YELLOW}WARN${NC}   [X2] Format check failed: ${artifact} (${format_check})"
        ((WARNINGS++)) || true
        ((contract_errors++)) || true
        continue
      fi
    fi

    echo -e "  ${GREEN}✓${NC} ${upstream} → ${downstream} (via ${artifact})"
  done < "$CONTRACTS_FILE"

  return "$contract_errors"
}

# Validate completion contract for a single output file
validate_output() {
  local output_file="$1" skill_name="$2"

  if [[ ! -f "$output_file" ]]; then
    echo -e "${YELLOW}WARN${NC}   Output file not found: ${output_file}"
    ((WARNINGS++)) || true
    return
  fi

  local findings=""
  local k1_pass=true

  # Capture K1-K4 output
  local k1_out k2_out k3_out k4_out
  k1_out=$(check_k1 "$output_file" "$skill_name" 2>&1) || k1_pass=false
  echo -n "$k1_out"
  [[ -n "$k1_out" ]] && echo ""

  if [[ "$k1_pass" == true ]]; then
    k2_out=$(check_k2 "$output_file" "$skill_name" 2>&1) || true
    echo -n "$k2_out"
    [[ -n "$k2_out" ]] && echo ""

    k3_out=$(check_k3 "$output_file" "$skill_name" 2>&1) || true
    echo -n "$k3_out"
    [[ -n "$k3_out" ]] && echo ""

    k4_out=$(check_k4 "$output_file" "$skill_name" 2>&1) || true
    echo -n "$k4_out"
    [[ -n "$k4_out" ]] && echo ""
  fi
}

# Find output files for a skill
find_skill_outputs() {
  local skill_name="$1"
  local outputs=()

  # Check audit/ directory for reports
  while IFS= read -r f; do
    outputs+=("$f")
  done < <(find audit/ -name "*${skill_name}*" -type f 2>/dev/null || true)

  # Check tests/golden/{skill}/actual-output.md
  local golden_out="tests/golden/${skill_name}/actual-output.md"
  if [[ -f "$golden_out" ]]; then
    outputs+=("$golden_out")
  fi

  if [[ ${#outputs[@]} -gt 0 ]]; then
    printf '%s\n' "${outputs[@]}"
  fi
}

# Main
echo -e "${CYAN}═══ Contract Validator ═══${NC}\n"

if [[ "$CHECK_MODE" == "cross-skill" ]]; then
  validate_cross_skill_contracts
  echo ""
  echo -e "Results: ${RED}${ERRORS} error(s)${NC}, ${YELLOW}${WARNINGS} warning(s)${NC}"
  [[ "$ERRORS" -gt 0 ]] && exit 1
  exit 0
fi

# Check completion contracts
if [[ -n "$OUTPUT_FILE" ]]; then
  # Check specific output file
  local_skill="${SKILL_FILTER:-unknown}"
  echo -e "${CYAN}${local_skill}${NC} (${OUTPUT_FILE})"
  validate_output "$OUTPUT_FILE" "$local_skill"
elif [[ -n "$SKILL_FILTER" ]]; then
  # Check single skill
  echo -e "${CYAN}${SKILL_FILTER}${NC}"
  outputs=$(find_skill_outputs "$SKILL_FILTER")
  if [[ -z "$outputs" ]]; then
    sev=$(severity_for_skill "$SKILL_FILTER")
    if [[ "$sev" == "FAIL" ]]; then
      echo -e "  ${YELLOW}WARN${NC}   No output files found for ${SKILL_FILTER} — skipping completion checks"
      ((WARNINGS++)) || true
    else
      echo -e "  ${GREEN}✓${NC} No output files found (Go skill) — completion checks deferred"
    fi
  else
    while IFS= read -r output; do
      [[ -z "$output" ]] && continue
      echo -e "  Checking: ${output}"
      validate_output "$output" "$SKILL_FILTER"
    done <<< "$outputs"
  fi
else
  # Check all skills with available outputs
  checked=0
  for skill_dir in "${SKILLS_DIR}"/*/; do
    [[ -d "$skill_dir" ]] || continue
    local_name=$(basename "$skill_dir")
    [[ "$local_name" == "_shared" ]] && continue

    outputs=$(find_skill_outputs "$local_name")
    if [[ -n "$outputs" ]]; then
      echo -e "${CYAN}${local_name}${NC}"
      while IFS= read -r output; do
        [[ -z "$output" ]] && continue
        echo -e "  Checking: ${output}"
        validate_output "$output" "$local_name"
      done <<< "$outputs"
      ((checked++)) || true
    fi
  done

  if [[ "$checked" -eq 0 ]]; then
    echo -e "${GREEN}✓${NC} No skill outputs found — completion checks vacuous pass"
  fi
fi

# Cross-skill contracts (if mode is "all")
if [[ "$CHECK_MODE" == "all" ]]; then
  echo ""
  validate_cross_skill_contracts
fi

echo ""
echo -e "Results: ${RED}${ERRORS} error(s)${NC}, ${YELLOW}${WARNINGS} warning(s)${NC}"

if [[ "$ERRORS" -gt 0 ]]; then
  exit 1
fi
exit 0
