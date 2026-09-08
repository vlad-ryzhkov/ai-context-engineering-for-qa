# Skills — the single source of truth

Every skill in this repository, its status, and what it is for. Nothing else in
the repo maintains a second list: if a skill is not here, it does not exist.

Skills live flat at `.claude/skills/<name>/SKILL.md`. **Claude Code does not scan
category subdirectories** — `.claude/skills/api-testing/api-tests/SKILL.md` would
never be discovered — so the grouping below is editorial, not a directory layout.

## Status vocabulary

| Status | Meaning |
| ------ | ------- |
| **active** | Maintained. Use it. |
| **private successor** | Published version still works, but the author now runs an improved non-public version. Expect the public one to lag. |
| **experimental** | Parked in `in-progress/`. Not wired into the setup, not discoverable as a skill. |
| **archived** | Removed from the active setup. Kept under `archive/` or in git history only. |

## API & test generation

The core pipeline. Each skill consumes the previous one's artifact.

```text
/repo-scout → /spec-audit → /api-test-cases → /api-tests → /api-test-review
```

| Skill | Status | Purpose |
| ----- | ------ | ------- |
| [`/repo-scout`](.claude/skills/repo-scout/SKILL.md) | active | Scan a backend repo (Go, Python, Node.js, Java/Kotlin); catalogue API surface, infrastructure, and coverage gaps |
| [`/spec-audit`](.claude/skills/spec-audit/SKILL.md) | active | Deep QA audit of a specification against ISTQB, BABOK, and OWASP; finds contradictions before tests are written |
| [`/api-test-cases`](.claude/skills/api-test-cases/SKILL.md) | active | Exhaustive test scenario matrix for **all** endpoints, grouped by domain |
| [`/api-isolated-tests`](.claude/skills/api-isolated-tests/SKILL.md) | active | Same, for **one** endpoint in depth — steps, data, expected results |
| [`/api-tests`](.claude/skills/api-tests/SKILL.md) | active | Kotlin test code (JUnit 5, ktor-client, Kotest, Allure) |
| [`/api-tests-java`](.claude/skills/api-tests-java/SKILL.md) | active | Java 17+ test code (JUnit 5, `java.net.http`, AssertJ, Allure) |
| [`/api-test-review`](.claude/skills/api-test-review/SKILL.md) | active | Deep code review of generated tests: security, architecture, quality |
| [`/api-mocks`](.claude/skills/api-mocks/SKILL.md) | active | In-process HTTP mock server plus WireMock singletons, from the spec |

Shared logic for the two `api-tests` variants lives in
[`.claude/skills/_shared/`](.claude/skills/_shared/) — architecture modes,
input validation, and the coverage matrix format.

## Repository & AI setup

Bootstrapping a project so an assistant knows how to work in it.

| Skill | Status | Purpose |
| ----- | ------ | ------- |
| [`/init-project`](.claude/skills/init-project/SKILL.md) | active | Generate a project `CLAUDE.md` — tech stack, commands, banned alternatives |
| [`/init-agent`](.claude/skills/init-agent/SKILL.md) | active | Generate `qa_agent.md` — the QA role, principles, and anti-patterns |
| [`/init-skill`](.claude/skills/init-skill/SKILL.md) | private successor | Scaffold a new skill with checkpoints and iterative refinement |

## Context engineering

Auditing and improving the instruction corpus itself — the meta layer.

| Skill | Status | Purpose |
| ----- | ------ | ------- |
| [`/improve-project-context`](.claude/skills/improve-project-context/SKILL.md) | active | Cold topology audit: dependency graph from `CLAUDE.md`, always-loaded token budget, orphans, duplication, ambiguous routing |
| [`/improve-context-from-sessions`](.claude/skills/improve-context-from-sessions/SKILL.md) | active | Warm retrospective: mine the live dialog for corrections and missing rules, propose surgical deltas |
| [`/skill-audit`](.claude/skills/skill-audit/SKILL.md) | private successor | Audit `SKILL.md` files for bloat, duplication, and harmful patterns |

`/improve-project-context` is static and `/improve-context-from-sessions` is
behavioural — they are complements, not alternatives. A rule the model keeps
violating passes a cold audit cleanly.

Budget numbers live in [`docs/context-budget.md`](docs/context-budget.md); the
measurement itself is [`scripts/context-check.py`](scripts/context-check.py).

## Documentation & review

| Skill | Status | Purpose |
| ----- | ------ | ------- |
| [`/doc-lint`](.claude/skills/doc-lint/SKILL.md) | active | Documentation quality audit — size, structure, cross-file duplication, single-source violations |
| [`/output-review`](.claude/skills/output-review/SKILL.md) | active | Independent audit of any skill's output against that skill's own checklist |
| [`/qa-translate`](.claude/skills/qa-translate/SKILL.md) | active | QA-grade technical translation, Russian to English, preserving markdown structure |
| [`/fix-markdown`](.claude/skills/fix-markdown/SKILL.md) | active | Fix markdownlint errors across the repo |
| [`/screenshot-analyze`](.claude/skills/screenshot-analyze/SKILL.md) | active | Mobile screenshots for localisation defects — translations, CLDR formats, right-to-left |

`/output-review` exists because a skill's own `SKILL COMPLETE` metrics are
self-certification. Do not treat them as a verdict.

## Code & CI review

| Skill | Status | Purpose |
| ----- | ------ | ------- |
| [`/bash-reviewer`](.claude/skills/bash-reviewer/SKILL.md) | active | Shell scripts for security, portability, and robustness anti-patterns; also DRY/KISS/SOLID |
| [`/workflow-expert`](.claude/skills/workflow-expert/SKILL.md) | active | GitHub Actions workflows — audit, fix, secure, optimise |
| [`/pr`](.claude/skills/pr/SKILL.md) | active | Open a pull request with a conventional-commit title |

Run ShellCheck before `/bash-reviewer`; the skill covers what ShellCheck cannot see.

## Experimental

| Skill | Status | Purpose |
| ----- | ------ | ------- |
| `/curate-lessons` | experimental | Deduplicate pending lessons and graduate them into context files. Part of the parked ACE loop — see [`in-progress/ace/`](in-progress/ace/README.md) |

## Archived

| Skill | Removed | Why |
| ----- | ------- | --- |
| `/agents-checker` | [`archive/agents/`](archive/agents/README.md) | Validated the three subagent files. Those are archived, so it has nothing to check |
| `/update-ai-setup` | [`archive/superseded/`](archive/superseded/) | Its only job was syncing `docs/ai-setup.md`, a generated inventory now replaced by this hand-maintained file. Drift detection is `/improve-project-context`'s job, and half its logic checked the per-IDE wrappers that were removed |
| `/load-tests` | git history | Encoded an internal load-testing harness contract (private utility classes, a reporting config schema) that does not belong in a public repository |

## What "private successor" means here

Three skills — `/init-skill`, `/skill-audit`, and the Multi-Session half of
`/improve-context-from-sessions` — have non-public versions the author actually
uses day to day. The published copies are real and working, not stubs, but they
are a snapshot: fixes land in the private version first and are back-ported
selectively, or not at all.

Where the private version is better and why:

| Public skill | What the private version adds |
| ------------ | ----------------------------- |
| `/init-skill` | Eval-backed scaffolding — a generated skill ships with a test suite that scores it, so "does this skill work" is measured instead of reviewed |
| `/skill-audit` | Scores against a graded rubric with a pass threshold, rather than emitting an unweighted findings list |
| `/improve-context-from-sessions` | A Multi-Session Mode that mines session archives for recurring tool-call clusters. Its scanner is a colleague's work in a private repository, so it is not republishable |

If you need the audited-and-scored variants, the public analogues worth reaching
for are [`anthropics/skills`](https://github.com/anthropics/skills) for
`skill-creator`, and [`vigiles`](https://github.com/zernie/vigiles) for grading a
harness — see [Related projects](README.md#related-projects).

## Adding a skill

See [`CONTRIBUTING.md`](CONTRIBUTING.md). A new skill must be added to this file
in the same change that creates it — a skill missing from this table is a defect,
and `/improve-project-context` will flag it as an orphan.
