# Coverage Matrix Reference

> Shared rules for test scenario generation. Used by `/api-test-cases` (bulk) and `/api-isolated-tests` (single-endpoint).

## The Grid — 7 Verification Dimensions

For **EVERY** endpoint apply these layers **EXCEPT types from `EXCLUDED_TYPES`** and **EXCEPT patterns from `EXCLUDED_SCENARIOS`**:

### POS (Positive)
Happy Path with Min data & Max data.

### NEG (Negative)
Validation (Body & Path params: Null, Empty, Wrong Type, Malformed JSON), State Conflicts (Action on PENDING/BLOCKED entity).

**One Representative Rule (`mode: api-integration`):** For validation checks repeating across fields with same error code, keep 1 representative field per check type. Full per-field NEG only when spec requires per-field error reporting verification or `mode: full-matrix`.

### BVA (Boundaries)
- Numbers: Min-1/Min, Max/Max+1
- Strings: Len-1, Len+1
- Logic Boundaries: e.g. >3 chars → 3 (Pass), 4 (Fail)
- Arrays: Empty, Max items

**One Representative Rule (`mode: api-integration`):** Full BVA pair for 1 representative field (the one with explicit business rule boundaries). Infrastructure-only fields reduced via DH-03.

### SEC (Security)
No Token, Invalid Token, Injection payloads (`' OR 1=1`), IDOR (another user's ID).

### L10N (Localization) [Conditional]
Apply if endpoint accepts user text fields (name, address, description, comment).
Verify POS with: Cyrillic (`{CYR_NAME}`), Arabic/RTL (`{AR_NAME}`), CJK (`{ZH_NAME}`), emoji (`{EMOJI_STRING}`), special characters (`{SPECIAL_CHARS}`: `& < > " '`).
Expected: data saved and returned without corruption (UTF-8 round-trip).

**L10N scenarios with 2xx response are POS-mutating — MUST contain `Contract Match: ...`.**
**If spec explicitly forbids a character class** (emoji, special characters) — generate as **NEG with 4xx**; `body.code` is required.
**Exclusion marker:** `L10N: out of scope`.

### IDEM (Idempotency) [MANDATORY for POST/PUT]
Four scenarios:
1. Repeated request with identical `Idempotency-Key` within cache window → same result without duplicate.
2. Repeated request after successful creation (no/different key) → `409 Conflict`.
3. **Cache-expiry + uniqueness conflict** — same data, same key, after window expires AND entity persisted → `409 Conflict` (uniqueness wins, NOT `201`).
4. **Body mismatch** — same key within cache window, different body → `400 BAD_REQUEST` + `IDEMPOTENCY_KEY_MISMATCH` (if spec defines) OR `⚠️ SPEC AMBIGUITY`.

**If endpoint is non-idempotent by design** (always 409) — cover only scenario (2) with note: `IDEM:success_variant: not applicable`.
**Exclusion marker:** `IDEM: not required`.

### HEADERS
For POS Happy Path of each endpoint add a header verification row (`Type: HEADERS`):
- `Content-Type: application/json; charset=utf-8` — mandatory
- Security headers: `X-Content-Type-Options: nosniff`

---

## Risk-Based Scope Tiers

| Risk | Dimensions | NEG Expansion |
|------|-----------|---------------|
| **CRITICAL** (Money, Security, Data Integrity) | All 7 + expanded NEG | Race conditions, double execution, partial failure |
| **HIGH** (Core business) | All 7 | Standard NEG set |
| **MEDIUM** (Read-only, dictionaries) | POS + NEG + SEC + HEADERS | Skip BVA, L10N, IDEM |

---

## Expected Result Engineering

### 1. Contract-First (Schema Validation)
For **POS** scenarios, `Expected Result` MUST contain JSON schema reference:
- Format: `Contract Match: {field}({type}), {field}({type})`
- Types: `string`, `UUID`, `ISO8601`, `boolean`, `integer`, `array`
- Example: `201 Created. Contract Match: verification_token(string/UUID), expires_at(ISO8601), status(string)`

### 2. State Verification (Side Effects)
For **any** 2xx mutating scenario, `Expected Result` MUST contain a state check:
- DB: `DB: users.status = 'PENDING'`
- Queue: `Event published: user.registered`
- Cache: `Cache invalidated: user:{UUID}`
- Read-only: `State: N/A (read-only)`

**External API (Isolation):** Specify mock contract: `Mock: {System_Name} returns {Response}`.
For `[CRITICAL]` add failure mock: `Mock: PaymentGW returns 503 → Expected: 502 + body.code: 'UPSTREAM_ERROR'`.

### 3. Headers & Security
HEADERS row per endpoint POS Happy Path:
`| {ID}h | HEADERS | Response headers | — | Content-Type: application/json; charset=utf-8. X-Content-Type-Options: nosniff |`

### 4. NEG Specificity
For **NEG** scenarios, `Expected Result` MUST contain `body.code`:
- Format: `{HTTP_CODE} + body.code: '{ERROR_CODE}'`
- Example: `400 Bad Request + body.code: 'VALIDATION_ERROR'`

### 5. Cleanup / Teardown
Every **POS** creating data MUST end with cleanup:
1. `Cleanup: DB: DELETE FROM {table} WHERE {field} = {VALUE}`
2. `Cleanup: Admin API: DELETE /admin/{resource}/{UUID}`
3. `Cleanup: Test API: DELETE /test/{resource}/{UUID}`
4. `Cleanup: Public API: DELETE /{resource}/{UUID} (requires auth token from step POS-01)`

**NEVER** `Cleanup: DELETE /users/{UUID}` without auth mechanism.
**If unknown** → `Cleanup: ⚠️ mechanism unspecified — requires Admin API or DB access.`
Read-only → `Cleanup: N/A`.

---

## DEFAULT_HEURISTICS (mode: api-integration)

> Skipped entirely when `mode: full-matrix`.

| ID | Heuristic | Action |
|---|---|---|
| DH-01 | Repeating Validation Pattern — null/empty/wrong-type across N fields, same error code | Keep 1 representative field per check type |
| DH-02 | Complexity Rule Combinatorics — M sub-rules for one field | Keep 1 representative NEG |
| DH-03 | Infrastructure BVA — schema-only length, no named business rule | Keep 1 BVA pair for 1 representative field |
| DH-04 | Format/Whitespace Consolidation — 3+ regex/spacing variants for same field | Keep 1 representative |
| DH-05 | Length Boundary Deduplication — NEG row = same boundary as BVA row | Keep BVA row, remove NEG duplicate |

### Exceptions (heuristic does NOT reduce)
- Scenario tests an explicitly named business rule from spec
- Scenario produces a different error code / HTTP status from others in its group
- Scenario tests state-dependent validation
- Endpoint is `[CRITICAL]` + scenario tests security/data-integrity

### API Coverage Guarantee (heuristics NEVER reduce)
All POS, SEC, IDEM, L10N, HEADERS; scenarios with distinct error code/HTTP status; explicitly named business rules; state-dependent and cross-field validation; `[CRITICAL]` security/data-integrity; ≥1 NEG per validation class; ≥1 BVA pair for field with business rule.

---

## Spec Exclusions Parsing

**Precedence:** Spec markers (`EXCLUDED_SCENARIOS`) > DEFAULT_HEURISTICS > `mode: full-matrix` override.

**Prevention-first:** Apply exclusions as generation constraints, not post-generation filters.

### Ownership → EXCLUDED_SCENARIOS Mapping

| Spec ownership marker | Add to EXCLUDED_SCENARIOS |
|---|---|
| `delegated to Middleware (Zod/Pydantic)` for field X | `NEG:format_X`, `BVA:X` — only `NEG:missing_X` survives |
| `covered by library` for feature Y | `NEG:Y_detail` — keep one representative NEG only |
| `Unit tests of shared-library` | `NEG:{feature}_combinations` — keep one POS + one basic NEG |
| `"Field presence only: missing → 400"` | All NEG except `NEG:missing_{field}` |
| `DB level` (BVA) | `BVA:{field}` for all mentioned fields |
| `L10N: out of scope` | `L10N:*` for all text fields |

### Contradiction Resolution
If a contradiction is **unresolvable from the spec** → generate scenario with the **stricter rule** (error over success) AND append:
`> ⚠️ SPEC AMBIGUITY: [Rule A] contradicts [Rule B] — expected behavior unspecified. Test generated with stricter interpretation: [result chosen].`

Key patterns to detect:
- IDEM cache-expiry vs. uniqueness constraint: `409 Conflict` (uniqueness wins), NOT `201 Created`
- Case-normalization + uniqueness: different case variant → `409 Conflict`
- IDEM body mismatch: same key + different body → `400 BAD_REQUEST` + `IDEMPOTENCY_KEY_MISMATCH` (if defined) or `⚠️ SPEC AMBIGUITY`
