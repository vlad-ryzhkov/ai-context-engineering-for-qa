# Specification: User Registration API v2

## Endpoint

`POST /api/v1/users/register` — account creation with `PENDING` status.

## Business Rules

1. **Uniqueness:** Email and Phone must be unique in the system. Email uniqueness check is **case-insensitive**: `ALEX@example.com` and `alex@example.com` are treated as the same address and return `409 CONFLICT`.
2. **2FA Flow:** A successful request returns `verification_token`. The system sends a 6-digit OTP code via SMS. To confirm registration, use endpoint `POST /api/v1/users/verify` (see OTP Specification).
3. **Password Security:**
   - 8–64 characters.
   - Required: uppercase letter, digit, special character.
   - **Forbidden (PII Check):** Using full words (tokens) from `full_name` or email (part before `@`). Word delimiters: dot, space, hyphen. Password validation **MUST perform case-insensitive substring matching** against tokens extracted from email and full_name.
4. **SMS Gateway Failure:** If the SMS gateway is unavailable, the request completes with code `503 SERVICE_UNAVAILABLE`, the DB record is not created (transaction is rolled back).
5. **Idempotency:** Clients SHOULD include `Idempotency-Key` header (RFC 7231) to ensure safe retries in 2FA flow. Repeated requests with identical `Idempotency-Key` within 5 minutes return cached token without resending SMS. After the cache expires (> 5 minutes), the uniqueness constraint (Rule #1) applies unconditionally: if the same email/phone already exists with `PENDING` status — the endpoint returns `409 CONFLICT`. To resend OTP for a `PENDING` user, use `POST /api/v1/users/resend-otp` (see OTP Specification). **Body mismatch:** If the same `Idempotency-Key` is reused within the cache window but with a different request body (email, phone, or any other field differs from the original request bound to that key), the endpoint returns `400 BAD_REQUEST` with code `IDEMPOTENCY_KEY_MISMATCH`.

## Data Schema (Request & Response)

| Field       | Type   | Required | Validation                                                             |
|-------------|--------|----------|------------------------------------------------------------------------|
| `email`     | string | Yes      | RFC 5321 (lowercase), max 254 characters                              |
| `phone`     | string | Yes      | E.164 (digits and `+` only), min 8, max 16 characters                 |
| `password`  | string | Yes      | See Security Rules                                                     |
| `full_name` | string | Yes      | 2–100 characters, Unicode letters (including Latin-Extended: é, ü, ñ, etc.), hyphens, and single spaces (no consecutive spaces, not at start/end). Apostrophes are not allowed. |

**Successful Response (201 Created):**
```json
{
  "verification_token": "jwt_token_here",
  "expires_at": "2026-02-18T22:30:00Z"
}
```

**Response Field Details:**
- **`verification_token`:** JWT (RFC 7519) signed with HS256 algorithm. Claims: `exp` (expiration time as Unix timestamp), `sub` ("registration"), `email` (user email), `aud` ("sms-verification"). Do NOT include phone or password in the token.
- **`expires_at`:** ISO 8601 UTC timestamp. Calculated as request_received_time + 15 minutes (900 seconds). Example: if request received at 2026-02-18T22:15:00Z, expires_at is 2026-02-18T22:30:00Z.

**Error Format (4xx/5xx):**
```json
{"code": "ERROR_CODE", "message": "Human readable text", "field": "field_name"}
```

## Example Request (Reference)

```json
{
  "email": "alex.kid@example.com",
  "phone": "+79991234567",
  "password": "Safe_Password_2026",
  "full_name": "Alex Kideer"
}
```

## Error Codes

| HTTP | Code                  | Description                                               |
|------|-----------------------|-----------------------------------------------------------|
| 400  | VALIDATION_ERROR      | Field validation error (format, length, password rules)   |
| 400  | IDEMPOTENCY_KEY_MISMATCH | `Idempotency-Key` reused within cache window with a different request body |
| 409  | CONFLICT              | Email or phone already registered                         |
| 500  | INTERNAL_ERROR        | Internal server error                                     |
| 503  | SERVICE_UNAVAILABLE   | SMS gateway unavailable. Transaction rolled back, record not created |

## Security & Logging

- **Transport:** HTTPS only (TLS 1.2+). Plain HTTP connections are rejected.
- **Logging:** The following fields MUST NOT be logged in plaintext:
  - `password`: Excluded from logs entirely
  - `email`: Mask local part (all characters before `@`). Log domain only (e.g., `***@example.com`)
  - `phone`: Log last 4 digits only (e.g., `+7999****4567`)

## Excluded from Test Scope (Test Scope Reduction)

The following aspects are **not tested** at this endpoint level:

| Scenario | Owner | What to test instead |
|----------|-------|----------------------|
| **Format validation** (`email`, `phone`, `full_name`): Regex, special characters | Middleware (Zod/Pydantic) | Field presence only: missing → 400 |
| **Uniqueness (Race Condition)**: parallel requests with same email/phone | Unique Index in DB | Only 409 on conflict (sequential requests) |
| **PII Logic (substrings, tokens)**: "name in password" combinations | Unit tests of shared-library | Only basic happy/sad path at API level |
| **SMS Delivery Guarantee**: user actually receiving the OTP | Notification Service | Only 201 on successful queue submission; 503 when gateway is unavailable |
| **Boundary value lengths** (e.g. 101 characters in `full_name`) | DB level | — |
