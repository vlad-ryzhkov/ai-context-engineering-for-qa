# Session Mode Algorithm — Detailed Steps

Loaded by Step 1 when Mode = Session (user feedback present, no "multi-session" request).

Session Mode processes axes 1a (dialog signals) + 1b (tool signals) + 1c (git state).

## Axis 1a — Dialog Signals (Cross-lingual Semantic Scan)

Priority: Correction > Missing Rule > Missing Trigger > Praise.

### Classification Regex Patterns

**Corrections** — user intent is "stop", "undo", or "fix":

- English markers: `(stop|wrong|revert|undo|should.*not|don't|should.*instead)`
- Russian markers: `(не\s+так|опять|стоп|назад|не\s+нужно|вместо)`
- Examples: "Stop here", "This is wrong, let me redo it", "Не так, отрев..."

**FALSE-POSITIVE GUARD (mandatory before classifying a correction):** a marker counts ONLY if it is a STANDALONE user interruption REACTING to the assistant's prior `tool_use`/action — short corrective phrase. A marker embedded in a task/business-logic description is NOT a correction.

- 🟢 Assistant: "pushed to main". User: "Стой, никогда не пушь в main без PR!" → marker `Стой`+`никогда не` follows an assistant action → REAL correction → patch `BANNED: push to main`.
- 🔴 User: "Реализуй метод `revert()` для коммитов" / "обработай кнопку Назад, если не так — верни ошибку" → `revert`/`Назад`/`не так` are inside a task spec, NOT after an assistant action → IGNORE, no patch.
- Rule: marker in a message that (a) directly follows assistant tool_use AND (b) is short/corrective in intent → classify. Else skip.

**Missing Rules** — user says "should", "must", "always", "remember", "next time":

- Markers: `(should|must|always|remember|next\s+time|default|invariant|require|constraint)`
- Examples: "This should always happen", "Remember to check this before...", "Make this the default"

**Missing Triggers** — user says "should have", "could have", "reflex", "trigger":

- Markers: `(should\s+have|could\s+have|auto-?fire|reflex|trigger|suggested)`
- Examples: "That trigger should have fired", "Why didn't the skill auto-invoke?"

**Praise** — user validates or institutionalizes:

- Markers: `(good\s+pattern|nice|like\s+this|keep\s+doing|institutionali|standard\s+approach)`
- Example: "Keep that pattern — it works well"

For cross-lingual: always extract intent first (what the user _means_), then match.

## Axis 1b — Tool Signals (Session Mode)

**Explicit loads detected from chat history:**

- User says "let me read X.md" → grep chat for `Read.*X.md`.
- User says "I looked at..." → they explicitly read.
- Skill was invoked → infer that its loads were intended.

**Reflex trigger analysis:**

- Check each reflex trigger in CLAUDE.md.
- Did the session's content _match_ the trigger condition?
- Did the trigger _fire_ (skill invoked)?
- No → candidate TRIGGER rule update.

Example: CLAUDE.md has reflex `before-`simplify-after-code-changes"` should auto-fire. Session edits 5 production files. Skill never invokes. → Candidate: "TRIGGER: Strengthen multi-file detection for /simplify auto-fire."

## Axis 1c — Git Diff + .gitignore Filter

Run at the start of Step 1:

```bash
git status --porcelain | head -n 50
git check-ignore -v <any-changed-path> 2>/dev/null
find .claude -maxdepth 2 -type l 2>/dev/null | while read f; do
  echo "$f -> $(readlink $f)"
done
```

Purpose: before any patch references a file, confirm:

1. File exists and is tracked (or is intentionally new).
2. File is not symlink'd to a .gitignore'd target.
3. Symlinks point to the real edit location.

## Axis 1d — Not Used in Session Mode

(Multi-Session Audit Mode only — see `session-mode-algorithm.md`.)

## Signal Extraction Checklist

Before proceeding to Step 2:

- [ ] Read full chat transcript.
- [ ] Grep for correction markers (stop, wrong, revert, undo, etc.).
- [ ] Grep for rule markers (should, must, always, remember, etc.).
- [ ] Grep for trigger markers (should have, auto-fire, reflex, etc.).
- [ ] Check each reflex trigger in CLAUDE.md against session actions.
- [ ] Run git checks (status, check-ignore, symlink).
- [ ] Classify each signal into one of four buckets (Correction, Rule, Trigger, Praise).
- [ ] Sort by priority.

Output: list of 3–8 signals, each tagged with type and reason.

## Gardener noise-filter (apply to EVERY candidate rule before Step 2)

Before emitting any synthesized rule, ask: **"Would this rule, written EXACTLY
as proposed, prevent the same mistake on the next run?"** No → DROP it.

- 🟢 Good: `Thread.sleep(2000)` appeared in output → `BANNED: Thread.sleep — use executeUntilTracked()`. Specific, verifiable, prevents recurrence.
- 🔴 Bad: "consider adding more assertions" / "code should be readable" → a wish, not a verifiable prohibition. Drop.

A rule that is a wish, restates an existing constraint, or cannot be checked
mechanically fails the filter. Prefer zero rules over noise.

## Example Signal Extraction

**Chat:**

```text
User: "You should have invoked /refactor here instead of manual loop."
Assistant: [ran custom Bash loop for cleanup]
User: "Also, remember to always check git branch before Edit — I got caught twice."
Assistant: [later Edit without branch check in subagent brief]
```

**Extracted signals:**

1. **Missing Trigger** — `/refactor` should auto-fire for cleanup patterns. Session had refactor-shaped loop. Trigger did not fire.
2. **Missing Rule** — Subagent briefs must include `git status --porcelain <path>` check BEFORE Edit. This constraint exists in `rules/file-edit-safety.md` (rule 1) but not explicitly in subagent-context guidance.

**Priority:** 1 > 2 (trigger is a reflex gap; rule already partial-exists, needs extension).

---

Proceed to **Step 2** with these signals.
