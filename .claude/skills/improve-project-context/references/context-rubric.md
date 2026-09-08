# Context-file rubric — 15 checks (adapted from ai-skills skill_rubric_review)

Apply to ANY context file (CLAUDE.md, rules/_.md, docs/_.md, SKILL.md), not just skill bundles. Treat CLAUDE.md as the entry "SKILL.md" and rules/docs as its reference files. Output one line per item: `N. <title> [Tier]: PASS | FINDING — <file:line + what>`. Sort findings Tier 1 first.

| id  | title                                | tier | question                                              | red-flag signals                                                                                       |
| --- | ------------------------------------ | ---- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| 1   | Trigger surface coherence            | T3   | Does each section describe the scope it claims?       | title/body mismatch; promises capability not present                                                   |
| 2   | Integrity/logicality                 | T3   | Followable without prior context?                     | undefined terms; missing prereqs; nonsensical ordering                                                 |
| 3   | Input completeness                   | T4   | Every referenced param/path/tool defined?             | referenced but undeclared; no type/format                                                              |
| 4   | Algorithm clarity                    | T2   | Clear procedure, in one place not fragmented?         | steps spread across files; same step in multiple places                                                |
| 5   | Computed parameters                  | T2   | Thresholds/limits have explicit source?               | "calculated from X" with no formula; threshold no source                                               |
| 6   | Behavior patterns w/ examples        | T4   | Patterns described AND exemplified?                   | abstract pattern, no example                                                                           |
| 7   | Tools/capabilities                   | T4   | External tools/hooks/skills described w/ when-to-use? | named, no description; multiple tools no choice-guidance                                               |
| 8   | Boundaries (out-of-scope)            | T4   | Explicit "must NOT" / escalation rules?               | no 'do not'; no refuse/clarify rule                                                                    |
| 9   | Priority hierarchy                   | T1   | When rules conflict, defined tie-breaker?             | conflicting rules, no priority; no safety-vs-task order                                                |
| 10  | Failure/recovery                     | T2   | What on missing input / hook failure / ambiguity?     | only happy-path; no escalation rule                                                                    |
| 11  | State between steps                  | T4   | Multi-step: what persists / final-answer reqs?        | long procedure, no reminders                                                                           |
| 12  | Response format                      | T3   | Format described, fill-rules clear, no contradiction? | conflicting constraints across files (≤72 vs ≤60)                                                      |
| 13  | Information density (anti-bloat)     | T1   | Every line carries weight?                            | filler ('it is important to note'); same rule restated 2-3x                                            |
| 14  | Cross-file duplicates/contradictions | T1   | Duplicated or contradictory across files?             | same rule in 2 files; 'always X' here 'never X' there; orphan rules                                    |
| 15  | Tool-output bloat (bin/ scripts)     | T2   | Custom bin/ script returns bounded/summarized output? | script returns raw unbounded array/dump w/o truncation, head-N, or summarization; floods model context |

## Tier meaning (output ordering)

- **T1** = surfaced first (worst pain): priority conflicts, anti-bloat, cross-file dup/contradiction.
- **T2** = algorithm clarity, computed params, failure/recovery.
- **T3** = trigger coherence, logicality, response format.
- **T4** = completeness, examples, tools, boundaries, state.

## Notes

- This rubric catches what pure string-dedup misses: contradictions (rule A says X, rule B says NOT-X) and unexplained two-number constraints. Run it AFTER the dedup scan, not instead of.
- A finding is only real if you can cite `file:line` + the exact conflicting text. NO citation = NO finding.
- Skip "false-positive coincidences": e.g. the same number (500) used for three genuinely different domains is NOT a duplicate.
