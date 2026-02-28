# Anti-Pattern: CompletableFuture.get() Without Timeout

## Problem

Calling `.get()` on a `CompletableFuture` without a timeout parameter blocks the thread indefinitely if the server hangs or is unavailable. In a CI environment this causes the entire test suite to hang until the job is killed by the runner's global timeout.

## Bad Example

```java
// ❌ BAD: .get() without timeout — hangs forever if server is down
@Test
void shouldCreateUser() throws Exception {
    var future = HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    var response = future.get();   // ← BLOCKS INDEFINITELY
    assertThat(response.statusCode()).as("Should return 201").isEqualTo(201);
}
```

## Good Example

```java
// ✅ GOOD: always specify timeout
@Test
void shouldCreateUser() throws Exception {
    var future = HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    var response = future.get(10, TimeUnit.SECONDS);  // ← explicit timeout
    assertThat(response.statusCode()).as("Should return 201").isEqualTo(201);
}

// ✅ GOOD: encapsulate in API client so all callers get the same timeout
public HttpResponse<String> send(HttpRequest request) throws Exception {
    return HTTP_CLIENT
        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .get(10, TimeUnit.SECONDS);
}
```

## Why

- `.get()` without timeout makes tests hang indefinitely on network issues or misconfigured mocks
- CI job is killed after global timeout (e.g., 30 min) with no useful diagnostic output
- `TimeoutException` from `.get(N, UNIT)` immediately signals which test is slow — actionable signal

## Detection

```bash
grep -rn "\.get()" src/test/java/ | grep -v "get(10\|get(5\|get(30\|get(60"
```

## See Also

- (ref: `platform/java/completablefuture-no-timeout.md`)
- Related: `platform/java/flaky-sleep-tests.md`
- Related: `platform/no-hardcoded-timeouts.md` — magic numbers in timeouts
