# API Testing Patterns (Java 17+)

## 1. Auth Flow

**Use when:** Endpoint requires authentication.
- **Flow:** Missing Header (401) -> Invalid Token (401) -> Expired (401) -> Wrong Role (403) -> Valid (200).
- **Helper:** `AuthHelper.getToken(role)` via `@Step`.
- **Refresh:** Verify token refresh works (Phase 3).

## 2. CRUD

**Use when:** Standard resource lifecycle.
- **Create:** 400 (validation) -> 201 (check fields) -> 409 (duplicate).
- **Read:** 200 (by ID) -> 404 (missing).
- **Update:** 200 (verify change). Verify ONLY changed fields.
- **Delete:** 204 -> Verify GET returns 404.

## 3. Pagination & List

**Use when:** GET returns lists.
- **Checks:**
  - Default params (page 1).
  - `pageSize` limits (request 5 -> get <=5).
  - Navigation (p1 != p2).
  - Empty results (valid 200).
  - Boundary (max size, invalid inputs -> 400).
- **Assert:** `assertThat(items.size()).as("page size").isLessThanOrEqualTo(pageSize)`.

## 4. Idempotency

**Use when:** RETRY safety (PUT, DELETE, Idempotency-Key).
- **Logic:** Request A -> Response X. Request A (again) -> Response X (Identical).
- **DELETE:** 204 -> 204 (or 404 depending on spec).
- **Key:** POST + Key -> 201. Retry + Same Key -> 200 (Not 201), same body.

## 5. Architecture

Full code examples: `references/java/examples.md` (if present).

| Component | Rule |
|-----------|------|
| `static final HttpClient HTTP_CLIENT` | Single shared client in requests layer. Never per-test or per-method. |
| `static final ObjectMapper MAPPER` | Configured once with `PropertyNamingStrategies.SNAKE_CASE`. |
| `class Endpoints` | All URLs — `static final String` constants. Hardcoded paths in client are FORBIDDEN. |
| `record` or POJO Request | Use Java records (JDK 16+) or POJO with all-args constructor for request models. |
| `@JsonNaming` | `PropertyNamingStrategies.SnakeCaseStrategy.class` on DTO class. |
| `class FeatureHelper` | `@Step` methods. `verify{Entity}InDb` — mandatory for `DB:` scenarios. |
| `FakerService` | Wrapper over Faker. TestData MUST NOT store static strings. |
| JSON schemas | `src/test/resources/schemas/`. For `Contract Match` scenarios. |

### HttpClient Static Singleton Pattern

```java
public class UserApiClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private static final String BASE_URL =
        System.getProperty("BASE_URL", "http://localhost:8080");

    public HttpResponse<String> createUser(CreateUserRequest body) throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + Endpoints.USERS))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
            .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .get(10, TimeUnit.SECONDS);
    }
}
```

### DTO with Jackson @JsonNaming

```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateUserRequest(
    String email,
    String phoneNumber,
    String fullName
) {}
```

### AssertJ Assertion Chain with .as()

```java
assertThat(response.statusCode())
    .as("Expected 201 Created for valid user registration")
    .isEqualTo(201);

assertThat(body.getUserId())
    .as("Response must contain non-null userId")
    .isNotNull();
```

### Awaitility Polling (Java Style)

```java
await()
    .atMost(10, SECONDS)
    .pollInterval(500, MILLISECONDS)
    .until(() -> {
        var resp = apiClient.getUser(userId).get(10, SECONDS);
        return resp.statusCode() == 200 &&
            MAPPER.readValue(resp.body(), UserResponse.class).getStatus().equals("ACTIVE");
    });

await()
    .atMost(10, SECONDS)
    .pollInterval(1, SECONDS)
    .untilAsserted(() ->
        assertThat(MAPPER.readValue(apiClient.getUser(userId).get(10, SECONDS).body(), UserResponse.class).getStatus())
            .as("User status should become ACTIVE within 10s")
            .isEqualTo("ACTIVE")
    );
```

### @Step in Helper Classes (Allure)

```java
public class UserHelper {

    @Step("Create user and return ID")
    public static String createUserAndGetId(CreateUserRequest request) throws Exception {
        var response = new UserApiClient().createUser(request);
        assertThat(response.statusCode()).as("User creation must succeed").isEqualTo(201);
        return MAPPER.readValue(response.body(), CreateUserResponse.class).getUserId();
    }

    @Step("Verify user exists in DB: {userId}")
    public static void verifyUserInDb(String userId) {
        // DB: assertion logic via JDBC or repository
    }
}
```

## 6. Translation Rules (Parsing Expected Result)

| Keyword in Expected | Generated code |
|---------------------|----------------|
| `Contract Match` | `validateSchema("schema_name.json", response.body())` |
| `DB:` | `Helper.verify{Entity}InDb(...)` after request |
| `Event published` | Awaitility check in Helper (async queue) |
| `Content-Type` | `assertThat(response.headers().firstValue("Content-Type").orElse(""))` |
| `Cleanup:` | `delete{Entity}` in `@AfterEach` or `try/finally` |
| `Idempotency-Key:` | `.header("Idempotency-Key", idempotencyKey)` in request builder |
| `Mock: SMS Gateway returns {code}` | WireMock stub in `@BeforeEach` |
| `Wait >5 min` / `cache expired` | `@Disabled("Time-dependent scenario...")` if no testability hook |
| `DB: Transaction rolled back` | `assertThat(helper.findByEmail(email)).as("DB rollback expected").isNull()` |

## 7. Coverage Matrix

| Category | Priority Checks |
|----------|-----------------|
| **Write** | 400 (Structural/Validation/Security) → 401/403 → 201 → 409 → 429 |
| **Read** | 200 (Fields/List/Empty) → Filter/Sort → 400 (Params) → 401/404 |
| **Delete** | 200/204 → 404 (Verify) → 401 → Idempotency |

## 8. Grouping Strategy (Parameterized Tests)

- NEG/BVA scenarios of the same endpoint with identical Expected → `@ParameterizedTest` when ≥3 matches.
- Data source: `@MethodSource("provide{EndpointName}ValidationData")`.
- Parameter: record `(inputData, expectedField, expectedErrorCode)`.
- Happy Path (POS) — always a separate `@Test` for clarity in Allure.
