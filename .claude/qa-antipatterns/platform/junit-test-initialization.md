# Anti-Pattern: @TestInstance(PER_CLASS) + Field Initialization Failures

## Problem

JUnit cannot create a test class instance if field initialization fails.

## Bad Example

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyTests {
    private val client = ApiClient() // Может упасть при init

    @Test
    fun test() { /* ... */ }
}
```

**Symptom:** "No tests found" — JUnit skips the entire class.

## Good Example

```kotlin
class MyTests {
    private lateinit var client: ApiClient

    @BeforeEach
    fun setUp() {
        client = ApiClient() // Контролируемая инициализация
    }

    @AfterEach
    fun tearDown() {
        client.close()
    }

    @Test
    fun test() { /* ... */ }
}
```

## Why

- `@BeforeEach` runs after successful class initialization
- Setup errors are isolated from the class
- Cleanup is guaranteed via `@AfterEach`

## Detection

```bash
./gradlew test --debug 2>&1 | grep "No tests found"
```

## References

- JUnit 5 User Guide: Test Instance Lifecycle
- (ref: junit-test-initialization.md)
