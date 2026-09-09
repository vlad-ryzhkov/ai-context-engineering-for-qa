#!/usr/bin/env bash
set -u

input=$(cat)

field() { printf '%s' "$input" | jq -r "$1" 2>/dev/null; }

model=$(field '.model.display_name // "?"')
dir=$(field '.workspace.current_dir // ""')
tokens=$(field '.context_window.total_input_tokens // 0')
window=$(field '.context_window.context_window_size // 200000')
session=$(field '.session_id // "unknown"')

warn_pct=${CTX_WARN_PCT:-60}
alarm_pct=${CTX_ALARM_PCT:-85}
idle_min=${CTX_IDLE_MIN:-5}

marker="${TMPDIR:-/tmp}/claude_statusline_${session}"
idle=0
if [ -f "$marker" ]; then
  now=$(date +%s)
  prev=$(stat -f %m "$marker" 2>/dev/null || stat -c %Y "$marker" 2>/dev/null || echo "$now")
  idle=$(( now - prev ))
fi
: > "$marker"

pct=0
if [ "$window" -gt 0 ] 2>/dev/null; then
  pct=$(( tokens * 100 / window ))
fi

colour='\033[0;32m'
if [ "$pct" -ge "$warn_pct" ]; then colour='\033[1;33m'; fi
if [ "$pct" -ge "$alarm_pct" ]; then colour='\033[1;31m'; fi

branch=""
if [ -n "$dir" ]; then
  branch=$( (cd "$dir" 2>/dev/null && git branch --show-current 2>/dev/null) || true )
fi

pause=""
if [ "$idle" -ge $(( idle_min * 60 )) ]; then
  pause=$(printf ' \033[2m⏸ %dm\033[0m' "$(( idle / 60 ))")
fi

printf '%s' "$model"
if [ -n "$branch" ]; then printf ' \033[2m%s\033[0m' "$branch"; fi
printf " ${colour}ctx %sk/%sk %s%%\033[0m" "$(( tokens / 1000 ))" "$(( window / 1000 ))" "$pct"
printf '%b' "$pause"
printf '\n'
