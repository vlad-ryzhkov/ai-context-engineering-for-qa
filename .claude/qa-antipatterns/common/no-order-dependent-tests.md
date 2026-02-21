# No Order-Dependent Tests

**Applies to:** `/api-tests`

## Why this is bad

Tests that depend on execution order:
- JUnit 5 does not guarantee order by default
- Parallel execution is impossible
- One failing test cascades and breaks all subsequent tests

## Bad Example

```kotlin
// ❌ BAD: Тесты зависят от порядка — delete не работает без create
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserTest : TestBase() {
    companion object {
        lateinit var userId: String
    }

    @Test @Order(1)
    fun `create user`() {
        val response = apiClient.execute { CreateUserRequest(TestData.validCreateBody()) }
        userId = response.body.id
    }

    @Test @Order(2)
    fun `get user`() {
        val response = apiClient.execute { GetUserRequest(userId) }
        assertEquals(200, response.code, "Get user should return 200")
    }

    @Test @Order(3)
    fun `delete user`() {
        val response = apiClient.execute { DeleteUserRequest(userId) }
        assertEquals(204, response.code, "Delete should return 204")
    }
}
```

## Good Example

```kotlin
// ✅ GOOD: Каждый тест полностью автономен
class UserTest : TestBase() {
    @Test
    fun `get user by id`() {
        val userId = UserHelper.createUser(TestData.validCreateBody())

        val response = apiClient.execute { GetUserRequest(userId) }
        assertEquals(200, response.code, "Get user should return 200")
    }

    @Test
    fun `delete user`() {
        val userId = UserHelper.createUser(TestData.validCreateBody())

        val response = apiClient.execute { DeleteUserRequest(userId) }
        assertEquals(204, response.code, "Delete should return 204")
    }
}
```

## What to look for in code review

- `@TestMethodOrder` + `@Order` annotations
- `lateinit var` in companion object, populated by one test
- Tests that fail when run individually
- Comments like "run after test X"
