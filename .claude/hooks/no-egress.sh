#!/usr/bin/env bash
# PreToolUse hook on Bash: deny commands whose job is to move bytes off this
# machine. Closes the casual exfiltration path a prompt injection would take.
#
# Scope, stated plainly: this is defence in depth, NOT a proof. Skills here need
# python3, npx and ./gradlew, and any interpreter can open a socket without
# naming a blocked binary. See docs/patterns.md "20. Egress fence" for what this
# does and does not buy.
#
# Contract: reads the PreToolUse JSON on stdin, exit 2 blocks the call and sends
# stderr back to the model. Any other exit lets the call through.
set -uo pipefail

payload=$(cat)

# jq is not guaranteed; python3 is (the repo's own scripts require it).
command=$(printf '%s' "$payload" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)
print((d.get("tool_input") or {}).get("command") or "")
' 2>/dev/null) || exit 0

[ -z "$command" ] && exit 0

# Strip quotes so `"curl"` and `'curl'` cannot walk past a word-boundary match,
# and fold whitespace so `curl\n-s` reads as one token stream.
normalised=$(printf '%s' "$command" | tr -d '"'\''' | tr '\n\t' '  ')

# Egress binaries. Matched on a word boundary so `mycurl` and `--curl-opt` pass.
egress_re='(^|[^A-Za-z0-9_./-])(curl|wget|nc|ncat|netcat|socat|telnet|ftp|sftp|scp|rsync|ssh)([^A-Za-z0-9_-]|$)'

# Protocol clients that are not a bare binary name.
extra_re='(openssl[[:space:]]+s_client|gh[[:space:]]+gist[[:space:]]+create|git[[:space:]]+push[[:space:]]+http)'

if printf '%s' "$normalised" | grep -qE "$egress_re" ||
   printf '%s' "$normalised" | grep -qE "$extra_re"; then
    cat >&2 <<EOF
BLOCKED by .claude/hooks/no-egress.sh — this command can send data off the machine.

  $command

Skills in this repository do not need network egress from Bash. If a task
genuinely does, run it yourself outside the agent, or remove this hook from
.claude/settings.json for that session and say why.

Rationale: docs/patterns.md "20. Egress fence".
EOF
    exit 2
fi

exit 0
