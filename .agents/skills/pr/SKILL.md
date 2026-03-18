---
name: pr
description: Create a pull request — runs tests, commits changes, pushes branch, opens PR.
---

# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/pr/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Use conventional commit title format.
- Run tests before committing.
- Confirm target branch with the user before pushing.
