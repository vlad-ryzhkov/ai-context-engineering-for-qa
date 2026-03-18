---
name: curate-lessons
description: Curate pending lessons from .ai-lessons/pending.md into context files.
---

# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/curate-lessons/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Deduplicate against existing rules in CLAUDE.md, qa-antipatterns/, and SKILL.md files.
- Graduate only validated lessons — do not auto-promote without verification.
- Use Delta Updates (Edit, not Write) when promoting rules to target files.
