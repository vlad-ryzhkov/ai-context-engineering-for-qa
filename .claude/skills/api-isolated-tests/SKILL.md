---
name: api-isolated-tests
description: Generates an exhaustive test scenario matrix (Markdown) directly from API specifications. Use when you need full regression coverage, find edge cases, or prepare a strict spec for automated tests. Do not use for generating automated test code — use /api-tests for that.
allowed-tools: "Read Write Edit Glob Grep"
agent: sdet
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

   **Ownership → EXCLUDED_SCENARIOS mapping:** See `_shared/coverage-matrix.md` § Spec Exclusions Parsing.

   - If no exclusions found — `EXCLUDED_TYPES = []`, `EXCLUDED_SCENARIOS = []`, apply the full Coverage Matrix.
3a. **API Integration Test Focus:** See `references/coverage-heuristics.md` for DEFAULT_HEURISTICS (DH-01–DH-05), Testing Level Classification, and API Coverage Guarantee. Skipped when `mode: full-matrix`.
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

> Full rules (Contract-First, State Verification, Headers, Audit-Ready, Cleanup): `references/expected-result-rules.md`

## Constraints (Violation = REJECT)

> Full constraints table: `references/constraints.md`


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
   Execute purge rules from `references/scope-purge-pass.md`. Apply spec-driven rules first, then DEFAULT_HEURISTICS safety net.

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
