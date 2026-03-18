# JetBrains AI (Junie) — Project Context Bridge

## CORE INSTRUCTION

**YOU MUST READ AND FOLLOW `CLAUDE.md` AT THE ROOT OF THIS PROJECT.**

`CLAUDE.md` is the **Single Source of Truth** for:

1. **Tech Stack:** Kotlin, JUnit 5, Allure, ktlint — LOCKED.
2. **Safety Protocols:** No destructive commands, no .env leaks.
3. **Code Style:** Formatting, naming conventions, assertion rules.
4. **Communication Protocol:** CLI-mode, no preambles, tool-first.

## QA AGENT PERSONA

**YOU MUST ALSO READ:** `.claude/qa_agent.md`

`qa_agent.md` defines:

- QA Lead philosophy and mindset
- Anti-patterns to avoid
- Workflow protocols (Fail Fast, Compilation Gate)

## CRITICAL BEHAVIOR

- If `CLAUDE.md` conflicts with any other instruction, `CLAUDE.md` WINS.
- Do NOT generate code that violates the strict dependencies listed in `CLAUDE.md`.
- All documentation and skill content must be written in **English**, unless explicitly stated otherwise.

## AVAILABLE SKILLS

Skills are defined in `.claude/skills/`. Read the `SKILL.md` file in each directory for the full protocol.

| Skill                | Path                                         | Purpose                                 |
| -------------------- | -------------------------------------------- | --------------------------------------- |
| `repo-scout`         | `.claude/skills/repo-scout/SKILL.md`         | Repository scanning                     |
| `spec-audit`         | `.claude/skills/spec-audit/SKILL.md`         | QA audit of requirements                |
| `api-isolated-tests` | `.claude/skills/api-isolated-tests/SKILL.md` | Test cases from specification           |
| `api-test-cases`     | `.claude/skills/api-test-cases/SKILL.md`     | Bulk test cases for entire API          |
| `api-tests`          | `.claude/skills/api-tests/SKILL.md`          | API automated tests (Kotlin)            |
| `api-tests-java`     | `.claude/skills/api-tests-java/SKILL.md`     | API automated tests (Java 17+)          |
| `api-test-review`    | `.claude/skills/api-test-review/SKILL.md`    | Deep code review of generated API tests |
| `api-mocks`          | `.claude/skills/api-mocks/SKILL.md`          | HTTP mock server generation             |
| `load-tests`         | `.claude/skills/load-tests/SKILL.md`         | JMeter DSL load test scenarios          |
| `screenshot-analyze` | `.claude/skills/screenshot-analyze/SKILL.md` | Screenshot analysis for L10N defects    |
| `doc-lint`           | `.claude/skills/doc-lint/SKILL.md`           | Documentation audit                     |
| `skill-audit`        | `.claude/skills/skill-audit/SKILL.md`        | SKILL.md files audit                    |
| `init-skill`         | `.claude/skills/init-skill/SKILL.md`         | New skill creation                      |
| `init-agent`         | `.claude/skills/init-agent/SKILL.md`         | qa_agent.md creation                    |
| `init-project`       | `.claude/skills/init-project/SKILL.md`       | CLAUDE.md project initialization        |
| `update-ai-setup`    | `.claude/skills/update-ai-setup/SKILL.md`    | AI setup registry update                |
| `output-review`      | `.claude/skills/output-review/SKILL.md`      | Independent skill output audit          |
| `agents-checker`     | `.claude/skills/agents-checker/SKILL.md`     | Agent setup validation                  |
| `qa-translate`       | `.claude/skills/qa-translate/SKILL.md`       | Technical translation RU→EN             |
| `fix-markdown`       | `.claude/skills/fix-markdown/SKILL.md`       | Fix markdownlint errors                 |
| `pr`                 | `.claude/skills/pr/SKILL.md`                 | Pull request creation                   |
| `curate-lessons`     | `.claude/skills/curate-lessons/SKILL.md`     | Lesson curation from pending.md         |

**Recommended Workflow:** `repo-scout` → `api-test-cases` → `api-tests` → `api-test-review`
