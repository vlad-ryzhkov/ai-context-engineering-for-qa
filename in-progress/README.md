# in-progress

Unfinished experiments. Nothing here is wired into the maintained setup: no skill
in `.claude/skills/` depends on it, no hook runs it, and `SKILLS.md` marks it
`experimental`.

It lives in the repository because the idea is worth keeping and the write-up is
worth reading — not because it works.

| Experiment | Status | What it was trying to do |
| ---------- | ------ | ------------------------ |
| [`ace/`](ace/README.md) | Parked | Make the AI's own instruction files improve themselves from run to run |

## Rules for this folder

- Nothing in `in-progress/` may be referenced from `.claude/`, `CLAUDE.md`, or a
  hook. If an experiment graduates, move it out first, then wire it in.
- An experiment that is abandoned rather than parked gets deleted, not left here.
