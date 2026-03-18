#!/usr/bin/env bash
# Regression Detector — compares current state against baseline snapshot.
# Usage:
#   bash scripts/lib/regression-detect.sh
#   bash scripts/lib/regression-detect.sh --skill <name>
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

SKILLS_DIR=".claude/skills"
BASELINE_FILE=".claude/baselines/skill-snapshot.json"
ERRORS=0
WARNINGS=0
INFOS=0
SKILL_FILTER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    *) shift ;;
  esac
done

if [[ ! -f "$BASELINE_FILE" ]]; then
  echo -e "${RED}No baseline found at ${BASELINE_FILE}. Run: bash scripts/skill-quality.sh --snapshot${NC}"
  exit 1
fi

# Critical sections that must never disappear
CRITICAL_SECTIONS=("quality_gate" "gardener" "silent_mode" "completion")
SECTION_LABELS=("Quality Gate" "Gardener" "SILENT MODE" "SKILL COMPLETE")

echo -e "${CYAN}═══ Regression Detector ═══${NC}\n"

# Get list of skills from baseline
baseline_skills() {
  jq -r '.skills | keys[]' "$BASELINE_FILE" 2>/dev/null | sort
}

# Get baseline value for a skill field
baseline_val() {
  local skill="$1" field="$2"
  # Convert dot notation to jq path: "sections.quality_gate" → ".sections.quality_gate"
  local jq_path
  jq_path=$(echo "$field" | sed 's/\././g; s/^/./')
  jq -r --arg skill "$skill" ".skills[\$skill]${jq_path} // empty" "$BASELINE_FILE" 2>/dev/null || echo ""
}

check_skill_regression() {
  local name="$1"
  local skill_dir="${SKILLS_DIR}/${name}"
  local skill_file="${skill_dir}/SKILL.md"
  local findings=""

  # Skill exists on disk?
  if [[ ! -d "$skill_dir" ]] || [[ ! -f "$skill_file" ]]; then
    # R5: Skill removed from disk
    findings="${findings}\n  ${YELLOW}WARN${NC}   [R5] Skill '${name}' exists in baseline but removed from disk"
    ((WARNINGS++)) || true
    echo -e "${CYAN}${name}${NC}${findings}"
    return
  fi

  # R1: Required sections removed
  for i in "${!CRITICAL_SECTIONS[@]}"; do
    local section="${CRITICAL_SECTIONS[$i]}"
    local label="${SECTION_LABELS[$i]}"
    local was_present
    was_present=$(baseline_val "$name" "sections.${section}")

    if [[ "$was_present" == "True" ]]; then
      local still_present=false
      case "$section" in
        quality_gate) grep -qiE 'quality gate|self-review|post-check' "$skill_file" 2>/dev/null && still_present=true ;;
        gardener) grep -qi 'gardener' "$skill_file" 2>/dev/null && still_present=true ;;
        silent_mode) grep -qiE 'silent mode|verbosity|token economy|machine mode' "$skill_file" 2>/dev/null && still_present=true ;;
        completion) grep -qiE 'skill complete|completion' "$skill_file" 2>/dev/null && still_present=true ;;
      esac

      if [[ "$still_present" == false ]]; then
        findings="${findings}\n  ${RED}ERROR${NC}  [R1] Required section removed: ${label}"
        ((ERRORS++)) || true
      fi
    fi
  done

  # R3: Line count grew >20%
  local base_lines
  base_lines=$(baseline_val "$name" "lines")
  if [[ -n "$base_lines" ]] && [[ "$base_lines" -gt 0 ]]; then
    local current_lines
    current_lines=$(wc -l < "$skill_file" | tr -d ' ')
    local growth=$(( (current_lines - base_lines) * 100 / base_lines ))
    if [[ "$growth" -gt 20 ]]; then
      findings="${findings}\n  ${YELLOW}WARN${NC}   [R3] Line count grew ${growth}% (${base_lines} → ${current_lines})"
      ((WARNINGS++)) || true
    fi
  fi

  if [[ -n "$findings" ]]; then
    echo -e "${CYAN}${name}${NC}${findings}"
  else
    echo -e "${GREEN}✓${NC} ${name}"
  fi
}

# Check skills from baseline
BASELINE_SKILLS=$(baseline_skills)

# Check for new skills not in baseline (R4)
for skill_dir in "${SKILLS_DIR}"/*/; do
  [[ -d "$skill_dir" ]] || continue
  local_name=$(basename "$skill_dir")
  if [[ -n "$SKILL_FILTER" && "$local_name" != "$SKILL_FILTER" ]]; then continue; fi

  if ! echo "$BASELINE_SKILLS" | grep -qx "$local_name"; then
    echo -e "${CYAN}${local_name}${NC}\n  ${CYAN}INFO${NC}   [R4] New skill not in baseline"
    ((INFOS++)) || true
  fi
done

# Check existing baseline skills
while IFS= read -r skill_name; do
  [[ -z "$skill_name" ]] && continue
  if [[ -n "$SKILL_FILTER" && "$skill_name" != "$SKILL_FILTER" ]]; then continue; fi
  check_skill_regression "$skill_name"
done <<< "$BASELINE_SKILLS"

echo ""
echo -e "Results: ${RED}${ERRORS} error(s)${NC}, ${YELLOW}${WARNINGS} warning(s)${NC}, ${CYAN}${INFOS} info(s)${NC}"

if [[ "$ERRORS" -gt 0 ]]; then
  exit 1
fi
exit 0
