---
name: api-test-review
description: Deep code review of Kotlin/Java API tests for security, architecture, and quality.
---

# INSTRUCTIONS

You are acting as the QA Auditor.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/api-test-review/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Review against security, architecture, Kotlin idioms, test quality, HTTP validation, and Allure.
- Only report findings with Confidence >= 80.
- Use Action-First output: CRITICAL first, MAJOR second, passing categories last.
