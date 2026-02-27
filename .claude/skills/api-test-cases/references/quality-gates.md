# Quality Gates Reference

> Constraints, quality gates, compliance checklist, and scope purge rules for `/api-test-cases`.

## Constraints (Violation = REJECT)

| Category | Rule | Violation → Correct |
|----------|------|---------------------|
| **Format** | NO CODE | `@Test fun...` (❌) → `\| ID \| Scenario \|` (✅) |
| **Data** | Placeholders | `test@test.com` (❌) → `{UNIQUE_EMAIL}` (✅) |
| **Privacy** | NO PII | `ivan.petrov` (❌) → `user_{uuid}` (✅) |
| **Privacy** | RFC 2606 Only | `@gmail.com` (❌) → `@example.com` (✅) |
| **Expectations** | Specificity | "Error" (❌) → "400 Bad Request + Code 'INVALID_ID'" (✅) |
| **Expectations** | NO Vague | `X OR Y` in Expected Result (❌) → two atomic scenarios (✅) |
| **Atomicity** | 1 Row = 1 Check | "Success then Fail" (❌) → Two separate rows (✅) |
| **BVA** | Full Coverage | Only Min-1 (fail) (❌) → Min-1 (fail) + Min (success) (✅) |
| **Completeness** | Full Grid | Only Happy Path (❌) → POS + NEG + BVA + SEC + HEADERS (✅) |
| **Duplication** | NO Duplicates | Same Action + Same Expected = Remove duplicate |
| **Contract** | Schema-First | `"Token returned"` (❌) → `"Contract Match: token(UUID)"` (✅) |
| **State** | Side Effects | `"201 Created"` (❌) → `"201 Created. DB: status='PENDING'"` (✅) |
| **Audit** | NEG Specificity | `"400 Bad Request"` (❌) → `"400 + body.code: 'VALIDATION_ERROR'"` (✅) |
| **Headers** | Content-Type | Not specified (❌) → separate `HEADERS` row per endpoint (✅) |
| **Cleanup** | Teardown | POS without cleanup (❌) → `Cleanup: DB: DELETE FROM {table}` (✅) |
| **L10N** | UTF-8 Round-Trip | Only ASCII in name/address (❌) → `{CYR_NAME}`, `{AR_NAME}` (✅) |
| **IDEM** | Idempotency | No repeated request (❌) → IDEM scenarios for all POST/PUT (✅) |
| **IDEM** | Cache-expiry | `IDEM expired → 201` (❌) → `409 Conflict` when uniqueness applies (✅) |
| **Risk** | Risk Tag | `[CRITICAL]` without expanded NEG (❌) → expanded NEG set (✅) |
| **Isolation** | External API Mock | No mock spec (❌) → `Mock: {System} returns {Response}` (✅) |

---

## Scope Purge Pass (MANDATORY — execute before writing output)

Active deletion step. Go through every generated row and apply rules in order. For each match: **delete the row**.

### Purge Rules

1. Row tests **format/regex/special-character rules** for field in `EXCLUDED_SCENARIOS` as `NEG:format_{field}` → **DELETE**
2. Row tests **string/number length boundaries (BVA)** for field listed as `BVA:{field}` → **DELETE**
3. Row tests **internal rule combinations** for feature in `NEG:{feature}_combinations` → **DELETE** (one representative NEG survives)
4. Row's Type is in `EXCLUDED_TYPES` → **DELETE**
5. Row matches any remaining `EXCLUDED_SCENARIOS` pattern → **DELETE**

### PROXY_HEURISTICS Safety Net (apply FIRST when `service_type: proxy-filter`, skip if `mode: full-matrix`)

Apply BEFORE DEFAULT_HEURISTICS. Catches unit-test-only rows:

- **PH-01:** Row requires internal state manipulation (storage fields, cache internals) → **DELETE**
- **PH-02:** Row requires mock injection (component replacement, error simulation) → **DELETE**
- **PH-03:** Row requires filling internal structures to capacity → **DELETE**
- **PH-04:** Row tests inapplicable attack vector (SQLi, XSS, CSRF, SSRF on proxy with no SQL/HTML/sessions/URL-forwarding) → **DELETE**
- **PH-05:** Row requires nanosecond timing control on internal TTL/expiry → **DELETE**

**API-Testability check before deleting:** Is the input constructible from HTTP elements? Is the result observable in HTTP response? If both YES → do NOT delete.

### DEFAULT_HEURISTICS Safety Net (skip if `mode: full-matrix`)

Apply AFTER PROXY_HEURISTICS (if applicable) and spec-driven rules. Catches unit-level rows that slipped past generation:

- **DH-01:** Validation repeating across N fields with same error code → keep 1 per check type, **DELETE** rest
- **DH-02:** M complexity sub-rules for single field → keep 1 NEG, **DELETE** rest
- **DH-03:** BVA for schema/proto validate tag field WITHOUT named checklist rule → classify as [INFRA_BVA], **MOVE** to `unit-like_test-scenarios.md`. Exception: if boundary is named in checklist → keep.
- **DH-04:** 3+ regex/whitespace variants for same field → keep 1, **DELETE** rest
- **DH-05:** NEG row = same boundary as BVA row → keep BVA, **DELETE** NEG
- **DH-06:** IDEM for read-only endpoint (List, Get, no mutation) → **SKIP** entirely, log as "IDEM_READONLY"
- **DH-07:** NEG scenario requires degraded service state (uninitialized client, missing dependency) → classify as [INFRA_STATE], **MOVE** to `unit-like_test-scenarios.md`

**Check exceptions before deleting:** Named business rule, different error code/status, state-dependent validation, `[CRITICAL]` security/data-integrity.

### Scope Reduction Log (append to output file)

```markdown
## Scope Reduction Log
| Removed ID | Scenario | Reason |
|---|---|---|
| {ID} | {Description} | {EXCLUDED_SCENARIOS pattern or DH-XX rule} |
```

If no rows deleted → `## Scope Reduction Log\n> No rows removed.`

---

## Per-Domain Quality Gates (Phase 4)

Run after generating each domain batch:

- [ ] Zero scenarios from `EXCLUDED_SCENARIOS` or `EXCLUDED_TYPES` remain
- [ ] Scope Reduction Log present, accounts for every deletion
- [ ] Every endpoint minimum coverage per risk tier:
  - CRITICAL/HIGH: 1 POS + 1 NEG/class + 1 BVA + 1 SEC + 1 HEADERS; POST/PUT: 1 IDEM
  - MEDIUM: 1 POS + 1 NEG/class + 1 SEC + 1 HEADERS
- [ ] `[CRITICAL]` endpoints have expanded NEG (race conditions, double execution, partial failure)
- [ ] No hardcoded data — placeholders only
- [ ] All Expected Results specific (HTTP code + business logic)
- [ ] POS scenarios: `Contract Match` with field types
- [ ] Mutating POS: `State Verification` in Expected Result
- [ ] NEG scenarios: `body.code: '{ERROR_CODE}'`
- [ ] POS scenarios: `Cleanup` step or `Cleanup: N/A`
- [ ] External API endpoints: `Mock: {System} returns {Response}`
- [ ] L10N present for text fields (if `L10N ∉ EXCLUDED_TYPES`) — **CRITICAL/HIGH only** (MEDIUM endpoints skip L10N per risk tier scope reduction)
- [ ] IDEM present for POST/PUT (if `IDEM ∉ EXCLUDED_TYPES`) — CRITICAL/HIGH only
- [ ] DEFAULT_HEURISTICS evaluated (DH-01..DH-08), unless `mode: full-matrix`
- [ ] Data-Driven blocks used where ≥3 same-endpoint variants exist (DH-08)
- [ ] Risk tags assigned: every endpoint header has `[CRITICAL]`, `[HIGH]`, or `[MEDIUM]`
- [ ] Mode declaration in file header
- [ ] No [INFRA_BVA] scenarios (DH-03): BVA from schema/proto validate tag without named checklist rule → moved to `unit-like_test-scenarios.md`
- [ ] No [IDEM_READONLY] scenarios (DH-06): IDEM for non-mutating endpoints (List, Get) removed
- [ ] No [INFRA_STATE] scenarios (DH-07): degraded-state NEG (uninitialized client, missing dependency) moved to `unit-like_test-scenarios.md`
- [ ] FLOW scenarios present for services with inheritance/propagation (if spec mentions "inherits from parent", "override restores inheritance") — CRITICAL/HIGH only
- [ ] **Proxy-filter only:** PROXY_HEURISTICS evaluated (PH-01..PH-05)
- [ ] **Proxy-filter only:** Zero unit-test-only scenarios remain (API-Testability check passed)
- [ ] **Proxy-filter only:** IDEM and L10N skipped (proxy does not create resources or process user text)

---

## Cross-Domain Quality Gates (Phase 6)

- [ ] All selected domains have generated files in `docs/api-test-cases/`
- [ ] Summary file `summary_{timestamp}.md` exists with aggregated statistics
- [ ] Cross-domain dependency map present
- [ ] No duplicate scenarios across domain files (same endpoint in two files)
- [ ] `developer-questions_{timestamp}.md` generated with all SPEC AMBIGUITY items (if any ⚠️ SPEC AMBIGUITY were found)
- [ ] `unit-like_test-scenarios.md` generated and populated (if any [INFRA_BVA], [INFRA_STATE], [IDEM_READONLY], [UNIT_TEST_CANDIDATE] were removed from domain files)
- [ ] `security_middleware` domain file generated (for services with auth middleware detected)

---

## Compliance Checklist

Final self-check before writing output:

- [ ] BVA complete? (Min-1/Min, Max/Max+1) — skip if `BVA ∈ EXCLUDED_TYPES`
- [ ] NO hardcode/PII? (no real emails, @gmail/@yandex, real names)
- [ ] Expectations specific + atomic? (no "or"/"depends on"; 1 row = 1 scenario)
- [ ] No scenarios from `EXCLUDED_TYPES` / `EXCLUDED_SCENARIOS`?
- [ ] Risk tags assigned on every endpoint header?
- [ ] `[CRITICAL]` expanded NEG present?
- [ ] External API mocks present?
- [ ] L10N: all 5 variants (`{CYR_NAME}`, `{AR_NAME}`, `{ZH_NAME}`, `{EMOJI_STRING}`, `{SPECIAL_CHARS}`)? (if applicable)
- [ ] DEFAULT_HEURISTICS evaluated?
- [ ] One representative per validation class? No NEG/BVA duplicates (DH-05)?
- [ ] Named business rules preserved? (heuristics did NOT reduce spec-named rules)
