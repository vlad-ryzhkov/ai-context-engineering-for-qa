# Output Formatting — Patch Cards and Tables

Used by Step 8 (Format Patches).

## Patch Card Format

Display top-3 patches first in this format:

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PATCH 1 — ORPHAN-USEFUL (token waste: 0, structural)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

FILE:       docs/architecture/core-test-runtime.md
LINES:      342
TOKENS:     ~2,563
SEVERITY:   HIGH — domain knowledge, orphaned

FINDING:
  Not linked from any entry point. Contains shared testing patterns with
  docs/agents/architecture/test-lifecycle.md. Should be linked from
  rules/testing.md or extracted as docs/common/test-runtime.md.

PLACEMENT OPTIONS:
  [1 SKIP]  — Keep as new file docs/test-runtime.md (no linking cost)
  [2 SKIP]  — Link from rules/testing.md (adds 1 line)
  [3 RECOMMENDED] — Add JIT pointer in agents.md under "Test Lifecycle"
                     Reason: already related entry point, minimal footprint

ACTION:
  (y/1) Apply patch 1  |  (a) Show all N  |  (n) Next  |  (q) Quit

───────────────────────────────────────────────────────────────────────

PATCH 2 — ROUTING-AMBIGUITY (token waste: ↑6,077)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

FILES:      docs/agents/tdd-manual.md, docs/agents/tdd-auto.md
OVERLAP:    28 lines (Red/Green/Refactor cycle shared)
IMPACT:     "write a test" loads BOTH docs (ambiguous trigger in agents.md)

FINDING:
  Triggers "write a test" and "create test case" both match the phrase
  "write a test". User request ambiguity → agent loads both documents,
  wasting 6,077 tokens. Real examples:
    - "write a test for X" → matches both [1] and [2]
    - "automated test" → only [1]
    - "manual test case" → only [2]

PATCH — Add Disambiguation Guard in agents.md:
  OLD:  "Write a test" → see docs/agents/tdd-manual.md
  NEW:  "Write a test" → "Automated (unit/integration)" → auto.md
                      OR "Manual (QA/acceptance)" → manual.md

PLACEMENT OPTIONS:
  [1 RECOMMENDED] — Add guard in agents.md (adds 3 lines, removes ambiguity)
  [2 SKIP]        — Extract shared Red/Green/Refactor to docs/common/tdd-rgr.md
                     + link from both (adds 2 lines per file)

ACTION:
  (y/1) Apply patch 2  |  (a) Show all N  |  (n) Next  |  (q) Quit

───────────────────────────────────────────────────────────────────────

PATCH 3 — DUPLICATION (token waste: ↑1,234)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

FILES:      rules/git.md (L187–205), rules/branches.md (L42–60)
OVERLAP:    19 lines (branch hygiene + commit discipline shared)
IMPACT:     Readers must maintain both

FINDING:
  Shared sections on commit-before-switch pattern. Could extract to
  docs/git-workflow-checkpoints.md with brief pointers from each rules/ file.

PATCH:
  1. Create docs/git-workflow-checkpoints.md (19 lines of shared content)
  2. Replace both sections with "→ Read docs/git-workflow-checkpoints.md"
  3. Save ~38 lines in rules/, keep invariants in place

PLACEMENT OPTIONS:
  [1 RECOMMENDED] — Extract to docs/, leave 1-line Read pointer in each rules/ file
  [2 SKIP]        — Keep as-is (duplication acceptable for fast reference)

ACTION:
  (y/1) Apply patch 3  |  (a) Show all N  |  (n) Next  |  (q) Quit
```

## Output Rules

1. **Top-3 first** — sort by impact (token waste > orphan severity > duplication > routing-ambiguity > novelty)
2. **One patch per card** — never collapse multiple findings into one action
3. **PLACEMENT OPTIONS** — include all viable zones; mark 1–2 as RECOMMENDED with reasoning
4. **Before/after token count** — show ∆ in patch impact section
5. **Example commands** — when filing an orphan, show concrete `Edit` anchors:

   ```text
   ADD this line to agents.md after "## Testing":
     → Read docs/architecture/core-test-runtime.md
   ```

6. **On "a / show all N"** — reveal rest, same format; no truncation of lower-ranked patches

## Dependency Table (Step 2)

```markdown
| Type            | File                                                             | Lines | ~Tokens | Trigger / Note                         |
| --------------- | ---------------------------------------------------------------- | ----- | ------- | -------------------------------------- |
| ENTRY           | docs/agents/agents.md                                            | 427   | 4,356   | symlink: CLAUDE.md AGENTS.md GEMINI.md |
| JIT             | docs/agents/development/v1/auto-test.md                          | 503   | 3,817   | "Automated Testing"                    |
| JIT↳            | docs/agents/architecture/test-lifecycle.md                       | 118   | 1,801   | CoreTestCase / JUnit ext               |
| COND            | docs/config/crowdin-translations.md                              | 305   | 9,606   | CrowdinHelper usage                    |
| ORPHAN-USEFUL   | docs/architecture/core-test-runtime.md                           | 342   | 2,563   | not linked from entry point            |
| ORPHAN-ARTIFACT | docs/agents/agents.original.md                                   | 256   | 1,920   | backup, delete                         |
| DUPLICATE       | docs/reports/crowdin-migration/ ↔ docs/crowdin-migration-report/ | —     | —       | same content                           |
```

**Key:** `JIT↳` = loaded only when parent JIT doc is also loaded (depth-2 conditional).

## Token Budget Table (Step 2)

```markdown
| Load scenario            | Chars now | ~Tokens now | % of 200k | ~Tokens after patches              | Delta  |
| ------------------------ | --------- | ----------- | --------- | ---------------------------------- | ------ |
| ENTRY (always-loaded)    | 17,424    | 4,356       | 2.2%      | 2,200 (after tutorial extraction)  | −2,156 |
| Ambiguous "write a test" | 48,614    | 12,154      | 6.1%      | 6,077 (after disambiguation guard) | −6,077 |
| Worst case               | 87,036    | 21,759      | 10.9%     | 15,000 (est.)                      | −6,759 |
```

All improvements shown in "after patches" column. Never assume patches will be applied; this column is conditional ("IF patch applied").

## Drift Score — one number per context file

A count of findings is not comparable across files: a 40-path entry point and a
7-path rule are different animals. Report a RATIO, with an absolute floor so a
handful of dead addresses still rings in a large file.

```text
broken_rate = max(broken / resolved,        min(broken / 6, 1.0))
moved_rate  = max(moved  / resolved,        min(moved  / 6, 1.0))
orphan_rate = orphans / docs_files
stale_rate  = min(lag_days / 60, 1.0) × max(min(churn / 20, 1.0), 0.25)   # 0 when churn == 0
dup_rate    = duplicated_lines / total_lines

drift = 100 × (0.34×broken_rate + 0.20×moved_rate + 0.16×orphan_rate
               + 0.13×stale_rate + 0.10×dup_rate + 0.07×ambiguous_rate)
```

Churn 0 → halve the result: a frozen corpus must not ring forever.

| Band | Score | Reading |
| --- | --- | --- |
| fresh | <20 | no action |
| drifting | 20–39 | fix on the next pass |
| stale | 40–59 | patch this run |
| broken | ≥60 | the entry point is actively misleading the agent |

Print one line per file above the patch cards, worst first, and record the number in
`docs/context-map.md` so the NEXT audit can compare instead of re-deriving:

```text
CLAUDE.md          drift 27.3 [drifting]  lag 3d · 1157 changes since
rules/promptfoo.md drift  8.1 [fresh]     lag 0d · 0 changes since
```

Worked single-patch-card example → `references/patch-card-example.md`.
