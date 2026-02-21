---
name: test-cases
description: Generates an exhaustive test scenario matrix (Markdown) directly from API specifications. Use when you need full regression coverage, find edge cases, or prepare a strict spec for automated tests. Do not use for generating automated test code — use /api-tests for that.
allowed-tools: "Read Write Edit Glob Grep"
agent: agents/sdet.md
context: fork
---

## Recommended Flow

For best results, run `/spec-audit` first — its audit output (`audit/spec-audit_{date}.md`) enriches scenario generation with discovered defects and edge cases. Running directly on a specification also works; scenario count may be lower.

---

## Input Context (Process Isolation)

`context: fork` — you cannot see chat history before your invocation.

**Allowed inputs:** Files explicitly read via tool + `$ARGUMENTS` path.
**Forbidden:** Assumptions from "previous agent context", inventing spec content.

---

## When to Use

- Before writing automated tests — create a baseline of test cases
- After `/spec-audit` — cover all discovered endpoints
- For regression coverage of a new API or changed endpoints
- As a spec for manual testing

## 🔒 SYSTEM REQUIREMENTS

Before execution the agent MUST load: `.claude/protocols/gardener.md`

---

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

2. **Scope:** **ALL ENDPOINTS (100% Coverage).**
   - Do not filter by importance. Test everything: Auth, Business Logic, Dictionaries, Settings.
   - Every discovered method (GET/POST/PUT/DELETE) MUST have a set of scenarios.

## Verbosity Protocol

**SILENT MODE:** All content goes to `docs/test-cases/test-scenarios.md`, not to chat.

**Chat output (restrictions):**
- Endpoint progress — MUST NOT output
- Intermediate tables — MUST NOT output
- Chat: only SKILL COMPLETE block + `📊 Artifact: docs/test-cases/test-scenarios.md`

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

   - If no exclusions found — `EXCLUDED_TYPES = []`, `EXCLUDED_SCENARIOS = []`, apply the full Coverage Matrix.

4. **Risk-Based Prioritization:**
   Before generating scenarios, assess the business risk of each endpoint:
   - **[CRITICAL]** — Money (payment, refund, discount, balance), Security (auth, tokens, permissions, password reset), Data Integrity (user deletion, account merge, data migration). For `[CRITICAL]` endpoints generate **expanded NEG scenarios**: race conditions (`concurrent {ACTION} with same {ID}`), double execution (`duplicate {PAYMENT_ID} within 1s`), partial failure (`timeout after debit before credit`).
   - **[HIGH]** — Core business logic (order creation, status transitions, matching). Standard NEG set.
   - **[MEDIUM]** — Dictionaries, settings, read-only endpoints. Standard NEG set.
   Tag each endpoint in the feature header: `## Feature: [Create Payment] (POST /payments) [CRITICAL]`

5. **Coverage Matrix (The Grid):**
   For **EVERY** endpoint apply verification layers **EXCEPT types from `EXCLUDED_TYPES`** and **EXCEPT patterns from `EXCLUDED_SCENARIOS`**:
   - **POS (Positive):** Happy Path (Min data & Max data).
   - **NEG (Negative):** Validation (Null, Empty, Wrong Type, Malformed JSON), State Conflicts (Action on PENDING/BLOCKED entity).
   - **BVA (Boundaries):** Numbers (Min-1/Min, Max/Max+1), Strings (Len-1, Len+1), Logic Boundaries (e.g. >3 chars → 3 (Pass), 4 (Fail)), Arrays (Empty, Max items).
   - **SEC (Security):** No Token, Invalid Token, Injection payloads (`' OR 1=1`), IDOR (another user's ID).
   - **L10N (Localization) [Conditional]:** Apply if the endpoint accepts user text fields (name, address, description, comment). Verify POS with: Cyrillic (`{CYR_NAME}`), Arabic/RTL (`{AR_NAME}`), CJK (`{ZH_NAME}`), emoji (`{EMOJI_STRING}`), special characters (`{SPECIAL_CHARS}`: `& < > " '`). Expected result: data saved and returned without corruption (UTF-8 round-trip). **L10N scenarios with 2xx response are POS-mutating — MUST contain `Contract Match: ...` on par with other POS.** **If the spec explicitly forbids a character class** (emoji, special characters) — the L10N scenario for it is generated as **NEG with 4xx** (not POS); Contract Match is not needed, `body.code` is required. **Exclusion marker in spec:** `L10N: out of scope`.
   - **IDEM (Idempotency) [MANDATORY for POST/PUT]:** Apply for **every** POST/PUT endpoint without exceptions. Two scenarios: (1) repeated request with identical data → MUST return **the same result** without creating a duplicate (`200 OK` or `201 Created` per contract); (2) repeated request after successful creation of a unique entity → `409 Conflict` (if business logic forbids duplicates). **If the endpoint's business logic forbids any repeated success (always 409 by design) — covering only scenario (2) is allowed with an explicit note in Scope Reductions: `IDEM:success_variant: not applicable (endpoint non-idempotent by design)`.** **Exclusion marker in spec:** `IDEM: not required`.

## Expected Result Engineering

Rules for the `Expected Result (HTTP + Logic)` column — mandatory for all scenarios:

### 1. Contract-First (Schema Validation)
For **POS** scenarios of mutating and read operations `Expected Result` MUST contain a JSON schema reference:
- Format: `Contract Match: {field}({type}), {field}({type})`
- Types: `string`, `UUID`, `ISO8601`, `boolean`, `integer`, `array`
- Example: `201 Created. Contract Match: verification_token(string/UUID), expires_at(ISO8601), status(string)`
- Benefit: a single test automatically catches field renaming, type change, or removal of a required key.

### 2. State Verification (Side Effects)
For **any** scenario of any type (POS, BVA, SEC, L10N, IDEM), if `Expected Result` contains a **2xx response** (request mutates the system), `Expected Result` MUST contain a system state check after the server response:
- DB: `DB: users.status = 'PENDING' WHERE email = {EMAIL}`
- Queue: `Event published: user.registered (topic: registrations)`
- Cache: `Cache invalidated: user:{UUID}`
- **External API (Isolation):** If business logic calls an external service (payment gateway, email/SMS provider, 3rd-party REST), `Expected Result` MUST specify the mock contract: `Mock: {System_Name} returns {Response}`. Example: `Mock: PaymentGW returns {"status":"success","tx_id":"{UUID}"}`. For `[CRITICAL]` endpoints also add failure mock: `Mock: PaymentGW returns 503 → Expected: 502 + body.code: 'UPSTREAM_ERROR'`.
- If external state is not checked — explicitly state `State: N/A (read-only)`.
- Example: `201 Created. DB: users.status='PENDING', verification_token NOT NULL. Mock: EmailProvider returns 200`

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
Every **POS** scenario that creates data MUST end with a cleanup step:
- Add to `Expected Result`: `Cleanup: DELETE /users/{UUID}` or `Cleanup: DB rollback`
- Goal: re-running tests MUST NOT produce uniqueness conflicts (duplicate email, duplicate phone).
- If cleanup is not needed (read-only) — explicitly state `Cleanup: N/A`.

## Constraints (Violation = REJECT)

| Category | Rule | Violation → Correct |
|----------|------|---------------------|
| **Format** | NO CODE | `@Test fun...` (❌) → `| ID | Scenario |` (✅) |
| **Data** | Placeholders | `test@test.com` (❌) → `{UNIQUE_EMAIL}` (✅) |
| **Privacy** | NO PII | `ivan.petrov` (❌) → `user_{uuid}` (✅) |
| **Privacy** | RFC 2606 Only | `@gmail.com` (❌) → `@example.com` (✅) |
| **Expectations** | Specificity | "Error" (❌) → "400 Bad Request + Code 'INVALID_ID'" (✅) |
| **Expectations** | NO Vague | `X OR Y` in Expected Result (❌). Rules: (1) success vs. error (2xx OR 4xx) → two atomic scenarios (✅); (2) two different success codes (200 OR 201) → ⚠️ WARNING: clarify the contract in spec, then one specific code; (3) vendor-specific ambiguity — still two rows. |
| **Atomicity** | 1 Row = 1 Check | "Success then Fail" (❌) → Two separate rows (✅) |
| **BVA** | Full Coverage | Only Min-1 (fail) (❌) → Min-1 (fail) + Min (success) (✅) |
| **Completeness** | Full Grid | Only Happy Path (❌) → POS + NEG + BVA + SEC + HEADERS (✅) |
| **Duplication** | NO Duplicates | Same Action + Same Expected = Remove duplicate |
| **Contract** | Schema-First | `"Token returned"` (❌) → `"Contract Match: token(UUID), expires_at(ISO8601)"` (✅) |
| **State** | Side Effects | `"201 Created"` (❌) → `"201 Created. DB: status='PENDING'"` (✅) for POST/PATCH |
| **Audit** | NEG Specificity | `"400 Bad Request"` (❌) → `"400 + body.code: 'VALIDATION_ERROR'"` (✅) |
| **Headers** | Content-Type | Content-Type not specified (❌) → separate `HEADERS` row for each endpoint (✅) |
| **Cleanup** | Teardown | POS without cleanup (❌) → `"Cleanup: DELETE /resource/{UUID}"` (✅) |
| **L10N** | UTF-8 Round-Trip | Only ASCII in name/address (❌) → `{CYR_NAME}`, `{AR_NAME}`, `{EMOJI_STRING}` (✅) if field is text |
| **IDEM** | Idempotency | No repeated request (❌) → IDEM scenarios mandatory for all POST/PUT: repeated request without duplicate + 409 on conflict (✅) |
| **Risk** | Risk Tag | `[CRITICAL]` endpoint without expanded NEG (race condition, double execution) (❌) → `[CRITICAL]` endpoints MUST have expanded NEG set (✅) |
| **Isolation** | External API Mock | Calls external API without mock spec (❌) → `Mock: {System} returns {Response}` in Expected Result (✅) |


## Output Template

Create file `docs/test-cases/test-scenarios.md`.
**Important:** If scenarios > 100 or the file becomes too large, create folder `audit/scenarios/` and split output: `01_Auth.md`, `02_Users.md`, `03_Orders.md`.

```markdown
# Test Scenarios Specification

## Feature: [User Auth] (POST /login) [CRITICAL]

| ID | Type | Scenario | Input Data | Expected Result (HTTP + Logic) |
|----|------|----------|------------|--------------------------------|
| AUTH-01 | POS | Valid Login | email: `{USER}`, pass: `{PASS}` | 200 OK, Token returned |
| AUTH-02 | NEG | Invalid Pass | email: `{USER}`, pass: `Wrong` | 401 Unauthorized |
| AUTH-03 | SEC | SQL Injection | email: `' OR 1=1--` | 400 Bad Request (Sanitized) |
| AUTH-04 | NEG | Concurrent Login Race | 2x parallel login `{USER}` | Both 200 OK, tokens differ. DB: sessions ≤ max_sessions |

## Feature: [Create Payment] (POST /payments) [CRITICAL]

| ID | Type | Scenario | Input Data | Expected Result (HTTP + Logic) |
|----|------|----------|------------|--------------------------------|
| PAY-01 | POS | Successful Payment | amount: `100`, currency: `USD` | 201 Created. Mock: PaymentGW returns `{"status":"success"}`. DB: tx.status='COMPLETED' |
| PAY-02 | NEG | Double Charge | 2x identical `{PAYMENT_ID}` within 1s | 409 Conflict + body.code: 'DUPLICATE_PAYMENT' |
| PAY-03 | NEG | Upstream Timeout | amount: `100` | Mock: PaymentGW returns 503 → 502 + body.code: 'UPSTREAM_ERROR'. DB: tx.status='FAILED' |

## Feature: [Create Order] (POST /orders) [HIGH]

| ID | Type | Scenario | Input Data | Expected Result (HTTP + Logic) |
|----|------|----------|------------|--------------------------------|
| ORD-01 | POS | Simple Order | items: `[{id:1, qty:1}]` | 201 Created, ID: `{UUID}` |
| ORD-02 | BVA | Max Qty | items: `[{id:1, qty:{MAX_INT}}]` | 201 Created |
| ORD-03 | BVA | Qty Overflow | items: `[{id:1, qty:{MAX_INT}+1}]` | 400 Bad Request |
| ORD-04 | NEG | Empty Cart | items: `[]` | 400 Bad Request |
```

## Execution Flow

1. **Analyze:** Find specifications. Compile a full list of endpoints.
   - Extract `EXCLUDED_TYPES` from the specification (see Spec Exclusions above).
   - If exclusions found — record them explicitly at the top of the output file: `> ⚠️ Scope: SEC excluded per spec (section X.Y)`.
   - **Risk Assessment:** Classify each endpoint as `[CRITICAL]`, `[HIGH]`, or `[MEDIUM]` based on domain (Money/Security/Data Integrity → CRITICAL, core business → HIGH, read-only/dictionaries → MEDIUM).
   - **Dependency Map:** Identify endpoints that call external APIs (payment gateways, email/SMS providers, 3rd-party services) — mark them for mock specification.
2. **Design Loop:** For each endpoint, **skipping types from `EXCLUDED_TYPES`**:
   - Generate POS scenarios (Min/Max).
   - Generate NEG (Validation).
   - **⛔ STOP before BVA** — explicitly check `EXCLUDED_SCENARIOS` for `BVA:{field}`. Found → skip field. Not found → generate full Min-1/Min/Max/Max+1.
   - Generate BVA (Boundary values).
   - Generate SEC (Auth/Injection).
   - Generate IDEM (repeated request) — MANDATORY for all POST/PUT.
   - **If `[CRITICAL]`** — generate expanded NEG: race conditions, double execution, partial failure.
   - **If endpoint calls external APIs** — add `Mock: {System} returns {Response}` to Expected Result; for `[CRITICAL]` add failure mock scenario.
3. **Review (Exclusion Compliance Checklist):**
   - [ ] BVA complete? (Min-1/Min, Max/Max+1) — **skip if BVA ∈ `EXCLUDED_TYPES`**
   - [ ] NO hardcode? (email/phone/name)
   - [ ] NO PII? (@gmail/@yandex, real names)
   - [ ] Expectations specific? (no "or", "depends on")
   - [ ] Atomic? (1 row = 1 scenario)
   - [ ] Duplicates? (Same Action + Same Expected → remove)
   - [ ] **No scenarios from `EXCLUDED_TYPES`?** → Find and remove all rows where `Type` is in `EXCLUDED_TYPES`.
   - [ ] **No scenarios from `EXCLUDED_SCENARIOS`?** → Find and remove rows matching patterns (injection, duplicate NEG per field, BVA of excluded fields, extra encoding variants of POS).
   - [ ] **Risk tags assigned?** → Every endpoint header has `[CRITICAL]`, `[HIGH]`, or `[MEDIUM]`.
   - [ ] **`[CRITICAL]` expanded NEG?** → Race condition, double execution, partial failure scenarios present for all `[CRITICAL]` endpoints.
   - [ ] **External API mocks?** → Endpoints calling external services have `Mock: {System} returns {Response}` in Expected Result.
   - [ ] **L10N: all 5 variants present** — `{CYR_NAME}`, `{AR_NAME}`, `{ZH_NAME}`, `{EMOJI_STRING}`, `{SPECIAL_CHARS}`? (if `L10N ∉ EXCLUDED_TYPES` and endpoint contains text fields)
4. **Write:** Save output to `docs/test-cases/test-scenarios.md` (or split into files per endpoint in `docs/test-cases/`).

## Quality Gates

- Every endpoint has at minimum 1 POS + 1 NEG + 1 BVA + 1 SEC + 1 HEADERS scenario; every POST/PUT additionally at minimum 1 IDEM
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

## Completion Contract

✅ SKILL COMPLETE: /test-cases
├─ Artifact: docs/test-cases/test-scenarios.md (or folder audit/scenarios/)
├─ Coverage: 100% found endpoints
├─ Scenarios: N (POS: X, NEG: Y, BVA: Z, SEC: W)
└─ Ready for: /api-tests (Implementation)
