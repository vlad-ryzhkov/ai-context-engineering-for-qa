# Static Object Mother (API Tests)

**Applies to:** `/api-tests` (automated API tests)

## Why this is bad

Static data in Object Mother / TestData:
- Conflicts during parallel execution (identical email/phone)
- Impossible to run the test twice without cleanup
- Flaky tests due to `UNIQUE constraint violation`
- Hide isolation problems between tests

## Bad Example

```kotlin
// ❌ BAD: Static constants
object RegistrationTestData {
    val VALID_EMAIL = "test@example.com"      // Conflict on second run
    val VALID_PHONE = "+79991234567"
    val VALID_PASSWORD = "Password123!"

    fun validRequest() = RegisterRequest(
        email = VALID_EMAIL,  // Always the same
        phone = VALID_PHONE,
        password = VALID_PASSWORD
    )
}

// ❌ BAD: Hardcoded in function without generation
fun validRequest() = RegisterRequest(
    email = "fixed_test@example.com",  // Static!
    phone = "+70001112233",
    password = "Test123!"
)
```

## Good Example

```kotlin
// ✅ GOOD: Object Mother with unique data generation
object RegistrationTestData {

    fun validRequest() = RegisterRequest(
        email = "auto_${System.currentTimeMillis()}@example.com",
        phone = "+7${(9000000000..9999999999).random()}",
        password = "Test#${UUID.randomUUID().toString().take(8)}",
        fullName = "Test User"
    )

    // Modifications via copy() — email is still unique
    fun withInvalidEmail() = validRequest().copy(
        email = "invalid-email-no-at-sign"
    )

    fun withWeakPassword() = validRequest().copy(
        password = "weak"
    )

    fun withEmptyName() = validRequest().copy(
        fullName = ""
    )
}
```

## Pattern: Unique Suffix Generator

```kotlin
// ✅ Reusable generator
object TestDataUtils {
    fun uniqueSuffix() = "${System.currentTimeMillis()}_${(1000..9999).random()}"
    fun uniqueEmail(prefix: String = "auto") = "${prefix}_${uniqueSuffix()}@example.com"
    fun uniquePhone() = "+7${(9000000000..9999999999).random()}"
}

object RegistrationTestData {
    fun validRequest() = RegisterRequest(
        email = TestDataUtils.uniqueEmail(),
        phone = TestDataUtils.uniquePhone(),
        // ...
    )
}
```

## What to look for in code review

- `const val` or `val` with fixed email/phone/id
- `valid*()` functions without `System.currentTimeMillis()` or `UUID`
- Missing randomization in data that must be unique
- Tests with `@Disabled` due to "data conflicts"
- Comments "run cleanup before test" or "change email before running"
