# Shared Reference: api-tests / api-tests-java

> This file contains sections shared between `/api-tests` (Kotlin) and `/api-tests-java` (Java).
> Each SKILL.md references specific sections via `§ Section Name`.
> Language-specific differences are noted inline.

## Input Source Strategy

**Primary Source:** `audit/test-scenarios.md` — each table row becomes one automated test.
**Secondary Source:** Specification directly — if test-scenarios.md is missing.

> **Kotlin:** Primary upstream is `/api-test-cases`.
> **Java:** Primary upstream is `/api-isolated-tests`.

## Input Validation (Mandatory Check)

**CRITICAL:** Before starting generation, perform a 2-phase validation.

### Phase 1: Check test-scenarios availability (Primary Source)

```bash
[ -f audit/test-scenarios.md ] || echo "WARNING"
```

**If the file is missing:**
```text
WARNING: audit/test-scenarios.md not found. Continuing without pre-built scenarios.
```

### Phase 2: Check for table rows (protection against empty file)

```bash
grep -q "^|" audit/test-scenarios.md || echo "WARNING"
```

**If no table rows found:**
```text
WARNING: test-scenarios.md exists but contains no table rows. Continuing with empty base.
```

### If all checks pass:

- Read `audit/test-scenarios.md` — extract all table rows (each row = one automated test)

### Parsing test-scenarios.md

1. Read `audit/test-scenarios.md`
2. For each table row extract: ID, Type, Scenario, Input, Expected
3. BVA values from the Input column — transfer to the automated test EXACTLY

**If User requests an endpoint without scenarios in the table:**
```text
WARNING: No scenarios for {endpoint} in audit/test-scenarios.md. Continuing without scenarios for this endpoint.
```

## Architecture Modes

**Step 0 (Workflow Pre-flight):** Before any generation, run auto-detection (sdet.md -> Architecture Routing). Determine `ARCH_MODE = A | B`. All output paths depend on this.

### Mode A: DDD Isolated (default)

> **Kotlin:** Extensions `.kt`, test naming `*Tests.kt`. AllureId: `./gradlew assignAllureIds`.
> **Java:** Extensions `.java`, test naming `*Tests.java` / `*Test.java`. Shared infra in `core/src/main/java/`. No DI framework assumptions.

```text
src/test/{lang}/{domain}/
+-- tests/      # generated here
+-- requests/   # generated here
+-- helpers/    # generated here
```

### Mode B: Gradle Multi-Module Enterprise

```text
# Where to generate for an existing domain module:
{domain}/src/test/{lang}/{pkg}/{domain}/
+-- {sub-domain}/            # new test class here
|   +-- {Feature}Test.{ext}  # *Test.{ext} (not *Tests.{ext})
+-- TestBase.{ext}           # if not present, create

# Where to add domain-specific DTOs:
{domain}/src/main/{lang}/{pkg}/{domain}/api/
+-- {Feature}Response.{ext} / {Feature}Request.{ext}

# If adding shared DTOs used across domains:
core/src/main/{lang}/{pkg}/core/api/response/
+-- {Shared}Response.{ext}
```

> **Kotlin Mode B:** No `by inject()` — use constructor params on `TestBase` or direct instantiation. AllureId: `./gradlew checkAllureIds --clean` (NEVER manually assign `@AllureId` values).
> **Java Mode B:** No DI framework assumptions — use constructor params or static factory methods.

## Verbosity Protocol

**Code first, talk later:** Generation -> Compilation -> Post-Check -> SKILL COMPLETE -> Gardener [-> Scenario Source Improvements]. No intermediate explanations.

**FORBIDDEN:**
- "I will now create..." — just Create
- "The test covers..." — coverage goes into SKILL COMPLETE metrics
- "Let me fix..." — just Fix and Compile
- Explanation after each file — group all files -> one compilation attempt

**Allowed:**
- Compilation errors — show stderr, not description
- SKILL COMPLETE — metrics (Coverage, Compilation status)

**Post-Check:** Inline (5 lines), verification against BANNED list and Quality Gates.

## Workflow

0. **Input Check (MANDATORY):**
   - **Architecture Detection:** Run auto-detection (sdet.md -> Architecture Routing). Determine `ARCH_MODE = A | B`. Use detected mode for all output paths (see Architecture Modes section).
   - Perform 2-phase test-scenarios validation (see Input Validation above)
   - If any phase FAILs -> output WARNING and continue with available data
   - If all checks PASS -> Read `audit/test-scenarios.md`
1. **Discovery:**
   - Read `CLAUDE.md`, build file (`build.gradle.kts` or `pom.xml`).
   - Read `audit/test-scenarios.md` (Primary Source) -> extract all table rows.
   - Glob existing test and request files for context of existing patterns.
   - Read `audit/test-plan.md` (if exists) — only for determining P0/P1/P2 priorities.
   - Print Summary: N scenarios found, M endpoints in plan.
2. **Plan & Gen:**
   - **Scenario source:** table rows from `audit/test-scenarios.md`.
   - Order: by priority from test-plan.md (P0 -> P1 -> P2). If test-plan.md is missing — row by row.
   - Check language-specific patterns reference for specific logic (Auth/CRUD/Page).
   - For each table row generate one automated test:
     - Implement Input as HTTP request parameters
     - Implement Expected as assertions (HTTP status + logic)
     - Transfer BVA values from the Input column EXACTLY (boundary values MUST NOT be rounded or modified)
     - Add `@Link(name = "Scenario {ID}", url = "file://audit/test-scenarios.md")` — mandatory
   - **Phase 1:** Stateless (Validation, Auth fail).
   - **Phase 2:** 1-step setup (CRUD, simple flows).
   - **Phase 3:** Multi-step (Helpers, State transitions).
3. **Translation & Grouping:** Apply mapping from language-specific patterns reference `#translation-rules`. NEG/BVA grouping — from `#grouping-strategy`.
4. **Compile:** Run language-specific compile command. If > 1 failed compilations -> ESCALATION (see below).
4a. **Smoke Run:** `./gradlew test 2>&1 | tail -80`. Classify failures:
   - `ConnectException`/`Connection refused` -> Missing mock server. Fix -> re-compile -> re-run.
   - `JsonMappingException`/`MismatchedInputException` -> DTO bug. Fix `@JsonNaming`/field names -> re-compile -> re-run (max 2 fix iterations).
   - `NoSuchMethodError`/`ClassNotFoundException` -> Dependency mismatch -> ESCALATION.
   - Assertion failure on TLS test (`plain HTTP rejected`) -> Infra-level. Mark test `@Disabled("TLS enforcement: requires HTTPS infrastructure")`.
   - All other failures = Infrastructure-only -> **Smoke Run: PASS** (infra-blocked).
5. **Verify:** Grep BANNED patterns (see Post-Check above). Fix violations -> re-compile.

## Escalation (3-Strike Rule)

**If > 1 failed compilations on a single endpoint:**

1. STOP generation for this item. Do NOT attempt workarounds (untyped maps, reflection, custom HTTP client).

> **Kotlin:** BANNED workaround is `Map<String, Any>`. **Java:** BANNED workaround is `Map<String, Object>`.

2. Output the following block:

```text
ESCALATION: Item #{N} ({METHOD} {endpoint}) UNIMPLEMENTABLE

Problem: {specific description of technical blocker}

Attempts:
- Attempt 1: Compilation FAIL — {specific compiler error}
- Attempt 2: Compilation FAIL — {specific compiler error}

Decision required from QA Lead:
1. Exclude {endpoint} from scope (if non-critical)
2. Supplement specification with missing DTOs/schemas
3. Update project dependencies (if version conflict)

Awaiting QA Lead decision.

Status of remaining items:
- Item #{M} ({endpoint}): DONE (X tests)
- Item #{K} ({endpoint}): SKIPPED (pending blocker resolution)
```

3. EXIT with `SKILL PARTIAL` (see Completion Contract below).

## Repo-Scout Cross-References (Conditional Fallback)

**Primary source:** Use `## Test Generation Context` block from `audit/test-scenarios.md` (emitted by `/api-test-cases` Phase 6.5). This block pre-filters and documents all extracted constraints from repo-scout.

**Fallback (only if context block is absent):** If `audit/repo-scout-report*.md` exists and `## Test Generation Context` block is not in `audit/test-scenarios.md`, read sections S11-S15 directly:

| Report Section | Impact on Test Generation |
|---------------|--------------------------|
| S11 State Transition Matrix | Generate tests for each valid `From->To` transition + rejected transitions (guard failures) |
| S12 Entity & Data Model | Use create-order chain for setup/teardown; apply consistency model for assert strategy |
| S13 Behavioral Nuances | Generate conditional tests (internal vs external, search semantics, non-existent resource) |
| S14 Config & Host Context | Use test env setup for `@BeforeAll`; skip tests requiring unavailable host system |
| S15 QA Scenario Matrix | Use P0/P1/P2 priorities for generation order; respect Skip list |

## Completion Contract

### Success (Full Coverage)

```text
SKILL COMPLETE: /{skill-name}
+-- Artifacts: src/test/{lang}/**/ (requests, helpers) + tests
+-- Compilation: PASS
+-- Source: audit/test-scenarios.md (N scenarios)
+-- Context: audit/test-plan.md (P0: X endpoints, P1: Y endpoints) | "none"
+-- Coverage: N/M scenarios implemented (NN%)
+-- Traceability: @Link(scenario ID) in N/N tests (100% mandatory)
+-- BANNED check: PASS
+-- Smoke Run: PASS | FAIL (N DTO bugs fixed) | INFRA (TLS-enforcement test only)
```

### Partial (With Blockers)

```text
SKILL PARTIAL: /{skill-name}
+-- Artifacts: [{file1}.{ext} (OK), {file2}.{ext} (FAIL)]
+-- Compilation: PARTIAL (X/Y files)
+-- Source: audit/test-scenarios.md (N scenarios)
+-- Coverage: X/N scenarios implemented (NN%)
+-- Blockers: 1 UNIMPLEMENTABLE (see ESCALATION above)
+-- Traceability: @Link present in X/Y successful automated tests
+-- Status: BLOCKED, Orchestrator decision required
```

**When to use SKILL PARTIAL:**
- After 3 failed compilations on a single endpoint (Escalation)
- Technical blocker (library does not support the feature)
- Incomplete specification for one endpoint (the rest are covered)

## Scenario Source Improvements

**After the SKILL COMPLETE block, also output (if applicable):**

`1-3 concrete suggestions on what to change in /api-test-cases to prevent implementation issues
found during this run (e.g., ambiguous scenario inputs, missing cleanup steps, underdefined BVA
boundaries, incorrect HTTP codes in Expected column). Omit this section entirely if
test-scenarios.md was clear and complete.`
