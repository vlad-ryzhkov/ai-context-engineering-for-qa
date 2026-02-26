# Report Template repo-scout-report.md

```markdown
# Repo Scout Report: {repo-name}

> Generated: {date} | Skill: /repo-scout

## 1. Repository Profile

| Parameter | Value |
|-----------|-------|
| Language | {Go / Python / Node.js / TypeScript / Java / Kotlin} |
| Runtime/Version | {go 1.21 / python 3.11 / node 20 / jdk 17} |
| Module/Package | {module path / package name / artifact ID} |
| Service Type | {REST API / gRPC / Mixed / CLI / Consumer} |
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

| # | Endpoint/RPC | Field | Rule | Error Code |
|---|-------------|-------|------|------------|

## 4. Error Mapping

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

## 7. Existing Test Coverage

| Type | Files | Location | Framework |
|------|-------|----------|-----------|
| Unit | {N} | {test directory} | {test framework} |
| Integration | {N} | {path} | {test framework + mock library} |
| E2E/API | {N or "external repo"} | {path or link} | {framework} |

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

| # | From | To | Trigger | Guard | Error on Rejection |
|---|------|----|---------|-------|--------------------|

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

| # | Entity | Create | Read | Update | Delete | Soft Delete |
|---|--------|--------|------|--------|--------|-------------|

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

| # | Layer | Component | Errors Generated | Config Location |
|---|-------|-----------|-----------------|-----------------|
| {order} | {Gateway / Mesh / Middleware / Handler} | {Envoy / Istio / custom} | {401 JWT / 403 RBAC / 429 Rate Limit} | {file path} |

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

### Cross-Repo Prerequisites

| # | Dependency Repo | What's Needed | Type | Status |
|---|----------------|---------------|------|--------|
| {N} | {repo-name} | {shared proto update / gateway config} | {PR / Config / Deploy} | {Merged / Pending / Blocked} |

## 15. QA Scenario Matrix

> CONDITIONAL: Include only if business logic analysis yielded testable scenarios in Phases 3.5–3.8.

### Priority Summary

| Priority | Count | Description |
|----------|-------|-------------|
| P0 — Smoke | {N} | Core happy paths, auth, create-order chain |
| P1 — Regression | {N} | State transitions, CRUD, pagination, error codes |
| P2 — Edge | {N} | Boundary values, type coercion, dead config |
| Skip | {N} | Requires infrastructure not available in test env |

### Cross-Cutting Scenarios

| # | RPC/Endpoint | Test Case | Key Input | Expected Result | Priority | Affected Endpoints |
|---|-------------|-----------|-----------|----------------|----------|--------------------|

### Per-Domain Scenarios

| # | RPC/Endpoint | Test Case | Key Input | Expected Result | Priority | Source Section |
|---|-------------|-----------|-----------|----------------|----------|---------------|

### Entity Lifecycle Scenarios

| # | Entity | Lifecycle Step | Priority | Dependencies | Cleanup Order |
|---|--------|---------------|----------|--------------|---------------|

### Skip List

| # | Scenario | Reason | Unblock Condition |
|---|----------|--------|-------------------|
```
