# Contributing

## Prerequisites

- Node.js 20+
- Bash 4+
- Git hooks: `bash scripts/setup-hooks.sh`

## Creating a Skill

1. Run `/init-skill` — follows the skill template and checklist
2. Validate: `bash scripts/skill-quality.sh --skill <name>`
3. Run `/skill-audit` for a deep audit
4. Update baseline: `bash scripts/skill-quality.sh --snapshot`

## Quality Requirements

All skills must pass:

| Check            | Tool                                                | Threshold                    |
|------------------|-----------------------------------------------------|------------------------------|
| AI config lint   | `npx agnix --target claude-code .`                  | Zero errors                  |
| Tier 1 Baseline  | `bash scripts/skill-quality.sh --check structure`   | Zero errors                  |
| Line count       | Tier 1 check S8                                     | ≤500 lines (warn >400)      |
| Token budget     | `bash scripts/skill-quality.sh --check budget`      | No HIGH status               |
| Regression       | `bash scripts/skill-quality.sh --check regression`  | No required section removed  |

### Tier 1 Baseline Sections (required in every SKILL.md)

- **Quality Gate / Self-Review** — inline checklist before output
- **Gardener** — reference to `.claude/protocols/gardener.md`
- **SILENT MODE / Verbosity** — token economy compliance
- **SKILL COMPLETE / Completion** — structured completion block

## Running Quality Checks

```bash
# Full pipeline (agnix + structure + budget)
bash scripts/skill-quality.sh

# Single skill
bash scripts/skill-quality.sh --skill api-tests

# CI mode (agnix + structure + regression + diff)
bash scripts/skill-quality.sh --ci

# Update baseline after approved changes
bash scripts/skill-quality.sh --snapshot

# Compare against baseline
bash scripts/skill-quality.sh --diff
```

## PR Checklist

Before opening a PR that touches `.claude/`:

- [ ] `npx agnix --target claude-code .` passes
- [ ] `bash scripts/skill-quality.sh --check structure` — zero errors
- [ ] `bash scripts/skill-quality.sh --check regression` — no regressions
- [ ] Baseline updated if skill structure changed: `bash scripts/skill-quality.sh --snapshot`
- [ ] SKILL.md ≤500 lines; overflow moved to `references/`

## Git Hooks

| Hook                     | What it checks                                                   |
|--------------------------|------------------------------------------------------------------|
| `pre-commit`             | Forbidden files, secret patterns, staged SKILL.md structure      |
| `pre-push`               | Branch naming, forbidden files, Kotlin compilation, markdownlint |
| `skill-lint` (post-edit) | Line count, Tier 1 Baseline sections, forbidden patterns         |

Setup: `bash scripts/setup-hooks.sh`

## Anti-patterns

All QA anti-patterns live in `.claude/qa-antipatterns/`. The index at `_index.md` must list every pattern file. Check S17 validates this.

To add a new anti-pattern:
1. Create `.claude/qa-antipatterns/{category}/{problem-name}.md`
2. Add entry to `_index.md`
3. Run `bash scripts/skill-quality.sh --check structure` to verify S17
