---
name: api-tests
description: Generates production-ready API automated tests in Kotlin (JUnit5, Allure). Use when you need to cover REST endpoints with tests from test-scenarios.md or specification. Do not use for test case generation — use /test-cases for that.
allowed-tools: "Read Write Edit Glob Grep Bash(./gradlew*)"
agent: agents/sdet.md
context: fork
---

## Recommended Flow

For best results: `/spec-audit` → `/test-cases` → `/api-tests`. Running directly on a specification also works — the skill will generate tests without a scenario matrix, but traceability `@Link` annotations will reference the spec instead of scenario IDs.

---

## 🔒 SYSTEM REQUIREMENTS

Before execution the agent MUST load: `.claude/protocols/gardener.md`

---

# SDET: API Automation (Kotlin)

<purpose>
Generates a complete set of Kotlin automated tests for REST API: models, HTTP client, helpers, tests.
Scenario source — `audit/test-scenarios.md` (result of /test-cases) or specification directly.
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
1. **Stack:** HTTP client = Ktor `HttpClient(CIO)` initialized in the `requests/` layer (not in tests) via `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)`. JUnit5 + `@ParameterizedTest` (`junit-jupiter-params`), Awaitility, Ktor Logging (`LogLevel.ALL`), JSON Schema Validator, Faker (data generation in TestData).
2. **BANNED:** `Thread.sleep`, `delay`, `runBlocking` (use `runTest`), `HttpClient(` in `*Tests.kt` (inline HTTP in tests), manual `@AllureId`, `shouldBe` (use `assertEquals`), `LocalDateTime.now()` in strict assertions. **Zero-comment policy:** `//` and `/* */` in generated code are FORBIDDEN.
2a. **Coroutine Tests:** Preferred: `fun test(): Unit = runTest { }` from `kotlinx-coroutines-test`. If `runBlocking` is unavoidable — explicit return type required: `fun test(): Unit = runBlocking { }`. FORBIDDEN: `delay()` as timing substitute — use Awaitility.
2b. **Test Lifecycle:** `@BeforeEach`/`@AfterEach` for setup/teardown. `lateinit var` for resources requiring cleanup. **FORBIDDEN:** `@TestInstance(PER_CLASS)` with field initialization — JUnit skips class init on constructor failure.
3. **Security Headers Rule:** Every positive test (POST/PUT/DELETE with 2xx) MUST verify `Content-Type`, `X-Content-Type-Options`, `Strict-Transport-Security` via `assertEquals` on `response.headers`.
4. **Structure:**
   - `requests/`: DTOs (`@JsonNaming`) + Request objects.
   - `helpers/`: `@Step` annotated flows.
   - `tests/`: `@Epic` (from feature/package name), `@Feature` (from endpoint name), `@Severity`, `@DisplayName`. `@AllureId` — **NOT generated**: assigned manually or via utility after TMS binding. **MANDATORY TAGS:** Analyze business logic — add `@Tag("CRITICAL")` + `@Severity(SeverityLevel.CRITICAL)` for Money flows, Security/Auth, or Data integrity endpoints; add `@Tag("REGRESSION")` for all others.
   - **External Integrations:** If the endpoint under test calls 3rd-party services (payments, SMS, email providers), the test MUST configure a WireMock stub in `@BeforeEach`. Do not make real HTTP calls to external domains.
4. **Gates:** `compileTestKotlin`, `ktlintCheck`.

## Input Source Strategy

**Primary Source:** `audit/test-scenarios.md` — result of /test-cases. Each table row → automated test.
**Secondary Source:** Specification directly — if test-scenarios.md is missing.

## Input Validation (Mandatory Check)

**CRITICAL:** Before starting generation, perform a 2-phase validation.

### Phase 1: Check test-scenarios availability (Primary Source)

```bash
[ -f audit/test-scenarios.md ] || echo "WARNING"
```

**If the file is missing:**
```
⚠️ WARNING: audit/test-scenarios.md not found. Continuing without pre-built scenarios.
```

### Phase 2: Check for table rows (protection against empty file)

```bash
grep -q "^|" audit/test-scenarios.md || echo "WARNING"
```

**If no table rows found:**
```
⚠️ WARNING: test-scenarios.md exists but contains no table rows. Continuing with empty base.
```

### If all checks pass:

- Read `audit/test-scenarios.md` — extract all table rows (each row = one automated test)
- Read `audit/test-plan.md` (if exists) for generation order prioritization (P0 → P1 → P2)

### Parsing test-scenarios.md

1. Read `audit/test-scenarios.md`
2. For each table row extract: ID, Type, Scenario, Input, Expected
3. BVA values from the Input column → transfer to the automated test EXACTLY
4. Generation order: by priority from `audit/test-plan.md` (if available) or row by row

**If User requests an endpoint without scenarios in the table:**
```
⚠️ WARNING: No scenarios for {endpoint} in audit/test-scenarios.md. Continuing without scenarios for this endpoint.
```

## Verbosity Protocol

**Code first, talk later:** Generation → Compilation → Post-Check → SKILL COMPLETE. No intermediate explanations.

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
grep -r "Thread.sleep\|delay(\|runBlocking\|shouldBe\|//\|body<\|@AllureId(\|LocalDateTime.now()" src/test/kotlin/
grep -rl "HttpClient(" src/test/kotlin/ | grep "Tests\.kt$"
grep -r "Map<String, Any>" src/test/kotlin/
```
⛔ Any match → FAIL. **Lazy Load Anti-Pattern Fix:**
1. Read `.claude/qa-antipatterns/_index.md` — find `{category}/{name}` matching the problem
2. Read `.claude/qa-antipatterns/{category}/{name}.md` → apply Good Example → cite `(ref: {category}/{name}.md)`
3. If not found → BLOCKER, do not guess the fix

**Categories:** `platform/` · `api/` · `common/` · `security/`

**Quality Gates:**
- Every mutating positive test (POST/PUT/DELETE) MUST contain a **Side Effects** check: DB state (`DB:`), queue events, or Cache — via Helper method call.
- All negative tests MUST verify not only the HTTP code but also the **business error code** (`assertEquals(expectedCode, response.body.code, "error code mismatch")`).
- **No duplication:** entity creation logic — only in Helpers; data logic — only in TestData/FakerService. Inline strings in tests are FORBIDDEN.
- **Time/Date Assertions:** Never use exact match for timestamps. Use relative matchers: `assertTrue(ChronoUnit.SECONDS.between(expected, actual) < 5, "timestamp drift")`. Never use `LocalDateTime.now()` in assertions.
- **Anti-Pattern "The Giant":** One `@Test` method MUST NOT exceed 30 lines. Extract setup into `@Step`-annotated Helper methods.
- **Anti-Pattern "The Liar":** Every `@Test` MUST contain at least one `assertEquals` or `assertTrue` evaluating the response body or side-effects.

## Workflow
0. **Input Check (MANDATORY):**
   - Perform 2-phase test-scenarios validation (see Input Validation above)
   - If any phase FAILs → output ⚠️ WARNING and continue with available data
   - If all checks PASS → Read `audit/test-scenarios.md`
1. **Discovery:**
   - Read `CLAUDE.md`, `build.gradle.kts`.
   - Read `audit/test-scenarios.md` (Primary Source) → extract all table rows.
   - Glob `src/**/*Test*.kt`, `src/**/requests/**/*.kt` (for context of existing patterns).
   - Read `audit/test-plan.md` (if exists) — only for determining P0/P1/P2 priorities.
   - **Style Analysis:** Glob `src/**/models/**/*.kt`. If fields already use snake_case without `@JsonNaming` → DO NOT add `@JsonNaming` to generated models. Read `src/**/TestBase.kt` — use the same superclasses, class-level annotations, and imports.
   - Print Summary: N scenarios found, M endpoints in plan, model style: [SnakeCase/Native].
2. **Plan & Gen:**
   - **Scenario source:** table rows from `audit/test-scenarios.md`.
   - Order: by priority from test-plan.md (P0 → P1 → P2). If test-plan.md is missing — row by row.
   - Check `references/api-patterns.md` for specific logic (Auth/CRUD/Page).
   - For each table row generate one automated test:
     - Implement Input as HTTP request parameters
     - Implement Expected as assertions (HTTP status + logic)
     - Transfer BVA values from the Input column EXACTLY (boundary values MUST NOT be rounded or modified)
     - Add `@Link(name = "Scenario {ID}", url = "file://audit/test-scenarios.md")` — mandatory
   - **Phase 1:** Stateless (Validation, Auth fail).
   - **Phase 2:** 1-step setup (CRUD, simple flows).
   - **Phase 3:** Multi-step (Helpers, State transitions).
3. **Translation & Grouping:** Apply mapping from `references/api-patterns.md#translation-rules`. NEG/BVA grouping — from `api-patterns.md#grouping-strategy`.
4. **Compile:** `./gradlew compileTestKotlin && ./gradlew ktlintCheck`. If > 1 failed compilations → ESCALATION (see below)
5. **Verify:** Grep BANNED patterns (see Post-Check above). Fix violations → re-compile.

### Escalation (3-Strike Rule)

**If > 1 failed compilations on a single endpoint:**

1. STOP generation for this item. Do NOT attempt workarounds (`Map<String, Any>`, reflection, custom HTTP client).
2. Output the following block:

```
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
1. Read `src/test/**/*.kt`.
2. Check against **Protocol** + `references/api-patterns.md#architecture` + `qa-antipatterns/_index.md`.
3. Report: `⛔ Violation (ref: antipattern)` / `✅ Pass`. DO NOT EDIT.

## References
- Architecture & patterns: `references/api-patterns.md` (Architecture, Translation Rules, Coverage Matrix, Grouping)
- Code examples: `references/examples.md`
- Anti-patterns: `.claude/qa-antipatterns/_index.md` → `platform/`, `api/`, `common/`, `security/`

## Completion Contract

### Success (Full Coverage)

```
✅ SKILL COMPLETE: /api-tests
├─ Artifacts: src/main/kotlin/**/ (requests, helpers, config) + src/test/kotlin/autotests/**/ (tests)
├─ Compilation: PASS
├─ Source: audit/test-scenarios.md (N scenarios)
├─ Context: audit/test-plan.md (P0: X endpoints, P1: Y endpoints) | "none"
├─ Coverage: N/M scenarios implemented (NN%)
├─ Traceability: @Link(scenario ID) in N/N tests (100% mandatory)
└─ BANNED check: PASS
```

### Partial (With Blockers)

```
⚠️ SKILL PARTIAL: /api-tests
├─ Artifacts: [{file1}.kt (✅), {file2}.kt (❌)]
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
