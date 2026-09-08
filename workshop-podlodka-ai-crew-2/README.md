# Workshop — Podlodka AI Crew #2

Materials from the "AI Context Engineering for QA" workshop, February 2026.
Kept as a record of the talk. The skills themselves have moved on since — for the
current catalogue see [`SKILLS.md`](../SKILLS.md) in the repository root.

## Contents

| File | What it is |
| ---- | ---------- |
| [`presentation/Workshop_AI_for_QA.pdf`](presentation/Workshop_AI_for_QA.pdf) | The slide deck as presented |
| [`presentation/context-pyramid.png`](presentation/context-pyramid.png) | The context pyramid diagram — the deck's central model |
| [`workshop-commands.md`](workshop-commands.md) | Per-IDE prompts used to run each skill live |
| [`example-test-scenarios.md`](example-test-scenarios.md) | Real `/api-test-cases` output generated during the session |
| [`model-comparison-notes.md`](model-comparison-notes.md) | Raw notes from the with-context vs. without-context demo |
| [`rtl-example/rtl.png`](rtl-example/rtl.png) | Right-to-left layout defect used in the `/screenshot-analyze` demo |

## Watching it back

- [Demo video](https://youtu.be/7VnjM44qkmc) — the capability walkthrough

## Caveat on `workshop-commands.md`

It documents how to invoke these skills from Cursor, Copilot, Codex, and JetBrains
AI via the per-IDE wrapper files that shipped at the time. Those wrappers have
since been removed as duplication — see
[`docs/adapting-to-other-tools.md`](../docs/adapting-to-other-tools.md) for the
current approach. The Claude Code commands in it still work as written.
