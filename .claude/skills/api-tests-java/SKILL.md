---
name: api-tests-java
description: Generates production-ready API automated tests in Java 17+ (JUnit5, Allure, AssertJ). Use when you need to cover REST endpoints with tests from test-scenarios.md or specification in Java. Do not use for Kotlin tests — use /api-tests for that.
allowed-tools: "Read Write Edit Glob Grep Bash(./gradlew*)"
agent: sdet
context: fork
---

## Recommended Flow

For best results: `/spec-audit` → `/api-isolated-tests` → `/api-tests-java`. Running directly on a specification also works — the skill will generate tests without a scenario matrix, but traceability `@Link` annotations will reference the spec instead of scenario IDs.

---

## 🔒 SYSTEM REQUIREMENTS

Before execution the agent MUST load: `.claude/protocols/gardener.md`

---

# SDET: API Automation (Java 17+)

<purpose>
Generates a complete set of Java 17+ automated tests for REST API: models, HTTP client, helpers, tests.
Scenario source — `audit/test-scenarios.md` (result of /api-isolated-tests) or specification directly.
</purpose>

## When to Use

- There is `audit/test-scenarios.md` with a scenario matrix — cover them with Java 17+ automated tests
- Need to write tests for a new endpoint from scratch in Java
- Review existing Java tests for standards compliance (`review` arg)

## Input Context (Process Isolation)

`context: fork` — you cannot see chat history before your invocation.

**Allowed inputs:** `audit/test-scenarios.md`, specification, `CLAUDE.md`, `build.gradle.kts` (or `pom.xml`), existing `src/` (style reference only).
**Forbidden:** Assumptions from "previous agent context", inventing endpoint contracts.

## Protocol

1. **Stack:** Locked in CLAUDE.md → Tech Stack (Java opt-in). Java-specific additions: Awaitility (base, not `-kotlin`), Jackson ObjectMapper as `static final`.
2. **BANNED:** `Thread.sleep`, `new HttpClient()` per test method, `Map<String, Object>` instead of DTOs, `.get()` on `CompletableFuture` without timeout, `RestAssured`, `OkHttp`, `Retrofit`, `assertThat(x).isEqualTo(y)` without `.as("description")`, manual `@AllureId`, `LocalDateTime.now()` in strict assertions. **Zero-comment policy:** `//` and `/* */` in generated code are FORBIDDEN.
2a. **Async Calls:** `CompletableFuture.get(10, TimeUnit.SECONDS)` — timeout mandatory on every `.get()` call. FORBIDDEN: `.get()` without timeout (blocks indefinitely on hung server).
2b. **Test Lifecycle:** `@BeforeEach`/`@AfterEach` for setup/teardown. FORBIDDEN: shared mutable static state between tests.
2c. **DRY API Clients:** FORBIDDEN: duplicating client method bodies to return different response types. Generate a single method returning `HttpResponse<String>` and let the test deserialize as needed.
2d. **Fail Fast in Test Clients:** FORBIDDEN: wrapping API client HTTP calls or JSON deserialization in `try/catch` blocks that swallow exceptions and return empty stub objects. Exceptions MUST propagate so the test runner reports the infrastructure failure at the point of failure, not on a later assertion. (ref: `api/silent-catch.md`)
2e. **Response Body Extraction Rule:** Use `objectMapper.readValue(response.body(), TargetDto.class)` for typed deserialization. FORBIDDEN: manual string parsing, `response.body().toString()` (that's the string itself — no metadata issue in Java, but it creates untyped coupling).
2f. **Single Serialization Framework Rule:** Use exactly one JSON framework — Jackson. All DTOs use `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` + `@JsonIgnoreProperties(ignoreUnknown = true)`. FORBIDDEN: mixing Jackson with other serialization libraries.
2g. **Time-Dependent Scenarios (TTL/Cache):** FORBIDDEN: Using `Thread.sleep` to simulate time passing. For short async state changes, use Awaitility. For long temporal conditions (e.g., 5-minute cache expiration), if the specification does NOT provide a testability hook, MUST annotate with `@Disabled("Time-dependent scenario: {Wait Time}. Requires testability hook (time-travel/cache-clear) or manual execution.")`.
2h. **Timestamp Response Validation:** Validate time-based fields with relative drift check: `assertThat(Math.abs(actual - expected)).as("timestamp drift").isLessThan(driftToleranceSec)`. FORBIDDEN: `LocalDateTime.now()` in strict equality assertions.
2i. **WireMock Integration for External Services:** When a test configures WireMock stubs, the test MUST connect the mock to the application under test via system property in `@BeforeEach` and clear it in `@AfterEach`. Disconnected WireMock (started but not referenced by the app) is a SILENT TEST BUG.
2j. **Configurable BASE_URL:** Define as static final so CI/CD can override the target server without recompilation: `private static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost:8080");` If `ConnectException` appears and no live server is available → run `/api-mocks` to generate an in-process mock, then re-run.
2k. **Eventual Consistency Writes:** If `repo-scout-report` §12 marks a write→read pair as "Eventual", FORBIDDEN to assert the read immediately after write. Use Awaitility polling with bounded timeout. (ref: `api/eventual-consistency-writes.md`)
2l. **Batch Partial Failure:** If `repo-scout-report` §12 lists batch operations, include a mixed valid+invalid input case to verify error propagation strategy (atomic rollback vs partial 207 Multi-Status). Testing only all-valid and all-invalid is INSUFFICIENT. (ref: `api/batch-partial-failure.md`)
3. **Security Headers Rule:** Verify `Content-Type`, `X-Content-Type-Options`, `Strict-Transport-Security` on every positive 2xx to detect missing security headers early — use AssertJ on `response.headers().map()`. (ref: `api/missing-security-headers.md`)
4. **Structure:**
   - `requests/`: DTOs + Request/Response objects. **ALL** DTOs mapped to snake_case JSON — apply `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` on all DTOs — omitting it on response DTOs causes silent null fields. Also add `@JsonIgnoreProperties(ignoreUnknown = true)`.
   - `helpers/`: `@Step` annotated flows (Allure — same as Kotlin).
   - `tests/`: `@Epic` (from feature/package name), `@Feature` (from endpoint name), `@Severity`, `@DisplayName`. `@AllureId` — **NOT generated**. **MANDATORY TAGS:** `@Tag("CRITICAL")` + `@Severity(SeverityLevel.CRITICAL)` for Money flows, Security/Auth, or Data integrity endpoints; `@Tag("REGRESSION")` for all others.
   - **External Integrations:** If the endpoint under test calls 3rd-party services, configure a WireMock stub in `@BeforeEach` so the test does not make real HTTP calls to external domains.
4. **Gates:** `./gradlew compileTestJava`.

## Input Source Strategy

> Shared rules: `_shared/api-tests-shared.md` § Input Source Strategy. Java upstream: `/api-isolated-tests`.

## Input Validation (Mandatory Check)

> Full 2-phase validation: `_shared/api-tests-shared.md` § Input Validation.

## Architecture Modes

> Shared structure: `_shared/api-tests-shared.md` § Architecture Modes. This skill uses `.java` extensions, `*Test.java`/`*Tests.java` naming. Shared infra in `core/src/main/java/`. No DI framework assumptions.

## Verbosity Protocol

> Shared rules: `_shared/api-tests-shared.md` § Verbosity Protocol.

**Mandatory Checks:**
```bash
grep -r "Thread.sleep\|\.get()\b\|RestAssured\|Map<String, Object>" src/test/java/
grep -rn "assertThat" src/test/java/ | grep -v "\.as("
grep -rl "new.*HttpClient\(\)" src/test/java/ | grep "Tests\.java$"
grep -rL "Strict-Transport-Security" src/test/java/*/tests/*Tests.java
```
⛔ Any match on line 1 → FAIL (BANNED pattern detected).
⛔ Any match on line 2 → FAIL (`assertThat` without `.as()` message).
⛔ Any match on line 3 → FAIL (inline `HttpClient` in test class).
⛔ Any POS-test file without HSTS check → FAIL (Protocol 3).

```bash
grep -rL "AfterEach\|finally" src/test/java/*/tests/*Tests.java
```
If `test-scenarios.md` contains `Cleanup:` for POS/L10N scenarios, every test file with POS tests MUST contain `@AfterEach` or `try/finally` → FAIL.

⛔ Any match → FAIL. **Lazy Load Anti-Pattern Fix:**
1. Read `.claude/qa-antipatterns/_index.md` — find `{category}/{name}` matching the problem
2. Read `.claude/qa-antipatterns/{category}/{name}.md` → apply Good Example → cite `(ref: {category}/{name}.md)`
3. If not found → BLOCKER, do not guess the fix

**Categories:** `platform/java/` · `api/java/` · `common/` · `security/`

**Quality Gates:**
- **Spec-Mandated Cleanup:** If the specification or test scenario explicitly declares a `Cleanup` step (e.g., `Cleanup: DELETE /users/{UUID}`), the generator MUST implement it in a `try/finally` block or `@AfterEach`. Omitting spec-mandated cleanup is a BLOCKER. (ref: `common/no-cleanup-pattern.md`)
- **Long Waits Policy:** Any scenario requiring > 5 seconds for a temporal condition MUST be annotated with `@Disabled("Time-dependent scenario: {Wait Time}. Requires testability hook (time-travel/cache-clear) or manual execution.")` unless the specification provides a testability hook. Generating `Thread.sleep(>5000)` is a Quality Gate FAIL.
- Every mutating positive test (POST/PUT/DELETE) MUST contain a **Side Effects** check: DB state (`DB:`), queue events, or Cache — via Helper method call.
- All negative tests MUST verify not only the HTTP code but also the **business error code** (`assertThat(body.getCode()).as("error code mismatch").isEqualTo(expectedCode)`).
- **No duplication:** entity creation logic — only in Helpers; data logic — only in TestData/FakerService. Inline strings in tests are FORBIDDEN.
- **Time/Date Assertions:** Never use exact match for timestamps. Use relative drift check.
- **Anti-Pattern "The Giant":** One `@Test` method MUST NOT exceed 30 lines. Extract setup into `@Step`-annotated Helper methods.
- **Anti-Pattern "The Liar":** Every `@Test` MUST contain at least one `assertThat(...)` evaluating the response body or side-effects.

## Workflow

> Generic steps (Input Check, Discovery, Plan & Gen, Translation, Compile, Verify): `_shared/api-tests-shared.md` § Workflow.

**Language-specific overrides:**
- Discovery: Glob `src/**/*Test*.java`, `src/**/requests/**/*.java`. Read `build.gradle.kts` or `pom.xml`.
- Plan & Gen: Check `references/java-api-patterns.md` for specific logic. Assertions use AssertJ `assertThat(...).as(...)`.
- Translation: Apply mapping from `references/java-api-patterns.md#translation-rules`.
- Compile: `./gradlew compileTestJava`. If > 1 failed → ESCALATION.
- Smoke Run: classify `ConnectException` → set `BASE_URL` from WireMock; `JsonMappingException` → fix DTOs; `NoSuchMethodError` → ESCALATION; TLS → `@Disabled`.

### Escalation (3-Strike Rule)

> Full protocol: `_shared/api-tests-shared.md` § Escalation. Language note: Java uses `Map<String, Object>`.

## Review Mode (`review` arg)

1. Read `src/test/**/*.java`.
2. Check against **Protocol** + `references/java-api-patterns.md#architecture` + `qa-antipatterns/_index.md`.
3. Report: `⛔ Violation (ref: antipattern)` / `✅ Pass`. DO NOT EDIT.

## Repo-Scout Cross-References

> Full rules: `_shared/api-tests-shared.md` § Repo-Scout Cross-References.

## References

- Architecture & patterns: `references/java-api-patterns.md`
- Anti-patterns: `.claude/qa-antipatterns/_index.md` → `platform/java/`, `api/java/`, `common/`, `security/`
- Repo-scout report: `audit/repo-scout-report*.md` (§11–§15 for entity/state/nuance context)

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

## Completion Contract

> Full templates: `_shared/api-tests-shared.md` § Completion Contract. Skill name: `/api-tests-java`, extensions: `.java`.

