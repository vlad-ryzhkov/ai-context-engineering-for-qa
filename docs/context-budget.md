# rules/ aggregate budget

**Scope — ONE doc for every repo.** Covers `~/.claude/rules/**` (global) AND
`<any-repo>/.claude/rules/**` (project-level), by explicit user decision
(2026-09 context-corpus audit): a project `.claude/` is the same class of
personal, machine-local Claude Code config as the global one — often gitignored
by the project itself — not a separate governance surface needing its own copy
of these caps. Same caps, same pre-edit gate, both directions.

## Hard caps

- **ALWAYS-LOADED**: CLAUDE.md + always-on rules (NO YAML `paths:` front-matter) ≤ **500 LOC**. This is the real per-session cost — path-scoped rules load only on matching files, so EXCLUDE them. Measure → `scripts/context-check.py`.
- **rules/ total** (always-on + path-scoped): ≤ **500 LOC** (`wc -l ~/.claude/rules/*.md | tail -1`). Softer — path-scoped rarely co-load.
- **Single file**: ≤ 100 LOC.
- **Tokens**: always-loaded ≤ **10k**, counting BOTH pools together — `~/.claude` (CLAUDE.md + always-on rules) AND the repo's `CLAUDE.md` + `.claude/rules/**`. `/context` reports them as one "Memory files" number, so a cap on the global pool alone never binds. Verify with `scripts/context-check.py`, which parses the two constants below.
- **Bytes-per-token**: `wc -c` bytes ÷ **2.4**. NOT ÷4. The old ÷4 was a generic-prose guess and understated this corpus by **1.65x** — measured 2026-09-04 against `/context`: 32,839 B of global memory files reported as 13,538 tok = 2.43 B/tok; project pool 6,576 B = 2,894 tok = 2.27 B/tok. Terse rule style is dense — arrows (`→`, 3 bytes), backticked identifiers, and Cyrillic (2 bytes/char) all push bytes-per-token far below 4. A ÷4 estimate passed the 9k cap at 8,209 while the true cost was 13,538.

<!-- context-check: TOKEN_CAP=10000 BYTES_PER_TOKEN=2.4 -->

### Re-calibrating the ratio

2.4 is empirical, so it drifts as the corpus changes shape — more Cyrillic,
more arrows, more code fences all move it. Re-check it whenever the corpus
changes a lot, or when the checker's estimate and `/context` disagree:

1. Run `/context`, read the "Memory files" token number.
2. `CLAUDE_PROJECT_DIR=<repo> python3 ~/.claude/scripts/context-check.py --calibrate <that number>`
3. Drift >10% → it prints the replacement `BYTES_PER_TOKEN=` value; write it into
   the marker above. Under 10% → it says the ratio still holds; change nothing.

`python3 ~/.claude/scripts/context-check.py --test` covers the budget maths:
constants parsed from this doc (never hardcoded), path-scoped rules excluded,
project pool included, silence under cap, and the drift verdict both ways.

- Cap is 9k, not the old 3.5k, because git-safety and file-edit invariants are incident-driven and irreducible without losing the protection they buy. Trim padding and duplication first; evacuate content only when it does not weaken a gate.

## Pre-edit gate

BEFORE `Write` / `Edit` / `Create` in `~/.claude/rules/**`:

1. CHECK always-loaded LOC (`scripts/context-check.py`). Path-scoped file (has YAML `paths:`) → counts only vs softer rules/ total.
2. New always-loaded total > 500 → STOP. Pick ONE:
   - **COMPRESS** target (keep `## Hard rules` ONLY, drop rationale/examples).
   - **EVACUATE** to `~/.claude/docs/<topic>.md`, leave 1-line `Read` pointer.
   - **REJECT** if fires <10% sessions → `docs/` + JIT trigger ONLY.
3. NEW `rules/*.md` requires gate + rarity ≥10%. Else `docs/` ONLY.
4. TIMING → corpus files read ONCE at session start; edits apply ONLY after `/clear`/restart. REQUIRE: ACCUMULATE all corpus edits into ONE pool, apply/commit at session END. BANNED: push corpus edits mid-session. Reason: NOT prompt-cache (mid-session disk edit does not break it) — every mid-session apply re-injects the WHOLE corpus on the next context refresh; batch-at-end = one re-injection, not many.
   - **Measured 2026-09-01** (`scripts/ai_delegation.py --section context`, 31 sessions of 20+ turns): the re-injected floor is **64,297 tokens** — median starting context, before any work. Three trickled applies therefore cost ~193k tokens for nothing. Working-window median was 170,572 and peak-median 226,639, so the floor is ~38% of a typical window.
   - Enforcement is by rule only, not by hook: nothing blocks a second apply. The pool discipline lives in `CLAUDE.md` §Context-corpus edit gate and in the Invariants of both `improve-*` skills.

## CLAUDE.md is an INDEX, not a manual

Hard shape rule, because this is what the cap keeps breaking on.

- CLAUDE.md holds ONLY: a trigger and where to read the answer. One row, one
  line. The `docs/` file carries the detail — steps, rationale, examples,
  commands, incident history.
- A CLAUDE.md entry that a reader could FOLLOW without opening anything else is
  too long. Cut it to the trigger and move the rest.
- Test before adding a line: "does this tell the model WHEN, or does it tell it
  HOW?" WHEN stays. HOW leaves.
- Same for `rules/**`: the `## Hard rules` list stays, the rationale goes to
  `docs/<topic>-rationale.md`. Several already do this — keep it uniform.
- An incident-driven gate MAY be evacuated. Keep the BANNED/REQUIRE line
  resident so the gate still fires; move only the WHY, the incident log, and the
  worked example. BANNED: evacuating the gate itself and leaving just a pointer
  — a rule the model never sees cannot fire.

## Location logic

- **rules/** = always-on constraints (BANNED/REQUIRE/MUST). Path-scoped or universal.
- **docs/** = passive JIT (schemas, logs, templates, rationale). Loaded via `Read ~/.claude/docs/<file>.md` from rules/ JIT trigger.

## Terse rule style — MANDATORY

Imperative ONLY, drop articles, arrows+caps, sentences <12 words, ONE example max, English only. Full spec → READ `docs/terse-rule-style.md`.

## Anti-patterns

- Incident logs / timestamps in `rules/` → `docs/`.
- Code examples >5 LOC in `rules/` → `docs/`.
- Niche files for <10% session fire → BANNED.
- Append without measuring → BANNED.

## Cross-refs

- Per-file Size Pressure: `improve-project-context` + `improve-context-from-sessions` skills.
- Reflex trigger: `~/.claude/CLAUDE.md` → "Context-corpus edit gate".
- Surfacing these numbers in a session — the `SessionStart` budget warning and the status-line
  context meter: `docs/context-visibility.md`.
