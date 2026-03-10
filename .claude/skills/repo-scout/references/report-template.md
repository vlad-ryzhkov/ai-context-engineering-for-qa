# Report Template repo-scout-report.md

## Table of Contents

**Required (§1–§10):** [Repository Profile](#1-repository-profile) · [API Surface Catalog](#2-api-surface-catalog) · [Validation Rules](#3-validation-rules) · [Error Mapping](#4-error-mapping) · [Auth & Access Control](#5-auth--access-control) · [Specification Inventory](#6-specification-inventory) · [Existing Test Coverage](#7-existing-test-coverage) · [Infrastructure](#8-infrastructure) · [AI Setup Status](#9-ai-setup-status) · [Readiness Assessment](#10-readiness-assessment)

**Conditional (§11–§18):** [State Transition Matrix](#11-state-transition-matrix) · [Entity & Data Model](#12-entity--data-model) · [Behavioral Nuances](#13-behavioral-nuances) · [Config & Host Context](#14-config--host-context) · [Test Generation Blueprint](#15-test-generation-blueprint) · [Event Catalog](#16-event-catalog) · [QA Risk Assessment](#17-qa-risk-assessment--testability-issues) · [Resilience Mechanisms](#18-resilience-mechanisms)

---

````markdown
# Repo Scout Report: {repo-name}

> Generated: {date} | Skill: /repo-scout

## 1. Repository Profile

| Parameter | Value |
|-----------|-------|
| Language | {Go / Python / Node.js / TypeScript / Java / Kotlin} |
| Runtime/Version | {go 1.21 / python 3.11 / node 20 / jdk 17} |
| Module/Package | {module path / package name / artifact ID} |
| Service Type | {REST API / gRPC / Mixed / CLI / Consumer} |
| API Protocol | {REST / gRPC / REST+gRPC / GraphQL / Mixed} |
| Documentation Language | {English / Russian / Mixed / N/A} |
| Services | {list of entry points / service modules} |
| Source Files | {N source files} |
| Test Files | {N test files} |

### Key Dependencies

| Category | Library |
|----------|---------|
| HTTP Framework | {gin / FastAPI / Express / Spring Boot / none} |
| gRPC | {present / none} |
| DB Driver | {driver name or ORM / none} |
| Queue Client | {kafka / rabbitmq / none} |
| Cache Client | {redis / none} |

## 2. API Surface Catalog

**Summary:** {N REST endpoints} + {M gRPC RPCs} + {K GraphQL queries/mutations} = {total}

### Business Domain Map

| # | Domain | Endpoints | Risk Level | Key Entities |
|---|--------|-----------|------------|--------------|
| 1 | {User Management} | {POST /users, GET /users/:id, ...} | {HIGH} | {User, Profile} |

### REST Endpoints
| # | Method | Path | Description | Auth | Risk |
|---|--------|------|-------------|------|------|

### gRPC RPCs
| # | Service | Method | Request → Response | Streaming |
|---|---------|--------|--------------------|-----------|

### Additional Sources
- [ ] HTTP client files: {path or "none"}
- [ ] Postman collections: {path or "none"}

## 3. Validation Rules

| # | Endpoint/RPC | Field | Rule | Error Code | Source | Documented |
|---|-------------|-------|------|------------|--------|-----------|
| {N} | {endpoint} | {field} | {rule} | {code} | {PROTO / SWAGGER / CODE / DOCS} | {Y / [UNDOCUMENTED]} |

> **Source:** Where the validation is defined. **Documented:** `[UNDOCUMENTED]` = code-level check with no proto/swagger counterpart — invisible to spec-only test generation.

## 4. Error Mapping

> **gRPC codes NOT returned by this service:** {list unused code IDs, e.g., 1, 2, 4, 6, 10, 11, 14, 15}
> **Non-standard choices:** {e.g., PermissionDenied(7) used for auth failures — standard: Unauthenticated(16)} or "none"

| Error Constant | gRPC Code | HTTP Code | Trigger Condition |
|---------------|-----------|-----------|-------------------|

## 5. Auth & Access Control

### Auth Mechanisms

| Mechanism | Type | Details |
|-----------|------|---------|
| {JWT / Session / OAuth / API Key} | {Header / Cookie / Query} | {library, config location} |

### Auth Flow

> {token extraction method} → {validation step} → {permission check} → {handler dispatch}
> On auth failure: {status code} + {error body}

### Endpoint Auth Matrix

| # | Endpoint/RPC | Auth Required | Role/Permission |
|---|-------------|---------------|-----------------|

## 6. Specification Inventory

> Exact relative file paths are MANDATORY — downstream skills read these files directly.

| File (relative path) | Format | Endpoints | Completeness |
|----------------------|--------|-----------|--------------|
| {exact/relative/path/to/file} | {OpenAPI 3.0 / Swagger 2.0 / Proto3} | {N} | {Complete / Partial / Stale} |

**Coverage:** {X}/{total} endpoints have specification = {%}

Formula: covered endpoints / (REST + gRPC) × 100

### Discovered Documentation (S-DOC)

> If a doc covers a topic comprehensively → reference doc path in relevant report section, report gaps only.

| Doc Path | Content Type | Topics Covered | Coverage | Quality |
|----------|-------------|----------------|----------|---------|
| {relative/path} | {QA Checklist / Env Setup / API Ref / Architecture} | {endpoints, errors, auth, setup, etc.} | {High / Medium / Low} | {Current / Stale / Partial} |

**Note:** Comprehensive docs (>70% coverage) are referenced rather than duplicated. Gaps and stale info flagged below.

### Documentation Gaps

| Topic | Doc Says | Code Says | Impact |
|-------|----------|-----------|--------|
| {topic} | {doc assertion or "not covered"} | {code reality} | {test generation impact} |

## 7. Existing Test Coverage

| Type | Files | Location | Framework | Libraries |
|------|-------|----------|-----------|-----------|
| Unit | {N} | {test directory} | {test framework} | {testify, mockk, etc.} |
| Integration | {N} | {path} | {test framework + mock library} | {testcontainers, sqlmock, etc.} |
| E2E/API | {N or "external repo"} | {path or link} | {framework} | {supertest, rest-assured, etc.} |

> Coverage benchmarks for /api-isolated-tests planning: controllers 95% · services 90% · helpers 85% · config/infra 70% · third-party 60%

## 8. Infrastructure

| Component | Present | Details |
|-----------|---------|---------|
| CI/CD | {✅/❌} | {GitHub Actions / GitLab CI} |
| Docker | {✅/❌} | {N services in compose} |
| DB | {✅/❌} | {MySQL / PostgreSQL / MongoDB} |
| Migrations | {✅/❌} | {Liquibase / Flyway / goose / Alembic / Knex}, {N changesets} |
| Message Queue | {✅/❌} | {Kafka / RabbitMQ / NATS} |
| Cache | {✅/❌} | {Redis / Memcached} |
| Dev-Platform | {✅/❌} | {shared services} |

### Deployment Topology (S6)

| Environment | Source File | Replicas | Key Config Differences |
|-------------|-----------|----------|----------------------|
| {dev / staging / prod} | {values-dev.yaml} | {N} | {feature flags, resource limits, etc.} |

### Dependency Staleness

| Lock File | Last Modified | Age | Critical Deps Flagged | Status |
|-----------|--------------|-----|----------------------|--------|
| {go.sum / package-lock.json / poetry.lock} | {date} | {N months} | {jwt-go, express@4, etc. or "none"} | {🟢 Current / 🟡 Stale / 🔴 [DEPENDENCY_RISK]} |

## 9. AI Setup Status

| File | Status |
|------|--------|
| CLAUDE.md | {✅ present / ❌ absent} |
| qa_agent.md | {✅ / ❌} |
| Skills | {N skills: list / ❌} |
| .agents/ | {✅ / ❌} |
| .cursor/rules/ | {✅ / ❌} |

## 10. Readiness Assessment

| Criterion | Status | Comment |
|-----------|--------|---------|
| API Specs | {🟢 Complete / 🟡 Partial / 🔴 Missing} | {details} |
| Test Infrastructure | {🟢 Ready / 🟡 Needs Setup / 🔴 Missing} | {unit: N files, integration: N files, e2e: present/absent. Benchmarks: controllers 95%, services 90%, helpers 85%} |
| Documentation | {🟢 / 🟡 / 🔴} | {details} |
| AI Setup | {🟢 / 🟡 / 🔴} | {details} |

### Blockers

{List of blockers or "No blockers"}

### Recommended Next Step

{Specific recommendation: /api-isolated-tests, /init-project, "obtain specification from the team", etc.}

## 11. State Transition Matrix

> CONDITIONAL: Include only if state machine patterns detected in Phase 3.5.

### State Enum: {EntityName}

| # | From | To | Trigger | Guard | Error Code on Rejection |
|---|------|----|---------|-------|------------------------|

> **Error Code on Rejection:** Record exact code — `HTTP 409`, `gRPC AlreadyExists(6)`.
> Use `[NO_SPECIFIC_CODE]` if handler returns generic error without precise status for this transition.

### Unreachable States

| State | Why Unreachable | Risk |
|-------|----------------|------|

### Multi-Step Transition Sequences

| # | Sequence | Business Flow | Priority |
|---|----------|--------------|----------|
| {N} | {state1 → state2 → state3 → ...} | {description of business lifecycle} | {P0/P1} |

## 12. Entity & Data Model

> CONDITIONAL: Include only if entity relationship patterns detected in Phase 3.6.

### CRUD Matrix

| # | Entity | ID Type | Create | Read | Update | Delete | Soft Delete |
|---|--------|---------|--------|------|--------|--------|-------------|
| {N} | {entity} | {int32 / int64 / UUID / string} | {✅ / [NO_CREATE]} | {✅} | {✅ / [NO_UPDATE]} | {✅ / [NO_DELETE]} | {✅ / ❌} |

> **Tags:** `[NO_{OP}]` = operation does not exist for this entity — do NOT generate test cases for it.

### Entity Hierarchy

> Include only if hierarchy depth > 2. Visual tree with depth count.

```text
{ENTITY_A}(1) → {ENTITY_B}(2) → {ENTITY_C}(3) → {ENTITY_D}(4)
```

**Max depth:** {N} — {`[DEEP_HIERARCHY]` if > 3, implies multi-step @BeforeEach setup}

### Create-Order Chain

> Entities MUST be created in this order (FK dependencies). Cleanup MUST proceed in REVERSE order.

```text
{Entity A} → {Entity B} → {Entity C}
```

### Entity Relationships

| Parent | Child | FK Field | On Delete | On Update |
|--------|-------|----------|-----------|-----------|

### Pagination

| Endpoint | Strategy | Parameters | Default Page Size | Max Page Size |
|----------|----------|------------|-------------------|---------------|

### Data Consistency Model

| Operation | Consistency | Mechanism | Test Implication |
|-----------|-------------|-----------|------------------|
| {Write X → Read X} | {Strong / Eventual} | {Transaction / Kafka / Cache TTL} | {Immediate assert / Awaitility polling} |

### Type Handling

| Field | Source Type | Target Type | Conversion | Edge Cases |
|-------|------------|-------------|------------|------------|

### Cross-Layer Type Consistency (S1)

> Fields appearing in multiple layers with different declared types.

| Field | Proto Type | Code Type | Doc Type | Verdict |
|-------|-----------|-----------|----------|---------|
| {field} | {int32} | {int64} | {int32} | {MISMATCH — API tests must handle both} |

### Read/Write Topology (S5)

> Include only if master/replica patterns detected.

| Operation Type | Target | Consistency | Test Implication |
|---------------|--------|-------------|------------------|
| {Write RPCs} | {MASTER_ONLY} | {Strong} | {Direct assert after write} |
| {Read RPCs} | {REPLICA_SAFE} | {Eventual} | {Polling/retry needed for write verification} |

### DB Constraints (S4-LITE)

> Include only if ORM tags found. Source: ORM annotations, not migrations.

| Entity | Field | Constraint | Value | NEG Test Scenario |
|--------|-------|-----------|-------|-------------------|
| {entity} | {field} | {VARCHAR / UNIQUE / NOT NULL} | {50 / composite(a,b)} | {Send 51 chars → expect 400} |

## 13. Behavioral Nuances

> CONDITIONAL: Include only if nuances detected in Phase 3.7.

### Internal vs External Endpoints

| Endpoint | Visibility | Caller | Auth Difference |
|----------|-----------|--------|----------------|

### Conditional Behavior

| Endpoint | Condition | Behavior A | Behavior B |
|----------|-----------|------------|------------|

### Search / Filter Semantics

| Endpoint | Parameter | Empty Value Behavior | Case Sensitivity | Partial Match |
|----------|-----------|---------------------|------------------|---------------|

### Concurrency Model

| Model | Grep Evidence | Detected Patterns | QA Risk |
|-------|--------------|-------------------|---------|
| {goroutines / asyncio / event loop / coroutines / threads} | {`go func`, `sync.Mutex`, etc.} | {N occurrences in M files} | {race conditions / deadlocks / unhandled rejections / etc.} |

### Non-Existent Resource Handling

| Endpoint | Resource Not Found | Response Code | Response Body |
|----------|--------------------|---------------|---------------|

### Enum / Value Range

| Field | Valid Values | Out-of-Range Behavior | Default |
|-------|-------------|----------------------|---------|

## 14. Config & Host Context

> CONDITIONAL: Include only if config/host patterns detected in Phase 3.8.

### Whitelisted / Hardcoded Values

| Config Key | Values | Source File | Used By |
|------------|--------|-------------|---------|

### Host System

| Component | Type | Integration Point | Test Impact |
|-----------|------|-------------------|-------------|

### Request Lifecycle Layers

> CONDITIONAL: include if middleware chain or host system detected (almost all REST services have middleware).

| # | Layer | Component | Errors Generated | Config Location |
|---|-------|-----------|-----------------|-----------------|
| {order} | {Entry Point / Middleware / Handler / Downstream} | {HTTP:8080 / JWT middleware / handler / PostgreSQL} | {401 JWT / 403 RBAC / 429 Rate Limit} | {file path} |

### Access Path Variants

| # | Path | Route | Whitelist Applied | Auth Difference |
|---|------|-------|-------------------|-----------------|
| 1 | Direct | {domain:port/endpoint} | {none / service-level} | {service JWT} |
| 2 | API Gateway | {gateway/endpoint} | {gateway whitelist} | {gateway + service} |
| 3 | Sidecar | {mesh-internal} | {mesh policy} | {mTLS} |

### Dead Config Detection

| Config Key | Defined In | Referenced By | Status |
|------------|-----------|---------------|--------|
| {key} | {config file} | {none — unreferenced} | DEAD |

### Test Environment Setup

| Dependency | Local Setup | CI Setup | Config Override |
|------------|-------------|----------|----------------|

#### Token Configuration (S-ENV)

| Token Type | Format | Source | Env Var / Config Path |
|-----------|--------|--------|-----------------------|
| {Auth token / API key / gRPC metadata} | {JWT / Bearer / custom} | {config file / env var / secrets manager} | {path or var name} |

#### Data Seed Requirements (S-ENV)

> What must exist before tests can run. Order matters (FK dependencies).

| # | Entity | Seed Method | Required Before | Notes |
|---|--------|------------|-----------------|-------|
| {N} | {entity} | {API call / DB seed script / fixture file} | {parent entity or "first"} | {creation order constraint} |

#### Local / Remote Setup Commands (S-ENV)

| Action | Command | Notes |
|--------|---------|-------|
| Build | {make build / ./gradlew build} | |
| Run locally | {make run / docker-compose up} | {required services} |
| Seed data | {make seed / script path} | |
| Run tests | {make test / ./gradlew test} | |
| gRPC reflection | {enabled / disabled} | {proto import paths if needed} |

### Cross-Repo Prerequisites

| # | Dependency Repo | What's Needed | Type | Status |
|---|----------------|---------------|------|--------|
| {N} | {repo-name} | {shared proto update / gateway config} | {PR / Config / Deploy} | {Merged / Pending / Blocked} |

## 15. Test Generation Blueprint

> CONDITIONAL: Include only if Phases 3.5–3.10 yielded flows, risks, or constraints.
> Goal: Directives for /api-test-cases and /api-isolated-tests — NOT individual test cases.

### Cross-Cutting Business Flows

- [FLOW] {label}: {step1} → {step2} → {step3} — verify {expected assertion}

### High-Risk Areas

- [RISK] {area}: {what to test} — source: §{N}

> Derive from: [UNDOCUMENTED], [WEAK_TYPE], [NO_SPECIFIC_CODE], [AUTH_ANOMALY],
> [NO_MOCK], resilience gaps from §17, [DEEP_HIERARCHY], **[DEBT: P0] from §15 Debt Markers**,
> **[HOTSPOT] from §1 VCS Analysis**

### Debt Markers

> Source: Phase 3.11. P0 items are automatically promoted to High-Risk Areas above.

| # | Endpoint/Handler | File:Line | Marker | Comment | Priority |
|---|-----------------|-----------|--------|---------|----------|
| {N} | {POST /checkout} | {payment/handler.go:142} | FIXME | {no transaction rollback here} | P0 |
| {N} | {GET /search} | {search/service.go:88} | TODO | {add pagination limit enforcement} | P1 |

> **Tag legend (extended):** [UNDOCUMENTED] · [WEAK_TYPE] · [NO_SPECIFIC_CODE] · [AUTH_ANOMALY] · [NO_MOCK] · [DEEP_HIERARCHY] · **[DEBT: FIXME]** · **[DEBT: HACK]** · **[DEBT: P0]** · **[HOTSPOT: N changes]**

### Blocker Constraints

- [BLOCKER] {constraint description}

> Sources: CRUD gaps [NO_CREATE]/[NO_DELETE] (§12) · Eventual consistency (§12) ·
> [NO_TIMEOUT] (§17) · gRPC reflection disabled (§14) · Cross-repo prerequisites (§14)

## 16. Event Catalog

> CONDITIONAL: Include only if queue client in §8 and publish calls found in handlers.

| # | Handler/RPC | Topic/Channel | Framework | Trigger Condition | Payload Hint | Linked Transition |
|---|-------------|--------------|-----------|-------------------|--------------|-------------------|
| 1 | {CreateZone} | {geo.zone.created} | {Kafka} | {Always on success} | {ZoneCreatedEvent{zone_id, city_id}} | {§11: none} |

> **Test implication for /api-isolated-tests:**
> Each row = side-effect assertion: `State: "Event published: {topic}"`
> For [CRITICAL] handlers: also add failure mock: `Mock: Kafka returns error → verify handler rollback`

## 17. QA Risk Assessment & Testability Issues

> CONDITIONAL: Include if any risk flags detected across Phases 3–6.

| # | Risk Description | Category | Severity | Affected Endpoint/Entity | Source | Recommended Action |
|---|-----------------|----------|----------|--------------------------|---------|--------------------|
| 1 | {Missing auth on DELETE /admin/resource} | [AUTH_ANOMALY] | CRITICAL | DELETE /admin/resource | §5 | Verify intent — add auth test |
| 2 | {Code validates len>50 but no proto constraint} | [UNDOCUMENTED] | HIGH | POST /users | §3 | Add NEG test for 51-char input |
| 3 | {config.allow_countries never read in code} | [DEAD_CONFIG] | MEDIUM | N/A | §14 | Remove or connect to handler |

> **Summary:** {CRITICAL: N} + {HIGH: N} + {MEDIUM: N} + {LOW: N} = {total issues}

## 18. Resilience Mechanisms

> CONDITIONAL: Include only if retry, circuit breaker, idempotency, or timeout patterns
> detected in Phase 3.10.

### Idempotency Keys

| # | Endpoint/RPC | Header/Field | Duplicate Behavior | Source |
|---|-------------|-------------|-------------------|--------|

### Retry Policies

| # | Call Site | Target | Max Attempts | Backoff | Non-Idempotent Risk |
|---|-----------|--------|-------------|---------|---------------------|

### Circuit Breakers

| # | Protected Dependency | Library | Open Threshold | Fallback Behavior |
|---|---------------------|---------|---------------|-------------------|

### Timeout Configuration

| # | Endpoint/Client | Timeout | Config Location | Risk |
|---|----------------|---------|----------------|------|

> **Test implications for /api-isolated-tests:**
> - Idempotency: duplicate submission with same key → assert deduplicated response
> - Retry + write: network failure on write endpoint → assert no duplicate records
> - Circuit breaker: mock downstream errors at threshold → assert fallback triggers
> - [NO_TIMEOUT]: flag for load testing — do NOT skip
````
