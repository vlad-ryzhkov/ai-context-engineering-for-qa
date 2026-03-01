# Output Template Reference

> Per-domain output file template for `/api-test-cases`.

## File Naming

```
docs/api-test-cases/{domain}_test-scenarios_{YYYYMMDD_HHMMSS}.md
```

Examples:
- `docs/api-test-cases/auth_test-scenarios_20260226_143000.md`
- `docs/api-test-cases/users_test-scenarios_20260226_143500.md`
- `docs/api-test-cases/summary_20260226_144000.md`

---

## Per-Domain File Template

```markdown
# {Domain} API Test Scenarios
> Mode: api-integration | Generated: {YYYYMMDD_HHMMSS}
> Service Type: {api-service|proxy-filter}
> Source: {spec_path}
> Skill: /api-test-cases

## Domain Summary
- Endpoints: {count} (or Decision Points for proxy-filter)
- Risk Level: {CRITICAL|HIGH|MEDIUM}
- EXCLUDED_TYPES: {list or "none"}
- EXCLUDED_SCENARIOS: {count} patterns applied

---

## Feature: [{Name}] ({METHOD} {path}) [{RISK}]

| ID | Type | Scenario | Input Data | Expected Result (HTTP + Logic) |
|----|------|----------|------------|--------------------------------|
| {PREFIX}-POS-01 | POS | Happy Path (min data) | {fields} | {HTTP code}. Contract Match: {fields}. {State}. Cleanup: {method} |
| {PREFIX}-POS-01h | HEADERS | Response headers | — | Content-Type: application/json; charset=utf-8. X-Content-Type-Options: nosniff |
| {PREFIX}-POS-02 | POS | Happy Path (max data) | {all optional fields} | {HTTP code}. Contract Match: {fields}. {State}. Cleanup: {method} |
| {PREFIX}-NEG-01 | NEG | Missing required field | {field}: null | 400 Bad Request + body.code: '{ERROR_CODE}' |
| {PREFIX}-NEG-02 | NEG | Wrong type | {field}: {wrong_type} | 400 Bad Request + body.code: '{ERROR_CODE}' |
| {PREFIX}-BVA-01 | BVA | {field} at Min boundary | {field}: {MIN} | {HTTP code} |
| {PREFIX}-BVA-02 | BVA | {field} below Min | {field}: {MIN-1} | 400 Bad Request + body.code: '{ERROR_CODE}' |
| {PREFIX}-SEC-01 | SEC | No auth token | — (no Authorization header) | 401 Unauthorized + body.code: '{ERROR_CODE}' |
| {PREFIX}-SEC-02 | SEC | Invalid token | Authorization: Bearer {INVALID_TOKEN} | 401 Unauthorized + body.code: '{ERROR_CODE}' |
| {PREFIX}-L10N-01 | L10N | Cyrillic input | {text_field}: {CYR_NAME} | {HTTP code}. Contract Match: {field}(string). UTF-8 round-trip |
| {PREFIX}-IDEM-01 | IDEM | Repeated request (cached) | Idempotency-Key: {SAME_KEY} | {HTTP code} — same result, no duplicate created |
| {PREFIX}-IDEM-02 | IDEM | Duplicate entity (no key) | Same data, no Idempotency-Key | 409 Conflict + body.code: '{ERROR_CODE}' |
| {PREFIX}-IDEM-03 | IDEM | Cache-expiry + uniqueness | Same data + same key after window expires | 409 Conflict + body.code: '{ERROR_CODE}' |
| {PREFIX}-IDEM-04 | IDEM | Body mismatch | Same key + different body | 400 Bad Request + body.code: 'IDEMPOTENCY_KEY_MISMATCH' |
```

### Data-Driven Block Format (DH-08)

When ≥3 scenarios for the same endpoint differ only in 1-2 fields, use this format:

```markdown
| ID | Type | Scenario | Input Data | Expected Result |
|----|------|----------|------------|-----------------|
| {PREFIX}-SEC-01 | SEC | Auth variants [Data-Driven: {N} params] | [see params] | [see params] |

**{PREFIX}-SEC-01 Parameters:**
| # | Variant | Input | Expected |
|---|---------|-------|----------|
| a | No token | — (no Authorization) | 401 + body.code: 'UNAUTHORIZED' |
| b | Invalid token | Bearer {INVALID_TOKEN} | 401 + body.code: 'INVALID_TOKEN' |
| c | Expired token | Bearer {EXPIRED_TOKEN} | 401 + body.code: 'TOKEN_EXPIRED' |
| d | Wrong role | Bearer {VIEWER_TOKEN} | 403 + body.code: 'FORBIDDEN' |
```

Rules:
- Parent row Type = the common type (SEC, NEG, L10N)
- Parameter table inherits parent ID with letter suffix (SEC-01a, SEC-01b, ...)
- Each parameter row is a separate test in api-tests (`@ParameterizedTest`)
- Do NOT use for scenarios with different HTTP status groups (2xx vs 4xx must be separate parent rows)

### Proxy-Filter Table Variant (service_type: proxy-filter)

Use this table format instead of the standard one when `service_type: proxy-filter`:

```markdown
## Decision Domain: [{Name}] [{RISK}]

| ID | Type | Scenario | Input (External) | Expected Result (HTTP + Logic) |
|----|------|----------|------------------|--------------------------------|
| {PREFIX}-POS-01 | POS | Valid request passes through | Authorization: Bearer {VALID_TOKEN}, GET {PATH} | 200 OK. Upstream response forwarded |
| {PREFIX}-NEG-01 | NEG | Invalid token rejected | Authorization: Bearer {INVALID_TOKEN} | 401 Unauthorized + body: '{ERROR_TEXT}' |
| {PREFIX}-SEC-01 | SEC | No auth header | — (no Authorization header) | 403 Forbidden + body: 'RBAC: access denied' |
| {PREFIX}-BVA-01 | BVA | Header at max length | Authorization: Bearer {MAX_LEN_TOKEN} | {expected behavior} |

Key differences from api-service template:
- "Decision Domain" replaces "Feature"
- "Input (External)" column explicitly names the HTTP element (header, URL, method, query param)
- No IDEM or L10N rows (proxy does not create resources or process user text)
- No Contract Match (proxy forwards upstream response, does not define its own schema)
- No Cleanup (proxy is stateless from HTTP perspective — internal state managed by event bus)
```

```markdown

---

## Scope Reduction Log
| Removed ID | Scenario | Reason |
|---|---|---|
| {ID} | {Description} | {Rule reference: EXCLUDED_SCENARIOS pattern or DH-XX} |

> No rows removed. ← (use this if nothing was removed)

---

## Cross-Domain Dependencies
- **Auth tokens:** Required by {list of endpoints in other domains}
- **Shared entities:** {entity} created in {domain A}, referenced in {domain B}
- **Prerequisite data:** {description}

> None identified. ← (use this if no cross-domain dependencies)

---

## Spec Ambiguities
> ⚠️ SPEC AMBIGUITY: {Rule A} contradicts {Rule B} — test generated with stricter interpretation: {result}

> No ambiguities found. ← (use this if none)
```

---

## ID Prefix Convention

Derive from domain + endpoint name:
- `auth/register` → `REG`
- `auth/login` → `LOGIN`
- `users/{id}` → `USR`
- `orders/create` → `ORD`
- `payments/charge` → `PAY`

Pattern: `{PREFIX}-{TYPE}-{NN}` (e.g., `REG-POS-01`, `PAY-NEG-03`, `USR-BVA-02`)

HEADERS rows: append `h` to the POS ID (e.g., `REG-POS-01h`)

---

## Summary File Template

```markdown
# API Test Cases — Cross-Domain Summary
> Generated: {YYYY-MM-DD HH:MM:SS}
> Mode: {api-integration|full-matrix}
> Service Type: {api-service|proxy-filter}
> Specs analyzed: {count}
> Skill: /api-test-cases

## Coverage Statistics
| Domain | Endpoints | Scenarios | POS | NEG | BVA | SEC | L10N | IDEM | HEADERS | Risk |
|--------|-----------|-----------|-----|-----|-----|-----|------|------|---------|------|
| auth | 4 | 42 | 8 | 16 | 6 | 4 | 4 | 4 | 4 | CRITICAL |
| users | 6 | 38 | 10 | 12 | 4 | 4 | 4 | 4 | 6 | HIGH |
| **Total** | **10** | **80** | **18** | **28** | **10** | **8** | **8** | **8** | **10** | — |

## Cross-Domain Dependencies
| Source Domain | Target Domain | Dependency | Type |
|--------------|--------------|------------|------|
| auth | users | Auth token required | prerequisite |
| users | orders | User ID referenced | data dependency |

## Excluded Scope
- EXCLUDED_TYPES: {list or "none"}
- EXCLUDED_SCENARIOS: {count} patterns applied across all domains
- MEDIUM risk reductions: {count} dimensions skipped
- PROXY_HEURISTICS: {count} scenarios filtered (proxy-filter only, omit for api-service)

## Spec Ambiguities (Aggregated)
| Domain | Ambiguity | Interpretation |
|--------|-----------|----------------|
| {domain} | {description} | {stricter rule chosen} |

## Generated Files
| # | File | Domain | Scenarios |
|---|------|--------|-----------|
| 1 | auth_test-scenarios_{ts}.md | auth | 42 |
| 2 | users_test-scenarios_{ts}.md | users | 38 |
```
