# Anti-Pattern: Flaky Sleep Tests (Java)

## Problem

`Thread.sleep()` creates flaky tests:
- On slow machines the test fails (sleep too short)
- On fast machines the test wastes time
- Impossible to predict how much time an async operation needs
- Silent dependency on execution speed — the hardcoded duration is always wrong somewhere

## Bad Example

```java
// ❌ BAD: Magic number sleep, flaky on slow CI
@Test
void userStatusBecomesActiveAfterRegistration() throws Exception {
    String userId = UserHelper.registerUser(TestData.validRequest());

    Thread.sleep(2000); // Wait "enough"

    var response = apiClient.getUser(userId).get(10, TimeUnit.SECONDS);
    var body = MAPPER.readValue(response.body(), UserResponse.class);
    assertThat(body.getStatus()).as("User should become ACTIVE").isEqualTo("ACTIVE");
}
```

## Good Example

```java
// ✅ GOOD: Awaitility polling with bounded timeout
@Test
void userStatusBecomesActiveAfterRegistration() throws Exception {
    String userId = UserHelper.registerUser(TestData.validRequest());

    await()
        .atMost(10, SECONDS)
        .pollInterval(500, MILLISECONDS)
        .until(() -> {
            var resp = apiClient.getUser(userId).get(10, SECONDS);
            return MAPPER.readValue(resp.body(), UserResponse.class)
                .getStatus().equals("ACTIVE");
        });
}

// ✅ GOOD: Awaitility with assertion message
@Test
void userStatusBecomesActiveAfterRegistration() throws Exception {
    String userId = UserHelper.registerUser(TestData.validRequest());

    await()
        .atMost(10, SECONDS)
        .pollInterval(1, SECONDS)
        .untilAsserted(() -> {
            var resp = apiClient.getUser(userId).get(10, SECONDS);
            var body = MAPPER.readValue(resp.body(), UserResponse.class);
            assertThat(body.getStatus())
                .as("User should become ACTIVE within 10s")
                .isEqualTo("ACTIVE");
        });
}
```

## Why

- Awaitility retries until the condition is met or timeout expires — no wasted time on fast machines
- `ConditionTimeoutException` from Awaitility provides the last observed value — actionable diagnostic
- `Thread.sleep` is a banned pattern in this project (CLAUDE.md)

## What to look for in code review

- `Thread.sleep()`, `TimeUnit.SECONDS.sleep()`
- Magic numbers without explanation in waits
- Comments like "wait for async operation"
- Tests that "sometimes fail" in CI

## Detection

```bash
grep -rn "Thread\.sleep\|TimeUnit\.\w*\.sleep" src/test/java/
```

## See Also

- (ref: `platform/java/flaky-sleep-tests.md`)
- Related: `platform/java/completablefuture-no-timeout.md`
- Related: `platform/no-hardcoded-timeouts.md`
- Kotlin equivalent: `platform/flaky-sleep-tests.md`
