# Execution Flow: 7-Phase Review

## Phase 1: Scope Definition
1. Accept file/directory path from user (e.g., `src/test/kotlin/domain/users/tests/`)
2. List all test files (*.kt, *.java)
3. Confirm scope: number of files, approximate lines of code

## Phase 2: Security Audit
Scan for hardcoded secrets, PII, and unsafe data patterns:

**BANNED PATTERNS:**
- Hardcoded JWT tokens, API keys, passwords
- Hardcoded environment-specific URLs (production, staging)
- Real PII in test data (names, emails, SSNs)
- Silent exception handlers `try { } catch (e: Exception) { }`

**CHECK RULES:**
- ✅ All secrets use `System.getenv()` or config providers
- ✅ Test data generated with DataFaker or placeholders
- ✅ No real user data in fixtures or test files
- ✅ Dummy data (test@example.com, 12345, user123) is correctly recognized as SAFE. Do not flag placeholder strings as PII.

**Output Format (Phase 2):**
```text
🔐 Security Audit
├─ Hardcoded Secrets: [file:line] [CRITICAL/PASS]
├─ PII in Test Data: [file:line] [CRITICAL/PASS]
├─ Safe Exception Handling: [PASS/Issues found]
└─ Verdict: [🔴 CRITICAL / 🟡 MINOR / ✅ PASS]
```

## Phase 3: Architecture Audit

### 3A. Data Transfer Object (DTO) Isolation

**RULE:** All DTOs must be in dedicated packages, never declared inside test classes.

**CHECK:**
```kotlin
// ✅ CORRECT: src/test/kotlin/domain/models/requests/UserRequest.kt
package domain.models.requests
@JsonNaming(SnakeCaseStrategy::class)
data class UserRequest(val firstName: String, val email: String)

// ❌ BANNED: Inside test class
class UserTests {
    data class UserRequest(...)  // Duplicate, not reusable
}
```

**Output Format:**
- List all DTOs found inline in test classes → 🟠 MAJOR
- Verify all DTOs use `@JsonNaming(SnakeCaseStrategy::class)` → 🟡 MINOR if missing
- Check structure: `src/test/kotlin/domain/models/requests/`, `models/responses/` → 🟡 MINOR if misplaced

### 3B. HTTP Client Structure

**RULE:** Encourage Builder or DSL patterns. Avoid inline HTTP calls in tests.

**CHECK:**
- Dedicated HTTP client class with suspend functions
- Builder/DSL pattern (Ktor Client, Retrofit)
- Base URL injection (not hardcoded)
- Content-Type and serialization set consistently

### 3C. Contract-Driven Testing

**RULE:** Validate against OpenAPI specification. Use strict serialization.

**CHECK:**
- ✅ Jackson configured with `@JsonNaming(SnakeCaseStrategy::class)`
- ✅ No `ignoreUnknownKeys = true` (unless explicitly required)
- ✅ DTO fields match API specification exactly

**Output Format (Phase 3):**
```text
🏗️  Architecture Audit
├─ DTO Isolation: [PASS / Issues]
│  ├─ Inline in test classes: [count] → MAJOR
│  ├─ Missing @JsonNaming: [count] → MINOR
│  └─ Correct structure: [count] → PASS
├─ HTTP Client Design: [PASS / Issues]
│  ├─ Client classes: [PASS / missing]
│  ├─ DSL/Builder usage: [PASS / inline calls detected]
│  └─ Base URL injection: [PASS / hardcoded]
├─ Contract Compliance: [PASS / Issues]
└─ Verdict: [🔴 CRITICAL / 🟠 MAJOR / ✅ PASS]
```

## Phase 4: Kotlin Idioms & Quality

### 4A. Async/Coroutine Patterns

**RULE:** Use `runTest {}` from kotlinx-coroutines-test. NO `Thread.sleep()`.

**BANNED:**
```kotlin
// ❌ BANNED
Thread.sleep(1000)
Thread.sleep(2000)
delay(500)  // in non-test context
```

**CORRECT:**
```kotlin
// ✅ CORRECT
@Test
fun testCreateUser() = runTest {
    val response = userApi.createUser(userRequest)
    response.status.shouldBe(201)
}

// ✅ CORRECT: Use Awaitility for timing-dependent logic
awaitility.await()
    .atMost(Duration.ofSeconds(5))
    .untilAsserted { /* assertion */ }
```

### 4B. Collections & Scope Functions

**RULE:** Replace loops with functional style. Use `apply`, `let`, `also` for object setup.

**BANNED:**
```kotlin
// ❌ POOR: Verbose loops
val users = mutableListOf<User>()
for (u in allUsers) {
    if (u.status == "active") {
        users.add(u.copy(lastSeen = now()))
    }
}

// ❌ POOR: Step-by-step assignment
val request = UserRequest("John", "Doe")
request.email = "john@example.com"
request.department = "QA"
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Functional + scope functions
val users = allUsers
    .filter { it.status == "active" }
    .map { it.copy(lastSeen = now()) }

// ✅ CORRECT: Builder setup with `apply`
val request = UserRequest("John", "Doe").apply {
    email = "john@example.com"
    department = "QA"
}
```

**Output Format (Phase 4):**
```text
📝 Kotlin Idioms & Quality
├─ Async/Coroutines: [PASS / Issues]
│  ├─ Thread.sleep() calls: [count] → MAJOR
│  ├─ runTest {} usage: [PASS / missing]
│  └─ No blocking delays in tests: [PASS / found]
├─ Collections & Scope Functions: [PASS / Issues]
│  ├─ Verbose loops: [count] → MINOR
│  ├─ Functional style usage: [percentage] → [PASS / improve]
│  └─ Scope functions (apply/let): [PASS / rarely used]
└─ Verdict: [🟠 MAJOR / 🟡 MINOR / ✅ PASS]
```

## Phase 5: Test Quality & DRY

### 5A. Parameterized Testing

**RULE:** Same test logic with multiple datasets → `@ParameterizedTest`.

**BANNED:**
```kotlin
// ❌ POOR: Code duplication
@Test
fun testStatus200() { /* test */ }
@Test
fun testStatus400() { /* same test */ }
@Test
fun testStatus401() { /* same test */ }
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Parameterized (JUnit 5)
@ParameterizedTest
@CsvSource(
    "200, true",
    "400, false",
    "401, false"
)
fun testResponseStatusAndValidity(status: Int, isValid: Boolean) {
    // Test logic once
}

// ✅ CORRECT: Data Driven (Kotest)
data class StatusTest(val status: Int, val isValid: Boolean)

class ApiTests : StringSpec({
    "validate response statuses" {
        withData(
            nameFn = { "Status ${it.status} should be valid: ${it.isValid}" },
            StatusTest(200, true),
            StatusTest(400, false),
            StatusTest(401, false)
        ) { data ->
            // Test logic once
        }
    }
})
```

### 5B. Helper Extraction (DRY)

**RULE:** 3+ repeated lines → extract to helper or base class.

**CHECK:**
- Identify repeated assertion patterns (3+ occurrences) → suggest extraction
- Check for common setup logic (3+ tests) → suggest base class helper
- Duplicated test data creation → suggest factory functions

### 5C. Test Data Builders (Fixtures)

**RULE:** Use builders or factory functions for test data, not static objects with mutation.

**BANNED:**
```kotlin
// ❌ POOR: Static shared mutable object
object UserTestData {
    val user = UserRequest("John", "john@example.com")
}
// Tests mutate it: UserTestData.user.email = "different@example.com"
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Builder or factory function
fun createUserRequest(
    firstName: String = faker.name.firstName(),
    email: String = faker.internet.email()
): UserRequest =
    UserRequest(firstName = firstName, email = email)
```

**Output Format (Phase 5):**
```text
✨ Test Quality & DRY
├─ Parameterization: [count] tests identified for consolidation → [MAJOR/MINOR]
├─ Helper Extraction: [count] repeated patterns → suggest extraction
├─ Test Data Builders: [PASS / Issues]
│  ├─ Static shared objects: [count] → MAJOR
│  ├─ Factory functions: [PASS / missing]
│  └─ DataFaker usage: [PASS / not used]
└─ Verdict: [🟠 MAJOR / 🟡 MINOR / ✅ PASS]
```

## Phase 6: HTTP Validation Rules

### 6A. Strict Status Code Checks

**RULE:** Never use range checks. Validate exact status codes per scenario.

**BANNED:**
```kotlin
// ❌ BANNED: Blurry checks
response.status should { it in 200..299 }  // Misses bugs
response.status should { it < 400 }        // Hides 201 vs 200 difference
assertThat(response.statusCode()).isBetween(200, 299)
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Exact checks
response.status.shouldBe(201)  // Created
response.status.shouldBe(400)  // Bad Request
response.status.shouldBe(500)  // Server Error
```

### 6B. Response Body Validation

**RULE:** Always parse and validate body, not just status code.

**BANNED:**
```kotlin
// ❌ POOR: Only status, ignore body
response.status.shouldBe(201)
// What if response is malformed? Unknown.
```

**CORRECT:**
```kotlin
// ✅ CORRECT
val user: UserResponse = response.body()
user.id.shouldNotBeBlank()
user.email.shouldBe(requestEmail)
user.createdAt.shouldNotBeNull()
```

### 6C. Error Response Validation

**RULE:** Negative tests must assert error structure.

**BANNED:**
```kotlin
// ❌ POOR: Ignore error structure
response.status.shouldBe(400)
// What does the error look like? Who knows?
```

**CORRECT:**
```kotlin
// ✅ CORRECT
val error: ErrorResponse = response.body()
error.code.shouldBe("DUPLICATE_EMAIL")
error.message.shouldContain("already exists")
error.fields.shouldContain("email")
```

### 6D. Test Cleanup & Independence

**RULE:** Every test must clean up after itself. No order-dependent tests.

**BANNED:**
```kotlin
// ❌ POOR: No cleanup, tests depend on execution order
@Test
fun test1() {
    val userId = api.createUser(...).id
    // userId remains in DB, test2 depends on it
}
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Cleanup in @AfterEach
@AfterEach
fun cleanup() {
    api.deleteUser(createdUserId)
}

// Or: Use transaction rollback (if DB-backed)
@Transactional
@Test
fun testCreateUser() { /* auto-rollback */ }
```

**Output Format (Phase 6):**
```text
🔗 HTTP Validation Rules
├─ Strict Status Codes: [PASS / Issues]
│  ├─ Blurry checks detected: [count] → MAJOR
│  └─ Exact assertions: [count] → PASS
├─ Response Body Validation: [PASS / Issues]
│  ├─ Tests checking only status: [count] → MAJOR
│  └─ Body parsing: [PASS / missing]
├─ Error Response Validation: [PASS / Issues]
├─ Test Cleanup & Independence: [PASS / Issues]
│  ├─ Missing @AfterEach: [count] → MAJOR
│  ├─ Order-dependent tests: [count] → MAJOR
│  └─ Cleanup present: [count] → PASS
└─ Verdict: [🟠 MAJOR / 🟡 MINOR / ✅ PASS]
```

## Phase 7: Allure Integration & Logging

### 7A. Traceability with Allure @Step

**RULE:** Every significant action must be wrapped in `@Step` or `step {}`.

**BANNED:**
```kotlin
// ❌ POOR: No steps, raw assertion
@Test
fun testUserCreation() {
    val user = api.createUser(UserRequest("test@example.com"))
    assert(user != null)  // No context in Allure report
}
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Step-wrapped actions
@Step("Create user with email {email}")
suspend fun createUser(email: String): UserResponse {
    val request = UserRequest(email = email)
    return api.createUser(request)
}

@Test
fun testUserCreation() = runTest {
    step("Prepare test data") {
        val email = faker.internet.email()
        val user = createUser(email)

        step("Verify user exists in database") {
            val dbUser = userRepository.findByEmail(email)
            dbUser.shouldNotBeNull()
        }
    }
}
```

### 7B. Assertion Messages (Semantic)

**RULE:** Use assertion libraries with clear error messages. Never raw `assertTrue`.

**BANNED:**
```kotlin
// ❌ POOR: No context
assert(response.status == 201)           // Fail: "assertion failed"
assertTrue(response.status == 201)       // Fail: "assertion failed"
assertEquals(201, response.status)       // Better, but no semantic meaning
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Kotest or StriKt
response.status.shouldBe(201)      // On fail: "Expected 201 but got 200"
response.body.email.shouldBe(email) // On fail: shows diff

// ✅ CORRECT: AssertJ with .as()
assertThat(response.status)
    .as("User creation response status")
    .isEqualTo(201)
```

### 7C. Request/Response Logging

**RULE:** Capture HTTP calls for debugging. Log to Allure on failure. Ensure sensitive headers (Authorization, Cookies, x-api-key) are masked before writing to Allure reports.

**BANNED:**
```kotlin
// ❌ POOR: Silent HTTP calls
val response = httpClient.post(url)
// If test fails, no visibility into the actual request/response

// ❌ POOR: Logging without masking sensitive headers
allureReporter.logRequest(request)  // Exposes Authorization token in plain text
```

**CORRECT:**
```kotlin
// ✅ CORRECT: Interceptor logs to Allure with masked headers
class HttpLoggingInterceptor(val allureReporter: AllureReporter) {
    private fun maskSensitiveHeaders(headers: Map<String, String>): Map<String, String> {
        val sensitiveKeys = setOf("authorization", "cookie", "x-api-key", "x-auth-token")
        return headers.mapValues { (key, value) ->
            if (key.lowercase() in sensitiveKeys) "***MASKED***" else value
        }
    }

    override suspend fun process(request: HttpRequest): HttpResponse {
        val maskedHeaders = maskSensitiveHeaders(request.headers)
        allureReporter.logRequest(request.copy(headers = maskedHeaders))
        val response = proceed(request)
        allureReporter.logResponse(response)
        return response
    }
}
```

**Output Format (Phase 7):**
```text
📊 Allure Integration & Logging
├─ @Step Annotations: [PASS / Issues]
│  ├─ Tests without steps: [count] → MAJOR
│  ├─ Covered actions: [count] → PASS
│  └─ % coverage: [percentage]
├─ Assertion Messages: [PASS / Issues]
│  ├─ Raw assert/assertTrue: [count] → MAJOR
│  ├─ Semantic assertions (shouldBe/assertThat): [count] → PASS
│  └─ Messages present: [percentage]
├─ HTTP Logging: [PASS / Issues]
│  └─ Interceptor logging: [PASS / missing]
└─ Verdict: [🟠 MAJOR / 🟡 MINOR / ✅ PASS]
```
