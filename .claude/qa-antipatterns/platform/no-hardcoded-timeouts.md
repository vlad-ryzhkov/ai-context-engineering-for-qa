# No Hardcoded Timeouts

**Applies to:** `/api-tests`

## Why this is bad

Hardcoded timeouts in Awaitility or HTTP client:
- Break on slow CI (timeout too small)
- Waste time on fast environments (timeout too large)
- Impossible to override for different environments (dev/staging/prod)

## Bad Example

```kotlin
// ❌ BAD: Magic numbers scattered across tests
await()
    .atMost(5, TimeUnit.SECONDS)
    .pollInterval(500, TimeUnit.MILLISECONDS)
    .until { getStatus(id) == "ACTIVE" }

// ❌ BAD: Different timeouts in each test
await().atMost(3, TimeUnit.SECONDS).until { ... }
await().atMost(10, TimeUnit.SECONDS).until { ... }
await().atMost(30, TimeUnit.SECONDS).until { ... }
```

## Good Example

```kotlin
// ✅ GOOD: Timeouts in Config, reused
object PollingConfig {
    val DEFAULT_TIMEOUT = Duration.ofSeconds(
        System.getenv("POLL_TIMEOUT_SEC")?.toLongOrNull() ?: 10
    )
    val DEFAULT_INTERVAL = Duration.ofSeconds(1)
}

await()
    .atMost(PollingConfig.DEFAULT_TIMEOUT)
    .pollInterval(PollingConfig.DEFAULT_INTERVAL)
    .until { getStatus(id) == "ACTIVE" }
```

## What to look for in code review

- `atMost(N, TimeUnit.SECONDS)` with literal numbers in tests
- Different timeouts for identical operations in different tests
- Missing centralized polling config
