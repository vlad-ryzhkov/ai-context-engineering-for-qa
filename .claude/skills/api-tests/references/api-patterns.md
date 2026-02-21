# API Testing Patterns

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
- **Assert:** `items.size <= pageSize`, `total` consistency.

## 4. Idempotency
**Use when:** RETRY safety (PUT, DELETE, Idempotency-Key).
- **Logic:** Request A -> Response X. Request A (again) -> Response X (Identical).
- **DELETE:** 204 -> 204 (or 404 depending on spec).
- **Key:** POST + Key -> 201. Retry + Same Key -> 200 (Not 201), same body.

## 5. Architecture
Full code examples: `references/examples.md`.

| Component | Rule |
|-----------|------|
| `object Endpoints` | All URLs — `const val` constants. Hardcoded paths in client are FORBIDDEN. |
| `data class` Request | All fields `Any?` — support for negative tests with invalid types |
| `@JsonNaming` | Determined by Style Analysis. Do not add if the project already uses native snake_case fields. |
| `object FeatureHelper` | `@Step` methods. `verify{Entity}InDb` — mandatory for `DB:` scenarios. |
| `FakerService` | Wrapper over Faker. TestData MUST NOT store static strings. |
| JSON schemas | `src/test/resources/schemas/`. For `Contract Match` scenarios. |
| Lazy init | `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)` for HttpClient, Faker. |

## 6. Translation Rules (Parsing Expected Result)

| Keyword in Expected | Generated code |
|---------------------|----------------|
| `Contract Match` | `response.validateSchema("schema_name.json")` |
| `DB:` | `Helper.verify{Entity}InDb(...)` after request |
| `Event published` | Awaitility check in Helper (async queue) |
| `Content-Type` | `assertEquals` on `response.headers["Content-Type"]` |
| `Cleanup:` | `delete{Entity}` in `@AfterEach` or try-finally |
| `Idempotency-Key:` | `header("Idempotency-Key", idempotencyKey)` in request builder |
| `Mock: SMS Gateway returns {code}` | WireMock stub in `@BeforeEach` |
| `Wait >5 min` / `cache expired` | `@Disabled("Time-dependent scenario...")` if no testability hook |
| `DB: Transaction rolled back` | `assertNull(helper.findByEmail(email), "DB rollback expected")` |

## 7. Coverage Matrix

| Category | Priority Checks |
|----------|-----------------|
| **Write** | 400 (Structural/Validation/Security) → 401/403 → 201 → 409 → 429 |
| **Read** | 200 (Fields/List/Empty) → Filter/Sort → 400 (Params) → 401/404 |
| **Delete** | 200/204 → 404 (Verify) → 401 → Idempotency |

## 8. Grouping Strategy (Parameterized Tests)

- NEG/BVA scenarios of the same endpoint with identical Expected → `@ParameterizedTest` when ≥3 matches.
- Data source: `@MethodSource("provide{EndpointName}ValidationData")`.
- Parameter: object `(inputData, expectedField, expectedErrorCode)`.
- Happy Path (POS) — always a separate `@Test` for clarity in Allure.
