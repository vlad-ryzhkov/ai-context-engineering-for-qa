# Anti-Pattern: HttpClient Created Inline in Test Body (Java)

## Problem

`HttpClient.newBuilder().build()` is called directly inside the `@Test` method or `@BeforeEach` per test instance.
Each test manages its own client — no single point of configuration, connection pool is not reused.

## Bad Example

```java
// ❌ BAD: new HttpClient per test — no pooling, no shared config
@Test
void shouldCreateUser() throws Exception {
    var client = HttpClient.newBuilder().build();
    var request = HttpRequest.newBuilder()
        .uri(URI.create(BASE_URL + "/api/v1/users"))
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(201);
}
```

## Good Example

```java
// ✅ GOOD: static final client in API client class
public class UserApiClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public HttpResponse<String> createUser(CreateUserRequest body) throws Exception {
        var req = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + Endpoints.USERS))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
            .build();
        return HTTP_CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
            .get(10, TimeUnit.SECONDS);
    }
}

// ✅ GOOD: test delegates to API client
@Test
void shouldCreateUser() throws Exception {
    var response = apiClient.createUser(TestData.validRequest());
    assertThat(response.statusCode()).as("User creation should return 201").isEqualTo(201);
}
```

## Why

- Inline client does not reuse the connection pool → slow tests, TCP overhead per test
- No single point for timeout, TLS, logging, or retry configuration
- When `BASE_URL` changes, N tests need updating instead of one config

## Detection

```bash
grep -rl "new.*HttpClient\(\)\|HttpClient\.newBuilder" src/test/java/ | grep "Tests\.java$"
```

## See Also

- (ref: `api/java/inline-http-calls.md`)
- General principle: `common/no-abstraction-layer.md`
- Related: `api/java/map-instead-of-dto.md`
