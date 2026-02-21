# Anti-Pattern: Duplicated API Client Methods (DRY Violation)

## Problem

Separate client methods are generated for success and error scenarios that differ
only in the expected response type. The request construction, URL, headers, and
serialization are copy-pasted, creating a maintenance burden and inconsistency risk.

## Bad Example

```kotlin
// ❌ BAD: 95% identical methods — only return type differs
suspend fun registerUser(request: RegisterRequest): RegisterResponse {
    return client.post("$BASE_URL/api/v1/register") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body<RegisterResponse>()
}

suspend fun registerUserExpectError(request: RegisterRequest): ErrorResponse {
    return client.post("$BASE_URL/api/v1/register") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body<ErrorResponse>()
}
```

## Good Example

```kotlin
// ✅ GOOD: single generic method — caller decides what to parse
suspend inline fun <reified T> post(path: String, body: Any): HttpResponse =
    client.post("$BASE_URL$path") {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

// In tests — parse as needed:
val ok = registrationClient.post("/api/v1/register", validPayload).body<RegisterResponse>()
val err = registrationClient.post("/api/v1/register", invalidPayload).body<ErrorResponse>()
```

```kotlin
// ✅ ALSO GOOD: return raw HttpResponse, assert on status, parse body once
suspend fun register(request: RegisterRequest): HttpResponse =
    client.post("$BASE_URL/api/v1/register") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }

// In test:
val response = registrationClient.register(invalidPayload)
assertEquals(422, response.status.value, "expected validation error")
val error = response.body<ErrorResponse>()
assertEquals("VALIDATION_ERROR", error.code, "error code mismatch")
```

## Why

- Copy-pasted method bodies diverge silently when the URL or headers change
- N response types → N nearly-identical methods → O(N) maintenance cost
- Generic `HttpResponse` return gives the test full control over status + body parsing
- `inline fun <reified T>` leverages Kotlin reification without reflection overhead

## Detection

```bash
# Find methods with identical bodies differing only by return type
grep -A10 "suspend fun.*Request" src/test/kotlin/*/requests/*.kt | grep -B5 "\.body<"
# Flag files with >2 methods posting to the same path
grep -c "\.post(" src/test/kotlin/*/requests/*.kt
```

## References

- (ref: api/dry-api-client.md)
- Related: `common/no-abstraction-layer.md`
