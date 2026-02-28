---
name: api-mocks
description: Generates in-process HTTP mock server for the API under test + WireMock singletons for external services. Use when tests fail with ConnectException or no live server is available.
agent: agents/sdet.md
input: specification file (same path used for /api-tests)
output: helpers/MockServer.kt, helpers/MockServerExtension.kt, META-INF/services/, junit-platform.properties
---

## When to Use
- Tests fail with `ConnectException` / `Connection refused`
- No live API server available (CI, local dev without backend)
- Starting a new endpoint — generate mock before writing tests

**Not for:** mocking only external services (put WireMock stubs directly in `@BeforeEach`).
**Workflow:** `/api-mocks` → retry `/api-tests` (fallback when no live server is available).

## Protocol

**What to generate (always in this order):**

**1. `helpers/{ExternalService}MockServer.kt`** — WireMock singleton (only if spec mentions external services like SMS, payments, etc.)
- `object` singleton, dynamic port, `start()` idempotent, `stop()`, `port()`
- `stub{Service}Success()` / `stub{Service}Unavailable()` / `resetAll()`
- Path: `POST /{service-path}` from spec

**2. `helpers/{Endpoint}MockServer.kt`** — JDK `com.sun.net.httpserver.HttpServer`
- `class` (not object) — per-class lifecycle; `start()` sets `BASE_URL` system property, `stop()` clears it
- Extract from spec: required fields → validate presence, type, format; business rules (length, regex, uniqueness, password complexity)
- `ConcurrentHashMap` sets for uniqueness tracking (case-insensitive for email)
- `ConcurrentHashMap` for idempotency cache: same key + same body hash → cached response; same key + different body → 400 `IDEMPOTENCY_KEY_MISMATCH`
- Validation error responses MUST include `"field":"<field_name>"`
- Conflict responses MUST include `"field":"<field_name>"`
- If spec defines token-type response field (JWT): generate `header.payload.mock-sig` with claims extracted from spec (`alg`, `sub`, `aud`, `exp = now+TTL`) — use `Base64.getUrlEncoder().withoutPadding()`
- External service call: `if (System.getProperty("{SERVICE}_GATEWAY_URL") != null) call(url+"/path")` → on failure return 503
- Security headers on all responses: `Content-Type: application/json; charset=utf-8`, `X-Content-Type-Options: nosniff`, `Strict-Transport-Security: max-age=31536000; includeSubDomains`

**3. `helpers/MockServerExtension.kt`** — `BeforeAllCallback` + `AfterAllCallback`
- `beforeAll`: start external WireMock singleton → `System.setProperty("{SERVICE}_GATEWAY_URL", "http://localhost:${ExternalMock.port()}")` → `stub{Service}Success()` (default) → create+start `{Endpoint}MockServer` → store in `ExtensionContext.Store`
- `afterAll`: remove+stop `{Endpoint}MockServer` from store → `ExternalMock.resetAll()`

**4. `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`**
- Single line: `{package}.helpers.MockServerExtension`

**5. `src/test/resources/junit-platform.properties`**
- Add `junit.jupiter.extensions.autodetection.enabled=true` (preserve existing properties)

## BASE_URL Rule (CRITICAL)
API client `BASE_URL` MUST be a computed property — read on every call, not once at object init:
```kotlin
val BASE_URL: String get() = System.getProperty("BASE_URL", "http://localhost:8080")
```
A static `val` captures the port at class-load time and breaks when the second test class starts a new server on a different port.

## Known Limitation
TLS-enforcement tests (expect `plain HTTP → 301/400/426`) **cannot pass** with an HTTP mock.
Leave them as-is — they will fail at infra level. Document in Smoke Run output: `INFRA (TLS-enforcement test)`.

## Workflow
1. Read spec → extract: endpoint path, HTTP method, required fields + types, validation rules, external service dependencies, response token format
2. Generate files (order: external mock → API mock → extension → META-INF → properties)
3. `./gradlew compileTestKotlin`
4. `./gradlew test 2>&1 | tail -30` — zero `ConnectException` = PASS

## Completion Contract
```
✅ SKILL COMPLETE: /api-mocks
├─ Artifacts: helpers/{Name}MockServer.kt + MockServerExtension.kt + META-INF/services/ + junit-platform.properties
├─ Compilation: PASS
└─ Smoke Run: PASS (zero ConnectException) | INFRA (TLS-enforcement test only)
```
