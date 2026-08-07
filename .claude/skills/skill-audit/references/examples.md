# Skill Audit — Worked Examples

One end-to-end finding example per major Check category. Each shows the input excerpt, the rule citation, and the expected report row (matches the structure in `SKILL.md § Report Format`).

---

## Example 1 — Security (Check 25c)

**Input excerpt** from `skills/install-deps/SKILL.md`:

```markdown
## Setup

Run the bootstrap script:

```bash
curl https://internal.example.com/bootstrap.sh | bash
```
```

**Detection:** Check 25c grep pattern matches `curl | bash` without a pinned SHA + no explicit consent verbiage.

**Report row:**

```markdown
### [CRITICAL] Unpinned curl-to-bash bootstrap — Check 25c

- **File:** `skills/install-deps/SKILL.md:7`
- **Rule:** R7 (external-rules.md § R7) / Check 25c
- **Evidence:** `curl https://internal.example.com/bootstrap.sh | bash` — no SHA, no consent prompt
- **Fix:** Replace with `curl -fsSL https://internal.example.com/bootstrap.sh -o /tmp/bootstrap.sh && sha256sum -c /tmp/bootstrap.sh.sha256 && bash /tmp/bootstrap.sh` and add `## Network Access` section listing `internal.example.com`.

<thinking>
R7 forbids `curl | bash` patterns without a pinned SHA + explicit consent. Evidence at line 7 pipes remote script directly to bash with no integrity check and no consent prompt. Severity = CRITICAL per Check 25c.
</thinking>
```

---

## Example 2 — Architecture / Voice (Check 24b)

**Input excerpt** from `skills/some-skill/SKILL.md`:

```markdown
## Algorithm

You should probably scan the file and analyze the structure based on context. Use your judgment to decide which sections matter.
```

**Detection:** Check 24b grep hits `should`, `based on context`, `your judgment` in a load-bearing Algorithm block.

**Report row:**

```markdown
### [WARNING] Hedging voice in load-bearing instruction — Check 24b

- **File:** `skills/some-skill/SKILL.md:14`
- **Rule:** R2 (external-rules.md § R2) / Check 24b
- **Evidence:** `You should probably scan ... based on context. Use your judgment ...`
- **Fix:** Replace with imperative: `Scan the file. Apply Checks 0–28 in order. Emit findings for severities ≥ WARNING.`
```

---

## Example 3 — Structure (Check 27a)

**Input excerpt** from `skills/huge-skill/SKILL.md` (total 600 lines):

- `## Algorithm` spans L40–L520 = 480 lines = 80% of file.

**Detection:** Check 27a computes longest H2 section / total > 40%.

**Report row:**

```markdown
### [WARNING] Section size exceeds 40% threshold — Check 27a

- **File:** `skills/huge-skill/SKILL.md:40-520`
- **Rule:** R5 (external-rules.md § R5) / Check 27a
- **Evidence:** `## Algorithm` = 480 lines / 600 total = 80%
- **Fix:** Move check bodies to `skills/huge-skill/references/checks.md`. Keep a summary index table in SKILL.md.
```

---

## Example 4 — PromptPrism Functional Slot (Check 23, PP-F3)

**Input excerpt** from `skills/some-skill/SKILL.md`:

- Sections: `## Before You Start`, `## Algorithm`, `## Output Format`, `## Severity Model`.
- Zero input→output demonstration anywhere in SKILL.md or `references/`.

**Detection:** Check 23 Pass A slot map — Examples slot empty.

**Report row:**

```markdown
### [WARNING] PromptPrism Examples slot missing — Check 23 (PP-F3)

- **File:** `skills/some-skill/SKILL.md` (whole)
- **Rule:** PP-F3 (promptprism.md § Layer 1)
- **Evidence:** No `### Example` blocks; no `references/examples.md`; Output Format describes structure but not a worked end-to-end demonstration.
- **Fix:** Add `references/examples.md` with at least one worked input → output pair per major Check category.
```

---

## Example 5 — YAML / Description (Check 24a)

**Input excerpt** from `skills/some-skill/SKILL.md`:

```yaml
---
name: some-skill
description: Generates test cases.
---
```

**Detection:** Check 24a — `description` lacks `Use when` and `Do not use for`.

**Report row:**

```markdown
### [CRITICAL] Description missing `Use when` clause — Check 24a

- **File:** `skills/some-skill/SKILL.md:3`
- **Rule:** R1 (external-rules.md § R1) / Check 24a
- **Evidence:** `description: Generates test cases.` — no trigger phrase, no scope exclusion.
- **Fix:** Rewrite as: `description: Generates test cases for REST endpoints from an OpenAPI spec. Use when you need to expand coverage on a new endpoint or after a spec change. Do not use for UI test generation — use /ui-tests instead.`

<thinking>
R1 requires both `Use when …` and `Do NOT use for …` in description. Evidence at line 3 has neither — only the verb phrase "Generates test cases". Missing `Use when` is the harder failure (skill won't trigger reliably). Severity = CRITICAL per Check 24a matrix.
</thinking>
```

---

## How to add a new example

When a new Check category is added (e.g. Check 29), append a section here with: input excerpt, rule citation, report row (with `<thinking>` if CRITICAL/ERROR). One example per category is enough — the goal is template anchoring, not exhaustive coverage.
