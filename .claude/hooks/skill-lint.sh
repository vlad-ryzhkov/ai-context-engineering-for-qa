#!/bin/bash
# Post-edit hook: fast validation of SKILL.md and qa_agent.md
# Full audit: /skill-audit
# Full audit: /skill-audit

set -e

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# Filter: only skill files and qa_agent.md
if [[ ! ("$FILE_PATH" == */.claude/skills/*/SKILL.md || "$FILE_PATH" == */.claude/qa_agent.md) ]]; then
  exit 0
fi

FINDINGS=""
FILENAME=$(basename "$FILE_PATH")
SKILL_DIR=$(basename "$(dirname "$FILE_PATH")")
LABEL="${SKILL_DIR}/${FILENAME}"

# Check 1: Line count
LINE_COUNT=$(wc -l < "$FILE_PATH" | tr -d ' ')
if [ "$LINE_COUNT" -gt 500 ]; then
  echo "  ⚠️ WARNING: ${LINE_COUNT} lines (limit: ≤500, split to references/)" >&2
fi

# Check 2: Self-Review Protocol (the anti-pattern, not the prohibition)
if grep -q 'Формат отчёта Self-Review\|Алгоритм Self-Review\|Scorecard Self-Review' "$FILE_PATH" 2>/dev/null; then
  FINDINGS="${FINDINGS}\n  ⛔ CRITICAL: Self-Review Protocol detected — replace with Post-Check inline"
fi

# Check 3: "DO NOT FIX" / "НЕ ИСПРАВЛЯТЬ" as instruction
if grep -q '\*\*НЕ ИСПРАВЛЯТЬ\*\*\|\*\*DO NOT FIX\*\*' "$FILE_PATH" 2>/dev/null; then
  FINDINGS="${FINDINGS}\n  ⛔ CRITICAL: 'DO NOT FIX' instruction — replace with 'FIX CODE/audit'"
fi

# Check 4: Tier 1 Baseline — Quality Gate / Self-Review section
if [[ "$FILE_PATH" == *SKILL.md ]]; then
  if ! grep -qiE 'quality gate|self-review|post-check' "$FILE_PATH" 2>/dev/null; then
    FINDINGS="${FINDINGS}\n  ⚠️ WARN: Missing Quality Gate / Self-Review section (Tier 1 Baseline S10)"
  fi

  # Check 5: Gardener reference
  if ! grep -qi 'gardener' "$FILE_PATH" 2>/dev/null; then
    FINDINGS="${FINDINGS}\n  ⚠️ WARN: Missing Gardener protocol reference (Tier 1 Baseline S11)"
  fi

  # Check 6: SILENT MODE / Verbosity
  if ! grep -qiE 'silent mode|verbosity|token economy|machine mode' "$FILE_PATH" 2>/dev/null; then
    FINDINGS="${FINDINGS}\n  ⚠️ WARN: Missing SILENT MODE / Verbosity (Tier 1 Baseline S12)"
  fi

  # Check 7: Completion block
  if ! grep -qiE 'skill complete|completion' "$FILE_PATH" 2>/dev/null; then
    FINDINGS="${FINDINGS}\n  ⚠️ WARN: Missing SKILL COMPLETE block (Tier 1 Baseline S13)"
  fi
fi

if [ -n "$FINDINGS" ]; then
  echo -e "🔍 skill-lint: ${LABEL}${FINDINGS}" >&2
  echo -e "  💡 Fix issues above. Full audit: /skill-audit" >&2
  exit 2
fi

exit 0
