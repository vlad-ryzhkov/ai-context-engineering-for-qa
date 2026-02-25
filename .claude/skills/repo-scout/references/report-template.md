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
| Services | {list from cmd/} |
| Source Files | {N .go files} |
| Test Files | {N _test.go files} |

### Key Dependencies

| Category | Library |
|----------|---------|
| HTTP Framework | {gin / FastAPI / Express / Spring Boot / none} |
| gRPC | {present / none} |
| DB Driver | {driver name or ORM / none} |
| Queue Client | {kafka / rabbitmq / none} |
| Cache Client | {redis / none} |

## 2. API Surface Catalog

**Summary:** {N REST endpoints} + {M gRPC RPCs} = {total}

### REST Endpoints
| # | Method | Path | Description | Auth |
|---|--------|------|-------------|------|

### gRPC RPCs
| # | Service | Method | Request → Response | Streaming |
|---|---------|--------|--------------------|-----------|

### Additional Sources
- [ ] HTTP client files: {path or "none"}
- [ ] Postman collections: {path or "none"}

## 3. Specification Inventory

| File | Format | Endpoints | Completeness |
|------|--------|-----------|--------------|
| {path} | {OpenAPI 3.0 / Swagger 2.0 / Proto3} | {N} | {Complete / Partial / Stale} |

**Coverage:** {X}/{total} endpoints have specification = {%}

Formula: covered endpoints / (REST + gRPC) × 100

## 4. Existing Test Coverage

| Type | Files | Location | Framework |
|------|-------|----------|-----------|
| Unit | {N} | {internal/...} | {testify / stdlib} |
| Integration | {N} | {path} | {testify + sqlmock} |
| E2E/API | {N or "external repo"} | {path or link} | {framework} |

> Coverage benchmarks for /test-cases planning: controllers 95% · services 90% · helpers 85% · config/infra 70% · third-party 60%

## 5. Infrastructure

| Component | Present | Details |
|-----------|---------|---------|
| CI/CD | {✅/❌} | {GitHub Actions / GitLab CI} |
| Docker | {✅/❌} | {N services in compose} |
| DB | {✅/❌} | {MySQL / PostgreSQL / MongoDB} |
| Migrations | {✅/❌} | {Liquibase / goose}, {N changesets} |
| Message Queue | {✅/❌} | {Kafka / RabbitMQ / NATS} |
| Cache | {✅/❌} | {Redis / Memcached} |
| Dev-Platform | {✅/❌} | {shared services} |

## 6. AI Setup Status

| File | Status |
|------|--------|
| CLAUDE.md | {✅ present / ❌ absent} |
| qa_agent.md | {✅ / ❌} |
| Skills | {N skills: list / ❌} |
| .agents/ | {✅ / ❌} |
| .cursor/rules/ | {✅ / ❌} |

## 7. Readiness Assessment

| Criterion | Status | Comment |
|-----------|--------|---------|
| API Specs | {🟢 Complete / 🟡 Partial / 🔴 Missing} | {details} |
| Test Infrastructure | {🟢 Ready / 🟡 Needs Setup / 🔴 Missing} | {unit: N files, integration: N files, e2e: present/absent. Benchmarks: controllers 95%, services 90%, helpers 85%} |
| Documentation | {🟢 / 🟡 / 🔴} | {details} |
| AI Setup | {🟢 / 🟡 / 🔴} | {details} |

### Blockers

{List of blockers or "No blockers"}

### Recommended Next Step

{Specific recommendation: /test-cases, /init-project, "obtain specification from the team", etc.}
```
