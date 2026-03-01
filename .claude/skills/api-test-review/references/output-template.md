# Completion Protocol: Output Template

**Output structured review report with timestamped artifact:**

```text
✅ REVIEW COMPLETE: /api-test-reviewer
📁 Artifact: audit/api-test-review-report_{domain}_{timestamp}.md

📋 Scope
├─ Files reviewed: [count]
├─ Total lines: [count]
└─ Directory: [path]

🔐 Security Audit
├─ Hardcoded Secrets: [PASS / 🔴 CRITICAL]
├─ PII in Test Data: [PASS / 🔴 CRITICAL]
└─ Verdict: [🔴 CRITICAL / ✅ PASS]

🏗️  Architecture Audit
├─ DTO Isolation: [PASS / 🟠 MAJOR / 🟡 MINOR]
├─ HTTP Client Design: [PASS / 🟠 MAJOR]
├─ Contract Compliance: [PASS / 🟡 MINOR]
└─ Verdict: [🟠 MAJOR / ✅ PASS]

📝 Kotlin Idioms
├─ Async/Coroutines: [PASS / 🟠 MAJOR]
├─ Collections & Scope Functions: [PASS / 🟡 MINOR]
└─ Verdict: [🟠 MAJOR / ✅ PASS]

✨ Test Quality & DRY
├─ Parameterization: [count candidates]
├─ Helper Extraction: [count suggestions]
├─ Test Data Builders: [PASS / 🟠 MAJOR]
└─ Verdict: [🟠 MAJOR / ✅ PASS]

🔗 HTTP Validation
├─ Strict Status Codes: [PASS / 🟠 MAJOR]
├─ Response Body Validation: [PASS / 🟠 MAJOR]
├─ Error Response Validation: [PASS / 🟡 MINOR]
├─ Test Cleanup: [PASS / 🟠 MAJOR]
└─ Verdict: [🟠 MAJOR / ✅ PASS]

📊 Allure Integration
├─ @Step Annotations: [PASS / 🟠 MAJOR]
├─ Assertion Messages: [PASS / 🟠 MAJOR]
├─ HTTP Logging: [PASS / 🟡 MINOR]
└─ Verdict: [🟠 MAJOR / ✅ PASS]

🎯 Overall Verdict
├─ Critical Blockers: [count / None]
├─ Major Issues: [count / None]
├─ Minor Suggestions: [count]
└─ Status: [🔴 BLOCK MERGE / 🟠 REQUEST CHANGES / ✅ APPROVED]

📝 Key Recommendations

---
```

---

## Issue Format

For **EACH** issue listed above, expand with this format:

```markdown
**Issue:** [Name of the rule broken, e.g., "Blurry HTTP Status Check"]
**Severity:** [🔴 CRITICAL / 🟠 MAJOR / 🟡 MINOR]
**Location:** [`filename.kt:line_number`]
**Reference Used:** [e.g., `qa-antipatterns/http/status-codes.md`]
**Fix:**
\`\`\`kotlin
// The corrected code applied to the user's context
\`\`\`
**Why:** [Brief explanation of why this matters for API tests]
```

### Example Issue

```
**Issue:** Blurry HTTP Status Check
**Severity:** 🟠 MAJOR
**Location:** [`src/test/kotlin/users/tests/UserApiTests.kt:42`]
**Reference Used:** [`qa-antipatterns/http/status-codes.md`]
**Fix:**
\`\`\`kotlin
// ❌ BEFORE
response.status should { it in 200..299 }

// ✅ AFTER
response.status.shouldBe(201)  // Created, not just any 2xx
\`\`\`
**Why:** Range checks (200..299) hide bugs where an endpoint returns 200 instead of expected 201 (Created). Always assert the exact status code per scenario.
```

**Communication Protocol:**
- **MINIMAL VERBOSITY:** Output only tool invocations and completion block
- **No preamble:** No "Let me read the file", "I'll analyze", etc.
- **Evidence-based (STRICT):** Every finding MUST include exact file paths and line numbers (e.g., `src/test/kotlin/domain/users/tests/UserTests.kt:45-48`). Do not summarize findings or refer to issues without pointing to exact lines of code.
- **Actionable:** Each recommendation includes concrete code snippet
- **Verifiable:** User can open the file and navigate directly to the issue location using the file:line reference
