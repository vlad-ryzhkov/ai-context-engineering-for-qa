#!/usr/bin/env bash
# Golden Test Runner — T1 structural assertion engine for skill output verification.
#
# Validates that skill output satisfies behavioral contracts defined by golden files.
# Two-tier strategy:
#   T1 (Structural): Heading presence, severity counts, pattern grep — FREE, deterministic
#   T3 (LLM Judge): Semantic equivalence — expensive, only for critical code-gen skills
#
# Directory structure per skill:
#   tests/golden/{skill_name}/
#     input/                   — Fixed input fixtures
#     expected-structure.txt   — Structural contract (T1 assertions)
#     actual-output.md         — Last saved output
#     run.sh                   — How to run the skill (optional)
#
# Usage:
#   bash scripts/golden-test.sh                              # All skills with golden tests
#   bash scripts/golden-test.sh --skill skill-audit          # Single skill
#   bash scripts/golden-test.sh --snapshot --skill api-tests # Save current output as golden
#   bash scripts/golden-test.sh --profile review             # Run only review-profile skills
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="${SCRIPT_DIR}/lib"

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

GOLDEN_DIR="tests/golden"
SKILL_FILTER=""
MODE="test"
PROFILE=""
ERRORS=0
WARNINGS=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    --snapshot) MODE="snapshot"; shift ;;
    --profile) PROFILE="$2"; shift 2 ;;
    -h|--help)
      echo "Usage: bash scripts/golden-test.sh [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --skill <name>     Run golden tests for a single skill"
      echo "  --snapshot         Save current actual-output.md as golden baseline"
      echo "  --profile <type>   Filter by profile: review, code-gen, sql, analysis"
      echo "  -h, --help         Show this help"
      echo ""
      echo "Profiles:"
      echo "  review    — golang-codereviewer, api-test-review"
      echo "  code-gen  — golang-tester, api-tests, api-tests-java"
      echo "  sql       — mysql-designer"
      echo "  analysis  — repo-scout, doc-lint, skill-audit, agents-checker, output-review"
      exit 0
      ;;
    *) shift ;;
  esac
done

# Profile → skill list mapping
skills_for_profile() {
  case "$1" in
    review) echo "golang-codereviewer api-test-review" ;;
    code-gen) echo "golang-tester api-tests api-tests-java" ;;
    sql) echo "mysql-designer" ;;
    analysis) echo "repo-scout doc-lint skill-audit agents-checker output-review" ;;
    *) echo "" ;;
  esac
}

# Determine profile for a skill
profile_for_skill() {
  local skill="$1"
  case "$skill" in
    golang-codereviewer|api-test-review) echo "review" ;;
    golang-tester|api-tests|api-tests-java) echo "code-gen" ;;
    mysql-designer) echo "sql" ;;
    repo-scout|doc-lint|skill-audit|agents-checker|output-review) echo "analysis" ;;
    *) echo "analysis" ;;
  esac
}

# Parse expected-structure.txt format:
# Each line: CHECK_TYPE | PATTERN | DESCRIPTION
# CHECK_TYPES: HEADING_PRESENT, HEADING_ABSENT, PATTERN_PRESENT, PATTERN_ABSENT,
#              COUNT_MIN, COUNT_MAX, SECTION_COUNT
parse_assertions() {
  local assertion_file="$1"
  if [[ ! -f "$assertion_file" ]]; then
    echo ""
    return
  fi
  # Return non-comment, non-empty lines
  grep -vE '^\s*(#|$)' "$assertion_file" 2>/dev/null || true
}

# G1: Check heading presence in actual output
check_heading_present() {
  local output_file="$1" pattern="$2" description="$3"
  if grep -qE "^#+.*${pattern}" "$output_file" 2>/dev/null; then
    return 0
  fi
  echo -e "  ${RED}FAIL${NC}   [G1] Heading not found: '${pattern}' — ${description}"
  ((ERRORS++)) || true
  return 1
}

# G1b: Check heading absence
check_heading_absent() {
  local output_file="$1" pattern="$2" description="$3"
  if grep -qE "^#+.*${pattern}" "$output_file" 2>/dev/null; then
    echo -e "  ${RED}FAIL${NC}   [G1] Unexpected heading found: '${pattern}' — ${description}"
    ((ERRORS++)) || true
    return 1
  fi
  return 0
}

# G2: Check pattern presence (non-heading)
check_pattern_present() {
  local output_file="$1" pattern="$2" description="$3"
  if grep -qF "$pattern" "$output_file" 2>/dev/null || grep -qE "$pattern" "$output_file" 2>/dev/null; then
    return 0
  fi
  echo -e "  ${RED}FAIL${NC}   [G2] Pattern not found: '${pattern}' — ${description}"
  ((ERRORS++)) || true
  return 1
}

# G2b: Check pattern absence (BANNED in output)
check_pattern_absent() {
  local output_file="$1" pattern="$2" description="$3"
  if grep -qF "$pattern" "$output_file" 2>/dev/null; then
    echo -e "  ${RED}FAIL${NC}   [G2] BANNED pattern found: '${pattern}' — ${description}"
    ((ERRORS++)) || true
    return 1
  fi
  return 0
}

# G3: Count occurrences minimum
check_count_min() {
  local output_file="$1" pattern="$2" description="$3"
  # description format: "min=N: actual description"
  local min_count
  min_count=$(echo "$description" | grep -oE 'min=[0-9]+' | head -1 | cut -d= -f2)
  if [[ -z "$min_count" ]]; then min_count=1; fi

  local actual_count
  actual_count=$(grep -cF "$pattern" "$output_file" 2>/dev/null || echo 0)

  if [[ "$actual_count" -lt "$min_count" ]]; then
    echo -e "  ${RED}FAIL${NC}   [G3] Pattern count ${actual_count} < min ${min_count}: '${pattern}'"
    ((ERRORS++)) || true
    return 1
  fi
  return 0
}

# G4: Completion block present
check_completion() {
  local output_file="$1" skill_name="$2"
  if grep -qiE '(SKILL COMPLETE|SKILL_COMPLETE|✅.*COMPLETE)' "$output_file" 2>/dev/null; then
    return 0
  fi
  echo -e "  ${YELLOW}WARN${NC}   [G4] Missing SKILL COMPLETE block"
  ((WARNINGS++)) || true
  return 1
}

# Run T1 structural assertions for a single skill
run_golden_test() {
  local skill_name="$1"
  local skill_golden_dir="${GOLDEN_DIR}/${skill_name}"
  local output_file="${skill_golden_dir}/actual-output.md"
  local structure_file="${skill_golden_dir}/expected-structure.txt"

  if [[ ! -d "$skill_golden_dir" ]]; then
    echo -e "${YELLOW}SKIP${NC}   ${skill_name} — no golden test directory"
    return 0
  fi

  if [[ ! -f "$output_file" ]]; then
    echo -e "${YELLOW}SKIP${NC}   ${skill_name} — no actual-output.md (run skill first, then --snapshot)"
    return 0
  fi

  echo -e "${CYAN}${skill_name}${NC} ($(profile_for_skill "$skill_name") profile)"

  local test_count=0
  local fail_count=0

  # Run assertions from expected-structure.txt
  if [[ -f "$structure_file" ]]; then
    while IFS='|' read -r check_type pattern description; do
      # Trim whitespace
      check_type=$(echo "$check_type" | xargs)
      pattern=$(echo "$pattern" | xargs)
      description=$(echo "$description" | xargs)

      [[ -z "$check_type" ]] && continue

      ((test_count++)) || true

      case "$check_type" in
        HEADING_PRESENT) check_heading_present "$output_file" "$pattern" "$description" || ((fail_count++)) || true ;;
        HEADING_ABSENT) check_heading_absent "$output_file" "$pattern" "$description" || ((fail_count++)) || true ;;
        PATTERN_PRESENT) check_pattern_present "$output_file" "$pattern" "$description" || ((fail_count++)) || true ;;
        PATTERN_ABSENT) check_pattern_absent "$output_file" "$pattern" "$description" || ((fail_count++)) || true ;;
        COUNT_MIN) check_count_min "$output_file" "$pattern" "$description" || ((fail_count++)) || true ;;
        *)
          echo -e "  ${YELLOW}WARN${NC}   Unknown check type: ${check_type}"
          ((WARNINGS++)) || true
          ;;
      esac
    done < <(parse_assertions "$structure_file")
  fi

  # Always check G4 (completion block)
  check_completion "$output_file" "$skill_name" || true
  ((test_count++)) || true

  local passed=$((test_count - fail_count))
  if [[ "$fail_count" -eq 0 ]]; then
    echo -e "  ${GREEN}✓${NC} All ${test_count} assertions passed"
  else
    echo -e "  ${RED}✗${NC} ${passed}/${test_count} passed, ${fail_count} failed"
  fi
  echo ""
}

# Snapshot mode: save current output
snapshot_skill() {
  local skill_name="$1"
  local skill_golden_dir="${GOLDEN_DIR}/${skill_name}"

  if [[ ! -d "$skill_golden_dir" ]]; then
    mkdir -p "$skill_golden_dir/input"
    echo -e "${GREEN}Created${NC} golden test directory: ${skill_golden_dir}/"
  fi

  # Look for output in common locations
  local found=false
  for candidate in "audit/${skill_name}-report.md" "audit/${skill_name}.md" "audit/report.md"; do
    if [[ -f "$candidate" ]]; then
      cp "$candidate" "${skill_golden_dir}/actual-output.md"
      echo -e "${GREEN}✓${NC} Saved ${candidate} → ${skill_golden_dir}/actual-output.md"
      found=true
      break
    fi
  done

  if [[ "$found" == false ]]; then
    echo -e "${YELLOW}WARN${NC}   No output found for ${skill_name}. Place output in ${skill_golden_dir}/actual-output.md manually."
  fi

  # Create expected-structure.txt template if missing
  if [[ ! -f "${skill_golden_dir}/expected-structure.txt" ]]; then
    local profile
    profile=$(profile_for_skill "$skill_name")

    cat > "${skill_golden_dir}/expected-structure.txt" << 'TEMPLATE'
# Golden Test Assertions for: SKILL_NAME
# Format: CHECK_TYPE | PATTERN | DESCRIPTION
#
# CHECK_TYPES:
#   HEADING_PRESENT  — Heading matching pattern must exist
#   HEADING_ABSENT   — Heading matching pattern must NOT exist
#   PATTERN_PRESENT  — Pattern must appear somewhere in output
#   PATTERN_ABSENT   — Pattern must NOT appear in output (BANNED)
#   COUNT_MIN        — Pattern must appear at least N times (description: "min=N: ...")
#
# Add your assertions below:
TEMPLATE
    sed -i '' "s/SKILL_NAME/${skill_name}/" "${skill_golden_dir}/expected-structure.txt" 2>/dev/null || true
    echo -e "${GREEN}Created${NC} ${skill_golden_dir}/expected-structure.txt (template — edit with assertions)"
  fi
}

# Main
echo -e "${CYAN}═══ Golden Test Runner (T1 Structural) ═══${NC}\n"

if [[ "$MODE" == "snapshot" ]]; then
  if [[ -n "$SKILL_FILTER" ]]; then
    snapshot_skill "$SKILL_FILTER"
  else
    echo -e "${RED}ERROR: --snapshot requires --skill <name>${NC}"
    exit 1
  fi
  exit 0
fi

# Determine skills to test
skills_to_test=()

if [[ -n "$SKILL_FILTER" ]]; then
  skills_to_test+=("$SKILL_FILTER")
elif [[ -n "$PROFILE" ]]; then
  for s in $(skills_for_profile "$PROFILE"); do
    skills_to_test+=("$s")
  done
else
  # All skills with golden directories
  if [[ -d "$GOLDEN_DIR" ]]; then
    for d in "${GOLDEN_DIR}"/*/; do
      [[ -d "$d" ]] || continue
      skills_to_test+=("$(basename "$d")")
    done
  fi
fi

if [[ ${#skills_to_test[@]} -eq 0 ]]; then
  echo -e "${YELLOW}No golden tests found.${NC}"
  echo -e "Create golden test fixtures in: ${GOLDEN_DIR}/{skill_name}/"
  echo -e "Or run: bash scripts/golden-test.sh --snapshot --skill <name>"
  exit 0
fi

for skill in "${skills_to_test[@]}"; do
  run_golden_test "$skill"
done

echo -e "Results: ${RED}${ERRORS} error(s)${NC}, ${YELLOW}${WARNINGS} warning(s)${NC}"

if [[ "$ERRORS" -gt 0 ]]; then
  exit 1
fi
exit 0
