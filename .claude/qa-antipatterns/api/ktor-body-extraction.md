# Anti-Pattern: response.toString() for Body Extraction in Ktor

## Problem

`response.toString()` is called to obtain the HTTP response body.
In Ktor, `HttpResponse.toString()` returns object metadata (status, headers info),
not the response body. The result is always an unparseable string.

## Bad Example

```kotlin
// ❌ BAD: toString() returns metadata, not body
val response = client.post("$BASE_URL/api/v1/register") {
    contentType(ContentType.Application.Json)
    setBody(payload)
}
val body = Json.decodeFromString<RegisterResponse>(response.toString())
```

## Good Example

```kotlin
// ✅ GOOD: typed body<T>() via ContentNegotiation plugin
val response: RegisterResponse = client.post("$BASE_URL/api/v1/register") {
    contentType(ContentType.Application.Json)
    setBody(payload)
}.body<RegisterResponse>()

// ✅ ACCEPTABLE: bodyAsText() when manual parsing is truly required
val raw: String = client.post("$BASE_URL/api/v1/register") {
    contentType(ContentType.Application.Json)
    setBody(payload)
}.bodyAsText()
val response = objectMapper.readValue<RegisterResponse>(raw)
```

## Why

- `HttpResponse.toString()` is `Object.toString()` — implementation detail, not contract
- Produces strings like `io.ktor.client.statement.DefaultHttpResponse@1a2b3c4d`
- `Json.decodeFromString` on such input throws `SerializationException` at runtime
- `body<T>()` leverages the installed `ContentNegotiation` plugin — no manual parsing needed

## Detection

```bash
grep -rn "\.toString()" src/test/kotlin/ | grep -i "response\|client"
grep -rn "decodeFromString.*toString\|readValue.*toString" src/test/kotlin/
```

## References

- (ref: api/ktor-body-extraction.md)
- Related: `api/configure-http-client.md`
