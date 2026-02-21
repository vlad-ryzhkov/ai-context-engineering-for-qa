# Anti-Pattern: Checking Only HTTP Code Without Business Error Code

## Problem

NEG-test checks only HTTP status (`400`, `422`) without checking `body.code`.
Test is green, but the actual business logic error (wrong `code`) goes undetected.

## Bad Example

```kotlin
// ❌ BAD: checking only HTTP status
@Test
fun `returns 400 for invalid email`() {
    val resp = apiClient.execute { RegisterRequest(TestData.invalidEmail()) }
    assertEquals(400, resp.code)
    // Business code not checked — any 400 will pass
}
```

## Good Example

```kotlin
// ✅ GOOD: checking HTTP status + business error code
@Test
fun `returns 400 for invalid email`() {
    val resp = apiClient.execute { RegisterRequest(TestData.invalidEmail()) }
    assertEquals(400, resp.code, "HTTP status mismatch")
    assertEquals("VALIDATION_ERROR", resp.body.code, "error code mismatch")
    assertEquals("email", resp.body.field, "error field mismatch")
}
```

## Why

- HTTP `400` can come for many reasons (auth, schema, rate limit)
- Without `body.code` the test cannot distinguish `VALIDATION_ERROR` from `MISSING_FIELD` or `INVALID_FORMAT`
- Regression in business error logic goes unnoticed

## Detection

```bash
grep -n "assertEquals(400\|assertEquals(422\|assertEquals(401" src/test/kotlin/ -r \
  | grep -v "body.code\|body\.error"
```

Result contains lines → check each test for `body.code` assertion presence.

## References

- (ref: missing-business-error-assertion.md)
