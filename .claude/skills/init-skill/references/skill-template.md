# SKILL.md — Tool Skill Template

> **Purpose:** Instructions for a typical task. Need to test an API — pulls out the "How to write API tests" instructions. Need to check UI — picks up another tool.

## ⚠️ Size Limit: ≤500 lines

**SKILL.md MUST NOT exceed 500 lines.** If larger — split into:
- `references/*.md` — examples, tables, checklists
- `scripts/*.py` — executable code
- `.claude/qa-antipatterns/*.md` — anti-patterns (common to all skills)

---

## Template

```markdown
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
```

---

## Progressive Disclosure

```
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

```
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

```
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

## Full Guide

`docs/ai-files-handbook.md` → Part 3: Skills
