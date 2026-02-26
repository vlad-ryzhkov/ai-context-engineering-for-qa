---
name: api-isolated-tests
description: Generate exhaustive test case matrix (Markdown) from API specifications.
---
# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/api-isolated-tests/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Cover positive, negative, edge cases and security scenarios.
- Output as Markdown table for use as input to `$api-tests`.
- Do NOT generate test code — use `$api-tests` for that.
