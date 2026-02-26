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

### Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **gRPC** | `func (s *Server) MethodName(ctx context.Context, req *pb.XXXRequest) (*pb.XXXResponse, error)` |
| **chi/gorilla** | `func handlerName(w http.ResponseWriter, r *http.Request)` |
| **echo** | `func handlerName(c echo.Context) error` |
| **gin** | `func handlerName(c *gin.Context)` |
| **fiber** | `func handlerName(c *fiber.Ctx) error` |

### Error Patterns

| Pattern | Purpose |
|---------|---------|
| `status.Error(codes.` / `status.Errorf(codes.` | gRPC error with code |
| `echo.NewHTTPError(` | Echo HTTP error |
| `c.JSON(http.Status` / `c.AbortWithStatusJSON(` | Gin/Echo response with status |
| `var Err*` / `errors.New(` | Custom error variables |
| `fmt.Errorf(` | Wrapped errors |

### Validation Patterns

| Pattern | Purpose |
|---------|---------|
| struct tag `validate:"required,min=,max="` | Go validator struct tags |
| `proto validate` tags in `.proto` | Protobuf field validation |
| `validator.New()` / `.Struct(` / `.Var(` | go-playground/validator calls |

### Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `interceptor` / `UnaryInterceptor` / `StreamInterceptor` | gRPC middleware |
| `r.Use(` / `e.Use(` / `app.Use(` | HTTP middleware registration |
| `extractToken` / `parseToken` / `jwt.Parse` | Token extraction |
| `checkAccess` / `authorize` / `rbac` | Access control checks |

### State Machine Patterns

| Pattern | Purpose |
|---------|---------|
| `iota` in `const ( ... )` block | Enum definition (Go idiom) |
| `type *Status int` / `type *State string` | Status/state type alias |
| `.Status =` / `.State =` | State assignment in handler/service |
| `switch *.Status` / `switch *.State` | State-conditional branching |
| `StatusPending`, `StatusActive`, `StatusDeleted` | Named state constants |
| `if prev.Status != X { return ErrInvalidTransition }` | Transition guard |

#### Grep String for State Machine Search

```text
iota|\.Status\s*=|\.State\s*=|switch\s+\w+\.Status|switch\s+\w+\.State|StatusPending|StatusActive|StatusDeleted|ErrInvalidTransition|InvalidTransition
```

### Entity Relationship Patterns

| Pattern | Purpose |
|---------|---------|
| `*_id` fields in structs / DB schemas | Foreign key reference |
| `REFERENCES` / `FOREIGN KEY` in migrations | DB-level FK constraint |
| `ON DELETE CASCADE` / `ON DELETE SET NULL` | Cascade behavior |
| `belongs_to` / `has_many` / ORM tags (`gorm:"foreignKey:"`) | ORM relationship |
| `JOIN` / `LEFT JOIN` in raw SQL | Cross-entity query |
| `tx.Create(&parent)` then `tx.Create(&child{ParentID: parent.ID})` | Create-order dependency |

#### Grep String for Entity Relationship Search

```text
_id\b|REFERENCES|FOREIGN KEY|CASCADE|SET NULL|belongs_to|has_many|foreignKey:|JOIN\s|LEFT JOIN|\.ParentID|\.parent_id
```

### Async / Consistency Patterns

| Pattern | Purpose |
|---------|---------|
| `go func()` | Fire-and-forget goroutine |
| `kafka.Produce` / `publisher.Publish` / `nats.Publish` | Async event emission |
| `tx.Commit()` / `tx.Rollback()` | Transaction boundary |
| `errgroup.Group` / `sync.WaitGroup` | Concurrent operation coordination |
| `eventual consistency` / `sync` / `async` in comments | Consistency model hint |
| `SELECT ... FOR UPDATE` | Pessimistic lock |

#### Grep String for Async/Consistency Search

```text
go func\(|kafka\.Produce|publisher\.Publish|nats\.Publish|tx\.Commit|tx\.Rollback|errgroup\.Group|sync\.WaitGroup|FOR UPDATE|eventual.consistency
```

### Batch / Collection Patterns

| Pattern | Purpose |
|---------|---------|
| `BatchCreate` / `BatchUpdate` / `BatchDelete` | Bulk mutation operations |
| `for _, item := range items` near DB/API call | Iterative batch processing |
| `cursor` / `offset` / `limit` / `page_token` | Pagination parameters |
| `stream.Send(` / `stream.Recv(` | gRPC streaming batch |
| `BulkInsert` / `InsertMany` / `CopyFrom` | DB bulk insert |

#### Grep String for Batch/Collection Search

```text
Batch(Create|Update|Delete)|BulkInsert|InsertMany|CopyFrom|for.*range.*items|cursor|offset|limit|page_token|stream\.Send\(|stream\.Recv\(
```

### Type Handling Patterns

| Pattern | Purpose |
|---------|---------|
| `strings.ToLower` / `strings.ToUpper` | Case normalization |
| `strconv.Atoi` / `strconv.ParseFloat` | String-to-number conversion |
| `json.Number` | Numeric JSON ambiguity handling |
| `UnmarshalJSON` / `MarshalJSON` | Custom JSON serialization |
| `time.Parse` / `time.Format` | Date/time format conversion |
| `uuid.Parse` / `uuid.New()` | UUID handling |

#### Grep String for Type Handling Search

```text
strings\.ToLower|strings\.ToUpper|strconv\.Atoi|strconv\.Parse|json\.Number|UnmarshalJSON|MarshalJSON|time\.Parse|time\.Format|uuid\.Parse|uuid\.New
```

### Host System / Plugin Detection Patterns

| Pattern | Purpose |
|---------|---------|
| `envoy` / `ext_proc` / `external_processing` | Envoy proxy filter |
| `istio` / `VirtualService` / `DestinationRule` | Istio service mesh CRDs |
| `nginx` / `ingress` annotations | Nginx ingress controller |
| `kong` / `KongPlugin` / `KongIngress` | Kong API gateway |
| `wasm` / `proxy_wasm` / `proxy-wasm` | WASM plugin (Envoy/Istio) |
| `grpc_web` / `envoy.filters.http` | Envoy HTTP filter chain |

#### Grep String for Host System Search

```text
envoy|ext_proc|external_processing|istio|VirtualService|DestinationRule|nginx|ingress|kong|KongPlugin|proxy.wasm|grpc_web|envoy\.filters
```

---

## Common Patterns (All Languages)

### Specification Files

| Glob | Format |
|------|--------|
| `**/swagger.json`, `**/swagger.yaml`, `**/swagger.yml` | Swagger 2.0 |
| `**/openapi.json`, `**/openapi.yaml`, `**/openapi.yml` | OpenAPI 3.x |
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
| `.dev-platform/` | Internal Dev-Platform |
| `config/*.yaml` | Environment configuration |
| `deployments/` | Helm charts, K8s manifests |

### Business Logic Detection (All Languages)

> Generic grep strings for detecting business logic patterns regardless of language.
> Use these when language-specific patterns yield no results or for cross-language monorepos.

| Category | Grep Pattern | What It Finds |
|----------|-------------|---------------|
| State Machines | `status\|state\|Status\|State\|iota\|enum\|ENUM` | State/status enums and transitions |
| Entity Relationships | `_id\b\|REFERENCES\|FOREIGN KEY\|belongs_to\|has_many\|JOIN` | FK references, ORM relations |
| Batch Operations | `[Bb]atch\|[Bb]ulk\|InsertMany\|CopyFrom` | Bulk data operations |
| Pagination | `cursor\|offset\|limit\|page_token\|pageSize\|page_size\|nextPage` | Pagination parameters |
| Async Patterns | `async\|await\|goroutine\|go func\|CompletableFuture\|Promise\|Deferred` | Async execution |
| Config Values | `whitelist\|allowlist\|blocklist\|blacklist\|config\.\|getenv\|os\.Getenv` | Hardcoded config / env access |
| Feature Flags | `feature.*flag\|toggle\|isEnabled\|is_enabled\|LaunchDarkly\|unleash` | Feature toggle patterns |
| Soft Delete | `deleted_at\|is_deleted\|soft.delete\|paranoid\|acts_as_paranoid` | Soft-delete markers |

### AI Setup Files

| File | Tool |
|------|------|
| `CLAUDE.md` | Claude Code |
| `.claude/qa_agent.md` | Claude Code QA Agent |
| `.claude/skills/**/*.md` | Claude Code Skills |
| `.agents/skills/**/*.md` | Alternative structure |
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

### Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **FastAPI** | `@app.get("/path")` / `@router.post("/path")` decorated `async def func_name(...)` |
| **Flask** | `@app.route("/path", methods=["GET"])` decorated `def func_name()` |
| **Django** | `def view_name(request)` in `views.py`, class-based `class ViewName(APIView)` |

### Error Patterns

| Pattern | Purpose |
|---------|---------|
| `HTTPException(status_code=` | FastAPI/Starlette HTTP error |
| `raise` + custom exception class | Custom error raising |
| `abort(` | Flask error response |
| `Response(status=` | DRF/Django response with status |

### Validation Patterns

| Pattern | Purpose |
|---------|---------|
| `Field(` / `field_validator` / `model_validator` | Pydantic v2 validation |
| `@validator` / `@root_validator` | Pydantic v1 validation |
| `serializers.CharField(` / `validators=[` | DRF serializer validation |
| `wtforms` / `Form` | Flask-WTF form validation |

### Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `Depends(` / `Security(` | FastAPI dependency injection (auth) |
| `@login_required` / `@permission_required` | Django/Flask auth decorators |
| `get_current_user` / `get_current_active_user` | FastAPI auth dependencies |
| `@app.before_request` / `@app.middleware` | Request-level middleware |

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

### Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **Express** | `(req, res)` or `(req, res, next)` callback |
| **NestJS** | `@Controller` class with `@Get()` / `@Post()` methods |
| **Fastify** | `(request, reply)` handler or schema-based route |

### Error Patterns

| Pattern | Purpose |
|---------|---------|
| `throw new HttpException(` | NestJS HTTP exception |
| `next(err)` / `next(new Error(` | Express error forwarding |
| `res.status(N).json(` | Express response with status |
| `reply.code(N).send(` | Fastify response with status |
| `class * extends Error` | Custom error classes |

### Validation Patterns

| Pattern | Purpose |
|---------|---------|
| `Joi.object(` / `.required()` / `.min(` | Joi schema validation |
| `z.object(` / `z.string()` / `.parse(` | Zod schema validation |
| `@IsNotEmpty()` / `@IsEmail()` / `@MinLength(` | class-validator decorators (NestJS) |
| `@UsePipes(ValidationPipe)` | NestJS validation pipe |

### Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `passport.authenticate(` | Passport.js strategy |
| `@UseGuards(AuthGuard)` | NestJS guard |
| `jwt.verify(` / `jwt.sign(` | JWT operations |
| `app.use(` / `router.use(` (with auth function) | Express middleware |

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

### Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **Spring MVC** | `@RestController` class with `@GetMapping` / `@PostMapping` methods |
| **Spring WebFlux** | `RouterFunction<ServerResponse>` or annotated controller with `Mono`/`Flux` |
| **Ktor** | `routing { get("/path") { ... } }` blocks |
| **Quarkus** | `@Path` class with `@GET` / `@POST` methods |

### Error Patterns

| Pattern | Purpose |
|---------|---------|
| `@ExceptionHandler` / `@ControllerAdvice` | Spring global error handling |
| `ResponseStatusException(` | Spring HTTP error |
| `throw` + custom exception | Custom exception throwing |
| `StatusPages` / `respondText(status =` | Ktor error handling |

### Validation Patterns

| Pattern | Purpose |
|---------|---------|
| `@Valid` / `@Validated` | Spring Bean Validation trigger |
| `@NotNull` / `@NotBlank` / `@Size(` / `@Pattern(` | Bean Validation (JSR 380) annotations |
| `@field:NotNull` / `@get:Size(` | Kotlin annotation use-site targets |
| `ConstraintValidator<` | Custom validator implementation |

### Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `@PreAuthorize(` / `@Secured(` / `@RolesAllowed(` | Spring method-level security |
| `SecurityFilterChain` / `HttpSecurity` | Spring Security config |
| `authenticate {` / `principal` | Ktor authentication |
| `@io.quarkus.security.Authenticated` | Quarkus security |
