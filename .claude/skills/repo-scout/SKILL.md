---
name: repo-scout
description: Scans a backend repository (Go, Python, Node.js, Java/Kotlin), catalogs API surface, infrastructure, and test coverage. Use when entering a new repo before writing tests. Do not use for QA projects — use /init-project for those.
allowed-tools: "Read Glob Grep Bash(ls*) Bash(wc*)"
agent: agents/sdet.md
context: fork
---

# /repo-scout — Backend Repository Reconnaissance

<purpose>
Deep scanning of a backend repository → structured report on API surface, architecture,
infrastructure, and current test coverage. Gives AI and humans a complete picture of the service
before planning test coverage.
</purpose>

## Scope (SINGLE AUTHORITY)

**repo-scout is the ONLY skill responsible for initial repository discovery.**
- It ONLY reads and catalogs.
- It NEVER evaluates code quality, architecture patterns, or bugs.
- It NEVER generates test files or execution plans.

## When to Use

- First entry into a new backend repository
- Before `/test-cases` — for data collection
- Before `/init-project` — to understand the target service
- Periodic audit: "what changed in the API surface?"

## When NOT to Use

- QA projects with tests (use `/init-project`)
- Code review (use standard tools)
- Frontend/mobile repositories

## Input Data

- Path to repository (or current directory)
- Does not require CLAUDE.md, qa_agent.md or other AI files
- Can be the **first step** in a new repo

## Algorithm

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the artifact (MD/HTML), not into chat.

**Chat output (constraints):**
- Brief Summary: max 5 lines (what was found, how many, result)
- Findings table: max 15 lines (top by severity)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress for each file. Checkpoint only when:
- Phase transition (Phase N → Phase N+1)
- Warning detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, without "Now I will grep..."
- Read → analyze → report, without "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist), not a separate file.

**Phases 1-5:** Silent execution. **Phase 6:** Only Summary table + "Report: audit/repo-scout-report_{timestamp}.md" (timestamp format: `YYYYMMDD_HHMMSS`).

### Before Starting

Read `.claude/qa_agent.md` (if present in the working project). Output:

```text
📋 TASK BRIEF
├─ Target: {repo-name} — backend service reconnaissance
├─ Scope: API surface + infrastructure + test coverage
├─ Constraint: Read-only, backend patterns only (Go / Python / Node.js / Java/Kotlin)
└─ Action: Invoking /repo-scout...
```

### Phase 1: File System Scan

**Goal:** Determine language, build system, directory structure.

1. **Detect language** — check build files using the detection table in `references/lang-patterns.md`:
   ```text
   Glob: go.mod, package.json, pom.xml, build.gradle.kts, requirements.txt, pyproject.toml, Cargo.toml
   ```
   - Record detected language(s). If multiple → monorepo, note all.
   - If none → ⚠️ WARNING: No known build file found. Generic scan only.
   - All subsequent pattern lookups use `references/lang-patterns.md` section for detected language.

2. Read primary build file for metadata:
   If Go detected: extract from `go.mod`: module name, Go version, key dependencies.
   For other languages: read primary build file for equivalent metadata.

3. Determine structure:
   ```text
   Glob: cmd/*/main.go → list of services
   Glob: internal/*/ → business modules
   Glob: pkg/*/ → public packages
   ```

4. Count size:
   ```text
   Number of .go files (excluding *_test.go)
   Number of *_test.go files
   ```

### Phase 2: API Surface Discovery

**Goal:** Find and catalog ALL API endpoints.

#### 2.1 OpenAPI / Swagger

Search for files:
```text
Glob: **/swagger.json, **/swagger.yaml, **/openapi.json, **/openapi.yaml, **/*.swagger.json
```

For each found file:
- Read the file
- Extract endpoints: Method, Path, Description
- Note presence/absence of response schemas, error codes

#### 2.2 Protocol Buffers (gRPC)

Search for files:
```text
Glob: **/*.proto
```

For each .proto file:
- Extract services and rpc methods
- Record Request/Response types
- Note streaming methods (if any)

#### 2.3 Route Registration (from code)

Read `references/lang-patterns.md` → section for detected language → use that language's **Grep String for Route Search**.

For each found:
- File + line
- HTTP method + path
- Handler function

⚠️ **Do not duplicate:** If endpoint already found in swagger/proto — do not add from code.

#### 2.4 HTTP Client Files

```text
Glob: **/*.http, **/api.http
```

If found — note as an additional source of examples.

#### 2.5 Risk Classification

Flag **Sensitive Endpoints** (auth, login, token, billing, pay, wallet, users, profile, export) in the report — SDET will apply `@Severity(CRITICAL)` patterns.

### Phase 3: Test Analysis

**Goal:** Assess current test coverage.

1. Find all test files:
   Read `references/lang-patterns.md` → section for detected language → use that language's **Test Patterns** for file glob and classification rules.

2. Classify by type:
   Classify per `lang-patterns.md` → Test Patterns table for detected language.

3. Determine test frameworks:
   Determine test frameworks: read `lang-patterns.md` → Test Frameworks table for detected language → search in primary build file.

4. Check for external test repositories:
   - Search README for links to `indrive-api-tests-*` or `*-tests`
   - Check `.dev-platform/` for test runner configurations

### Phase 4: Infrastructure Scan

**Goal:** Understand the infrastructure context.

#### 4.1 CI/CD

```text
Glob: .github/workflows/*.yml, .gitlab-ci.yml, Jenkinsfile
```

#### 4.2 Docker

```text
Glob: **/Dockerfile, **/docker-compose.yaml, **/docker-compose.yml
```

#### 4.3 Database

```text
Glob: migrations/**, **/migrations/**, **/liquibase/**
```

#### 4.4 Configuration

```text
Glob: config/*.yaml, config/*.yml
```

#### 4.5 Dev-Platform

```text
Glob: .dev-platform/**
```

#### 4.6 Security & Secrets Baseline

1. **Secrets:** `ls -la .env .env.*`, grep for RSA/OPENSSH keys, api_key/secret_key
2. **Dependencies:** `govulncheck ./...`
3. **SQL Injection:** grep for unsafe `fmt.Sprintf` in SQL statements

### Phase 5: AI Setup Status

Check for AI files:
```text
- CLAUDE.md
- .claude/qa_agent.md
- .claude/skills/*/SKILL.md
- .agents/skills/*/SKILL.md
- AGENTS.md
- .cursor/rules/*.mdc
- .github/copilot-instructions.md
```

### Phase 6: Report Generation

Compile the report and save to `audit/repo-scout-report_{timestamp}.md` (timestamp format: `YYYYMMDD_HHMMSS`). Full report template with examples — in `references/report-template.md`.

**Required sections:**
1. Repository Profile (module, Go version, service type, dependencies)
2. API Surface Catalog (REST + gRPC endpoints with Summary)
3. Specification Inventory (coverage formula)
4. Existing Test Coverage (unit/integration/e2e)
5. Infrastructure (CI/CD, Docker, DB, Migrations, Queue, Cache)
6. AI Setup Status (CLAUDE.md, qa_agent.md, skills)
7. Readiness Assessment (specs, tests, docs, AI setup + blockers + next step)

## Quality Gates

- [ ] go.mod found and parsed
- [ ] All proto files read and RPCs cataloged
- [ ] All swagger/openapi files read and endpoints cataloged
- [ ] Endpoint counts are correct (formula shown)
- [ ] No placeholders `{xxx}` in the final report (except "none")
- [ ] Readiness Assessment filled for all 4 criteria

## Self-Check

Before saving the report, verify:

- [ ] **Completeness:** All 7 sections filled?
- [ ] **Accuracy:** Endpoint counts match between sections 2 and 3?
- [ ] **No Hallucinations:** Each endpoint actually found in a file (source specified)?
- [ ] **Readiness:** Assessment is backed by data from sections 2-5?

## Completion

After saving `audit/repo-scout-report_{timestamp}.md` — print `SKILL COMPLETE` block (format in qa_agent.md § Skill Completion Protocol).

Self-Review for this skill **is not generated** (read-only scanning, not content generation).

```text
✅ SKILL COMPLETE: /repo-scout
├─ Artifacts: audit/repo-scout-report_{timestamp}.md — **Each invocation creates a new timestamped file**
├─ Self-Review: N/A (scanning)
├─ Compilation: N/A
├─ Upstream: none
└─ Endpoints: {N REST} + {M gRPC} = {total}
```

## Related Files

- Go patterns: `references/lang-patterns.md`
- Next step: `/test-cases` (uses repo-scout-report.md as input)
- AI files: `/init-project` → CLAUDE.md, `/init-agent` → qa_agent.md
