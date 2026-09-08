#!/bin/bash
# Post-edit hook: warn when Write tool overwrites a governed context file.
# Advisory (exit 0) — change to exit 2 to make it blocking.

set -e

INPUT=$(cat)
TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // empty')
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

if [ -z "$FILE_PATH" ] || [ -z "$TOOL_NAME" ]; then
  exit 0
fi

# Only check Write (full overwrite), not Edit (surgical replace)
if [ "$TOOL_NAME" != "Write" ]; then
  exit 0
fi

# Governed paths — context files that should evolve incrementally
GOVERNED=false
case "$FILE_PATH" in
  */CLAUDE.md) GOVERNED=true ;;
  */.claude/qa_agent.md) GOVERNED=true ;;
  */.claude/agents/*.md) GOVERNED=true ;;
  */.claude/protocols/*.md) GOVERNED=true ;;
  */.claude/qa-antipatterns/*.md) GOVERNED=true ;;
  */.claude/qa-antipatterns/**/*.md) GOVERNED=true ;;
  */.claude/skills/*/SKILL.md) GOVERNED=true ;;
  */.claude/rules/*.md) GOVERNED=true ;;
  */.ai-lessons/*.md) GOVERNED=true ;;
  */.ai-lessons/*.jsonl) GOVERNED=true ;;
esac

if [ "$GOVERNED" = true ] && [ -f "$FILE_PATH" ]; then
  echo "  DELTA-GUARD: Write (full overwrite) on governed file: $(basename "$FILE_PATH")" >&2
  echo "  Prefer Edit (surgical replace) to prevent context collapse." >&2
fi

exit 0
