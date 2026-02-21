# Flaky Sleep Tests

## Why this is bad

`Thread.sleep()` / `delay()` creates flaky tests:
- On slow machines the test fails (timeout too short)
- On fast machines the test wastes time
- Impossible to predict how much time an async operation needs

## Bad Example

```kotlin
// ❌ BAD: Magic number, flaky on slow CI
@Test
fun `user status becomes ACTIVE after registration`() {
    val userId = RegistrationHelper.registerUser(FeatureTestData.validRequest())

    Thread.sleep(2000) // Ждём "достаточно"

    val response = ApiHelper.apiClient.execute { GetUserRequest(userId) }
    assertEquals("ACTIVE", response.body.status, "User should become ACTIVE")
}
```

## Good Example

```kotlin
// ✅ GOOD: Awaitility polling with timeout
@Test
fun `user status becomes ACTIVE after registration`() {
    val userId = RegistrationHelper.registerUser(FeatureTestData.validRequest())

    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until {
            val response = ApiHelper.apiClient.execute { GetUserRequest(userId) }
            response.body.status == "ACTIVE"
        }
}

// ✅ GOOD: Awaitility с assertion message
await()
    .atMost(10, TimeUnit.SECONDS)
    .pollInterval(1, TimeUnit.SECONDS)
    .untilAsserted {
        val response = ApiHelper.apiClient.execute { GetUserRequest(userId) }
        assertEquals("ACTIVE", response.body.status, "User should become ACTIVE within 10s")
    }
```

## What to look for in code review

- `Thread.sleep()`, `delay()`, `TimeUnit.SECONDS.sleep()`
- Magic numbers in timeouts without explanation
- Comments like "wait for async operation"
- Tests that "sometimes fail"
