---
name: doc-lint
description: Audit documentation quality — size, structure, duplicates, SSOT violations.
---
# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow the instructions in:
`.claude/skills/doc-lint/SKILL.md`

## CRITICAL REMINDERS

- Check for duplication between files and SSOT violations.
- Do NOT use for code review or source code analysis — use `$skill-audit` for SKILL.md files.
