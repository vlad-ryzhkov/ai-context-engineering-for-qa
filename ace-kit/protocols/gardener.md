# Gardener Protocol — Continuous Improvement

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
GARDENER ANALYSIS
| # | Observation | Proposed rule | Section | Target file |
|---|-------------|---------------|---------|-------------|
| 1 | {what happened} | {specific prohibition/rule} | {Protocol/BANNED/Quality Gates/...} | skills/{name}/SKILL.md |
```

If no proposals:

```text
GARDENER: no proposals for this run
```

## Where to Output

| Skill artifact type                           | Action                                                                |
| --------------------------------------------- | --------------------------------------------------------------------- |
| Markdown report (`.md`)                       | Append section `## Gardener Analysis` to the end of the artifact file |
| Code (`.kt`, `.go`, etc.)                     | Output to chat (do not add to code)                                   |
| Config/init file (`CLAUDE.md`, `qa_agent.md`) | Output to chat                                                        |
| No file (chat-only skill)                     | Output to chat                                                        |

**Markdown skills** (append to artifact): Any skill that produces a `.md` artifact file.

**Target file selection:**

- Rule is skill-specific (checklist step, output format) → `skills/{name}/SKILL.md`
- Rule is a global QA pattern (assertion, test structure, async) → `qa-antipatterns/{category}.md`
- Rule is cross-cutting (applies to multiple skills, not captured by existing antipatterns) → `.ai-lessons/pending.md`

## Structured Logging (after generating the table)

1. **JSONL persistence:** For each observation row in the Gardener table, append a JSONL line to `.ai-lessons/gardener-log.jsonl`:

   ```json
   {
     "ts": "2026-03-18T10:00:00Z",
     "skill": "api-tests",
     "observation": "Thread.sleep used in test body",
     "proposed_rule": "BANNED: Thread.sleep — use Awaitility",
     "target_file": "skills/api-tests/SKILL.md"
   }
   ```

   Use ISO 8601 UTC timestamp. One line per observation. Append only — never rewrite the file.

2. **Telemetry recording:** After the Gardener table (and JSONL logging), execute:
   ```bash
   bash scripts/hooks/telemetry-hook.sh --skill {name} --status {complete|partial|loop_guard} --gardener-count {N} [--error-type {type}]
   ```
   Where `{N}` is the number of Gardener observation rows (0 if "no proposals").

## Generation Rules

- Formulate as a prohibition or specific requirement, not as a wish
- Only if the rule is **missing** from SKILL.md — do not duplicate existing rules
- If >5 observations — group by topic (max 5 rows in the table)
- Do not apply independently — suggestion only, the user decides

## Examples: Good vs. Bad Observations

| Quality | Observation                                                | Proposed rule                                                                       | Why                                                                |
| ------- | ---------------------------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| Good    | `validateBodyContains` used in test body instead of helper | BANNED: call `validateBodyContains` directly in `@Test` — extract to `@Step` helper | Specific, actionable, maps to an existing convention               |
| Good    | `Thread.sleep(2000)` appeared in generated test            | BANNED: `Thread.sleep` — use Awaitility polling instead                             | Missing from BANNED at time of run; prevents recurrence            |
| Bad     | "Consider adding more assertions"                          | — (too vague)                                                                       | Not a prohibition or specific requirement — a wish                 |
| Bad     | "Tests should be readable"                                 | — (not actionable)                                                                  | Already implied by ktlint + existing rules; adds no new constraint |

**Noise filter:** Before adding a row, ask — "Would this rule, written exactly as proposed, prevent the same mistake on the next run?" If no → drop it.
