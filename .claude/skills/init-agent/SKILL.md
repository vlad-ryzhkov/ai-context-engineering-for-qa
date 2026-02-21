---
name: init-agent
description: Generates qa_agent.md — a job description for the AI with QA team culture, principles, and anti-patterns. Use when setting up AI for a project, onboarding new AI agents, or standardizing testing approaches. Do not use for editing existing qa_agent.md — edit manually.
allowed-tools: "Read Write Edit Glob Grep"
agent: agents/auditor.md
context: fork
---

# /init-agent — qa_agent.md Generator

<purpose>
Creating a "job description" for the AI: mindset, anti-patterns, quality gates.
Focus: broad-profile QA engineers (API, UI, Mobile, Performance).
</purpose>

## Before Starting

Read `.claude/qa_agent.md`.

## When to Use

- Setting up AI for a specific QA team's culture
- Standardizing testing approaches
- Onboarding new AI agents into a project

## Execution Algorithm

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the artifact (MD/HTML), not into chat.

**Chat output (constraints):**
- Brief Summary: max 5 lines (what was found, how many, result)
- Findings table: max 15 lines (top by severity)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress for each file. Checkpoint only on:
- Phase transition (Phase N → Phase N+1)
- Blocker detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, no "Now I will grep..."
- Read → analyze → report, no "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist), not a separate file.

### Step 1: Determine QA Profile

Ask the user or determine from the project:

```text
What type of testing predominates?

1. API Testing — REST/GraphQL, contract testing
2. UI/E2E Testing — Web (Playwright/Selenium), Mobile (Appium)
3. Performance Testing — load testing, stress testing
4. Security Testing — OWASP, pentesting
5. Mixed — combination of multiple types
```

### Step 1 Error Handling

**`references/qa-profiles.md` not found** → Use built-in principles from the "Step 4: Quality Gates" section and proceed to Step 4 directly.

**Profile is ambiguous** (user answered vaguely) → Clarify with one question:

```text
Name 1-2 task types that take > 70% of the team's time:
REST API tests / UI automation / Load testing / Pentesting
```

**`qa_agent.md` already exists** → Warn: "File already exists. Overwrite? (yes / no)". Do not overwrite without confirmation.

### Step 2: Select Core Principles

Read `references/qa-profiles.md` and select principles by profile from Step 1:

- Always include **Universal principles** (5 principles)
- Add principles for the specific profile (API/UI/Performance/Security)
- For Mixed profile — combine principles from all relevant categories

### Step 3: Collect Anti-Patterns

From `references/qa-profiles.md` select relevant anti-patterns:

- Always include **Universal Anti-Patterns** (8 entries)
- Add profile-specific ones (if applicable)
- Build a unified table for the "Anti-Patterns (BANNED)" section

### Step 4: Define Quality Gates

```markdown
## Quality Gates

### Before Commit
- [ ] Code compiles without errors
- [ ] Tests pass locally
- [ ] No hardcoded values (IDs, emails, phone numbers)
- [ ] Cleanup works

### Before PR
- [ ] Tests are isolated (can run in any order)
- [ ] Naming convention followed
- [ ] No commented-out code
- [ ] Critical scenarios covered

### Before Release
- [ ] All tests green in CI
- [ ] No flaky tests
- [ ] Performance baseline not degraded
```

### Step 5: Generate qa_agent.md

```markdown
# QA Agent: [Profile]

## Core Mindset

[3-5 principles from Step 2]

## Anti-Patterns (BANNED)

[Table from Step 3]

## Quality Gates

[Checklists from Step 4]

## Test Design

### Structure (AAA)
```[language]
// Arrange — setup
// Act — action
// Assert — verification
```

### Naming Convention

```text
`[actor] can [action] when [condition]`
`[actor] cannot [action] when [condition]`
```

## Data Management

### Unique Data Generation

[Example for the chosen language]

### Cleanup Pattern

[try-finally example for the chosen language]

## Cross-Skill Protocol

1. `/spec-audit` → verify requirements
2. `/testcases` → write test cases
3. `/api-tests` → automate

**Do not jump straight to code!**
```text

## Output

Save to `.claude/qa_agent.md`

## Example Dialog

```
User: /init-agent

AI: What QA engineer profile?
1. API Testing
2. UI/E2E Testing
3. Performance Testing
4. Security Testing
5. Mixed (universal)

User: 5

AI: Generating qa_agent.md for universal QA...

[Shows file with principles from all categories]

Save to .claude/qa_agent.md? (y/n)
```text

## Self-Check (before saving)

- [ ] **Core Principles:** 3-5 principles selected and filled in?
- [ ] **Anti-Patterns:** Table contains at least 5 entries?
- [ ] **Quality Gates:** All 3 sections (commit/PR/release) present?
- [ ] **No placeholders:** No remaining `[xxx]` in the text?
- [ ] **Cross-Skill Protocol:** Section present with `/spec-audit` → `/testcases` → `/api-tests`?

## Related Files

- Template: `.claude/skills/init-agent/references/qa-agent-template.md`
- QA profiles: `.claude/skills/init-agent/references/qa-profiles.md`
- Full guide: `docs/ai-files-handbook.md`
