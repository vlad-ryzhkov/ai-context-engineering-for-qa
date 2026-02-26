# AGENTS.md — Project Context Bridge

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

Invoke with `$skill-name` or via the skill selector:

| Skill               | Purpose                                      |
|---------------------|----------------------------------------------|
| `$repo-scout`       | Repository scanning                          |
| `$spec-audit`       | QA audit of requirements                     |
| `$api-isolated-tests`       | Test cases from specification                |
| `$api-tests`        | API automated tests (Kotlin)                 |
| `$screenshot-analyze` | Screenshot analysis for L10N defects       |
| `$doc-lint`         | Documentation audit                          |
| `$skill-audit`      | SKILL.md files audit                         |
| `$init-skill`       | New skill creation                           |
| `$init-agent`       | qa_agent.md creation                         |
| `$init-project`     | CLAUDE.md project initialization             |
| `$update-ai-setup`  | AI setup registry update                     |
| `$output-review`    | Independent skill output audit               |
| `$agents-checker`   | Agent setup validation                       |
| `$qa-translate`     | Technical translation RU→EN                  |

**Recommended Workflow:** `$repo-scout` → `$spec-audit` → `$api-isolated-tests` → `$api-tests`
