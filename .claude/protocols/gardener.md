# 🌱 Gardener Protocol — Continuous Improvement

Runs **MANDATORY** at the end of every skill, BEFORE the `SKILL COMPLETE` block.

## Algorithm

1. Read `.claude/skills/{current-skill}/SKILL.md`
2. Analyze the current run:
   - What problems/deviations were found in the artifact?
   - Which algorithm step was ambiguous or required interpretation?
   - Is there an error pattern not covered by explicit SKILL.md rules?
3. For each observation: is the rule **missing** from SKILL.md? → include in the table

## Output Format (mandatory always)

```text
🌱 GARDENER ANALYSIS
| # | Observation | Proposed rule | Section | Target file |
|---|-------------|---------------|---------|-------------|
| 1 | {what happened} | {specific prohibition/rule} | {Protocol/BANNED/Quality Gates/...} | skills/{name}/SKILL.md |
```

If no proposals:
```text
🌱 GARDENER: no proposals for this run
```

## Where to Output

| Skill artifact type | Action |
|---------------------|--------|
| Markdown report (`.md`) | Append section `## 🌱 Gardener Analysis` to the end of the artifact file |
| Code (`.kt`, `.go`, etc.) | Output to chat (do not add to code) |
| Config/init file (`CLAUDE.md`, `qa_agent.md`) | Output to chat |
| No file (chat-only skill) | Output to chat |

**Markdown skills** (append to artifact): `test-cases`, `spec-audit`, `output-review`, `repo-scout`, `doc-lint`, `skill-audit`

## Generation Rules

- Formulate as a prohibition or specific requirement, not as a wish
- Only if the rule is **missing** from SKILL.md — do not duplicate existing rules
- If >5 observations — group by topic (max 5 rows in the table)
- Do not apply independently — suggestion only, the user decides
