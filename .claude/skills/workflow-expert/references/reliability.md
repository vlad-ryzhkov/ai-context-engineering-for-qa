# Reliability Patterns Reference — /workflow-expert

## Runner Resource Limits

### GitHub-Hosted Runner Specifications

| Runner                   | vCPU    | RAM   | SSD    | Default Timeout |
| ------------------------ | ------- | ----- | ------ | --------------- |
| `ubuntu-latest`          | 2       | 7 GB  | 14 GB  | 6 hours         |
| `ubuntu-latest-4-cores`  | 4       | 16 GB | 150 GB | 6 hours         |
| `ubuntu-latest-8-cores`  | 8       | 32 GB | 300 GB | 6 hours         |
| `ubuntu-latest-16-cores` | 16      | 64 GB | 256 GB | 6 hours         |
| `macos-latest`           | 3 (M1)  | 7 GB  | 14 GB  | 6 hours         |
| `macos-latest-xlarge`    | 12 (M1) | 30 GB | 14 GB  | 6 hours         |
| `windows-latest`         | 2       | 7 GB  | 14 GB  | 6 hours         |

**Key constraint:** The 6-hour default timeout is a money burn risk. Always set explicit `timeout-minutes`.

---

## OOM Prevention

### Signs of OOM on GitHub Actions

- Job silently stops with no error message (exit code 137)
- `Killed` appears in step output without explanation
- GC loops visible in Java/Node logs before failure
- Build time increases dramatically, then job disappears

### Mitigation by Runtime

**Java / Kotlin (Gradle/Maven):**

```yaml
env:
  JAVA_OPTS: -Xmx4g -XX:+UseG1GC
  GRADLE_OPTS: -Dorg.gradle.jvmargs="-Xmx4g -XX:+HeapDumpOnOutOfMemoryError"
```

- Standard runner (7 GB): set `-Xmx3g` (leave room for OS + Gradle daemon)
- 4-core runner (16 GB): set `-Xmx12g`
- Use `--no-daemon` in CI to avoid memory leaks across builds

**Node.js:**

```yaml
env:
  NODE_OPTIONS: --max-old-space-size=4096
```

- Standard runner: `--max-old-space-size=5120` (5 GB, leaves 2 GB for OS)
- Watch for `FATAL ERROR: CALL_AND_RETRY_LAST Allocation failed - JavaScript heap out of memory`

**Jest / Vitest:**

```yaml
run: npx jest --max-workers=2
```

- Standard runner: max 2 workers (each worker ~1-2 GB for large suites)
- 4-core runner: max 3 workers
- `--runInBand` as last resort (serializes tests, slowest but lowest memory)

**Docker builds:**

- Multi-stage builds with `RUN` commands that install + compile → peak memory at build time
- Use `--memory` flag or upgrade runner for large builds
- BuildKit parallelism can spike memory: `DOCKER_BUILDKIT=1` with `--build-arg BUILDKIT_INLINE_CACHE=1`

---

## Timeout Isolation

### Job-Level Timeouts (Mandatory)

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    timeout-minutes: 5 # Fast checks: 5 min
  unit-test:
    runs-on: ubuntu-latest
    timeout-minutes: 15 # Unit tests: 15 min
  integration-test:
    runs-on: ubuntu-latest
    timeout-minutes: 30 # Integration: 30 min
  deploy:
    runs-on: ubuntu-latest
    timeout-minutes: 20 # Deploy: 20 min
```

**Rule:** Every job MUST have `timeout-minutes`. Without it, GitHub defaults to 6 hours — a stuck job burns $0.008/min ($2.88/run on standard, more on larger runners).

### Step-Level Timeouts for External Calls

```yaml
steps:
  - name: Deploy to staging
    timeout-minutes: 10
    run: ./scripts/deploy.sh staging

  - name: Health check
    timeout-minutes: 3
    run: |
      for i in $(seq 1 30); do
        curl -sf "$STAGING_URL/health" && exit 0
        sleep 5
      done
      exit 1
```

**Rule:** Any step that calls an external service (deploy, API call, health check) needs its own `timeout-minutes`.

---

## Deadlock Prevention

### Matrix + Self-Hosted Pool Deadlock

**Scenario:** Pool has 4 runners. Matrix generates 6 jobs. Jobs A-D start, consuming all runners. Jobs E-F wait. If A depends on E (via `needs:`), A waits for E, but E waits for a runner → deadlock.

**Prevention formula:**

```
max-parallel ≤ pool_size - max(jobs_in_dependency_chain)
```

```yaml
strategy:
  matrix:
    service: [auth, payments, users, notifications, billing, search]
  max-parallel: 3 # 4 runners, reserve 1 for dependent jobs
```

### Workflow-Level Deadlock

**Scenario:** Workflow A calls reusable workflow B with `workflow_call`. B calls C. If all three compete for the same concurrency group, B blocks waiting for A to finish, but A waits for B → deadlock.

**Prevention:**

- Use different concurrency groups for different workflow nesting levels
- Concurrency group should include the workflow name: `group: ${{ github.workflow }}-${{ github.ref }}`

---

## Flaky Test Isolation

### Selective Retry with `nick-fields/retry`

```yaml
- uses: nick-fields/retry@<SHA> # v3
  with:
    max_attempts: 3
    timeout_minutes: 10
    command: npm run test:e2e
    retry_on: error
```

**When to use:** Only for genuinely non-deterministic failures (network timeouts, container startup races).

**When NOT to use:** Never as a substitute for fixing the root cause. If a test fails > 10% of runs, fix it.

### Quarantine Pattern

```yaml
jobs:
  stable-tests:
    runs-on: ubuntu-latest
    steps:
      - run: npm run test -- --exclude-tags quarantine

  quarantine-tests:
    runs-on: ubuntu-latest
    continue-on-error: true # OK here — quarantine is explicitly non-blocking
    steps:
      - run: npm run test -- --tags quarantine
```

**Rule:** `continue-on-error: true` is acceptable ONLY on explicitly quarantined jobs. Never on production-path steps.
