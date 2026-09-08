# SDLC skill-map workflow (pipeline step 7)

The SDLC map lives in `docs/context-map.md` (merged skill-map + topology): for each lifecycle phase (Setup/Design/Plan/Implement/Debug/Review/Close/Meta) it lists which skill applies + when-to-invoke + auto-fire flag, plus Utility and Not-applicable buckets. Without it, the model can't pick between same-domain skills (e.g. writing-plans vs to-prd, executing-plans vs subagent-driven, systematic-debugging vs diagnose).

## Step 7a — Find

```bash
ls ~/.claude/docs/context-map.md 2>/dev/null          # global map
ls <repo>/.claude/docs/context-map.md 2>/dev/null     # project map
```

- Global map exists, no project map, repo has its own `.claude/skills/` → project skills are unrouted. Go to 7b.
- Map(s) exist → go to 7c (reconcile).
- No map anywhere + skills present → PROPOSE create (7b).

## Step 7b — Create (PROPOSE, never silent)

Offer to scaffold from the template below. Populate the phase rows by reading each skill's `name` + `description` frontmatter. Show the draft, get consent before writing.

```markdown
# SDLC skills — suggest, don't auto-invoke

Map of installed skills by lifecycle phase. **Rule**: request matches a phase + skill NOT active → ASK once "This looks like <phase>. Invoke `<skill>`?". No auto-invoke without consent UNLESS the skill has its own reflex trigger in CLAUDE.md.

## Map

| Phase     | Skill | Trigger phrase / situation |
| --------- | ----- | -------------------------- |
| Setup     | …     | …                          |
| Design    | …     | …                          |
| Plan      | …     | …                          |
| Implement | …     | …                          |
| Debug     | …     | …                          |
| Review    | …     | …                          |
| Close     | …     | …                          |
| Meta      | …     | …                          |

## Anti-patterns

- Auto-invoking a non-AUTO skill without asking → noisy.
- Re-suggesting after a "no".
- Suggesting a phase the user consciously skipped.
```

## Step 7c — Reconcile

- List installed skills (global + project). Cross against the map.
- Skill present but NOT in any phase row → ASK user: "Skill `X` isn't in the SDLC map. Which phase owns it (Setup/Design/Plan/Implement/Debug/Review/Close/Meta), or is it out-of-lifecycle?"
- Two+ skills in one phase with overlapping trigger phrases → FLAG ambiguity, propose distinguishing trigger wording.
- Map row pointing at a skill that no longer exists → propose removal.

## Step 7d — Recommend auto-fire (PROPOSE ONLY)

If usage data (from `improve-context-from-sessions` skill-usage audit) shows a map skill invoked frequently but still marked suggest-only:

- PROPOSE adding a reflex trigger to `~/.claude/CLAUDE.md` "Reflex triggers" section: `before <situation> → Skill(<name>) FIRST`.
- Show the exact line + where it goes. Get consent.
- **HARD RULE: never flip a skill from suggest to auto-fire without explicit user consent.** Auto-invoke without asking = the map's own top anti-pattern. The skill recommends; the user decides.

## Output

Fold SDLC-map findings into the patch list (step 8): missing map (create), unrouted skills (assign phase), suggest→auto candidates (reflex proposal). Each as a normal patch card with consent gate.
