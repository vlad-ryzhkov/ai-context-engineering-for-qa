# Controlled Retries

**Applies to:** `/api-tests`

## Why this is bad

Uncontrolled retry logic in tests:
- Infinite retries hide real bugs
- Retries without backoff overload the test server
- Retrying all errors masks non-retriable failures (400, 403)

## Bad Example

```kotlin
// ❌ BAD: Retry all errors, masks bugs
fun createUserWithRetry(body: CreateUserRequest): UserResponse {
    repeat(5) {
        try {
            val response = apiClient.execute { CreateUserRequest(body) }
            if (response.code == 201) return response.body
        } catch (e: Exception) { }
    }
    throw RuntimeException("Failed after 5 retries")
}

// ❌ BAD: Awaitility on a non-async operation — hides instability
await().atMost(10, TimeUnit.SECONDS).until {
    apiClient.execute { CreateUserRequest(body) }.code == 201
}
```

## Good Example

```kotlin
// ✅ GOOD: Awaitility only for ASYNC operations (status polling)
await()
    .atMost(10, TimeUnit.SECONDS)
    .pollInterval(1, TimeUnit.SECONDS)
    .until {
        val response = apiClient.execute { GetUserRequest(userId) }
        response.body.status == "ACTIVE"
    }

// ✅ GOOD: Sync operations without retry — if it fails, it's a bug
@Test
fun `create user`() {
    val response = apiClient.execute { CreateUserRequest(TestData.validCreateBody()) }
    assertEquals(201, response.code, "Create user should succeed")
}
```

## What to look for in code review

- `repeat(N)` or `while` loop around API calls
- `catch (e: Exception)` with empty body (swallowing errors)
- Awaitility on synchronous CRUD operations (not async status polling)
- Retry without distinguishing retriable (5xx, timeout) and non-retriable (4xx) errors
