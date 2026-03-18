## ACE Pipeline — Continuous Improvement

After completing any task:

1. Analyze what rules were missing (Gardener Protocol)
2. Append observations to `.ai-lessons/pending.md`
3. On failure: formulate 1 root-cause rule (Reflection Protocol)

When `.ai-lessons/pending.md` has >=3 entries, run /curate-lessons to promote rules.

Delta Update Protocol: Use Edit (surgical replace), never Write (full overwrite) on context files.

### Gardener Output Format

```text
GARDENER ANALYSIS
| # | Observation | Proposed rule | Section | Target file |
```

If no proposals: "GARDENER: no proposals for this run"

### Target File Selection

- Skill-specific rule -> `skills/{name}/SKILL.md`
- Global QA pattern -> `qa-antipatterns/{category}.md`
- Cross-cutting rule -> `.ai-lessons/pending.md`

### Reflection (on failure only)

- Identify root cause (not symptom)
- Formulate exactly 1 rule
- Dedup check before appending to pending.md
