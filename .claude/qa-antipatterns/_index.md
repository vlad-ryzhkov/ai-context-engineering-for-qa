# QA Anti-Patterns Index

> **Lazy Load Protocol:** Read a file ONLY when a violation is detected.
> Preemptive loading of all files is FORBIDDEN (Token Economy).

## Naming Convention

`{category}/{problem-name}.md` → problem description and Good Example.

## Available Patterns

### common/ — Basic Code Hygiene

| File | Problem |
|------|---------|
| `common/assertion-without-message.md` | Assertions without message |
| `common/hardcoded-test-data.md` | Hardcoded data |
| `common/no-abstraction-layer.md` | Direct HTTP calls in tests |
| `common/static-object-mother.md` | Static Object Mother |
| `common/no-order-dependent-tests.md` | Tests depend on each other |
| `common/no-cleanup-pattern.md` | No cleanup after tests |

### api/ — HTTP and Protocol Specifics

#### api/java/ — Java-Specific HTTP Patterns

| File | Problem |
|------|---------|
| `api/java/inline-http-calls.md` | `HttpClient` created per test method (Java) |
| `api/java/map-instead-of-dto.md` | `Map<String, Object>` instead of DTO (Java) |

#### api/ — Kotlin/Common HTTP Patterns

| File | Problem |
|------|---------|
| `api/map-instead-of-dto.md` | `Map<String, Any>` instead of DTO |
| `api/missing-content-type-validation.md` | Content-Type not validated |
| `api/configure-http-client.md` | HTTP client not configured |
| `api/wrap-infrastructure-errors.md` | Unwrapped infrastructure errors |
| `api/inline-http-calls.md` | `HttpClient(` created inline in test |
| `api/missing-security-headers.md` | POS-test without security headers check |
| `api/missing-business-error-assertion.md` | NEG-test without `body.code` check |
| `api/ktor-body-extraction.md` | `response.toString()` instead of `bodyAsText()` / `body<T>()` |
| `api/silent-catch.md` | Silent `catch` returning empty stub hides infrastructure failures |
| `api/dry-api-client.md` | Duplicated client methods differing only by response type |
| `api/serialization-mixing.md` | Mixing Jackson + kotlinx.serialization in same project |
| `api/eventual-consistency-writes.md` | Asserting read immediately after write in eventually-consistent system |
| `api/batch-partial-failure.md` | Not testing batch error propagation strategy |

### platform/ — JUnit5 + Language-Specific

#### platform/java/ — Java-Specific Patterns

| File | Problem |
|------|---------|
| `platform/java/completablefuture-no-timeout.md` | `.get()` without timeout on `CompletableFuture` |
| `platform/java/flaky-sleep-tests.md` | `Thread.sleep()` instead of Awaitility (Java) |

#### platform/ — Kotlin-Specific Patterns

| File | Problem |
|------|---------|
| `platform/coroutine-test-return-type.md` | `runBlocking` without explicit Unit type |
| `platform/junit-test-initialization.md` | `@TestInstance(PER_CLASS)` + field init failures |
| `platform/flaky-sleep-tests.md` | `Thread.sleep()` / `delay()` instead of polling |
| `platform/no-hardcoded-timeouts.md` | Magic numbers in timeouts |
| `platform/no-shared-mutable-state.md` | Shared state between tests |
| `platform/controlled-retries.md` | Uncontrolled retry logic |

### security/ — Data and Security

| File | Problem |
|------|---------|
| `security/no-sensitive-data-logging.md` | PII in logs |
| `security/information-leakage-in-errors.md` | Data leakage in error logs |
| `security/pii-combined.md` | PII in test data and code (api-tests + testcases) |

## Usage (for SDET)

When a problem is found in code:
1. Determine the category: common / api / platform / security
2. Read `.claude/qa-antipatterns/{category}/{name}.md` → apply Good Example → cite `(ref: {category}/{name}.md)`
3. If reference not found → BLOCKER, do not guess the fix

## Usage (for Auditor)

```bash
# Scan by category
ls .claude/qa-antipatterns/api/

# Grep in artifact
grep -r "HttpClient(\|Map<String, Any>\|response\.toString()\|catch.*Exception" src/test/kotlin/

# Read file on match
cat .claude/qa-antipatterns/api/inline-http-calls.md
```
