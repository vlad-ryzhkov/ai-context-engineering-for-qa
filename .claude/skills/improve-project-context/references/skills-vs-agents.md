# Skills vs Agent Subagents — Decision Guide (May 2026)

Prefer skills over `.claude/agents/*.md` subagent definitions in almost all cases.

## Decision Table

| Situation                                                        | Recommendation                                            | Why                                                                                                    |
| ---------------------------------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Specialized workflow (workflow-modify, verify-docs, test-writer) | **Skill**                                                 | Invocable, self-contained, discoverable, testable                                                      |
| Role with persistent style/persona (SDET, Auditor)               | **Skill** — embed persona in the skill's identity section | Agent definitions in `.claude/agents/` are invisible at session start; model must be told to load them |
| One-shot analysis task                                           | **Skill**                                                 | Simpler, no dispatch overhead                                                                          |
| True parallelism: 2+ fully independent tasks simultaneously      | **Agent dispatch** via `Agent` tool inside a skill        | Only real use case for subagents                                                                       |
| Orchestrator coordinating 3+ specialized roles                   | **Skill** that calls `Agent` tool internally              | Skill provides the orchestration algorithm; agents do leaf work                                        |

## Why Agent Definitions Are Problematic

`.claude/agents/` files are NOT loaded at session start — the model has no
knowledge they exist unless something explicitly says "Read .claude/agents/sdet.md".
This means:

- An agent defined in `.claude/agents/sdet.md` is effectively invisible
  unless the entry point (CLAUDE.md or a skill) explicitly references it
- Skills are listed in the system prompt automatically and appear in the
  model's available-skills list
- Skills have `allowed-tools`, versioning, `references/`, `bin/` — agents
  have none of this structure

## Migration Pattern

If a repo has `.claude/agents/sdet.md`:

1. Create `.claude/skills/sdet/SKILL.md` with the persona embedded in the skill
2. Update the entry point (CLAUDE.md/agents.md) to reference the skill by name
3. Either delete the old agent file or keep it as a legacy reference
   (but stop routing through it)
