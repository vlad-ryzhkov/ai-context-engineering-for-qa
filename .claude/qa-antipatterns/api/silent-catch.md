# Anti-Pattern: Silent Catch in Test Client

## Problem

HTTP calls or JSON deserialization in the API client are wrapped in a `try/catch`
that swallows the exception and returns an empty stub object (e.g., `RegisterResponse()`).
The test does not fail at the infrastructure error — it fails later on assertions
with a misleading message, hiding the real root cause.

## Bad Example

```kotlin
// ❌ BAD: silent catch returns empty stub — real error is invisible
fun register(request: RegisterRequest): RegisterResponse {
    return try {
        val response = client.post("$BASE_URL/api/v1/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        objectMapper.readValue(response.bodyAsText())
    } catch (e: Exception) {
        RegisterResponse() // test will fail on assertions, not here
    }
}
```

## Good Example

```kotlin
// ✅ GOOD: exceptions propagate — test runner reports the real failure
fun register(request: RegisterRequest): RegisterResponse =
    runTest {
        client.post("$BASE_URL/api/v1/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<RegisterResponse>()
    }
```

## Why

- Swallowed exceptions hide infrastructure failures (network errors, invalid JSON, wrong base URL)
- Test output says "expected 201 but was 0" instead of "JsonParseException: unexpected token"
- Root cause diagnosis requires extra debugging that would be unnecessary if the exception propagated
- Every test relying on this client becomes a liar: it appears to "run" but produces garbage data

## Detection

```bash
grep -rn "catch.*Exception" src/test/kotlin/ | grep -v "assertThrows\|shouldThrow\|assertFailsWith"
grep -A3 "} catch" src/test/kotlin/ | grep -E "return \w+\(\)"
```

## References

- (ref: api/silent-catch.md)
- Related: `common/no-abstraction-layer.md`
- Related: `api/wrap-infrastructure-errors.md`
- Related: `platform/controlled-retries.md`
