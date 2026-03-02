# Output Template: API Test Review Report (Action-First)

## Structure

Generate report in **Action-First** order (violations only, Confidence ≥ 80):

```markdown
# API Test Review Report: {domain}

## 🔴 CRITICAL Issues

`[Issue title + file:line + brief why + fix code]`
`[Issue 2...]`
`[If none → omit entire section]`

## 🟠 MAJOR Issues

`[Issue title + file:line + brief why + fix code]`
`[Issue 2...]`
`[If none → omit entire section]`

## Test Overview

- **Files analyzed:** {count}
- **Language mode:** Kotlin | Java
- **Total tests:** {count}
- **Verdict:** ✅ PASS | 🔴 NEEDS FIXES

## ✅ Passing Categories

- Security: No issues found
- Architecture: No issues found
- HTTP Validation: No issues found
- [List ONLY categories with zero violations]

## 📝 Summary

{1-2 sentences on overall quality and next steps}
```

---

## Issue Format (Compact, Action-Oriented)

For **EACH** violation (Confidence ≥ 80), use:

```markdown
🟠 MAJOR: [Short title, e.g., "Blurry HTTP Status Check"]
📍 src/test/kotlin/domain/users/tests/UserTests.kt:42
[Brief 1-sentence explanation: why this matters + impact]

Fix:
\`\`\`kotlin
// ✅ AFTER: Corrected code
response.status.shouldBe(201)  // Created, not just any 2xx
\`\`\`
```

**Golden Rules:**
1. **Never report issues with Confidence < 80** (they're nitpicks)
2. **Never output PASS blocks** (e.g., "✅ No hardcoded secrets detected")
3. **Never output 🟡 MINOR issues** in main report (waste of time)
4. **One issue = max 4 lines** (title | location | why | fix)
5. **If zero violations → output only "Test Overview" + "Passing Categories"** (1 page max)

---

## Example: Report with MAJOR Issues

```markdown
# API Test Review Report: users

## 🟠 MAJOR Issues

🟠 MAJOR: Blurry HTTP Status Codes
📍 src/test/kotlin/users/tests/UserApiTests.kt:42, 58, 71 (and 3 more)
Range checks (200..299) hide bugs where an endpoint returns 200 instead of 201 (Created). Assert exact codes per scenario.

Fix:
\`\`\`kotlin
// ❌ BEFORE
response.status should { it in 200..299 }

// ✅ AFTER
response.status.shouldBe(201)  // Created, not just any 2xx
\`\`\`

---

🟠 MAJOR: Missing Allure @Step Annotations
📍 src/test/kotlin/users/tests/UserApiTests.kt:12–25 (test method body)
Without @Step, Allure reports show only generic test name. Users can't see test flow in reports or dashboards.

Fix:
\`\`\`kotlin
@Test
fun testCreateUserWithInvalidEmail() {
    val invalidEmail = "not-an-email"

    step("Create user with invalid email") {
        val response = userClient.createUser(invalidEmail)
        response.status.shouldBe(400)
    }
}
\`\`\`

---

## Test Overview

- **Files analyzed:** 6
- **Language mode:** Kotlin
- **Total tests:** 23
- **Verdict:** 🔴 NEEDS FIXES

## ✅ Passing Categories

- Security: No issues found
- Architecture: No issues found
- Test Quality: No issues found
- HTTP Error Handling: No issues found

## 📝 Summary

2/6 files have MAJOR issues (blurry assertions, missing Allure steps). Fix these before merging; they block release validation and observability in CI/CD.
```

---

## Example: Report with Zero Violations

```markdown
# API Test Review Report: auth

## Test Overview

- **Files analyzed:** 4
- **Language mode:** Kotlin
- **Total tests:** 18
- **Verdict:** ✅ PASS

## ✅ Passing Categories

- Security: No issues found
- Architecture: No issues found
- HTTP Validation: No issues found
- Allure Integration: No issues found
- Test Quality: No issues found

## 📝 Summary

All tests meet baseline standards. Well-organized, clear assertions, proper cleanup. Ready to merge.
```

---

## Communication Protocol

- **MINIMAL VERBOSITY:** Output only violations and completion block
- **No preamble:** No "Let me read the file", "I'll analyze", "Here's what I found", etc.
- **Evidence-based (STRICT):** Every finding MUST include exact file:line reference
- **Actionable:** Each recommendation includes copy-paste-ready code
- **Verifiable:** User can navigate directly to the issue using file:line reference
