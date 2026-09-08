#!/usr/bin/env bash
# Telemetry hook: appends 1 JSONL event to tests/telemetry/events.jsonl.
# Called by Gardener Protocol after each skill run.
#
# Usage:
#   bash scripts/hooks/telemetry-hook.sh \
#     --skill api-tests --status complete --gardener-count 2 [--error-type none]
#
# Flags:
#   --skill           Skill name (required)
#   --status          complete|partial|loop_guard (required)
#   --gardener-count  Number of Gardener observations (required)
#   --error-type      none|compilation|network|timeout|loop_guard (default: none)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TELEMETRY_DIR="${REPO_ROOT}/tests/telemetry"
EVENTS_FILE="${TELEMETRY_DIR}/events.jsonl"

SKILL=""
STATUS=""
GARDENER_COUNT=0
ERROR_TYPE="none"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL="$2"; shift 2 ;;
    --status) STATUS="$2"; shift 2 ;;
    --gardener-count) GARDENER_COUNT="$2"; shift 2 ;;
    --error-type) ERROR_TYPE="$2"; shift 2 ;;
    *) shift ;;
  esac
done

if [[ -z "$SKILL" || -z "$STATUS" ]]; then
  echo "ERROR: --skill and --status are required" >&2
  exit 1
fi

# Validate status
case "$STATUS" in
  complete|partial|loop_guard) ;;
  *) echo "ERROR: --status must be complete|partial|loop_guard" >&2; exit 1 ;;
esac

# Validate error_type
case "$ERROR_TYPE" in
  none|compilation|network|timeout|loop_guard) ;;
  *) echo "ERROR: --error-type must be none|compilation|network|timeout|loop_guard" >&2; exit 1 ;;
esac

# Ensure directory exists
mkdir -p "$TELEMETRY_DIR"

# Generate ISO timestamp
TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Append JSONL event (silent — no stdout)
printf '{"ts":"%s","skill":"%s","status":"%s","gardener_count":%d,"error_type":"%s"}\n' \
  "$TS" "$SKILL" "$STATUS" "$GARDENER_COUNT" "$ERROR_TYPE" >> "$EVENTS_FILE"

exit 0
