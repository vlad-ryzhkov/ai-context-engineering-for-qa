#!/usr/bin/env bash
# Reflector — Layer 1: Detection Engine (bash)
#
# Reads gardener-log.jsonl + events.jsonl + pending.md, groups by keyword
# signatures, flags recurring patterns (3+ occurrences).
# Output: tests/telemetry/reflector-report.json (or stdout with --dry-run).
#
# The bash script NEVER writes to pending.md. It produces a detection report
# that Layer 2 (LLM protocol: .claude/protocols/reflector.md) reads and acts on.
#
# Usage:
#   bash scripts/lib/reflector.sh                    # Full detection -> report
#   bash scripts/lib/reflector.sh --dry-run           # Print report to stdout only
#   bash scripts/lib/reflector.sh --window 30         # Last 30 events (default: 50)
#   bash scripts/lib/reflector.sh --skill api-tests   # Single skill scope
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

GARDENER_LOG="${REPO_ROOT}/.ai-lessons/gardener-log.jsonl"
EVENTS_FILE="${REPO_ROOT}/tests/telemetry/events.jsonl"
PENDING_FILE="${REPO_ROOT}/.ai-lessons/pending.md"
REPORT_FILE="${REPO_ROOT}/tests/telemetry/reflector-report.json"

DRY_RUN=false
WINDOW=50
SKILL_FILTER=""
THRESHOLD=3

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=true; shift ;;
    --window) WINDOW="$2"; shift 2 ;;
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    --threshold) THRESHOLD="$2"; shift 2 ;;
    -h|--help)
      echo "Reflector — Layer 1: Detection Engine"
      echo ""
      echo "Usage:"
      echo "  bash scripts/lib/reflector.sh                    # Full detection -> report"
      echo "  bash scripts/lib/reflector.sh --dry-run           # Print to stdout only"
      echo "  bash scripts/lib/reflector.sh --window 30         # Last N events (default: 50)"
      echo "  bash scripts/lib/reflector.sh --skill api-tests   # Single skill scope"
      echo "  bash scripts/lib/reflector.sh --threshold 2       # Min occurrences (default: 3)"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# Check jq dependency
if ! command -v jq >/dev/null 2>&1; then
  echo -e "${RED}ERROR: jq is required but not found${NC}" >&2
  exit 1
fi

echo -e "${CYAN}=== Reflector — Layer 1: Detection Engine ===${NC}"
echo ""

FINDINGS="[]"
HAS_DATA=false

# --- Helper: extract 2-keyword signature from text ---
# Removes stop words, takes first 2 content words by order of appearance.
# Order-of-appearance preserves semantic structure across paraphrased observations
# (e.g., "Thread.sleep in test code" and "Thread.sleep use Awaitility" → "thread sleep").
extract_keywords() {
  echo "$1" | tr '[:upper:]' '[:lower:]' | \
    sed 's/[^a-z0-9 ]/ /g' | \
    tr -s ' ' '\n' | \
    grep -vE '^.{0,2}$' | \
    grep -vE '^(the|are|was|were|been|being|have|has|had|does|did|will|would|could|should|may|might|must|shall|can|need|dare|ought|used|for|with|from|into|through|during|before|after|above|below|between|out|off|over|under|again|further|then|once|here|there|when|where|why|how|all|each|every|both|few|more|most|other|some|such|nor|not|only|own|same|than|too|very|just|because|but|and|that|this|its|use|using|should|must|rule|banned|required|code|test|tests|add|also|make|made|run|get|set|new|old)$' | \
    head -2 | \
    paste -sd ' ' -
}

# --- Detector 1: Recurring Gardener Observations ---
detect_gardener() {
  if [[ ! -f "$GARDENER_LOG" ]] || [[ ! -s "$GARDENER_LOG" ]]; then
    echo -e "  ${YELLOW}SKIP${NC} gardener-log.jsonl: empty or missing"
    return
  fi
  HAS_DATA=true

  echo -e "  Scanning gardener-log.jsonl..."

  # Apply skill filter and window
  local filtered
  if [[ -n "$SKILL_FILTER" ]]; then
    filtered=$(tail -n "$WINDOW" "$GARDENER_LOG" | jq -c "select(.skill == \"$SKILL_FILTER\")" 2>/dev/null)
  else
    filtered=$(tail -n "$WINDOW" "$GARDENER_LOG" 2>/dev/null)
  fi

  if [[ -z "$filtered" ]]; then
    echo -e "  ${YELLOW}SKIP${NC} No gardener entries in window"
    return
  fi

  # Group by keyword signature of proposed_rule
  local groups
  groups=$(echo "$filtered" | while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    rule=$(echo "$line" | jq -r '.proposed_rule // empty' 2>/dev/null)
    [[ -z "$rule" ]] && continue
    keywords=$(extract_keywords "$rule")
    echo "${keywords}|${line}"
  done | sort -t'|' -k1,1)

  if [[ -z "$groups" ]]; then
    return
  fi

  # Count and filter by threshold
  local current_sig="" current_count=0 current_entries="" current_skills=""
  while IFS='|' read -r sig entry; do
    [[ -z "$sig" ]] && continue
    if [[ "$sig" != "$current_sig" ]]; then
      # Emit previous group if above threshold
      if [[ "$current_count" -ge "$THRESHOLD" && -n "$current_sig" ]]; then
        local skills_array
        skills_array=$(echo "$current_skills" | tr ',' '\n' | sort -u | jq -R . | jq -s .)
        local evidence_array
        evidence_array=$(echo "$current_entries" | head -5)
        local finding
        finding=$(jq -n \
          --arg detector "recurring_gardener" \
          --argjson count "$current_count" \
          --argjson skills "$skills_array" \
          --arg keyword_signature "$current_sig" \
          --argjson evidence "$(echo "$evidence_array" | jq -s '.')" \
          '{detector: $detector, count: $count, skills: $skills, keyword_signature: $keyword_signature, evidence: $evidence}')
        FINDINGS=$(echo "$FINDINGS" | jq --argjson f "$finding" '. + [$f]')
      fi
      current_sig="$sig"
      current_count=0
      current_entries=""
      current_skills=""
    fi
    ((current_count++)) || true
    if [[ -n "$current_entries" ]]; then
      current_entries="${current_entries}"$'\n'"${entry}"
    else
      current_entries="$entry"
    fi
    skill=$(echo "$entry" | jq -r '.skill // "unknown"' 2>/dev/null)
    if [[ -n "$current_skills" ]]; then
      current_skills="${current_skills},${skill}"
    else
      current_skills="$skill"
    fi
  done <<< "$groups"

  # Don't forget the last group
  if [[ "$current_count" -ge "$THRESHOLD" && -n "$current_sig" ]]; then
    local skills_array
    skills_array=$(echo "$current_skills" | tr ',' '\n' | sort -u | jq -R . | jq -s .)
    local evidence_array
    evidence_array=$(echo "$current_entries" | head -5)
    local finding
    finding=$(jq -n \
      --arg detector "recurring_gardener" \
      --argjson count "$current_count" \
      --argjson skills "$skills_array" \
      --arg keyword_signature "$current_sig" \
      --argjson evidence "$(echo "$evidence_array" | jq -s '.')" \
      '{detector: $detector, count: $count, skills: $skills, keyword_signature: $keyword_signature, evidence: $evidence}')
    FINDINGS=$(echo "$FINDINGS" | jq --argjson f "$finding" '. + [$f]')
  fi
}

# --- Detector 2: Recurring Failures ---
detect_failures() {
  if [[ ! -f "$EVENTS_FILE" ]] || [[ ! -s "$EVENTS_FILE" ]]; then
    echo -e "  ${YELLOW}SKIP${NC} events.jsonl: empty or missing"
    return
  fi
  HAS_DATA=true

  echo -e "  Scanning events.jsonl..."

  # Filter to failures only, apply window and skill filter
  local filter_expr='select(.status != "complete")'
  if [[ -n "$SKILL_FILTER" ]]; then
    filter_expr="select(.status != \"complete\" and .skill == \"$SKILL_FILTER\")"
  fi

  local failures
  failures=$(tail -n "$WINDOW" "$EVENTS_FILE" | jq -c "$filter_expr" 2>/dev/null)

  if [[ -z "$failures" ]]; then
    echo -e "  ${GREEN}OK${NC} No failures in window"
    return
  fi

  # Group by (skill, error_type) — use process substitution to avoid subshell
  local grouped
  grouped=$(echo "$failures" | jq -r '[.skill, .error_type] | @tsv' 2>/dev/null | sort | uniq -c | sort -rn)

  while read -r count skill error_type; do
    [[ -z "$count" ]] && continue
    if [[ "$count" -ge "$THRESHOLD" ]]; then
      local sig="${skill} ${error_type}"
      local evidence
      evidence=$(echo "$failures" | jq -c "select(.skill == \"$skill\" and .error_type == \"$error_type\")" 2>/dev/null | head -3 | jq -s '.')
      local finding
      finding=$(jq -n \
        --arg detector "recurring_failure" \
        --argjson count "$count" \
        --arg skill "$skill" \
        --arg keyword_signature "$sig" \
        --argjson evidence "$evidence" \
        '{detector: $detector, count: $count, skills: [$skill], keyword_signature: $keyword_signature, evidence: $evidence}')
      FINDINGS=$(echo "$FINDINGS" | jq --argjson f "$finding" '. + [$f]')
    fi
  done <<< "$grouped"
}

# --- Detector 3: Pending.md Pattern Scan ---
detect_pending_patterns() {
  if [[ ! -f "$PENDING_FILE" ]] || [[ ! -s "$PENDING_FILE" ]]; then
    echo -e "  ${YELLOW}SKIP${NC} pending.md: empty or missing"
    return
  fi

  echo -e "  Scanning pending.md..."

  # Extract RULE lines
  local rules
  rules=$(grep -E '^\s*-\s*(RULE|\[REFLECTOR\])' "$PENDING_FILE" 2>/dev/null || true)

  if [[ -z "$rules" ]]; then
    echo -e "  ${GREEN}OK${NC} No rule entries in pending.md"
    return
  fi
  HAS_DATA=true

  # Group by keyword signature
  local groups
  groups=$(echo "$rules" | while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    keywords=$(extract_keywords "$line")
    echo "${keywords}|${line}"
  done | sort -t'|' -k1,1)

  if [[ -z "$groups" ]]; then
    return
  fi

  local current_sig="" current_count=0 current_entries=""
  while IFS='|' read -r sig entry; do
    [[ -z "$sig" ]] && continue
    if [[ "$sig" != "$current_sig" ]]; then
      if [[ "$current_count" -ge "$THRESHOLD" && -n "$current_sig" ]]; then
        local evidence_array
        evidence_array=$(echo "$current_entries" | jq -R . | jq -s '.')
        local finding
        finding=$(jq -n \
          --arg detector "pending_pattern" \
          --argjson count "$current_count" \
          --arg keyword_signature "$current_sig" \
          --argjson evidence "$evidence_array" \
          '{detector: $detector, count: $count, skills: [], keyword_signature: $keyword_signature, evidence: $evidence}')
        FINDINGS=$(echo "$FINDINGS" | jq --argjson f "$finding" '. + [$f]')
      fi
      current_sig="$sig"
      current_count=0
      current_entries=""
    fi
    ((current_count++)) || true
    if [[ -n "$current_entries" ]]; then
      current_entries="${current_entries}"$'\n'"${entry}"
    else
      current_entries="$entry"
    fi
  done <<< "$groups"

  # Last group
  if [[ "$current_count" -ge "$THRESHOLD" && -n "$current_sig" ]]; then
    local evidence_array
    evidence_array=$(echo "$current_entries" | jq -R . | jq -s '.')
    local finding
    finding=$(jq -n \
      --arg detector "pending_pattern" \
      --argjson count "$current_count" \
      --arg keyword_signature "$current_sig" \
      --argjson evidence "$evidence_array" \
      '{detector: $detector, count: $count, skills: [], keyword_signature: $keyword_signature, evidence: $evidence}')
    FINDINGS=$(echo "$FINDINGS" | jq --argjson f "$finding" '. + [$f]')
  fi
}

# --- Run all detectors ---
detect_gardener
detect_failures
detect_pending_patterns

# --- Build report ---
TOTAL=$(echo "$FINDINGS" | jq 'length')
TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)

REPORT=$(jq -n \
  --arg ts "$TS" \
  --argjson window "$WINDOW" \
  --argjson findings "$FINDINGS" \
  '{ts: $ts, window: $window, findings: $findings}')

# --- Output ---
echo ""
if [[ "$TOTAL" -eq 0 ]]; then
  echo -e "${GREEN}OK${NC} No recurring patterns detected (threshold: ${THRESHOLD}+)"
  if [[ "$HAS_DATA" = false ]]; then
    echo -e "  ${YELLOW}Note:${NC} No telemetry data found. Run skills with Gardener Protocol to populate."
  fi
else
  echo -e "${CYAN}Found ${TOTAL} recurring pattern(s):${NC}"
  echo "$FINDINGS" | jq -r '.[] | "  \(.detector): \(.keyword_signature) (\(.count)x)"'
fi

if [[ "$DRY_RUN" = true ]]; then
  echo ""
  echo -e "${CYAN}--- Report (dry-run, stdout only) ---${NC}"
  echo "$REPORT" | jq .
else
  mkdir -p "$(dirname "$REPORT_FILE")"
  echo "$REPORT" | jq . > "$REPORT_FILE"
  echo ""
  echo -e "Report written to: ${REPORT_FILE}"
fi

echo ""
if [[ "$TOTAL" -gt 0 ]]; then
  echo -e "${CYAN}Next step:${NC} Run Layer 2 (LLM protocol) → .claude/protocols/reflector.md"
fi

exit 0
