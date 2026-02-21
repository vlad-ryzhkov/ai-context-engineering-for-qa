# Hardcoded Test Data (Manual Test Cases)

**Applies to:** `/testcases` (manual test cases in Kotlin DSL)

## Why this is bad

Hardcoded data in manual test cases:
- Tester copies values instead of understanding boundaries
- Hides the logic behind test data selection (why this particular value?)
- When requirements change, all hardcoded locations must be found
- Impossible to reuse the test case for other environments

## Bad Example

```kotlin
// ❌ BAD: Specific values in expected
testCase("Successful registration") {
    precondition("User is not registered")

    step("Enter email") {
        action = "Enter test@example.com"  // Why this one?
        expected = "Email is displayed"
    }

    step("Enter password") {
        action = "Enter Password123!"  // Hardcoded specific password
        expected = "Password accepted"
    }
}
```

## Good Example

```kotlin
// ✅ GOOD: Description of data class, not a specific value
testCase("Successful registration") {
    precondition("User is not registered")

    step("Enter email") {
        action = "Enter a valid email (format user@domain.com)"
        expected = "Email is displayed in the input field"
    }

    step("Enter password") {
        action = "Enter a password meeting requirements (≥8 characters, letters + digits + special character)"
        expected = "Password accepted, strength indicator — green"
    }
}

// ✅ GOOD: For BVA — specify the boundary, not a specific value
testCase("Minimum password length") {
    step("Enter password") {
        action = "Enter a password exactly 8 characters long (minimum boundary)"
        expected = "Password accepted"
    }
}
```

## What to look for in review

- Specific email/phone/password in `action` or `expected`
- Missing explanation of "why this value" (boundary? valid? invalid?)
- Identical literal values in different test cases
- Technical details instead of business description
