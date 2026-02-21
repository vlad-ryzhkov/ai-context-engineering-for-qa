# No Cleanup Pattern

## Why this is bad

Tests without data cleanup:
- Pollute the DB with thousands of test records
- Create flaky tests (uniqueness conflicts)
- Make parallel execution impossible
- Complicate debugging on staging/dev environments

## Bad Example

```kotlin
// ❌ BAD: Data stays in DB forever
@Test
fun `user can register`() {
    val payload = RegisterRequest(
        email = "test_${System.currentTimeMillis()}@example.com",
    )

    val response = ApiHelper.apiClient.execute { RegisterRequest(payload) }
    assertEquals(201, response.code, "Registration should succeed")
    // Test finished, user remains in DB
}
```

## Good Example

```kotlin
// ✅ GOOD: try-finally guarantees cleanup
@Test
fun `user can register`() {
    var userId: String? = null

    try {
        val response = ApiHelper.apiClient.execute { RegisterRequest(validPayload) }
        assertEquals(201, response.code, "Registration should succeed")
        userId = response.body.userId

        // Assertions...
    } finally {
        userId?.let { ApiHelper.apiClient.execute { DeleteUserRequest(it) } }
    }
}
```

## Recommended Strategy: Cleanup-First

**Cleanup in `@BeforeEach` (not `@AfterEach`)** — the recommended approach for integration tests.

**Why Cleanup-First is better than Cleanup-After:**
- On test failure, data is **preserved** in DB for debugging
- The next run **cleans up** before itself (idempotent)
- `@AfterEach` may not execute on JVM crash/timeout

```kotlin
// ✅ RECOMMENDED: Cleanup-First in @BeforeEach
@BeforeEach
fun cleanup() {
    runCatching { ApiHelper.apiClient.execute { DeleteUserByEmailRequest(testEmail) } }
}

@Test
fun `user can register`() {
    val response = ApiHelper.apiClient.execute { RegisterRequest(payload) }
    assertEquals(201, response.code, "Registration should succeed")
}
```

```kotlin
// ✅ GOOD: Cleanup-First inline (for individual tests)
@Test
fun `user can register`() {
    runCatching { ApiHelper.apiClient.execute { DeleteUserByEmailRequest(testEmail) } }

    val response = ApiHelper.apiClient.execute { RegisterRequest(payload) }
    assertEquals(201, response.code, "Registration should succeed")
}
```

**When to use which approach:**

| Strategy | When |
|----------|------|
| **Cleanup-First (`@BeforeEach`)** | Integration tests, shared DB, need debugging on failure |
| **try-finally** | Test creates a unique resource that must be deleted immediately |
| **Cleanup-After (`@AfterEach`)** | Only if Cleanup-First is not possible (no idempotent DELETE) |

## What to look for in code review

- Missing `finally` block, `@BeforeEach` cleanup, or `@AfterEach`
- "Unique prefixes" as the only isolation strategy
- Tests that fail on re-run
- `@AfterEach` instead of `@BeforeEach` cleanup without justification
- Cleanup operations that are not idempotent (fail if resource does not exist)
