# Scenario Checklist

Two quality gate checklists + critical gotchas.

---

## Pre-Flight (Commit Gate)

Run before writing any code:

- [ ] Load profile defined (RPS targets, ramp-up duration, hold duration)
- [ ] AutoStop thresholds specified (error rate %, p95/p99 latency)
- [ ] Test data source identified (CSV dataset / DB seed query / random generation)
- [ ] No duplicate scenario for same endpoint: `find src/test/java/scenarios -name "*.kt"`
- [ ] JMETER_SCENARIO key is unique (not already in scenario_config.yaml)

---

## PR Gate (Compilation + Review)

Run after code is written, before merge:

- [ ] `./gradlew compileTestKotlin testClasses -x test` -> BUILD SUCCESS
- [ ] No shared sampler names across helper methods
- [ ] `scenario_config.yaml` entry added with matching JMETER_SCENARIO key
- [ ] `autoStop` block present with both error rate AND latency conditions
- [ ] `Util.params(JMETER_SCENARIO)` called in testPlan (loads Vault secrets)
- [ ] `Util.influxlistener(JMETER_SCENARIO)` present (pushes metrics to InfluxDB)
- [ ] `Stats.stats = testplan.overall()` after `.run()` (captures stats for reporting)
- [ ] `.maxThreads(N)` set on ALL `rpsThreadGroup` instances (never unbounded)
- [ ] No hardcoded URLs or credentials (use `System.getenv()` + Vault)
- [ ] Ramp-up duration >= 30 seconds (stabilization period)

---

## Gotchas

### 1. One-Time Function Execution in testPlan

Kotlin functions called inside `testPlan` but **outside** `jsr223Sampler` execute **once** during testPlan creation — not per iteration.

```kotlin
// WRONG: generateId() called once, same value for all iterations
rpsThreadGroup().children(
    httpSampler("POST /orders", "http://$host/orders")
        .body("{\"id\": \"${generateId()}\"}")  // static after creation!
)

// CORRECT: wrap in jsr223PreProcessor for per-iteration execution
rpsThreadGroup().children(
    httpSampler("POST /orders", "http://$host/orders")
        .body("{\"id\": \"\${orderId}\"}")
        .children(
            jsr223PreProcessor("vars.put('orderId', generateId())")
        )
)
```

### 2. RPS Distribution Across Samplers

Total RPS in `rpsThreadGroup` is **evenly distributed** across ALL samplers in the group.

```kotlin
// With 2 samplers at 100 RPS total:
// request_1 gets ~50 RPS, request_2 gets ~50 RPS
rpsThreadGroup()
    .rampToAndHold(100.0, Duration.ofMinutes(3), Duration.ofMinutes(3))
    .children(
        httpSampler("request_1", ...),  // ~50 RPS
        httpSampler("request_2", ...),  // ~50 RPS
    )
```

The more samplers added, the less load each one receives.

### 3. Separate rpsThreadGroups for Independent RPS

If flow A needs 100 RPS and flow B needs 50 RPS, use **separate** `rpsThreadGroup` blocks:

```kotlin
testPlan(
    rpsThreadGroup()
        .maxThreads(200)
        .rampToAndHold(100.0, Duration.ofMinutes(1), Duration.ofMinutes(10))
        .children(flowA(host)),     // 100 RPS dedicated
    rpsThreadGroup()
        .maxThreads(100)
        .rampToAndHold(50.0, Duration.ofMinutes(1), Duration.ofMinutes(10))
        .children(flowB(host)),     // 50 RPS dedicated
    // ...
)
```

Do NOT put both flows in one group expecting 150 total — RPS would be split ~75/~75.
