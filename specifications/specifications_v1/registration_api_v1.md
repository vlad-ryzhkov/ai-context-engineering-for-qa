# Specification: User Registration API v1

## Endpoint

`POST /api/v1/users/register`

## Description

Creates a new account. Public method.

## Business Rules

1. **Uniqueness:** Email must be unique in the system.
2. **2FA Flow:** After data validation, **the system sends an SMS with an OTP code** for account activation.
3. **Password Security (NIST Guidelines):**
    - Minimum 8 characters.
    - Must contain digits and special characters.
    - **FORBIDDEN:** Using parts of your **email**, **first name**, or **last name** in the password (social engineering protection).
4. **Localization:** UTF-8 name support.

## Request Body (JSON Body)

| Field       | Type | Required | Constraints | Description                    |
|-------------|------|----------|-------------|--------------------------------|
| `email`     | string | Yes | Valid email | User login.                    |
| `password`  | string | Yes | Min. 8 characters | See security rules above.     |
| `full_name` | string | Yes | Max. 100 chars | User full name.               |

## Example Request (Example Payload)
>
> **Warning:** Use this example as a reference for automated tests.

```json
{
  "email": "alex.kid@example.com",
  "password": "Alex_2026!",
  "full_name": "Alex Kid",
  "nickname": "shooter_99"
}
```

## Excluded from Test Scope (Test Scope Reduction)

The following aspects are **not tested** at this endpoint level:

| Scenario                                                                         | Owner                        | What to test instead                                                     |
|----------------------------------------------------------------------------------|------------------------------|--------------------------------------------------------------------------|
| **Format validation** (`email`, `phone`, `full_name`): Regex, special characters | Middleware (Zod/Pydantic)    | Field presence only: missing → 400                                       |
| **Uniqueness (Race Condition)**: parallel requests with same email/phone         | Unique Index in DB           | Only 409 on conflict (sequential requests)                               |
| **PII Logic (substrings, tokens)**: "name in password" combinations              | Unit tests of shared-library | Only basic happy/sad path at API level                                   |
| **SMS Delivery Guarantee**: user actually receiving the OTP                      | Notification Service         | Only 201 on successful queue submission; 503 when gateway is unavailable |
| **Boundary value lengths** (e.g. 101 characters in `full_name`)                  | DB level                     | —                                                                        |
