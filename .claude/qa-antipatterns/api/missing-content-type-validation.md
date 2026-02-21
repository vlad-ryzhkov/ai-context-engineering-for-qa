# Missing Content-Type Validation

**Applies to:** `/api-tests`

## Why this is bad

Tests do not validate the response Content-Type:
- Server may return HTML instead of JSON (reverse proxy error)
- Deserialization silently parses garbage into null fields
- Bug is only discovered in production during integration

## Bad Example

```kotlin
// ❌ BAD: Проверяем только status code, Content-Type игнорируется
@Test
fun `get user returns JSON`() {
    val response = apiClient.execute { GetUserRequest(userId) }
    assertEquals(200, response.code, "Get user should return 200")
    assertNotNull(response.body.id, "User should have ID")
}
```

## Good Example

```kotlin
// ✅ GOOD: Проверяем Content-Type для критичных endpoints
@Test
@Severity(NORMAL)
@DisplayName("[Headers] GET user возвращает application/json")
fun getUserReturnsJsonContentType() {
    val response = apiClient.execute { GetUserRequest(userId) }
    assertEquals(200, response.code, "Get user should return 200")

    val contentType = response.headers["Content-Type"]?.firstOrNull().orEmpty()
    assertTrue(
        contentType.contains("application/json"),
        "Content-Type should be application/json, got: $contentType"
    )
}

// ✅ GOOD: Cross-cutting тест на Content-Type для error responses
@Test
@Severity(NORMAL)
@DisplayName("[Headers] Error response возвращает application/json")
fun errorResponseReturnsJsonContentType() {
    val response = apiClient.execute { CreateRawRequest("""{"invalid": true}""") }

    val contentType = response.headers["Content-Type"]?.firstOrNull().orEmpty()
    assertTrue(
        contentType.contains("application/json"),
        "Error Content-Type should be application/json, got: $contentType"
    )
}
```

## What to look for in code review

- No test checks the `Content-Type` header
- Response deserialization without verifying the response is actually JSON
- Error responses (4xx/5xx) not checked for Content-Type
