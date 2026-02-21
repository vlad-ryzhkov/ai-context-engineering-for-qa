# Information Leakage in Error Responses

**Applies to:** `/api-tests` (automated tests), `/spec-audit` (requirements audit)

## Why this is bad

Leaking internal information through error responses:
- Stack traces reveal the technology stack, library versions, and code structure
- Internal paths reveal the server file system (`/opt/app/src/...`)
- Debug info reveals SQL queries, table names, connection strings
- Attackers use this information for targeted attacks

## Bad Example

```kotlin
// ❌ BAD: Test does not check for information leakage in error
@Test
@DisplayName("[Error] Server returns 500 for invalid data")
fun serverError() {
    val response = ApiHelper.apiClient.execute { CreateRawRequest(TestData.corruptedJson()) }
    assertEquals(500, response.code, "Server should return 500")
    // Test passed, but response contains:
    // {"error": "NullPointerException at com.company.service.UserService.create(UserService.kt:42)"}
}

// ❌ BAD: Test checks for stack trace presence as expected behavior
@Test
fun serverErrorContainsDetails() {
    val response = ApiHelper.apiClient.execute { CreateRawRequest("""{"invalid": true}""") }
    assertTrue(response.rawBody.contains("Exception"), "Codifies leakage as a feature")
}
```

## Good Example

```kotlin
// ✅ GOOD: Test verifies that error does NOT contain internal details
@Test
@Severity(CRITICAL)
@DisplayName("[Security] 500 error does not reveal stack trace")
fun serverErrorDoesNotLeakStackTrace() {
    val response = ApiHelper.apiClient.execute { CreateRawRequest("""{"trigger": "server_error"}""") }
    assertEquals(500, response.code, "Server should return 500")

    val body = response.rawBody.orEmpty()
    assertFalse(body.contains("Exception"), "Error response should not contain Exception")
    assertFalse(body.contains("at com."), "Error response should not contain stack trace")
    assertFalse(body.contains(".kt:"), "Error response should not contain source references")
}

// ✅ GOOD: Test verifies that error does not contain internal paths
@Test
@Severity(CRITICAL)
@DisplayName("[Security] Error response does not contain internal paths")
fun errorDoesNotLeakInternalPaths() {
    val response = ApiHelper.apiClient.execute { CreateRawRequest("""{"trigger": "bad_request"}""") }

    val body = response.rawBody.orEmpty()
    assertFalse(body.contains("/opt/"), "Error should not contain server paths")
    assertFalse(body.contains("/home/"), "Error should not contain home directory paths")
    assertFalse(body.contains("src/main/"), "Error should not contain source paths")
}

// ✅ GOOD: Test verifies generic error response format
@Test
@Severity(NORMAL)
@DisplayName("[Error] 500 error returns generic message")
fun serverErrorReturnsGenericMessage() {
    val response = ApiHelper.apiClient.execute { CreateRawRequest("""{"trigger": "server_error"}""") }
    assertEquals(500, response.code, "Server should return 500")
    assertEquals("Internal Server Error", response.body.message, "Error should have generic message")
    assertNull(response.body.details, "Error should not expose details to client")
}
```

## What to look for in review

- Tests for 4xx/5xx errors do not check error body content for leaks
- Error response contains words: `Exception`, `Error at`, `stack`, `trace`
- Error response contains file system paths (`/opt/`, `/home/`, `/var/`, `src/`)
- Error response contains class names (`com.company.`, `io.ktor.`)
- Error response contains SQL fragments (`SELECT`, `INSERT`, `table`)
- Error response contains technology versions

## Grep signatures for automatic detection

```
# In error response assertions — find tests that do NOT check for leaks
pattern: "assertEquals(500"  # → verify that body assertion is nearby
pattern: "response.error"    # → verify that leaks are not codified as expected behavior
```
