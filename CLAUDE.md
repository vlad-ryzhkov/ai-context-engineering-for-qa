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

| Component | Technology | BANNED |
|-----------|------------|--------|
| HTTP Client | ktor-client (CIO) + ktor-serialization-jackson | Custom HTTP wrappers, retrofit |
| Serialization | Jackson (SNAKE_CASE) + jackson-module-kotlin | Gson, Moshi |
| Assertions | Kotest assertions-core | Assertions without message |
| Async/Coroutines | kotlinx-coroutines-test | `Thread.sleep()`, `delay()` in tests |
| Test Framework | JUnit 5 | TestNG |
| Reporting | Allure | — |

## Project Structure

```text
src/
└── test/
    ├── kotlin/
    │   └── registration/
    │       ├── tests/        # Test classes (*Tests.kt)
    │       ├── requests/     # HTTP clients + Request/Response models
    │       └── helpers/      # Helpers + test data
    └── resources/
        ├── schemas/          # JSON schemas for response validation
        └── screenshots/      # Screenshots for L10N tests
```

## Commands

| Action | Command |
|--------|---------|
| Build | `./gradlew build` |
| Test | `./gradlew test` |
| Single test | `./gradlew test --tests "FullClassName"` |
| Clean | `./gradlew clean` |

## Core Principles

1. **Trust No One** — verify requirements for contradictions
2. **Production Ready** — code compiles without modifications
3. **Safety** — destructive commands only with confirmation

## Safety Protocols

⛔ **FORBIDDEN:** `git reset --hard`, `git clean -fd`, branch deletion, `rm -rf .git`
✅ **MANDATORY:** Backup before destructive operations
⚠️ **OVERRIDE:** Requires the word **DESTROY** from the user

## Token Economy

- PAUSE on tasks > 20,000 tokens
- Full scan only with keyword **FULL_SCAN**

## Workflow

For tasks > 3 files: Analysis → Plan → Execute → Verify

**Loop Guard:**
- FORBIDDEN to repeat the same action more than 3 times without progress
- After 3 unsuccessful attempts → Output exactly "🛑 LOOP_GUARD_TRIGGERED: [Reason]" and immediately PAUSE execution to wait for user input
- Examples: fix-retry lint/compilation, re-running the same command, searching for a file with the same pattern

## Git Workflow

**Main branch:** `main` — all PRs target `main`.

Before pushing to any branch, explicitly confirm the target branch name with the user. Never assume `main` vs `master` or feature branch names.

## Editing Conventions

When asked to shorten, simplify, or trim output/content — remove only what is explicitly requested. Never remove safety protocols or customization prompts unless explicitly stated.

## Universal Protocols

> Applies to ALL agents and skills. Overrides default behavior.

**Output:** Always finish with the completion block:
```text
✅ SKILL COMPLETE: /{skill-name}
├─ Artifacts: [list]
├─ Compilation: [PASS/FAIL/N/A]
├─ Upstream: [file path | N/A]
└─ Coverage: [X/Y]
```

## QA Skills

**Agent context:** `.claude/qa_agent.md` — for core testing and orchestration skills (`/spec-audit`, `/test-cases`, `/api-tests`) you MUST read this file before proceeding.

| Skill | Purpose |
|-------|---------|
| `/repo-scout` | Repository scanning |
| `/spec-audit` | QA audit of requirements |
| `/test-cases` | Test cases from specification |
| `/api-tests` | API automated tests (Kotlin) |
| `/screenshot-analyze` | Screenshot analysis for L10N defects |
| `/doc-lint` | Documentation audit |
| `/skill-audit` | SKILL.md files audit |
| `/output-review` | Skill output audit |
| `/agents-checker` | Agent setup validation |
| `/init-skill` | New skill creation |
| `/init-agent` | qa_agent.md creation |
| `/init-project` | Project CLAUDE.md initialization |
| `/update-ai-setup` | AI setup registry update |
| `/qa-translate` | Technical translation RU→EN |

**Workflow:** `/repo-scout` → `/spec-audit` → `/test-cases` → `/api-tests`

**Structure:** `.claude/` → `qa_agent.md`, `agents/`, `skills/`, `qa-antipatterns/`, `references/`
