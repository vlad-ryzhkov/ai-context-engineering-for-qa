# QA Profiles — Core Principles and Anti-Patterns

> **Purpose:** Reference of principles and anti-patterns for different testing types. Used in `/init-agent` for generating qa_agent.md tailored to a specific profile.

---

## Universal Principles (for all profiles)

```markdown
1. **Trust No One** — verify requirements for contradictions
2. **Isolation First** — tests do not depend on each other
3. **Cleanup Always** — delete created data
4. **Fail Fast** — fail early, fail loudly
5. **Evidence-Based** — every bug with evidence
```

---

## API Testing

### Core Principles

```markdown
1. **Contract First** — test verifies the contract, not the implementation
2. **Boundary Obsession** — boundary values matter more than happy path
3. **Negative > Positive** — more negative scenarios than positive
4. **Idempotency Check** — repeated request = same result
```

### Anti-Patterns

```markdown
| ❌ Bad | ✅ Good | Why |
|--------|---------|-----|
| Hardcoded endpoint URL | Configuration via env | Portability |
| Ignoring status code | Exact code verification | Contract |
| Single assert per test | Structured assertions | Completeness |
| Testing implementation | Testing contract | Flexibility |
```

---

## UI/E2E Testing

### Core Principles

```markdown
1. **User Perspective** — think like a user
2. **Stable Selectors** — data-testid is better than CSS classes
3. **Visual Regression** — screenshots of critical screens
4. **Flaky = Bug** — an unstable test is a test bug
```

### Anti-Patterns

```markdown
| ❌ Bad | ✅ Good | Why |
|--------|---------|-----|
| XPath selectors | data-testid | Stability |
| Hardcoded waits | Polling with condition | Synchronization |
| Screenshots everywhere | Screenshots of critical areas | Speed |
| One long test | Atomic scenarios | Debugging |
```

---

## Performance Testing

### Core Principles

```markdown
1. **Baseline First** — measure first, then optimize
2. **Realistic Load** — load profile from production
3. **Percentiles > Average** — p95/p99 matter more than average
4. **Resource Monitoring** — CPU/RAM/IO during the test
```

### Anti-Patterns

```markdown
| ❌ Bad | ✅ Good | Why |
|--------|---------|-----|
| Average time only | Percentiles p95/p99 | Real picture |
| Constant load | Gradual ramp-up | Realism |
| Ignoring resources | CPU/RAM/IO monitoring | Bottlenecks |
| Testing on laptop | Production-like environment | Accuracy |
```

---

## Security Testing

### Core Principles

```markdown
1. **OWASP Top 10** — minimum checklist
2. **AuthZ ≠ AuthN** — authorization and authentication are different things
3. **Trust Nothing** — all input data is potentially malicious
4. **Least Privilege** — minimum permissions for operation
```

### Anti-Patterns

```markdown
| ❌ Bad | ✅ Good | Why |
|--------|---------|-----|
| Hardcoded credentials | Secrets in vault | Security |
| Ignoring SQL injection | Parameterized queries | XSS/SQLi |
| Trusting input data | Validation + sanitization | Injection attacks |
| Admin tokens in tests | Minimum permissions | Least Privilege |
```

---

## Universal Anti-Patterns (for all profiles)

```markdown
| ❌ Bad | ✅ Good | Why |
|--------|---------|-----|
| Thread.sleep() | Polling with timeout | Flaky tests |
| Hardcoded ID/email | UUID/timestamp generation | Collisions |
| Shared test data | Isolated data | Dependencies |
| Ignoring cleanup | try-finally | Garbage in system |
| assertEquals without msg | assertEquals with msg | Debugging in CI |
| var instead of val | val everywhere | Immutability |
| Catch without handling | Logging + rethrow | Information loss |
| Magic numbers | Named constants | Readability |
```
