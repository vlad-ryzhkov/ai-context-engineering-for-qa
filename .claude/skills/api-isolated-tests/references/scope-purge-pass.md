# Scope Purge Pass

> Reference file for /api-isolated-tests. Mandatory before writing output.

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
