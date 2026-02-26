# Anti-Pattern: HttpClient Created Inline in Test Body

## Problem

`HttpClient(...)` is created directly inside the `@Test` method.
Each test manages its own client — no single point of configuration.

## Bad Example

```kotlin
// ❌ BAD: inline HttpClient в тесте
@Test
fun `should create user`() {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }
    val response = client.post("$BASE_URL/api/v1/users") {
        setBody(payload)
    }
    assertEquals(201, response.status.value)
}
```

## Good Example

```kotlin
// ✅ GOOD: Request class через apiClient из TestBase
@Test
fun `should create user`() {
    val response = apiClient.execute { CreateUserRequest(TestData.valid()) }
    assertEquals(201, response.code, "User creation should return 201")
}
```

## Why

- Inline client does not reuse the connection pool → slow tests
- No single point for Logging, Auth, Retry configuration
- When baseUrl changes, N tests need updating instead of one Config

## Detection

```bash
grep -rn "HttpClient(" src/test/kotlin/
```

## References

- (ref: inline-http-calls.md)
- General principle: `common/no-abstraction-layer.md`
- Related: `api/configure-http-client.md`
- Related: `api/dry-api-client.md`
