# Test Types Reference

Thread group patterns for each load test type.

---

## 1. Maximum Performance Test / Find Max RPS

**When to use:**

- Initial system testing to determine performance limits
- Before scaling decisions
- Resource planning

**Thread Group Pattern — Stepped rampToAndHold:**

```kotlin
// Option A: Explicit steps
rpsThreadGroup()
    .maxThreads(1000)
    .rampToAndHold(10.0, Duration.ofSeconds(60), Duration.ofMinutes(5))
    .rampToAndHold(20.0, Duration.ofSeconds(60), Duration.ofMinutes(5))
    .rampToAndHold(30.0, Duration.ofSeconds(60), Duration.ofMinutes(5))
    .rampToAndHold(40.0, Duration.ofSeconds(60), Duration.ofMinutes(5))
    // ... continue until failure point

// Option B: Using fold (dynamic step count)
(10..100 step 10).fold(rpsThreadGroup().maxThreads(500)) { acc, rps ->
    acc.rampToAndHold(rps.toDouble(), Duration.ofSeconds(60), Duration.ofMinutes(5))
}
```

**Duration:** Medium (1-3 hours)
**AutoStop thresholds:**

- Error rate > 10%
- p95 latency > 10x baseline
- Successful ops plateau (throughput does not increase with load growth)

**Success criteria:**

- Maximum load level (Lmax) determined
- System stable at ~90% of Lmax

---

## 2. Stability Test / Soak Test

**When to use:**

- Check for memory/goroutine leaks
- Before production release
- Verify long-term stability
- When resource issues suspected

**Thread Group Pattern — Single rampToAndHold:**

```kotlin
rpsThreadGroup()
    .maxThreads(200)
    .rampToAndHold(24.0, Duration.ofSeconds(300), Duration.ofHours(12))
    .children(
        // samplers
    )
```

**Duration:** Long (12+ hours)
**Load level:** 70-90% of maximum performance
**AutoStop thresholds:**

- Error rate > 5% (stricter than max perf — stability matters)
- p95 latency > 2x baseline

**Success criteria:**

- No memory consumption growth over time
- Response time remains stable
- Error count does not increase
- No resource overflow (Kafka lag, NATS ack pending)

---

## 3. Stress Test

**When to use:**

- Verify resilience to overloads
- Verify protection mechanisms (rate limits, circuit breakers)
- Before high-traffic events (Black Friday, promotions)
- Verify autoscaling (HPA)

**Thread Group Pattern — Ramp up, hold, ramp down:**

```kotlin
rpsThreadGroup()
    .maxThreads(1000)
    .rampToAndHold(100.0, Duration.ofSeconds(300), Duration.ofMinutes(10))  // Normal load
    .rampToAndHold(200.0, Duration.ofSeconds(60), Duration.ofMinutes(5))    // Stress: 200%
    .rampToAndHold(100.0, Duration.ofSeconds(300), Duration.ofMinutes(10))  // Recovery
```

**Duration:** Medium (1-2 hours)
**Load level:** 200-300% of normal
**AutoStop thresholds:**

- Error rate > 30% (allow higher errors under stress)
- p95 latency > 30s

**Success criteria:**

- System does not crash under extreme load
- System recovers after load reduction
- Protection mechanisms work correctly
- No data loss

---

## 4. Spike Test

**When to use:**

- Verify reaction to sudden events (viral content, promotions)
- Verify autoscaling
- Simulate DDoS (controlled)
- Before events with unpredictable load

**Thread Group Pattern — Short ramp, brief hold, back:**

```kotlin
rpsThreadGroup()
    .maxThreads(800)
    .rampToAndHold(50.0, Duration.ofSeconds(60), Duration.ofMinutes(10))    // Steady load
    .rampToAndHold(200.0, Duration.ofSeconds(30), Duration.ofMinutes(2))    // Spike: 30s ramp
    .rampToAndHold(50.0, Duration.ofSeconds(60), Duration.ofMinutes(10))    // Return to normal
```

**Duration:** Short (10-30 minutes)
**Load level:** Sharp spike to 200-400% of normal
**AutoStop thresholds:**

- Error rate > 20%
- p95 latency > 15s

**Success criteria:**

- System handles spike without critical errors
- Autoscaling triggers quickly
- System recovers quickly after spike

---

## 5. Volume Test

**When to use:**

- Working with large data volumes
- Verify database performance as data grows
- Determine optimal data size
- Before data archiving or cleanup

**Thread Group Pattern — Fixed threads, long duration:**

```kotlin
// Volume tests typically use threadGroup (not rpsThreadGroup)
// because the focus is data volume growth, not constant RPS
threadGroup(10, Duration.ofHours(2))
    .children(
        createOrder(host),    // Operations that create data
        getOrders(host),      // Operations that read growing dataset
    )
```

**Duration:** Medium (2-4 hours)
**Load level:** Constant, but data volume grows
**AutoStop thresholds:**

- Error rate > 10%
- p95 latency > 10s (may increase naturally as data grows)

**Success criteria:**

- System processes large data volumes without degradation
- Response time remains within acceptable limits
- No memory issues

---

## 6. Endurance Test

**When to use:**

- Verify long-term stability
- Before long operations (migrations, large data processing)
- Verify 24/7 systems

**Thread Group Pattern — Extended hold:**

```kotlin
rpsThreadGroup()
    .maxThreads(200)
    .rampToAndHold(50.0, Duration.ofSeconds(300), Duration.ofDays(1))
    .children(
        // samplers
    )
```

**Duration:** Very long (24+ hours)
**Load level:** Steady (typical production load)
**AutoStop thresholds:**

- Error rate > 5%
- p95 latency > 3x baseline

**Success criteria:**

- No performance degradation over time
- No error accumulation
- Resources remain stable

---

## 7. Reliability Test

**When to use:**

- Verify fault tolerance
- Verify recovery mechanisms
- Before important releases
- Verify operation during dependency failures

**Thread Group Pattern — Normal load + failure injection:**

```kotlin
rpsThreadGroup()
    .maxThreads(200)
    .rampToAndHold(50.0, Duration.ofSeconds(300), Duration.ofMinutes(30))
    .children(
        // Normal operations
        getResource(host),
        // Chaos Mesh integration for failure injection
        // chaosMeshExperiment("pod-failure"),
    )
```

**Duration:** Medium (1-2 hours)
**Load level:** 70% of maximum + failure injection
**AutoStop thresholds:**

- Error rate > 50% (expect errors during failures)
- p95 latency > 30s

**Success criteria:**

- System continues working during partial failures
- System recovers within required time
- No data loss during failures

---

## Comparison Table

| Test Type           | Duration | Load Level            | Primary Objective            |
| ------------------- | -------- | --------------------- | ---------------------------- |
| Maximum Performance | 1-3h     | Up to failure point   | Determine max RPS            |
| Stability (Soak)    | 12+h     | 70-90% of max         | Detect leaks and degradation |
| Stress              | 1-2h     | 200-300% of normal    | Verify overload resilience   |
| Spike               | 10-30min | Sharp spike 200-400%  | Verify sudden event reaction |
| Volume              | 2-4h     | Constant, data grows  | Verify large data handling   |
| Endurance           | 24+h     | Steady                | Verify long-term stability   |
| Reliability         | 1-2h     | 70% of max + failures | Verify fault tolerance       |

---

## Choosing the Right Test Type

**Quality Gate tests:**

- Stability (Soak) or Maximum Performance
- Load: 100-120% of current peak production RPS

**Performance verification:**

- Start with Maximum Performance (find limits)
- Then Stability (verify long-term operation)

**Before high-traffic events:**

- Stress + Spike tests

**Fault tolerance:**

- Reliability test with Chaos Mesh
