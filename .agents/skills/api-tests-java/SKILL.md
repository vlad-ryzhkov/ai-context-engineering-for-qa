---
name: api-tests-java
description: Generate Production-Ready API tests in Java 17+ (JUnit5, Allure, AssertJ) from specifications.
---

# INSTRUCTIONS

You are acting as the QA Automation Lead.
Read `AGENTS.md` to understand the project philosophy and tech stack.

## LOGIC SOURCE

Do NOT guess the procedure and do NOT output anything yet.
You MUST use your file-reading tool to fetch and strictly follow:

1. First, read the core agent context: `.claude/qa_agent.md`
2. Second, read the specific skill protocol: `.claude/skills/api-tests-java/SKILL.md`
3. Execute based STRICTLY on the logic and output format defined in those files.

## CRITICAL REMINDERS

- Use `java.net.http.HttpClient` (JDK 17 built-in) — RestAssured, OkHttp, Retrofit are BANNED.
- Assertions must use AssertJ with `.as("message")` on every assertion.
- Serialization: Jackson (PropertyNamingStrategies.SNAKE_CASE) only.
- Async waiting: Awaitility, not Thread.sleep().
