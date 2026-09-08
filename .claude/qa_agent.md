# QA Engineering Role

How the assistant should behave on QA tasks in this project. `CLAUDE.md` owns the
tech stack and communication rules; this file owns the QA judgement.

Full skill catalogue: [`SKILLS.md`](../SKILLS.md) — the single source of truth.
Do not maintain a second skill list here.

## Project Structure

```text
src/
└── test/
├── kotlin/
│   └── {domain_name}/
│       ├── tests/        # Test classes (*Tests.kt)
│       ├── requests/     # HTTP clients + Request/Response models
│       └── helpers/      # Helpers + test data
├── java/
│   └── {domain_name}/
│       ├── tests/        # Test classes (*Tests.java)  [/api-tests-java]
│       ├── requests/     # HTTP clients + DTO models
│       └── helpers/      # Helpers + test data
└── resources/
    └── schemas/          # JSON schemas for response validation
```

> **Mode A (single-module, domain-isolated)** — default for new projects.
> Gradle multi-module projects use Mode B. Both modes and the auto-detection
> algorithm live in `.claude/skills/_shared/api-tests-shared.md` § Architecture Modes.

## Core Mindset

| Principle              | Description                                                            |
| :--------------------- | :--------------------------------------------------------------------- |
| **Zero Hallucination** | Only facts from tools, never fabricate.                                |
| **Fail Fast**          | Blocker at Discovery/Strategy → stop the pipeline.                     |
| **SSOT Reliance**      | `CLAUDE.md` and `audit/test-scenarios.md` — the only sources of truth. |
| **Verifiable Quality** | "Quality" = metric (Coverage %, Pass Rate, Lint Score).                |
| **Evidence over Claim** | A verdict comes from a command's own exit code, never from a guess.   |

## Anti-Patterns (BANNED)

| Pattern (❌)           | Why it's bad                                      | Correct action (✅)                            |
| :--------------------- | :------------------------------------------------ | :--------------------------------------------- |
| **Vague Instructions** | "Test everything" without context.                | Specify exact Scope, Endpoint, and Constraints. |
| **Silent Looping**     | Endlessly retrying on the same error.             | Stop after the 2nd failure, change strategy.    |
| **Ignore Artifacts**   | Ignoring existing `audit/` reports.               | Start with `/repo-scout` and reading reports.   |
| **Self-Certification** | Trusting a skill's own `SKILL COMPLETE` metrics.  | Verify with `/output-review` or a real run.     |

QA anti-patterns for generated code live in `.claude/qa-antipatterns/`. Read
`_index.md` before generating test code.

## Verbosity Protocol

→ Communication rules: see `CLAUDE.md` Communication Protocol.

Text is mandatory only for `🚨 BLOCKER` or `🌱 GARDENER SUGGESTION`.
On success, output only the `✅ SKILL COMPLETE` block.

## Pipeline

```text
/repo-scout → /spec-audit → /api-test-cases | /api-isolated-tests → /api-tests → /api-test-review
```

| Phase            | Skill                                                     | Gate (transition criteria)                        | Output                                                        |
| :--------------- | :-------------------------------------------------------- | :------------------------------------------------ | :------------------------------------------------------------ |
| **1. Discovery** | `/repo-scout` → `/spec-audit`                             | No API/access? → recommend, continue pipeline.    | `audit/repo-scout-report_{ts}.md` + findings                  |
| **2. Execution** | `/api-test-cases` or `/api-isolated-tests` → `/api-tests` | `Compilation PASS` + `@Link` traceability.        | `audit/test-scenarios.md` + `src/test/**`                     |
| **3. Quality**   | `/api-test-review` → `/output-review`                     | Quality Score ≥ 70%. Otherwise → fix (max 1).     | `audit/output-review_{skill}_{date}.md`                       |

### Quality Gates

| Gate               | Criteria                                                                              |
| ------------------ | ------------------------------------------------------------------------------------- |
| Commit (Discovery) | Repo accessible + `/repo-scout` completed + `/spec-audit` no BLOCKER                  |
| PR (Execution)     | ≤3 generation attempts + `BUILD SUCCESS` + review completed                           |
| Release (Quality)  | Artifacts exist on disk + review `✅ PASS` or `🟡 PASS WITH WARNINGS` + final report   |

### Retry Policy

**Compilation FAIL:** fix once, then STOP. On the fix attempt, state an error synopsis:

```text
Error Synopsis (Attempt N):
- Root cause: [specific error / failing class / line number]
- Avoid: [exact pattern that caused the failure]
```

**Review score < 70%:** one iteration of fixes. Repeated fail → escalate to the user
with both positions quoted verbatim.

**FORBIDDEN:** silently looping on fix-retry without progress.

## Ad-Hoc Routing

| User request                               | Skill                                                                       |
| ------------------------------------------ | --------------------------------------------------------------------------- |
| "Analyze the specification / requirements" | `/spec-audit`                                                               |
| "Create a complete list of tests"          | `/api-isolated-tests` (single endpoint) or `/api-test-cases` (bulk)         |
| "Cover all endpoints / full API coverage"  | `/api-test-cases`                                                           |
| "Write tests for /endpoint"                | test-scenarios exist? NO → `/api-isolated-tests`. YES → `/api-tests`        |
| "Write Java tests for /endpoint"           | `/api-tests-java`                                                           |
| "Check screenshot / L10n"                  | `/screenshot-analyze`                                                       |
| "Check quality / do a review"              | `/api-test-review`, `/output-review`, or `/skill-audit`                     |
| "Repository reconnaissance"                | `/repo-scout`                                                               |
| "Full testing cycle"                       | Pipeline: Discovery → Execution → Quality                                   |

## Dynamic Coverage Discovery

Run before `/api-isolated-tests` or `/api-tests` to scope generation.

| Purpose                       | Command                                                                                                 |
| ----------------------------- | ------------------------------------------------------------------------------------------------------- |
| List production Kotlin files  | `find src/main -name "*.kt" \| sort`                                                                    |
| List existing test files      | `find src/test/kotlin -name "*Tests.kt" \| sort`                                                        |
| Find public/suspend functions | `grep -rn "^\s*\(suspend \)\?fun " src/main/kotlin --include="*.kt" \| grep -v "//\|private\|internal"` |
| Find untested classes         | Cross-reference: production files without a `*Tests.kt` counterpart                                     |

Use the results as **Scope** (files to cover), **Existing** (avoid duplicates), and
**Gaps** (no test file yet).

## Repo-Scout Data Flow (§11–§15 → Downstream Skills)

| Report Section                | Consumer Skill                      | How It's Used                                                                                            |
| ----------------------------- | ----------------------------------- | -------------------------------------------------------------------------------------------------------- |
| §11 State Transition Matrix   | `/api-isolated-tests`, `/api-tests` | Generate transition + rejected-transition test cases                                                     |
| §12 Entity & Data Model       | `/api-tests`                        | Create-order chain → setup/teardown order; consistency model → assert strategy (immediate vs Awaitility) |
| §13 Behavioral Nuances        | `/api-isolated-tests`, `/api-tests` | Conditional behavior → parameterized tests; search semantics → edge case scenarios                       |
| §14 Config & Host Context     | `/api-tests`                        | Test env setup → `@BeforeAll`; dead config → skip list                                                   |
| §15 Test Generation Blueprint | `/api-isolated-tests`, `/api-tests` | P0/P1/P2 priorities → generation order; Skip list → `@Disabled` annotations                              |

**Context pruning:** when a skill runs in a forked context, pass only the report
sections relevant to the target module or endpoint — minimum §15 Blueprint plus
§11–§13 scoped to the target domain.

## Meta-Learning

The Gardener protocol (`.claude/protocols/gardener.md`) runs before the
`SKILL COMPLETE` block: if a run surfaced a missing rule, propose it.

The wider self-improvement loop — Reflection, Reflector, and the lesson-curation
pipeline — is an unfinished experiment kept in `in-progress/ace/`. It is not part
of the maintained setup.

## Markdown Artifact Quality Rules

All skills that generate `.md` artifacts MUST follow these rules:

- **MD040:** Every fenced code block MUST have a language tag (`json`, `text`, `bash`, etc.)
- **MD056:** Pipe `|` inside a table cell MUST be escaped as `\|`

## Skill Completion Protocol

Each skill ends with one of the following blocks:

```text
✅ SKILL COMPLETE: /{skill-name}
├─ Artifacts: [list]
├─ Compilation: [PASS/FAIL/N/A]
├─ Upstream: [file path | N/A]
└─ Coverage: [X/Y]
```

```text
⚠️ SKILL PARTIAL: /{skill-name}
├─ Artifacts: [list (✅/❌)]
├─ Compilation: [PARTIAL (X/Y files)]
├─ Upstream: [file path | N/A]
├─ Coverage: [X/Y]
└─ Blockers: [description]
```
