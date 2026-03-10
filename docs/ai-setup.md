# AI Setup Registry

> Complete inventory of every AI context file in this project — what it is, where it lives, and why it exists.
> Updated automatically via `/update-ai-setup`.

## What are "AI context files" and why do they matter?

Modern AI coding assistants (Claude Code, Cursor, Copilot, Codex) read special markdown files from your repository to understand your project rules, coding standards, and workflows. Without these files the AI starts from zero every conversation — with them, it already knows your tech stack, test patterns, and naming conventions.

This project stores its AI context in several folders, each targeting a specific IDE:

| Folder / File                     | IDE                           | What's inside                                   |
|-----------------------------------|-------------------------------|-------------------------------------------------|
| `.claude/`                        | Claude Code, OpenCode         | Skills, agents, anti-patterns, protocols, hooks |
| `.cursor/rules/*.mdc`             | Cursor                        | Wrapper rules referencing `.claude/` files      |
| `.agents/skills/`                 | OpenAI Codex                  | Wrapper skills referencing `.claude/` files     |
| `CLAUDE.md`                       | Claude Code, OpenCode, Cursor | Project-level instructions (always loaded)      |
| `AGENTS.md`                       | OpenAI Codex                  | Project-level instructions for Codex            |
| `.github/copilot-instructions.md` | VS Code / IntelliJ Copilot    | Project-level instructions for Copilot          |

The source of truth is `.claude/` — other IDE folders contain lightweight wrappers that reference the same files. You only need to maintain one set of content.

---

## AI Context Architecture

Three layers, loaded on demand (not all at once):

```text
┌─────────────────────────────────────┐
│  Layer 1: CLAUDE.md (129 lines)     │  ← Always in context
│  Tech Stack, Safety, Skills         │
├─────────────────────────────────────┤
│  Layer 2: qa_agent.md (188 lines)   │  ← On any skill invocation
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
│   │     ├── agents/sdet.md      →  /api-isolated-tests, /api-tests, /init-skill
│   │     └── agents/auditor.md   →  /skill-audit, /doc-lint, /screenshot-analyze
│   │
│   ├── protocols/                   # Agent behavior protocols
│   │   ├── gardener.md              # "Suggest improvements" protocol
│   │   └── reflection.md            # Failure analysis → pending lessons
│   ├── settings.json                # Plugins, permissions, hooks
│   ├── agents/                      # Role-specific agents
│   │   ├── sdet.md                  #   Test code generation
│   │   └── auditor.md               #   Planning + quality audit
│   ├── hooks/                       # PostToolUse hooks
│   │   ├── skill-lint.sh            #   SKILL.md validation on every edit
│   │   └── delta-guard.sh           #   Warns on Write to governed files
│   ├── qa-antipatterns/             # Code quality checks (31 files + 1 index)
│   │   ├── _index.md
│   │   ├── api/                     # 12 patterns
│   │   │   └── java/                # 2 Java-specific patterns
│   │   ├── common/                  # 6 patterns
│   │   ├── platform/               # 6 patterns
│   │   │   └── java/                # 2 Java-specific patterns
│   │   └── security/               # 3 patterns
│   └── skills/                      # Skills (21 total)
│       ├── agents-checker/          # /agents-checker — agent compliance check
│       ├── api-isolated-tests/      # /api-isolated-tests — test case generation
│       ├── api-mocks/               # /api-mocks — HTTP mock server generation
│       ├── api-test-cases/          # /api-test-cases — bulk test cases for API
│       ├── api-tests/               # /api-tests — API automated tests (Kotlin)
│       ├── api-tests-java/          # /api-tests-java — API automated tests (Java 17+)
│       ├── doc-lint/                # /doc-lint — documentation audit
│       ├── fix-markdown/            # /fix-markdown — markdown lint fix
│       ├── init-agent/              # /init-agent — qa_agent.md generation
│       ├── init-project/            # /init-project — CLAUDE.md generation
│       ├── init-skill/              # /init-skill — new skill generation
│       ├── output-review/           # /output-review — skill output audit
│       ├── pr/                      # /pr — pull request creation
│       ├── qa-translate/            # /qa-translate — technical translation RU→EN
│       ├── repo-scout/              # /repo-scout — backend repo reconnaissance
│       ├── screenshot-analyze/      # /screenshot-analyze — L10n UI audit
│       ├── skill-audit/             # /skill-audit — SKILL.md audit
│       ├── spec-audit/              # /spec-audit — QA audit of requirements
│       ├── curate-lessons/          # /curate-lessons — lesson curation
│       └── update-ai-setup/         # /update-ai-setup — registry update
│
├── .ai-lessons/                     # Pending + graduated lessons (ACE)
│   ├── pending.md
│   └── graduated.md
│
├── .cursor/rules/                   # Cursor wrappers → reference .claude/ files
├── .agents/skills/                  # Codex wrappers → reference .claude/ files
├── .github/
│   ├── copilot-instructions.md      # Copilot wrapper → key project rules
│   └── workflows/
│       └── skill-quality.yml        # CI: skill quality pipeline
│
├── docs/
│   ├── ai-setup.md                  # This file
│   ├── api-isolated-tests/                  # Manual test scenarios
│   └── workshop-commands.md         # Workshop commands per IDE
│
├── scripts/
│   ├── skill-quality.sh             # Quality pipeline orchestrator
│   └── lib/
│       ├── skill-structure.sh       # Tier 1 Baseline checks
│       ├── token-budget.sh          # Token counting + snapshots
│       └── regression-detect.sh     # Section removal detection
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
| CLAUDE.md         | `CLAUDE.md`                     |   129 | Main onboarding: stack, safety, conventions           |
| QA Agent          | `.claude/qa_agent.md`           |   188 | Mindset, anti-patterns, Cross-Skill Protocol          |
| Gardener Protocol | `.claude/protocols/gardener.md` |    55 | "Suggest improvements" protocol injected at runtime   |
| Reflection Protocol | `.claude/protocols/reflection.md` |    36 | Failure analysis → pending lessons                  |
| Settings          | `.claude/settings.json`         |    50 | Plugins, permissions, hooks                           |
| MCP Servers       | `.mcp.json`                     |    12 | context7 + sequential-thinking                        |
| Markdownlint      | `.markdownlint.yaml`            |    38 | Markdown linting rules                                |

### Skills

| Skill                 | Path                                         | Lines | Category   | What it does                               |
|-----------------------|----------------------------------------------|------:|------------|--------------------------------------------|
| `/agents-checker`     | `.claude/skills/agents-checker/SKILL.md`     |   192 | Analysis   | Agent compliance verification              |
| `/api-isolated-tests` | `.claude/skills/api-isolated-tests/SKILL.md` |   300 | Generation | Test case generation from specification    |
| `/api-mocks`          | `.claude/skills/api-mocks/SKILL.md`          |    70 | Generation | HTTP mock server generation                |
| `/api-test-cases`     | `.claude/skills/api-test-cases/SKILL.md`     |   335 | Generation | Bulk test cases for entire API             |
| `/api-test-review`    | `.claude/skills/api-test-review/SKILL.md`    |   ~400 | Analysis   | Deep code review of generated API tests    |
| `/api-tests`          | `.claude/skills/api-tests/SKILL.md`          |   288 | Generation | API automated tests (Kotlin + JUnit 5)     |
| `/api-tests-java`     | `.claude/skills/api-tests-java/SKILL.md`     |   220 | Generation | API automated tests (Java 17+ + JUnit 5)   |
| `/doc-lint`           | `.claude/skills/doc-lint/SKILL.md`           |   248 | Analysis   | Documentation quality audit                |
| `/fix-markdown`       | `.claude/skills/fix-markdown/SKILL.md`       |    33 | Meta       | Fix markdownlint errors across repo        |
| `/init-agent`         | `.claude/skills/init-agent/SKILL.md`         |   203 | Meta       | qa_agent.md generation                     |
| `/init-project`       | `.claude/skills/init-project/SKILL.md`       |   178 | Meta       | CLAUDE.md generation                       |
| `/init-skill`         | `.claude/skills/init-skill/SKILL.md`         |   283 | Meta       | New skill generation                       |
| `/output-review`      | `.claude/skills/output-review/SKILL.md`      |   291 | Analysis   | Skill output audit                         |
| `/pr`                 | `.claude/skills/pr/SKILL.md`                 |    78 | Meta       | Pull request creation                      |
| `/qa-translate`       | `.claude/skills/qa-translate/SKILL.md`       |   282 | Meta       | Technical translation RU to EN             |
| `/repo-scout`         | `.claude/skills/repo-scout/SKILL.md`         |   416 | Analysis   | Backend repo reconnaissance                |
| `/screenshot-analyze` | `.claude/skills/screenshot-analyze/SKILL.md` |   300 | Analysis   | L10N and UI defects                        |
| `/skill-audit`        | `.claude/skills/skill-audit/SKILL.md`        |   224 | Analysis   | SKILL.md audit                             |
| `/spec-audit`         | `.claude/skills/spec-audit/SKILL.md`         |   284 | Analysis   | QA audit of requirements                   |
| `/curate-lessons`     | `.claude/skills/curate-lessons/SKILL.md`     |   163 | Meta       | Lesson curation from pending.md            |
| `/update-ai-setup`    | `.claude/skills/update-ai-setup/SKILL.md`    |   168 | Meta       | This registry update                       |

### Anti-Patterns

31 files the AI checks generated code against. Organized by category:

| File                             | Path                                                                | Lines | Category |
|----------------------------------|---------------------------------------------------------------------|------:|----------|
| _index                           | `.claude/qa-antipatterns/_index.md`                                 |    78 | Index    |
| batch-partial-failure            | `.claude/qa-antipatterns/api/batch-partial-failure.md`              |    96 | api      |
| configure-http-client            | `.claude/qa-antipatterns/api/configure-http-client.md`              |    53 | api      |
| dry-api-client                   | `.claude/qa-antipatterns/api/dry-api-client.md`                     |    77 | api      |
| eventual-consistency-writes      | `.claude/qa-antipatterns/api/eventual-consistency-writes.md`        |    70 | api      |
| inline-http-calls                | `.claude/qa-antipatterns/api/inline-http-calls.md`                  |    52 | api      |
| inline-http-calls (java)         | `.claude/qa-antipatterns/api/java/inline-http-calls.md`             |    52 | api/java |
| map-instead-of-dto (java)        | `.claude/qa-antipatterns/api/java/map-instead-of-dto.md`            |    55 | api/java |
| ktor-body-extraction             | `.claude/qa-antipatterns/api/ktor-body-extraction.md`               |    54 | api      |
| map-instead-of-dto               | `.claude/qa-antipatterns/api/map-instead-of-dto.md`                 |    66 | api      |
| missing-business-error-assertion | `.claude/qa-antipatterns/api/missing-business-error-assertion.md`   |    50 | api      |
| missing-content-type-validation  | `.claude/qa-antipatterns/api/missing-content-type-validation.md`    |    61 | api      |
| missing-security-headers         | `.claude/qa-antipatterns/api/missing-security-headers.md`           |    56 | api      |
| silent-catch                     | `.claude/qa-antipatterns/api/silent-catch.md`                       |    59 | api      |
| wrap-infrastructure-errors       | `.claude/qa-antipatterns/api/wrap-infrastructure-errors.md`         |    60 | api      |
| assertion-without-message        | `.claude/qa-antipatterns/common/assertion-without-message.md`       |    80 | common   |
| hardcoded-test-data              | `.claude/qa-antipatterns/common/hardcoded-test-data.md`             |    64 | common   |
| no-abstraction-layer             | `.claude/qa-antipatterns/common/no-abstraction-layer.md`            |    70 | common   |
| no-cleanup-pattern               | `.claude/qa-antipatterns/common/no-cleanup-pattern.md`              |   123 | common   |
| no-order-dependent-tests         | `.claude/qa-antipatterns/common/no-order-dependent-tests.md`        |    70 | common   |
| static-object-mother             | `.claude/qa-antipatterns/common/static-object-mother.md`            |    90 | common   |
| controlled-retries               | `.claude/qa-antipatterns/platform/controlled-retries.md`            |    57 | platform |
| completablefuture-no-timeout     | `.claude/qa-antipatterns/platform/java/completablefuture-no-timeout.md` | 50 | platform/java |
| flaky-sleep-tests (java)         | `.claude/qa-antipatterns/platform/java/flaky-sleep-tests.md`        |    58 | platform/java |
| coroutine-test-return-type       | `.claude/qa-antipatterns/platform/coroutine-test-return-type.md`    |    79 | platform |
| flaky-sleep-tests                | `.claude/qa-antipatterns/platform/flaky-sleep-tests.md`             |    62 | platform |
| junit-test-initialization        | `.claude/qa-antipatterns/platform/junit-test-initialization.md`     |    57 | platform |
| no-hardcoded-timeouts            | `.claude/qa-antipatterns/platform/no-hardcoded-timeouts.md`         |    48 | platform |
| no-shared-mutable-state          | `.claude/qa-antipatterns/platform/no-shared-mutable-state.md`       |    76 | platform |
| information-leakage-in-errors    | `.claude/qa-antipatterns/security/information-leakage-in-errors.md` |    91 | security |
| no-sensitive-data-logging        | `.claude/qa-antipatterns/security/no-sensitive-data-logging.md`     |    53 | security |
| pii-combined                     | `.claude/qa-antipatterns/security/pii-combined.md`                  |    81 | security |

### Reference Files

Supporting data for skills — templates, examples, glossaries:

| File                | Path                                                                | Lines | Purpose                                       |
|---------------------|---------------------------------------------------------------------|------:|-----------------------------------------------|
| coverage-matrix     | `.claude/skills/api-test-cases/references/coverage-matrix.md`       |   221 | Coverage matrix for /api-test-cases           |
| output-template     | `.claude/skills/api-test-cases/references/output-template.md`       |   195 | Output template for /api-test-cases           |
| quality-gates       | `.claude/skills/api-test-cases/references/quality-gates.md`         |   142 | Quality gates for /api-test-cases             |
| api-patterns        | `.claude/skills/api-tests/references/api-patterns.md`               |    77 | Patterns for API tests (Kotlin)               |
| api-patterns-java   | `.claude/skills/api-tests-java/references/java/api-patterns.md`     |    95 | Patterns for API tests (Java 17+)             |
| examples            | `.claude/skills/api-tests/references/examples.md`                   |   172 | Code examples for /api-tests                  |
| best-practices      | `.claude/skills/doc-lint/references/best-practices.md`              |    82 | Corporate documentation practices             |
| check-rules         | `.claude/skills/doc-lint/references/check-rules.md`                 |   110 | Thresholds, duplicate signatures, SSOT matrix |
| phases              | `.claude/skills/doc-lint/references/phases.md`                      |   215 | Phases and algorithm for /doc-lint            |
| qa-agent-template   | `.claude/skills/init-agent/references/qa-agent-template.md`         |   107 | qa_agent.md template                          |
| qa-profiles         | `.claude/skills/init-agent/references/qa-profiles.md`               |   128 | QA agent profiles                             |
| claude-md-template  | `.claude/skills/init-project/references/claude-md-template.md`      |   152 | CLAUDE.md template                            |
| interaction-guide   | `.claude/skills/init-skill/references/interaction-guide.md`         |    95 | Interactive workflow guide for /init-skill    |
| skill-template      | `.claude/skills/init-skill/references/skill-template.md`            |   143 | SKILL.md template                             |
| validation-checklist | `.claude/skills/init-skill/references/validation-checklist.md`      |    39 | Validation checklist for /init-skill          |
| yaml-reference      | `.claude/skills/init-skill/references/yaml-reference.md`            |    99 | Skill YAML specification                      |
| glossary            | `.claude/skills/qa-translate/references/glossary.md`                |   182 | Translation glossary RU to EN                 |
| examples            | `.claude/skills/qa-translate/references/examples.md`                |   148 | Translation examples                          |
| formatting-rules    | `.claude/skills/qa-translate/references/formatting-rules.md`        |   268 | Formatting rules for translation              |
| lang-patterns       | `.claude/skills/repo-scout/references/lang-patterns.md`             |   275 | Language patterns index (detection + cross-language) |
| lang-go             | `.claude/skills/repo-scout/references/lang-go.md`                   |   200 | Go-specific patterns for /repo-scout          |
| lang-python         | `.claude/skills/repo-scout/references/lang-python.md`               |    93 | Python-specific patterns for /repo-scout      |
| lang-nodejs         | `.claude/skills/repo-scout/references/lang-nodejs.md`               |    93 | Node.js/TS-specific patterns for /repo-scout  |
| lang-jvm            | `.claude/skills/repo-scout/references/lang-jvm.md`                  |    86 | Java/Kotlin-specific patterns for /repo-scout |
| report-template     | `.claude/skills/repo-scout/references/report-template.md`           |   299 | Report template for /repo-scout               |
| checklists          | `.claude/skills/screenshot-analyze/references/checklists.md`        |   113 | L10N check checklists                         |
| cldr-tables         | `.claude/skills/screenshot-analyze/references/cldr-tables.md`       |   151 | CLDR reference tables                         |
| html-template       | `.claude/skills/screenshot-analyze/references/html-template.md`     |   160 | HTML report template                          |
| l10n-domain-rules   | `.claude/skills/screenshot-analyze/references/l10n-domain-rules.md` |    41 | Localization domain rules                     |
| lqa-rules           | `.claude/skills/screenshot-analyze/references/lqa-rules.md`         |    42 | LQA check rules for /screenshot-analyze       |

### Agents

| File    | Path                        | Lines | Role                     |
|---------|-----------------------------|------:|--------------------------|
| auditor | `.claude/agents/auditor.md` |   169 | Planning + quality audit |
| sdet    | `.claude/agents/sdet.md`    |   201 | Test code generation     |

### Hooks and Scripts

| File             | Path                            | Lines | Trigger / Usage          | Purpose                        |
|------------------|---------------------------------|------:|--------------------------|--------------------------------|
| skill-lint.sh    | `.claude/hooks/skill-lint.sh`   |    46 | PostToolUse (Write/Edit) | SKILL.md validation on edit    |
| delta-guard.sh   | `.claude/hooks/delta-guard.sh`  |    38 | PostToolUse (Write)      | Warns Write on governed files  |
| pre-commit.sh    | `scripts/pre-commit.sh`        |     — | git pre-commit           | Blocks secrets from commit     |
| pre-push.sh      | `scripts/pre-push.sh`          |     — | git pre-push             | Blocks secrets from push       |
| setup-hooks.sh   | `scripts/setup-hooks.sh`       |     — | manual                   | Installs git hooks             |

### Quality Scripts

| Script                | Path                               | Purpose                               |
|-----------------------|------------------------------------|---------------------------------------|
| skill-quality.sh      | `scripts/skill-quality.sh`         | Pipeline orchestrator (agnix + checks)|
| skill-structure.sh    | `scripts/lib/skill-structure.sh`   | Tier 1 Baseline (S8–S17)             |
| token-budget.sh       | `scripts/lib/token-budget.sh`      | Token counting + snapshots            |
| regression-detect.sh  | `scripts/lib/regression-detect.sh` | Section removal detection             |

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
**Solution:** 31 pattern files across 4 categories. The AI checks its output against them before finishing.

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
**Solution:** `/repo-scout` → `/api-test-cases` → `/api-tests` → `/api-test-review` — each skill builds on upstream artifacts.

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
│ /spec-audit │     │           │     │/api-isolated-tests│
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

### Quality Pipeline

Automated enforcement for AI context files: `bash scripts/skill-quality.sh`

```text
skill-quality.sh (orchestrator)
├── npx agnix                          # 230+ generic AI config rules
├── scripts/lib/skill-structure.sh     # Tier 1 Baseline (S8-S17)
├── scripts/lib/token-budget.sh        # Token counting + snapshots
└── scripts/lib/regression-detect.sh   # Section removal detection
```

Baseline: `.claude/baselines/skill-snapshot.json` — tracks 21 skills (tokens, lines, section compliance).

<details>
<summary>Pipeline modes and CI integration</summary>

| Command | What it does |
|---------|-------------|
| `bash scripts/skill-quality.sh` | Full: agnix + structure + budget |
| `--check structure` | Tier 1 Baseline only |
| `--check budget` | Token budget report |
| `--check regression` | Detect removed sections vs baseline |
| `--snapshot` | Save current state as baseline |
| `--diff` | Compare current vs baseline |
| `--ci` | CI mode: agnix + structure + regression + diff |

CI: `.github/workflows/skill-quality.yml` runs on PRs touching `.claude/`.

</details>

### Adaptive Context Evolution (ACE)

Adopted from [ACE paper (arXiv:2510.04618)](https://arxiv.org/abs/2510.04618).

| ACE Concept | Implementation | Files |
|-------------|----------------|-------|
| **Delta Updates** | Surgical `Edit`, never full `Write`. `delta-guard.sh` warns. | `CLAUDE.md`, `.claude/hooks/delta-guard.sh` |
| **Rule Annotations** | `Freq` column in antipattern index (`high`/`med`/`low`). | `.claude/qa-antipatterns/_index.md` |
| **Reflection** | Failure analysis → 1 rule → `.ai-lessons/pending.md` | `.claude/protocols/reflection.md` |
| **Lesson Curation** | `/curate-lessons` deduplicates + graduates rules | `.claude/skills/curate-lessons/SKILL.md` |

<details>
<summary>Learning loop diagram</summary>

```text
Skill failure (PARTIAL / LOOP_GUARD)
  → Reflection Protocol
    → Append to .ai-lessons/pending.md
      → ≥ 3 lessons → /curate-lessons
        → Dedup against CLAUDE.md, qa-antipatterns/, SKILL.md
          → Graduate → target files (Delta Update)
            → Archive → .ai-lessons/graduated.md
```

Gardener Protocol also feeds `pending.md` for cross-cutting rules.

</details>

<details>
<summary>Skipped ACE concepts (and why)</summary>

| Concept | Reason |
|---------|--------|
| Auto helpful/harmful counters | No persistent state. Manual `Freq` column substitutes |
| Embedding dedup | Keyword-grep sufficient for ~31 patterns |
| Autonomous curation | Auto-mutating context is risky. User-triggered safer |
| Playbook ID system | File-path referencing serves as ID |

</details>

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
| HTTP Client (Java, opt-in) | `java.net.http.HttpClient` (JDK 17) | RestAssured, OkHttp, Retrofit      |
| Assertions (Java, opt-in) | AssertJ (`assertThat(...).as("msg")`) | Assertions without `.as()` message |

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

## init-skill vs Official skill-creator

Official alternative: [skill-creator](https://github.com/anthropics/skills/tree/main/skills/skill-creator)

|                          | /init-skill (this repo)                     | skill-creator (official)                      |
|--------------------------|---------------------------------------------|-----------------------------------------------|
| Infrastructure           | Zero — bash + filesystem                    | Python 3 + `claude -p` CLI                    |
| Verbosity                | Strict protocol, 6 checkpoints              | Conversational, flexible                      |
| Validation               | Static checklist + bash                     | Live A/B evals + grading.json                 |
| Improve existing skill   | ✅ Phase 0 mode                              | ✅ Core feature                                |
| Description optimization | ❌                                           | ✅ run_loop.py                                 |
| HTML review viewer       | ❌                                           | ✅ generate_review.py                          |
| Best for                 | QA skills, any routine task, fast iteration | High-reuse skills, eval-driven quality, scale |

---

<details>
<summary>Known Limitations and Design Tradeoffs</summary>

| Item | Status | Why |
|------|--------|-----|
| `api-test-review` 564 lines, no quality_gate | Known | 8-phase pipeline needs density; splitting breaks phase flow |
| `spec-audit` no quality_gate | Known | Audit output IS the quality gate |
| `fix-markdown` / `pr` lite (35/89 lines) | By design | Utility skills; full Tier 1 overhead exceeds logic |

</details>

## Changelog

| Date       | Description                                                                                                                                                                                       |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-03-09 | ACE adoption (delta-guard hook, reflection protocol, lesson curation). +1 skill (`/curate-lessons`), +1 protocol (`reflection.md`), +1 hook (`delta-guard.sh`). Quality pipeline documented. Antipattern index enhanced with grep signatures + freq. Total: 21 skills, 31 anti-patterns |
| 2026-03-01 | Added `/api-test-review` skill (deep code review of generated API tests). Updated main pipeline. Total: 20 skills, 31 anti-patterns |
| 2026-02-28 | Added `/api-tests-java` skill (Java 17+ test generation), 4 Java-specific anti-patterns (api/java/inline-http-calls, api/java/map-instead-of-dto, platform/java/completablefuture-no-timeout, platform/java/flaky-sleep-tests), 1 reference (api-patterns-java). Updated sdet.md with Java Compilation Rules. Total: 19 skills, 31 anti-patterns, 26 references |
| 2026-02-28 | Added 4 skills (api-mocks, api-test-cases, fix-markdown, pr), 2 anti-patterns (batch-partial-failure, eventual-consistency-writes), 3 reference files (api-test-cases refs). Updated all line counts. Total: 18 skills, 27 anti-patterns, 25 references |
| 2026-02-21 | Rewrote intro and pattern catalog for clarity. Added 3 skills (agents-checker, qa-translate, output-review), expanded anti-patterns to 22 files, updated line counts, synced tech stack with CLAUDE.md |
