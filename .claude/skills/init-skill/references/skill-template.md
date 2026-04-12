# SKILL.md — Tool Skill Template

> **Purpose:** Instructions for a typical task. Need to test an API — pulls out the "How to write API tests" instructions. Need to check UI — picks up another tool.

## ⚠️ Size Limit: ≤500 lines

**SKILL.md MUST NOT exceed 500 lines.** If larger — split into:
- `references/*.md` — examples, tables, checklists
- `scripts/*.py` — executable code
- `.claude/qa-antipatterns/*.md` — anti-patterns (common to all skills)

---

## Template

````markdown
---
description: [Verb] + [what] + [context]. Max 100 characters.
---

# /[skill-name] — [Title]

<purpose>
[1-2 sentences: what it does and for whom]
</purpose>

## When to Use
- [Trigger 1]
- [Trigger 2]

## Input
- [What is needed from the user]

## Algorithm

### Step 1: [Title]
[Specific actions]

### Step 2: [Title]
[Specific actions]

### Step N: [Title]
[Specific actions]

## Output Format

```[language]
[Result template]
```

## Quality Gates

- [ ] [Check 1]
- [ ] [Check 2]

## Related Files (optional)

- `scripts/[name]` — [purpose]
- `references/[name]` — [purpose]
````

---

## Progressive Disclosure

```text
┌─────────────────────────────────────────────────────────┐
│ Level 1: YAML header                                    │
│ → Always in the system prompt (< 100 characters)        │
├─────────────────────────────────────────────────────────┤
│ Level 2: SKILL.md body                                  │
│ → Loaded on skill activation                            │
├─────────────────────────────────────────────────────────┤
│ Level 3: scripts/ and references/                       │
│ → Loaded on explicit request                            │
└─────────────────────────────────────────────────────────┘
```

---

## Skill Directory Structure

```text
.claude/skills/{skill-name}/
├── SKILL.md              # Levels 1-2: header + instructions (≤500 lines)
├── scripts/              # Level 3: executable code (optional)
│   ├── generate.py
│   └── validate.sh
└── references/           # Level 3: documentation (optional)
    ├── checklist.md
    └── examples.json
```

### Real Example: screenshot-analyze

```text
.claude/skills/screenshot-analyze/
├── SKILL.md                    # 335 lines — core logic
└── references/
    ├── cldr-tables.md          # CLDR references (currencies, numbers, time)
    ├── checklists.md           # Full checklists
    └── html-template.md        # HTML report template
```

**Before:** 1031 lines in a single file
**After:** 335 lines core + 3 reference files

**Result:** AI loads only what is needed on demand

---

## Description Examples

**Good:**
```yaml
description: Generates API automated tests in Kotlin with common-test-libs and JUnit 5
description: Analyzes specification for contradictions and gaps
description: Validates tests for naming convention compliance
```

**Bad:**
```yaml
description: This skill is designed for...  # too long
description: Helps with testing             # too abstract
description: API tests                       # no verb
```

---

## Skill Categories

| Category | Examples | Typical Output |
|----------|---------|----------------|
| **Analysis** | /spec-audit, /security-audit | Report with findings |
| **Generation** | /testcases, /api-tests | Code or document |
| **Validation** | /lint-tests, /check-coverage | Pass/Fail + details |
| **Transformation** | /openapi-to-tests | Format conversion |

---

---

## Tier 1 Baseline Injection

Every new skill MUST include these baseline features. Use exact blocks from `.claude/protocols/` or inject these templates:

### Block A — SILENT MODE

Place immediately after the main heading or inside Phase 1.

```markdown
> **SILENT MODE**: Execute all analytical and generation phases silently. Do not output
> intermediate reasoning or conversational filler. Only the final SKILL COMPLETE block
> (or an explicit ESCALATION if blocked) goes to chat.
```

### Block B — Quality Gate + Gardener

Place immediately before the SKILL COMPLETE block. Adapt the 6 checklist items to your skill.

```markdown
### Quality Gate (Self-Review)

Before finalizing, verify internally:
- [ ] Primary task completed without skipping phases.
- [ ] Output adheres exactly to the required format.
- [ ] All input files scanned / all use cases covered.
- [ ] No hardcoded URLs, secrets, or PII in generated code.
- [ ] Assertions include explicit messages (.as() / .shouldBe()).
- [ ] {Skill-specific item, e.g. "Code compiles" / "All Allure steps annotated"}

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified
missing rules or inefficiencies during this run, output a brief proposal table.
Otherwise: `🌱 Gardener: No updates needed.`
```

### Block B-Lite — for Micro-Skills (/fix-markdown, /pr)

```markdown
> **Gardener**: If you noticed rule drift during this run, briefly note it here.
```

### Block C — Loop Guard

Use for code-generating skills (/api-tests, /api-mocks, /api-isolated-tests).

```markdown
> **Loop Guard**: If you encounter the same error or validation failure twice in a row,
> do NOT attempt a third blind fix. Output an ESCALATION block with the failure details
> and wait for user instruction.
```

### Block D — Status Reporting

Use for multi-file scan skills (doc-lint, agents-checker, skill-audit).

```markdown
> **Status Reporting**: After processing each batch, emit a single-line JSON progress:
> `{"agent": "skill-name", "phase": N, "files_scanned": N, "issues_found": N, "remaining": N}`
```

### Block E — Verification Gate

Use for ALL skills. Place immediately before SKILL COMPLETE block, after Quality Gate.

```markdown
> **Verification Gate**: Before writing SKILL COMPLETE, execute the proof command
> (build, test, lint, file existence check). Read the FULL output + exit code.
> If output does not positively confirm the claim → SKILL PARTIAL, not SKILL COMPLETE.
>
> **Red flags** (restart verification): "should work", "probably fine",
> "I already tested earlier", "confident it works", "just this once".
```

**Rationalization prevention** (add to anti-patterns table if skill has one):

| Rationalization | Counter |
|---|---|
| "Should work now" | Run it. "Should" is not evidence. |
| "I'm confident" | Confidence is not verification. Execute the proof. |
| "Just this once" | No exceptions. Every completion needs fresh evidence. |
| "Already tested earlier" | Prior runs are stale. Re-verify with current state. |
| "It's a simple change" | Simple changes cause sneaky bugs. Verify. |

---

