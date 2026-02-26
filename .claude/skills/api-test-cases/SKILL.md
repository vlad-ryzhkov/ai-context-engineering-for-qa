---
name: api-test-cases
description: Generates exhaustive test scenario matrices for ALL API endpoints grouped by domain. Use for full regression coverage across entire API surface. Do not use for single-endpoint deep-dive — use /api-isolated-tests.
allowed-tools: "Read Write Edit Glob Grep AskUserQuestion"
agent: agents/sdet.md
context: fork
---

## Recommended Flow

`/repo-scout` → `/spec-audit` → `/api-test-cases` → `/api-tests`

For single-endpoint deep-dive use `/api-isolated-tests` instead.

## Input Context (Process Isolation)

`context: fork` — you cannot see chat history before your invocation.
**Allowed inputs:** Files explicitly read via tool + `$ARGUMENTS` path. **Forbidden:** Assumptions from "previous agent context", inventing spec content.

## System Requirements

Before execution the agent MUST load (via `Read` tool):
1. `.claude/protocols/gardener.md`
2. `.claude/skills/api-test-cases/references/coverage-matrix.md`
3. `.claude/skills/api-test-cases/references/quality-gates.md`
4. `.claude/skills/api-test-cases/references/output-template.md`

---

## EXCLUDED_SCENARIOS (TOP-LEVEL IMPERATIVE)

> **Non-negotiable.** Read this section BEFORE generating any domain batch. Re-read BEFORE each subsequent batch (anti-degradation reinforcement).

Parse every specification for exclusion directives at two levels:

**Level 1 — Entire type (`EXCLUDED_TYPES`):**
Markers: `no security testing`, `BVA not required`, `skip NEG`, `SEC: out of scope`.
- `EXCLUDED_TYPES = [SEC, BVA, ...]` — entire type skipped for all endpoints.

**Level 2 — Specific scenarios (`EXCLUDED_SCENARIOS`):**
Markers: `handled by ORM`, `delegated to Middleware`, `covered by library (Zod/Pydantic)`, `not tested`, `N/A`.
- Examples:
  - `SEC:injection` — SQLi, XSS, SSTI (ORM protects)
  - `NEG:missing_field` — keep **one** NEG per error type, not per field
  - `BVA:{field}` — BVA for specific field delegated to Middleware
  - `POS:encoding_variants` — extra Happy Path with Unicode/hyphens

Full ownership → EXCLUDED_SCENARIOS mapping: see `references/coverage-matrix.md` § Spec Exclusions Parsing.

If no exclusions found → `EXCLUDED_TYPES = []`, `EXCLUDED_SCENARIOS = []`, apply full Coverage Matrix.

---

## Input Strategy (Auto-Discovery)

1. **Primary:** `audit/repo-scout-report.md` — extract endpoint catalog from API Surface section.
2. **Fallback:** Scan `specifications/` directory for `*.md`, `*.yaml`, `*.json`, `*.proto` files.
3. **Direct:** `$ARGUMENTS` path or user-provided file path.
4. **Enrichment:** `audit/spec-audit-report.md` (if exists) — incorporate findings.
5. **If no specs found** → `⚠️ WARNING: No specifications found. Provide a path or run /repo-scout first.` → STOP.

## Mode

`mode: api-integration` (DEFAULT) — applies DEFAULT_HEURISTICS to reduce unit-level noise.
`mode: full-matrix` — disables all heuristics. Trigger when arguments contain `full-matrix`, `full`, or `exhaustive`.

---

## Algorithm

### Phase 1: Scout — Endpoint Catalog

1. Parse `audit/repo-scout-report.md` (via `Read` tool) to extract exact file paths of all API specifications from the API Surface section. If the report is missing, fallback to scanning `specifications/` directory via `Glob` for `*.md`, `*.yaml`, `*.json`, `*.proto`.
2. Read each discovered specification file via `Read` tool.
3. Extract every endpoint: `{METHOD} {path}` + description + request/response schema.
4. Compile a flat endpoint list with source file reference.

### Phase 2: Map — Domain Grouping & Risk Classification

1. **Group by domain/resource tag** (Auth, Users, Orders, Payments, etc.):
   - Parse URL path segments: `/api/v1/{domain}/...`
   - Use spec section headers or OpenAPI tags as grouping hints.
   - Ungrouped endpoints → domain `_misc`.

2. **Classify risk** per endpoint:
   - **[CRITICAL]** — Money (payment, refund, balance), Security (auth, tokens, permissions, password reset), Data Integrity (user deletion, account merge).
   - **[HIGH]** — Core business logic (order creation, status transitions, matching).
   - **[MEDIUM]** — Dictionaries, settings, read-only endpoints.

   **Conflict resolution:** If an endpoint touches multiple risk domains, assign the **highest** applicable tier.

3. **Batch planning:**
   - Max **10 endpoints per batch**.
   - Domains with >10 endpoints → split into sub-batches: `{domain}_part1`, `{domain}_part2`.

4. **Output domain summary table** (to chat):

```
📊 API Surface Map
| # | Domain | Endpoints | Risk | Spec Source | Batch |
|---|--------|-----------|------|-------------|-------|
| 1 | auth   | 4         | CRITICAL | specifications/auth_api.md | 1 |
| 2 | users  | 12        | HIGH | specifications/users_api.md | 2 (split: 2a=6, 2b=6) |
```

### Phase 3: Scope Confirmation

Ask user via `AskUserQuestion`:
- **Option 1:** "All domains" (Recommended) — generate for every domain in order.
- **Option 2:** "Select domains" — user picks specific domains by number.
- **Option 3:** "Critical only" — generate only for CRITICAL + HIGH risk domains.

Wait for user response before proceeding. **Non-interactive fallback:** If `AskUserQuestion` is unavailable or no response is received, default to Option 1 (All domains) and proceed.

### Phase 4: Chunked Generation (Per-Domain Loop)

```
FOR each selected domain batch:
  1. Output active exclusions as internal reasoning step:
     "Active EXCLUDED_TYPES: [...], Active EXCLUDED_SCENARIOS: [...]"
     (silent — not to chat, forces context reinforcement before each batch)
  2. Load specification for this domain's endpoints
  3. Parse EXCLUDED_TYPES and EXCLUDED_SCENARIOS from the spec (merge with active set)
  4. Apply coverage matrix (→ references/coverage-matrix.md):
     - CRITICAL: 7 dimensions + expanded NEG (race conditions, double execution, partial failure)
     - HIGH: 7 dimensions (POS/NEG/BVA/SEC/L10N/IDEM/HEADERS)
     - MEDIUM: POS + key NEG + SEC + HEADERS only (skip BVA/L10N/IDEM)
  5. Apply DEFAULT_HEURISTICS (DH-01..DH-05) as **generation constraints** (prevention-first: do NOT generate unit-level scenarios, apply heuristics DURING generation, not as post-filters). Skip when `mode: full-matrix`.
  6. Execute Scope Purge Pass (→ references/quality-gates.md)
  7. Run Quality Gates checklist for this domain
  8. Save to docs/api-test-cases/{domain}_test-scenarios_{timestamp}.md
     (use output template → references/output-template.md)
  9. Output progress: "✅ {Domain}: {N} scenarios (POS:{X}/NEG:{Y}/BVA:{Z}/SEC:{W}/L10N:{V}/IDEM:{U}/HEADERS:{H})"
END FOR
```

**Edge case:** If a domain has 0 endpoints after `EXCLUDED_SCENARIOS` filtering → skip domain, log in summary: `⏭️ {Domain}: 0 endpoints after exclusions — skipped`.

**MEDIUM risk scope reduction:**
For MEDIUM-risk endpoints, generate ONLY:
- POS (Happy Path min + max data)
- NEG (1 representative per validation class: missing, null, wrong-type, invalid-value)
- SEC (No Token, Invalid Token)
- HEADERS (Content-Type verification)

Skip: BVA, L10N, IDEM. Log skipped dimensions in Scope Reduction Log with reason: `MEDIUM risk — reduced scope per risk tier`.

### Phase 5: Cross-Domain Analysis

After all domain batches complete:
1. **Dependency map:** Identify shared entities across domains (auth tokens used by other domains, user IDs referenced in orders, etc.).
2. **Integration points:** List endpoints that must be called in sequence (e.g., create user → create order for user).
3. **Shared prerequisites:** Data setup required across multiple domains.

### Phase 6: Summary Report

Generate `docs/api-test-cases/summary_{timestamp}.md`:

```markdown
# API Test Cases — Cross-Domain Summary
> Generated: {YYYY-MM-DD HH:MM:SS}
> Mode: {api-integration|full-matrix}
> Specs analyzed: {count}
> Skill: /api-test-cases

## Coverage Statistics
| Domain | Endpoints | Scenarios | POS | NEG | BVA | SEC | L10N | IDEM | HEADERS | Risk |
|--------|-----------|-----------|-----|-----|-----|-----|------|------|---------|------|

**Total: {N} scenarios across {M} endpoints in {D} domains**

## Cross-Domain Dependencies
[from Phase 5]

## Excluded Scope
- EXCLUDED_TYPES: {list}
- EXCLUDED_SCENARIOS: {count} patterns applied
- MEDIUM risk reductions: {count} dimensions skipped

## Aggregated Scope Reduction
| Domain | Removed Count | Top Reasons |
|--------|---------------|-------------|
| {domain} | {N} | {top 2-3 reasons} |
| **Total** | **{sum}** | — |

## Spec Ambiguities
[aggregated from all domains]
```

---

## Verbosity Protocol

**SILENT MODE:** All scenario content goes to files, not to chat.

**Chat output (allowed):**
- Phase 2 domain summary table — MUST output
- Phase 3 scope confirmation question — MUST output
- Phase 4 per-domain progress line — MUST output
- Phase 6 summary statistics — MUST output

**Chat output (forbidden):**
- Individual scenario tables
- Intermediate generation progress
- File content echoing

---

## Quality Gates

> Full rules: `references/quality-gates.md`

**Per-domain gates (Phase 4, step 7):**
- Zero scenarios from `EXCLUDED_SCENARIOS` or `EXCLUDED_TYPES` remain
- Scope Reduction Log present and accounts for every deletion
- Every endpoint has minimum coverage per risk tier:
  - CRITICAL/HIGH: 1 POS + 1 NEG per validation class + 1 BVA + 1 SEC + 1 HEADERS; POST/PUT: 1 IDEM
  - MEDIUM: 1 POS + 1 NEG per validation class + 1 SEC + 1 HEADERS
- CRITICAL endpoints have expanded NEG (race conditions, double execution)
- No hardcoded data (email, phone, name) — placeholders only
- All Expected Results are specific (HTTP code + business logic + body.code for NEG)
- POS scenarios contain Contract Match + Cleanup
- Mode declaration present in file header

**Cross-domain gates (Phase 6):**
- All selected domains have generated files
- Summary file exists with aggregated statistics
- Cross-domain dependency map present

---

## Completion Contract

```text
✅ SKILL COMPLETE: /api-test-cases
├─ Artifacts: docs/api-test-cases/{domain}_test-scenarios_{ts}.md (×{D} domains) + summary_{ts}.md
├─ Coverage: {M}/{T} endpoints ({P}%)
├─ Scenarios: {N} total (POS:{X}, NEG:{Y}, BVA:{Z}, SEC:{W}, L10N:{V}, IDEM:{U}, HEADERS:{H})
├─ Domains: {D} generated, {S} skipped
├─ Excluded: EXCLUDED_TYPES={list}, EXCLUDED_SCENARIOS={count} patterns
└─ Ready for: /api-tests (Implementation)
```

```text
⚠️ SKILL PARTIAL: /api-test-cases
├─ Artifacts: [list (✅/❌ per domain)]
├─ Coverage: {M}/{T} endpoints ({P}%)
├─ Scenarios: {N} total
├─ Domains: {D} generated, {F} failed, {S} skipped
├─ Blockers: [description]
└─ Ready for: /api-tests (partial — missing domains: {list})
```
