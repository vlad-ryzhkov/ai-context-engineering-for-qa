---
name: output-review
description: Independent audit of any skill's output against its checklist. Use after skill completion to verify output quality. Do not use for auditing SKILL.md files themselves — use /skill-audit for that.
allowed-tools: "Read Write Edit Glob Grep Bash(./gradlew*) Bash(wc*)"
agent: agents/auditor.md
context: fork
---

# /output-review — Independent Audit of Skill Output

Verifies skill OUTPUT against checklists from the target skill's SKILL.md. Independent assessment — not the same AI context that generated the result.

## Before Starting

Read `.claude/qa_agent.md` and `.claude/agents/auditor.md`.

---

## When to Use

- Immediately after any skill completes (`SKILL COMPLETE`)
- When Score in SKILL COMPLETE seems inflated
- For independent validation before merge/release

## Input Data

| Parameter | Required | Description |
|-----------|:--------:|-------------|
| skill-name | Optional | Skill name (`/output-review api-tests`). If not specified — searches for `SKILL COMPLETE` in chat |

---

## Algorithm (7 Phases)

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the artifact (MD/HTML), not into chat.

**Chat output (constraints):**
- Brief Summary: max 5 lines (what was found, how many, result)
- Findings table: max 15 lines (top by severity)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress for each file. Checkpoint only when:
- Phase transition (Phase N → Phase N+1)
- Blocker detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, without "Now I will grep..."
- Read → analyze → report, without "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist), not a separate file.

**Phases 1-6:** Silent. **Phase 7:** Save full report to file + brief summary in chat (max 5 lines).

### Phase 1 — Target Identification

**Goal:** Determine which skill to audit.

1. If parameter `/output-review {skill-name}` is specified → use it
2. Otherwise — search for the last `SKILL COMPLETE: /{skill-name}` in chat context
3. Fallback — ask the user: "Which skill to audit?"
4. Validation: Glob `.claude/skills/{skill-name}/SKILL.md` — file MUST exist

**If skill not found → STOP:**
```text
❌ Skill /{skill-name} not found in .claude/skills/
Available: [list from Glob]
```

---

### Phase 2 — Checklist Extraction

**Goal:** Extract all checks from the target skill's SKILL.md.

1. Read `.claude/skills/{skill-name}/SKILL.md`
2. Find sections (regex by headings `##` / `###`):
   - `Self-Check` / `Definition of Done`
   - `Quality Gates`
   - `Post-*Check` (Post-Check, Post-Compilation Check, Post-Audit Check)
   - `BANNED` / `FORBIDDEN`
   - `Compilation Gate`
3. If `references/` exists → Glob `references/*.md` → read, search for checklists
4. Extract each `- [ ]` item and each numbered item from BANNED

**Output before continuing:** table `| # | Group | Item |` + line `Total: N checks.`

---

### Phase 3 — Artifact Discovery

**Goal:** Find artifacts (files) created by the skill.

**Search priority:**

1. **SKILL COMPLETE block** → line `Artifacts:` → parse file paths
2. **SKILL.md output section** → read sections "Output Format", "Output Data", "Artifacts" → Glob by expected paths
3. **Chat-only skills** (spec-audit, skill-audit, doc-lint, screenshot-analyze) → artifact = chat context + HTML/MD report if available

**For each found file — verify existence via Glob.**

**If no artifacts found:**
```text
⚠️ No artifacts detected. Evaluation will be performed based on chat context.
```

---

### Phase 4 — Evaluation

**Goal:** Independently verify each checklist item.

For each item from Phase 2:

1. **Read** the relevant artifact (file or chat context)
2. **Evaluate** compliance with the item
3. **Assign verdict:**

| Verdict | Meaning |
|---------|---------|
| ✅ PASS | Fully compliant |
| ❌ FAIL | Non-compliant — with evidence |
| ⚠️ PARTIAL | Partially compliant — describe what is wrong |
| ⏭️ SKIP | Not applicable to this context |

**Rules:**

- Each ❌ FAIL — MUST include **specific evidence**: file, line, code fragment
- BANNED items: Grep artifacts for signatures (e.g. `Thread.sleep`, `shouldBe`, `Map<String, Any>`)
- Compilation Gate: run command from SKILL.md (1 attempt). If already run — use result from chat

**FAIL example:** `❌ FAIL: assertEquals without message — file:line — Found: X / Expected: Y`

---

### Phase 5 — Anti-Pattern Scan

**Goal:** Check artifacts for anti-patterns from agents/sdet.md.

1. `ls .claude/qa-antipatterns/` — get list of anti-patterns
2. Grep artifacts for key signatures from file names (e.g. `Thread.sleep`, `Map<String, Any>`, PII)
3. If detected — read the corresponding reference file for context

**If skill is not in the Anti-Patterns table → skip this phase.**

| Verdict | Meaning |
|---------|---------|
| ✅ CLEAN | Signature not found |
| ❌ FOUND | Signature found — file:line |

---

### Phase 6 — Universal Checks

**Goal:** Checks common to all skills.

1. **SKILL COMPLETE block** — present in chat?
2. **Format** — contains 5 required fields:
   - `Artifacts`
   - `Compilation`
   - `Upstream`
   - `Coverage`
   - Skill name in the header
3. **Score < 70%** — if Coverage is specified as X/Y and X/Y < 0.7 → warning

---

### Phase 7 — Report

**Goal:** Generate the final report and save to file `audit/output-review_{skill-name}_{YYYY-MM-DD}.md`.

**If a file with this name already exists — add suffix `_2`, `_3`, etc.**

#### Results Table (by groups)

```markdown
## Output Review Report: /{skill-name}

### Self-Check / Definition of Done

| # | Item | Verdict | Comment |
|---|------|---------|---------|
| 1 | Architecture | ✅ PASS | config/, requests/, helpers/ (main) + tests (test) — present |
| 2 | assertions with message | ❌ FAIL | assertEquals without message in RegistrationApiTests.kt:45 |

### BANNED

| # | Rule | Verdict | Comment |
|---|------|---------|---------|
| 1 | Thread.sleep() | ✅ PASS | Not found |

### Anti-Patterns (agents/sdet.md)

| # | Pattern | Verdict | Comment |
|---|---------|---------|---------|
| 1 | PII in code | ✅ CLEAN | @gmail.com not found |

### Universal Checks

| # | Check | Verdict | Comment |
|---|-------|---------|---------|
| 1 | SKILL COMPLETE block | ✅ PASS | Present |
| 2 | 5 format fields | ⚠️ PARTIAL | Missing Coverage |
```

#### Scorecard

```text
Score = PASS / (PASS + FAIL) × 100

Formula: {N_pass} + {N_partial}×0.5 / ({N_pass} + {N_partial}×0.5 + {N_fail}) × 100
SKIP not counted.
```

**Example:**
```text
Scorecard: 12 PASS + 1 PARTIAL×0.5 / (12 + 0.5 + 2) × 100 = 86%
```

#### Discrepancies with Skill's Post-Check

If the skill already performed Post-Check in chat — compare:
- Where self-review disagrees with the skill's assessment
- Format: `Item X: skill → ✅, self-review → ❌ (reason)`

#### Artifact Recommendations

Specific actions to resolve each FAIL in the artifact:

```markdown
### Artifact Recommendations

1. **[FAIL]** assertEquals without message in RegistrationApiTests.kt:45
   → Add message: `assertEquals(200, response.code, "Registration should return 200")`

2. **[PARTIAL]** Coverage not specified in SKILL COMPLETE
   → Add line `├─ Coverage: X/Y tests`
```

#### Skill Improvement Recommendations

**Goal:** Based on found FAIL and PARTIAL items — propose specific edits to `.claude/skills/{skill-name}/SKILL.md`, so the next skill run does not reproduce the same errors.

Logic: each FAIL/PARTIAL is a symptom of a missing or unclear rule in the skill.

```markdown
### Skill Improvement Recommendations for /{skill-name}

| # | Problem in artifact | Cause (rule is missing) | Recommended rule in SKILL.md |
|---|---------------------|-------------------------|------------------------------|
| 1 | Two HTTP codes in one Expected Result (`201 OR 400`) | No explicit atomicity requirement | Add to Protocol: "Each scenario contains exactly 1 Expected Result. FORBIDDEN to use `X OR Y` in a single line." |
| 2 | L10N scenarios missing | No explicit Coverage Matrix for L10N | Add to Coverage Matrix: `L10N: {EMOJI_STRING}, {SPECIAL_CHARS}` as required types, unless the spec explicitly prohibits them. |
```

**Recommendation generation rules:**
- Only for FAIL and PARTIAL (ignore PASS and SKIP)
- Recommendation MUST be specific: what exactly to add/change in SKILL.md and in which section
- Formulate as a rule/prohibition, not a suggestion
- If >5 FAIL found — group by theme, do not list each one separately
```text
```

---

## Constraints

- Save full report to `audit/output-review_{skill-name}_{YYYY-MM-DD}.md`, brief summary (5 lines) — in chat
- Compilation Gate: maximum 1 attempt (do not fix code, only record the result)
- Do not fix artifacts — only document findings
- If artifact is too large (>500 lines) — check by key patterns, do not read entirely

---

## Post-Review Check (inline before SKILL COMPLETE)

- [ ] All extracted checklist items verified?
- [ ] Each FAIL contains specific evidence (file:line)?
- [ ] BANNED items checked via Grep, not visually?
- [ ] Scorecard calculated with formula shown (numerator/denominator)?
- [ ] No false FAILs (context of each finding re-verified)?
- [ ] Full report saved to `audit/output-review_{skill-name}_{YYYY-MM-DD}.md`?

**If you found an error in the audit → fix it.**

---

### Completion

After Post-Review Check — print `SKILL COMPLETE` block (format in qa_agent.md § Skill Completion Protocol).
