---
name: api-tests-java
description: Generates production-ready API automated tests in Java 17+ (JUnit5, Allure, AssertJ). Use when you need to cover REST endpoints with tests from test-scenarios.md or specification in Java. Do not use for Kotlin tests — use /api-tests for that.
allowed-tools: "Read Write Edit Glob Grep Bash(./gradlew*)"
agent: agents/sdet.md
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

**Primary Source:** `audit/test-scenarios.md` — result of /api-isolated-tests. Each table row → automated test.
**Secondary Source:** Specification directly — if test-scenarios.md is missing.

## Input Validation (Mandatory Check)

**CRITICAL:** Before starting generation, perform a 2-phase validation.

### Phase 1: Check test-scenarios availability (Primary Source)

```bash
[ -f audit/test-scenarios.md ] || echo "WARNING"
```

**If the file is missing:**
```text
⚠️ WARNING: audit/test-scenarios.md not found. Continuing without pre-built scenarios.
```

### Phase 2: Check for table rows (protection against empty file)

```bash
grep -q "^|" audit/test-scenarios.md || echo "WARNING"
```

**If no table rows found:**
```text
⚠️ WARNING: test-scenarios.md exists but contains no table rows. Continuing with empty base.
```

### If all checks pass:

- Read `audit/test-scenarios.md` — extract all table rows (each row = one automated test)

### Parsing test-scenarios.md

1. Read `audit/test-scenarios.md`
2. For each table row extract: ID, Type, Scenario, Input, Expected
3. BVA values from the Input column → transfer to the automated test EXACTLY

**If User requests an endpoint without scenarios in the table:**
```text
⚠️ WARNING: No scenarios for {endpoint} in audit/test-scenarios.md. Continuing without scenarios for this endpoint.
```

## Architecture Modes

**Step 0 (Workflow Pre-flight):** Before any generation, run auto-detection (sdet.md → Architecture Routing). Determine `ARCH_MODE = A | B`. All output paths depend on this.

### Mode A: DDD Isolated (default)

```
src/test/java/{domain}/
├── tests/      # generated here  (*Tests.java)
├── requests/   # generated here
└── helpers/    # generated here
```

### Mode B: Gradle Multi-Module Enterprise

```
# Where to generate for an existing domain module:
{domain}/src/test/java/{pkg}/{domain}/
├── {sub-domain}/           # new test class here
│   └── {Feature}Test.java  # *Test.java (not *Tests.java)
└── TestBase.java           # if not present, create

# Where to add domain-specific DTOs:
{domain}/src/main/java/{pkg}/{domain}/api/
└── {Feature}Response.java / {Feature}Request.java

# If adding shared DTOs used across domains:
core/src/main/java/{pkg}/core/api/response/
└── {Shared}Response.java
```

Shared infra in `core/src/main/java/`. No DI framework assumptions — use constructor params or static factory methods.

## Verbosity Protocol

**Code first, talk later:** Generation → Compilation → Post-Check → SKILL COMPLETE → Gardener [→ Scenario Source Improvements]. No intermediate explanations.

**FORBIDDEN:**
- "I will now create..." — just Create
- "The test covers..." — coverage goes into SKILL COMPLETE metrics
- "Let me fix..." — just Fix and Compile
- Explanation after each file — group all files → one compilation attempt

**Allowed:**
- Compilation errors — show stderr, not description
- SKILL COMPLETE — metrics (Coverage, Compilation status)

**Post-Check:** Inline (5 lines), verification against BANNED list and Quality Gates.

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

0. **Input Check (MANDATORY):**
   - **Architecture Detection:** Run auto-detection (sdet.md → Architecture Routing). Determine `ARCH_MODE = A | B`. Use detected mode for all output paths (see Architecture Modes section).
   - Perform 2-phase test-scenarios validation (see Input Validation above)
   - If any phase FAILs → output ⚠️ WARNING and continue with available data
   - If all checks PASS → Read `audit/test-scenarios.md`
1. **Discovery:**
   - Read `CLAUDE.md`, `build.gradle.kts` (or `pom.xml`).
   - Read `audit/test-scenarios.md` (Primary Source) → extract all table rows.
   - Glob `src/**/*Test*.java`, `src/**/requests/**/*.java` (for context of existing patterns).
   - Read `audit/test-plan.md` (if exists) — only for determining P0/P1/P2 priorities.
   - Print Summary: N scenarios found, M endpoints in plan.
2. **Plan & Gen:**
   - **Scenario source:** table rows from `audit/test-scenarios.md`.
   - Order: by priority from test-plan.md (P0 → P1 → P2). If test-plan.md is missing — row by row.
   - Check `references/java/api-patterns.md` for specific logic (Auth/CRUD/Page).
   - For each table row generate one automated test:
     - Implement Input as HTTP request parameters
     - Implement Expected as AssertJ assertions (HTTP status + logic)
     - Transfer BVA values from the Input column EXACTLY (boundary values MUST NOT be rounded or modified)
     - Add `@Link(name = "Scenario {ID}", url = "file://audit/test-scenarios.md")` — mandatory
   - **Phase 1:** Stateless (Validation, Auth fail).
   - **Phase 2:** 1-step setup (CRUD, simple flows).
   - **Phase 3:** Multi-step (Helpers, State transitions).
3. **Translation & Grouping:** Apply mapping from `references/java/api-patterns.md#translation-rules`. NEG/BVA grouping — from `api-patterns.md#grouping-strategy`.
4. **Compile:** `./gradlew compileTestJava`. If > 1 failed compilations → ESCALATION (see below)
4a. **Smoke Run:** `./gradlew test 2>&1 | tail -80`. Classify failures:
   - `ConnectException`/`Connection refused` → 🐛 Missing mock server. Set `BASE_URL` system property from WireMock port. Do NOT report as infra-blocked — fix → re-compile → re-run.
   - `JsonMappingException`/`MismatchedInputException`/`UnrecognizedPropertyException` → 🐛 DTO bug. Fix `@JsonNaming`/field names → re-compile → re-run (max 2 fix iterations).
   - `NoSuchMethodError`/`ClassNotFoundException` → Dependency mismatch → ESCALATION.
   - Assertion failure on TLS test (`plain HTTP rejected`) → ⚠️ Infra-level. Mark test `@Disabled("TLS enforcement: requires HTTPS infrastructure")`.
   - All other failures = Infrastructure-only → **Smoke Run: PASS** (infra-blocked).
5. **Verify:** Grep BANNED patterns (see Post-Check above). Fix violations → re-compile.

### Escalation (3-Strike Rule)

**If > 1 failed compilations on a single endpoint:**

1. STOP generation for this item. Do NOT attempt workarounds (`Map<String, Object>`, reflection, custom HTTP client).
2. Output the following block:

```text
🚨 ESCALATION: Item #{N} ({METHOD} {endpoint}) UNIMPLEMENTABLE

Problem: {specific description of technical blocker}

Attempts:
- Attempt 1: Compilation FAIL — {specific compiler error}
- Attempt 2: Compilation FAIL — {specific compiler error}

Decision required from QA Lead:
1. Exclude {endpoint} from scope (if non-critical)
2. Supplement specification with missing DTOs/schemas
3. Update project dependencies (if version conflict)

⏸️ Awaiting QA Lead decision.

Status of remaining items:
- Item #{M} ({endpoint}): ✅ DONE (X tests)
- Item #{K} ({endpoint}): ⏩ SKIPPED (pending blocker resolution)
```

3. EXIT with `⚠️ SKILL PARTIAL` (see Completion Contract below).

## Review Mode (`review` arg)

1. Read `src/test/**/*.java`.
2. Check against **Protocol** + `references/java/api-patterns.md#architecture` + `qa-antipatterns/_index.md`.
3. Report: `⛔ Violation (ref: antipattern)` / `✅ Pass`. DO NOT EDIT.

## Repo-Scout Cross-References

If `audit/repo-scout-report*.md` exists, read sections §11–§15 for test generation context:

| Report Section | Impact on Test Generation |
|---------------|--------------------------|
| §11 State Transition Matrix | Generate tests for each valid `From→To` transition + rejected transitions |
| §12 Entity & Data Model | Use create-order chain for setup/teardown; apply consistency model for assert strategy |
| §13 Behavioral Nuances | Generate conditional tests (internal vs external, search semantics) |
| §14 Config & Host Context | Use test env setup for `@BeforeAll`; skip tests requiring unavailable host system |
| §15 QA Scenario Matrix | Use P0/P1/P2 priorities for generation order; respect Skip list |

## References

- Architecture & patterns: `references/java/api-patterns.md`
- Anti-patterns: `.claude/qa-antipatterns/_index.md` → `platform/java/`, `api/java/`, `common/`, `security/`
- Repo-scout report: `audit/repo-scout-report*.md` (§11–§15 for entity/state/nuance context)

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

## Completion Contract

### Success (Full Coverage)

```text
✅ SKILL COMPLETE: /api-tests-java
├─ Artifacts: src/test/java/**/ (requests, helpers) + tests
├─ Compilation: PASS
├─ Source: audit/test-scenarios.md (N scenarios)
├─ Context: audit/test-plan.md (P0: X endpoints, P1: Y endpoints) | "none"
├─ Coverage: N/M scenarios implemented (NN%)
├─ Traceability: @Link(scenario ID) in N/N tests (100% mandatory)
├─ BANNED check: PASS
└─ Smoke Run: PASS | FAIL (N DTO bugs fixed) | INFRA (TLS-enforcement test only)
```

### Partial (With Blockers)

```text
⚠️ SKILL PARTIAL: /api-tests-java
├─ Artifacts: [{file1}.java (✅), {file2}.java (❌)]
├─ Compilation: PARTIAL (X/Y files)
├─ Source: audit/test-scenarios.md (N scenarios)
├─ Coverage: X/N scenarios implemented (NN%)
├─ Blockers: 1 UNIMPLEMENTABLE (see ESCALATION above)
├─ Traceability: @Link present in X/Y successful automated tests
└─ Status: BLOCKED, Orchestrator decision required
```

**When to use SKILL PARTIAL:**
- After 3 failed compilations on a single endpoint (Escalation)
- Technical blocker (library does not support the feature)
- Incomplete specification for one endpoint (the rest are covered)

**After the SKILL COMPLETE block, also output (if applicable):**

## 💡 Scenario Source Improvements (Gardener)

`1–3 concrete suggestions on what to change in /api-test-cases to prevent implementation issues
found during this run (e.g., ambiguous scenario inputs, missing cleanup steps, underdefined BVA
boundaries, incorrect HTTP codes in Expected column). Omit this section entirely if
test-scenarios.md was clear and complete.`
