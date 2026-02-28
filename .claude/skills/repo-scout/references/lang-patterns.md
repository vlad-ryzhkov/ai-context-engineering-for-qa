# Backend Patterns — Reference for /repo-scout

## Table of Contents

- [Language Detection](#language-detection)
- [Per-Language Pattern Files](#per-language-pattern-files)
- [Testing Libraries Detection](#testing-libraries-detection-all-languages)
- [Concurrency Model Detection](#concurrency-model-detection-all-languages)
- [Common Patterns](#common-patterns-all-languages)
- [gRPC Streaming Patterns](#grpc-streaming-patterns-l5-all-languages)
- [WebSocket & SSE Patterns](#websocket--sse-patterns-l6-all-languages)

---

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

## Per-Language Pattern Files

| Language | Reference File |
|----------|---------------|
| Go | `references/lang-go.md` |
| Python | `references/lang-python.md` |
| Node.js / TypeScript | `references/lang-nodejs.md` |
| Java / Kotlin | `references/lang-jvm.md` |

---

## Testing Libraries Detection (All Languages)

> Used by Phase 4.1 to identify test libraries beyond the primary framework.

### Grep by Build File

| Language | Build File(s) | Grep Pattern |
|----------|--------------|-------------|
| Go | `go.mod` | `testify\|mockery\|testcontainers\|dockertest\|gomock\|go-sqlmock` |
| Python | `requirements*.txt`, `pyproject.toml` | `pytest-cov\|factory.boy\|faker\|pytest-mock\|httpx\|pytest-asyncio` |
| JS/TS | `package.json` | `supertest\|nock\|@testing-library\|msw\|testcontainers\|sinon` |
| Java/Kotlin | `build.gradle.kts`, `build.gradle`, `pom.xml` | `mockk\|kotest\|testcontainers\|mockito\|assertj\|rest-assured` |

### Test File Naming Conventions

| Language | Unit Tests | Integration Tests |
|----------|-----------|------------------|
| Go | `*_test.go` | `*_test.go` + `//go:build integration` |
| Python | `test_*.py`, `*_test.py` | `test_*.py` + `@pytest.mark.integration` |
| JS/TS | `*.test.js`, `*.spec.js`, `*.test.ts`, `*.spec.ts` | `*.integration.spec.ts`, `*.e2e-spec.ts` |
| Java | `*Test.java`, `*Tests.java` | `*IT.java`, `*IntegrationTest.java` |
| Kotlin | `*Test.kt`, `*Tests.kt` | `*IT.kt`, `*IntegrationTest.kt` |

---

## Concurrency Model Detection (All Languages)

> Used by Phase 3.7.5 for detecting concurrency model and associated QA risks.

| Language | Model | Grep Pattern | QA Risk |
|----------|-------|-------------|---------|
| Go | goroutines + channels | `go func\|sync\.Mutex\|sync\.WaitGroup\|sync\.RWMutex\|<-chan\|chan<-` | goroutine leaks, race conditions, deadlocks |
| Python | asyncio / threading | `async def\|asyncio\.gather\|threading\.Lock\|ThreadPoolExecutor\|asyncio\.create_task` | GIL contention, event loop blocking |
| Node.js | event loop + promises | `Promise\.all\|Promise\.allSettled\|async/await\|new Worker\|worker_threads` | unhandled rejections, worker thread crashes |
| Kotlin | coroutines | `suspend fun\|launch\|async\s*\{\|withContext\|Dispatchers\.\|CoroutineScope` | structured concurrency violations, dispatcher misuse |
| Java | threads + virtual threads | `ExecutorService\|CompletableFuture\|synchronized\|ReentrantLock\|Thread\.startVirtualThread` | thread pool exhaustion, deadlocks |

### Grep String for Concurrency Detection

```text
go func\|sync\.Mutex|sync\.WaitGroup|async def|asyncio\.gather|threading\.Lock|Promise\.all|new Worker|suspend fun|launch\s*\{|async\s*\{|withContext|ExecutorService|CompletableFuture|synchronized
```

---

## Common Patterns (All Languages)

### Documentation Discovery Patterns (L-DOC)

> Used by Phase 3.0 (S-DOC) to find ALL existing documentation before handler analysis.

#### Machine-Readable API Specifications

| Glob | Format |
|------|--------|
| `**/swagger.json`, `**/swagger.yaml`, `**/swagger.yml` | Swagger 2.0 |
| `**/openapi.json`, `**/openapi.yaml`, `**/openapi.yml` | OpenAPI 3.x |
| `**/*.swagger.json` | gRPC-gateway generated |
| `**/*.proto` | Protocol Buffers |
| `**/*.graphql`, `**/schema.graphqls` | GraphQL schema |
| `**/*.http`, `**/api.http` | JetBrains HTTP Client |
| `**/postman_collection.json`, `**/*.postman_collection.json` | Postman |

#### Human-Readable Documentation

| Glob | Content Type |
|------|-------------|
| `docs/**/*.md` | General documentation |
| `**/README.md` | Project overview |
| `**/API.md`, `**/api-docs*` | API reference |
| `**/GUIDE.md`, `**/CONTRIBUTING.md` | Developer guides |
| `**/qa-checklist*` | QA test checklist |
| `**/qa-environment*` | Test environment setup |
| `**/testing*.md`, `**/TESTING.md` | Test documentation |
| `**/architecture*.md`, `**/design*.md` | Architecture docs |
| `**/ADR*.md`, `**/adr-*.md` | Architecture Decision Records |

#### Grep String for Documentation Search

```text
qa.checklist|qa.environment|api.doc|test.guide|test.setup|architecture|design.doc|ADR
```

### Proto Validate Tag Patterns (L1)

> Used by Phase 2.2 and 3.2 for extracting protobuf validation constraints.

| Library | Import / Option | Tag Patterns |
|---------|-----------------|-------------|
| **protoc-gen-validate (PGV)** | `import "validate/validate.proto"` | `[(validate.rules).string.min_len = N]`, `[(validate.rules).int64.gt = 0]`, `[(validate.rules).message.required = true]` |
| **buf validate (protovalidate)** | `import "buf/validate/validate.proto"` | `[(buf.validate.field).required = true]`, `[(buf.validate.field).string.min_len = N]` |
| **@gotags** | `// @gotags: validate:"required"` | Go struct tags injected via comments |
| **custom options** | `import "options.proto"` | Service-specific custom validation extensions |

#### Grep String for Proto Validation Search

```text
validate\.rules|buf\.validate|@gotags|validate:"required|validate:"min|validate:"max|option \(validate
```

### Read/Write Topology Patterns (L3)

> Used by Phase 3.6 step 8 (S5) for detecting master/replica configurations.

| Pattern | Purpose |
|---------|---------|
| `master` / `primary` / `leader` | Write-target DB/service identifier |
| `replica` / `secondary` / `follower` / `slave` | Read-target DB/service identifier |
| `readDB` / `writeDB` / `readConn` / `writeConn` | Separate DB connection handles |
| `ReadPreference` / `readPreference` | MongoDB read preference |
| `ReplicaMode` / `replica_mode` / `read_only` | Service replica mode flag |
| `MASTER_ONLY` / `REPLICA_SAFE` | Operation classification |
| `PermissionDenied` near write operations | Write blocked on replica |
| `binlog` / `replication_lag` / `sync_delay` | Replication mechanism indicators |

#### Grep String for Read/Write Topology Search

```text
master|primary|leader|replica|secondary|follower|readDB|writeDB|ReadPreference|ReplicaMode|replica_mode|read_only|MASTER_ONLY|REPLICA_SAFE|binlog|replication_lag
```

### Deployment Topology Patterns (L4)

> Used by Phase 6.6 (S6) for infrastructure discovery.

| Glob / Pattern | What It Finds |
|----------------|---------------|
| `**/Chart.yaml` | Helm chart definition |
| `**/values*.yaml` (in Helm dir) | Helm values per environment |
| `**/templates/*.yaml` (in Helm dir) | Helm K8s templates |
| `**/kustomization.yaml` / `.yml` | Kustomize overlay |
| `**/terraform/*.tf`, `**/main.tf` | Terraform infrastructure |
| `**/skaffold.yaml` | Skaffold dev config |
| `**/Tiltfile` | Tilt dev config |
| `**/docker-compose*.yaml` / `.yml` | Docker Compose variants |

#### Grep String for Deployment Topology Search

```text
Chart\.yaml|values.*\.yaml|kustomization|terraform|skaffold|Tiltfile|replicas:|resources:|limits:|requests:
```

### Specification Files

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

## gRPC Streaming Patterns (L5, All Languages)

> Used by Phase 2.2 for detecting streaming RPC types beyond Go.

### Go

| Pattern | Streaming Type |
|---------|---------------|
| `stream.Send(` / `stream.Recv(` | Server/Client streaming |
| `stream.SendAndClose(` | Client streaming (server response) |
| `stream.CloseAndRecv(` | Client streaming (client side) |

### Python

| Pattern | Streaming Type |
|---------|---------------|
| `yield` in service method | Server streaming |
| `async for request in request_iterator` | Client streaming |
| `grpc.stream_stream_rpc_method_handler` | Bidirectional |
| `stub.MethodName.future(` | Async unary |

### Node.js

| Pattern | Streaming Type |
|---------|---------------|
| `call.write(` / `call.end()` | Server streaming |
| `call.on('data'` / `call.on('end'` | Client streaming receive |
| `grpc.ServerWritableStream` | Server streaming type |
| `grpc.ServerDuplexStream` | Bidirectional type |

### Java / Kotlin

| Pattern | Streaming Type |
|---------|---------------|
| `StreamObserver<Response> responseObserver` | Server streaming |
| `StreamObserver<Request>` as return type | Client streaming |
| `@GrpcService` / `io.grpc.stub.StreamObserver` | gRPC service implementation |
| `stub.withDeadline(` / `stub.withCompression(` | Client configuration |

#### Grep String for Streaming Search (All Languages)

```text
stream\.Send\(|stream\.Recv\(|StreamObserver|ServerWritableStream|ServerDuplexStream|grpc\.stream|call\.write\(|call\.on\('data|request_iterator|yield.*response
```

---

## WebSocket & SSE Patterns (L6, All Languages)

> Used by Phase 2.6 to detect WebSocket and Server-Sent Events endpoints.
> These are NOT REST endpoints — exclude from REST count, mark as [WS] in §2.

### Go

| Pattern | Protocol | What It Finds |
|---------|----------|---------------|
| `websocket.Upgrade` / `ws.NewConn` | WS/WSS | gorilla/websocket or nhooyr.io/websocket upgrade |
| `gorilla/websocket` | WS/WSS | Import declaration |
| `websocket.Accept` | WS/WSS | nhooyr.io/websocket handler |
| `http.Flusher` | SSE | Chunked streaming for SSE |
| `text/event-stream` | SSE | Content-Type header for SSE response |

### Python

| Pattern | Protocol | What It Finds |
|---------|----------|---------------|
| `WebSocket` / `websockets.connect` | WS/WSS | websockets library |
| `@app.websocket` | WS/WSS | FastAPI WebSocket route decorator |
| `WebSocketResponse` | WS/WSS | aiohttp WebSocket handler |
| `EventSource` / `text/event-stream` | SSE | SSE endpoint detection |
| `sse_response` / `EventSourceResponse` | SSE | sse-starlette / aiohttp-sse |

### Node.js / TypeScript

| Pattern | Protocol | What It Finds |
|---------|----------|---------------|
| `socket.io` / `Socket` | WS/WSS | Socket.IO server |
| `ws.Server` / `new WebSocket.Server` | WS/WSS | ws library server |
| `wss.on('connection'` | WS/WSS | WebSocket connection handler |
| `EventSource` | SSE | SSE client-side (browser/node) |
| `res.write('data:` / `text/event-stream` | SSE | Manual SSE stream |
| `SseEmitter` | SSE | Spring-style SSE (NestJS) |

### Java / Kotlin

| Pattern | Protocol | What It Finds |
|---------|----------|---------------|
| `@ServerEndpoint` / `javax.websocket` | WS/WSS | JAX-RS WebSocket |
| `WebSocketHandler` / `TextWebSocketHandler` | WS/WSS | Spring WebSocket |
| `SseEmitter` | SSE | Spring SSE emitter |
| `ServerSentEvent` | SSE | Spring WebFlux SSE |
| `MediaType.TEXT_EVENT_STREAM` | SSE | WebFlux SSE content type |
| `@MessageMapping` | WS | Spring WebSocket message handler |

#### Grep String for WebSocket & SSE Search (All Languages)

```text
WebSocket|ws\.NewConn|websocket\.Upgrade|websocket\.Accept|gorilla/websocket|EventSource|text/event-stream|SseEmitter|http\.Flusher|socket\.io|ws\.Server|@ServerEndpoint|TextWebSocketHandler|ServerSentEvent|MediaType\.TEXT_EVENT_STREAM
```
