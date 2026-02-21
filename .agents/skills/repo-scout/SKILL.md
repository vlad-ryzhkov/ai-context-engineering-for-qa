---
name: repo-scout
description: Scan backend repository (Go), catalog API surface, infrastructure and test coverage.
---
# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow the instructions in:
`.claude/skills/repo-scout/SKILL.md`

## CRITICAL REMINDERS

- Use when entering a new repo before writing tests.
- Do NOT use for QA projects — use `$init-project` for those.
- First step in the recommended workflow: `$repo-scout` → `$spec-audit` → `$test-cases` → `$api-tests`.
