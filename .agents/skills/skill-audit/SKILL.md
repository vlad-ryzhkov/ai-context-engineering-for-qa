---
name: skill-audit
description: Audit SKILL.md files for bloat, duplication and harmful patterns.
---
# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow the instructions in:
`.claude/skills/skill-audit/SKILL.md`

## CRITICAL REMINDERS

- Detect bloated templates, duplication, and "DO NOT FIX" anti-patterns.
- Use for optimizing AI setup and reducing token costs.
- Do NOT use for auditing human-readable documentation — use `$doc-lint` for that.
