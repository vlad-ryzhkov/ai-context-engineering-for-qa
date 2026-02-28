# Anti-Pattern: Map Instead of DTO (Java)

## Problem

Using `Map<String, Object>` or `Map<String, String>` instead of typed Java models for request/response bodies:
- Compiler does not catch typos in field names
- No IDE autocomplete or refactoring support
- API refactoring requires searching strings across the entire project
- `@JsonNaming` cannot be applied — field names must be managed manually

## Bad Example

```java
// ❌ BAD: Map — compiler won't help
@Test
void shouldRegisterUser() throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("email", "test@example.com");
    payload.put("phone_number", "+79991234567");
    payload.put("pasword", "Test123!");  // Typo! Compiler is silent.
    payload.put("full_name", "Test User");

    var response = apiClient.post("/register", payload);
    assertThat(response.statusCode()).isEqualTo(201);
}
```

## Good Example

```java
// ✅ GOOD: Typed record with @JsonNaming
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
    String email,
    String phoneNumber,   // Typo = compilation error
    String password,
    String fullName
) {}

// ✅ GOOD: test uses typed DTO
@Test
void shouldRegisterUser() throws Exception {
    var payload = new RegisterRequest(
        "test@example.com",
        "+79991234567",
        "Test123!",
        "Test User"
    );
    var response = apiClient.register(payload);
    assertThat(response.statusCode()).as("Registration must return 201").isEqualTo(201);
}
```

## Why

- `Map<String, Object>` silently serializes wrong keys — test passes against wrong contract
- Jackson `PropertyNamingStrategies.SNAKE_CASE` has no effect on `Map` entries
- Removing or renaming a field in `Map` is invisible to the compiler and grep-based refactoring

## Detection

```bash
grep -rn "Map<String, Object>\|Map<String, String>" src/test/java/
grep -rn "new HashMap\|new LinkedHashMap" src/test/java/*/requests/
```

## See Also

- (ref: `api/java/map-instead-of-dto.md`)
- Related: `api/java/inline-http-calls.md`
- Kotlin equivalent: `api/map-instead-of-dto.md`
