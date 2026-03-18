# AI Context Engineering for QA

Stop writing ad-hoc prompts. Start using engineered AI skills for QA.

This repository is a ready-to-use library of **21 AI skills** (reusable prompts that tell the AI exactly how to perform a QA task),
**31 anti-pattern quality gates** (rules the AI checks its own output against before finishing),
and **3 specialized agents** (AI roles — one writes test code, one reviews it, one generates load tests) designed specifically for QA workflows.
Copy the `.claude/` folder into your project, and your AI assistant immediately knows how to audit specs,
generate test cases, write API tests (Kotlin/Java), and check its own output.

Works with Claude Code, OpenCode, Cursor, VS Code Copilot, Codex, JetBrains AI, and Gemini Code Assist.

<p align="center">
  <img src="presentation/context-pyramid.png" alt="Context Pyramid" width="300"/>
</p>

---

## Why does this exist?

AI coding assistants can write your API tests, audit your specs, and review your code — but only if you give them
the right instructions. Without context, the AI starts from zero every conversation: it picks random libraries,
ignores your coding standards, and produces output you have to rewrite. This repository contains field-tested
instructions that make the AI behave like a senior QA engineer who already knows your project.

---

## Quick Start

You don't need to read the whole repo. Three steps to start getting value:

1. **Copy** — Copy the `.claude/` folder from this repo into your backend or QA project root.
   For non-Claude IDEs (Cursor, Copilot, etc.), see [IDE Compatibility](#ide-compatibility) below.

2. **Create CLAUDE.md by hand** — Write a minimal `CLAUDE.md` in your project root with only:
   tech stack, build/test commands, and banned alternatives.
   See [docs/claudemd-instructions.md](docs/claudemd-instructions.md) for guidelines.
   Research shows that hand-written context files outperform AI-generated ones (+4% vs −3% success rate).

3. **Run your first skill** — Try scanning a backend repo:
   ```text
   /repo-scout
   ```
   The AI reads your codebase and produces a structured report: API endpoints found, tech stack detected,
   test coverage gaps, and a blueprint for test generation.

> **New to AI coding assistants?** Start with the [`spec-only` branch](../../tree/spec-only) — it contains
> only API specifications with no pre-generated code, so you can follow the full pipeline from scratch.
> See [Demo Video](https://youtu.be/7VnjM44qkmc) for a walkthrough.

---

## Core QA Workflow

The main pipeline — choose your starting point based on scope:

```text
/repo-scout       →  /api-test-cases     →  /api-tests      →  /api-test-review
(backend repo)       (test scenarios)       (QA test repo)     (code review)
```

| Skill              | Input                 | Output                                        | What you get                                         |
| ------------------ | --------------------- | --------------------------------------------- | ---------------------------------------------------- |
| `/repo-scout`      | Backend repository    | API surface map, coverage gaps                | Catalog of endpoints, infrastructure, entry points   |
| `/api-test-cases`  | Specification + audit | Test scenario matrix (Markdown)               | Exhaustive test cases for all endpoints, by priority |
| `/api-tests`       | Test scenarios + spec | Kotlin test code (JUnit 5, Allure)            | Production-ready tests, run with `./gradlew test`    |
| `/api-tests-java`  | Test scenarios + spec | Java 17+ test code (JUnit 5, Allure, AssertJ) | Same as above, Java teams opt-in                     |
| `/api-test-review` | Test code + spec      | Review report (Markdown)                      | Deep code review: security, architecture, quality    |

> While `/api-tests` provides good coverage out of the box, it is designed to be adapted to your team's architectural guidelines.
>
> **Optional:** `/api-isolated-tests` — generates detailed test scenarios for a single endpoint (steps, data, expected results). Use when you need a deep-dive into one area instead of full API coverage.

### See it in action

Here is what real skill output looks like (truncated). Each example links to the full artifact in this repo.

**`/spec-audit` — finds contradictions in API specifications:**

```text
Spec Audit — Registration API v1
Verdict: BLOCKED | Score: 0%

Top 3 Risks:
1. [BLOCKER] Spec declares 2FA via SMS but phone field is absent from Request Body
2. [CRITICAL] No HTTP Responses section — no success code, no error codes
3. [CRITICAL] Example payload violates Business Rule 3 (password contains "Alex")
```

> Full report: [`audit/spec-audit_registration-api-v1_20260226_000000.md`](audit/spec-audit_registration-api-v1_20260226_000000.md)

**`/api-test-cases` — generates exhaustive test scenario matrix:**

```text
Feature: User Registration (POST /api/v1/users/register) [CRITICAL]

| ID     | Type | Scenario                          | Expected Result          |
|--------|------|-----------------------------------|--------------------------|
| REG-01 | POS  | Happy path — minimal valid data   | 201 Created + JWT token  |
| REG-07 | NEG  | Missing email field               | 400 + VALIDATION_ERROR   |
| REG-12 | NEG  | Email is empty string             | 400 + VALIDATION_ERROR   |
| ...    |      | (40+ scenarios per endpoint)      |                          |
```

> Full scenarios: [`docs/test-cases/test-scenarios_20260226_120000.md`](docs/test-cases/test-scenarios_20260226_120000.md)

**`/api-test-review` — deep code review of generated tests:**

```text
API Test Review Report: Registration
Scope: 19 files, 70+ tests

🔴 CRITICAL: Thread.sleep(2000) in RegistrationIdempotencyTests.kt:45
   Fix: Replace with runTest { advanceTimeBy(2.seconds) }

🟠 MAJOR: Missing Content-Type assertion in RegistrationPositiveTests.kt:28
   Fix: response.contentType() shouldBe ContentType.Application.Json

✅ Security: No issues found
✅ Allure Integration: No issues found
```

> Full review: [`audit/api-test-review-report_registration_20260301_143000.md`](audit/api-test-review-report_registration_20260301_143000.md)

### Pipeline for Existing Test Suites

When a test suite already exists, skip generation and focus on auditing and remediating:

```text
/repo-scout  →  /api-test-cases  →  /api-test-review  →  /api-tests fix
(fresh map)     (gap analysis)      (legacy audit +        (surgical fixes,
                                     contract check)         no regeneration)
```

Key differences from the greenfield flow:

- Skip `/api-tests` generate — tests already exist
- `/api-test-review` reads API contracts (Swagger/OpenAPI/Protobuf/GraphQL) from the repo and validates test DTOs against the actual spec
- `/api-tests fix` automatically fixes common issues (Thread.sleep, runBlocking, missing timeouts) without rewriting test logic

### Utility Skills

| Skill            | Purpose                                                                     |
| ---------------- | --------------------------------------------------------------------------- |
| `/skill-audit`   | Audit SKILL.md files for bloat, duplication, and harmful patterns           |
| `/output-review` | Independent AI audit of any skill's output against its own checklist        |
| `/doc-lint`      | Documentation quality audit — structure issues, duplicates, SSOT violations |
| `/api-mocks`     | Generate HTTP mock server + WireMock singletons from spec                   |
| `/fix-markdown`  | Fix markdownlint errors across the repo                                     |
| `/pr`            | Create a pull request with conventional commit title                        |

> Full catalog of all 21 skills (setup, audit, analysis, translation): [docs/ai-setup.md](docs/ai-setup.md)

> **Disclaimer:** Always review AI-generated results. Even with well-crafted prompts and agents, outputs must be validated by a human before being merged or executed.

---

## CLAUDE.md — Keep It Minimal

Research shows that bloated or LLM-generated context files **reduce** agent success rate and increase inference cost by over 20%.
Keep your `CLAUDE.md` as small as possible: only specific tooling, build commands, and banned alternatives.
Do not add codebase overviews or duplicate existing documentation.

See [docs/claudemd-instructions.md](docs/claudemd-instructions.md) for the full guidelines.

---

## How to Adapt

This library is a starting point. To make it yours:

1. **Run a skill and review.** Expect some gaps on the first try.
2. **Tweak the `.md` files.** Skills are just natural language. Add your team's specific requirements, remove noise, or ask the AI to improve the prompt directly.
3. **Iterate and share.** After major edits, run `/skill-audit` to check quality.
4. **Once results are consistently good**, share the updated skill files with your team.

---

## IDE Compatibility

<details>
<summary><strong>Compatibility matrix — click to expand</strong></summary>

| Capability    | Claude Code | OpenCode   | Cursor                  | VS Code Copilot             | IntelliJ Copilot            | Codex               | JetBrains AI             | Gemini Code Assist      | Generic Chat |
| ------------- | ----------- | ---------- | ----------------------- | --------------------------- | --------------------------- | ------------------- | ------------------------ | ----------------------- | ------------ |
| `CLAUDE.md`   | **Native**  | **Native** | **Native**              | → `copilot-instructions.md` | → `copilot-instructions.md` | → `AGENTS.md`       | → `.junie/guidelines.md` | → `GEMINI.md` (symlink) | Copy-paste   |
| `qa_agent.md` | **Native**  | **Native** | → `.cursor/rules/*.mdc` | → `copilot-instructions.md` | → `copilot-instructions.md` | → `AGENTS.md`       | → `.junie/guidelines.md` | Manual read             | Copy-paste   |
| `skills/*.md` | **Native**  | **Native** | → `.cursor/rules/*.mdc` | **Native**                  | Open in editor              | → `.agents/skills/` | Read `.claude/skills/`   | Manual read             | Copy-paste   |
| Plugins       | Yes         | No         | No                      | No                          | No                          | Yes                 | No                       | No                      | No           |
| Anti-patterns | Yes         | Yes        | Yes                     | Yes                         | Yes                         | Yes                 | Yes                      | Yes                     | Copy-paste   |

> **Disclaimer:** Non-Claude tools may consume higher token usage — check token usage for any skill.
> If significant, use the native file structure per official documentation.

</details>

> IDE-specific prompts for running each skill: [docs/workshop-commands.md](docs/workshop-commands.md)

---

## Architecture

- **21 skills** in `.claude/skills/` — from repo scanning to test generation to translation
- **31 anti-pattern quality gates** in `.claude/qa-antipatterns/` — the AI checks generated code against these before finishing
- **3 specialized agents** in `.claude/agents/` — SDET (writes test code), Auditor (reviews quality), and Perf Engineer (generates load tests)
- **Layered context loading** — the AI reads only what it needs for the current task (`CLAUDE.md` always, agent/skill files on demand), keeping responses fast and focused
- **Self-improving loop** — the AI suggests improvements to its own knowledge base at the end of each run, so skills get better over time
- **Chained pipeline** — each skill builds on the previous one's output (`/repo-scout` → `/api-test-cases` → `/api-tests` → `/api-test-review`), so results are consistent and traceable

> Full inventory of all files and architectural patterns: [docs/ai-setup.md](docs/ai-setup.md)

---

## Tech Stack (for generated API tests)

**Kotlin (default — `/api-tests`):**

| Component      | Technology             |
| -------------- | ---------------------- |
| Language       | Kotlin                 |
| Test Framework | JUnit 5                |
| HTTP Client    | ktor-client (CIO)      |
| Serialization  | Jackson                |
| Assertions     | Kotest assertions-core |
| Reporting      | Allure                 |

**Java 17+ (opt-in — `/api-tests-java`):**

| Component      | Technology                          |
| -------------- | ----------------------------------- |
| Language       | Java 17+                            |
| Test Framework | JUnit 5                             |
| HTTP Client    | `java.net.http.HttpClient` (JDK 17) |
| Serialization  | Jackson (PropertyNamingStrategies)  |
| Assertions     | AssertJ (`.as()` message required)  |
| Async wait     | Awaitility                          |
| Reporting      | Allure                              |

---

## Demo & Resources

- [Demo Video](https://youtu.be/7VnjM44qkmc) — capability walkthrough, presented at Podlodka AI Crew #2 (February 2026)
- [Presentation (PDF)](presentation/Workshop_AI_for_QA.pdf)
- [Workshop commands & IDE prompts](docs/workshop-commands.md)
- Branches: `main` (fully configured project with generated tests), `spec-only` (clean starting point — API specs only, no generated code; best for first-time users)

<details>
<summary><strong>Additional resources — click to expand</strong></summary>

### Prompt Engineering

- [Anthropic Prompt Guide](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/overview)

### Anthropic (Claude)

- [Anthropic Cookbook](https://github.com/anthropics/anthropic-cookbook)
- [The Complete Guide to Building Skills for Claude (PDF)](https://resources.anthropic.com/hubfs/The-Complete-Guide-to-Building-Skill-for-Claude.pdf?hsLang=en)
- [Sub-agents](https://code.claude.com/docs/en/sub-agents)

### VS Code & GitHub Copilot

- [Custom Instructions](https://code.visualstudio.com/docs/copilot/customization/custom-instructions)

### Cursor

- [Skills](https://cursor.com/docs/context/skills)
- [Subagents](https://cursor.com/docs/context/subagents)

### Codex

- [Agent Skills](https://developers.openai.com/codex/skills/)

### Skills

- [Official skill creator](https://github.com/anthropics/skills/tree/main/skills/skill-creator)

### Vibe Coding

- [Vibe coding tips](https://www.threads.com/@boris_cherny/post/DTBVlMIkpcm)

### Translations

- [Translated version of this repository (ru)](https://github.com/vlad-ryzhkov/AI-QA-workshop-feb19)

</details>
