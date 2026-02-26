# No Abstraction Layer

## Why this is bad

HTTP calls directly in tests:
- When a URL changes, 100+ tests need fixing
- Duplication of client configuration code
- Hard to add logging/retry/auth
- Tests know too much about API implementation

## Bad Example

```kotlin
// ❌ BAD: Raw HTTP directly in each test
@Test
fun `user can register`() {
    val response = httpClient.post("https://api.example.com/api/v1/users/register") {
        contentType(ContentType.Application.Json)
        header("X-Api-Key", "secret-key")
        setBody(payload)
    }

    assertEquals(201, response.code)
}

@Test
fun `registration fails with invalid email`() {
    // Same boilerplate again...
    val response = httpClient.post("https://api.example.com/api/v1/users/register") {
        contentType(ContentType.Application.Json)
        header("X-Api-Key", "secret-key")
        setBody(invalidPayload)
    }
}
```

## Good Example

```kotlin
// ✅ GOOD: Request class encapsulates HTTP
class RegisterRequest(
    request: FeatureRequest
) : ApiRequestBaseJson<FeatureResponse>(FeatureResponse::class.java) {
    init {
        url = Config.baseUrl + Endpoints.REGISTER
        body = request
    }
}

// Tests are clean and readable
@Test
fun `user can register`() {
    val response = ApiHelper.apiClient.execute { RegisterRequest(validPayload) }
    assertEquals(201, response.code, "Registration should succeed with valid payload")
}
```

## What to look for in code review

- Raw HTTP calls (`httpClient.post()`, `httpClient.get()`) directly in `@Test` methods
- Duplication of URL, headers, contentType
- Missing Request classes extending `ApiRequestBaseJson<T>`
- Hardcoded URLs in tests (`"https://..."`)
- Custom `ApiClient`/`ApiResponse` wrappers instead of common-test-libs

## See Also

- `api/configure-http-client.md` — shared client configuration is part of the abstraction layer
- `api/inline-http-calls.md` — inline HTTP calls are the primary symptom of a missing abstraction
- `api/dry-api-client.md` — DRY violations in request classes extend the same root problem
