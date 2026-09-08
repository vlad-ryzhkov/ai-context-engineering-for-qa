# Signals Mapping — Dialog, Tools, Tool-Drift Detection

Loaded by Step 1 (axes 1a + 1b). Detailed signal classification for Session Mode.

## 1a — Dialog Signals (Semantic, Cross-lingual)

Any language — intent trumps wording. Priority: Correction > Missing Rule > Missing Trigger > Praise.

| Signal Type         | What it looks like                                                                                       | Action in Step 2                                                                       |
| ------------------- | -------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| **Correction**      | Stopping agent, rejecting output, ordering revert/redo, repeating correction because constraint missed   | Propose REQUIRE or BANNED rule. Source: correction incident.                           |
| **Missing Rule**    | "How this should have been done", "what to remember next time", default behaviours, implicit constraints | Propose REQUIRE or TOOL-INSIGHT rule. May require new `rules/*.md` file.               |
| **Missing Trigger** | User manually invoked something a reflex trigger _should_ have fired for                                 | Propose TRIGGER addition to CLAUDE.md. Check `docs/context-map.md` for coverage gaps. |
| **Praise**          | Validating workflow, praising structural choice, asking to institutionalise approach                     | Low priority. If pattern repeats across sessions, promote to rule. Else skip.          |


## 1a-bis — Review Findings (third-party correction, arrives as TOOL OUTPUT)

A reviewer's comment is a correction the USER never typed: it lands in a `gh pr
view --comments` / `gh api .../comments` result. Nothing in the 1a table matches
it, so this axis exists to keep it from passing through unharvested. Harvest
EVERY finding on a PR this session fixed — human and bot, any severity.

| Signal Type            | What it looks like                                                | Action in Step 2                                                             |
| ---------------------- | ----------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| **Review finding**     | Reviewer names a defect; the session then edited code and pushed  | Propose the rule for the DEFECT CLASS, never for the one site. Source: PR N.  |
| **Widened fix**        | One comment, the fix landed in 2+ places (a sweep found siblings) | REQUIRE / BANNED — the class is proven repo-wide, not incidental.             |
| **Guard added**        | The fix needed a test or lint rule to stay fixed                  | TOOL-INSIGHT naming the guard, so the next author reaches for one too.        |
| **Reviewer disagreed** | Finding rejected with evidence (precondition false, assert right) | NO rule. A rejected finding is not a lesson; record nothing.                  |

**Placement ladder — strongest first. BANNED: prose where a test fits.**

1. **Executable guard** in the repo (test or linter that fails on the old
   shape). Costs tokens once, and holds for colleagues too.
2. **Repo `.claude/` rule** — class is real but project-specific.
3. **Global `~/.claude` rule** — survives a change of repository.
4. **Nothing** — a one-off typo, or a finding that was rejected.

Two questions gate every promotion: would the rule have PREVENTED the finding
(not merely described it afterwards), and does it hold in another repo? A no to
either drops it one rung. Measured floor from one real session: 8 findings → 1
guard test → 2 global rules.

## 1b — Tool Signals (Session Mode)

- Which skills/rules/docs were _actually loaded_ (User explicitly read them)
- Which reflex triggers _should have fired_ but didn't (from CLAUDE.md)
- Tool-drift detection: agent workaround when installed tool covered task

### Tool-Drift Markers

| Pattern                                                                                 | Indicator        | Rule Type in Step 2                                                   |
| --------------------------------------------------------------------------------------- | ---------------- | --------------------------------------------------------------------- |
| Agent runs custom Bash loop instead of `/refactor`                                      | Heavy workaround | TOOL-INSIGHT: BANNED custom refactoring, REQUIRE /refactor            |
| Agent writes code from scratch when an existing review skill would fit                          | Scope creep      | TOOL-INSIGHT: REQUIRE pre-review                                      |
| Agent reads large file (>500 LOC) instead of delegating to Explore on `haiku`               | Overspend        | TOOL-INSIGHT: REQUIRE delegation for large analysis                   |
| Agent runs 3+ consecutive edits without simplify check                                  | Complexity drift | TOOL-INSIGHT: REQUIRE `/simplify` after multi-file changes            |
| Unscoped `git diff`/`gh`/`find` dumping >200 lines into the same turn                   | Output bloat     | TOOL-INSIGHT: REQUIRE redirect-to-file (`docs/large-command-output.md`) |
| `git -C <dir>` / `command git` / aliased git (hides target repo from guards)             | Guard bypass     | TOOL-INSIGHT: BANNED `git -C`, USE `cd <dir> && git`                  |
| Naked `gh ... logs` / `git log` huge output not redirected to file                      | Token-drift      | TOOL-INSIGHT: REQUIRE redirect to `/tmp` |

### Merge Logic (Multiple Signals in One Dialog)

If a session surfaces both Correction + Missing Trigger:

1. Prioritize correction (the failure is real).
2. Check if installed trigger would have prevented it.
3. If yes, propose TRIGGER rule change (strengthen existing, or add new).
4. If no, propose REQUIRE rule (new constraint needed).

Never merge distinct signals into one patch — each gets its own PLACEMENT_OPTIONS analysis in Step 4.

## 1c — Git Diff + .gitignore Filter

Canonical tool invocations in SKILL.md (lines 88–99). Purpose: detect symlinks and .gitignore'd paths before edits.

Symlink resolution:

```bash
find .claude -maxdepth 2 -type l 2>/dev/null | while read f; do
  echo "$f -> $(readlink $f)"
done
```

If a patch target (e.g., `~/.claude/rules/git.md`) is a symlink, the real edit location is the target.

## 1d — Correction Markers for Multi-Session Audit

From JSONL stream analysis (loaded in Multi-Session Mode Step 1d).

User message immediately after an assistant tool_use containing one of:

- English: `stop`, `wrong`, `revert`, `undo`
- Russian: `не так`, `опять`, `стоп`, `назад`

The preceding tool_use's canonical signature becomes the cluster key for BANNED rule proposal.
