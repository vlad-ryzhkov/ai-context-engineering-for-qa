#!/usr/bin/env bash
# Runtime Token Budget Tracker (STUB) — tracks actual token consumption per skill invocation.
#
# Complementary to token-budget.sh (static context cost). This tracks runtime usage.
# Currently a manual-entry stub. Future: auto-collect from Claude Code stdout or API.
#
# Usage:
#   bash scripts/lib/runtime-budget.sh record --skill api-tests --input-tokens 5000 --output-tokens 3000 [--duration 12.5]
#   bash scripts/lib/runtime-budget.sh report
#   bash scripts/lib/runtime-budget.sh report --skill api-tests
#   bash scripts/lib/runtime-budget.sh check
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

BENCHMARKS_DIR="tests/benchmarks"
BUDGET_FILE="${BENCHMARKS_DIR}/runtime-budget.jsonl"

# Skill → category + threshold mapping (bash 3.x compatible)
skill_category() {
  local skill="$1"
  case "$skill" in
    fix-markdown|pr|qa-translate|curate-lessons|update-ai-setup)
      echo "lightweight" ;;
    api-tests|api-tests-java|api-test-review|api-mocks|load-tests)
      echo "heavy" ;;
    *)
      echo "standard" ;;
  esac
}

threshold_for_category() {
  case "$1" in
    lightweight) echo 10000 ;;
    heavy) echo 100000 ;;
    pipeline) echo 300000 ;;
    *) echo 50000 ;;  # standard
  esac
}

MODE="${1:-help}"
shift 2>/dev/null || true

SKILL_NAME=""
INPUT_TOKENS=0
OUTPUT_TOKENS=0
DURATION=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL_NAME="$2"; shift 2 ;;
    --input-tokens) INPUT_TOKENS="$2"; shift 2 ;;
    --output-tokens) OUTPUT_TOKENS="$2"; shift 2 ;;
    --duration) DURATION="$2"; shift 2 ;;
    *) shift ;;
  esac
done

ensure_dir() {
  if [[ ! -d "$BENCHMARKS_DIR" ]]; then
    mkdir -p "$BENCHMARKS_DIR"
  fi
}

# Record a benchmark entry
cmd_record() {
  if [[ -z "$SKILL_NAME" ]]; then
    echo -e "${RED}ERROR: --skill is required${NC}"
    echo "Usage: bash scripts/lib/runtime-budget.sh record --skill <name> --input-tokens <N> --output-tokens <N>"
    exit 1
  fi

  ensure_dir

  local total_tokens=$((INPUT_TOKENS + OUTPUT_TOKENS))
  local timestamp
  timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  local duration_field=""
  if [[ -n "$DURATION" ]]; then
    duration_field=',"duration_s":'"$DURATION"
  fi

  local record='{"timestamp":"'"$timestamp"'","skill":"'"$SKILL_NAME"'","input_tokens":'"$INPUT_TOKENS"',"output_tokens":'"$OUTPUT_TOKENS"',"total_tokens":'"$total_tokens"''${duration_field}'}'
  echo "$record" >> "$BUDGET_FILE"

  echo -e "${GREEN}✓${NC} Recorded: ${SKILL_NAME} — ${total_tokens} tokens (${INPUT_TOKENS} in / ${OUTPUT_TOKENS} out)"
}

# Report mode — tabular summary
cmd_report() {
  if [[ ! -f "$BUDGET_FILE" ]]; then
    echo -e "${CYAN}═══ Runtime Budget Report ═══${NC}"
    echo -e "\nNo benchmarks recorded yet. Use 'record' command to add entries."
    echo -e "File: ${BUDGET_FILE}"
    return
  fi

  echo -e "${CYAN}═══ Runtime Budget Report ═══${NC}\n"

  printf "%-22s %8s %8s %8s %10s %s\n" "Skill" "Runs" "Avg Tok" "Last Tok" "Threshold" "Status"
  printf "%-22s %8s %8s %8s %10s %s\n" "─────" "────" "───────" "────────" "─────────" "──────"

  # Get unique skills
  local skills
  if [[ -n "$SKILL_NAME" ]]; then
    skills="$SKILL_NAME"
  else
    skills=$(jq -r '.skill' "$BUDGET_FILE" 2>/dev/null | sort -u)
  fi

  if [[ -z "$skills" ]]; then
    echo -e "  No data found."
    return
  fi

  while IFS= read -r skill; do
    [[ -z "$skill" ]] && continue

    local stats
    stats=$(jq -r --arg skill "$skill" 'select(.skill==$skill) | .total_tokens' "$BUDGET_FILE" 2>/dev/null | \
      awk '{sum+=$1; count++; last=$1} END {if(count>0) printf "%d %d %d", count, int(sum/count), last; else print "0 0 0"}')

    local run_count avg_tok last_tok
    run_count=$(echo "$stats" | cut -d' ' -f1)
    avg_tok=$(echo "$stats" | cut -d' ' -f2)
    last_tok=$(echo "$stats" | cut -d' ' -f3)

    local cat threshold status
    cat=$(skill_category "$skill")
    threshold=$(threshold_for_category "$cat")

    if [[ "$avg_tok" -gt "$threshold" ]]; then
      status="${RED}OVER${NC}"
    elif [[ "$avg_tok" -gt $((threshold * 80 / 100)) ]]; then
      status="${YELLOW}WARN${NC}"
    else
      status="${GREEN}OK${NC}"
    fi

    printf "%-22s %8d %8d %8d %10d " "$skill" "$run_count" "$avg_tok" "$last_tok" "$threshold"
    echo -e "$status"
  done <<< "$skills"
}

# Check mode — anomaly detection
cmd_check() {
  if [[ ! -f "$BUDGET_FILE" ]]; then
    echo -e "${GREEN}✓${NC} No benchmarks to check"
    return
  fi

  echo -e "${CYAN}═══ Runtime Budget Anomaly Check ═══${NC}\n"

  local anomalies=0

  jq -r '[.skill, .total_tokens] | @tsv' "$BUDGET_FILE" 2>/dev/null | \
  awk -F'\t' '{
    skills[$1] = skills[$1] " " $2
  }
  END {
    for (s in skills) {
      n = split(skills[s], r, " ")
      if (n < 3) continue
      sum = 0
      for (i = 1; i < n; i++) sum += r[i]
      avg = sum / (n - 1)
      latest = r[n]
      if (avg > 0 && latest > 2 * avg)
        printf "ANOMALY|%s|%d|%d|%.1fx\n", s, latest, int(avg), latest / avg
    }
  }' | while IFS='|' read -r tag skill latest avg ratio; do
    echo -e "  ${RED}ANOMALY${NC} ${skill}: latest ${latest} tokens vs avg ${avg} (${ratio})"
    ((anomalies++)) || true
  done

  if [[ "$anomalies" -eq 0 ]]; then
    echo -e "${GREEN}✓${NC} No anomalies detected"
  fi
}

# Main dispatch
case "$MODE" in
  record)
    cmd_record
    ;;
  report)
    cmd_report
    ;;
  check)
    cmd_check
    ;;
  help|-h|--help)
    echo "Runtime Token Budget Tracker (STUB)"
    echo ""
    echo "Usage:"
    echo "  bash scripts/lib/runtime-budget.sh record --skill <name> --input-tokens <N> --output-tokens <N> [--duration <sec>]"
    echo "  bash scripts/lib/runtime-budget.sh report [--skill <name>]"
    echo "  bash scripts/lib/runtime-budget.sh check"
    echo ""
    echo "Data stored in: ${BUDGET_FILE} (JSONL format)"
    echo ""
    echo "NOTE: This is a stub. Token metrics are recorded manually."
    echo "Future: auto-collect from Claude Code stdout parsing or API hooks."
    ;;
  *)
    echo -e "${RED}Unknown command: $MODE${NC}"
    echo "Use: record | report | check | help"
    exit 1
    ;;
esac
