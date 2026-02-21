# Go Backend Patterns — Reference for /repo-scout

## Build Files

| File | Purpose |
|------|---------|
| `go.mod` | Module, Go version, dependencies |
| `go.sum` | Dependency checksums |
| `Makefile` | Build targets, utilities |
| `.golangci.yaml` / `.golangci.yml` | Linter config |

## Route Registration Patterns

### REST Frameworks

| Framework | Import | Route Patterns |
|-----------|--------|----------------|
| **go-chi** | `github.com/go-chi/chi` | `r.Get(`, `r.Post(`, `r.Put(`, `r.Delete(`, `r.Route(`, `r.HandleFunc(` |
| **gin** | `github.com/gin-gonic/gin` | `gin.GET(`, `gin.POST(`, `engine.GET(`, `group.GET(` |
| **echo** | `github.com/labstack/echo` | `e.GET(`, `e.POST(`, `echo.GET(` |
| **stdlib** | `net/http` | `http.HandleFunc(`, `mux.Handle(`, `mux.HandleFunc(` |
| **gorilla/mux** | `github.com/gorilla/mux` | `r.HandleFunc(`, `r.Methods(` |
| **fiber** | `github.com/gofiber/fiber` | `app.Get(`, `app.Post(` |

### gRPC

| Pattern | Purpose |
|---------|---------|
| `pb.Register*Server(` | gRPC service registration |
| `google.golang.org/grpc` | gRPC framework import |
| `*.proto` files | Service + RPC definitions |
| `protoc-gen-go-grpc` | Go code generator from proto |

### Grep String for Route Search

```text
r\.Get\(|r\.Post\(|r\.Put\(|r\.Delete\(|r\.Route\(|r\.HandleFunc\(|\.GET\(|\.POST\(|\.PUT\(|\.DELETE\(|HandleFunc\(|pb\.Register|echo\.|fiber\.
```

## Test Patterns

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

## Specification Files

| Glob | Format |
|------|--------|
| `**/swagger.json`, `**/swagger.yaml` | Swagger 2.0 |
| `**/openapi.json`, `**/openapi.yaml` | OpenAPI 3.x |
| `**/*.swagger.json` | gRPC-gateway generated |
| `**/*.proto` | Protocol Buffers |
| `**/*.http`, `**/api.http` | JetBrains HTTP Client |

## Infrastructure Markers

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

## AI Setup Files

| File | Tool |
|------|------|
| `CLAUDE.md` | Claude Code |
| `.claude/qa_agent.md` | Claude Code QA Agent |
| `.claude/skills/*/SKILL.md` | Claude Code Skills |
| `.agents/skills/*/SKILL.md` | Alternative structure |
| `AGENTS.md` | Zed/Cline/Continue.dev |
| `.cursor/rules/*.mdc` | Cursor IDE |
| `.github/copilot-instructions.md` | GitHub/VS Code Copilot |
