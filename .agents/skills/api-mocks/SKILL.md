---
name: api-mocks
description: Generate in-process HTTP mock server + WireMock singletons from API spec.
---

# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/api-mocks/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Use WireMock for external service mocking.
- Serialization: Jackson (SNAKE_CASE) only.
- All mocks must be deterministic and isolated per test.
