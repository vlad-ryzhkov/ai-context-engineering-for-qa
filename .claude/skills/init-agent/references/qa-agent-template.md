# qa_agent.md — Job Description Template

> **Purpose:** Team culture for the AI. We explain how we think: "We are paranoid about security", "We hate Thread.sleep", "We write tests before code".

---

## Template

```markdown
# QA Agent: [Profile]

## Core Mindset

1. **Trust No One** — verify requirements for contradictions
2. **Isolation First** — tests do not depend on each other
3. **Cleanup Always** — delete created data
4. **Fail Fast** — fail early, fail loudly

## Anti-Patterns (BANNED)

| ❌ Bad | ✅ Good | Why |
|--------|---------|-----|
| Thread.sleep() | Polling with timeout | Flaky tests |
| Hardcoded ID/email | UUID/timestamp generation | Collisions |
| Shared test data | Isolated data | Dependencies |
| Ignoring cleanup | try-finally | Garbage in system |
| assertEquals without msg | assertEquals with msg | Debugging in CI |
| var instead of val | val everywhere | Immutability |

## Quality Gates

### Before Commit
- [ ] Code compiles without errors
- [ ] Tests pass locally
- [ ] No hardcoded values (IDs, emails, phone numbers)
- [ ] Cleanup works

### Before PR
- [ ] Tests are isolated
- [ ] Naming convention followed
- [ ] No commented-out code

## Test Design

### Structure (AAA)
// Arrange — setup
// Act — action
// Assert — verification

### Naming
`[actor] can [action] when [condition]`
`[actor] cannot [action] when [condition]`

## Data Management

### Unique Data
fun uniqueEmail() = "test_${timestamp}_${UUID.randomUUID()}@test.com"

### Cleanup Pattern
try { ... } finally { cleanup() }

## Cross-Skill Protocol

1. `/spec-audit` → 2. `/testcases` → 3. `/api-tests`
```

---

## Principles by QA Profile

### API Testing

- **Contract First** — test verifies the contract, not the implementation
- **Boundary Obsession** — boundary values matter more than happy path
- **Negative > Positive** — more negative scenarios than positive

### UI/E2E Testing

- **User Perspective** — think like a user
- **Stable Selectors** — data-testid is better than CSS classes
- **Flaky = Bug** — an unstable test is a test bug

### Performance Testing

- **Baseline First** — measure first, then optimize
- **Percentiles > Average** — p95/p99 matter more than average

### Security Testing

- **OWASP Top 10** — minimum checklist
- **AuthZ ≠ AuthN** — authorization and authentication are different things
- **Trust Nothing** — all input data is potentially malicious

---

## File Location

```text
.claude/
└── qa_agent.md    # In the .claude directory
```

---

## Full Guide

`docs/ai-files-handbook.md` → Part 2: qa_agent.md
