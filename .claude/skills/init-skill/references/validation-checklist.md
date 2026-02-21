# New Skill Validation Checklist

## Structure

- [ ] File is named exactly `SKILL.md` (case-sensitive)
- [ ] Directory is in kebab-case, matches the `name` field
- [ ] No README.md inside the skill directory
- [ ] **SKILL.md ≤ 500 lines** (if larger — split into references/)

## YAML Frontmatter

- [ ] Field `name` is present and in kebab-case
- [ ] Field `name` DOES NOT contain "claude" or "anthropic"
- [ ] YAML description < 1024 characters
- [ ] Description contains **What** + **When**
- [ ] Description has no XML tags (`<`, `>`)

## Content

- [ ] Has a "When to Use" section
- [ ] Steps are numbered and specific
- [ ] Style is imperative (no "you should", "it is recommended")
- [ ] Links to resources point to actual files
- [ ] Has an output example
- [ ] Has Quality Gates
- [ ] Large files extracted into references/

## If SKILL.md > 500 Lines

```text
Skill is too large ([N] lines > 500).

I suggest extracting:
1. Code examples → references/examples.md
2. Checklists → references/checklist.md
3. Tables → references/tables.md

Split? (yes / keep as is)
```
