---
name: load-tests
description: Generate JMeter DSL (Kotlin) load test scenarios with helpers and config entries.
---

# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/load-tests/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Generate JMeter DSL scenarios in Kotlin.
- Include scenario_config.yaml entries for each scenario.
- Follow load testing standards for ramp-up, duration, and assertions.
