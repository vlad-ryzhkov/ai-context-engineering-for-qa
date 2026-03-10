#!/usr/bin/env bash
# Static Context Budget Tracker — measures file sizes loaded into context window.
# Tracks ONLY static context cost (file sizes / 4 ≈ tokens). NOT runtime token usage.
# Usage:
#   bash scripts/lib/token-budget.sh                  # Report
#   bash scripts/lib/token-budget.sh --snapshot       # Save baseline
#   bash scripts/lib/token-budget.sh --diff           # Compare vs baseline
#   bash scripts/lib/token-budget.sh --skill <name>   # Single skill
#   bash scripts/lib/token-budget.sh --json           # JSON output
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

SKILLS_DIR=".claude/skills"
BASELINE_FILE=".claude/baselines/skill-snapshot.json"
MODE="report"
SKILL_FILTER=""
JSON_OUTPUT=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --snapshot) MODE="snapshot"; shift ;;
    --diff) MODE="diff"; shift ;;
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    --json) JSON_OUTPUT=true; shift ;;
    *) shift ;;
  esac
done

# Approximate tokens = chars / 4
file_tokens() {
  local file="$1"
  if [[ -f "$file" ]]; then
    local chars
    chars=$(wc -c < "$file" | tr -d ' ')
    echo $(( chars / 4 ))
  else
    echo 0
  fi
}

dir_tokens() {
  local dir="$1"
  local total=0
  if [[ -d "$dir" ]]; then
    while IFS= read -r f; do
      local t
      t=$(file_tokens "$f")
      total=$((total + t))
    done < <(find "$dir" -type f -name "*.md" -o -name "*.sh" 2>/dev/null)
  fi
  echo "$total"
}

# Core context (loaded on every invocation)
core_context_tokens() {
  local total=0
  for f in CLAUDE.md .claude/qa_agent.md .claude/protocols/gardener.md .claude/agents/auditor.md .claude/agents/sdet.md; do
    if [[ -f "$f" ]]; then
      total=$((total + $(file_tokens "$f")))
    fi
  done
  echo "$total"
}

# Per-skill: SKILL.md + references/* + scripts/*
skill_tokens() {
  local skill_dir="$1"
  local skill_file="${skill_dir}/SKILL.md"
  local skill_t=0
  local refs_t=0

  skill_t=$(file_tokens "$skill_file")
  refs_t=$(dir_tokens "${skill_dir}/references")

  # Add scripts/ dir if present
  local scripts_t=0
  scripts_t=$(dir_tokens "${skill_dir}/scripts")
  refs_t=$((refs_t + scripts_t))

  echo "${skill_t} ${refs_t}"
}

# Generate JSON snapshot
generate_snapshot() {
  local json='{"generated":"'"$(date -u +%Y-%m-%dT%H:%M:%SZ)"'","core_tokens":'"$(core_context_tokens)"',"skills":{'
  local first=true

  for skill_dir in "${SKILLS_DIR}"/*/; do
    [[ -d "$skill_dir" ]] || continue
    local name
    name=$(basename "$skill_dir")
    local tokens
    tokens=$(skill_tokens "$skill_dir")
    local skill_t=${tokens%% *}
    local refs_t=${tokens##* }
    local total_t=$((skill_t + refs_t))
    local line_count=0
    if [[ -f "${skill_dir}/SKILL.md" ]]; then
      line_count=$(wc -l < "${skill_dir}/SKILL.md" | tr -d ' ')
    fi

    # Sections present (for regression detection)
    local has_quality_gate=false has_gardener=false has_silent=false has_completion=false
    local sf="${skill_dir}/SKILL.md"
    if [[ -f "$sf" ]]; then
      grep -qiE 'quality gate|self-review|post-check' "$sf" 2>/dev/null && has_quality_gate=true
      grep -qi 'gardener' "$sf" 2>/dev/null && has_gardener=true
      grep -qiE 'silent mode|verbosity|token economy|machine mode' "$sf" 2>/dev/null && has_silent=true
      grep -qiE 'skill complete|completion' "$sf" 2>/dev/null && has_completion=true
    fi

    if [[ "$first" == true ]]; then
      first=false
    else
      json+=","
    fi
    json+='"'"$name"'":{"skill_tokens":'"$skill_t"',"refs_tokens":'"$refs_t"',"total_tokens":'"$total_t"',"lines":'"$line_count"',"sections":{"quality_gate":'"$has_quality_gate"',"gardener":'"$has_gardener"',"silent_mode":'"$has_silent"',"completion":'"$has_completion"'}}'
  done

  json+='}}'
  echo "$json" | python3 -m json.tool 2>/dev/null || echo "$json"
}

# Report mode
report() {
  local core_t
  core_t=$(core_context_tokens)

  echo -e "${CYAN}═══ Static Context Budget Report ═══${NC}"
  echo -e "Note: Static context cost (file sizes ÷ 4). Runtime token usage varies by task.\n"

  printf "%-22s %8s %8s %8s %s\n" "Skill" "SKILL.md" "Refs" "Total" "Status"
  printf "%-22s %8s %8s %8s %s\n" "─────" "────────" "────" "─────" "──────"

  local grand_total=0

  for skill_dir in "${SKILLS_DIR}"/*/; do
    [[ -d "$skill_dir" ]] || continue
    local name
    name=$(basename "$skill_dir")
    if [[ -n "$SKILL_FILTER" && "$name" != "$SKILL_FILTER" ]]; then continue; fi

    local tokens
    tokens=$(skill_tokens "$skill_dir")
    local skill_t=${tokens%% *}
    local refs_t=${tokens##* }
    local total_t=$((skill_t + refs_t))
    grand_total=$((grand_total + total_t))

    local status="${GREEN}OK${NC}"
    if [[ "$total_t" -gt 15000 ]]; then
      status="${RED}HIGH${NC}"
    elif [[ "$total_t" -gt 8000 ]]; then
      status="${YELLOW}WARN${NC}"
    fi

    printf "%-22s %8d %8d %8d " "$name" "$skill_t" "$refs_t" "$total_t"
    echo -e "$status"
  done

  echo ""
  echo -e "Core context (CLAUDE.md + agents + protocols): ${CYAN}${core_t}${NC} tokens"
  echo -e "Skills total: ${CYAN}${grand_total}${NC} tokens"
  echo -e "Activation cost (core + avg skill): ${CYAN}$((core_t + grand_total / $(ls -d "${SKILLS_DIR}"/*/ 2>/dev/null | wc -l | tr -d ' ')))${NC} tokens (approx)"
}

# Diff mode
diff_report() {
  if [[ ! -f "$BASELINE_FILE" ]]; then
    echo -e "${RED}No baseline found at ${BASELINE_FILE}. Run --snapshot first.${NC}"
    exit 1
  fi

  echo -e "${CYAN}═══ Budget Diff vs Baseline ═══${NC}\n"

  local baseline_total=0
  local current_total=0
  local has_issues=false

  printf "%-22s %8s %8s %8s %s\n" "Skill" "Baseline" "Current" "Delta" "Status"
  printf "%-22s %8s %8s %8s %s\n" "─────" "────────" "───────" "─────" "──────"

  for skill_dir in "${SKILLS_DIR}"/*/; do
    [[ -d "$skill_dir" ]] || continue
    local name
    name=$(basename "$skill_dir")
    if [[ -n "$SKILL_FILTER" && "$name" != "$SKILL_FILTER" ]]; then continue; fi

    local tokens
    tokens=$(skill_tokens "$skill_dir")
    local skill_t=${tokens%% *}
    local refs_t=${tokens##* }
    local current_t=$((skill_t + refs_t))
    current_total=$((current_total + current_t))

    # Read baseline value
    local base_t=0
    if command -v python3 >/dev/null 2>&1; then
      base_t=$(python3 -c "
import json, sys
try:
    d = json.load(open('$BASELINE_FILE'))
    print(d.get('skills',{}).get('$name',{}).get('total_tokens',0))
except: print(0)
" 2>/dev/null || echo 0)
    fi
    baseline_total=$((baseline_total + base_t))

    local delta=$((current_t - base_t))
    local status="${GREEN}OK${NC}"

    if [[ "$base_t" -gt 0 ]]; then
      local pct=$((delta * 100 / base_t))
      if [[ "$delta" -gt 500 ]] && [[ "$pct" -gt 10 ]]; then
        status="${YELLOW}WARN +${pct}%${NC}"
        has_issues=true
      fi
    elif [[ "$base_t" -eq 0 ]] && [[ "$current_t" -gt 0 ]]; then
      status="${CYAN}NEW${NC}"
    fi

    printf "%-22s %8d %8d %+8d " "$name" "$base_t" "$current_t" "$delta"
    echo -e "$status"
  done

  local total_delta=$((current_total - baseline_total))
  echo ""
  echo -e "Total delta: ${CYAN}${total_delta}${NC} tokens"

  if [[ "$baseline_total" -gt 0 ]]; then
    local total_pct=$((total_delta * 100 / baseline_total))
    if [[ "$total_pct" -gt 10 ]]; then
      echo -e "${RED}ERROR: Total budget grew ${total_pct}% (threshold: 10%)${NC}"
      exit 1
    elif [[ "$total_pct" -gt 5 ]]; then
      echo -e "${YELLOW}WARNING: Total budget grew ${total_pct}% (threshold: 5%)${NC}"
    fi
  fi
}

# Main
case "$MODE" in
  snapshot)
    generate_snapshot > "$BASELINE_FILE"
    echo -e "${GREEN}Baseline saved to ${BASELINE_FILE}${NC}"
    ;;
  diff)
    diff_report
    ;;
  report)
    if [[ "$JSON_OUTPUT" == true ]]; then
      generate_snapshot
    else
      report
    fi
    ;;
esac
