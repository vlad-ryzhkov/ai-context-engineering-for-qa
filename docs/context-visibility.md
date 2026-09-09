# Seeing your context cost while you work

Two signals, two moments. The first fires once, at session start, and reports what the always-loaded
corpus costs before you type anything. The second sits at the bottom of the screen and reports the
live context window. Both are shell one-liners in `settings.json` — nothing is installed globally.

## Signal 1 — the always-loaded budget, at session start

`scripts/context-check.py` measures every file Claude Code loads on every session: the repo
`CLAUDE.md`, `.claude/rules/**` without `paths:` front-matter, and the same pair under `~/.claude`.
Over the cap it prints one line and nothing otherwise:

```text
BUDGET    always-loaded ~12405 tok > cap 10000 (8 files, 29773 bytes @ 2.4 B/tok); biggest: CLAUDE.md ~5649, verification.md ~2000, git.md ~1249
```

Wire it into `~/.claude/settings.json` (or the repo's `.claude/settings.json`):

```json
{
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "python3 $CLAUDE_PROJECT_DIR/scripts/context-check.py --budget-only 2>/dev/null | jq -Rrs 'select(length>0) | {systemMessage:.}'"
          }
        ]
      }
    ]
  }
}
```

The cap and the bytes-per-token ratio are not hardcoded in the script — it parses them out of
`docs/context-budget.md`, from the line `<!-- context-check: TOKEN_CAP=10000 BYTES_PER_TOKEN=2.4 -->`.
Change the doc, and the checker follows. That doc also explains why the ratio is 2.4 and not 4.

## Signal 2 — the live context window, in the status line

`scripts/statusline-context-warn.sh` prints the model, the git branch, and the window in thousands
of tokens, coloured by how full it is:

```text
Opus 5  worktree-share-context-tools  ctx 562k/1000k 56%
Opus 5  worktree-share-context-tools  ctx 890k/1000k 89%   ⏸ 112m
```

Green under 60%, yellow from 60%, red from 85%. The `⏸` marker appears when more than five minutes
passed since the previous render — a long pause means the session went cold, and picking it back up
costs a full context refresh.

```json
{
  "statusLine": {
    "type": "command",
    "command": "bash $CLAUDE_PROJECT_DIR/scripts/statusline-context-warn.sh",
    "padding": 0,
    "refreshInterval": 10
  }
}
```

Three thresholds are environment variables, so a colleague who wants different ones changes nothing
in the script: `CTX_WARN_PCT` (default 60), `CTX_ALARM_PCT` (85), `CTX_IDLE_MIN` (5).

Requirements: `jq`, and `python3` for the first signal. The script needs the executable bit —
`chmod +x scripts/statusline-context-warn.sh`.

## What the numbers mean

`context_window.total_input_tokens` counts everything the last API response carried: the
conversation, cache reads, and cache writes. It is `0` before the first response of a session, so
the status line reads `ctx 0k` for the first turn — that is the field being empty, not a bug.

`context_window_size` is 200000 for most models and 1000000 for the extended-context ones, so the
same percentage means very different absolute room depending on which one you are on.

The two signals answer different questions. Signal 1 asks whether the corpus you ship to every
session is too heavy — that cost is paid whether or not you use it. Signal 2 asks how much room this
particular conversation has left. A repo can pass the first and still fill the window by lunchtime.

Detail on the caps, the ratio, and where a rule belongs → `docs/context-budget.md`.
