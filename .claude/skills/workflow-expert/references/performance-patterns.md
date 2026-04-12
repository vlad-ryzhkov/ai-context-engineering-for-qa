# Performance Patterns Reference — /workflow-expert

## Multi-Level Caching Strategies

### Dependency Caching

Most setup actions have built-in caching. Prefer built-in over manual `actions/cache` when available.

```yaml
# Node.js — built-in cache
- uses: actions/setup-node@<SHA> # v4
  with:
    node-version: 20
    cache: "npm"

# Gradle — built-in cache
- uses: gradle/actions/setup-gradle@<SHA>
  with:
    cache-read-only: ${{ github.ref != 'refs/heads/main' }}

# Python — built-in cache
- uses: actions/setup-python@<SHA> # v5
  with:
    python-version: "3.12"
    cache: "pip"

# Go — built-in cache
- uses: actions/setup-go@<SHA> # v5
  with:
    go-version: "1.22"
    cache: true
```

### Manual Cache with `actions/cache`

Use when built-in caching is insufficient or unavailable.

```yaml
- uses: actions/cache@<SHA> # v4
  with:
    path: |
      ~/.cache/pip
      ~/.local/share/virtualenvs
    key: ${{ runner.os }}-pip-${{ hashFiles('**/requirements.txt') }}
    restore-keys: |
      ${{ runner.os }}-pip-
```

**Cache key strategy:**

- Primary key: exact match on lock file hash
- Restore keys: progressively broader fallbacks
- Include `runner.os` to avoid cross-platform cache conflicts

### Cache Size Limits

- GitHub-hosted: 10 GB per repository (LRU eviction after 7 days)
- Self-hosted: configurable per runner
- Individual cache entries: max 10 GB

---

## Docker Layer Caching

### Build-Push Action with Registry Cache

```yaml
- uses: docker/build-push-action@<SHA>
  with:
    context: .
    push: true
    tags: ${{ env.IMAGE_TAG }}
    cache-from: type=registry,ref=${{ env.REGISTRY }}/${{ env.IMAGE }}:cache
    cache-to: type=registry,ref=${{ env.REGISTRY }}/${{ env.IMAGE }}:cache,mode=max
```

### GitHub Actions Cache Backend

```yaml
- uses: docker/build-push-action@<SHA>
  with:
    context: .
    push: true
    tags: ${{ env.IMAGE_TAG }}
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### Dockerfile Optimization for Cache Hits

```dockerfile
# 1. Static base (changes rarely)
FROM node:20-alpine

# 2. Dependencies (changes on package.json update)
COPY package.json package-lock.json ./
RUN npm ci --production

# 3. Application code (changes on every commit)
COPY . .
RUN npm run build
```

**Rule:** Most volatile layers go LAST to maximize cache reuse.

---

## Concurrency Management

### Basic: Cancel Redundant Runs

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

- **Group key:** workflow name + branch ref ensures one active run per branch
- **cancel-in-progress:** kills older runs when a new push arrives

### Advanced: Protect Main Branch Deploys

```yaml
concurrency:
  group: deploy-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
```

- Feature branches: cancel older runs (safe)
- Main branch: queue deployments, never cancel (protects releases)

### Per-Environment Concurrency

```yaml
jobs:
  deploy:
    concurrency:
      group: deploy-${{ inputs.environment }}
      cancel-in-progress: false
    environment: ${{ inputs.environment }}
```

- Prevents parallel deploys to the same environment
- Different environments can deploy concurrently

---

## Dynamic Matrix with `fromJson()`

### Problem: Static Matrix in Monorepo

```yaml
# BAD — runs all 15 services even when only 1 changed
strategy:
  matrix:
    service: [auth, payments, users, notifications, ...]
```

### Solution: Two-Phase Dynamic Matrix

**Phase 1 — Generate matrix from changed paths:**

```yaml
jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.set-matrix.outputs.matrix }}
      has_changes: ${{ steps.set-matrix.outputs.has_changes }}
    steps:
      - uses: actions/checkout@<SHA>
        with:
          fetch-depth: 0          # need history for diff
      - id: detect
        # Zero-dependency path detection — no third-party actions needed
        run: |
          CHANGED=$(git diff --name-only ${{ github.event.before }}..${{ github.sha }} -- services/)
          SERVICES=()
          for svc in auth payments users; do
            if echo "$CHANGED" | grep -q "^services/${svc}/"; then
              SERVICES+=("\"$svc\"")
            fi
          done
          if [ ${#SERVICES[@]} -eq 0 ]; then
            echo "has_changes=false" >> "$GITHUB_OUTPUT"
            echo 'matrix=[]' >> "$GITHUB_OUTPUT"
          else
            MATRIX=$(IFS=,; echo "[${SERVICES[*]}]")
            echo "has_changes=true" >> "$GITHUB_OUTPUT"
            echo "matrix=$MATRIX" >> "$GITHUB_OUTPUT"
          fi
```

**Phase 2 — Use dynamic matrix:**

```yaml
test:
  needs: detect-changes
  if: needs.detect-changes.outputs.has_changes == 'true'
  strategy:
    matrix:
      service: ${{ fromJson(needs.detect-changes.outputs.matrix) }}
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@<SHA>
    - run: cd services/${{ matrix.service }} && make test
```

---

## Fail-Fast Architecture

### Tiered Job Execution

```yaml
jobs:
  # Tier 1: Fast checks (< 1 min)
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@<SHA>
      - run: npm run lint
      - run: npm run typecheck

  # Tier 2: Unit tests (1-5 min) — depends on lint
  unit-test:
    needs: lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@<SHA>
      - run: npm test

  # Tier 3: Integration tests (5-15 min) — depends on unit
  integration-test:
    needs: unit-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@<SHA>
      - run: npm run test:integration

  # Tier 4: E2E tests (15+ min) — depends on integration
  e2e-test:
    needs: integration-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@<SHA>
      - run: npm run test:e2e
```

**Benefits:**

- Fast feedback: lint errors caught in < 1 minute
- Resource savings: no E2E runs if lint fails
- Clear failure isolation: each tier narrows the problem scope

---

## Reusable Workflow Centralization

### When to Extract

Extract a reusable workflow when:

- Same logic exists in 3+ repositories
- Organization needs to enforce a standard (security scan, SBOM generation)
- Workflow requires specialized knowledge to maintain

### Structure

```
org/shared-workflows/
└── .github/workflows/
    ├── ci-build.yml          # Build + test template
    ├── ci-security-scan.yml  # Mandatory security checks
    ├── cd-deploy.yml         # Deployment with environment gates
    └── cd-release.yml        # Release + changelog generation
```

### Caller Pattern

```yaml
jobs:
  build:
    uses: org/shared-workflows/.github/workflows/ci-build.yml@<SHA>
    with:
      node-version: "20"
      test-command: "npm test"
    secrets:
      NPM_TOKEN: ${{ secrets.NPM_TOKEN }}
```

---

## Runner Optimization

### Job-Runner Matching

| Job Type          | Recommended Runner               | Rationale                          |
| ----------------- | -------------------------------- | ---------------------------------- |
| Lint, typecheck   | `ubuntu-latest` (2-core)         | Low compute, fast startup          |
| Unit tests        | `ubuntu-latest` (2-core)         | Sufficient for most suites         |
| Docker build      | `ubuntu-latest` or larger runner | I/O bound, benefits from SSD       |
| E2E / integration | Larger runner (4-8 core)         | CPU-bound, parallel test execution |
| iOS build         | `macos-latest`                   | Required for Xcode toolchain       |

### Runner Sizing, OOM Prevention & Deadlocks

See `references/reliability.md` for full runner specs table, OOM mitigation by runtime, and matrix deadlock prevention formula.

### Self-Hosted Runner Considerations

- Use labels to target specific hardware: `runs-on: [self-hosted, gpu, linux]`
- Always clean workspace after jobs to avoid state leakage
- Monitor runner utilization; scale horizontally for queue depth > 0
