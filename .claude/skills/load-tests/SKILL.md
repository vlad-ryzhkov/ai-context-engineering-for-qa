---
name: load-tests
description: "Generates JMeter DSL (Kotlin) load test scenarios with helpers and scenario_config.yaml entries. Use when creating new load test scenarios."
allowed-tools: "Read Write Edit Glob Grep Bash"
agent: perf-engineer
context: fork
---

# /load-tests — Load Test Scenario Generator

<purpose>
Generate compilable JMeter DSL (Kotlin) load test scenarios.
Output: helper file + scenario class + scenario_config.yaml entry.
</purpose>

## SILENT MODE

Do NOT output intermediate commentary. No "Let me read...", "I will now...", "The file contains...".
Output ONLY tool invocations and completion blocks (DONE / BLOCKER / STATUS).

## Loop Guard

If `./gradlew compileTestKotlin testClasses -x test` fails more than 3 times consecutively:

1. Stop attempting fixes
2. Output `BLOCKER: Compilation failed 3 times` with the last stderr
3. Wait for user input

## Before Starting

Read `.claude/agents/perf-engineer.md` — it defines anti-patterns, quality gates, and tech stack constraints that govern all output.

## Phase Checkpoints

```text
STOP  if: service name not provided | no endpoints specified | load profile missing
WARN  if: scenario_config.yaml has no entry for this service yet | helper directory does not exist
INFORM: detected existing helpers/scenarios for the service, will reuse
```

---

## Input

Collect from the user (all 4 required):

| #   | Parameter        | Example                                            | Required |
| --- | ---------------- | -------------------------------------------------- | -------- |
| 1   | **Service name** | `new-order`, `passenger-new`, `api-gw`             | Yes      |
| 2   | **Endpoints**    | `POST /v1/orders`, `GET /v1/status/{id}`           | Yes      |
| 3   | **Load profile** | Test type + RPS targets + duration                 | Yes      |
| 4   | **Test data**    | CSV file path, DB seed query, or random generation | Yes      |

If any parameter is missing — STOP and ask.

---

## Step 1: Discovery

Search the codebase for existing artifacts related to the target service.

```text
1. Glob: src/test/java/scenarios/<service>/**/*.kt    → existing scenarios
2. Glob: src/main/java/helpers/<service>/**/*.kt       → existing helpers
3. Grep: scenario_config.yaml for service name         → existing config entries
4. Glob: src/test/resources/dataset/<service>*          → existing test data
```

**If existing scenarios found:**

- Read them to understand current patterns (imports, host resolution, thread group style)
- Reuse existing helpers where endpoints overlap
- Ensure new JMETER_SCENARIO key does not collide with existing keys

**If no existing artifacts:**

- Create new helper file + scenario file from scratch
- Create new scenario_config.yaml entry

---

## Step 2: Load Profile Selection

Select thread group pattern based on test type. Reference: `references/test-types.md`.

| Test Type           | Thread Group                                               |
| ------------------- | ---------------------------------------------------------- |
| Maximum Performance | Stepped `rampToAndHold` via fold or explicit chain         |
| Stability / Soak    | Single `.rampToAndHold(rps, 5min, 12h)` at 70-90% max      |
| Stress              | Ramp to 200-300% -> hold -> ramp down to normal            |
| Spike               | Short ramp (30s) to 200-400% -> hold 2min -> back          |
| Volume              | `threadGroup(N, Duration.ofHours(2))` with growing dataset |
| Endurance           | `.rampToAndHold(rps, 5min, Duration.ofDays(1))`            |
| Reliability         | Normal load + ChaosMesh integration                        |

**Mandatory for all types:**

- `.maxThreads(N)` on every `rpsThreadGroup` — NEVER leave unbounded
- `autoStop` block with error rate + latency thresholds
- Ramp-up duration >= 30 seconds (stabilization period)

---

## Step 3: Helper Generation

Create or update helper file at `src/main/java/helpers/<service>/<ServiceName>Helpers.kt`.

### HTTP Sampler Pattern

```kotlin
package helpers.<service>

import org.apache.jmeter.protocol.http.util.HTTPConstants
import us.abstracta.jmeter.javadsl.JmeterDsl.*
import us.abstracta.jmeter.javadsl.http.DslHttpSampler

fun getResource(host: String): DslHttpSampler = httpSampler(
    "GET /api/v1/resource", "http://$host:8080/api/v1/resource"
).method(HTTPConstants.GET)
    .header("Content-Type", "application/json")
```

### gRPC Sampler Pattern

```kotlin
fun grpcCall(host: String) = httpSampler(
    "gRPC serviceName/MethodName", "http://$host:8080"
).method(HTTPConstants.POST)
    .header("Content-Type", "application/grpc")
    .body(protoBody)
```

### Rules

- **Sampler name format:** `"METHOD /path"` — e.g., `"POST /v1/orders"`, `"GET /v1/status"`
- **No duplicate names** across the entire helper file
- **No hardcoded URLs** — always use `host` parameter
- **No hardcoded credentials** — use JMeter variables `${token}`, `${apiKey}` populated via Util.params()
- **jsonExtractor** for response data extraction when chaining requests

---

## Step 4: Scenario Generation

Create scenario file at `src/test/java/scenarios/<service>/<ScenarioName>.kt`.

### Pattern A — Single Test Class (default for non-scenario tests)

```kotlin
package scenarios.<service>

import helpers.<service>.*
import org.junit.jupiter.api.Test
import us.abstracta.jmeter.javadsl.JmeterDsl.*
import us.abstracta.jmeter.javadsl.core.listeners.AutoStopListener.AutoStopCondition.errors
import us.abstracta.jmeter.javadsl.core.listeners.AutoStopListener.AutoStopCondition.latencyTime
import utils.Stats
import utils.Util
import java.time.Duration

class ScenarioNameTest {

    val JMETER_SCENARIO = "service-scenario-key"

    @Test
    @Throws(NullPointerException::class)
    fun test() {
        val testplan = testPlan(
            Util.params(JMETER_SCENARIO, use_local_env = true),
            Util.influxlistener(JMETER_SCENARIO),
            rpsThreadGroup()
                .maxThreads(200)
                .rampToAndHold(50.0, Duration.ofMinutes(1), Duration.ofMinutes(10))
                .children(
                    getResource(host)
                ),
            autoStop().`when`(errors().percent().greaterThan(10.0)),
            autoStop().`when`(latencyTime().percentile(95.0).greaterThan(Duration.ofSeconds(5))),
            resultsTreeVisualizer()
        ).run()

        Stats.stats = testplan.overall()
    }
}
```

### Pattern B — Abstract Base + Variants (for scenario tests or multi-profile tests)

**Abstract base:**

```kotlin
package scenarios.<service>

import utils.Util

abstract class BaseScenarioName {
    val namespace = System.getenv("NAMESPACE") ?: ""
    val host = System.getenv("host") ?: "service-${Util.hashNs(namespace)}.${System.getenv("BASE_DOMAIN") ?: "example.dev"}"

    protected abstract fun setupLoadModel(): /* thread group type */
}
```

**Concrete variant:**

```kotlin
class ScenarioMaxPerf : BaseScenarioName() {

    val JMETER_SCENARIO = "service-max-perf"

    override fun setupLoadModel() = rpsThreadGroup()
        .maxThreads(500)
        .rampToAndHold(10.0, Duration.ofSeconds(60), Duration.ofMinutes(5))
        .rampToAndHold(20.0, Duration.ofSeconds(60), Duration.ofMinutes(5))
        .rampToAndHold(30.0, Duration.ofSeconds(60), Duration.ofMinutes(5))

    @Test
    fun test() { /* same testPlan structure as Pattern A */ }
}
```

### Host Resolution

- Use `System.getenv("host")` with fallback to namespace-based URL
- For base domain: `System.getenv("BASE_DOMAIN") ?: "example.dev"` — **customize per project**
- Pattern: `"https://service-${hashNs(namespace)}.$BASE_DOMAIN"`

### Mandatory Elements in Every testPlan

| Element                                              | Purpose                                  |
| ---------------------------------------------------- | ---------------------------------------- |
| `Util.params(JMETER_SCENARIO, use_local_env = true)` | Load Vault secrets, set JMeter variables |
| `Util.influxlistener(JMETER_SCENARIO)`               | Push metrics to InfluxDB for Grafana     |
| `autoStop().when(errors()...)`                       | Circuit breaker on error rate            |
| `autoStop().when(latencyTime()...)`                  | Circuit breaker on latency               |
| `resultsTreeVisualizer()`                            | Local debugging tree                     |
| `Stats.stats = testplan.overall()`                   | Capture stats for reporting              |

---

## Step 5: Config Update

Add entry to `src/test/resources/scenario_config.yaml`:

```yaml
<jmeter-scenario-key>:
  enableManager: true
  report:
    service: ["<service-name>"]
    atlassian_folder_id: "<FOLDER_ID>"
    slack_channel: <CHANNEL_ID>
    default_dashboard: true
    tagged_dashboard: ["http"]
```

- Ask user for `atlassian_folder_id` and `slack_channel` if not known
- Set `tagged_dashboard` based on protocol: `["http"]` for REST, `["kafka"]` for Kafka, `["http", "kafka"]` for mixed
- JMETER_SCENARIO key must match the `val JMETER_SCENARIO` in the scenario class

---

## Step 6: Compilation

Run:

```bash
./gradlew compileTestKotlin testClasses -x test
```

- On `BUILD SUCCESS` — proceed to Step 7
- On `BUILD FAILED` — fix the error, re-run (max 3 attempts, then BLOCKER per Loop Guard)
- Output only stderr on failure, no progress messages

---

## Step 7: Quality Gate

Self-review checklist before completion:

- [ ] `./gradlew compileTestKotlin testClasses -x test` -> BUILD SUCCESS
- [ ] No shared sampler names across helper methods
- [ ] `scenario_config.yaml` entry added with correct JMETER_SCENARIO key
- [ ] `autoStop` block present with error rate AND latency thresholds
- [ ] `Util.params()` + `Util.influxlistener()` present in testPlan
- [ ] `Stats.stats = testplan.overall()` after `.run()`
- [ ] `.maxThreads(N)` on all `rpsThreadGroup` instances
- [ ] No hardcoded URLs or credentials
- [ ] Ramp-up duration >= 30 seconds

If any item fails — fix before outputting completion.

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

---

## Anti-Patterns (BANNED)

| Pattern                   | Why                                    | Fix                                           |
| ------------------------- | -------------------------------------- | --------------------------------------------- |
| Uncapped `rpsThreadGroup` | Unbounded load can destroy infra       | Always `.maxThreads(N)`                       |
| Missing `autoStop`        | Test continues while infra burns       | Mandatory error rate + latency thresholds     |
| Static test data          | Contention, false cache hits           | CSV/DB seed per VU                            |
| Shared sampler names      | Grafana dashboards become unreadable   | Unique `"METHOD /path"` per sampler           |
| Short stabilization       | False failures from cold start         | Min 30s ramp-up                               |
| Hardcoded URLs/creds      | Security violation, breaks across envs | `System.getenv()` + Vault via `Util.params()` |

---

## Gotchas

1. **Kotlin functions in testPlan (outside jsr223Sampler) execute once** at testPlan creation time, not per iteration. To generate dynamic values per iteration, wrap calls in `jsr223Sampler`, `jsr223PreProcessor`, or `jsr223PostProcessor`.

2. **RPS in rpsThreadGroup is total across ALL samplers** in the group. With 2 samplers at 100 RPS total, each gets ~50 RPS. For independent per-flow RPS, use separate `rpsThreadGroup` instances.

3. **Separate rpsThreadGroups for independent RPS.** If flow A needs 100 RPS and flow B needs 50 RPS, create two `rpsThreadGroup` blocks — do NOT put both in one group expecting 150 total.

---

## Output Format

On successful completion, output:

```text
SKILL COMPLETE: /load-tests

| Artifact | Path |
|----------|------|
| Helper   | src/main/java/helpers/<service>/<Name>Helpers.kt |
| Scenario | src/test/java/scenarios/<service>/<ScenarioName>.kt |
| Config   | src/test/resources/scenario_config.yaml (entry: <key>) |
| Build    | BUILD SUCCESS |

Test type: <type>
Load profile: <rps> RPS, <ramp> ramp, <duration> hold
AutoStop: errors > <N>%, p95 latency > <N>s
```

---

## Related Files

- Agent: `.claude/agents/perf-engineer.md`
- Test types reference: `.claude/skills/load-tests/references/test-types.md`
- Scenario checklist: `.claude/skills/load-tests/references/scenario-checklist.md`
- Existing scenarios: `src/test/java/scenarios/` (target project)
- Existing helpers: `src/main/java/helpers/` (target project)
- Config: `src/test/resources/scenario_config.yaml` (target project)
