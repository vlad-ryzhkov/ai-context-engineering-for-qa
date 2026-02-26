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

### DEFAULT_HEURISTICS Safety Net (skip if `mode: full-matrix`)

Apply AFTER spec-driven rules. Catches unit-level rows that slipped past generation:

- **DH-01:** Validation repeating across N fields with same error code → keep 1 per check type, **DELETE** rest
- **DH-02:** M complexity sub-rules for single field → keep 1 NEG, **DELETE** rest
- **DH-03:** BVA for infrastructure-only field → keep 1 BVA pair for 1 field, **DELETE** rest
- **DH-04:** 3+ regex/whitespace variants for same field → keep 1, **DELETE** rest
- **DH-05:** NEG row = same boundary as BVA row → keep BVA, **DELETE** NEG

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
- [ ] DEFAULT_HEURISTICS evaluated (DH-01..DH-05), unless `mode: full-matrix`
- [ ] Risk tags assigned: every endpoint header has `[CRITICAL]`, `[HIGH]`, or `[MEDIUM]`
- [ ] Mode declaration in file header

---

## Cross-Domain Quality Gates (Phase 6)

- [ ] All selected domains have generated files in `docs/api-test-cases/`
- [ ] Summary file `summary_{timestamp}.md` exists with aggregated statistics
- [ ] Cross-domain dependency map present
- [ ] No duplicate scenarios across domain files (same endpoint in two files)

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
