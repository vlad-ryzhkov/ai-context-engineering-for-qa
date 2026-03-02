# Review Examples (Action-First Format)

## Example 1: CRITICAL Security Issue

```markdown
🔴 CRITICAL: Hardcoded JWT token exposed
📍 src/test/kotlin/users/tests/UserTests.kt:45
Hardcoded tokens can be accidentally committed to version control. Use environment variables or test fixtures instead, so tokens rotate with implementation changes (not scattered across 50 tests).

Fix:
\`\`\`kotlin
// ❌ BEFORE
private val token = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

// ✅ AFTER
private val token = AuthFixture.generateToken()  // or System.getenv("API_TOKEN") ?: error("Missing API_TOKEN")
\`\`\`
```

---

## Example 2: MAJOR Architecture Issue

```markdown
🟠 MAJOR: DTO declared inline in test class (breaks reusability)
📍 src/test/kotlin/users/tests/UserTests.kt:12–18
Inline DTOs can't be reused across test files. When API contract changes, you'll update the same DTO in 10 different places instead of one. Move to dedicated requests/responses package.

Fix:
\`\`\`kotlin
// ❌ BEFORE
class UserTests {
    data class UserRequest(val name: String, val email: String)

    @Test
    fun testCreateUser() {
        val request = UserRequest("Alice", "alice@example.com")
        ...
    }
}

// ✅ AFTER
// File: src/test/kotlin/users/requests/UserRequest.kt
@JsonNaming(SnakeCaseStrategy::class)
data class UserRequest(val name: String, val email: String)

// File: src/test/kotlin/users/tests/UserTests.kt
import users.requests.UserRequest

@Test
fun testCreateUser() {
    val request = UserRequest("Alice", "alice@example.com")
    ...
}
\`\`\`
```

---

## Example 3: MAJOR HTTP Validation Issue

```markdown
🟠 MAJOR: Blurry HTTP status assertion (range instead of exact code)
📍 src/test/kotlin/orders/tests/OrderApiTests.kt:42, 58, 71 (and 3 more instances)
Range checks (200..299) hide bugs: endpoint returns 200 (OK) instead of 201 (Created), but test still passes. Assert exact status codes per API contract.

Fix:
\`\`\`kotlin
// ❌ BEFORE
response.status should { it in 200..299 }
// ✅ AFTER: exact code per scenario
response.status.shouldBe(201)  // POST → Created
\`\`\`
```

---

## Example 4: MAJOR Testing Quality Issue

```markdown
🟠 MAJOR: Missing Allure @Step annotations (no observability in reports)
📍 src/test/kotlin/auth/tests/LoginTests.kt:15–35 (test method body)
Without @Step, Allure report shows only generic test name. Developers can't see test flow in dashboards or CI/CD logs.

Fix:
\`\`\`kotlin
// ❌ BEFORE
@Test
fun testLoginWithValidCredentials() {
    val response = client.login("user@example.com", "password123")
    response.status.shouldBe(200)
    response.body.token.shouldNotBeBlank()
}

// ✅ AFTER
@Test
fun testLoginWithValidCredentials() = runTest {
    step("Send login request") {
        val response = client.login("user@example.com", "password123")
        response.status.shouldBe(200)
        Allure.addAttachment("Login Response", "application/json", response.body.toString())
    }

    step("Verify token in response") {
        response.body.token.shouldNotBeBlank()
    }
}
\`\`\`
```

---

## Example 5: Full Report (Multiple Issues)

```markdown
# API Test Review Report: users

## 🔴 CRITICAL Issues

🔴 CRITICAL: Hardcoded API token in test data
📍 src/test/kotlin/users/helpers/UserTestData.kt:12
This token can be accidentally committed to the repository. Use AuthFixture.generateToken() to generate fresh tokens per test.

Fix:
\`\`\`kotlin
// ❌ BEFORE
val defaultToken = "Bearer eyJhbGc..."

// ✅ AFTER
val defaultToken = AuthFixture.generateToken()
\`\`\`

---

## 🟠 MAJOR Issues

🟠 MAJOR: Blurry HTTP status codes (ranges instead of exact)
📍 src/test/kotlin/users/tests/UserApiTests.kt:42, 58, 71 (and 3 more instances)
Range checks (200..299) hide bugs where endpoint returns 200 instead of 201. Assert exact codes per contract.

Fix:
\`\`\`kotlin
// ❌ BEFORE
response.status should { it in 200..299 }
// ✅ AFTER
response.status.shouldBe(201)  // Created
\`\`\`

---

🟠 MAJOR: Missing Allure @Step annotations
📍 src/test/kotlin/users/tests/UserApiTests.kt:15–35 (6 test methods)
Without @Step, reports show only generic test names. Add steps to show test flow in Allure dashboard.

Fix:
\`\`\`kotlin
@Test
fun testCreateUser() = runTest {
    step("Create user with valid email") {
        val response = userClient.createUser("alice@example.com")
        response.status.shouldBe(201)
    }

    step("Verify user ID in response") {
        response.body.id.shouldNotBeBlank()
    }
}
\`\`\`

---

## Test Overview

- **Files analyzed:** 6
- **Language mode:** Kotlin
- **Total tests:** 23
- **Verdict:** 🔴 NEEDS FIXES

## ✅ Passing Categories

- Architecture: No issues found
- Test Quality: No issues found

## 📝 Summary

1 CRITICAL (hardcoded token) + 2 MAJOR issues (blurry assertions, missing Allure). Fix these before merging. CRITICAL blocks release validation; MAJORs block proper CI/CD observability.
```

---

## Example 6: Report with Zero Issues (Clean Pass)

```markdown
# API Test Review Report: auth

## Test Overview

- **Files analyzed:** 4
- **Language mode:** Kotlin
- **Total tests:** 18
- **Verdict:** ✅ PASS

## ✅ Passing Categories

- Security: No issues found
- Architecture: No issues found
- HTTP Validation: No issues found
- Allure Integration: No issues found
- Test Quality: No issues found

## 📝 Summary

All tests meet baseline standards. Well-organized, exact assertions, proper Allure instrumentation, secure test data. Ready to merge.
```

---

## Key Patterns to Recognize

### ✅ Confidence ≥ 80 (Report)
- Hardcoded secrets, API keys, PII
- Blurry HTTP assertions (ranges, < operators)
- Missing @Step annotations (blocks observability)
- Inline DTOs (breaks reusability)
- Connection leaks (.use{} missing)

### ❌ Confidence < 80 (Ignore)
- Style preferences (if/else vs when)
- Variable naming conventions
- Minor inefficiencies (could use .first() instead of [0])
- Context-dependent cleanup strategies
- Typos in test names
