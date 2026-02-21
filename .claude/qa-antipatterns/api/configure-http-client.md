# Configure HTTP Client

**Applies to:** `/api-tests`

## Why this is bad

Default HTTP client configuration in tests:
- Default timeout (infinite or too large) hangs CI
- Redirect following hides real problems (301/302)
- Missing connection pool limits leads to resource exhaustion

## Bad Example

```kotlin
// ❌ BAD: Default client without timeouts
object ApiHelper {
    val apiClient = ApiClient(Config.BASE_URL)
}

// ❌ BAD: Timeout set differently in each test
@Test
fun `slow endpoint`() {
    apiClient.setReadTimeout(30000)
    val response = apiClient.execute { SlowRequest() }
    apiClient.setReadTimeout(5000)
}
```

## Good Example

```kotlin
// ✅ GOOD: Centralized configuration in Config
object ApiHelper {
    val apiClient = ApiClient(Config.BASE_URL).apply {
        setConnectTimeout(Config.CONNECT_TIMEOUT_MS)
        setReadTimeout(Config.READ_TIMEOUT_MS)
        setFollowRedirects(false)
    }
}

object Config {
    val BASE_URL: String = System.getenv("BASE_URL") ?: "http://localhost:8080"
    const val CONNECT_TIMEOUT_MS = 5_000
    const val READ_TIMEOUT_MS = 10_000
}
```

## What to look for in code review

- `ApiClient()` without explicit timeouts
- `setReadTimeout` / `setConnectTimeout` in test bodies (not in config)
- Different timeouts in different tests for the same service
- `followRedirects = true` (hides redirect bugs)
