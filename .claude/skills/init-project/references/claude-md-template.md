# CLAUDE.md — Project Onboarding Template

> **Purpose:** Project wiki for the AI. First day for a new employee — what language, where are configs, how we format code.

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

## Project Structure

```text
[Actual project structure]
```

## Commands

| Action | Command |
|--------|---------|
| Build | `[command]` |
| Test | `[command]` |
| Single test | `[command]` |

## Safety Protocols

⛔ **FORBIDDEN:** `git reset --hard`, `git clean -fd`, branch deletion
✅ **MANDATORY:** Backup before destructive operations
⚠️ **OVERRIDE:** Requires the word **DESTROY**

## Token Economy

- PAUSE on tasks > 20,000 tokens
- Full scan only with **FULL_SCAN**

## Workflow

For tasks > 3 files: Analysis → Plan → Execute → Verify

<!-- SECTION: Architecture — only for infra/backend projects (no src/test/) -->
## Architecture

[Narrative about key design decisions: components, interaction schema, non-trivial configurations]

<!-- SECTION: Key Values — if non-trivial defaults exist in values.yaml / application.yml / .env -->
## Key Values

### [Subsection by component]

- `[key]` — [what it does, why it matters]

<!-- SECTION: CI/CD Flow — if .github/workflows/, .gitlab-ci.yml, Jenkinsfile found -->
## CI/CD Flow

```text
[pipeline diagram: step → step → step]
```

<!-- SECTION: QA Skills — only if .claude/skills/ exists in the project -->
## QA Skills

| Skill | Purpose |
|-------|---------|
| `/spec-audit` | QA audit of requirements |
| `/api-isolated-tests` | Test cases from specification |
| `/api-tests` | API automated tests |

**Workflow:** `/spec-audit` → `/api-isolated-tests` → `/api-tests`
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

## Full Guide

`docs/ai-files-handbook.md` → Part 1: CLAUDE.md
