# AGENTS.md

Entry point for assistants that read `AGENTS.md` rather than `CLAUDE.md`
(OpenAI Codex and others following the same convention).

## Read these, in order

1. **[`CLAUDE.md`](CLAUDE.md)** — the single source of truth for tech stack,
   commands, safety protocols, and the communication protocol. If it conflicts
   with anything else, `CLAUDE.md` wins.
2. **[`.claude/qa_agent.md`](.claude/qa_agent.md)** — the QA role: pipeline,
   quality gates, retry policy, anti-patterns.
3. **[`SKILLS.md`](SKILLS.md)** — every available skill, grouped, with status.
   This is the only skill list in the repository; do not expect another.

## Invoking a skill

Skills are plain markdown at `.claude/skills/<name>/SKILL.md`. If your tool has
no native skill mechanism, read the file and follow it as instructions. See
[`docs/adapting-to-other-tools.md`](docs/adapting-to-other-tools.md).

## Non-negotiable

- Do not generate code that violates the locked dependencies in `CLAUDE.md`.
- All documentation and skill content is written in English.
- Anti-patterns for generated test code live in `.claude/qa-antipatterns/`; read
  `_index.md` before generating tests.
