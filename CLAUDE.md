# AI QA Workshop Environment

## Context

- **Project:** QA Automation Workshop
- **Role:** Senior QA Automation Engineer
- **Language:** Kotlin, Markdown

## Communication Protocol (STRICT)

- **CLI mode, not chat:** You are a CLI utility. Your goal is execution, not conversation.
- **No preamble:** FORBIDDEN to write "Great", "Got it", "Sure", "Let me look".
- **No announcements:** MUST NOT write "I'll now read the file..." or "I'll execute the command...". Invoke the tool immediately.
- **Tool-First:** Action first (Bash, Read, Edit), comments only AFTER output, if analysis is needed.
- **Concise output:** If the action succeeded and is clear from context — output nothing or use 1 line of output.

## General Conventions

- All documentation, test reports, and skill content for this project MUST be written in English.
- When performing mathematical calculations (coverage percentages, statistics, counts) show the full formula with numerator and denominator before the result. Verify denominators — count ALL elements, not just a subset.

## Tech Stack (LOCKED)

| Component                  | Technology                                     | BANNED                               |
| -------------------------- | ---------------------------------------------- | ------------------------------------ |
| HTTP Client                | ktor-client (CIO) + ktor-serialization-jackson | Custom HTTP wrappers, retrofit       |
| Serialization              | Jackson (SNAKE_CASE) + jackson-module-kotlin   | Gson, Moshi                          |
| Assertions                 | Kotest assertions-core                         | Assertions without message           |
| Async/Coroutines           | kotlinx-coroutines-test                        | `Thread.sleep()`, `delay()` in tests |
| Test Framework             | JUnit 5                                        | TestNG                               |
| Reporting                  | Allure                                         | —                                    |
| Environment / Mocks        | Testcontainers (PostgreSQL/Redis) + WireMock   | H2 in-memory DB (unless specified)   |
| HTTP Client (Java, opt-in) | `java.net.http.HttpClient` (JDK 17 built-in)   | RestAssured, OkHttp, Retrofit        |
| Assertions (Java, opt-in)  | AssertJ (`assertThat(...).as("msg")`)          | Assertions without `.as()` message   |

## Commands

| Action      | Command                                  |
| ----------- | ---------------------------------------- |
| Build       | `./gradlew build`                        |
| Test        | `./gradlew test`                         |
| Single test | `./gradlew test --tests "FullClassName"` |
| Clean       | `./gradlew clean`                        |

## Secrets — Never Commit

Forbidden patterns: see `.gitignore` (Security section).
Enforcement: `scripts/pre-commit.sh` (blocks commit), `scripts/pre-push.sh` (blocks push).

Setup hooks once: `bash scripts/setup-hooks.sh`

If a secret was already committed → **rotate immediately**, then remove from history:
`git filter-repo --path-glob '*.env' --invert-paths`

## Loop Guard

- FORBIDDEN to repeat the same action more than 3 times without progress
- After 3 unsuccessful attempts → Output exactly "🛑 LOOP_GUARD_TRIGGERED: [Reason]" and immediately PAUSE execution to wait for user input
- Examples: fix-retry lint/compilation, re-running the same command, searching for a file with the same pattern

## Git Workflow

**Main branch:** `main` — all PRs target `main`.

Before pushing to any branch, explicitly confirm the target branch name with the user. Never assume `main` vs `master` or feature branch names.

## Editing Conventions

When asked to shorten, simplify, or trim output/content — remove only what is explicitly requested. Never remove safety protocols or customization prompts unless explicitly stated.

**Agent context:** `.claude/qa_agent.md` — for core testing and orchestration skills, read this file before proceeding. Contains output format, skill completion protocol, and workflow pipeline.

**Delta Update Protocol:** Context files evolve via surgical `Edit`, never full `Write` overwrites. `delta-guard.sh` hook warns on violations.

# Eval fixtures — anti-leakage

When authoring or auditing fixture files for AI/LLM eval suite (`tests/<skill>/evals/files/` or `skills/<skill>/evals/files/`):

## Filename rule — STRICT

Fixture filenames must NOT telegraph expected outcome. Skill must detect defects from file CONTENT alone, never from name.

Forbidden in fixture names:

- `bad_*` / `*_bad` / `good_*` / `*_good`
- `clean_*` / `*_clean` / `dirty_*` / `*_dirty`
- `no_*` (`no_time_filter`, `no_index`, `no_auth`, etc.)
- `*_violation` / `*_violates` / `*_correct` / `*_wrong`
- `*_fail` / `*_pass` / `_should_fail` / `_should_pass`
- `legitimate_*` / `intentional_*` (inverse hints — also forbidden)

Use realistic production-style names: `payment_gateway.yaml`, `user_repository.go`, `zone_config.sql`, `order_handler.py`. Paired fixtures must use TWO DIFFERENT base names — `payment_gateway.yaml` + `auth_service.yaml`, not `payment_gateway_v1.yaml` + `payment_gateway_v2.yaml` (v1/v2 re-introduces ordering hint).

## Content rule — STRICT

Fixture CONTENT must NOT contain header comments telegraphing test intent. Forbidden patterns:

- `// BAD: missing rate limit`
- `-- This violates rule 7`
- `// CO-TRIGGER fixture: plants violations A/B/C`
- `# Note: this file is a clean baseline`
- `// false_positive_trap fixture for <skill>`
- `// A correct version would open with...`
- `// the point of the fixture is the gomock.Eq misuse`

Fixture must read like real production code. Defects checked via `expectations` array in `evals.json`, not announced by comments.

## Why

Skill reading `user_metric_bad.sql` knows it's supposed to find defects before reading content. Phase A (without skill) and Phase B (with skill) become indistinguishable — benchmark measures filename-pattern recognition, not skill value.

## When auditing fixtures

```bash
# Filename leakage scan
ls <fixtures-dir> | grep -E '(^|_)(bad|good|clean|dirty|no|legitimate|intentional|wrong|correct|fail|pass)(_|\.|$)'

# Content leakage scan
grep -rEn '(BAD:|GOOD:|VIOLATES:|This violates|FIXME-eval|expected:|skill must (NOT )?flag|the point of the fixture is)' <fixtures-dir>
```

Flag matches as blockers, not nits.

# Skill Benchmarking — Run Economy

## Reuse baseline (without-skill) runs

Never re-run baseline if one exists in workspace for same spec.
Check `api-spec-audit-workspace/iteration-N/*/without_skill/` (or equivalent) before launching without-skill agents.
Found → reuse. Baselines don't depend on skill version.

## Reuse with-skill runs when skill hasn't changed

If skill + model unchanged since recent run on same spec, ask user whether to reuse existing with-skill output.
Reuse when:

- SKILL.md + referenced files unchanged since previous run
- Model same
- Spec unchanged

Confirm reuse plan with user before launching — they may want fresh run even if nothing changed.

## Default launch scheme

- New iteration after skill change → with-skill only; reuse prior baselines
- Fresh skill on new specs → run both once; store for future reuse
- Investigating regression → full re-run (both configs) to rule out flakiness

## Workflow

1. Check workspace for existing without-skill outputs for target specs
2. Check if skill files changed since last with-skill run (`git log --oneline skills/<skill-name>/`)
3. Propose minimal run set to user, wait for confirmation
4. Launch only agreed agents

# Multi-branch & multi-step task — temporary PROGRESS.md

## When this rule applies

Task spans:

- More than 2 branches, OR
- More than 5 sequential commits across one or more branches, OR
- Plan with explicit "Этап / Phase / Step N" structure, OR
- Same file touched on multiple branches in one session.

## What to do

Create temporary `PROGRESS.md` at START of task, before first edit.
Path: repo root or workspace dir (e.g. `<task>-workspace/PROGRESS.md`).
Add to `.gitignore` OR keep untracked (`echo PROGRESS.md >> .git/info/exclude`).

Schema (use exactly):

```markdown
# PROGRESS — <ticket / plan name>

Source of truth for branch ownership of shared files (decided up front):

- docs/EVAL_RUNBOOK.md → harden branch
- docs/skill-creation-guide.md → harden branch
- ...

## Step ledger

| Step | Branch                    | Action         | File(s)                        | Commit SHA | Status  |
| ---- | ------------------------- | -------------- | ------------------------------ | ---------- | ------- |
| 1.1  | feat/CORE-1816-...-evals  | rebase on main | —                              | a6eeea7    | done    |
| 2.3  | feat/CORE-1816-...-harden | edit + commit  | docs/EVAL_RUNBOOK.md           | 7d659bd    | done    |
| 3.1  | feat/CORE-1816-...-harden | git mv         | skills/.../evals.json → tests/ | f65cd66    | done    |
| 4.2  | feat/CORE-1816-...-evals  | edit + commit  | tests/.../evals.json (Eval 4)  | f21d4ae    | done    |
| ...  | ...                       | ...            | ...                            | ...        | pending |
```

## When to update ledger

- IMMEDIATELY after every commit — write SHA + branch name.
- IMMEDIATELY after every `git checkout <branch>` — log switch as own row.
- IMMEDIATELY after destructive op (revert, reset, rm) — log it.
- IMMEDIATELY after scope change — add row, update source-of-truth section.

Ledger is your memory. Skip a row = misattribute later edits.

## Cross-checks before any edit

Before EVERY file edit, write, mv, or rm:

1. Read last 3 ledger rows.
2. Run `git rev-parse --abbrev-ref HEAD`.
3. Confirm branch matches source-of-truth for file. If not — STOP, switch, log, then edit.

## When to delete PROGRESS.md

After user confirms `git push` done.

## Connection to plan files

Plan file = WHAT to do. PROGRESS.md = WHAT YOU HAVE DONE. Not the same. Don't conflate.
