## Expected Result Engineering

> Reference file for /api-isolated-tests. Rules for the Expected Result column.

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
