# Phase 3: Business Logic Analysis — Sub-Steps

> Referenced by `SKILL.md` Phase 3. Execute all sub-steps in order.
> Strategy: Skip DTO mapping / field copying blocks. Focus on conditional branches, error returns, validation calls, auth checks.
> Reference: `references/lang-patterns.md` → Handler Patterns, Error Patterns, Validation Patterns, Auth/Middleware Patterns for detected language.

## Table of Contents

- [3.0 Documentation & Specification Discovery](#30-documentation--specification-discovery-s-doc)
- [3.1 Handler Analysis](#31-handler-analysis-s2-undocumented-validation-extraction)
- [3.2 Validation Rules + Cross-Layer Type Consistency](#32-validation-rules--cross-layer-type-consistency-s1)
- [3.3 Error Mapping](#33-error-mapping)
- [3.4 Auth & Access Control](#34-auth--access-control)
- [3.5 State Transition Matrix](#35-state-transition-matrix-conditional)
- [3.6 Entity & Data Model Analysis](#36-entity--data-model-analysis-conditional)
- [3.7 Behavioral Nuances](#37-behavioral-nuances-conditional)
- [3.8 Config & Host Context](#38-config--host-context-conditional)
- [3.9 Domain Event Detection](#39-domain-event-detection-conditional)
- [3.10 Resilience & Idempotency](#310-resilience--idempotency-conditional)
- [3.11 Codebase Debt Extraction](#311-codebase-debt-extraction-conditional)

---

## 3.0 Documentation & Specification Discovery (S-DOC)

Before analyzing handlers, discover ALL existing API documentation and specifications:

1. **API specifications** — glob for machine-readable specs:
   - `**/swagger.json`, `**/swagger.yaml`, `**/swagger.yml`, `**/openapi.json`, `**/openapi.yaml`, `**/openapi.yml`, `**/*.swagger.json`
   - `**/*.proto`
   - `**/*.graphql`, `**/schema.graphqls`
   - `**/*.http`, `**/api.http`
   - `**/postman_collection.json`, `**/*.postman_collection.json`
2. **Human-readable docs** — glob for documentation (execute ALL globs, list every match):
   - `docs/**/*.md` ← **CRITICAL: always run this glob, many repos keep QA/API docs here**
   - `**/README.md`, `**/API.md`, `**/GUIDE.md`
   - `**/qa-checklist*`, `**/qa-environment*`, `**/api-docs*`
   - `**/testing*.md`, `**/TESTING.md`, `**/CONTRIBUTING.md`
   - `**/architecture*.md`, `**/design*.md`, `**/ADR*.md`
3. **List ALL found files explicitly** — do not summarize, do not skip. For each found doc, assess:
   - Content type (API spec, test checklist, environment setup, architecture, ADR)
   - Coverage: does it cover endpoints, error codes, auth, test setup?
   - Quality: is it current (cross-reference against code)?
4. If docs are comprehensive (cover >70% of API surface):
   - Reference the doc path in the report — do NOT duplicate its content
   - Focus analysis only on **gaps** (what the doc misses or gets wrong)
   - Flag stale information (doc says X, code says Y)
5. Record ALL found docs in §6 Discovered Documentation table (both machine-readable specs and human docs)
6. Record doc language (English, Russian, etc.) — if non-English, extract key business terms with English translations

---

## 3.1 Handler Analysis (S2: Undocumented Validation Extraction)

For each endpoint/RPC discovered in Phase 2:
1. Locate the handler function body using lang-patterns.md → Handler Patterns.
   **Token-safety limit:** If the handler's source file exceeds 300 lines, do NOT read the
   entire file. Use targeted grep to extract the handler body only:
   - Go:         `grep -n -A 60 "func {HandlerName}" {file}`
   - Python:     `grep -n -A 60 "def {handler_name}" {file}`
   - Node.js/TS: `grep -n -A 60 "{handlerName}" {file}`
   - Java/Kotlin:`grep -n -A 80 "fun {handlerName}\|void {handlerName}" {file}`
   Stop at the next function boundary. If context still exceeds 100 lines, grep only for
   error/validation keywords: `grep -n "if\|throw\|return\|error\|err\|raise" {file}`.
2. Read handler code — extract:
   - Input validations (parameter checks, schema validation calls)
   - Error branches (`if err`, `throw`, `raise`, status code returns)
   - Auth checks (token extraction, permission verification)
   - Key business rules (conditional logic that changes behavior)
3. **Extract validations from 3 sources separately:**
   - **(a) Schema/proto-level:** validate tags, required fields, type constraints
   - **(b) Framework/middleware:** interceptors, validators, pipes that apply before handler
   - **(c) Code-level inline checks:** `if param == nil`, `if len(x) > N`, manual range checks in handler body
4. **Flag code-level checks without schema counterpart as `[UNDOCUMENTED]`** — these are invisible error branches that API tests reading only proto/swagger will miss
5. **Explicitly check for these common hidden validations:**
   - Nil/empty parameter guards (`if params == nil`, `if len(list) == 0`) on ALL RPCs — not just CRUD
   - Empty collection/slice checks (e.g., empty `sections[]` array on GetConfig-style RPCs)
   - Business-rule length checks that differ from proto (e.g., code uses `MinSearchLen=2` while proto has no constraint)
   - Unit semantics: if validators use `min=2` on strings, note whether the framework measures bytes or characters (Go's go-playground/validator measures **bytes** for `min`/`max` on strings — 1 Cyrillic char = 2 bytes)
   - Auth anomalies: endpoints with weaker auth than siblings → flag as `[AUTH_ANOMALY]`
6. Detect weakly-typed payload handling and parsing patterns:
   - Weakly-typed fields: grep `interface{}/map[string]any` (Go), `dict/Any` (Python),
     `unknown/Record<string,any>` (TS), `JsonNode/Map<String,Object>` (JVM).
     Flag as [WEAK_TYPE].
   - Date/time parsing: grep `time.Parse`, `strptime`, `LocalDate.parse`, `DateTimeFormatter`.
     Extract exact format string. Record as [DATE_FORMAT: {fmt}].
   - Regex validation: grep `regexp.MustCompile`, `re.compile`, `Pattern.compile`.
     Extract pattern. Record as [REGEX: {pattern}].
   - Record in §3 Validation Rules with Source=CODE and appropriate tag.
7. Record per-endpoint: `Endpoint | Validations | Error Branches | Auth Check | Business Rules`

---

## 3.2 Validation Rules + Cross-Layer Type Consistency (S1)

1. **Proto-level** (if gRPC): extract `validate` tags from Phase 2.2 results
2. **Code-level:** search for framework-specific validators using lang-patterns.md → Validation Patterns
3. **Cross-layer type check:** For fields appearing in multiple layers (proto, swagger, code, docs), compare declared types. Record mismatches in report §12 Cross-Layer Type Consistency table. Specifically check:
   - **Request vs Response ID types:** If request uses `int32` for entity ID but Create response returns `int64`, flag as MISMATCH — API tests must construct correct type per direction
   - **Proto vs DB vs Code:** e.g., proto `int64` but DB `int(32-bit)` or code `int32`
   - **Pagination cursor types:** different entities may use different cursor types (int64 vs string UUID)
4. Output: table per `references/report-template.md` § Validation Rules (include `Source` and `Documented` columns)

---

## 3.3 Error Mapping

1. Find all error constants/enums using lang-patterns.md → Error Patterns
2. Map each to HTTP status code and/or gRPC code
3. **Standard code cross-reference (gRPC services only):** After mapping all found codes,
   check against the full gRPC standard set (0–16). Note in §4 report header:
   - Codes NOT returned by any RPC → testers must not write assertions for these
   - Non-standard choices → e.g., "uses PermissionDenied(7) for auth failures
     instead of Unauthenticated(16)"
4. Output: table per `references/report-template.md` § Error Mapping

---

## 3.4 Auth & Access Control

1. Find middleware/interceptors using lang-patterns.md → Auth/Middleware Patterns
2. Extract auth flow: token extraction → validation → failure handling
3. **Extract for QA test setup (mandatory):**
   - Auth header name used by this service (e.g., `Authorization`, `X-Api-Key`,
     `grpc-metadata-authorization`)
   - Token format/prefix (e.g., `Bearer {jwt}`, `servicename_tokenvalue`, raw API key)
   - Record in §5 Auth Mechanisms "Details" column in format:
     `Header: {name} | Format: {prefix} {value_format}`
4. Classify endpoints: `PUBLIC` | `AUTH` | `ADMIN`
5. Output: endpoint auth matrix + auth flow diagram per report template

---

## 3.5 State Transition Matrix (CONDITIONAL)

> Skip if no state machine patterns found. Use `references/lang-patterns.md` → State Machine Patterns.

1. Search for state/status enums using lang-patterns grep strings
2. For each enum found:
   - List all named states (e.g., `Pending`, `Active`, `Deleted`)
   - Trace transitions: grep for `.Status =` / `.State =` assignments in handlers/services
   - Record: `From → To | Trigger (handler/method) | Guard (if-condition) | Error on rejection`
   - For "Error on rejection": record exact code (HTTP 409, gRPC AlreadyExists(6)).
     If handler returns generic error → flag [NO_SPECIFIC_CODE].
     Grep near transition guards: `status.Errorf(codes.`, `http.StatusConflict`,
     `http.StatusUnprocessableEntity`, `ErrInvalidTransition`.
3. Identify unreachable states: states defined in enum but never assigned in any transition
4. Identify multi-step test sequences — chains of transitions that represent real business flows (e.g., `created → active → suspended → reactivated`, `pending → approved → completed → archived`). Document the full chain with business flow description. Without this, tests will verify single-step transitions but miss the end-to-end lifecycle.
5. Output: table per `references/report-template.md` §11 (include Multi-Step Transition Sequences sub-table)

---

## 3.6 Entity & Data Model Analysis (CONDITIONAL)

> Skip if no entity relationship patterns found. Use `references/lang-patterns.md` → Entity Relationship Patterns.

1. **CRUD Matrix:** For each entity, determine which operations exist (Create/Read/Update/Delete/Soft Delete). Mark missing operations as `[NO_{OP}]` — prevents generating dead API test cases for non-existent endpoints.
2. **Hierarchy Depth (S3):** Record full entity chain with depth. Flag >3 levels as `[DEEP_HIERARCHY]` — API tests need multi-step setup in @BeforeEach. Output visual tree: `WORLD(1) → COUNTRY(2) → CITY(3) → ZONE(4)`.
3. **Asymmetric CRUD (S3):** Identify entities where CRUD operations differ from siblings (e.g., zones have Delete but no Update). Mark asymmetries in CRUD matrix.
4. **Identifier Types (S3):** Record ID type per entity (int32, int64, UUID, string). Flag mixed types in same hierarchy — API test data generators must use correct type per entity.
5. **Entity Relationships:** Extract FK references from migrations, ORM tags, or struct fields
6. **Create-Order Chain:** Determine entity creation order based on FK dependencies (parent before child). Record reverse order for cleanup.
7. **Pagination:** Find endpoints with `cursor`/`offset`/`limit` parameters. Record strategy, defaults, max values.
8. **Data Consistency + Read/Write Topology (S5):** For each write→read pair, determine consistency model (strong/eventual). Check for `tx.Commit`, async events, cache layers. Detect master/replica patterns using lang-patterns.md → Read/Write Topology Patterns. Classify operations: `MASTER_ONLY` / `REPLICA_SAFE` / `UNKNOWN`. Record test implication (e.g., write→read needs polling/retry for eventual consistency on replicas).
9. **Batch Operations:** Find `BatchCreate`/`BulkInsert` patterns. Note error propagation strategy (atomic vs partial).
10. **Type Handling:** Find type conversion patterns (`strconv.Atoi`, `json.Number`, custom `UnmarshalJSON`). Note edge cases.
11. **DB Constraint Extraction (S4-LITE, conditional):** If ORM models/struct definitions are found (not migration files), extract:
    - `VARCHAR(N)` / string length limits from ORM tags (gorm, SQLAlchemy, TypeORM, JPA `@Column(length=)`) — enables NEG test: send N+1 chars
    - `UNIQUE` constraints (especially composite) — enables duplicate-creation NEG scenarios
    - `NOT NULL` on non-obvious fields — useful if API tests seed data via direct DB inserts
    - Skip full DDL parsing. Source: ORM tags only, not migration files.
12. Output: tables per `references/report-template.md` §12

---

## 3.7 Behavioral Nuances (CONDITIONAL)

> Skip if all endpoints follow uniform patterns. Scan for conditional logic that changes behavior.

1. **Internal vs External:** Identify endpoints reachable only from internal network (middleware checks, IP whitelists, separate port)
2. **Conditional Behavior:** Find endpoints whose response differs based on caller role, feature flag, or request header
3. **Search/Filter Semantics:** For list/search endpoints — determine: empty query behavior, case sensitivity, partial match support
4. **Non-Existent Resource:** For GET/PUT/DELETE by ID — determine: 404 vs 200-empty vs default-object
5. **Enum/Value Range:** For fields with constrained values — extract valid set, out-of-range behavior, default
6. **Concurrency Model:** Grep using `lang-patterns.md` → Concurrency Model Detection for detected language. Record model, pattern count, QA risks. Output in §13 Concurrency Model subtable.
7. Output: tables per `references/report-template.md` §13

---

## 3.8 Config & Host Context (CONDITIONAL)

> Skip if no config/host patterns found. Use `references/lang-patterns.md` → Host System / Plugin Detection Patterns + Business Logic Detection.

1. **Whitelist Extraction:** Grep for hardcoded config values (allowed countries, currency codes, status lists). Record source file + values. Determine access path variants: direct domain call vs API gateway routing vs sidecar proxy — different paths may apply different whitelists.
2. **Request Lifecycle Tracing (always):** Trace the full request lifecycle for ANY service:
   - Entry point (port/listener) → middleware stack (auth interceptor, rate limiter, request validator) → handler → service layer → downstream (DB, queue, external HTTP)
   - Even for a plain REST service: `HTTP:8080 → JWT middleware → handler → PostgreSQL`
   - Document layer-generated errors at each stage (not just handler errors)
   - Merge into §14 "Request Lifecycle Layers" table (CONDITIONAL: include if middleware chain or host system detected)

   **Host System Detection:** Additionally check if the service is a plugin/filter for a host system (Envoy, Istio, Nginx, Kong). Record integration points and test implications.
   - Document host-layer errors: errors generated outside the service code (JWT validation at gateway, Istio 403 RBAC deny, rate-limit 429). These are NOT in the service's error mapping but ARE visible to callers.
3. **Dead Config Detection:** Cross-reference config keys defined in config files vs actually referenced in code. Flag unreferenced keys.
4. **Test Environment Setup:** From docker-compose, CI config, and Makefile — extract required services, env vars, and setup commands needed to run tests locally.
5. Output: tables per `references/report-template.md` §14

---

## 3.9 Domain Event Detection (CONDITIONAL)

> Skip if no queue client detected in §8 Infrastructure (no Kafka, NATS, RabbitMQ, Redis Pub/Sub).

1. Grep using lang-patterns.md → Event Publishing Patterns for detected language
2. For each publish call found — record:
   - Handler/RPC that contains the call
   - Topic / queue / channel name (string constant)
   - Trigger condition (always? on success only? specific state transition?)
   - Payload hint (struct name or key fields passed to the call)
3. Cross-reference with §11 State Transitions — link event to triggering transition if applicable
4. Output: §16 Event Catalog table

---

## 3.10 Resilience & Idempotency (CONDITIONAL)

> Skip if no retry/circuit-breaker/idempotency patterns found.

1. Idempotency keys: grep `X-Idempotency-Key`, `idempotent`, `deduplication_id`.
   Record: endpoint, header/field name, duplicate behavior.
2. Retry logic: grep `retry`, `backoff`, `RetryPolicy`, `maxAttempts`, `retryOn`.
   Flag retries on non-idempotent writes as [RETRY_RISK].
3. Circuit breakers: grep `CircuitBreaker`, `hystrix`, `resilience4j`, `gobreaker`.
   Record: protected dependency, open threshold, fallback behavior.
4. Timeout config: grep `Timeout`, `context.WithTimeout`, `grpc.WithTimeout`.
   Flag endpoints with no timeout as [NO_TIMEOUT].
5. Output: §18 Resilience Mechanisms.

---

## 3.11 Codebase Debt Extraction (CONDITIONAL)

> Skip if source directory cannot be determined. Run only on source files — exclude vendor/, node_modules/, test files, .git.

1. Grep for dev markers in source files:
   ```bash
   grep -rn -E "(TODO|FIXME|HACK|BUG|XXX|NOSONAR)" . \
     --include="*.go" --include="*.kt" --include="*.java" \
     --include="*.py" --include="*.ts" --include="*.js" \
     --exclude-dir=vendor --exclude-dir=node_modules \
     --exclude-dir=.git --exclude="*_test.*" --exclude="*.spec.*"
   ```
2. For each marker found:
   - Record: file path + line + marker type + comment text.
   - Cross-reference file path against Phase 2 endpoint→handler mapping.
   - If matched to an endpoint → assign tag `[DEBT: {TYPE}]` (e.g., `[DEBT: FIXME]`).
   - If comment mentions transaction, rollback, race, lock, auth, payment, quota → escalate to `[DEBT: P0]`.
3. Aggregate by endpoint in §15 Debt Markers table.
4. P0 debt items automatically get `[RISK]` entries in §15 High-Risk Areas (source: §15 Debt Markers).
