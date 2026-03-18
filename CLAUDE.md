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
