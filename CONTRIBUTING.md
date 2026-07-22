# Contributing

## Prerequisites

- Node.js 20+
- Bash 4+
- Git hooks: `bash scripts/setup-hooks.sh`

## Creating a Skill

1. Run `/init-skill` — follows the skill template and checklist
2. Validate: `npx agnix --target claude-code .`
3. Run `/skill-audit` for a deep audit

## Quality Requirements

All skills must pass:

| Check          | Tool                               | Threshold              |
| -------------- | ---------------------------------- | ---------------------- |
| AI config lint | `npx agnix --target claude-code .` | Zero errors            |
| Harness audit  | `npx vigiles lint`                 | Exit 0 (CI-gated)      |
| Line count     | `/skill-audit` check               | ≤500 lines (warn >400) |

`agnix` checks skill structure/format; `vigiles lint` checks harness truthfulness — that every path, tool contract, hook, and skill reference actually resolves. Both run in CI.

### Tier 1 Baseline Sections (required in every SKILL.md)

- **Quality Gate / Self-Review** — inline checklist before output
- **Gardener** — reference to `.claude/protocols/gardener.md`
- **SILENT MODE / Verbosity** — token economy compliance
- **SKILL COMPLETE / Completion** — structured completion block

## Running Quality Checks

```bash
# Lint all AI config files
npx agnix --target claude-code .

# Strict mode (warnings become errors)
npx agnix --target claude-code --strict .

# Auto-fix where possible
npx agnix --target claude-code --fix .
```

## PR Checklist

Before opening a PR that touches `.claude/`:

- [ ] `npx agnix --target claude-code .` passes
- [ ] `npx vigiles lint` — exit 0
- [ ] `/skill-audit` — zero errors
- [ ] SKILL.md ≤500 lines; overflow moved to `references/`

## Git Hooks

| Hook                     | What it checks                                                   |
| ------------------------ | ---------------------------------------------------------------- |
| `pre-commit`             | Forbidden files, secret patterns, staged SKILL.md structure      |
| `pre-push`               | Branch naming, forbidden files, Kotlin compilation, markdownlint |
| `skill-lint` (post-edit) | Line count, Tier 1 Baseline sections, forbidden patterns         |

Setup: `bash scripts/setup-hooks.sh`

## Anti-patterns

All QA anti-patterns live in `.claude/qa-antipatterns/`. The index at `_index.md` must list every pattern file. Check S17 validates this.

To add a new anti-pattern:

1. Create `.claude/qa-antipatterns/{category}/{problem-name}.md`
2. Add entry to `_index.md`
3. Run `npx agnix --target claude-code .` to verify
