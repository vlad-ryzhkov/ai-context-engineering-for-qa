# AI QA Workshop — Copilot Instructions

## INSTRUCTIONS FOR GITHUB COPILOT

ALWAYS prioritize the context defined in `CLAUDE.md` and `.claude/qa_agent.md`.
If the user asks for a specific task (like "analyze" or "test"), **YOU MUST** ask them to open the corresponding SKILL file if it is not already in the context.

## Context

- **Project:** Mobile/Backend QA Automation Workshop
- **Role:** Senior QA Automation Engineer
- **Languages:** Kotlin, Markdown
- **Documentation:** in English

> **Tech Stack, Core Principles, Safety Protocols:** see `CLAUDE.md` at the repository root (SSOT)

## Anti-Patterns (BANNED)

| Problem | What to do instead |
|---------|--------------------|
| `Thread.sleep()` in tests | Polling with timeout (Awaitility) |
| `Map<String, Any>` for API | Typed DTOs with `@JsonNaming(SnakeCaseStrategy::class)` |
| HTTP calls directly in test | Client layer (abstraction) |
| PII in test data | Faker or masked values |
| Assertion without message | `assertEquals` with context description |

## Skills (How to Use)

GitHub Copilot does not read instructions automatically. To execute a task:

1. **Specification audit:**
   - Type in chat: `@workspace Read .claude/skills/spec-audit/SKILL.md and perform audit for file specifications/api.yaml`

2. **Test generation:**
   - Open file `src/test/kotlin/MyTest.kt`
   - Open file `.claude/skills/api-tests/SKILL.md`
   - Type: "Generate tests based on the open SKILL file"

| Command (alias)    | Which file to add to context                        | Purpose                                |
|--------------------|------------------------------------------------------|----------------------------------------|
| Repo Scout         | `.claude/skills/repo-scout/SKILL.md`                 | Repository scanning                    |
| Spec Audit         | `.claude/skills/spec-audit/SKILL.md`                 | QA audit of specification              |
| Test Cases         | `.claude/skills/api-isolated-tests/SKILL.md`                 | Test cases from specification          |
| API Tests          | `.claude/skills/api-tests/SKILL.md`                  | API automated tests (JUnit 5, Allure)  |
| Screenshot         | `.claude/skills/screenshot-analyze/SKILL.md`         | L10N and UI defects                    |
| Doc Lint           | `.claude/skills/doc-lint/SKILL.md`                   | Documentation audit                    |
| Skill Audit        | `.claude/skills/skill-audit/SKILL.md`                | SKILL.md files audit                   |
| Output Review      | `.claude/skills/output-review/SKILL.md`              | Skill output audit                     |
| Agents Checker     | `.claude/skills/agents-checker/SKILL.md`             | Agent setup validation                 |
| Init Skill         | `.claude/skills/init-skill/SKILL.md`                 | New skill creation                     |
| Init Agent         | `.claude/skills/init-agent/SKILL.md`                 | qa_agent.md creation                   |
| Init Project       | `.claude/skills/init-project/SKILL.md`               | CLAUDE.md project initialization       |
| Update AI Setup    | `.claude/skills/update-ai-setup/SKILL.md`            | AI setup registry update               |
| QA Translate       | `.claude/skills/qa-translate/SKILL.md`               | Technical translation RU→EN            |

**Workflow:** Audit → Test Cases → API Tests

## Project Structure

```text
CLAUDE.md                        # Full project context (Single Source of Truth)
.claude/qa_agent.md              # Mindset + Anti-Patterns + Protocols
.claude/skills/                  # Detailed instructions per task
specifications/                  # API specifications for analysis
src/test/kotlin/                 # API automated tests
audit/                           # Requirements audit results
```

> Full project context: see `CLAUDE.md` at the repository root.
> QA agent and anti-patterns: see `.claude/qa_agent.md`.
