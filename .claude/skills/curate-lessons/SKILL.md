---
name: curate-lessons
description: Curates pending lessons from .ai-lessons/pending.md into context files. Use when pending.md has ≥3 entries to deduplicate and promote lessons. Do not use to create rules from scratch.
allowed-tools: "Read Edit Glob Grep"
---

# /curate-lessons — Lesson Curation

Curates lessons from `.ai-lessons/pending.md`, deduplicates against existing rules, and promotes
confirmed patterns into target context files.

## When to Use

Run when `.ai-lessons/pending.md` has ≥ 3 entries.

---

## Phase Checkpoints

**STOP if:**
- `.ai-lessons/pending.md` does not exist → output `⚠️ curate-lessons: .ai-lessons/pending.md not found. Create it first.` and STOP
- Fewer than 3 entries in `pending.md` → output count warning and STOP

**WARN if:**
- A rule has no `Source:` or `Date:` metadata → tag as unverified, proceed with caution

**INFORM:**
- Phase transitions only (no per-rule progress lines)

---

## SILENT MODE

Output only phase transitions and the final SKILL COMPLETE block. No intermediate progress lines.

---

## Phase 1: Load Pending Lessons

Read `.ai-lessons/pending.md`. Extract all `RULE:` entries with source and date metadata.

If file is empty or has < 3 entries → output:

```text
⚠️ curate-lessons: only {N} pending entries — minimum 3 required. Run again when more lessons accumulate.
```

and STOP.

---

## Phase 2: 2-Pass Hybrid Dedup

**Pass 1 — Grep Narrowing:** For each pending rule, extract 2–3 keywords. Run Grep across dedup targets:
1. `CLAUDE.md`
2. `.claude/qa-antipatterns/**/*.md`
3. `.claude/skills/*/SKILL.md`
4. `.claude/protocols/*.md`

Collect only files with keyword hits → candidate set.

**Pass 2 — Semantic Comparison (candidates only):** Read each candidate file. Compare rule intent semantically.

Assign one of 4 verdicts:

| Verdict | Meaning | Action |
|---------|---------|--------|
| `DUPLICATE` | Identical or semantically equivalent rule already exists | Skip — do not promote |
| `OVERLAP` | Partial overlap with existing rule | Flag for user — suggest merging |
| `UNIQUE` | No coverage in any target file | Promote |
| `META` | Self-referential: about curation/gardener process | Target: `curate-lessons/SKILL.md` or `protocols/gardener.md`, NOT `CLAUDE.md` |

---

## Phase 3: Curation Report — STOP for Approval

Generate and show the curation table to the user:

| # | Rule (excerpt) | Verdict | Target file |
|---|----------------|---------|-------------|
| 1 | … | UNIQUE | `CLAUDE.md` |
| 2 | … | DUPLICATE | — (skip) |
| 3 | … | META | `protocols/gardener.md` |

**STOP — wait for user approval before proceeding to Phase 4.**

Output:

```text
📋 Curation Report above. Approve to promote {N} rules (Y/n)?
```

Do not proceed to Phase 4 until user confirms.

---

## Phase 4: Delta Update Protocol

For each approved `UNIQUE` or `META` rule:

**Routing:**

| Rule type | Target |
|-----------|--------|
| Global convention (applies to all skills) | `CLAUDE.md` |
| Skill-specific (checklist step, output format) | `.claude/skills/{name}/SKILL.md` |
| QA code pattern (assertion, test structure, async) | `.claude/qa-antipatterns/{category}.md` |
| Self-referential (about curation/gardener process) | `.claude/skills/curate-lessons/SKILL.md` or `.claude/protocols/gardener.md` |

**Rules:**
- Use `Edit` only — NEVER `Write` on governed files (`CLAUDE.md`, `SKILL.md`, `protocols/`)
- Append rule to the appropriate section (BANNED / Quality Gates / Anti-patterns)
- Remove promoted entry from `.ai-lessons/pending.md` using `Edit` (not `Write`)
- Add entry to `.ai-lessons/graduated.md`: `{date} | {rule excerpt} | → {target}`

For `OVERLAP` rules — output suggestion to user; do not auto-modify.

---

## Phase 5: Frequency Index Update (Optional)

Check if `.claude/qa-antipatterns/_index.md` has a `Freq` column.
- If yes → increment frequency counter for promoted antipattern categories
- If no → skip silently

---

## Quality Gates

| Gate | Criteria |
|------|----------|
| All pending entries processed | Every entry has DUPLICATE / OVERLAP / UNIQUE / META verdict |
| No wrong-target promotion | Skill-specific rules NOT added to global antipatterns |
| pending.md cleaned | Promoted entries removed from `.ai-lessons/pending.md` |
| graduated.md updated | Promotion log has date + target for each promoted rule |
| Phase 3 and Phase 4 separate | User approval received before any file modification |

**Gardener:** Run `.claude/protocols/gardener.md` before SKILL COMPLETE.

---

## BANNED

| Pattern | Why |
|---------|-----|
| Promoting a rule that already exists in any target file | Creates duplicate constraints, inflates token budget |
| Bulk-promoting all pending rules without dedup check | Poisons target files with noise |
| Leaving `.ai-lessons/pending.md` unchanged after promotion | Pending file grows unbounded |
| Using `Write` on governed files (`CLAUDE.md`, `SKILL.md`, `protocols/`) | Destroys existing content — use `Edit` only |
| Phase 3 and Phase 4 in the same turn | Removes user approval checkpoint — safety violation |
| Routing META rules to `CLAUDE.md` | META rules are self-referential — target `curate-lessons/SKILL.md` or `protocols/gardener.md` |

---

## Completion

```text
✅ SKILL COMPLETE: /curate-lessons
├─ Artifacts: .ai-lessons/graduated.md (updated)
├─ Compilation: N/A
├─ Upstream: .ai-lessons/pending.md
└─ Coverage: {promoted}/{total} rules promoted, {N} duplicates skipped
```
