# YAML Frontmatter Reference

## Required Fields

### name

- **Format:** kebab-case
- **Constraints:**
  - Lowercase letters, digits, hyphens only
  - MUST match the skill folder name
  - No "claude", "anthropic" prefixes
  - Unique within the project
- **Examples:**
  - ✅ `test-cases`, `api-tests`, `screenshot-analyze`
  - ❌ `TestPlan`, `api_tests`, `claude-helper`

### description

- **Format:** `[What it does]. [When to use]. [When NOT to use]`
- **Constraints:**
  - Maximum 1024 characters
  - No XML tags (<>, &lt;, &gt;)
  - No line breaks (single line)
  - Use trigger phrases from usage examples
- **Structure:**
  1. What it does (1-2 sentences)
  2. When to use (specific scenarios)
  3. When NOT to use (anti-use-cases)
- **Examples:**
  - ✅ `Generates test cases from API specification. Use after /spec-audit for endpoint test coverage. Do not use for UI testing.`
  - ❌ `A useful tool for testing` (too generic)

## Optional Fields

### allowed-tools

- **Format:** Space-separated string
- **Examples:**
  - `"Read Write Edit Glob Grep"`
  - `"Read Write Bash(wc*) Bash(git*)"`
- **Wildcards:** Bash commands can be restricted by pattern: `Bash(ls*)` allows only `ls`

### agent

- **Format:** Path to agent file relative to `.claude/`
- **Example:** `agents/sdet.md`, `agents/auditor.md`

### context

- **Options:**
  - `fork` — isolated context (Process Isolation)
  - `inherit` — inherited context (default)

## Ready-made YAML Examples

### Analysis Skill

```yaml
---
name: spec-audit
description: Audits OpenAPI/Proto specification for completeness and correctness. Use before /testcases for endpoint validation. Do not use for code review.
allowed-tools: "Read Write Edit Glob Grep"
agent: agents/auditor.md
context: fork
---
```

### Generation Skill

```yaml
---
name: api-tests
description: Generates Kotlin automated tests from test cases. Use after /testcases for automation. Do not use without prepared test cases.
allowed-tools: "Read Write Edit Glob Grep Bash"
agent: agents/sdet.md
context: fork
---
```

### Validation Skill

```yaml
---
name: lint-tests
description: Validates automated tests against standards. Use after /api-tests for quality control. Do not use for production code.
allowed-tools: "Read Glob Grep"
agent: agents/auditor.md
context: fork
---
```

## Validation

After writing YAML, verify:
- [ ] `name` = skill directory name
- [ ] `description` contains all 3 parts (what/when/not when)
- [ ] `description` < 1024 characters
- [ ] No XML characters in `description`
- [ ] YAML is syntactically correct (triple-dash opening and closing)
