# Anti-Pattern: Asserting Read Immediately After Write in Eventually-Consistent System

## Problem

After a write operation (POST/PUT/DELETE), the test immediately reads (GET) and asserts
the new state. In eventually-consistent systems (async replication, Kafka projection,
cache invalidation), the read may return stale data, causing flaky tests.

## Bad Example

```kotlin
// ❌ BAD: immediate read after write in eventually-consistent system
val createResponse = apiClient.createEntity(request)
assertEquals(HttpStatusCode.Created, createResponse.status)

val getResponse = apiClient.getEntity(createResponse.body<CreateResponse>().id)
assertEquals("active", getResponse.body<EntityResponse>().status) // FLAKY: projection may not be ready
```

## Good Example

```kotlin
// ✅ GOOD: Awaitility polling with bounded timeout
val createResponse = apiClient.createEntity(request)
assertEquals(HttpStatusCode.Created, createResponse.status)

val entityId = createResponse.body<CreateResponse>().id
await()
    .atMost(Duration.ofSeconds(5))
    .pollInterval(Duration.ofMillis(500))
    .untilAsserted {
        val getResponse = runTest { apiClient.getEntity(entityId) }
        assertEquals("active", getResponse.body<EntityResponse>().status,
            "Entity $entityId should transition to 'active' after creation")
    }
```

## Why

- Eventually-consistent writes propagate asynchronously (Kafka consumer lag, cache TTL, async DB replication)
- Immediate assertion races against the propagation delay
- Test passes locally (fast DB) but fails in CI (slower infra) — classic flaky test
- `Thread.sleep(3000)` is not a fix: it wastes time in the happy case and still flakes under load

## Detection

```bash
grep -B5 -A2 "assertEquals.*status\|shouldBe.*status" src/test/kotlin/ | grep -B7 "getEntity\|findEntity\|listEntit"
grep -rn "Thread.sleep\|delay(" src/test/kotlin/ | grep -v "@Disabled"
```

Look for: write call → immediate read call → status assertion without `await()`.

## When This Applies

- System uses Kafka/RabbitMQ for event propagation
- Read-after-write crosses service boundaries (write to Service A, read from Service B)
- Cache layer with TTL between write and read paths
- Database replication lag (read replica)

## When This Does NOT Apply

- Strong consistency (single DB, same transaction for write and read)
- The API specification guarantees synchronous write-read consistency

## References

- (ref: api/eventual-consistency-writes.md)
- Related: `platform/flaky-sleep-tests.md`
- Related: `platform/controlled-retries.md`
