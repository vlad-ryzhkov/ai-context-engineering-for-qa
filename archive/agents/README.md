# Why agents were archived

This repository used to ship three subagents — `sdet` (wrote test code),
`auditor` (reviewed quality), and `perf-engineer` (generated load tests) — plus
`agents-checker`, a skill whose only job was validating those three files.

They are archived, not deleted, because the *content* was fine. The *packaging*
was the problem.

## What a subagent buys you, and what it costs

A subagent is a separate AI process with its own context window and its own tool
allowlist. That buys exactly two things:

1. **Context isolation** — the subagent's file reads never enter the main
   conversation, so a large search does not consume the caller's budget.
2. **Tool restriction** — a read-only reviewer genuinely cannot write files.

It costs a full context re-load per invocation: the subagent starts blind, so
everything it needs about the task has to be re-stated in the dispatch prompt.

## Why that trade stopped paying here

Every one of the three agents was doing a job a skill does better:

- **The persona was the payload.** `sdet.md` and `auditor.md` were mostly role
  framing ("you are a senior SDET", verbosity rules, anti-pattern lists). That
  is context, not isolation. A skill delivers context without paying for a
  second process.
- **They duplicated the skills that called them.** Each agent restated rules
  already written in the paired skill, so a change had to land in two files.
  `agents-checker` existed largely to detect exactly that drift — a checker that
  only exists because of avoidable duplication is a smell, not a feature.
- **The `agent:` frontmatter key was inert.** Skills declared `agent: auditor`
  in their YAML frontmatter. Claude Code does not read that key, so it never
  dispatched anything. The pairing was documentation pretending to be wiring.
- **Isolation was not the bottleneck.** These skills produce a report or a set of
  files; none of them needed a hidden context to do it.

## What replaced them

| Archived agent  | Now covered by                                             |
| --------------- | ---------------------------------------------------------- |
| `sdet`          | `/api-tests`, `/api-tests-java`, `/api-mocks`              |
| `auditor`       | `/api-test-review`, `/output-review`, `/skill-audit`, `/doc-lint` |
| `perf-engineer` | `/load-tests`                                              |
| `agents-checker` | Nothing — the drift it detected no longer has two places to drift between |

The QA role framing that lived across all three agents survives in
`.claude/qa_agent.md`, which is a single file every skill can read.

## When a subagent would still be right

Reach for one when you actually want its two properties: a broad codebase sweep
whose file reads must stay out of the main context, or a reviewer that must be
mechanically unable to edit what it reviews. Neither applied here.
