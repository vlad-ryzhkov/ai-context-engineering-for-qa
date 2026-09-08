# Patch Card — Compliant Example

> Verbatim shared copy. The same file ships in `improve-project-context` and in
> `improve-context-from-sessions`, deliberately — a skill directory must stay
> self-contained. Change one, change BOTH in the same commit; a silent divergence
> is what item 14 of the rubric exists to catch.

This is the reference format for a single finding card, shown by whichever step
discloses patches — Step 8 in `improve-project-context`, Step 5 in
`improve-context-from-sessions`.

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📍 [1/3]  FILE: ~/.claude/rules/git.md  (74 lines → 76 lines after patch)

  ACTION:  EXTEND EXISTING RULE — "Branch hygiene" section

  WHY:     (REQUIRED) Claude was reminded twice to check branch before Edit.
           Rule exists but doesn't cover subagent Edit context.

  PLACEMENT OPTIONS:
    1. global CLAUDE.md   [SKIP] — would exceed 200-line cap; too granular for reflex
    2. global /rules      [RECOMMENDED] — universal rule for all git repos
    3. global /docs       [SKIP] — too atomic for a reference doc
    4. global /skills     [SKIP] — a constraint, not an invocable tool
    5. project CLAUDE.md  [SKIP] — violates DRY; rule is useful beyond this project
    6. project /rules     [SKIP] — git hygiene applies everywhere, not project-specific
    7. project /docs      [SKIP] — not project business logic
    8. project /skills    [SKIP] — not a local automation script

  REASONING: git.md is 74 lines (cap 100) — safe to append. Rule must fire
    on every subagent dispatch globally → /rules is the correct always-on zone.

  PATCH:
    + Subagent briefs MUST include `git status --porcelain <path>` precheck.
    + STOP-if-M clause required in any subagent prompt that calls Edit.

  y/1 apply   n/2 skip   d show full patch   a show all N

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
