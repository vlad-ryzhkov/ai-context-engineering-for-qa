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

## Test Frameworks in go.mod

| Library | Purpose |
|---------|---------|
| `github.com/stretchr/testify` | Assertions (assert/require) + mocking |
| `go.uber.org/mock` / `github.com/golang/mock` | GoMock code generation |
| `github.com/DATA-DOG/go-sqlmock` | SQL mocking |
| `github.com/testcontainers/testcontainers-go` | Docker-based integration tests |
| `github.com/ory/dockertest` | Docker test helpers |

## Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **gRPC** | `func (s *Server) MethodName(ctx context.Context, req *pb.XXXRequest) (*pb.XXXResponse, error)` |
| **chi/gorilla** | `func handlerName(w http.ResponseWriter, r *http.Request)` |
| **echo** | `func handlerName(c echo.Context) error` |
| **gin** | `func handlerName(c *gin.Context)` |
| **fiber** | `func handlerName(c *fiber.Ctx) error` |

## Error Patterns

| Pattern | Purpose |
|---------|---------|
| `status.Error(codes.` / `status.Errorf(codes.` | gRPC error with code |
| `echo.NewHTTPError(` | Echo HTTP error |
| `c.JSON(http.Status` / `c.AbortWithStatusJSON(` | Gin/Echo response with status |
| `var Err*` / `errors.New(` | Custom error variables |
| `fmt.Errorf(` | Wrapped errors |

## Validation Patterns

| Pattern | Purpose |
|---------|---------|
| struct tag `validate:"required,min=,max="` | Go validator struct tags |
| `proto validate` tags in `.proto` | Protobuf field validation |
| `validator.New()` / `.Struct(` / `.Var(` | go-playground/validator calls |

## Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `interceptor` / `UnaryInterceptor` / `StreamInterceptor` | gRPC middleware |
| `r.Use(` / `e.Use(` / `app.Use(` | HTTP middleware registration |
| `extractToken` / `parseToken` / `jwt.Parse` | Token extraction |
| `checkAccess` / `authorize` / `rbac` | Access control checks |
| `Bearer` / `Authorization` | Auth header name and token prefix |

### Grep String for Auth/Middleware Search

```text
interceptor|UnaryInterceptor|StreamInterceptor|\.Use\(|extractToken|parseToken|jwt\.Parse|checkAccess|authorize|rbac|Bearer|Authorization
```

## State Machine Patterns

| Pattern | Purpose |
|---------|---------|
| `iota` in `const ( ... )` block | Enum definition (Go idiom) |
| `type *Status int` / `type *State string` | Status/state type alias |
| `.Status =` / `.State =` | State assignment in handler/service |
| `switch *.Status` / `switch *.State` | State-conditional branching |
| `StatusPending`, `StatusActive`, `StatusDeleted` | Named state constants |
| `if prev.Status != X { return ErrInvalidTransition }` | Transition guard |

### Grep String for State Machine Search

```text
iota|\.Status\s*=|\.State\s*=|switch\s+\w+\.Status|switch\s+\w+\.State|StatusPending|StatusActive|StatusDeleted|ErrInvalidTransition|InvalidTransition
```

## Entity Relationship Patterns

| Pattern | Purpose |
|---------|---------|
| `*_id` fields in structs / DB schemas | Foreign key reference |
| `REFERENCES` / `FOREIGN KEY` in migrations | DB-level FK constraint |
| `ON DELETE CASCADE` / `ON DELETE SET NULL` | Cascade behavior |
| `belongs_to` / `has_many` / ORM tags (`gorm:"foreignKey:"`) | ORM relationship |
| `JOIN` / `LEFT JOIN` in raw SQL | Cross-entity query |
| `tx.Create(&parent)` then `tx.Create(&child{ParentID: parent.ID})` | Create-order dependency |

### Grep String for Entity Relationship Search

```text
_id\b|REFERENCES|FOREIGN KEY|CASCADE|SET NULL|belongs_to|has_many|foreignKey:|JOIN\s|LEFT JOIN|\.ParentID|\.parent_id
```

## Async / Consistency Patterns

| Pattern | Purpose |
|---------|---------|
| `go func()` | Fire-and-forget goroutine |
| `kafka.Produce` / `publisher.Publish` / `nats.Publish` | Async event emission |
| `tx.Commit()` / `tx.Rollback()` | Transaction boundary |
| `errgroup.Group` / `sync.WaitGroup` | Concurrent operation coordination |
| `eventual consistency` / `sync` / `async` in comments | Consistency model hint |
| `SELECT ... FOR UPDATE` | Pessimistic lock |

### Grep String for Async/Consistency Search

```text
go func\(|kafka\.Produce|publisher\.Publish|nats\.Publish|tx\.Commit|tx\.Rollback|errgroup\.Group|sync\.WaitGroup|FOR UPDATE|eventual.consistency
```

### Event Publishing Patterns

| Pattern | Framework | Notes |
|---------|-----------|-------|
| `producer.Produce(` / `writer.WriteMessages(` | kafka-go / Sarama | topic in 1st arg |
| `publisher.Publish(` | watermill / custom bus | topic as string constant |
| `nats.Publish(` / `js.Publish(` | NATS / JetStream | subject as string |
| `rdb.Publish(ctx, channel,` | go-redis | channel name as 2nd arg |

### Grep String for Event Publishing Search

```text
producer\.Produce\(|writer\.WriteMessages\(|publisher\.Publish\(|nats\.Publish\(|js\.Publish\(|rdb\.Publish\(|\.Emit\(.*[Tt]opic|eventbus\.Publish\(
```

## Batch / Collection Patterns

| Pattern | Purpose |
|---------|---------|
| `BatchCreate` / `BatchUpdate` / `BatchDelete` | Bulk mutation operations |
| `for _, item := range items` near DB/API call | Iterative batch processing |
| `cursor` / `offset` / `limit` / `page_token` | Pagination parameters |
| `stream.Send(` / `stream.Recv(` | gRPC streaming batch |
| `BulkInsert` / `InsertMany` / `CopyFrom` | DB bulk insert |

### Grep String for Batch/Collection Search

```text
Batch(Create|Update|Delete)|BulkInsert|InsertMany|CopyFrom|for.*range.*items|cursor|offset|limit|page_token|stream\.Send\(|stream\.Recv\(
```

## Type Handling Patterns

| Pattern | Purpose |
|---------|---------|
| `strings.ToLower` / `strings.ToUpper` | Case normalization |
| `strconv.Atoi` / `strconv.ParseFloat` | String-to-number conversion |
| `json.Number` | Numeric JSON ambiguity handling |
| `UnmarshalJSON` / `MarshalJSON` | Custom JSON serialization |
| `time.Parse` / `time.Format` | Date/time format conversion |
| `uuid.Parse` / `uuid.New()` | UUID handling |

### Grep String for Type Handling Search

```text
strings\.ToLower|strings\.ToUpper|strconv\.Atoi|strconv\.Parse|json\.Number|UnmarshalJSON|MarshalJSON|time\.Parse|time\.Format|uuid\.Parse|uuid\.New
```
