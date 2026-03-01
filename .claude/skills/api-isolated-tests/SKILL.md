---
name: api-isolated-tests
description: Generates an exhaustive test scenario matrix (Markdown) directly from API specifications. Use when you need full regression coverage, find edge cases, or prepare a strict spec for automated tests. Do not use for generating automated test code — use /api-tests for that.
allowed-tools: "Read Write Edit Glob Grep"
agent: agents/sdet.md
context: fork
---

## Recommended Flow

For best results, run `/spec-audit` first — its audit output enriches scenario generation. Running directly on a specification also works.

## Input Context (Process Isolation)

`context: fork` — you cannot see chat history before your invocation.
**Allowed inputs:** Files explicitly read via tool + `$ARGUMENTS` path. **Forbidden:** Assumptions from "previous agent context", inventing spec content.

## 🔒 SYSTEM REQUIREMENTS

Before execution the agent MUST load: `.claude/protocols/gardener.md`

# Test Scenario Designer (Full Coverage)

## Input Strategy (Auto-Discovery)

1. **Source:** Search for API specifications in the repository:
   - `$ARGUMENTS` — if a path is substituted here (Claude Code CLI) → read the file with the `Read` tool.
   - User message — if it contains a file path (`.md`, `.yaml`, `.json`, `.txt`) → read it with the `Read` tool. (Cursor and other environments where `$ARGUMENTS` is not substituted.)
   - `*.yaml`, `*.json` (Swagger/OpenAPI)
   - `*.proto` (gRPC)
   - `audit/spec-audit-report.md` (if exists)
   - **Ignore** `audit/test-plan.md` (work directly with the specification).
   - **If no spec found** → output to chat: `⚠️ WARNING: No specification found. Please provide a file path or run /spec-audit first.` and stop.

2. **Mode:** `mode: api-integration` (DEFAULT) — applies DEFAULT_HEURISTICS to reduce unit-level noise. `mode: full-matrix` — disables all heuristics, generates exhaustive per-field coverage. Trigger `full-matrix` when arguments contain `full-matrix`, `full`, or `exhaustive`. Mode is declared in the output file header: `> Mode: api-integration` or `> Mode: full-matrix`.

3. **Scope:** **ALL ENDPOINTS (100% Coverage).**
   - Do not filter by importance. Test everything: Auth, Business Logic, Dictionaries, Settings.
   - Every discovered method (GET/POST/PUT/DELETE) MUST have a set of scenarios.

## Verbosity Protocol

**SILENT MODE:** All content goes to `docs/api-isolated-tests/test-scenarios.md`, not to chat.

**Chat output (restrictions):**
- Endpoint progress — MUST NOT output
- Intermediate tables — MUST NOT output
- Chat: only SKILL COMPLETE block + `📊 Artifact: docs/api-isolated-tests/test-scenarios.md`

---

## Protocol

1. **Format:** Markdown Table (Strict Structure). **No code (Kotlin/Java).**
2. **Data Strategy:** Abstract placeholders (`{UNIQUE_EMAIL}`, `{UUID}`, `{MAX_INT+1}`, `{PAST_DATE}`).
3. **Spec Exclusions (PRIORITY — read BEFORE generation):**
   Before generation explicitly search the specification for exclusion directives at two levels:

   **Level 1 — entire type (`EXCLUDED_TYPES`):**
   Markers for an entire type: `no security testing`, `BVA not required`, `skip NEG`, `SEC: out of scope`.
   - `EXCLUDED_TYPES = [SEC, BVA, ...]` — the entire type is skipped for all endpoints.

   **Level 2 — specific scenarios (`EXCLUDED_SCENARIOS`):**
   Markers for specific checks: `handled by ORM`, `delegated to Middleware`, `covered by library (Zod/Pydantic)`, `not tested`, `N/A`, explicit examples of cases the spec puts out of scope.
   - `EXCLUDED_SCENARIOS` — list of scenario patterns not to generate. Examples:
     - `SEC:injection` — SQLi, XSS, SSTI (ORM protects, infrastructure coverage)
     - `NEG:missing_field` — duplicate "missing field" if validator (Zod/Pydantic) handles them uniformly → keep **one** NEG per error type, not per field
     - `BVA:{field}` — BVA for a specific field (e.g. `BVA:password`), if length is delegated to Middleware
     - `POS:encoding_variants` — extra Happy Path with Unicode/hyphens on top of basic POS if standard validation library covers them

   **Ownership → EXCLUDED_SCENARIOS mapping (parse spec's exclusion table row by row):**
   | Spec ownership marker | Add to EXCLUDED_SCENARIOS |
   |---|---|
   | `delegated to Middleware (Zod/Pydantic)` for field X | `NEG:format_X`, `BVA:X` — only `NEG:missing_X` survives |
   | `covered by library` for feature Y | `NEG:Y_detail` — keep one representative NEG (any input = error) only |
   | `Unit tests of shared-library` | `NEG:{feature}_combinations` — keep only one POS + one basic NEG |
   | `"Field presence only: missing → 400"` | All NEG for that field **except** `NEG:missing_{field}` |
   | `DB level` (BVA) | `BVA:{field}` for all mentioned fields |
   | `L10N: out of scope` | `L10N:*` for all text fields |

   - If no exclusions found — `EXCLUDED_TYPES = []`, `EXCLUDED_SCENARIOS = []`, apply the full Coverage Matrix.
3a. **API Integration Test Focus (DEFAULT — `mode: api-integration`):**
   **Prevention-first:** Do NOT generate unit-level scenarios — apply heuristics as generation constraints, not post-generation filters. Skipped when `mode: full-matrix`.
   **Testing Level Classification:**
   | API-level (ALWAYS generate) | Unit-level (1 representative) |
   |---|---|
   | Contract: status codes, response schema, headers | Same validation × N fields, same error code |
   | Business logic: state transitions, side effects, named rules | M sub-rules of one regex/validator |
   | Cross-field / state-dependent validation | BVA for infrastructure fields (no business rule) |
   | Auth/authz (SEC), Idempotency (IDEM), L10N round-trip | Whitespace/spacing/format variants of same check |
   | Error code routing (distinct codes = distinct scenarios) | NEG duplicating a BVA boundary |
   Principle: 1 representative proves the validator is wired; proving it for field B after A — unit-test scope.
   **Precedence:** Spec markers (`EXCLUDED_SCENARIOS`) > DEFAULT_HEURISTICS > `mode: full-matrix` override.

   **DEFAULT_HEURISTICS:**
   | ID | Heuristic | Action |
   |---|---|---|
   | DH-01 | Repeating Validation Pattern — null/empty/wrong-type across N fields, same error code | Keep 1 representative field per check type (e.g., 1 for missing, 1 for null, 1 for wrong-type — not per-field) |
   | DH-02 | Complexity Rule Combinatorics — M sub-rules for one field | Keep 1 representative NEG |
   | DH-03 | Infrastructure BVA — schema-only length, no named business rule | Keep 1 BVA pair for 1 representative field |
   | DH-04 | Format/Whitespace Consolidation — 3+ regex/spacing variants for same field | Keep 1 representative |
   | DH-05 | Length Boundary Deduplication — NEG row = same boundary as BVA row | Keep BVA row, remove NEG duplicate |

   **Exceptions (heuristic does NOT reduce):**
   - Scenario tests an explicitly named business rule from spec (e.g., "Apostrophes are not allowed")
   - Scenario produces a different error code / HTTP status from others in its group
   - Scenario tests state-dependent validation
   - Endpoint is `[CRITICAL]` + scenario tests security/data-integrity
   **API Coverage Guarantee (heuristics NEVER reduce):** All POS, SEC, IDEM, L10N, HEADERS; scenarios with distinct error code/HTTP status; explicitly named business rules; state-dependent and cross-field validation; `[CRITICAL]` security/data-integrity; ≥1 NEG per validation class; ≥1 BVA pair for field with business rule.
4. **Risk-Based Prioritization:**
   Before generating scenarios, assess the business risk of each endpoint:
   - **[CRITICAL]** — Money (payment, refund, discount, balance), Security (auth, tokens, permissions, password reset), Data Integrity (user deletion, account merge, data migration). For `[CRITICAL]` endpoints generate **expanded NEG scenarios**: race conditions (`concurrent {ACTION} with same {ID}`), double execution (`duplicate {PAYMENT_ID} within 1s`), partial failure (`timeout after debit before credit`).
   - **[HIGH]** — Core business logic (order creation, status transitions, matching). Standard NEG set.
   - **[MEDIUM]** — Dictionaries, settings, read-only endpoints. Standard NEG set.
   Tag each endpoint in the feature header: `## Feature: [Create Payment] (POST /payments) [CRITICAL]`

5. **Coverage Matrix (The Grid):**
   For **EVERY** endpoint apply verification layers **EXCEPT types from `EXCLUDED_TYPES`** and **EXCEPT patterns from `EXCLUDED_SCENARIOS`**:
   - **POS (Positive):** Happy Path (Min data & Max data).
   - **NEG (Negative):** Validation (Body & Path params: Null, Empty, Wrong Type, Malformed JSON), State Conflicts (Action on PENDING/BLOCKED entity). **One Representative Rule (`mode: api-integration`):** for validation checks repeating across fields with same error code, keep 1 representative field per check type. Full per-field NEG only when spec requires per-field error reporting verification or `mode: full-matrix`.
   - **BVA (Boundaries):** Numbers (Min-1/Min, Max/Max+1), Strings (Len-1, Len+1), Logic Boundaries (e.g. >3 chars → 3 (Pass), 4 (Fail)), Arrays (Empty, Max items). **One Representative Rule (`mode: api-integration`):** full BVA pair for 1 representative field (the one with explicit business rule boundaries). Infrastructure-only fields (schema constraints without named business rule) reduced via DH-03.
   - **SEC (Security):** No Token, Invalid Token, Injection payloads (`' OR 1=1`), IDOR (another user's ID).
   - **L10N (Localization) [Conditional]:** Apply if the endpoint accepts user text fields (name, address, description, comment). Verify POS with: Cyrillic (`{CYR_NAME}`), Arabic/RTL (`{AR_NAME}`), CJK (`{ZH_NAME}`), emoji (`{EMOJI_STRING}`), special characters (`{SPECIAL_CHARS}`: `& < > " '`). Expected result: data saved and returned without corruption (UTF-8 round-trip). **L10N scenarios with 2xx response are POS-mutating — MUST contain `Contract Match: ...` on par with other POS.** **If the spec explicitly forbids a character class** (emoji, special characters) — the L10N scenario for it is generated as **NEG with 4xx** (not POS); Contract Match is not needed, `body.code` is required. **Exclusion marker in spec:** `L10N: out of scope`.
   - **IDEM (Idempotency) [MANDATORY for POST/PUT]:** Apply for **every** POST/PUT endpoint without exceptions. Four scenarios: (1) repeated request with identical `Idempotency-Key` within the cache window → MUST return **the same result** without creating a duplicate (`200 OK` or `201 Created` per contract); (2) repeated request after successful creation of a unique entity (no Idempotency-Key or different key) → `409 Conflict` (if business logic forbids duplicates); (3) **Cache-expiry + uniqueness conflict** — same data, same `Idempotency-Key`, but after the idempotency window expires AND the entity was already persisted → `409 Conflict` (uniqueness rule wins; NOT a new `201 Created`). Generate scenario (3) as NEG whenever the spec has both an idempotency window AND a uniqueness constraint; (4) **Body mismatch** — same `Idempotency-Key` within the cache window, but different request body (any field differs from the original request bound to that key) → `400 BAD_REQUEST` + code `IDEMPOTENCY_KEY_MISMATCH` (if spec defines this behavior) OR append `⚠️ SPEC AMBIGUITY: Idempotency-Key body mismatch behavior undefined` to the output file (if spec is silent — the system must choose: return cached response OR reject with 400). **If the endpoint's business logic forbids any repeated success (always 409 by design) — covering only scenario (2) is allowed with an explicit note in Scope Reductions: `IDEM:success_variant: not applicable (endpoint non-idempotent by design)`.** **Exclusion marker in spec:** `IDEM: not required`.

## Expected Result Engineering

Rules for the `Expected Result (HTTP + Logic)` column — mandatory for all scenarios:

### 1. Contract-First (Schema Validation)

For **POS** scenarios of mutating and read operations `Expected Result` MUST contain a JSON schema reference:
- Format: `Contract Match: {field}({type}), {field}({type})`
- Types: `string`, `UUID`, `ISO8601`, `boolean`, `integer`, `array`
- Example: `201 Created. Contract Match: verification_token(string/UUID), expires_at(ISO8601), status(string)`
- Benefit: a single test automatically catches field renaming, type change, or removal of a required key.

### 2. State Verification (Side Effects)

For **any** scenario with a **2xx response** that mutates the system, `Expected Result` MUST contain a state check: DB (`DB: users.status = 'PENDING'`), Queue (`Event published: user.registered`), Cache (`Cache invalidated: user:{UUID}`), or `State: N/A (read-only)`.
**External API (Isolation):** If business logic calls an external service, specify mock contract: `Mock: {System_Name} returns {Response}`. For `[CRITICAL]` also add failure mock: `Mock: PaymentGW returns 503 → Expected: 502 + body.code: 'UPSTREAM_ERROR'`.

### 3. Headers & Security

For **POS Happy Path** of each endpoint add a header verification row (`Type: HEADERS`):
- `Content-Type: application/json; charset=utf-8` — mandatory
- Security headers: `X-Content-Type-Options: nosniff`
- Example table row: `| REG-01h | HEADERS | Response headers | — | Content-Type: application/json; charset=utf-8. X-Content-Type-Options: nosniff |`

### 4. Audit-Ready (NEG Specificity)

For **NEG** scenarios `Expected Result` MUST contain `code` from the response body, not just the HTTP status:
- Format: `{HTTP_CODE} + body.code: '{ERROR_CODE}'`
- Example: `400 Bad Request + body.code: 'VALIDATION_ERROR'` (❌ just `400 Bad Request`)

### 5. Cleanup / Teardown

Every **POS** scenario that creates data MUST end with a cleanup step (re-running tests MUST NOT produce uniqueness conflicts).

**Priority order:** (1) `Cleanup: DB: DELETE FROM {table} WHERE {field} = {VALUE}` (2) `Cleanup: Admin API: DELETE /admin/{resource}/{UUID}` (3) `Cleanup: Test API: DELETE /test/{resource}/{UUID}` (4) `Cleanup: Public API: DELETE /{resource}/{UUID} (requires auth token from step POS-01)`.
**NEVER** write `Cleanup: DELETE /users/{UUID}` without auth mechanism — unauthenticated public DELETE is a security anti-pattern.
**If unknown from spec** → `Cleanup: ⚠️ mechanism unspecified — requires Admin API or DB access.`
Read-only → `Cleanup: N/A`.

## Constraints (Violation = REJECT)

| Category         | Rule                      | Violation → Correct                                                                                                                                                                                                                                                     |
|------------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Format**       | NO CODE                   | `@Test fun...` (❌) → `\| ID \| Scenario \|` (✅)                                                                                                                                                                                                                         |
| **Data**         | Placeholders              | `test@test.com` (❌) → `{UNIQUE_EMAIL}` (✅)                                                                                                                                                                                                                              |
| **Privacy**      | NO PII                    | `ivan.petrov` (❌) → `user_{uuid}` (✅)                                                                                                                                                                                                                                   |
| **Privacy**      | RFC 2606 Only             | `@gmail.com` (❌) → `@example.com` (✅)                                                                                                                                                                                                                                   |
| **Expectations** | Specificity               | "Error" (❌) → "400 Bad Request + Code 'INVALID_ID'" (✅)                                                                                                                                                                                                                 |
| **Expectations** | NO Vague                  | `X OR Y` in Expected Result (❌). Rules: (1) success vs. error (2xx OR 4xx) → two atomic scenarios (✅); (2) two different success codes (200 OR 201) → ⚠️ WARNING: clarify the contract in spec, then one specific code; (3) vendor-specific ambiguity — still two rows. |
| **Atomicity**    | 1 Row = 1 Check           | "Success then Fail" (❌) → Two separate rows (✅)                                                                                                                                                                                                                         |
| **BVA**          | Full Coverage             | Only Min-1 (fail) (❌) → Min-1 (fail) + Min (success) (✅)                                                                                                                                                                                                                |
| **Completeness** | Full Grid                 | Only Happy Path (❌) → POS + NEG + BVA + SEC + HEADERS (✅)                                                                                                                                                                                                               |
| **Duplication**  | NO Duplicates             | Same Action + Same Expected = Remove duplicate                                                                                                                                                                                                                          |
| **Contract**     | Schema-First              | `"Token returned"` (❌) → `"Contract Match: token(UUID), expires_at(ISO8601)"` (✅)                                                                                                                                                                                       |
| **State**        | Side Effects              | `"201 Created"` (❌) → `"201 Created. DB: status='PENDING'"` (✅) for POST/PATCH                                                                                                                                                                                          |
| **Audit**        | NEG Specificity           | `"400 Bad Request"` (❌) → `"400 + body.code: 'VALIDATION_ERROR'"` (✅)                                                                                                                                                                                                   |
| **Headers**      | Content-Type              | Content-Type not specified (❌) → separate `HEADERS` row for each endpoint (✅)                                                                                                                                                                                           |
| **Cleanup**      | Teardown                  | POS without cleanup (❌) → `"Cleanup: DB: DELETE FROM {table} WHERE ..."` or Admin API (✅). `Cleanup: DELETE /resource/{UUID}` without auth/ownership specified (❌)                                                                                                      |
| **L10N**         | UTF-8 Round-Trip          | Only ASCII in name/address (❌) → `{CYR_NAME}`, `{AR_NAME}`, `{EMOJI_STRING}` (✅) if field is text                                                                                                                                                                       |
| **IDEM**         | Idempotency               | No repeated request (❌) → IDEM scenarios mandatory for all POST/PUT: repeated request without duplicate + 409 on conflict (✅)                                                                                                                                           |
| **IDEM**         | Cache-expiry + uniqueness | `IDEM expired → 201 Created` (❌) → `IDEM expired → 409 Conflict` when entity was persisted and uniqueness constraint applies (✅)                                                                                                                                        |
| **Risk**         | Risk Tag                  | `[CRITICAL]` endpoint without expanded NEG (race condition, double execution) (❌) → `[CRITICAL]` endpoints MUST have expanded NEG set (✅)                                                                                                                               |
| **Isolation**    | External API Mock         | Calls external API without mock spec (❌) → `Mock: {System} returns {Response}` in Expected Result (✅)                                                                                                                                                                   |


## Output Template

Create file `docs/api-isolated-tests/test-scenarios_{timestamp}.md` (timestamp format: `YYYYMMDD_HHMMSS`).
**Important:** Each invocation produces a new timestamped file. If scenarios > 100 or the file becomes too large, create folder `docs/api-isolated-tests/scenarios/` and split output with timestamp: `01_Auth_{timestamp}.md`, `02_Users_{timestamp}.md`, `03_Orders_{timestamp}.md`.

```markdown
# Test Scenarios Specification
> Mode: api-integration

## Feature: [Create Payment] (POST /payments) [CRITICAL]

| ID | Type | Scenario | Input Data | Expected Result (HTTP + Logic) |
|----|------|----------|------------|--------------------------------|
| PAY-01 | POS | Successful Payment | amount: `100`, currency: `USD` | 201 Created. Contract Match: tx_id(UUID), status(string). Mock: PaymentGW returns `{"status":"success"}`. DB: tx.status='COMPLETED'. Cleanup: Admin API: DELETE /admin/transactions/{UUID} |
| PAY-01h | HEADERS | Response headers | — | Content-Type: application/json; charset=utf-8. X-Content-Type-Options: nosniff |
| PAY-02 | NEG | Double Charge | 2x identical `{PAYMENT_ID}` within 1s | 409 Conflict + body.code: 'DUPLICATE_PAYMENT' |
| PAY-03 | NEG | Upstream Timeout | amount: `100` | Mock: PaymentGW returns 503 → 502 + body.code: 'UPSTREAM_ERROR'. DB: tx.status='FAILED' |
| PAY-04 | BVA | Max Amount | amount: `{MAX_AMOUNT}` | 201 Created |
```

## Execution Flow

1. **Analyze:** Find specifications. Compile a full list of endpoints.
   - Extract `EXCLUDED_TYPES` from the specification (see Spec Exclusions above).
   - If exclusions found — record them explicitly at the top of the output file: `> ⚠️ Scope: SEC excluded per spec (section X.Y)`.
   - **Risk Assessment:** Classify each endpoint as `[CRITICAL]`, `[HIGH]`, or `[MEDIUM]` based on domain (Money/Security/Data Integrity → CRITICAL, core business → HIGH, read-only/dictionaries → MEDIUM).
   - **Dependency Map:** Identify endpoints that call external APIs (payment gateways, email/SMS providers, 3rd-party services) — mark them for mock specification.
   - **Contradiction Check:** Cross-reference business rules for logical conflicts before generating any scenario. Key patterns to detect:
     - IDEM cache-expiry behavior vs. uniqueness constraint: if both exist, scenario "same data after cache expires" yields `409 Conflict` (uniqueness wins), NOT a new `201 Created`. **⛔ Generating `201` here is a guaranteed audit FAIL.**
     - State-dependent logic vs. validation rules: if a rule applies only to certain entity states (e.g., PENDING vs. ACTIVE), verify that test inputs reflect the correct state.
     - **Case-normalization + uniqueness:** if spec mandates lowercase or case-normalization for a field (e.g., RFC 5321 lowercase for email) AND has a uniqueness constraint, generate a NEG scenario: same value in a different case variant (e.g., `ALEX@example.com` vs stored `alex@example.com`) → `409 Conflict`.
     - **IDEM body mismatch:** if spec defines an idempotency window, generate IDEM scenario (4): same key + different body → `400 BAD_REQUEST` + `body.code: 'IDEMPOTENCY_KEY_MISMATCH'`. If spec is silent on this behavior → append `⚠️ SPEC AMBIGUITY` instead.
     - If a contradiction is **unresolvable from the spec** → generate the scenario using the **stricter rule** (error over success), AND append to the output file after the table: `> ⚠️ SPEC AMBIGUITY: [Rule A] contradicts [Rule B] — expected behavior unspecified. Test generated with stricter interpretation: [result chosen].`
2. **Design Loop:** For each endpoint, **skipping types from `EXCLUDED_TYPES`**:
   - Generate POS scenarios (Min/Max).
   - Generate NEG (Validation) **applying DH constraints (`mode: api-integration`):** generate only 1 representative field per check type (DH-01), 1 representative per complexity group (DH-02), 1 representative per format/spacing pattern (DH-04, DH-05). Check §3a exceptions before skipping. In `mode: full-matrix` — generate all.
   - **⛔ STOP before BVA** — check `EXCLUDED_SCENARIOS` for `BVA:{field}`. Found → skip field. Not found → in `mode: api-integration` generate full BVA pair for 1 representative field with explicit business rule (DH-03); in `mode: full-matrix` generate full Min-1/Min/Max/Max+1 for all fields.
   - Generate BVA (Boundary values).
   - Generate SEC (Auth/Injection).
   - Generate IDEM (repeated request) — MANDATORY for all POST/PUT.
   - **If `[CRITICAL]`** — generate expanded NEG: race conditions, double execution, partial failure.
   - **If endpoint calls external APIs** — add `Mock: {System} returns {Response}` to Expected Result; for `[CRITICAL]` add failure mock scenario.
   - **IDEM Self-Check (MANDATORY after IDEM generation):** Count IDEM rows. Minimum: (a) first request, (b) cached repeat, (c) cache-expiry, (d) body mismatch (if spec defines it). Verify cache-expiry expects `409` (not `201`) when uniqueness applies. Missing rows → ADD before proceeding.
3. **Scope Purge Pass (MANDATORY — execute before writing any output):**
   Active deletion step. Go through every generated row and apply the following rules in order. For each matching row: **delete it**, do not keep it.

   **Purge rules (apply to every row):**
   - Row's Scenario tests **format, regex, or special-character rules** for a field listed in `EXCLUDED_SCENARIOS` as `NEG:format_{field}` → **DELETE**
   - Row's Scenario tests **string/number length boundaries (BVA)** for a field listed as `BVA:{field}` → **DELETE**
   - Row's Scenario tests **internal rule combinations** (password complexity variants, PII substring combos, encoding permutations) for a feature listed as `NEG:{feature}_combinations` → **DELETE** (the one representative NEG kept in step 2 survives)
   - Row's Type is in `EXCLUDED_TYPES` → **DELETE**
   - Row matches any remaining pattern in `EXCLUDED_SCENARIOS` → **DELETE**

   **DEFAULT_HEURISTICS safety net (skip ALL if `mode: full-matrix`):**
   Apply AFTER spec-driven rules. Catches unit-level rows that slipped past generation constraints. Check §3a exceptions before deleting.
   - **DH-01:** Row tests validation (null/empty/missing/wrong-type) that repeats across N fields with same error code → keep 1 representative field per check type, **DELETE** the rest
   - **DH-02:** Row tests one of M complexity sub-rules for a single field (e.g., password: uppercase, digit, special char) → keep 1 representative NEG, **DELETE** the rest
   - **DH-03:** Row tests BVA for an infrastructure-only field (schema length constraint, no named business rule) → keep 1 BVA pair for 1 representative field, **DELETE** the rest
   - **DH-04:** Row tests one of 3+ regex/whitespace/spacing variants for same field → keep 1 representative, **DELETE** the rest
   - **DH-05:** NEG row tests the exact same boundary already covered by a BVA row → keep BVA row, **DELETE** NEG duplicate

   **After purge — append `## Scope Reduction Log` to the output file:**
   ```markdown
   ## Scope Reduction Log
   | Removed ID | Scenario | Reason |
   |---|---|---|
   | REG-NEG-FORMAT-01 | Invalid phone format (non-E.164) | NEG:format_phone — delegated to Middleware (spec: Excluded from Test Scope) |
   ```
   If no rows were deleted → `## Scope Reduction Log\n> No rows removed.`

4. **Compliance Checklist:**
   - [ ] BVA complete? (Min-1/Min, Max/Max+1) — **skip if BVA ∈ `EXCLUDED_TYPES`**
   - [ ] NO hardcode/PII? (no real emails, @gmail/@yandex, real names)
   - [ ] Expectations specific + atomic? (no "or"/"depends on"; 1 row = 1 scenario; no duplicates)
   - [ ] **No scenarios from `EXCLUDED_TYPES` / `EXCLUDED_SCENARIOS`?** → Verify purge was complete.
   - [ ] **Risk tags assigned?** → Every endpoint header has `[CRITICAL]`, `[HIGH]`, or `[MEDIUM]`.
   - [ ] **`[CRITICAL]` expanded NEG?** → Race condition, double execution, partial failure scenarios present for all `[CRITICAL]` endpoints.
   - [ ] **External API mocks?** → Endpoints calling external services have `Mock: {System} returns {Response}` in Expected Result.
   - [ ] **L10N: all 5 variants present** — `{CYR_NAME}`, `{AR_NAME}`, `{ZH_NAME}`, `{EMOJI_STRING}`, `{SPECIAL_CHARS}`? (if `L10N ∉ EXCLUDED_TYPES` and endpoint contains text fields)
   - [ ] **DEFAULT_HEURISTICS evaluated?** (DH-01 through DH-05 applied, unless `mode: full-matrix`)
   - [ ] **One representative per validation class?** (not per-field) + **No NEG/BVA duplicates?** (DH-05)
   - [ ] **Explicitly named business rules preserved?** (heuristics did NOT reduce spec-named rules)
5. **Write:** Save output to `docs/api-isolated-tests/test-scenarios.md` (or split into files per endpoint in `docs/api-isolated-tests/`).

> **Loop Guard**: If a spec parse error or constraint violation (hardcoded data, ambiguous expected results)
> persists after two fix attempts, output an ESCALATION block with the error details and wait for user instruction.

## Quality Gates

**Agent Collaboration**: Output is a standalone scenario matrix for handoff to `/api-tests` (Kotlin) or `/api-tests-java` (Java)
for implementation. Include traceability metadata if upstream `/spec-audit` was run (reference its timestamp).

- **Zero scenarios from `EXCLUDED_SCENARIOS` or `EXCLUDED_TYPES` remain in the output** — `## Scope Reduction Log` is present and accounts for every deletion
- Every endpoint has at minimum: 1 POS + 1 NEG per validation class (missing, null/empty, wrong-type, invalid-value) + 1 BVA + 1 SEC + 1 HEADERS; every POST/PUT additionally at minimum 1 IDEM. In `mode: api-integration` the NEG minimum is 1 per validation class (not 1 per field × class)
- No scenario contains hardcoded data (email, phone, name)
- All Expected Results are specific (HTTP code + business logic)
- BVA covers both boundary values (Min-1/Min, Max/Max+1)
- No duplicate rows (Same Action + Same Expected)
- POS scenarios of mutating endpoints contain `Contract Match` with types of all key fields
- Mutating endpoints (POST/PATCH/PUT/DELETE) contain `State Verification` in Expected Result
- NEG scenarios contain `body.code: '{ERROR_CODE}'`, not just the HTTP status
- Every POS scenario contains `Cleanup` — a cleanup step or explicit `Cleanup: N/A`
- Endpoints with text fields (name/address/description) contain L10N scenarios (`{CYR_NAME}`, `{AR_NAME}`) or explicit `L10N: out of scope` from the specification
- All POST/PUT endpoints contain IDEM scenarios (repeated request without duplicate + conflict) or explicit `IDEM: not required` from the specification
- `[CRITICAL]` endpoints (Money, Security, Data Integrity) contain expanded NEG scenarios: race conditions, double execution, partial failure
- Endpoints calling external APIs contain `Mock: {System} returns {Response}` in Expected Result; `[CRITICAL]` endpoints additionally contain failure mock scenario
- DH Scope Reduction Log is present (unless `mode: full-matrix`)
- Mode declaration (`mode: api-integration` or `mode: full-matrix`) is present in the output file header

## Completion Contract

✅ SKILL COMPLETE: /api-isolated-tests
├─ Artifact: docs/api-isolated-tests/test-scenarios_{timestamp}.md (or folder docs/api-isolated-tests/scenarios/) — **Each invocation creates a new timestamped file**
├─ Coverage: 100% found endpoints
├─ Scenarios: N (POS: X, NEG: Y, BVA: Z, SEC: W)
└─ Ready for: /api-tests (Implementation)
