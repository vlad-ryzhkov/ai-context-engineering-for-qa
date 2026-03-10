# CLAUDE.md — Minimal Project Template

> **Purpose:** Minimal context for the AI: tech stack, commands, banned alternatives.
> Do NOT add codebase overviews, directory listings, or architecture descriptions.
> Research: [arxiv.org/abs/2602.11988](https://arxiv.org/abs/2602.11988)

---

## Template

````markdown
# [Project Name]

## Context
- **Project:** [What we test/develop]
- **Role:** Senior QA Automation Engineer
- **Language:** [Kotlin/Python/TypeScript]

## Tech Stack (LOCKED)

| Component | Technology | BANNED |
|-----------|------------|--------|
| HTTP Client | [common-test-libs ApiClient/requests/axios] | [alternatives] |
| Serialization | [Jackson/Pydantic/zod] | [alternatives] |
| Assertions | [assertEquals with message/pytest/Jest] | [alternatives] |
| Test Framework | [JUnit 5/pytest/Jest] | [alternatives] |
| Reporting | [Allure] | — |

## Commands

| Action | Command |
|--------|---------|
| Build | `[command]` |
| Test | `[command]` |
| Single test | `[command]` |

<!-- SECTION: Key Values — ONLY if non-trivial defaults exist in values.yaml / application.yml / .env -->
## Key Values

### [Subsection by component]

- `[key]` — [what it does, why it matters]

<!-- SECTION: API Documentation — only if swagger/proto/graphql/.http/postman files found -->
## API Documentation

| Type | Path |
|------|------|
| [OpenAPI / gRPC / GraphQL / HTTP / Postman] | `[relative/path/to/file]` |
````

---

## Tech Stack by Languages

### Kotlin

```text
| Component | Technology | BANNED |
|-----------|------------|--------|
| HTTP | common-test-libs ApiClient + ApiRequestBaseJson<T> | Custom HTTP wrappers |
| JSON | Jackson (SNAKE_CASE) | Gson |
| Assertions | assertEquals with message + Hamcrest checkAll | Assertions without message |
| Polling | Awaitility (await.atMost().until {}) | Thread.sleep(), delay() |
| Framework | JUnit 5 | TestNG |
| Code Style | ktlint | — |
```

### Python

```text
| Component | Technology | BANNED |
|-----------|------------|--------|
| HTTP | httpx / requests | urllib |
| JSON | Pydantic | manual parsing |
| Assertions | pytest assert | unittest |
| Framework | pytest | nose |
```

### TypeScript

```text
| Component | Technology | BANNED |
|-----------|------------|--------|
| HTTP | axios / fetch | request |
| Validation | zod | manual |
| Assertions | Jest expect | chai |
| Framework | Jest / Vitest | Mocha |
```

### Infrastructure (Helm / Kubernetes / Terraform)

```text
| Component | Technology | BANNED |
|-----------|------------|--------|
| Package Manager | Helm 3 | Kustomize, raw kubectl apply |
| Service Mesh | Istio / Linkerd | — |
| IaC | Terraform | manual kubectl |
| Registry | OCI (Harbor) | Docker Hub (prod) |
| Lint | helm lint . / tflint | — |
| CI/CD | GitHub Actions / GitLab CI | — |
```

---

## File Location

```text
project-root/
└── CLAUDE.md    # In the project root
```

---

