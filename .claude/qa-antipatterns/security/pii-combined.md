# Anti-Pattern: PII in Tests and Code

**Applies to:** `/api-tests`, `/testcases`

## Problem

Personal data (real or "realistic") in test code and test cases:
- Code ends up in Git → PII leak on repository publication
- Testers copy data from test cases to production
- GDPR / data protection regulation violation during codebase audit
- "Vasya's test account" is still PII

## Bad Example (API Tests)

```kotlin
// ❌ BAD: real domains and formats
object TestData {
    fun validRequest() = RegisterRequest(
        email = "ivan.petrov@gmail.com",     // real domain
        phone = "+79161234567",               // real format
        fullName = "Petrov Ivan Sergeevich"   // looks like a real person
    )
}

const val TEST_EMAIL = "vasya.dev@company.com"  // colleague's PII
const val TEST_PHONE = "+79031112233"            // someone's number
```

## Bad Example (Test Cases)

```kotlin
// ❌ BAD: real data in test case steps
testCase("Registration") {
    step("Enter data") {
        action = "Enter email: ivan.petrov@gmail.com, phone: +79161234567"
    }
}
```

## Good Example (API Tests)

```kotlin
// ✅ GOOD: RFC 2606 + explicitly invalid formats
object TestData {
    fun validRequest() = RegisterRequest(
        email = "auto_${System.currentTimeMillis()}@example.com",  // RFC 2606
        phone = "+70000000000",  // clearly test data (zeros)
        fullName = "Test User ${UUID.randomUUID().toString().take(4)}"
    )
}
```

## Good Example (Test Cases)

```kotlin
// ✅ GOOD: data class description without specifics
testCase("Registration") {
    step("Enter data") {
        action = "Enter a valid email and phone in +7XXXXXXXXXX format"
    }
}
```

## Safe Patterns

| Type | ✅ Safe | ❌ Forbidden |
|------|--------|-------------|
| Email | `@example.com`, `@example.org` (RFC 2606) | `@gmail.com`, `@yandex.ru`, `@company.com` |
| Phone | `+70000000000`, `+79999999999` | `+7916...`, `+7903...` |
| Name | `Test User`, `QA Bot`, `Auto Test 123` | Full name in "Last First Middle" format |
| Card | link to test cards from payment system docs | any 16-digit numbers without reference |

## Detection

```bash
grep -rn "@gmail\.com\|@yandex\.ru\|@mail\.ru\|+7916\|+7903\|+7925" src/test/
```

## References

- (ref: pii-combined.md)
