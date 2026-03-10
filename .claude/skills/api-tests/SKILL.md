---
name: api-tests
description: Generates production-ready API automated tests in Kotlin (JUnit5, Allure). Use when you need to cover REST endpoints with tests from test-scenarios.md or specification. Do not use for test case generation — use /api-isolated-tests for that.
allowed-tools: "Read Write Edit Glob Grep Bash(./gradlew*)"
agent: sdet
context: fork
---

## Recommended Flow

**Main pipeline:** `/repo-scout` → `/api-test-cases` → `/api-tests`.

For single-endpoint deep-dive: `/spec-audit` → `/api-isolated-tests` → `/api-tests`.

Running directly on a specification also works — the skill will generate tests without a scenario matrix, but traceability `@Link` annotations will reference the spec instead of scenario IDs.

---

## 🔒 SYSTEM REQUIREMENTS

Before execution the agent MUST load: `.claude/protocols/gardener.md`

---

# SDET: API Automation (Kotlin)

<purpose>
Generates a complete set of Kotlin automated tests for REST API: models, HTTP client, helpers, tests.
Scenario source — `audit/test-scenarios.md` (result of /api-test-cases) or specification directly.
</purpose>

## When to Use

- There is `audit/test-scenarios.md` with a scenario matrix — cover them with automated tests
- Need to write tests for a new endpoint from scratch
- Review existing tests for standards compliance (`review` arg)

## Input Context (Process Isolation)

`context: fork` — you cannot see chat history before your invocation.

**Allowed inputs:** `audit/test-scenarios.md`, specification, `CLAUDE.md`, `build.gradle.kts`, existing `src/` (style reference only).
**Forbidden:** Assumptions from "previous agent context", inventing endpoint contracts.

## Protocol

1. **Stack:** Locked in CLAUDE.md → Tech Stack. Kotlin-specific additions: Ktor HttpClient(CIO) via `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)`, Awaitility (seconds only), Faker (TestData generators), JSON Schema Validator.
2. **BANNED:** `Thread.sleep`, `delay`, `runBlocking` (use `runTest`), `HttpClient(` in `*Tests.kt` (inline HTTP in tests), manual `@AllureId`, `shouldBe` (use `assertEquals`), `assert(` (Kotlin builtin — use `assertEquals`/`assertTrue` from `org.junit.jupiter.api.Assertions`), `LocalDateTime.now()` in strict assertions. **Zero-comment policy:** `//` and `/* */` in generated code are FORBIDDEN.
2a. **Coroutine Tests:** Preferred: `fun test(): Unit = runTest { }` from `kotlinx-coroutines-test`. If `runBlocking` is unavoidable — explicit return type required: `fun test(): Unit = runBlocking { }`. FORBIDDEN: `delay()` as timing substitute — use Awaitility.
2b. **Test Lifecycle:** `@BeforeEach`/`@AfterEach` for setup/teardown. `lateinit var` for resources requiring cleanup. **FORBIDDEN:** `@TestInstance(PER_CLASS)` with field initialization — JUnit skips class init on constructor failure.
2c. **DRY API Clients:** FORBIDDEN: duplicating client method bodies to return different response types (e.g., `registerUser(): RegisterResponse` + `registerUserExpectError(): ErrorResponse` with identical request construction). Generate a single method returning `HttpResponse` or a generic `suspend inline fun <reified T> request(...)` and let the test parse the body as needed. (ref: `api/dry-api-client.md`)
2d. **Fail Fast in Test Clients:** FORBIDDEN: wrapping API client HTTP calls or JSON deserialization in `try/catch` blocks that return empty stub objects (`catch (e: Exception) { return RegisterResponse() }`). Exceptions MUST propagate so the test runner reports the infrastructure failure at the point of failure, not on a later assertion. (ref: `api/silent-catch.md`)
2e. **Ktor Body Extraction Rule:** FORBIDDEN: `response.toString()` to obtain response body — in Ktor this returns object metadata, not the body. Use `response.body<T>()` (typed, via `ContentNegotiation` plugin) or `response.bodyAsText()` if raw string is required. Manual `Json.decodeFromString(response.toString())` is a guaranteed runtime crash. (ref: `api/ktor-body-extraction.md`)
2f. **Single Serialization Framework Rule:** Use exactly one JSON framework across the entire generation — either Jackson OR kotlinx.serialization. FORBIDDEN: mixing Jackson annotations (`@JsonProperty`, `@JsonNaming`, `@JsonIgnoreProperties`) with `kotlinx.serialization` parsing (`Json.decodeFromString`, `@Serializable`). If using Ktor with `ContentNegotiation(jackson())`, deserialize via `response.body<T>()` — never parse raw strings manually. If the stack is `jackson`, all DTOs use `@JsonNaming`/`@JsonProperty`. If the stack is `kotlinx.serialization`, all DTOs use `@Serializable` and no Jackson annotations appear anywhere.
2g. **Time-Dependent Scenarios (TTL/Cache):** FORBIDDEN: Using `Thread.sleep` or `delay()` to simulate time passing for cache expiration, idempotency TTLs, or rate limits. Do not blindly execute sequential requests expecting time to advance instantly. For short asynchronous state changes, use Awaitility. For long temporal conditions (e.g., 5-minute cache expiration), if the specification does NOT explicitly provide a testability hook (e.g., `X-Test-Advance-Time` header or cache invalidation endpoint), you MUST generate the complete test logic and annotate the test method with `@Disabled("Time-dependent scenario: {Wait Time}. Requires testability hook (time-travel/cache-clear) or manual execution.")`.
2h. **Timestamp Response Validation:** If the specification defines a time-based response field (e.g., `expires_at = request_time + N minutes`, JWT `exp` claim), the test MUST validate it with a relative drift check: `assertTrue(abs(actual - expected) < driftToleranceSeconds, "timestamp drift")`. Using `Instant.now()` + offset is acceptable; using `isNotBlank()` or `> 0` alone is INSUFFICIENT. FORBIDDEN: `LocalDateTime.now()` in assertions.
2i. **WireMock Integration for External Services:** When a test configures WireMock stubs for external services (SMS gateway, payment provider, etc.), the test MUST connect the mock to the application under test. At minimum: set a system property or environment variable (e.g., `System.setProperty("SMS_GATEWAY_URL", "http://localhost:${wireMockServer.port()}")`) in `@BeforeEach` and clear it in `@AfterEach`. A disconnected WireMock server (started but not referenced by the app) is a SILENT TEST BUG — the test passes/fails based on real service state, not mock behavior.
2k. **Eventual Consistency Writes:** If `repo-scout-report` §12 marks a write→read pair as "Eventual", FORBIDDEN to assert the read immediately after write. Use Awaitility polling with bounded timeout. (ref: `api/eventual-consistency-writes.md`)
2l. **Batch Partial Failure:** If `repo-scout-report` §12 lists batch operations, include a mixed valid+invalid input case to verify error propagation strategy (atomic rollback vs partial 207 Multi-Status). Testing only all-valid and all-invalid is INSUFFICIENT. (ref: `api/batch-partial-failure.md`)
2j. **Configurable BASE_URL:** Use a computed property so any CI/CD environment can override the target server without recompilation: `val BASE_URL: String get() = System.getProperty("BASE_URL", "http://localhost:8080")`. If `ConnectException` appears in Smoke Run and no live server is available → run `/api-mocks` to generate an in-process mock, then re-run. TLS-enforcement tests cannot pass with an HTTP mock — acceptable as documented infra limitation.
2m. **TLS/HTTPS Enforcement Tests:** FORBIDDEN: generating a test that replaces `https://` with `http://` and asserts a 4xx rejection — when both URLs hit the same HTTP-only mock, the test structurally passes regardless of TLS state (silent false-positive). If a scenario requires verifying TLS enforcement and no HTTPS infrastructure exists: generate the test skeleton, immediately annotate `@Disabled("TLS enforcement: requires HTTPS-capable server or WireMock HTTPS mode. HTTP mock cannot simulate certificate/protocol rejection.")`, and add a code comment explaining the correct infrastructure requirement. Do NOT generate a functionally broken test.
2n. **HTTP Client Timeout (Mandatory):** Every `HttpClient` instantiation MUST include: `install(HttpTimeout) { requestTimeoutMillis = 30_000L; connectTimeoutMillis = 10_000L; socketTimeoutMillis = 30_000L }`. Tests without timeout configuration hang indefinitely on infrastructure failures and block CI pipelines.
3. **Security Headers Rule:** Every positive test (POST/PUT/DELETE with 2xx) MUST verify `Content-Type`, `X-Content-Type-Options`, `Strict-Transport-Security` via `assertEquals` on `response.headers`. (ref: `api/missing-security-headers.md`)
4. **Structure:**
   - `requests/`: DTOs + Request/Response objects. **ALL** data classes mapped to snake_case JSON (request AND response) — apply `@JsonNaming(SnakeCaseStrategy::class)` on all DTOs — omitting it on response DTOs causes silent null fields. Also add `@JsonIgnoreProperties(ignoreUnknown = true)`.
   - `helpers/`: `@Step` annotated flows.
   - `tests/`: `@Epic` (from feature/package name), `@Feature` (from endpoint name), `@Severity`, `@DisplayName`. `@AllureId` — **NOT generated**: assigned manually or via utility after TMS binding. **MANDATORY TAGS:** Analyze business logic — add `@Tag("CRITICAL")` + `@Severity(SeverityLevel.CRITICAL)` for Money flows, Security/Auth, or Data integrity endpoints; add `@Tag("REGRESSION")` for all others.
   - **External Integrations:** If the endpoint under test calls 3rd-party services (payments, SMS, email providers), configure a WireMock stub in `@BeforeEach` so the test does not make real HTTP calls to external domains.
4. **Gates:** `compileTestKotlin`, `ktlintCheck`.

## Input Source Strategy

> Shared rules: `_shared/api-tests-shared.md` § Input Source Strategy. Kotlin upstream: `/api-test-cases`.

## Input Validation (Mandatory Check)

> Full 2-phase validation: `_shared/api-tests-shared.md` § Input Validation.

## Architecture Modes

> Shared structure: `_shared/api-tests-shared.md` § Architecture Modes. This skill uses `.kt` extensions, `*Tests.kt` naming. AllureId: `./gradlew assignAllureIds`.

## Verbosity Protocol

> Shared rules: `_shared/api-tests-shared.md` § Verbosity Protocol.

**Mandatory Checks:**
```bash
grep -r "Thread.sleep\|delay(\|runBlocking\|shouldBe\|//\|@AllureId(\|LocalDateTime.now()\|response\.toString()\|catch.*Exception\|kotlinx\.serialization\|Json\.decodeFromString" src/test/kotlin/
grep -rn "^\s*assert(" src/test/kotlin/ | grep -v "assertEquals\|assertTrue\|assertNotNull\|assertNotEquals\|assertThrows\|assertFalse"
grep -rl "HttpClient(" src/test/kotlin/ | grep "Tests\.kt$"
grep -r "Map<String, Any>" src/test/kotlin/
grep -rL "Strict-Transport-Security" src/test/kotlin/*/tests/*Tests.kt
grep -rL "HttpTimeout" src/test/kotlin/*/requests/*Client.kt
```
⛔ Any match on lines 1-2 → FAIL (BANNED pattern detected).
The `assert(` check (line 2) catches Kotlin builtin `assert()` while excluding JUnit `assertEquals`/`assertTrue`/etc.
Any POS-test file without HSTS check → FAIL (Protocol 3).
Any client file without HttpTimeout → FAIL (Protocol 2n).
```bash
grep -rL "AfterEach\|finally" src/test/kotlin/*/tests/*Tests.kt
```
If `test-scenarios.md` contains `Cleanup:` for POS/L10N scenarios, every test file with POS tests MUST contain `@AfterEach` or `try/finally` → FAIL.

⛔ Any match → FAIL. **Lazy Load Anti-Pattern Fix:**
1. Read `.claude/qa-antipatterns/_index.md` — find `{category}/{name}` matching the problem
2. Read `.claude/qa-antipatterns/{category}/{name}.md` → apply Good Example → cite `(ref: {category}/{name}.md)`
3. If not found → BLOCKER, do not guess the fix

**Categories:** `platform/` · `api/` · `common/` · `security/`

**Quality Gates:**
- **Spec-Mandated Cleanup:** If the specification or test scenario explicitly declares a `Cleanup` step (e.g., `Cleanup: DELETE /users/{UUID}`), the generator MUST implement it: generate the client method (e.g., `deleteUser(uuid)`), extract the resource ID from the response, and call cleanup in a `try/finally` block or `@AfterEach`. Omitting spec-mandated cleanup is a BLOCKER. (ref: `common/no-cleanup-pattern.md`)
- **Long Waits Policy:** Any scenario requiring a delay of > 5 seconds to verify a temporal condition (TTL expiry, cache invalidation, idempotency window) MUST be annotated with `@Disabled("Time-dependent scenario: {Wait Time}. Requires testability hook (time-travel/cache-clear) or manual execution.")` unless the specification explicitly provides a testability hook (e.g., `X-Test-Advance-Time` header, cache flush endpoint). Generating `Thread.sleep(>5000)` or `delay(>5000)` is a Quality Gate FAIL. If any test scenario's `@DisplayName` contains "cache expir", "after {N} min", or "TTL" — verify `@Disabled` on the same method. Missing `@Disabled` on time-dependent test without testability hook = Quality Gate FAIL.
- Every mutating positive test (POST/PUT/DELETE) MUST contain a **Side Effects** check: DB state (`DB:`), queue events, or Cache — via Helper method call.
- All negative tests MUST verify not only the HTTP code but also the **business error code** (`assertEquals(expectedCode, response.body.code, "error code mismatch")`).
- **No duplication:** entity creation logic — only in Helpers; data logic — only in TestData/FakerService. Inline strings in tests are FORBIDDEN.
- **Assertion DRY Rule:** If ≥3 test methods repeat an identical assertion block of ≥5 lines (e.g., HTTP status + security headers), extract it into a `@Step`-annotated Helper method (e.g., `assertSuccessfulRegistration(response)`). Inline repetition of security-header checks across tests is a DRY violation.
- **Time/Date Assertions:** Never use exact match for timestamps. Use relative matchers: `assertTrue(ChronoUnit.SECONDS.between(expected, actual) < 5, "timestamp drift")`. Never use `LocalDateTime.now()` in assertions.
- **Anti-Pattern "The Giant":** One `@Test` method MUST NOT exceed 30 lines. Extract setup into `@Step`-annotated Helper methods.
- **Anti-Pattern "The Liar":** Every `@Test` MUST contain at least one `assertEquals` or `assertTrue` evaluating the response body or side-effects.
- **Create-Order Cleanup:** If `repo-scout-report` §12 defines a create-order chain (e.g., `A → B → C`), cleanup in `@AfterEach` MUST proceed in REVERSE order (`C → B → A`). Deleting a parent before its children causes FK constraint violations and flaky tests.

## Workflow

> Generic steps (Input Check, Discovery, Plan & Gen, Translation, Compile, Verify): `_shared/api-tests-shared.md` § Workflow.

**Language-specific overrides:**
- Discovery: Glob `src/**/*Test*.kt`, `src/**/requests/**/*.kt`. Read `audit/test-plan.md` for priorities. **Style Analysis:** Glob `src/**/models/**/*.kt` for model conventions.
- Plan & Gen: Check `references/api-patterns.md` for specific logic. Assertions use JUnit 5 `assertEquals`.
- Translation: Apply mapping from `references/api-patterns.md#translation-rules`.
- Compile: `./gradlew compileTestKotlin && ./gradlew ktlintCheck`. If > 1 failed → ESCALATION.
- Smoke Run: classify `ConnectException` → generate `MockServerExtension`; `JsonMappingException` → fix DTOs (max 2 iterations); `NoSuchMethodError` → ESCALATION; TLS assertion failure → `@Disabled`.

### Escalation (3-Strike Rule)

> Full protocol: `_shared/api-tests-shared.md` § Escalation. Language note: Kotlin uses `Map<String, Any>`.

## Review Mode (`review` arg)

1. Read `src/test/**/*.kt`.
2. Check against **Protocol** + `references/api-patterns.md#architecture` + `qa-antipatterns/_index.md`.
3. Report: `⛔ Violation (ref: antipattern)` / `✅ Pass`. DO NOT EDIT.

## Fix Mode (`fix` arg)

> Full protocol: `references/fix-mode.md`. Trigger: `/api-tests fix src/test/kotlin/domain/`

## Repo-Scout Cross-References

> Full rules: `_shared/api-tests-shared.md` § Repo-Scout Cross-References.

## References

- Architecture & patterns: `references/api-patterns.md` (Architecture, Translation Rules, Coverage Matrix, Grouping)
- Code examples: `references/examples.md`
- Anti-patterns: `.claude/qa-antipatterns/_index.md` → `platform/`, `api/`, `common/`, `security/`
- Repo-scout report: `audit/repo-scout-report*.md` (§11–§15 for entity/state/nuance context)
- Anti-patterns (new): `api/eventual-consistency-writes.md`, `api/batch-partial-failure.md`

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

## Completion Contract

> Full templates: `_shared/api-tests-shared.md` § Completion Contract. Skill name: `/api-tests`, extensions: `.kt`.
