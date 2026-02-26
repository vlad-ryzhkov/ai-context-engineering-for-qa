---
name: repo-scout
description: Scans a backend repository (Go, Python, Node.js, Java/Kotlin), catalogs API surface, infrastructure, test coverage, and generates QA scenario priority matrix. Use when entering a new repo before writing tests. Do not use for QA projects — use /init-project for those.
allowed-tools: "Read Glob Grep Bash(ls*) Bash(wc*)"
agent: agents/sdet.md
context: fork
---

# /repo-scout — Backend Repository Reconnaissance

<purpose>
Deep scanning of a backend repository → structured report on API surface, architecture,
infrastructure, current test coverage, entity dependencies, behavioral nuances, and QA scenario
priority matrix. Gives AI and humans a complete picture of the service before planning test coverage.
</purpose>

## Scope (SINGLE AUTHORITY)

**repo-scout is the ONLY skill responsible for initial repository discovery.**
- It reads, catalogs, and extracts business logic facts (validations, errors, auth flows).
- It detects entity dependencies, state machines, behavioral nuances, and QA scenario priorities.
- It NEVER evaluates code quality, suggests improvements, or generates test code.
- It NEVER generates test files or execution plans.

## When to Use

- First entry into a new backend repository
- Before `/api-isolated-tests` — for data collection
- Before `/init-project` — to understand the target service
- Periodic audit: "what changed in the API surface?"

## When NOT to Use

- QA projects with tests (use `/init-project`)
- Code review (use standard tools)
- Frontend/mobile repositories

## Input Data

- Path to repository (or current directory)
- Does not require CLAUDE.md, qa_agent.md or other AI files
- Can be the **first step** in a new repo

## Algorithm

## Verbosity Protocol

- **Output:** All analysis → artifact (MD), not chat. Chat: max 5-line summary + `📊 Full report: {path}`
- **Checkpoints:** Phase transitions + warnings only. No per-file progress.
- **Tools first:** Grep/Read → table → report. No "Now I will..." preambles.
- **Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist).
- **Phases 1-8:** Silent. **Phase 9:** Summary table + report path (timestamp: `YYYYMMDD_HHMMSS`).

### Before Starting

Read `.claude/qa_agent.md` (if present in the working project). Output:

```text
📋 TASK BRIEF
├─ Target: {repo-name} — backend service reconnaissance
├─ Scope: API surface + infrastructure + test coverage
├─ Constraint: Read-only, backend patterns only (Go / Python / Node.js / Java/Kotlin)
└─ Action: Invoking /repo-scout...
```

### Phase 1: File System Scan

**Goal:** Determine language, build system, directory structure.

1. **Detect language** — check build files using the detection table in `references/lang-patterns.md`:
   ```text
   Glob: go.mod, package.json, pom.xml, build.gradle.kts, build.gradle, requirements.txt, pyproject.toml, setup.py, Cargo.toml
   ```
   - Record detected language(s). If multiple → monorepo, note all.
   - If none → ⚠️ WARNING: No known build file found. Generic scan only.
   - All subsequent pattern lookups use `references/lang-patterns.md` section for detected language.

2. Read primary build file for metadata:
   Per `references/lang-patterns.md` → Build Files for detected language, extract: module/package name, runtime version, key dependencies.

3. Determine structure:
   Use `references/lang-patterns.md` for detected language to identify entry-point directories and module layout.

4. Count size:
   Use `references/lang-patterns.md` → Test Patterns for detected language to distinguish source vs test files.
   Record: number of source files, number of test files.

### Phase 2: API Surface Discovery

**Goal:** Find and catalog ALL API endpoints.

#### 2.1 OpenAPI / Swagger

Search for files:
```text
Glob: **/swagger.json, **/swagger.yaml, **/swagger.yml, **/openapi.json, **/openapi.yaml, **/openapi.yml, **/*.swagger.json
```

For each found file:
- If file exceeds 500 lines → summarize by tags/domains instead of listing every endpoint. Record exact relative file path for downstream skills to read directly.
- Otherwise read the file fully.
- Extract endpoints: Method, Path, Description
- Note presence/absence of response schemas, error codes

#### 2.2 Protocol Buffers (gRPC)

Search for files:
```text
Glob: **/*.proto
```

For each .proto file:
- Extract services and rpc methods
- Record Request/Response types
- Note streaming type (unary / server-stream / client-stream / bidirectional)
- Extract `validate` tags on message fields (`required`, `min`, `max`, `pattern`)
- Note deprecated RPCs/fields (if any)

#### 2.3 Route Registration (from code)

Read `references/lang-patterns.md` → section for detected language → use that language's **Grep String for Route Search**.

For each found:
- File + line
- HTTP method + path
- Handler function

⚠️ **Do not duplicate:** If endpoint already found in swagger/proto — do not add from code.

#### 2.4 HTTP Client Files

```text
Glob: **/*.http, **/api.http
```

If found — note as an additional source of examples.

#### 2.5 GraphQL

```text
Glob: **/*.graphql, **/schema.graphqls
Grep: type Query|type Mutation|ApolloServer|graphqlHTTP|GraphQLSchema|@GraphQLApi
```

If found: extract queries and mutations with types.

#### 2.6 Risk Classification

Flag **Sensitive Endpoints** (auth, login, token, billing, pay, wallet, users, profile, export) in the report — SDET will apply `@Severity(CRITICAL)` patterns.

### Phase 3: Business Logic Analysis

**Goal:** Extract business logic facts from handler code for downstream test generation.

**Token-saving strategy:** Skip DTO mapping / field copying blocks. Focus on conditional branches, error returns, validation calls, auth checks.

Reference: `references/lang-patterns.md` → Handler Patterns, Error Patterns, Validation Patterns, Auth/Middleware Patterns for detected language.

#### 3.1 Handler Analysis

For each endpoint/RPC discovered in Phase 2:
1. Locate the handler function body using lang-patterns.md → Handler Patterns
2. Read handler code — extract:
   - Input validations (parameter checks, schema validation calls)
   - Error branches (`if err`, `throw`, `raise`, status code returns)
   - Auth checks (token extraction, permission verification)
   - Key business rules (conditional logic that changes behavior)
3. Record per-endpoint: `Endpoint | Validations | Error Branches | Auth Check | Business Rules`

#### 3.2 Validation Rules

1. **Proto-level** (if gRPC): extract `validate` tags from Phase 2.2 results
2. **Code-level:** search for framework-specific validators using lang-patterns.md → Validation Patterns
3. Output: table per `references/report-template.md` § Validation Rules

#### 3.3 Error Mapping

1. Find all error constants/enums using lang-patterns.md → Error Patterns
2. Map each to HTTP status code and/or gRPC code
3. Output: table per `references/report-template.md` § Error Mapping

#### 3.4 Auth & Access Control

1. Find middleware/interceptors using lang-patterns.md → Auth/Middleware Patterns
2. Extract auth flow: token extraction → validation → failure handling
3. Classify endpoints: `PUBLIC` | `AUTH` | `ADMIN`
4. Output: endpoint auth matrix + auth flow diagram per report template

#### 3.5 State Transition Matrix (CONDITIONAL)

> Skip if no state machine patterns found. Use `references/lang-patterns.md` → State Machine Patterns.

1. Search for state/status enums using lang-patterns grep strings
2. For each enum found:
   - List all named states (e.g., `Pending`, `Active`, `Deleted`)
   - Trace transitions: grep for `.Status =` / `.State =` assignments in handlers/services
   - Record: `From → To | Trigger (handler/method) | Guard (if-condition) | Error on rejection`
3. Identify unreachable states: states defined in enum but never assigned in any transition
4. Identify multi-step test sequences — chains of transitions that represent real business flows (e.g., `created → active → suspended → reactivated`, `pending → approved → completed → archived`). Document the full chain with business flow description. Without this, tests will verify single-step transitions but miss the end-to-end lifecycle.
5. Output: table per `references/report-template.md` §11 (include Multi-Step Transition Sequences sub-table)

#### 3.6 Entity & Data Model Analysis (CONDITIONAL)

> Skip if no entity relationship patterns found. Use `references/lang-patterns.md` → Entity Relationship Patterns.

1. **CRUD Matrix:** For each entity, determine which operations exist (Create/Read/Update/Delete/Soft Delete)
2. **Entity Relationships:** Extract FK references from migrations, ORM tags, or struct fields
3. **Create-Order Chain:** Determine entity creation order based on FK dependencies (parent before child). Record reverse order for cleanup.
4. **Pagination:** Find endpoints with `cursor`/`offset`/`limit` parameters. Record strategy, defaults, max values.
5. **Data Consistency:** For each write→read pair, determine consistency model (strong/eventual). Check for `tx.Commit`, async events, cache layers.
6. **Batch Operations:** Find `BatchCreate`/`BulkInsert` patterns. Note error propagation strategy (atomic vs partial).
7. **Type Handling:** Find type conversion patterns (`strconv.Atoi`, `json.Number`, custom `UnmarshalJSON`). Note edge cases.
8. Output: tables per `references/report-template.md` §12

#### 3.7 Behavioral Nuances (CONDITIONAL)

> Skip if all endpoints follow uniform patterns. Scan for conditional logic that changes behavior.

1. **Internal vs External:** Identify endpoints reachable only from internal network (middleware checks, IP whitelists, separate port)
2. **Conditional Behavior:** Find endpoints whose response differs based on caller role, feature flag, or request header
3. **Search/Filter Semantics:** For list/search endpoints — determine: empty query behavior, case sensitivity, partial match support
4. **Non-Existent Resource:** For GET/PUT/DELETE by ID — determine: 404 vs 200-empty vs default-object
5. **Enum/Value Range:** For fields with constrained values — extract valid set, out-of-range behavior, default
6. Output: tables per `references/report-template.md` §13

#### 3.8 Config & Host Context (CONDITIONAL)

> Skip if no config/host patterns found. Use `references/lang-patterns.md` → Host System / Plugin Detection Patterns + Business Logic Detection.

1. **Whitelist Extraction:** Grep for hardcoded config values (allowed countries, currency codes, status lists). Record source file + values. Determine access path variants: direct domain call vs API gateway routing vs sidecar proxy — different paths may apply different whitelists.
2. **Host System Detection:** Check if the service is a plugin/filter for a host system (Envoy, Istio, Nginx, Kong). Record integration points and test implications.
   - Build request lifecycle layers diagram: what executes before the service handler (e.g., Envoy ext_proc → Istio mTLS → service middleware → handler).
   - Document host-layer errors: errors generated outside the service code (JWT validation at gateway, Istio 403 RBAC deny, rate-limit 429). These are NOT in the service's error mapping but ARE visible to callers.
3. **Dead Config Detection:** Cross-reference config keys defined in config files vs actually referenced in code. Flag unreferenced keys.
4. **Test Environment Setup:** From docker-compose, CI config, and Makefile — extract required services, env vars, and setup commands needed to run tests locally.
5. Output: tables per `references/report-template.md` §14

### Phase 4: Test Analysis

**Goal:** Assess current test coverage.

1. Find all test files:
   Read `references/lang-patterns.md` → section for detected language → use that language's **Test Patterns** for file glob and classification rules.

2. Classify by type:
   Classify per `lang-patterns.md` → Test Patterns table for detected language.

3. Determine test frameworks:
   Determine test frameworks: read `lang-patterns.md` → Test Frameworks table for detected language → search in primary build file.

4. Check for external test repositories:
   - Search README for links to `*-api-tests-*` or `*-tests`
   - Check for test runner configurations in CI/CD or platform directories

5. **E2E Test Dependency Graph (if E2E tests exist):**
   - Identify test setup/teardown chains (which entities are created before tests, in what order)
   - Cross-reference with Create-Order Chain from §12 — flag mismatches
   - Parse `.dev-platform/template.yaml` (or equivalent: `tilt.json`, `skaffold.yaml`, `docker-compose.override.yml`) for image placeholders and service overrides that affect test setup

6. **Test Environment Setup:**
   - Extract from docker-compose / CI config: required services, ports, env vars
   - Note: test DB seeding scripts, fixture files, mock server configs
   - Identify cross-repo prerequisites: changes in adjacent repositories required before tests can run (e.g., shared proto definitions, gateway config updates). Record as "Cross-Repo PR" blockers.

7. **Output Routing:** Explicitly route Phase 4 findings:
   - E2E dependency graph + cross-repo refs → report §7 (Existing Test Coverage)
   - Test env setup details → report §14 (Config & Host Context → Test Environment Setup sub-table)

### Phase 5: QA Scenario Matrix (CONDITIONAL)

**Goal:** Synthesize findings from Phases 3.5–3.8 into a prioritized QA scenario list for downstream skills.

> Skip entirely if Phases 3.5–3.8 yielded no results (simple CRUD service with no state machines, no entity deps, no nuances).

1. **Classify scenarios by priority:**
   - **P0 — Smoke:** Core happy paths, auth flow, create-order chain validation
   - **P1 — Regression:** State transitions (all valid From→To), CRUD per entity, pagination, error code coverage
   - **P2 — Edge:** Boundary values, type coercion edge cases, dead config, unreachable states
   - **Skip:** Scenarios requiring infrastructure not available in test env (e.g., Kafka consumer lag, WASM plugin host)

2. **Cross-Cutting Scenarios:** Scenarios that span multiple endpoints or domains (e.g., "create parent → create child → delete parent → verify cascade")

3. **Per-Domain Scenarios:** Group by API domain/entity, reference source section (§11–§14)

4. **Entity Lifecycle Scenarios:** Full CRUD lifecycle per entity respecting create-order chain and cleanup order

5. **Format Requirement:** Per-Domain and Cross-Cutting scenario tables MUST use the structured format consumable by `/api-tests`:
   `| # | RPC/Endpoint | Test Case | Key Input | Expected Result | Priority |`
   This is the canonical row format. Do NOT output free-text scenario descriptions — each row must be actionable as a test case.

6. Output: tables per `references/report-template.md` §15

### Phase 6: Infrastructure Scan

**Goal:** Understand the infrastructure context.

#### 6.1–6.5 Infrastructure Globs

| Component | Glob |
|-----------|------|
| CI/CD | `.github/workflows/*.yml`, `.gitlab-ci.yml`, `Jenkinsfile` |
| Docker | `**/Dockerfile`, `**/docker-compose.yaml` / `.yml` |
| Database | `migrations/**`, `**/migrations/**`, `**/liquibase/**` |
| Configuration | `config/*.yaml`, `config/*.yml` |
| Dev-Platform | `.dev-platform/**` |

#### 6.6 Security & Secrets Baseline

1. **Secrets:** `ls -la .env .env.*`, grep for RSA/OPENSSH keys, api_key/secret_key
2. **Dependencies:** Check for vulnerability scanner config files (`.govulncheck.yaml`, `.nsprc`, `.safety-policy.yml`, `dependency-check-suppression.xml`). Note presence/absence. Check build file for security plugins (OWASP dependency-check, snyk). Actual scanner execution is outside this skill's allowed-tools scope.
3. **SQL Injection:** grep for unsafe string interpolation near SQL keywords:
   - Go: `fmt.Sprintf` near SELECT/INSERT/UPDATE/DELETE
   - Python: f-string or `%` near `execute(` / `cursor.`
   - Node.js: template literal near `.query(`
   - Java/Kotlin: `String.format` or `+` near `createQuery` / `prepareStatement`

### Phase 7: AI Setup Status

Check for AI files:
```text
- CLAUDE.md
- .claude/qa_agent.md
- .claude/skills/**/*.md
- .agents/skills/**/*.md
- AGENTS.md
- .cursor/rules/*.mdc
- .github/copilot-instructions.md
```

### Phase 8: Report Generation

Compile the report and save to `audit/repo-scout-report_{timestamp}.md` (timestamp format: `YYYYMMDD_HHMMSS`). Full report template with examples — in `references/report-template.md`.

**Required sections (always present):**
1. Repository Profile (module, programming language version, service type, dependencies)
2. API Surface Catalog (REST + gRPC + GraphQL endpoints with Summary)
3. Validation Rules (endpoint × field × rule × error code)
4. Error Mapping (error constant × gRPC/HTTP code × trigger)
5. Auth & Access Control (auth matrix + flow diagram)
6. Specification Inventory (coverage formula + exact relative file paths to all discovered .json, .yaml, .yml, and .proto specification files)
7. Existing Test Coverage (unit/integration/e2e)
8. Infrastructure (CI/CD, Docker, DB, Migrations, Queue, Cache)
9. AI Setup Status (CLAUDE.md, qa_agent.md, skills)
10. Readiness Assessment (specs, tests, docs, AI setup + blockers + next step)

**Conditional sections (include only if data found in Phases 3.5–5):**
11. State Transition Matrix (from Phase 3.5)
12. Entity & Data Model (from Phase 3.6)
13. Behavioral Nuances (from Phase 3.7)
14. Config & Host Context (from Phase 3.8)
15. QA Scenario Matrix (from Phase 5)

## Quality Gates

- [ ] Build file for detected language found and parsed
- [ ] All proto files read and RPCs cataloged
- [ ] All swagger/openapi files read and endpoints cataloged
- [ ] Endpoint counts are correct (formula shown)
- [ ] Handler analysis performed for all discovered endpoints
- [ ] Error constants cataloged with code mappings
- [ ] Auth flow documented
- [ ] No placeholders `{xxx}` in the final report (except "none")
- [ ] Readiness Assessment filled for all 4 criteria
- [ ] §11: If state enums found → all transitions traced, unreachable states listed
- [ ] §12: If entities found → CRUD matrix complete, create-order chain documented, consistency model per write→read pair
- [ ] §13: If conditional behavior found → internal/external classified, search semantics documented
- [ ] §14: If config patterns found → dead config flagged, host system noted
- [ ] §15: If §11–§14 yielded data → QA scenarios classified by P0/P1/P2/Skip
- [ ] §12→§15: Create-order chain referenced in entity lifecycle scenarios
- [ ] E2E test dependency graph built (if E2E tests exist)
- [ ] Cross-repo test prerequisites extracted (if cross-repo dependencies detected)
- [ ] Test environment setup documented with required services and env vars
- [ ] No service-specific content leaked (no hardcoded service names from real repos)

## Self-Check

Before saving the report, verify:
- [ ] **Completeness:** All 10 required sections filled? Conditional §11–§15 present if data found?
- [ ] **Accuracy:** Endpoint counts match between sections 2 and 6?
- [ ] **No Hallucinations:** Each endpoint actually found in a file (source specified)?
- [ ] **Validation Rules:** Count matches proto validate tags + code-level validators found?
- [ ] **Error Mapping:** Covers all error constants found in code?
- [ ] **Readiness:** Assessment is backed by data from sections 2-8?
- [ ] **State Machines:** Every From→To pair has a source file reference?
- [ ] **Entity Chain:** Create-order chain matches FK constraints from migrations/ORM?
- [ ] **QA Scenarios:** Every P0 scenario maps to a discovered endpoint?
- [ ] **Consistency:** §12 consistency model aligns with §15 test implications?
- [ ] **Dead Config:** Each DEAD config key verified against handler code (not just config file grep) — confirm zero references in business logic

## Completion

After saving `audit/repo-scout-report_{timestamp}.md` — print `SKILL COMPLETE` block (format in qa_agent.md § Skill Completion Protocol).

Self-Review for this skill **is not generated** (read-only scanning, not content generation).

```text
✅ SKILL COMPLETE: /repo-scout
├─ Artifacts: audit/repo-scout-report_{timestamp}.md — **Each invocation creates a new timestamped file**
├─ Self-Review: N/A (scanning)
├─ Compilation: N/A
├─ Upstream: none
├─ Endpoints: {N REST} + {M gRPC} + {K GraphQL} = {total}
├─ Business Logic: {V validations} + {E errors} + {A auth rules}
├─ Entities: {N entities} + {M relationships} + create-order chain: {A → B → C}
├─ State Machines: {N state enums} + {M transitions} + {K unreachable states}
├─ Nuances: {N internal endpoints} + {M conditional behaviors}
└─ QA Scenarios: {P0: N} + {P1: M} + {P2: K} + {Skip: S} = {total}
```

## Related Files

- Language patterns: `references/lang-patterns.md` (§ State Machine, Entity Relationship, Async/Consistency, Batch/Collection, Type Handling, Host System)
- Report template: `references/report-template.md` (§1–§10 required, §11–§15 conditional)
- Downstream: `/spec-audit` (next pipeline step), `/api-test-cases` (reads §2+§15), `/api-tests` (reads §11–§15 for test generation)
- AI files: `/init-project` → CLAUDE.md, `/init-agent` → qa_agent.md
- Anti-patterns: `.claude/qa-antipatterns/api/eventual-consistency-writes.md` (§12 consistency), `.claude/qa-antipatterns/api/batch-partial-failure.md` (§12 batch)
