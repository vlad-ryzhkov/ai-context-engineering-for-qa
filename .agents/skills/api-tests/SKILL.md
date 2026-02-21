---
name: api-tests
description: Generate Production-Ready API tests in Kotlin (JUnit5, Allure) from specifications.
---
# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/api-tests/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Use `ApiRequestBaseJson` wrapper — custom HTTP wrappers are BANNED.
- Assertions must use JUnit 5 format with message parameter.
- Link all tests to Allure IDs.
- Serialization: Jackson (SNAKE_CASE) only.
