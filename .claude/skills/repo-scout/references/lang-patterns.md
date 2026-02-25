# Backend Patterns — Reference for /repo-scout

## Language Detection

| Build File | Language |
|---|---|
| `go.mod` | Go |
| `package.json` | Node.js / TypeScript |
| `pom.xml` / `build.gradle` / `build.gradle.kts` | Java / Kotlin |
| `requirements.txt` / `pyproject.toml` / `setup.py` | Python |
| `Cargo.toml` | Rust (generic scan, no dedicated section) |

If multiple build files → monorepo. Note all detected languages and scan each independently.
If none found → ⚠️ WARNING: Unknown stack. Generic scan only.

---

## Go Backend Patterns

### Build Files

| File | Purpose |
|------|---------|
| `go.mod` | Module, Go version, dependencies |
| `go.sum` | Dependency checksums |
| `Makefile` | Build targets, utilities |
| `.golangci.yaml` / `.golangci.yml` | Linter config |

### Route Registration Patterns

#### REST Frameworks

| Framework | Import | Route Patterns |
|-----------|--------|----------------|
| **go-chi** | `github.com/go-chi/chi` | `r.Get(`, `r.Post(`, `r.Put(`, `r.Delete(`, `r.Route(`, `r.HandleFunc(` |
| **gin** | `github.com/gin-gonic/gin` | `gin.GET(`, `gin.POST(`, `engine.GET(`, `group.GET(` |
| **echo** | `github.com/labstack/echo` | `e.GET(`, `e.POST(`, `echo.GET(` |
| **stdlib** | `net/http` | `http.HandleFunc(`, `mux.Handle(`, `mux.HandleFunc(` |
| **gorilla/mux** | `github.com/gorilla/mux` | `r.HandleFunc(`, `r.Methods(` |
| **fiber** | `github.com/gofiber/fiber` | `app.Get(`, `app.Post(` |

#### gRPC

| Pattern | Purpose |
|---------|---------|
| `pb.Register*Server(` | gRPC service registration |
| `google.golang.org/grpc` | gRPC framework import |
| `*.proto` files | Service + RPC definitions |
| `protoc-gen-go-grpc` | Go code generator from proto |

#### Grep String for Route Search

```text
r\.Get\(|r\.Post\(|r\.Put\(|r\.Delete\(|r\.Route\(|r\.HandleFunc\(|\.GET\(|\.POST\(|\.PUT\(|\.DELETE\(|HandleFunc\(|pb\.Register|echo\.|fiber\.
```

### Test Patterns

| Type | Indicators |
|------|------------|
| **Unit** | `*_test.go` without build tags, imports: `testing`, `testify`, `gomock` |
| **Integration** | `//go:build integration`, imports: `sqlmock`, `testcontainers`, `dockertest` |
| **Benchmark** | `Benchmark*` functions in `*_test.go` |
| **Fuzz** | `Fuzz*` functions in `*_test.go` (Go 1.18+) |

### Test Frameworks in go.mod

| Library | Purpose |
|---------|---------|
| `github.com/stretchr/testify` | Assertions (assert/require) + mocking |
| `go.uber.org/mock` / `github.com/golang/mock` | GoMock code generation |
| `github.com/DATA-DOG/go-sqlmock` | SQL mocking |
| `github.com/testcontainers/testcontainers-go` | Docker-based integration tests |
| `github.com/ory/dockertest` | Docker test helpers |

### Specification Files

| Glob | Format |
|------|--------|
| `**/swagger.json`, `**/swagger.yaml` | Swagger 2.0 |
| `**/openapi.json`, `**/openapi.yaml` | OpenAPI 3.x |
| `**/*.swagger.json` | gRPC-gateway generated |
| `**/*.proto` | Protocol Buffers |
| `**/*.http`, `**/api.http` | JetBrains HTTP Client |

### Infrastructure Markers

| Glob | What it is |
|------|------------|
| `.github/workflows/*.yml` | GitHub Actions CI/CD |
| `.gitlab-ci.yml` | GitLab CI |
| `Jenkinsfile` | Jenkins pipeline |
| `Dockerfile`, `docker-compose.yaml` | Containerization |
| `migrations/`, `**/changesets/` | DB migrations (Liquibase) |
| `**/goose/`, `**/atlas.hcl` | DB migrations (goose/Atlas) |
| `.dev-platform/` | inDriver Dev-Platform |
| `config/*.yaml` | Environment configuration |
| `deployments/` | Helm charts, K8s manifests |

### AI Setup Files

| File | Tool |
|------|------|
| `CLAUDE.md` | Claude Code |
| `.claude/qa_agent.md` | Claude Code QA Agent |
| `.claude/skills/*/SKILL.md` | Claude Code Skills |
| `.agents/skills/*/SKILL.md` | Alternative structure |
| `AGENTS.md` | Zed/Cline/Continue.dev |
| `.cursor/rules/*.mdc` | Cursor IDE |
| `.github/copilot-instructions.md` | GitHub/VS Code Copilot |

---

## Python Backend Patterns

### Build Files

| File | Purpose |
|------|---------|
| `requirements.txt` / `pyproject.toml` | Dependencies |
| `setup.py` / `setup.cfg` | Package metadata |
| `Makefile` / `tox.ini` | Build/test runners |

### Route Registration Patterns

| Framework | Route Patterns |
|-----------|----------------|
| **FastAPI** | `@app.get(`, `@app.post(`, `@router.get(`, `@router.post(` |
| **Flask** | `@app.route(`, `@bp.route(` |
| **Django** | `path(`, `re_path(`, `url(` in `urls.py` |
| **Starlette** | `Route(`, `routes=[` |

### Grep String for Route Search

```text
@app\.get\(|@app\.post\(|@router\.get\(|@router\.post\(|@app\.route\(|@bp\.route\(|path\(|re_path\(
```

### Test Patterns

| Type | Indicators |
|------|------------|
| **Unit** | `test_*.py` / `*_test.py`, imports: `pytest`, `unittest` |
| **Integration** | `@pytest.mark.integration`, `TestCase` with DB/Docker |
| **E2E/API** | Separate test repo, or `tests/e2e/` |

### Test Frameworks (requirements.txt)

| Library | Purpose |
|---------|---------|
| `pytest` | Test runner + assertions |
| `pytest-asyncio` | Async test support |
| `pytest-mock` / `unittest.mock` | Mocking |
| `httpx` / `requests` | HTTP client for API tests |
| `testcontainers` | Docker-based integration |

---

## Node.js / TypeScript Backend Patterns

### Build Files

| File | Purpose |
|------|---------|
| `package.json` | Dependencies, scripts |
| `tsconfig.json` | TypeScript config |
| `nest-cli.json` | NestJS config |

### Route Registration Patterns

| Framework | Route Patterns |
|-----------|----------------|
| **Express** | `router.get(`, `router.post(`, `app.get(`, `app.post(` |
| **NestJS** | `@Controller(`, `@Get(`, `@Post(`, `@Put(`, `@Delete(` |
| **Fastify** | `fastify.get(`, `fastify.post(`, `app.route(` |
| **Koa** | `router.get(`, `router.post(` |

### Grep String for Route Search

```text
router\.get\(|router\.post\(|app\.get\(|app\.post\(|@Controller\(|@Get\(|@Post\(|fastify\.get\(
```

### Test Patterns

| Type | Indicators |
|------|------------|
| **Unit** | `*.test.ts` / `*.spec.ts`, imports: `jest`, `vitest`, `mocha` |
| **Integration** | `*.integration.spec.ts`, `supertest`, `testcontainers-node` |
| **E2E** | `*.e2e-spec.ts` (NestJS convention), Playwright/Cypress for API |

### Test Frameworks (package.json)

| Library | Purpose |
|---------|---------|
| `jest` / `vitest` | Test runner |
| `@nestjs/testing` | NestJS test module |
| `supertest` | HTTP integration testing |
| `testcontainers` | Docker-based integration |

---

## Java / Kotlin Backend Patterns

### Build Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies |
| `build.gradle` / `build.gradle.kts` | Gradle dependencies |
| `settings.gradle.kts` | Multi-module config |

### Route Registration Patterns

| Framework | Route Patterns |
|-----------|----------------|
| **Spring MVC** | `@GetMapping(`, `@PostMapping(`, `@PutMapping(`, `@DeleteMapping(`, `@RequestMapping(` |
| **Spring WebFlux** | `RouterFunction`, `route().GET(`, `route().POST(` |
| **Ktor** | `routing {`, `get(`, `post(`, `put(`, `delete(` |
| **Quarkus** | `@Path(`, `@GET`, `@POST`, `@PUT`, `@DELETE` |

### Grep String for Route Search

```text
@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@RequestMapping|@Path\(|routing \{|route\(\)\.GET
```

### Test Patterns

| Type | Indicators |
|------|------------|
| **Unit** | `*Test.java` / `*Test.kt` / `*Tests.kt`, `@Test` (JUnit 5) |
| **Integration** | `@SpringBootTest`, `@DataJpaTest`, `@Testcontainers` |
| **E2E/API** | `@AutoConfigureMockMvc`, separate test module |

### Test Frameworks (pom.xml / build.gradle)

| Library | Purpose |
|---------|---------|
| `junit-jupiter` | JUnit 5 test runner |
| `mockito-kotlin` / `mockk` | Mocking |
| `testcontainers` | Docker-based integration |
| `spring-boot-test` | Spring context testing |
| `kotest` | Kotlin assertion/test framework |
