---
name: improve-context-from-sessions
description: >-
  Session retrospective for AI context artifacts: mines the current dialog for corrections,
  tool-drift, and missing rules, then proposes delta-updates to rules, skills, and docs. Use when a
  task is done, on completion signals ("shipped", "done", "merge", "готово"), or after review fixes
  land on a PR. Not for a cold repo topology audit — that is improve-project-context. Never drafts
  new skills.
allowed-tools: Read Edit Write Bash Glob
disallowed-tools: WebFetch, WebSearch
---

# /improve-context-from-sessions — Behavioral Retrospective

ROLE: turns corrections, tool-drift, and validated approaches from the live dialog into context
patches. NEVER generates new skills (→ `/init-skill`).

Trigger: completion signal ("done", "shipped", "merge", "готово") → ASK "Run
/improve-context-from-sessions to capture learnings?". Wait for consent. NO auto-fire.

SECOND trigger — BEFORE the artifact leaves the machine, not after: about to publish
outward-facing work into a repo, tracker, or board you do not own. Mine the corpus for the gate
that already covers it and run that gate FIRST. A retrospective that fires only on completion
learns the lesson after the cost is paid; a draft killed at publication time has already burned
the whole drafting session.

## Invariants (BANNED to violate)

- Surgical Edit ONLY. Wholesale rewrite = BANNED.
- **ONE pool, ONE apply.** Collect EVERY accepted delta, then apply and commit them together.
  BANNED: apply a batch → keep mining → apply again. Each apply re-injects the whole corpus at the
  next context refresh, so N applies cost N × that. Re-run `python3 scripts/context-check.py` AFTER the
  pool, before the commit.
- Terse rule style for `rules/**` + `SKILL.md` + JIT pointers ONLY. `docs/**` = human-prose OK.
  Style spec: `docs/terse-rule-style.md`.
- NO new-skill generation (handoff → `/init-skill`).
- Silent corpus edits = BANNED. ASK before any rules / CLAUDE.md / docs write.
- **ALREADY-COVERED test — run it per candidate, BEFORE the patch card.** Grep CLAUDE.md +
  `rules/**` + the owning `SKILL.md` for the behaviour, not the wording. A rule that already
  exists AND is correct AND was simply ignored = NO patch: a fourth phrasing of the same rule buys
  nothing and costs every turn. Dedup (Step 3) catches a textual twin; this catches the live rule
  that fired and lost. Report it as "covered by `<path>:<line>`, model ignored it" — that is a hook
  or gate question, not a corpus edit.
- SCOPE (warm vs cold): THIS skill writes POINT DELTAS into EXISTING `rules/*.md` / CLAUDE.md (a
  BANNED / REQUIRE / TRIGGER line). ALSO IN SCOPE: ONE small new `docs/*.md` (single focused
  pattern) + its 1-line JIT pointer — create directly, NO handoff. OUT of scope (→ HANDOFF
  patch-card, action `Run /improve-project-context`): MULTI-file change, evacuate block → `docs/`,
  repurpose a file, file grew or shrank significantly. This skill FINDS structure-at-scale;
  improve-project-context EDITS it.
- CONTEXT-MAP sync: created / renamed / resized a context file → UPDATE `context-map.md`
  IMMEDIATELY in the same run. Cannot locate the map, or a multi-file resync is needed → append the
  recommendation "run /improve-project-context to resync". BANNED: leave the map drifted silently.
- PLACEMENT: rule unique-to-project AND useful-to-colleagues → `<repo>/.claude/` (git-shared).
  Global rule OR personal preference → `~/.claude/` (CLAUDE.md / rules / docs).
- SHRINK SKILL.md over 300: TWO levers, in order — (1) EXTRACT blocks (examples, algorithms,
  tables, rationale) → `references/` (JIT, 0 start-cost); (2) THEN compress remaining prose per
  `docs/terse-rule-style.md`. Extract beats compress.
- COMPRESS prose-heavy CLAUDE.md / `rules/*.md` over budget → RECOMMEND a compress pass per
  `docs/terse-rule-style.md`. Other `.md` → softer recommend, only if token-pressured.

## Pipeline (5 steps, JIT-loaded)

1. **Read the signals** → READ `references/session-mode-algorithm.md` +
   `references/signals-mapping.md`. Source is the live dialog: user corrections, rejected tool
   calls, repeated failures, and approaches that were validated and should be pinned.
2. **Synthesize rules**: TYPE ∈ {REQUIRE | BANNED | TRIGGER | TOOL-INSIGHT | NEW-ARTIFACT}.
   Concrete, verifiable. NO proof = NO rule.
3. **Dedup**: Read CLAUDE.md + Glob `.claude/**/*.md`. Find ANY duplicate — rules ↔ CLAUDE.md ↔
   docs AND overlapping skills / commands (ASK which owns it). Exact = SKIP. Overlap = EXTEND.
   Sibling >20 LOC overlap = FLAG ROUTING-AMBIGUITY.
4. **Route + cap-check** → READ `references/infrastructure-layers.md`. Layer 1–4 + Tier 1/2/3.
   `rules/` aggregate >500 OR CLAUDE.md + rules COMBINED >500 = STOP, evacuate per
   `docs/context-budget.md`.
5. **Disclose**: Sort by correction frequency, then severity. TOP-3 first. Card format:
   `references/patch-card-example.md`. Wait for user consent BEFORE writing.

## Caps

SINGLE SOURCE → `docs/context-budget.md`. Read it for current numbers; BANNED to restate them here
(three drifting copies was a real defect).

Measure BOTH axes: LOC **and** chars. LOC understates real cost roughly 2x on long lines — a
corpus can pass the LOC cap and sit 3–4x over the token cap. Token breach outranks LOC pass. Detail
→ `references/infrastructure-layers.md`.

THIRD axis — money. REQUIRE: price every token delta.
`$/month = Δtokens × turns_per_month × rate`. Get the `$ / 1M tokens` rate from
`python3 scripts/ai-efficiency.py`. The always-loaded corpus is re-read every turn, so
Δtokens × turns is the real multiplier. REQUIRE: the patch card states tokens AND `$/month`.
BANNED: hardcoding the rate here — it drifts, so read it. BANNED: a saved-$ figure measured
against a version that was never written.

## Verbosity

> **SILENT MODE**: run the mining and dedup phases silently. Do not narrate intermediate
> reasoning. Only the sorted patch cards (or an explicit ESCALATION if blocked) go to chat.

## Quality Gate (Self-Review)

Before returning, verify internally:

- [ ] Every candidate ran the ALREADY-COVERED grep; covered ones reported as `<path>:<line>`, not patched.
- [ ] Dedup step 3 ran against CLAUDE.md and `.claude/**/*.md`.
- [ ] Every patch card states PLACEMENT, REASONING, LOC, and `$/month`.
- [ ] Cap check ran via `python3 scripts/context-check.py`; its exit code is quoted, not guessed.
- [ ] Sort applied; TOP-3 disclosed first.
- [ ] NO proof = NO rule. Nothing written without user consent.

**Gardener Protocol**: call `.claude/protocols/gardener.md`. If this run surfaced a missing rule
about the retrospective process itself, output a brief proposal table. Otherwise:
`🌱 Gardener: No updates needed.`

## Scope note — Session Mode only

An earlier version also had a Multi-Session Mode that mined `~/.claude/projects/*.jsonl` archives
for recurring tool-call clusters across many sessions. Its scanner was vendored from a colleague's
work in a private corporate repository, so it is not published here. Everything above operates on
the live dialog and needs no archive scanner.

## Completion

```text
✅ SKILL COMPLETE: /improve-context-from-sessions
├─ Signals mined: [N corrections, N tool-drift, N validated approaches]
├─ Candidates: [N] (already covered: [N], patched: [N], declined: [N])
├─ Budget: [PASS/FAIL — context-check.py exit code]
└─ Cost delta: [Δtokens, $/month]
```

## NOT this skill

- Cold topology audit → `improve-project-context`
- New skill drafts → `/init-skill`
- Memory writes, code review → BANNED here
