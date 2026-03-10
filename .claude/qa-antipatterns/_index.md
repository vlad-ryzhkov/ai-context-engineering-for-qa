# QA Anti-Patterns Index

> **Lazy Load Protocol:** Read a file ONLY when a violation is detected.
> Preemptive loading of all files is FORBIDDEN (Token Economy).

## Naming Convention

`{category}/{problem-name}.md` → problem description and Good Example.

## Available Patterns

### common/ — Basic Code Hygiene

| File | Problem | Grep signature | Freq |
|------|---------|---------------|------|
| `common/assertion-without-message.md` | Assertions without message | `assertEquals(\|assertNotNull(` | high |
| `common/hardcoded-test-data.md` | Hardcoded data | `password.*Password123\|email.*@example\.com` | high |
| `common/no-abstraction-layer.md` | Direct HTTP calls in tests | `httpClient\.(post\|get\|put)(` | med |
| `common/static-object-mother.md` | Static Object Mother | `const val.*@example\.com\|val.*VALID_` | med |
| `common/no-order-dependent-tests.md` | Tests depend on each other | `@TestMethodOrder\|@Order(` | low |
| `common/no-cleanup-pattern.md` | No cleanup after tests | missing `@AfterEach\|finally {` | med |

### api/ — HTTP and Protocol Specifics

#### api/java/ — Java-Specific HTTP Patterns

| File | Problem | Grep signature | Freq |
|------|---------|---------------|------|
| `api/java/inline-http-calls.md` | `HttpClient` created per test method (Java) | `HttpClient\.newBuilder\|new.*HttpClient()` | med |
| `api/java/map-instead-of-dto.md` | `Map<String, Object>` instead of DTO (Java) | `Map<String, Object>\|new HashMap<` | med |

#### api/ — Kotlin/Common HTTP Patterns

| File | Problem | Grep signature | Freq |
|------|---------|---------------|------|
| `api/map-instead-of-dto.md` | `Map<String, Any>` instead of DTO | `Map<String, Any>\|mutableMapOf(` | high |
| `api/missing-content-type-validation.md` | Content-Type not validated | missing `Content-Type` header assertion | med |
| `api/configure-http-client.md` | HTTP client not configured | `ApiClient()\|HttpClient(` without config | med |
| `api/wrap-infrastructure-errors.md` | Unwrapped infrastructure errors | `ConnectException\|SocketTimeoutException` | low |
| `api/inline-http-calls.md` | `HttpClient(` created inline in test | `HttpClient(CIO)\|\.post(\|\.get(` in test | high |
| `api/missing-security-headers.md` | POS-test without security headers check | missing `X-Content-Type-Options` assertion | low |
| `api/missing-business-error-assertion.md` | NEG-test without `body.code` check | `assertEquals(400\|assertEquals(422` no body | med |
| `api/ktor-body-extraction.md` | `response.toString()` instead of `bodyAsText()` / `body<T>()` | `response\.toString()\|readValue.*toString` | med |
| `api/silent-catch.md` | Silent `catch` returning empty stub hides infrastructure failures | `catch.*Exception.*return\|catch.*Exception.*{}` | med |
| `api/dry-api-client.md` | Duplicated client methods differing only by response type | `suspend fun.*Request.*suspend fun.*ExpectError` | low |
| `api/eventual-consistency-writes.md` | Asserting read immediately after write in eventually-consistent system | write → immediate GET without `await()` | low |
| `api/batch-partial-failure.md` | Not testing batch error propagation strategy | only `allValid` + `allInvalid`, no mixed | low |

### platform/ — JUnit5 + Language-Specific

#### platform/java/ — Java-Specific Patterns

| File | Problem | Grep signature | Freq |
|------|---------|---------------|------|
| `platform/java/completablefuture-no-timeout.md` | `.get()` without timeout on `CompletableFuture` | `\.get()` without timeout args | med |
| `platform/java/flaky-sleep-tests.md` | `Thread.sleep()` instead of Awaitility (Java) | `Thread\.sleep(\|TimeUnit\.\w*\.sleep(` | high |

#### platform/ — Kotlin-Specific Patterns

| File | Problem | Grep signature | Freq |
|------|---------|---------------|------|
| `platform/coroutine-test-return-type.md` | `runBlocking` without explicit Unit type | `fun.*() = runBlocking\|fun.*() = runTest` | med |
| `platform/junit-test-initialization.md` | `@TestInstance(PER_CLASS)` + field init failures | `@TestInstance(PER_CLASS)` + field init | low |
| `platform/flaky-sleep-tests.md` | `Thread.sleep()` / `delay()` instead of polling | `Thread\.sleep(\|delay(` | high |
| `platform/no-hardcoded-timeouts.md` | Magic numbers in timeouts | `atMost(\d+,\|pollInterval(\d+,` | med |
| `platform/no-shared-mutable-state.md` | Shared state between tests | `companion object.*var\|var.*created` | med |
| `platform/controlled-retries.md` | Uncontrolled retry logic | `repeat(\|while.*true\|catch.*Exception.*{}` | low |

### security/ — Data and Security

| File | Problem | Grep signature | Freq |
|------|---------|---------------|------|
| `security/no-sensitive-data-logging.md` | PII in logs | `@Step.*{token}\|@Step.*{password}` | med |
| `security/information-leakage-in-errors.md` | Data leakage in error logs | `Exception\|\.kt:\|/opt/\|SELECT` in errors | low |
| `security/pii-combined.md` | PII in test data and code (api-tests + testcases) | `@gmail\.com\|@yandex\.ru\|+7916` | high |

## Usage (for SDET)

When a problem is found in code:
1. Determine the category: common / api / platform / security
2. Read `.claude/qa-antipatterns/{category}/{name}.md` → apply Good Example → cite `(ref: {category}/{name}.md)`
3. If reference not found → BLOCKER, do not guess the fix

## Usage (for Auditor)

Quick scan using grep signatures from the index above:

```bash
# Scan by category
ls .claude/qa-antipatterns/api/

# High-frequency patterns — scan these first
grep -rn "Thread\.sleep\|delay(" src/test/
grep -rn "Map<String, Any>\|mutableMapOf(" src/test/
grep -rn "@gmail\.com\|@example\.com\|Password123" src/test/
grep -rn "assertEquals(\|assertNotNull(" src/test/ | grep -v "message\|\.as("

# Read file on match
cat .claude/qa-antipatterns/api/inline-http-calls.md
```

<details>
<summary>Full grep commands by category</summary>

```bash
# common/
grep -rn "assertEquals(\|assertNotNull(" src/test/
grep -rn "password.*Password123\|email.*@example\.com" src/test/
grep -rn "httpClient\.\(post\|get\|put\)(" src/test/
grep -rn "@TestMethodOrder\|@Order(" src/test/

# api/
grep -rn "HttpClient(CIO)" src/test/
grep -rn "Map<String, Any>\|mutableMapOf(" src/test/
grep -rn "response\.toString()" src/test/
grep -rn "catch.*Exception" src/test/

# platform/
grep -rn "Thread\.sleep(\|delay(" src/test/
grep -rn "fun.*() = runBlocking\|fun.*() = runTest" src/test/
grep -rn "companion object" src/test/ | grep "var "

# security/
grep -rn "@gmail\.com\|@yandex\.ru\|@mail\.ru" src/test/
grep -rn "@Step.*token\|@Step.*password" src/test/
```

</details>
