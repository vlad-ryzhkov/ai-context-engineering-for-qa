---
name: spec-audit
description: Performs a deep QA audit of a specification based on ISTQB, BABOK, and OWASP standards. Identifies not only architectural gaps but also logical contradictions between Requirements, Data schema, and Examples (Dry Run). Use before writing tests, when reviewing requirements, or analyzing a specification for contradictions. Do not use for code review or test code analysis.
allowed-tools: "Read Write Glob"
agent: agents/auditor.md
context: fork
---

## 🔒 SYSTEM REQUIREMENTS

Before execution the agent MUST:
1. Load `.claude/protocols/gardener.md`
2. All output artifacts (`.md` files, tables, headers, examples) MUST be written in English. Field names and code identifiers remain as-is.

---

# /spec-audit — Specification Integrity and Risk Analysis

## Protocol

1. **Role:** Senior engineer & offensive QA. "Evil tester". Critical QA auditor. Zero tolerance for Ambiguity.
2. **Objective:** Find reasons why implementing this specification will lead to bugs, vulnerabilities, or development Blockers.
3. **Principle — Shift Left Extreme:** We hunt bugs in *text* while they cost $1, not $1000 in production.
4. **Principle — Trust No One:** Every invocation performs a FRESH, INDEPENDENT audit. Never reuse or reference previous audits. Even if an audit for this specification already exists, create a NEW audit file with a unique timestamp. This ensures consistency, reduces hallucination, and preserves audit history.
5. **Anti-Hallucination Rule:** Never assume a field exists unless it is explicitly listed in the table or schema. If an action (SMS, Push, Email) is mentioned in the text but the field (`phone`, `device_token`, `email`) is missing from the Request Body — this is a specification ERROR, not a reason to add a field "from memory" or logical inference. Log as Defect 10.
6. **Principle — API Integration Lens:** This audit evaluates specification readiness for **API integration testing**, not unit testing. Prioritize defects that affect the HTTP contract (status codes, response schema, headers, error codes), business logic at the API boundary, and cross-system behavior. Per-field validation gaps that are typically handled by framework-level validators (Zod, Pydantic, Bean Validation) are valid spec-consistency findings but should be deprioritized to Minor (4-5) unless they represent a named business rule or affect the API error contract.

## Input Data (Step 0 — execute FIRST, before everything else)

Determine the specification by Priority. **Evaluate steps in order. Stop at the first match — do NOT proceed to subsequent steps.**

1. **`$ARGUMENTS`** — if a path is provided here (Claude Code CLI) → read the file with the `Read` tool. **→ STOP. Skip steps 2–4.**
2. **User message** — if it contains a file path (`.md`, `.yaml`, `.json`, `.txt`) → read it with the `Read` tool. (Cursor and other environments where `$ARGUMENTS` is not substituted.) **→ STOP. Skip steps 3–4.**
3. **Auto-search** — only if NO path was found in steps 1 or 2 → run `Glob: specifications/**/*.md`, read the **first result only**.
4. **Auto-search yielded no results** — output `⚠️ WARNING: specification not found` and continue with an empty base.

**Multiple specifications:** Only if multiple file paths are **explicitly provided** in $ARGUMENTS or user message — run the full analysis for each separately. Each spec generates its own artifact with a unique filename. Do not merge findings across specs. **Auto-search never produces multiple specs — it reads the first result only.**

## Before Starting

Read `.claude/qa_agent.md`.

## Analysis Algorithm (4 passes)

You MUST perform the analysis in 4 stages. Do not mix conclusions.

### 1. Static Analysis (Deep Cross-Check)

- **Key-to-Key Mapping (List Method):** You MUST physically write out two sorted lists:
  - **List A:** all keys from the JSON example (line by line, in alphabetical order).
  - **List B:** all fields from the Parameters Table (line by line, in alphabetical order).
    Compute the delta character-by-character: `A \ B` (in JSON but not in table) and `B \ A` (in table but not in JSON). Any non-empty delta set — **Defect 9**. Skipping list construction is not allowed — an incomplete list invalidates the analysis.
- **Constraint Verification:** Take each value from the Example Payload and verify it against ALL table constraints (min/max length, type, format, regex). If the table says `max: 100` and the string in the example is longer — **Defect 9**. If the text says "min. 8 characters" and the example has 7 — **Defect 9**.
- **Boundary Arithmetic Test:** For each field with a constraint (min/max length, min/max value) you MUST write out the equation and compute the result:
  - `len("value_from_example") = N; min=X, max=Y → PASS` (if X ≤ N ≤ Y)
  - `len("value_from_example") = N; min=X, max=Y → FAIL` (if N < X or N > Y) → **Defect 9**
  - Example: `len("Pass1234") = 8; min=8, max=64 → PASS`
    Silence is not allowed: every field with a numeric constraint MUST have a line with the test result.
- **Null Matrix:** You MUST create a table for all request fields:
    | Field | required | HTTP response when field is missing described? | Status |
    |---|---|---|---|
    | `email` | true | 400 + `{"error": "email required"}` | PASS |
    | `phone` | true | not described | FAIL → Defect 8 |

    **Presumption of required:** If the `required` column is absent from the table — treat all fields as required by default. Log the absence of the column itself as **Defect 4-5 (Minor)** — "Adding a `required` column is recommended to eliminate Ambiguity". Do not escalate to Defect 8 if the behavior when a field is missing is implicitly covered by a generic error code (e.g. `400 VALIDATION_ERROR`). Assign Defect 8 only if the behavior when a field is missing is **not described at all** — neither explicitly nor via a generic handler.
- **Regex Literal Test:** If the specification defines a regex pattern for a field — you MUST apply it literally to the value from the Example Payload. Record the result explicitly:
  - `regex="..."` applied to `"value"` → MATCH → PASS
  - `regex="..."` applied to `"value"` → NO-MATCH → analyze the cause (see below)
    If a regex is specified but the example is not tested — the analysis is incomplete.
    **Rule for interpreting example-to-rule mismatch:** If the example violates a rule — first check whether the rule itself contains Ambiguity. Variants:
    1. Rule is unambiguous, example is clearly wrong → **Defect 9** "Example violates Requirement".
    2. Rule is ambiguous (e.g. "Unicode letters only" vs. space as PII separator in the same document) → this is a Contradiction in the **specification**, not an example error. Classify as **Defect 10 (Contradiction)** if the rules are mutually exclusive, or **Defect 7-8** if the Ambiguity can be resolved with clarification. The Recommendation should propose fixing the rule (or regex), not the example.
- **Type Checking:** Are the data types appropriate? (e.g. `money` as float — this is a risk, decimal/int is needed).
- **Verb-Data Lineage (Data Tracing):** Find ALL system actions (verbs) in the text: sending SMS, Email, Push, writing to DB, calling an external service. You MUST compile a table:
    | Action | Required field | Present in Request Body? |
    |---|---|---|
    | Send SMS | `phone` | FOUND / MISSING |

    If status is MISSING — **Blocker (Data Gap, Priority 10)**. Anti-Hallucination Rule: do not add a field to the table "from memory".

### 2. Mental Sandbox (Simulation and Fuzzing)

- **Rule Enforcement (Dry Run):** Take the Example Payload and "run" it through each Business rule literally.
- **PII Dry Run (mandatory when password security rules are present):** If a rule states "password MUST NOT contain personal data" — perform a mechanical check:
    1. Extract all tokens from `email` (part before `@` and after `@`) and `full_name`, token length > 3 characters.
    2. For each token: find its occurrence in the `password` string (case-insensitive).
    3. Record the result: token `"ivan"` in `"Ivan2024!"` → MATCH → **Defect 9** "Business logic violation in example data".
    Coverage: every token MUST be checked explicitly. Skipping is not allowed.
- **HTTP Status Exhaustion (Branch Coverage):** Find all conditional branches in Business rules ("if", "in case", "when", "otherwise"). You MUST compile a table:
    | Condition (branch) | HTTP status described? | Error body format described? |
    |---|---|---|
    | Email already registered | 409 / not specified | `{"error": "..."}` / not specified |

    If the status or body is not described for any branch — **Defect 8** (Undefined behavior).
- **Happy Path Dry Run:** Run the example data through the Business rules step by step.
- **Mental Fuzzing (Most important):** Attack the requirements. Devise 3 boundary scenarios that will break the logic:
  - *Null/Empty:* What if a required field arrives empty? Is the error described?
  - *Boundary values:* Maximum length, negative numbers, special characters, emoji.
  - *Status Conflicts:* What if the status is already "Completed" and we send "Cancel"?

  **Focus:** Prioritize scenarios testable at the API boundary (HTTP request → response). Internal implementation concerns (how the validator parses each character class, regex engine behavior) are unit-test scope — note them as Minor if the spec is inconsistent, but do not escalate.

### 3. Architecture and NFR (Non-Functional Requirements)

- **Concurrency:** What happens with two simultaneous requests? (Is an Idempotency Key required?)
- **Security (OWASP):**
  - Are there IDOR risks? (userId in URL without authorization check).
  - PII: Is there sensitive data in logs or the response?
- **Distributed Systems:** Are external system timeouts accounted for? What happens if the database responded but the message broker went down?

### 4. Ambiguity Check

- Look for weasel words: "quickly", "correctly", "as usual", "later". These are signs of tech debt.

---

## Step 5 — Defect Consolidation (MANDATORY — execute AFTER all 4 passes, BEFORE writing any output)

**Purpose:** Guarantee that every labeled defect from the analysis makes it into the Risk Matrix. This step prevents silent defect loss.

1. Scan the full text of all 4 passes for any item labeled `DEFECT N`, `Defect N`, `→ Defect`, `BLOCKER`, or `FAIL →`.
2. Compile a numbered master list. Each **unique issue** = one entry. Do not group or merge distinct issues.
3. Count totals by priority band: 10, 8-9, 6-7, 4-5.
4. **Every entry MUST become a row in the Risk Matrix.** Merging rows is FORBIDDEN unless two items are literally the same defect described twice.
5. **Write the Executive Summary LAST** — after the Risk Matrix is finalized. Use the counts from step 3 as inputs to the Score formula. Do not compute the Score from intermediate estimates.

**Verification:** `count(Risk Matrix rows) == count(master list entries)`. If not equal — the report is incomplete. Do not generate the output file until they match.

---

## When to Use

- Before writing test cases or automated tests for a new feature
- When reviewing requirements from PO/analyst
- When the specification contains ambiguous or potentially conflicting requirements

## Output Results

**Default:** save full audit to `audit/spec-audit_{SPEC_NAME}_{YYYYMMDD_HHMMSS}.md`, output SKILL COMPLETE block to chat (timestamp format: `YYYYMMDD_HHMMSS`).

**SPEC_NAME** — derived from the specification filename: lowercase, spaces and slashes replaced with `-`, extension stripped. Example: `registration_api_v1.md` → `registration-api-v1`.

**Multiple runs same day for the same spec** — create a NEW file with unique timestamp. Never overwrite previous audits. This ensures audit history and compliance with "Trust No One" principle.

## Output Contract

**Limit:** Maximum 42 Defects.

### 1. Executive Summary

- **Verdict:** `Ready for development` / `Approved with corrections` / `Blocked`.
- **Specification Quality Score:** (0-100%). Calculated by formula:
  - Start: **100%**
  - **-20%** for each Blocker (Priority 10)
  - **-10%** for each Critical (Priority 8-9)
  - **-5%** for each Major (Priority 6-7)
  - **-2%** for each Minor (Priority 4-5)
  - Score cannot be below **0%**.
  - Formula: `Score = max(0, 100 - 20*Blockers - 10*Critical - 5*Major - 2*Minor)`
- **Top 3 risks:** Brief, main issues.

### 2. Risk Matrix (Defect Table)

Sort by Priority (10 → 1).

**Priority Scale:**
- **10 (Blocker):** Only two kinds:
    1. **Data Gap** — data required to perform a declared action is completely missing from the schema (e.g. action "send SMS" but the `phone` field is absent entirely).
    2. **Direct logical Contradiction** — two rules are mutually exclusive and cannot be implemented simultaneously without changing the specification.
    Everything else — not a Blocker.
- **8-9 (Critical):** High risk of a bug in production: undescribed Business logic branches, undefined behavior on external system failure, Critical NFR gaps.
- **6-7 (Major):** Architectural risk (no Idempotency, poor data format), standards violations.
- **4-5 (Minor):** Ambiguity in wording, missing error examples, missing auxiliary schema attributes (required, max for email).

**Scope-Priority interaction:** A per-field validation defect (`Scope: VAL`) that does NOT represent a named business rule MUST NOT exceed Priority 6. Named business rules (e.g., "password must not contain personal data") retain their natural priority regardless of scope. This prevents field-level spec-consistency findings from dominating the Risk Matrix over API-contract issues.

| Priority | Scope | Category | Issue | Scenario / Evidence | Recommendation |
|:---:|:---:|---|---|---|---|
| **10** | API | Data Gap | No `phone` for SMS | Logic requires 2FA, but `POST /register` has no phone field. | Add a field or retrieve from profile. |
| **8** | API | Security | IDOR risk | `GET /orders/{id}` does not require owner verification in the description. | Explicitly state the rule: "Order.userId == CurrentUser.id". |
| **7** | API | Fuzzing | Negative price | Behavior for `amount: -100` is not described. | Add validation `min: 0.01`. |
| **5** | VAL | Spec Consistency | Example violates max length | `len("value") = 120; max=100 → FAIL` | Fix example or update constraint. |

Scope values:
- **API** — affects HTTP contract, business logic, cross-system behavior (status codes, error routing, auth, idempotency, state transitions)
- **VAL** — field-level validation consistency (per-field constraint violations, regex mismatches, boundary arithmetic failures in examples). Relevant for spec quality but typically covered by framework validators at unit level.
- **ARCH** — non-functional / architectural (concurrency, PII exposure, observability gaps)

### 3. Readiness Checklist (Gap Analysis)

Mark what is present (✅), what is missing (❌).

- [ ] **Schema:** JSON example matches the table.
- [ ] **Validation:** min/max/regex specified for all fields.
- [ ] **Errors:** Error codes (4xx, 5xx) and error response format are described.
- [ ] **Cross-Check:** Every technical action (SMS, Email, Push, DB Write) is backed by a corresponding field in the input data or context.
- [ ] **Security:** Scope/Roles specified for the Endpoint.
- [ ] **Observability:** Clear what to log (and what NOT to log, e.g. card PAN).

### 4. Blocking Questions

Only questions without answers to which coding cannot begin.

**Style:** Each question — a complete, polite, and precise sentence in English, addressed to the analyst or product owner. Do not use abbreviations or jargon.
*Good question example:* "Please clarify which HTTP status and error message the Endpoint should return if a user with the specified email is already registered in the system."

1. [Complete interrogative sentence.] (Impact: ...)

## Definition of Done (for AI agent)

Before generating the output file, verify ALL conditions are met:

- [ ] 4 analysis passes completed (Static, Mental, Architecture, Ambiguity).
- [ ] Key-to-Key, Null Matrix, and Verb-Data Lineage tables are physically generated.
- [ ] Defect Consolidation (Step 5) completed: master list compiled, `count(Risk Matrix rows) == count(master list entries)`.
- [ ] Risk matrix is sorted by priority descending (10 → 1).
- [ ] Specification Quality Score calculated AFTER Risk Matrix is finalized, using exact Risk Matrix row counts (not intermediate estimates). Formula inputs must match Risk Matrix totals.
- [ ] Report is written in English (field names and code identifiers remain as-is).
- [ ] Gardener Analysis section appended to the artifact file.
- [ ] Every Risk Matrix row has a Scope tag (API/VAL/ARCH). No VAL-scoped defect without named business rule exceeds Priority 6.

If any condition is NOT met — do not generate the file, complete the missing pass first.

## Self-Check (Critically important)

Before output, check yourself against 10 specific errors in the specification:
1. **Key-to-Key Mapping:** Are all JSON keys and all table fields listed? Is the delta computed? Discrepancy = Defect 9.
2. **Constraint Verification:** Are Example Payload values verified against all min/max/format constraints? Violation = Defect 9.
3. **Schema mismatch:** Are there fields in the example that are not in the description (or types do not match)?
4. **Data Gap:** Is there logic (e.g. 2FA, sending Email) for which no input data is provided?
5. **Rule violation (Dry Run):** Does the example data (payload) violate textual Business rules?
6. **Undefined behavior:** Are boundary cases not described (null, negative numbers, special characters)?
7. **NFR and security:** Are there IDOR, PII in logs, missing Idempotency, race conditions?
8. **Boundary Arithmetic Test:** For each field with a constraint — is the equation `len = N; min=X, max=Y → PASS/FAIL` recorded? Violation = Defect 9.
9. **HTTP Status Exhaustion:** For each Business logic branch — is the HTTP status and response body format specified? Unclosed branch = Defect 8.
10. **Null Matrix:** For each `required` field — is the API response when it is missing described? No = Defect 8.

11. **Defect Completeness:** Count all items labeled `DEFECT N` / `→ Defect` / `BLOCKER` in analysis sections 1–4. Count rows in Risk Matrix. Are they equal? If not — add missing rows before output.
12. **Score Formula Accuracy:** Do the Priority counts in the Score formula exactly match the Risk Matrix row counts by band? Mismatch = recompute Score.
13. **Scope Tags:** Every Risk Matrix row has a Scope tag (API/VAL/ARCH). VAL-scoped defects without a named business rule do not exceed Priority 6.

**If you found any — output them with Priority 8-10 (Critical/Blocker).**

## Verbosity Protocol

**VERBOSITY: MINIMAL:** Minimum explanatory text. Output only tools and completion blocks.

**Communication modes:**

| Mode | When | Format |
|------|------|--------|
| **DONE** | Task completed | `✅ SKILL COMPLETE: ...` block |
| **WARNING** | Issue, but continuing | `⚠️ WARNING: [Issue]` |
| **STATUS** | Phase change | `🤖 Orchestrator Status` (only on agent/phase change) |

**No chat:**
- No "I will read the file" — just the Read tool
- No "I will now execute" — just the Bash tool
- No "The file contains..." — output goes into the completion block
- No "Successfully created..." — the completion block shows artifacts

**Exception:** On WARNING or Gardener Suggestion — explanation is mandatory.

**Decision format:** BLOCK / REJECT / PASS WITH WARNINGS / APPROVE.

**Audit report:** File only. Risk matrix, tables, and Defect details — FORBIDDEN to output in chat.

### Completion

1. Run Gardener Analysis (per `.claude/protocols/gardener.md`) → append `## 🌱 Gardener Analysis` section to the artifact file.
2. Save the full audit result to `audit/spec-audit_{SPEC_NAME}_{YYYYMMDD_HHMMSS}.md`.
3. Output `SKILL COMPLETE` block to chat only (no Risk Matrix, no Defect details, no summary table):

```text
✅ SKILL COMPLETE: /spec-audit
├─ Artifacts: audit/spec-audit_{SPEC_NAME}_{YYYYMMDD_HHMMSS}.md — **Each invocation creates a new timestamped file**
├─ Compilation: N/A
├─ Upstream: {specification file path}
├─ Specification Quality Score: X%
├─ Defects: N total (Priority 10: X, 8-9: Y, 6-7: Z, 4-5: W)
└─ Status: BLOCKED / APPROVED WITH CORRECTIONS / READY FOR DEVELOPMENT
```

Full findings → artifact file.
