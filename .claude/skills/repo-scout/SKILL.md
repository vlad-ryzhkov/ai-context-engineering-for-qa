---
name: repo-scout
description: Scans a backend repository (Go, Python, Node.js, Java/Kotlin), catalogs API surface, infrastructure, test coverage, and produces a Test Generation Blueprint for downstream skills. Use when entering a new repo before writing tests. Do not use for QA projects — use /init-project for those.
allowed-tools: "Read Glob Grep Bash(ls*) Bash(wc*) Bash(jq*) Bash(yq*)"
agent: sdet
context: fork
---

# /repo-scout — Backend Repository Reconnaissance

<purpose>
Deep scanning of a backend repository → structured report on API surface, architecture,
infrastructure, current test coverage, entity dependencies, behavioral nuances, and Test Generation
Blueprint with cross-cutting flows, high-risk areas, and blocker constraints. Gives AI and humans a complete picture of the service before planning test coverage.
</purpose>

## Scope (SINGLE AUTHORITY)

**repo-scout is the ONLY skill responsible for initial repository discovery.**
- It reads, catalogs, and extracts business logic facts (validations, errors, auth flows).
- It detects entity dependencies, state machines, behavioral nuances, resilience patterns, and produces a Test Generation Blueprint for downstream skills.
- It NEVER evaluates code quality, suggests improvements, or generates test code.
- It NEVER generates test files or execution plans.

**Out of Scope (NEVER do these):**
- Architecture pattern detection or diagram generation (MVC, Hexagonal, etc.) — irrelevant for test planning
- Code quality evaluation or improvement suggestions (existing constraint — restated for clarity)
- Execution of external scripts or dependency installation (`pip install`, `npm install`, etc.)

## When to Use

- First entry into a new backend repository
- Before `/api-isolated-tests` — for data collection
- Before `/init-project` — to understand the target service
- Periodic audit: "what changed in the API surface?"

## When NOT to Use

- QA projects with tests (use `/init-project`)
- Code review (use standard tools)
- Frontend/mobile repositories

## Analysis Anti-Patterns (NEVER DO)

| Anti-Pattern | Why Skipped |
|-------------|-------------|
| Line count (cloc) | Useless metric for QA — number of routes and hierarchy depth matter |
| Cyclomatic complexity | Parsing linter JSON in bash is fragile; delegate to SonarQube/CI |
| Architecture pattern detection (MVC, DDD, Clean, Hexagonal) | Folder naming is irrelevant — routes and DB schema are what matter |

## Input Data

- Path to repository (or current directory)
- Does not require CLAUDE.md, qa_agent.md or other AI files
- Can be the **first step** in a new repo
- Scan mode (optional, default `full`):
  - `full` — complete 9-phase scan (current behavior)
  - `shallow` — skips Phase 3 (Business Logic) and Phase 5 (Test Generation Blueprint);
    outputs §1–§2, §6–§10 only. Use for large monorepos to save tokens.

## Algorithm

## Shallow Mode Guard

**If `mode: shallow` already specified in prompt** → skip Phases 3 and 5, print once:
```text
⚡ SHALLOW MODE: Phases 3 and 5 skipped
```
Report includes §1–§2, §6–§10 only.

**If `mode:` is NOT specified in prompt** → after printing TASK BRIEF, ask before Phase 1:
```text
❓ Is this a very large monorepo (hundreds of modules/services)?
   Reply `mode: shallow` to skip Business Logic + Blueprint (saves tokens and time).
   Otherwise the full 9-phase scan starts now.
```
Wait for user response. If user replies `mode: shallow` → activate shallow mode. Any other reply → proceed with `full`.

## Verbosity Protocol

- **Output:** All analysis → artifact (MD), not chat. Chat: max 5-line summary + `📊 Full report: {path}`
- **Checkpoints:** Phase transitions + warnings only. No per-file progress.
- **Tools first:** Grep/Read → table → report. No "Now I will..." preambles.
- **Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist).
- **Phases 1-9:** Silent. **Phase 10:** Summary table + report path (timestamp: `YYYYMMDD_HHMMSS`).

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
   - Set **language reference file**: Go → `references/lang-go.md`, Python → `references/lang-python.md`, Node.js/TS → `references/lang-nodejs.md`, Java/Kotlin → `references/lang-jvm.md`. All subsequent "lang-patterns.md → {section} for detected language" mean: read from this file. Cross-language patterns (Testing Libraries, Concurrency Model, Common Patterns, Host System, gRPC Streaming) remain in `references/lang-patterns.md`.

2. Read primary build file for metadata:
   Per `references/lang-patterns.md` → Build Files for detected language, extract: module/package name, runtime version, key dependencies.

3. Determine structure:
   Use `references/lang-patterns.md` for detected language to identify entry-point directories and module layout.
   **Exclude from all scans:** `node_modules/`, `vendor/`, `__pycache__/`, `.venv/`, `venv/`,
   `dist/`, `build/`, `target/`, `.gradle/`, `.mvn/`, `bin/`, `obj/`, `.git/`,
   `fixtures/`, `seeds/`, `mock_data/`, `testdata/`

   **Data files:** Skip `*.sql` files inside `seeds/`, `fixtures/`, or `mock_data/` directories —
   they contain raw INSERT data, not schema. For DB schema analysis use ORM model files or
   DDL migration files only (e.g., `migrations/**/*.sql` is OK; `seeds/*.sql` is not).

4. Count size:
   Use `references/lang-patterns.md` → Test Patterns for detected language to distinguish source vs test files.
   Record: number of source files, number of test files.

5. **VCS Hotspot Analysis** (run only if `.git` directory exists):
   ```bash
   git log --pretty=format: --name-only --since="1 year ago" \
     | grep -v vendor/ | grep -v node_modules/ \
     | sort | uniq -c | sort -rg | head -10
   ```
   - Record: file path + change frequency.
   - Map hotspot files → handlers → endpoints from Phase 2 (match by file path).
   - Tag mapped endpoints as `[HOTSPOT: N changes]` in §2 API Surface Catalog Risk column.
   - If `.git` absent → skip silently.

### Phase 2: API Surface Discovery

**Goal:** Find and catalog ALL API endpoints.

#### 2.1 OpenAPI / Swagger

Search for files:
```text
Glob: **/swagger.json, **/swagger.yaml, **/swagger.yml, **/openapi.json, **/openapi.yaml, **/openapi.yml, **/*.swagger.json
```

For each found file:
- If file exceeds 500 lines → summarize by tags/domains instead of listing every endpoint. Record exact relative file path for downstream skills to read directly.
  Use `cat {file} | jq '[.paths | keys[]]'` (JSON) or `yq '[.paths | keys[]]' {file}` (YAML) to extract the route list without reading the full document. Only read sections relevant to specific endpoints.
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

#### 2.6 WebSockets & SSE

Grep: `WebSocket|ws\.NewConn|websocket\.Upgrade|websocket\.Accept|gorilla/websocket`
      `EventSource|text/event-stream|SseEmitter|http\.Flusher|socket\.io`

If found:
- Record each endpoint: path, protocol (WS/WSS/SSE), handler function
- Extract event types emitted (structs passed to WriteMessage / json.NewEncoder(w).Encode)
- Note auth mechanism (WS handshake header vs query param)
- Mark as [WS] in §2 API Surface Catalog — exclude from REST endpoint count

Reference: `lang-patterns.md → §L6 WebSocket & SSE Patterns` for per-language grep strings.

#### 2.7 Risk Classification

Flag **Sensitive Endpoints** (auth, login, token, billing, pay, wallet, users, profile, export) in the report — SDET will apply `@Severity(CRITICAL)` patterns.

#### 2.8 Business Domain Grouping

After cataloging all endpoints, group them into 3–8 high-level business domains
(e.g., "User Management", "Billing", "Order Processing", "Catalog", "Auth").
- If < 5 endpoints total → skip this step (single domain by definition)
- Domains become the organizing structure for §15 Test Generation Blueprint
- Record as Business Domain Map table in §2 of the report

### Phase 3: Business Logic Analysis

**Goal:** Extract business logic facts from handler code for downstream test generation.

**Token-saving strategy:** Skip DTO mapping / field copying blocks. Focus on conditional branches, error returns, validation calls, auth checks.

**Reference:** `references/lang-patterns.md` → Handler Patterns, Error Patterns, Validation Patterns, Auth/Middleware Patterns for detected language.

**Read `references/phase3-analysis.md`** — execute sub-steps §3.0–§3.11 in order. All output targets, conditional guards, and grep patterns are defined there.

### Phase 4: Test Analysis (S-UNIT: Simplified + S-ENV: Enhanced Environment)

**Goal:** Superficial test assessment + thorough environment extraction.

#### 4.1 Test File Census (S-UNIT — lightweight)

1. Count test files by pattern using `lang-patterns.md` → Test Patterns for detected language
2. Note test framework from build file using `lang-patterns.md` → Test Frameworks
3. Detect test libraries: grep build file using `lang-patterns.md` → Testing Libraries Detection. Record in §7 Libraries column.
4. Note if E2E/API tests exist (in-repo or external — check README for `*-api-tests-*` links)
5. Output: file counts + framework + libraries in §7. Skip detailed classification/coverage analysis.

#### 4.2 Test Environment Extraction (S-ENV — thorough)

1. **Token config:** Search for token/auth setup in config files, env vars, CI scripts. Record:
   - Token format (JWT, API key, gRPC metadata)
   - Where to find test tokens (config file paths, env var names, secrets manager keys)
   - Token generation commands if available
2. **Local setup commands:** Extract from Makefile, docker-compose, README:
   - `make` targets for build/test/seed
   - `docker-compose up` variants and required profiles
   - Any pre-test setup scripts
3. **Data seed requirements:** Determine what must exist before tests run:
   - Required entities and their creation order (cross-ref with §12 create-order chain)
   - Seed scripts or fixture files
   - DB migration prerequisite
4. **gRPC-specific (if applicable):**
   - Reflection: determine the **default value** — check local/test config files for the reflection flag. Report "enabled" or "disabled locally" (not just "configurable"). If disabled, testers cannot use grpcurl/grpcui for discovery and must use `-import-path` with proto files.
   - Proto import paths needed for client generation
   - Required proto plugins and their versions
5. **Remote env setup:** Parse `.dev-platform/`, CI config for:
   - Namespace configuration
   - Service dependencies and ports
   - Cross-repo prerequisites (shared proto, gateway config). Record as "Cross-Repo PR" blockers.
6. **External Dependencies & Mock Servers:**
   - Grep HTTP client init: `http.NewRequest`, `resty.New()`, `axios.create`, `WebClient.builder()`
   - Grep gRPC client stubs: `grpc.Dial`, `NewXxxClient(`
   - Grep mock libraries in build file: `WireMock`, `mockserver`, `nock`, `httpmock`, `gock`
   - For each external call: record target service, base URL, protocol, mock status
   - Flag calls with no mock setup as [NO_MOCK]
   - Record in §14 Test Environment Setup → External Dependencies sub-table
7. **Output Routing:**
   - Test census → report §7
   - All environment data → report §14 Test Environment Setup

### Phase 5: Test Generation Blueprint (CONDITIONAL)

**Goal:** Synthesize findings from Phases 3.5–3.10 into directives for downstream
skills (/api-test-cases, /api-isolated-tests, /api-tests). Produces guidance, NOT test cases.

> Skip if Phases 3.5–3.10 yielded no significant findings.

1. **Cross-Cutting Business Flows** — for each multi-endpoint scenario:
   `"[FLOW] {label}: step1 → step2 → step3 — verify {assertion}"`
   Source: §11 Multi-Step Sequences + §12 Create-Order Chain

2. **High-Risk Areas** — for each risk finding, one directive:
   `"[RISK] {area}: {what to test} — source: §{N}"`
   Derive from: [UNDOCUMENTED], [WEAK_TYPE], [NO_SPECIFIC_CODE], [AUTH_ANOMALY],
   [NO_MOCK], resilience gaps from §18, [DEEP_HIERARCHY]

3. **Blocker Constraints** — one bullet per hard constraint:
   `"[BLOCKER] {description}"`
   Derive from: [NO_CREATE]/[NO_DELETE] (§12), eventual consistency (§12),
   [NO_TIMEOUT] (§18), disabled gRPC reflection (§14), cross-repo blockers (§14)

4. **Apply AI Rules:** Before finalizing priorities, check for testing conventions in the target repo's AI files:
   - Glob: `CLAUDE.md`, `.claude/qa_agent.md`, `AGENTS.md`, `.cursor/rules/*.mdc`, `.github/copilot-instructions.md`
   - Read each found file and extract testing constraints (e.g., "no destructive tests in dev", "always use UUIDv4", "skip latency tests in CI").
   - Adjust P1/P2 scenario priorities and add/remove rows accordingly.
   - In each affected row append `[RULE: {source file} — {constraint excerpt}]` to the Expected Result column so downstream skills inherit the constraint.
   - If no AI files found or no testing constraints extracted → skip silently.

5. Output: §15 Test Generation Blueprint

6. **Risk Consolidation (→ §17):** Collect ALL risk tags from Phases 3–6 into §17:
   - [AUTH_ANOMALY] from §3.1 and §5
   - [UNDOCUMENTED] validations from §3.1
   - [DEAD_CONFIG] from §3.8
   - [DEEP_HIERARCHY] from §3.6
   - Mixed ID types from §3.6
   - SQL injection grep hits from §6.6
   Rate each: CRITICAL / HIGH / MEDIUM / LOW.
   Include recommended action (verify / add test / fix / document).

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

#### 6.6 Deployment Topology (S6)

Glob for Helm/K8s configs using `lang-patterns.md` → Deployment Topology Patterns:
- `**/Chart.yaml`, `**/values*.yaml`, `**/templates/*.yaml` (Helm)
- `**/kustomization.yaml`, `**/kustomization.yml` (Kustomize)
- `**/terraform/*.tf`, `**/main.tf` (Terraform)
- `**/skaffold.yaml`, `**/Tiltfile` (Dev tools)

Record: environment variants (dev/staging/prod), key differences (replicas, resource limits, feature flags per env). Output in §8 Infrastructure → Deployment Topology sub-table.

#### 6.6 Security & Secrets Baseline

1. **Secrets:** `ls -la .env .env.*`, grep for RSA/OPENSSH keys, api_key/secret_key
2. **Dependencies:** Check for vulnerability scanner config files (`.govulncheck.yaml`, `.nsprc`, `.safety-policy.yml`, `dependency-check-suppression.xml`). Note presence/absence. Check build file for security plugins (OWASP dependency-check, snyk). Actual scanner execution is outside this skill's allowed-tools scope.
3. **SQL Injection:** grep for unsafe string interpolation near SQL keywords:
   - Go: `fmt.Sprintf` near SELECT/INSERT/UPDATE/DELETE
   - Python: f-string or `%` near `execute(` / `cursor.`
   - Node.js: template literal near `.query(`
   - Java/Kotlin: `String.format` or `+` near `createQuery` / `prepareStatement`
4. **Dependency Staleness:** Parse lock files for staleness signals without running scanners.
   - Glob: `go.sum`, `package-lock.json`, `yarn.lock`, `poetry.lock`, `Pipfile.lock`, `pom.xml`
   - Check last modification date via `ls -la` on lock file.
   - If lock file not modified in > 12 months AND project is > 1 year old → flag `[DEPENDENCY_RISK]`.
   - Grep `go.mod` for critical deps with known major version jumps:
     - `jwt-go` (use `golang-jwt/jwt` v5+), `gorilla/mux` EOL, `go-yaml v2` (use v3)
   - Grep `package.json` / `package-lock.json` for: `express@^4` (v5 out), `jsonwebtoken@^8`, `axios@^0`
   - Record in §8 Infrastructure → Dependency Staleness sub-table.

### Phase 7: AI Setup Status

Glob for AI files: `CLAUDE.md`, `.claude/qa_agent.md`, `.claude/skills/**/*.md`, `.agents/skills/**/*.md`, `AGENTS.md`, `.cursor/rules/*.mdc`, `.github/copilot-instructions.md`.

### Phase 8: Non-English Documentation (S8, CONDITIONAL)

> Skip if all docs are in English.

If docs are non-English:
1. Record doc language in §1 Repository Profile (`Documentation Language` field)
2. Extract key business terms (entity names, status values, error descriptions)
3. Provide English translations with originals in parentheses: `zone (зона)`, `agglomeration (агломерация)`
4. Record translated term glossary in §6 Specification Inventory

### Phase 9: Report Generation

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
15. Test Generation Blueprint (from Phase 5)
16. Event Catalog (from Phase 3.9)
17. QA Risk Assessment & Testability Issues (from Phase 5 step 6)
18. Resilience Mechanisms (from Phase 3.10)

**QA Onboarding Guide (conditional):** If Phase 4.2 extracted token config + setup commands →
also save `audit/qa-environment.md` using `references/qa-environment-template.md`.
Print path in SKILL COMPLETE block.

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
- [ ] §3: Code-level validations without proto/swagger counterpart flagged as `[UNDOCUMENTED]`
- [ ] §6: Existing QA/test documentation discovered and referenced (if present)
- [ ] §12: If hierarchy depth > 2 → full chain documented with depth + visual tree
- [ ] §12: If master/replica patterns found → Read/Write Topology populated
- [ ] §12: ID types recorded per entity, mixed types in same hierarchy flagged
- [ ] §12: Asymmetric CRUD marked with `[NO_{OP}]` tags
- [ ] §14: If config patterns found → dead config flagged, host system noted
- [ ] §7: Test libraries detected from build file and recorded in Libraries column
- [ ] §13: If concurrency patterns found → model identified, risks documented
- [ ] §14: Test environment setup includes token config and data seed requirements
- [ ] §15: If §11–§14 yielded data → Blueprint has [FLOW]/[RISK]/[BLOCKER] bullets with source section citations
- [ ] §15: If AI rule files found in target repo → affected scenario rows include `[RULE: ...]` annotation
- [ ] §12→§15: Create-order chain referenced as [FLOW] or [BLOCKER] in §15
- [ ] §16: If queue client detected → event publishing calls scanned
- [ ] Cross-repo test prerequisites extracted (if cross-repo dependencies detected)
- [ ] No service-specific content leaked (no hardcoded service names from real repos)
- [ ] §2: WebSocket/SSE endpoints marked [WS], excluded from REST count
- [ ] §11: Each rejection records exact HTTP/gRPC code; missing codes flagged [NO_SPECIFIC_CODE]
- [ ] §14: External calls without mock flagged [NO_MOCK] in External Dependencies sub-table
- [ ] §17: All [AUTH_ANOMALY], [UNDOCUMENTED], [DEAD_CONFIG] items consolidated (count matches §§3–14)
- [ ] §18: If resilience patterns found → Resilience Mechanisms section present
- [ ] §2: Business Domain Map present if total endpoints > 5
- [ ] shallow mode: Phases 3 and 5 skipped when mode=shallow specified
- [ ] qa-environment.md generated if Phase 4.2 has token config + setup commands
- [ ] §1: VCS hotspot analysis run (if .git present); top-10 hotspot files mapped to endpoints
- [ ] §15: If FIXME/HACK/BUG markers found in handlers → Debt Markers table present; P0 items in High-Risk Areas
- [ ] §8: Dependency staleness checked; lock files older than 12 months flagged [DEPENDENCY_RISK]

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
- [ ] **Test Blueprint:** Every [FLOW] references §11/§12? Every [RISK] and [BLOCKER] cites source section?
- [ ] **AI Rules:** Scenarios adjusted for constraints found in CLAUDE.md / qa_agent.md?
- [ ] **Consistency:** §12 eventual consistency entries appear as [BLOCKER] bullets in §15?
- [ ] **Dead Config:** Each DEAD config key verified against handler code (not just config file grep) — confirm zero references in business logic
- [ ] **Domain Map:** endpoint count per domain sums to §2 API totals?
- [ ] **Risk §17:** every [UNDOCUMENTED] and [AUTH_ANOMALY] from body of report appears in §17?

## Completion

After saving `audit/repo-scout-report_{timestamp}.md` — print `SKILL COMPLETE` block (format in qa_agent.md § Skill Completion Protocol).

Self-Review for this skill **is not generated** (read-only scanning, not content generation).

```text
✅ SKILL COMPLETE: /repo-scout
├─ Artifacts: audit/repo-scout-report_{timestamp}.md — **Each invocation creates a new timestamped file**
├─ Onboarding: audit/qa-environment.md — {generated / skipped (insufficient env data)}
├─ Self-Review: N/A (scanning)
├─ Compilation: N/A
├─ Upstream: none
├─ Endpoints: {N REST} + {M gRPC} + {K GraphQL} = {total}
├─ Business Logic: {V validations} + {E errors} + {A auth rules}
├─ Entities: {N entities} + {M relationships} + create-order chain: {A → B → C}
├─ State Machines: {N state enums} + {M transitions} + {K unreachable states}
├─ Nuances: {N internal endpoints} + {M conditional behaviors} + {W WebSocket/SSE endpoints}
├─ Events: {N handlers publish events} + {M topics}
├─ Resilience: {N idempotency keys} + {M retry policies} + {K circuit breakers}
├─ Debt: {N TODO} + {M FIXME} + {K HACK} markers → {P} endpoint-linked, {Q} P0-escalated
├─ Hotspots: {N files} mapped to {M endpoints} — top: {filename} ({N changes}/year)
└─ Blueprint: {F cross-cutting flows} + {R high-risk areas} + {B blocker constraints}
```

## Quality Gate (Self-Review)

Before finalizing the repo-scout report:

- [ ] All 9 phases completed (or Phases 1-2, 6-10 if shallow mode)
- [ ] API surface documented with endpoint count
- [ ] Database schema extracted (tables, relationships)
- [ ] State machines and entity flows identified
- [ ] Behavioral nuances documented (error handling, validation rules)
- [ ] Test Generation Blueprint section complete
- [ ] Report saved with timestamp

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

---

## Related Files

- Language patterns (index): `references/lang-patterns.md` (Language Detection, Testing Libraries, Concurrency, Common Patterns, Host System, gRPC Streaming)
- Language patterns (per-lang): `references/lang-go.md`, `references/lang-python.md`, `references/lang-nodejs.md`, `references/lang-jvm.md`
- Phase 3 sub-steps: `references/phase3-analysis.md` (§3.0–§3.11 full algorithm)
- Report template: `references/report-template.md` (§1–§10 required, §11–§18 conditional)
- Onboarding template: `references/qa-environment-template.md` (generates `audit/qa-environment.md`)
- Downstream: `/spec-audit` (next pipeline step), `/api-test-cases` (reads §2+§15), `/api-tests` (reads §11–§15+§17 for test generation)
- AI files: `/init-project` → CLAUDE.md, `/init-agent` → qa_agent.md
- Anti-patterns: `.claude/qa-antipatterns/api/eventual-consistency-writes.md` (§12 consistency), `.claude/qa-antipatterns/api/batch-partial-failure.md` (§12 batch)
