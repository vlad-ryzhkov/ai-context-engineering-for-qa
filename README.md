# AI Context Engineering for QA

Stop writing ad-hoc prompts. Start using engineered AI skills for QA.

This repository is a ready-to-use library of **18 AI skills**, **28 anti-pattern quality gates**, and **2 specialized agents** designed specifically for QA workflows. Copy the `.claude/` folder into your project, and your AI assistant immediately knows how to audit specs, generate test cases, write API tests, and check its own output.

Works with Claude Code, OpenCode, Cursor, VS Code Copilot, and Codex.

<p align="center">
  <img src="presentation/context-pyramid.png" alt="Context Pyramid" width="300"/>
</p>

---

## Quick Start

You don't need to read the whole repo. Three steps to start getting value:

1. **Copy** — Copy the `.claude/` folder from this repo into your backend or QA project root.
   For non-Claude IDEs (Cursor, Copilot, etc.), see [IDE Compatibility](#ide-compatibility) below.

2. **Initialize** — Open your AI assistant in the project and run:
   ```text
   /init-project
   ```
   This generates `CLAUDE.md` with project-specific AI instructions.

3. **Run your first skill** — Try scanning a backend repo:
   ```text
   /repo-scout
   ```

---

## Core QA Workflow

The main pipeline — choose your starting point based on scope:

```text
/repo-scout       →  /api-test-cases     →  /api-tests
(backend repo)       (test scenarios)       (QA test repo)
```

| Skill             | Input                 | Output                             | What you get                                         |
|-------------------|-----------------------|------------------------------------|------------------------------------------------------|
| `/repo-scout`     | Backend repository    | API surface map, coverage gaps     | Catalog of endpoints, infrastructure, entry points   |
| `/api-test-cases` | Specification + audit | Test scenario matrix (Markdown)    | Exhaustive test cases for all endpoints, by priority |
| `/api-tests`      | Test scenarios + spec | Kotlin test code (JUnit 5, Allure) | Production-ready tests, run with `./gradlew test`    |

> While `/api-tests` provides good coverage out of the box, it is designed to be adapted to your team's architectural guidelines.
>
> **Optional:** `/api-isolated-tests` — generates detailed test scenarios for a single endpoint (steps, data, expected results). Use when you need a deep-dive into one area instead of full API coverage.

### Utility Skills

| Skill            | Purpose                                                                     |
|------------------|-----------------------------------------------------------------------------|
| `/skill-audit`   | Audit SKILL.md files for bloat, duplication, and harmful patterns           |
| `/output-review` | Independent AI audit of any skill's output against its own checklist        |
| `/doc-lint`      | Documentation quality audit — structure issues, duplicates, SSOT violations |
| `/api-mocks`     | Generate HTTP mock server + WireMock singletons from spec                   |
| `/fix-markdown`  | Fix markdownlint errors across the repo                                     |
| `/pr`            | Create a pull request with conventional commit title                        |

> Full catalog of all 18 skills (setup, audit, analysis, translation): [docs/ai-setup.md](docs/ai-setup.md)

> **Disclaimer:** Always review AI-generated results. Even with well-crafted prompts and agents, outputs must be validated by a human before being merged or executed.

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

| Capability    | Claude Code | OpenCode   | Cursor                  | VS Code Copilot             | IntelliJ Copilot            | Codex               | Generic Chat |
|---------------|-------------|------------|-------------------------|-----------------------------|-----------------------------|---------------------|--------------|
| `CLAUDE.md`   | **Native**  | **Native** | **Native**              | → `copilot-instructions.md` | → `copilot-instructions.md` | → `AGENTS.md`       | Copy-paste   |
| `qa_agent.md` | **Native**  | **Native** | → `.cursor/rules/*.mdc` | → `copilot-instructions.md` | → `copilot-instructions.md` | → `AGENTS.md`       | Copy-paste   |
| `skills/*.md` | **Native**  | **Native** | → `.cursor/rules/*.mdc` | **Native**                  | Open in editor              | → `.agents/skills/` | Copy-paste   |
| Plugins       | Yes         | No         | No                      | No                          | No                          | Yes                 | No           |
| Anti-patterns | Yes         | Yes        | Yes                     | Yes                         | Yes                         | Yes                 | Copy-paste   |

> **Disclaimer:** Non-Claude tools may consume higher token usage — check token usage for any skill.
> If significant, use the native file structure per official documentation.

</details>

> IDE-specific prompts for running each skill: [docs/workshop-commands.md](docs/workshop-commands.md)

---

## Architecture

- **18 skills** in `.claude/skills/` — from repo scanning to test generation to translation
- **28 anti-pattern quality gates** in `.claude/qa-antipatterns/` — the AI checks generated code against these before finishing
- **2 specialized agents** (Auditor + SDET) in `.claude/agents/` — delegate planning vs. code generation
- **Progressive Disclosure** — `CLAUDE.md` → `qa_agent.md` → `SKILL.md` load only on demand, saving tokens
- **Gardener Protocol** — AI suggests improvements to the knowledge base at the end of each run
- **Cross-Skill Pipeline** — each skill builds on upstream artifacts for consistent, traceable results

> Full inventory of all files and patterns: [docs/ai-setup.md](docs/ai-setup.md)

---

## Tech Stack (for generated API tests)

| Component      | Technology             |
|----------------|------------------------|
| Language       | Kotlin                 |
| Test Framework | JUnit 5                |
| HTTP Client    | ktor-client (CIO)      |
| Serialization  | Jackson                |
| Assertions     | Kotest assertions-core |
| Reporting      | Allure                 |

---

## Demo & Resources

- [Demo Video](https://youtu.be/7VnjM44qkmc) — capability walkthrough, presented at Podlodka AI Crew #2 (February 2026)
- [Presentation (PDF)](presentation/Workshop_AI_for_QA.pdf)
- [Workshop commands & IDE prompts](docs/workshop-commands.md)
- Branches: `main` (configured project), `spec-only` (clean starting point)

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
