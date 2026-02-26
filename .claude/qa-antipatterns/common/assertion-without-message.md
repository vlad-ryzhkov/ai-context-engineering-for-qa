# Assertion Without Message

## Why this is bad

Assertions without messages:
- On failure it is unclear what exactly was being checked
- Hard to debug in CI (only stack trace)
- Need to open the code to understand the cause
- Allure reports become useless

## Bad Example

```kotlin
// ❌ BAD: What failed? Why?
@Test
fun `user registration flow`() {
    val response = ApiHelper.apiClient.execute { RegisterRequest(payload) }

    assertEquals(201, response.code)           // AssertionError: expected 201 but was 400
    assertNotNull(response.body.userId)        // Which userId? Why null?
    assertEquals("PENDING", response.body.status)
}

// In CI logs:
// AssertionError: expected:<201> but was:<400>
// 🤷 What went wrong?
```

## Good Example

```kotlin
// ✅ GOOD: assertEquals with message
@Test
fun `user registration flow`() {
    val response = ApiHelper.apiClient.execute { RegisterRequest(payload) }

    assertEquals(201, response.code, "Registration should return 201 for valid payload")
    assertNotNull(response.body.userId, "User ID should be returned after successful registration")
    assertEquals("PENDING", response.body.status, "New user should have PENDING status until OTP verification")
}

// ✅ GOOD: Hamcrest checkAll for multiple assertions
@Test
fun `user registration flow`() {
    val response = ApiHelper.apiClient.execute { RegisterRequest(payload) }

    checkAll {
        assertEquals(201, response.code, "Registration should return 201")
        assertNotNull(response.body.userId, "User ID should be present")
        assertEquals("PENDING", response.body.status, "Status should be PENDING")
    }
}

// ✅ GOOD: Allure step with context
@Test
fun `user registration flow`() {
    step("Register new user") {
        val response = ApiHelper.apiClient.execute { RegisterRequest(payload) }

        step("Verify HTTP 201 Created") {
            assertEquals(201, response.code, "Registration should succeed")
        }

        step("Verify user ID is returned") {
            assertNotNull(response.body.userId, "User ID should be present")
        }
    }
}
```

## What to look for in code review

- `assertEquals`, `assertNotNull` without a message parameter
- Multiple assertions in a row without context
- Missing Allure `step()` in integration tests
- Assertions on nested fields without explanation of the structure

## See Also

- `common/hardcoded-test-data.md` — test data context is often missing alongside assertion messages
