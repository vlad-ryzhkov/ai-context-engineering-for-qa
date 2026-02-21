# AI Setup Registry

> Complete inventory of every AI context file in this project — what it is, where it lives, and why it exists.
> Updated automatically via `/update-ai-setup`.

## What are "AI context files" and why do they matter?

Modern AI coding assistants (Claude Code, Cursor, Copilot, Codex) read special markdown files from your repository to understand your project rules, coding standards, and workflows. Without these files the AI starts from zero every conversation — with them, it already knows your tech stack, test patterns, and naming conventions.

This project stores its AI context in several folders, each targeting a specific IDE:

| Folder / File                        | IDE                      | What's inside                                                     |
|--------------------------------------|--------------------------|-------------------------------------------------------------------|
| `.claude/`                           | Claude Code, OpenCode    | Skills, agents, anti-patterns, protocols, hooks                   |
| `.cursor/rules/*.mdc`               | Cursor                   | Wrapper rules referencing `.claude/` files                        |
| `.agents/skills/`                    | OpenAI Codex             | Wrapper skills referencing `.claude/` files                       |
| `CLAUDE.md`                          | Claude Code, OpenCode, Cursor | Project-level instructions (always loaded)                   |
| `AGENTS.md`                          | OpenAI Codex             | Project-level instructions for Codex                              |
| `.github/copilot-instructions.md`   | VS Code / IntelliJ Copilot | Project-level instructions for Copilot                         |

The source of truth is `.claude/` — other IDE folders contain lightweight wrappers that reference the same files. You only need to maintain one set of content.

---

## AI Context Architecture

Three layers, loaded on demand (not all at once):

```text
┌─────────────────────────────────────┐
│  Layer 1: CLAUDE.md (127 lines)     │  ← Always in context
│  Tech Stack, Safety, Skills         │
├─────────────────────────────────────┤
│  Layer 2: qa_agent.md (204 lines)   │  ← On any skill invocation
│  Mindset, Anti-Patterns, Protocols  │
├─────────────────────────────────────┤
│  Layer 3: SKILL.md + references/    │  ← On specific skill activation
│  Algorithm, examples, checklists    │
└─────────────────────────────────────┘
```

**Why layers?** Each layer adds tokens to the AI's context window. Loading everything at once wastes tokens and dilutes attention. Loading only what's needed for the current task gives better results and costs less.

---

## File Inventory

### Project Structure

```text
.
├── CLAUDE.md                        # Project instructions — always loaded by AI
├── AGENTS.md                        # Same role, for OpenAI Codex
├── .mcp.json                        # MCP servers: context7, sequential-thinking
├── .markdownlint.yaml               # Markdown linting rules
│
├── .claude/                         # Claude Code / OpenCode configuration
│   ├── qa_agent.md                  # QA agent: mindset, anti-patterns, protocols
│   │
│   │   Agent routing:
│   │   qa_agent.md (Orchestrator)
│   │     ├── agents/sdet.md      →  /test-cases, /api-tests, /init-skill
│   │     └── agents/auditor.md   →  /skill-audit, /doc-lint, /screenshot-analyze
│   │
│   ├── protocols/                   # Agent behavior protocols
│   │   └── gardener.md              # "Suggest improvements" protocol
│   ├── settings.json                # Plugins, permissions, hooks
│   ├── agents/                      # Role-specific agents
│   │   ├── sdet.md                  #   Test code generation
│   │   └── auditor.md               #   Planning + quality audit
│   ├── hooks/                       # PostToolUse hooks
│   │   └── skill-lint.sh            #   SKILL.md validation on every edit
│   ├── qa-antipatterns/             # Code quality checks (25 files + 1 index)
│   │   ├── _index.md
│   │   ├── api/                     # 10 patterns
│   │   ├── common/                  # 6 patterns
│   │   ├── platform/               # 6 patterns
│   │   └── security/               # 3 patterns
│   └── skills/                      # Skills (14 total)
│       ├── agents-checker/          # /agents-checker — agent compliance check
│       ├── api-tests/               # /api-tests — API automated tests
│       ├── doc-lint/                # /doc-lint — documentation audit
│       ├── init-agent/              # /init-agent — qa_agent.md generation
│       ├── init-project/            # /init-project — CLAUDE.md generation
│       ├── init-skill/              # /init-skill — new skill generation
│       ├── output-review/           # /output-review — skill output audit
│       ├── qa-translate/            # /qa-translate — technical translation RU→EN
│       ├── repo-scout/              # /repo-scout — backend repo reconnaissance
│       ├── screenshot-analyze/      # /screenshot-analyze — L10n UI audit
│       ├── skill-audit/             # /skill-audit — SKILL.md audit
│       ├── spec-audit/              # /spec-audit — QA audit of requirements
│       ├── test-cases/              # /test-cases — test case generation
│       └── update-ai-setup/         # /update-ai-setup — registry update
│
├── .cursor/rules/                   # Cursor wrappers → reference .claude/ files
├── .agents/skills/                  # Codex wrappers → reference .claude/ files
├── .github/
│   └── copilot-instructions.md      # Copilot wrapper → key project rules
│
├── docs/
│   ├── ai-setup.md                  # This file
│   ├── test-cases/                  # Manual test scenarios
│   └── workshop-commands.md         # Workshop commands per IDE
│
├── specifications/                  # API specifications for analysis
│
├── src/test/
│   ├── kotlin/                      # API automated tests
│   └── resources/screenshots/       # Screenshots for L10n analysis
│
├── audit/                           # Requirements audit results
└── rtl-example/                     # RTL layout example
```

### Root Configuration Files

| File              | Path                            | Lines | Purpose                                               |
|-------------------|---------------------------------|------:|-------------------------------------------------------|
| CLAUDE.md         | `CLAUDE.md`                     |   127 | Main onboarding: stack, safety, conventions           |
| QA Agent          | `.claude/qa_agent.md`           |   204 | Mindset, anti-patterns, Cross-Skill Protocol          |
| Gardener Protocol | `.claude/protocols/gardener.md` |    44 | "Suggest improvements" protocol injected at runtime   |
| Settings          | `.claude/settings.json`         |    50 | Plugins, permissions, hooks                           |
| MCP Servers       | `.mcp.json`                     |    12 | context7 + sequential-thinking                        |
| Markdownlint      | `.markdownlint.yaml`            |    38 | Markdown linting rules                                |

### Skills

| Skill                 | Path                                         | Lines | Category   | What it does                               |
|-----------------------|----------------------------------------------|------:|------------|--------------------------------------------|
| `/agents-checker`     | `.claude/skills/agents-checker/SKILL.md`     |   192 | Analysis   | Agent compliance verification              |
| `/api-tests`          | `.claude/skills/api-tests/SKILL.md`          |   232 | Generation | API automated tests (Kotlin + JUnit 5)     |
| `/doc-lint`           | `.claude/skills/doc-lint/SKILL.md`           |   248 | Analysis   | Documentation quality audit                |
| `/init-agent`         | `.claude/skills/init-agent/SKILL.md`         |   200 | Meta       | qa_agent.md generation                     |
| `/init-project`       | `.claude/skills/init-project/SKILL.md`       |   170 | Meta       | CLAUDE.md generation                       |
| `/init-skill`         | `.claude/skills/init-skill/SKILL.md`         |   283 | Meta       | New skill generation                       |
| `/output-review`      | `.claude/skills/output-review/SKILL.md`      |   290 | Analysis   | Skill output audit                         |
| `/qa-translate`       | `.claude/skills/qa-translate/SKILL.md`       |   282 | Meta       | Technical translation RU to EN             |
| `/repo-scout`         | `.claude/skills/repo-scout/SKILL.md`         |   272 | Analysis   | Backend repo reconnaissance                |
| `/screenshot-analyze` | `.claude/skills/screenshot-analyze/SKILL.md` |   289 | Analysis   | L10N and UI defects                        |
| `/skill-audit`        | `.claude/skills/skill-audit/SKILL.md`        |   211 | Analysis   | SKILL.md audit                             |
| `/spec-audit`         | `.claude/skills/spec-audit/SKILL.md`         |   229 | Analysis   | QA audit of requirements                   |
| `/test-cases`         | `.claude/skills/test-cases/SKILL.md`         |   246 | Generation | Manual test cases                          |
| `/update-ai-setup`    | `.claude/skills/update-ai-setup/SKILL.md`    |   168 | Meta       | This registry update                       |

### Anti-Patterns

25 files the AI checks generated code against. Organized by category:

| File                              | Path                                                              | Lines | Category |
|-----------------------------------|-------------------------------------------------------------------|------:|----------|
| _index                            | `.claude/qa-antipatterns/_index.md`                               |    72 | Index    |
| configure-http-client             | `.claude/qa-antipatterns/api/configure-http-client.md`            |    53 | api      |
| inline-http-calls                 | `.claude/qa-antipatterns/api/inline-http-calls.md`                |    50 | api      |
| map-instead-of-dto                | `.claude/qa-antipatterns/api/map-instead-of-dto.md`               |    61 | api      |
| missing-business-error-assertion  | `.claude/qa-antipatterns/api/missing-business-error-assertion.md` |    50 | api      |
| missing-content-type-validation   | `.claude/qa-antipatterns/api/missing-content-type-validation.md`  |    61 | api      |
| missing-security-headers          | `.claude/qa-antipatterns/api/missing-security-headers.md`         |    56 | api      |
| dry-api-client                    | `.claude/qa-antipatterns/api/dry-api-client.md`                   |    77 | api      |
| ktor-body-extraction              | `.claude/qa-antipatterns/api/ktor-body-extraction.md`             |    54 | api      |
| silent-catch                      | `.claude/qa-antipatterns/api/silent-catch.md`                     |    57 | api      |
| wrap-infrastructure-errors        | `.claude/qa-antipatterns/api/wrap-infrastructure-errors.md`       |    60 | api      |
| assertion-without-message         | `.claude/qa-antipatterns/common/assertion-without-message.md`     |    76 | common   |
| hardcoded-test-data               | `.claude/qa-antipatterns/common/hardcoded-test-data.md`           |    64 | common   |
| no-abstraction-layer              | `.claude/qa-antipatterns/common/no-abstraction-layer.md`          |    64 | common   |
| no-cleanup-pattern                | `.claude/qa-antipatterns/common/no-cleanup-pattern.md`            |    95 | common   |
| no-order-dependent-tests          | `.claude/qa-antipatterns/common/no-order-dependent-tests.md`      |    70 | common   |
| static-object-mother              | `.claude/qa-antipatterns/common/static-object-mother.md`          |    90 | common   |
| controlled-retries                | `.claude/qa-antipatterns/platform/controlled-retries.md`          |    57 | platform |
| coroutine-test-return-type        | `.claude/qa-antipatterns/platform/coroutine-test-return-type.md`  |    79 | platform |
| flaky-sleep-tests                 | `.claude/qa-antipatterns/platform/flaky-sleep-tests.md`           |    57 | platform |
| junit-test-initialization         | `.claude/qa-antipatterns/platform/junit-test-initialization.md`   |    57 | platform |
| no-hardcoded-timeouts             | `.claude/qa-antipatterns/platform/no-hardcoded-timeouts.md`       |    48 | platform |
| no-shared-mutable-state           | `.claude/qa-antipatterns/platform/no-shared-mutable-state.md`     |    76 | platform |
| information-leakage-in-errors     | `.claude/qa-antipatterns/security/information-leakage-in-errors.md` |  91 | security |
| no-sensitive-data-logging         | `.claude/qa-antipatterns/security/no-sensitive-data-logging.md`   |    53 | security |
| pii-combined                      | `.claude/qa-antipatterns/security/pii-combined.md`                |    81 | security |

### Reference Files

Supporting data for skills — templates, examples, glossaries:

| File                 | Path                                                                | Lines | Purpose                                         |
|----------------------|---------------------------------------------------------------------|------:|-------------------------------------------------|
| api-patterns         | `.claude/skills/api-tests/references/api-patterns.md`               |    68 | Patterns for API tests                          |
| examples             | `.claude/skills/api-tests/references/examples.md`                   |    68 | Code examples for /api-tests                    |
| best-practices       | `.claude/skills/doc-lint/references/best-practices.md`              |    82 | Corporate documentation practices               |
| check-rules          | `.claude/skills/doc-lint/references/check-rules.md`                 |   110 | Thresholds, duplicate signatures, SSOT matrix   |
| phases               | `.claude/skills/doc-lint/references/phases.md`                      |   215 | Phases and algorithm for /doc-lint              |
| qa-agent-template    | `.claude/skills/init-agent/references/qa-agent-template.md`         |   103 | qa_agent.md template                            |
| qa-profiles          | `.claude/skills/init-agent/references/qa-profiles.md`               |   128 | QA agent profiles                               |
| claude-md-template   | `.claude/skills/init-project/references/claude-md-template.md`      |   147 | CLAUDE.md template                              |
| interaction-guide    | `.claude/skills/init-skill/references/interaction-guide.md`         |    95 | Interactive workflow guide for /init-skill       |
| skill-template       | `.claude/skills/init-skill/references/skill-template.md`            |   143 | SKILL.md template                               |
| validation-checklist | `.claude/skills/init-skill/references/validation-checklist.md`      |    39 | Validation checklist for /init-skill            |
| yaml-reference       | `.claude/skills/init-skill/references/yaml-reference.md`            |    91 | Skill YAML specification                        |
| glossary             | `.claude/skills/qa-translate/references/glossary.md`                |   176 | Translation glossary RU to EN                   |
| examples             | `.claude/skills/qa-translate/references/examples.md`                |   148 | Translation examples                            |
| formatting-rules     | `.claude/skills/qa-translate/references/formatting-rules.md`        |   266 | Formatting rules for translation                |
| lang-patterns        | `.claude/skills/repo-scout/references/lang-patterns.md`             |    93 | Language patterns for /repo-scout               |
| report-template      | `.claude/skills/repo-scout/references/report-template.md`           |   101 | Report template for /repo-scout                 |
| checklists           | `.claude/skills/screenshot-analyze/references/checklists.md`        |   113 | L10N check checklists                           |
| cldr-tables          | `.claude/skills/screenshot-analyze/references/cldr-tables.md`       |   151 | CLDR reference tables                           |
| html-template        | `.claude/skills/screenshot-analyze/references/html-template.md`     |   160 | HTML report template                            |
| l10n-domain-rules    | `.claude/skills/screenshot-analyze/references/l10n-domain-rules.md` |    41 | Localization domain rules                       |
| lqa-rules            | `.claude/skills/screenshot-analyze/references/lqa-rules.md`         |    42 | LQA check rules for /screenshot-analyze         |

### Agents

| File    | Path                        | Lines | Role                     |
|---------|-----------------------------|------:|--------------------------|
| auditor | `.claude/agents/auditor.md` |   165 | Planning + quality audit |
| sdet    | `.claude/agents/sdet.md`    |   189 | Test code generation     |

### Hooks

| File          | Path                          | Lines | Trigger                  | Purpose                     |
|---------------|-------------------------------|------:|--------------------------|-----------------------------|
| skill-lint.sh | `.claude/hooks/skill-lint.sh` |    48 | PostToolUse (Write/Edit) | SKILL.md validation on edit |

### Documentation

| File                 | Path                        | Lines | Purpose                   |
|----------------------|-----------------------------|------:|---------------------------|
| AI Setup (this file) | `docs/ai-setup.md`          |     — | AI configuration registry |
| Workshop Commands    | `docs/workshop-commands.md` |   237 | Workshop commands per IDE |

---

## Pattern Catalog

Approaches used in this project. Each pattern solves a specific problem.

### 1. Three-Layer AI Context

**Problem:** AI forgets project rules between conversations.
**Solution:** Three files (`CLAUDE.md` → `qa_agent.md` → `SKILL.md`) loaded on demand — each adds detail without wasting tokens.

### 2. Progressive Disclosure

**Problem:** Loading all instructions at once wastes tokens and dilutes AI attention.
**Solution:** YAML description loads first → SKILL.md body only when the skill is invoked → references/scripts only when needed.

### 3. Self-Review Protocol

**Problem:** AI can produce low-quality output and not notice.
**Solution:** Every skill ends with a self-review scorecard. Score < 70% → warning.

### 4. 4-Layer Test Architecture

**Problem:** Generated test code becomes tangled — models, HTTP calls, and assertions mixed together.
**Solution:** Models → Client → Data → Tests. Separation of concerns.

### 5. Anti-Pattern Library

**Problem:** AI repeats the same mistakes — hardcoded data, missing assertions, Thread.sleep().
**Solution:** 25 pattern files across 4 categories. The AI checks its output against them before finishing.

### 6. Locked Tech Stack + BANNED list

**Problem:** AI picks random libraries (Gson instead of Jackson, TestNG instead of JUnit).
**Solution:** Fixed stack table with an explicit BANNED column in `CLAUDE.md`.

### 7. Token Economy

**Problem:** AI runs away on large tasks, burning tokens with no progress.
**Solution:** PAUSE on tasks > 20K tokens. FULL_SCAN keyword for explicit full scanning.

### 8. Safety Protocols

**Problem:** AI runs destructive git commands or deletes files without asking.
**Solution:** FORBIDDEN command list, DESTROY keyword override, mandatory backup.

### 9. Cross-Skill Pipeline

**Problem:** Skills run in isolation, not building on each other's output.
**Solution:** `/spec-audit` → `/test-cases` → `/api-tests` — each skill expects upstream artifacts.

### 10. Compilation Gate

**Problem:** AI generates test code that doesn't compile.
**Solution:** Mandatory `./gradlew compileTestKotlin` before finishing. Max 3 retry attempts.

### 11. Traceability

**Problem:** No link between manual test cases and automated tests.
**Solution:** `@Link("TC-XX")` annotations in generated test code.

### 12. Security-First Mindset

**Problem:** Generated tests ignore security concerns.
**Solution:** OWASP, PII checks, SQL Injection, XSS, IDOR built into the agent mindset and anti-patterns.

### 13. Meta-Skills Bootstrap

**Problem:** Creating AI config files from scratch is tedious and error-prone.
**Solution:** `/init-project`, `/init-agent`, `/init-skill` generate config files interactively.

### 14. Plugin: kotlin-lsp

Kotlin LSP plugin for code navigation. Enabled in `.claude/settings.json`.

### 15. Skill Size Limit

**Problem:** Large SKILL.md files waste tokens and confuse the AI.
**Solution:** SKILL.md ≤ 500 lines. Exceeded → extract to `references/`, `scripts/`, `qa-antipatterns/`.

### 16. Cross-IDE Compatibility

Files work across Claude Code, OpenCode, Cursor, VS Code, IntelliJ, Codex. See [README.md — IDE Compatibility](../README.md#-ide-compatibility).

### 17. Workshop Checkpoint Branches

Git branches for workshop checkpoints — start from any stage.

### 18. MCP Integration

MCP servers extend AI: `context7` for up-to-date library docs, `sequential-thinking` for step-by-step analysis.

### 19. Technical Translation

`/qa-translate` translates markdown files RU→EN with glossary enforcement.

### 20. Markdown Lint

Automatic markdown quality checking via `.markdownlint.yaml`.

### 21. Dependency Injection (Gardener)

**Problem:** AI completes the task but doesn't notice improvements it could suggest.
**Solution:** Agents inject `gardener.md` protocol at runtime — AI suggests knowledge base improvements without blocking the main flow.

### 22. Agent Compliance Check

`/agents-checker` verifies agent files match init-agent standards.

---

## Feedback Loop

How the system improves over time:

```text
┌─────────────┐     ┌───────────┐     ┌───────────┐
│  Discovery  │────▶│ Strategy  │────▶│ Execution │
│ /repo-scout │     │           │     │ /api-tests│
│ /spec-audit │     │           │     │/test-cases│
└─────────────┘     └───────────┘     └─────┬─────┘
       ▲                                     │
       │            ┌──────────────────────┐ │
       │            │  Gardener Protocol   │ │
       └────────────│  error → rule →      │◀┘
                    │  prevention          │
                    └──────────┬───────────┘
                               │ updates
                               ▼
                    ┌──────────────────────┐
                    │ qa-antipatterns/     │
                    │ skills/*/SKILL.md    │
                    │ agents/*.md          │
                    │ CLAUDE.md            │
                    └──────────────────────┘
```

### Quality Gates

| Transition       | What is checked                                         | Blocker                                  |
|------------------|---------------------------------------------------------|------------------------------------------|
| Plan → Execution | Endpoint coverage, priorities, gaps in specifications   | Missing Critical endpoints               |
| Execution → Done | Compilation, `@Link` to specification, coverage vs plan | `compileTestKotlin` fail (max 3 retry)   |

### Self-Improvement Mechanisms

| #  | Mechanism                     | What it does                                                                  |
|----|-------------------------------|-------------------------------------------------------------------------------|
| 1  | Multi-Agent Orchestration     | 2 agents (SDET, Auditor) + orchestrator `qa_agent.md`                         |
| 2  | Doc-Lint                      | Cross-file duplicates, SSOT violations, health score                          |
| 3  | Skill-Audit                   | 9 checks: bloat, waste sections, duplication, harmful patterns                |
| 4  | AI Registry Sync              | Delta update of this file — registry of all project AI files                  |
| 5  | Real-Time Hook                | `skill-lint.sh` validates SKILL.md on every edit                              |
| 6  | Gardener Protocol             | AI notices "smells" during work → suggests fixes without blocking             |
| 7  | Anti-Pattern Library          | 25 pattern files in 4 categories — reference-driven checks                    |
| 8  | kotlin-lsp Plugin             | Kotlin code navigation and analysis                                           |
| 9  | Segregation of Duties         | SDET codes, Auditor reviews — no one reviews their own work                   |

---

## Tech Stack and Plugins

### Tech Stack (LOCKED)

| Component      | Technology                                     | BANNED                               |
|----------------|------------------------------------------------|--------------------------------------|
| HTTP Client    | ktor-client (CIO) + ktor-serialization-jackson | Custom HTTP wrappers, retrofit       |
| Serialization  | Jackson (SNAKE_CASE) + jackson-module-kotlin   | Gson, Moshi                          |
| Assertions     | Kotest assertions-core                         | Assertions without message           |
| Async          | kotlinx-coroutines-test                        | `Thread.sleep()`, `delay()` in tests |
| Test Framework | JUnit 5                                        | TestNG                               |
| Reporting      | Allure                                         | —                                    |

### Build

| Component | Version |
|-----------|---------|
| Kotlin    | 1.9.22  |
| JVM       | 17      |
| Gradle    | 9.2.1   |

### Plugins

| Plugin     | Package                 | Status  | Purpose                                     |
|------------|-------------------------|---------|---------------------------------------------|
| kotlin-lsp | claude-plugins-official | Enabled | Kotlin LSP for code navigation and analysis |

### MCP Servers

| Server              | Package                                          | Purpose                            |
|---------------------|--------------------------------------------------|------------------------------------|
| context7            | @upstash/context7-mcp@latest                     | Up-to-date library documentation   |
| sequential-thinking | @modelcontextprotocol/server-sequential-thinking | Step-by-step complex task analysis |

---

## Security and Governance

| Mechanism        | Description                                                          | Defined in            |
|------------------|----------------------------------------------------------------------|-----------------------|
| FORBIDDEN        | `git reset --hard`, `git clean -fd`, branch deletion, `rm -rf .git` | `CLAUDE.md:34`        |
| DESTROY          | Override for destructive operations — requires keyword from user     | `CLAUDE.md:36`        |
| Token Economy    | PAUSE > 20K tokens, FULL_SCAN for full scanning                     | `CLAUDE.md:38-40`     |
| Planning First   | Tasks > 3 files → Analysis → Plan → Execute                         | `CLAUDE.md:42`        |
| Git Workflow     | Branch confirmation before push                                      | `CLAUDE.md:44-46`     |
| Compilation Gate | `./gradlew compileTestKotlin` before commit, max 3 attempts         | `qa_agent.md:183-193` |
| Fail Fast        | BLOCKER on untestable/contradictory requirements                    | `qa_agent.md:15-34`   |

---

## Changelog

| Date       | Description                                                                                                                                                                                       |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-02-21 | Rewrote intro and pattern catalog for clarity. Added 3 skills (agents-checker, qa-translate, output-review), expanded anti-patterns to 22 files, updated line counts, synced tech stack with CLAUDE.md |
