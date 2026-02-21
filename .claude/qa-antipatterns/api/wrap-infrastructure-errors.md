# Wrap Infrastructure Errors

**Applies to:** `/api-tests`

## Why this is bad

Infrastructure errors (network, timeout, DNS) are indistinguishable from business errors:
- Test fails with `ConnectException` — unclear whether it is a bug or an infra issue
- CI report shows 50 FAILED, but all due to a single service being down
- No separation between "test found a bug" and "environment is broken"

## Bad Example

```kotlin
// ❌ BAD: Infrastructure exception выглядит как test failure
@Test
fun `create user`() {
    val response = apiClient.execute { CreateUserRequest(TestData.validCreateBody()) }
    assertEquals(201, response.code, "Create user should succeed")
}
// При ConnectException: AssertionError → невозможно отличить от бага
```

## Good Example

```kotlin
// ✅ GOOD: Health check в @BeforeAll — infra issues видны сразу
companion object {
    @JvmStatic
    @BeforeAll
    fun verifyServiceAvailable() {
        val healthResponse = runCatching {
            apiClient.execute { HealthCheckRequest() }
        }.getOrNull()

        assertEquals(
            200,
            healthResponse?.code,
            "Service unavailable at ${Config.BASE_URL} — check infrastructure"
        )
    }
}

// ✅ GOOD: Assertion message указывает на возможную infra причину
@Test
fun `create user`() {
    val response = runCatching {
        apiClient.execute { CreateUserRequest(TestData.validCreateBody()) }
    }.getOrElse {
        fail("Request failed with infrastructure error: ${it.javaClass.simpleName} — ${it.message}")
    }
    assertEquals(201, response.code, "Create user should succeed")
}
```

## What to look for in code review

- Missing health check or connectivity check before the test suite
- `ConnectException`, `SocketTimeoutException` in CI without explanation
- All tests in a suite fail the same way (sign of infra issue, not a bug)
