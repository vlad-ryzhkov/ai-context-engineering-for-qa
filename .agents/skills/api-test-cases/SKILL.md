---
name: api-test-cases
description: Generate exhaustive test scenario matrices for all API endpoints grouped by domain.
---

# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/api-test-cases/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Generate test cases for ALL endpoints, not a subset.
- Group scenarios by domain feature.
- Include positive, negative, boundary, and security scenarios.
