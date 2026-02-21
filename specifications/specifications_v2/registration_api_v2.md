# Specification: User Registration API v3

## Endpoint

`POST /api/v1/users/register` — account creation with `PENDING` status.

## Business Rules

1. **Uniqueness:** Email and Phone must be unique in the system.
2. **2FA Flow:** A successful request returns `verification_token`. The system sends a 6-digit OTP code via SMS. To confirm registration, use endpoint `POST /api/v1/users/verify` (see OTP Specification).
3. **Password Security:**
   - 8–64 characters.
   - Required: uppercase letter, digit, special character.
   - **Forbidden (PII Check):** Using full words (tokens) from `full_name` or email (part before `@`). Word delimiters: dot, space, hyphen.
4. **SMS Gateway Failure:** If the SMS gateway is unavailable, the request completes with code `503 SERVICE_UNAVAILABLE`, the DB record is not created (transaction is rolled back).

## Data Schema (Request & Response)

| Field       | Type   | Required | Validation                                                             |
|-------------|--------|----------|------------------------------------------------------------------------|
| `email`     | string | Yes      | RFC 5321 (lowercase), max 254 characters                              |
| `phone`     | string | Yes      | E.164 (digits and `+` only), min 8, max 16 characters                 |
| `password`  | string | Yes      | See Security Rules                                                     |
| `full_name` | string | Yes      | 2–100 characters, Unicode letters, hyphens and single spaces (not at start/end) |

**Successful Response (201 Created):**
```json
{
  "verification_token": "jwt_token_here",
  "expires_at": "2026-02-18T22:30:00Z"
}
```

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
| 409  | CONFLICT              | Email or phone already registered                         |
| 500  | INTERNAL_ERROR        | Internal server error                                     |
| 503  | SERVICE_UNAVAILABLE   | SMS gateway unavailable. Transaction rolled back, record not created |

## Security & Logging

- **Transport:** TLS 1.2+ only. HTTP requests are rejected.
- **Logging:** The `password` field must be masked or excluded from application logs.

## Excluded from Test Scope (Test Scope Reduction)

The following aspects are **not tested** at this endpoint level:

| Scenario | Owner | What to test instead |
|----------|-------|----------------------|
| **Format validation** (`email`, `phone`, `full_name`): Regex, special characters | Middleware (Zod/Pydantic) | Field presence only: missing → 400 |
| **Uniqueness (Race Condition)**: parallel requests with same email/phone | Unique Index in DB | Only 409 on conflict (sequential requests) |
| **PII Logic (substrings, tokens)**: "name in password" combinations | Unit tests of shared-library | Only basic happy/sad path at API level |
| **SMS Delivery Guarantee**: user actually receiving the OTP | Notification Service | Only 201 on successful queue submission; 503 when gateway is unavailable |
| **Boundary value lengths** (e.g. 101 characters in `full_name`) | DB level | — |
