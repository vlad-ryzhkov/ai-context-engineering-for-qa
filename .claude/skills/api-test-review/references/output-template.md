# Completion Protocol: Output Template

**Output structured review report:**

```text
✅ REVIEW COMPLETE: /api-test-reviewer

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
├─ [Recommendation 1 with file:line]
├─ [Recommendation 2 with file:line]
└─ [Recommendation N]
```

**Communication Protocol:**
- **MINIMAL VERBOSITY:** Output only tool invocations and completion block
- **No preamble:** No "Let me read the file", "I'll analyze", etc.
- **Evidence-based:** Every finding includes file:line reference
- **Actionable:** Each recommendation includes concrete suggestion
