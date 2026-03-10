# Fix Mode Reference (Kotlin)

**Purpose:** Surgically remediate BANNED pattern violations in existing test files without regenerating them. Preserves test logic, boundary values, and business assertions.

**Trigger:** User invokes `/api-tests fix src/test/kotlin/domain/`

**Rules:**
- Use `Read` to read the existing file fully before any edit
- Use `Edit` tool for each individual violation — one `Edit` call per fix
- Preserve ALL test logic, assertion values, @DisplayName, @AllureId, scenario IDs
- DO NOT use `Write` to overwrite the entire file
- DO NOT alter boundary values, expected status codes, or business error assertions
- DO NOT add new test methods or remove existing ones

**Workflow:**
1. **Discover violations** — run the same Mandatory Checks grep as post-check:
   ```bash
   grep -rn "Thread.sleep\|runBlocking\|delay(\|Map<String, Any>" src/test/kotlin/
   grep -rn "^\s*assert(" src/test/kotlin/ | grep -v "assertEquals\|assertTrue\|assertNotNull"
   grep -rl "HttpClient(" src/test/kotlin/ | grep "Tests\.kt$"
   grep -rL "HttpTimeout" src/test/kotlin/*/requests/*Client.kt
   ```
2. **For each violation, apply surgical Edit:**

| BANNED pattern | Safe replacement |
|---|---|
| `Thread.sleep(N)` | `await().atMost(N, SECONDS).untilAsserted { ... }` |
| `runBlocking { ... }` in test | `@Test fun test() = runTest { ... }` |
| Bare `assert(condition)` | `assertTrue(condition, "descriptive message")` |
| `Map<String, Any>` request body | Extract typed DTO class to `requests/` package |
| `HttpClient(` inside `*Tests.kt` | Move to `*Client.kt` in `requests/` package as singleton |
| Missing `HttpTimeout` in client | Add `install(HttpTimeout) { requestTimeoutMillis = 30_000 }` |

3. **Compile gate:** Run `./gradlew compileTestKotlin` after all edits. If compilation fails -> output exact error + revert the offending edit using `Edit` with original content.
4. **Scope guard:** If a violation requires adding a new class (e.g., extracting a DTO), create the file only in the correct package (`requests/` or `helpers/`). Never create files in `tests/`.
5. **3-Strike Rule:** If the same violation fails to fix after 3 Edit attempts -> output `LOOP_GUARD_TRIGGERED: Cannot fix [pattern] in [file:line] — requires manual refactoring` and skip to next violation.

**Output:**
```text
Fix Mode Report — {domain}
Files scanned: N | Violations found: M | Fixes applied: K | Manual review needed: J

Fixed:
- [file:line] Thread.sleep(2000) -> await().atMost(2, SECONDS)...
- [file:line] runBlocking -> runTest

Manual Review Required:
- [file:line] Map<String, Any> — DTO structure too complex to extract automatically
```
