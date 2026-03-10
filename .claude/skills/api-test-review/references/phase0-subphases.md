> Reference file for /api-test-review. Internal Chain-of-Thought only — never output to user.

# Phase 0 Subphases (0.1–0.3)

### Phase 0.1: Language Lock (Mandatory)

**Purpose:** Prevent cross-language pattern suggestions. Detect the primary test language and enforce strict language mode.

**Actions:**
1. **Scan input test files for language:**
   - Use `Glob` to identify test file extensions in the input path:
     - If **≥50% files are `.kt`** → activate `LANGUAGE_MODE = KOTLIN`
     - If **≥50% files are `.java`** → activate `LANGUAGE_MODE = JAVA`
     - If **mixed ratio** (close to 50/50) → log warning "Mixed language project detected; prioritize primary language"
2. **Apply strict language rules:**
   - **IF KOTLIN MODE:**
     - ✅ ALLOWED: coroutines, `runTest`, `launch`, `async`, suspend functions, scope functions (`.let`, `.apply`, `.run`, `.also`)
     - ✅ ALLOWED: Kotest assertions (if in build file), JUnit 5 assertions
     - ❌ BANNED: `CompletableFuture`, `java.util.concurrent.*` (use coroutines instead)
     - ❌ BANNED: `@BeforeClass` / `@AfterClass` (use `@BeforeAll` / `@AfterAll` with companion object)
     - ❌ BANNED: AssertJ suggestions (unless explicitly in build.gradle.kts)
     - ❌ BANNED: Awaitility (use `runTest` + `advanceUntilIdle` instead)
   - **IF JAVA MODE:**
     - ✅ ALLOWED: `CompletableFuture`, `java.util.concurrent.*`, Awaitility
     - ✅ ALLOWED: AssertJ assertions, JUnit 5 assertions
     - ❌ BANNED: coroutines, suspend functions, `runTest` (Java doesn't have these)
     - ❌ BANNED: Kotlin scope functions (`.let`, `.apply`, `.run`, `.also`)
     - ❌ BANNED: `kotlinx-coroutines-test` suggestions
3. **Log language mode:**
   ```text
   Phase 0.1 — Language Lock:
   ├─ Test files scanned: {count}
   ├─ Language mode: {KOTLIN | JAVA}
   └─ Enforcing: {language-specific rules}
   ```

### Phase 0.2: Dynamic Antipattern Loading

**Purpose:** Load antipatterns from language-specific directories to avoid irrelevant suggestions.

**Actions:**
1. **Determine antipattern directory based on LANGUAGE_MODE:**
   - **IF KOTLIN:** Read antipatterns from `.claude/qa-antipatterns/platform/` (general platform patterns apply to Kotlin)
   - **IF JAVA:** Read antipatterns from `.claude/qa-antipatterns/platform/java/` (Java-specific patterns, with fallback to general patterns)
2. **When reviewing code, reference ONLY the applicable antipattern set:**
   - Example: "Reviewing .java file → read only from `qa-antipatterns/platform/java/`"
   - Example: "Reviewing .kt file → read only from `qa-antipatterns/platform/` (unless platform/kotlin/ exists)"
3. **Antipattern files to load:**
   - **Common (both Kotlin & Java):** `security/`, `http/`, `common/`
   - **Platform-specific:**
     - Kotlin: `platform/coroutine-test-return-type.md`, `platform/flaky-sleep-tests.md`, `platform/controlled-retries.md`, `platform/no-hardcoded-timeouts.md`, `platform/no-shared-mutable-state.md`
     - Java: `platform/java/completablefuture-no-timeout.md`, `platform/java/flaky-sleep-tests.md`
4. **Implementation rule for detection:**
   - If reviewing `.kt` file → only cite patterns from `.claude/qa-antipatterns/platform/` (general or Kotlin-specific if available)
   - If reviewing `.java` file → ONLY cite patterns from `.claude/qa-antipatterns/platform/java/`; for common issues, cite from parent `platform/` as fallback if Java-specific file doesn't exist

### Phase 0.3: Contract Discovery (Optional)

**Purpose:** Locate API contracts in the repository to enable specification-driven validation in Phase 3C. Prevents false positives on DTO fields that are contractually correct.

**Actions:**
1. **Glob for contract files** in the repository root and common directories:
   - OpenAPI/Swagger: `**/*.yaml`, `**/*.yml`, `**/*.json` (filter: must contain `openapi:` or `swagger:` keyword)
   - Protobuf: `**/*.proto`
   - GraphQL: `**/*.graphql`, `**/*.graphqls`
   - Also read: `CLAUDE.md`, `README.md` (architecture/stack notes, max 50 lines each)
2. **Filter by domain** — match contracts to the domain being reviewed (e.g., if reviewing `users/` tests, prefer `users.yaml` or endpoints containing `/users`)
3. **Extract relevant definitions** (read limit: max 100 lines per contract file):
   - OpenAPI: endpoint paths, request/response schema field names and types, required/nullable flags
   - Protobuf: message field names, types, field numbers
   - GraphQL: type definitions, query/mutation field names and types
4. **Store as:** `contractContext = { spec_type, domain, endpoints: [...], schemas: [...] }`
5. **Log status:**
   ```text
   Phase 0.3 — Contract Discovery:
   ├─ OpenAPI spec: {found: path | not found}
   ├─ Protobuf: {found: path | not found}
   ├─ GraphQL: {found: path | not found}
   └─ CLAUDE.md / README.md: {loaded | not found}
   ```
6. **If no contract files found:** skip silently, proceed with heuristic-only review. Phase 3C falls back to annotation-only checks.
