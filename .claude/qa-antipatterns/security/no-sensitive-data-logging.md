# No Sensitive Data Logging

**Applies to:** `/api-tests`, `/spec-audit`

## Why this is bad

Logging sensitive data in tests:
- Tokens and passwords end up in CI logs (accessible to the entire team)
- Allure reports with secrets are accessible via link
- Compliance violation (GDPR, PCI DSS)

## Bad Example

```kotlin
// ❌ BAD: Token in Allure step
@Step("Auth with token: {token}")
fun authenticate(token: String): AuthResponse {
    return apiClient.execute { AuthRequest(token) }
}

// ❌ BAD: Password in assertion message
assertEquals(200, response.code, "Auth failed for password=$password")

// ❌ BAD: Full response body with tokens in println
println("Response: ${response.body}")
```

## Good Example

```kotlin
// ✅ GOOD: Masked token in step
@Step("Auth with token: {maskedToken}")
fun authenticate(token: String): AuthResponse {
    val maskedToken = "${token.take(4)}****"
    return apiClient.execute { AuthRequest(token) }
}

// ✅ GOOD: Assertion without secrets
assertEquals(200, response.code, "Auth should succeed for test user")

// ✅ GOOD: Log only structure, not values
@Step("Verify response has required fields")
fun verifyResponseStructure(response: ApiResponse<UserResponse>) {
    assertNotNull(response.body.id, "Response should contain user ID")
}
```

## What to look for in code review

- `@Step` with `{token}`, `{password}`, `{secret}` in template
- `println` / `logger` with response body (may contain tokens)
- Assertion messages with interpolated secrets
- Hardcoded real tokens/API keys in test code
