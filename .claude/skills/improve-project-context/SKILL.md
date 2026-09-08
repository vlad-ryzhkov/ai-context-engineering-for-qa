---
name: improve-project-context
description: >-
  Cold audit of AI context artifacts in ~/.claude and .claude — skills, rules, docs, hooks,
  commands. Maps the dependency graph from CLAUDE.md / AGENTS.md entry points, measures the
  always-loaded token budget, and flags orphans, duplication, and ambiguous JIT routing. Surgical
  edits only. Use on /improve-project-context, "audit context", "review AI setup", "check repo
  context". ALSO use before hand-rolling any measurement over the context corpus — counting
  tokens, chars, LOC, or files across CLAUDE.md / rules / docs / skills; generating or re-syncing
  a docs registry or context map; or checking corpus budget drift. It owns those measurements and
  runs them through scripts/context-check.py. Not for a session retrospective — that is
  improve-context-from-sessions.
allowed-tools: Read Edit Write Bash Glob
---

# /improve-project-context — Cold Topology Audit

ROLE: static analyser. Builds dep graph, measures token budget, finds structural problems. NO session signals, NO JSONL mining (→ `improve-context-from-sessions`).

## Invariants (BANNED to violate)

- Surgical Edit ONLY. Existing files → `Edit` anchored on header or last 2 lines. New files → `Write`. Wholesale rewrite = BANNED.
- **ONE pool, ONE apply.** Collect EVERY accepted finding, then apply and commit them together. BANNED: apply a batch → keep auditing → apply again. Each apply re-injects the whole corpus at the next context refresh (measured 2026-09-01: 64,297 tok floor), so N applies cost N × that. Re-run `scripts/context-check.py` AFTER the pool, before the commit.
- Terse rule style for `rules/**` + `SKILL.md` + JIT pointers ONLY. `docs/**` = human-prose OK (rationale, examples, incidents, decision trees). Style spec: `docs/context-budget.md`.
- BANNED reads/edits: `*.local.json`, gitignored paths (verify FIRST), `*.zip`/binary (flag ORPHAN-ARTIFACT).
- Trigger: explicit user call ONLY (`/improve-project-context`, "audit context", "cold audit"). NO auto-fire.
- PLACEMENT: rule unique-to-project AND useful-to-colleagues → `<repo>/.claude/` (git-shared). Global rule OR personal preference → `~/.claude/` (CLAUDE.md/rules/docs).
- BANNED project→global ref: shared project context (repo CLAUDE.md / `.claude/` / docs/) MUST NOT depend on `~/.claude/**` — dangling in a colleague's clone. Inline the gist or keep it in repo `.claude/`. A `~/.claude` ref is allowed ONLY as an explicitly-optional personal add-on. FLAG every such ref in audits.
- BANNED hardcoded feature-branch in shared files: scripts / docs / templates committed for colleagues MUST NOT pin a personal branch (`--ref feat/...`, `git checkout feat/...`). Use `main` or a `<your-branch>` placeholder. FLAG every committed `--ref <non-main-branch>` / branch literal in audits.
- SHRINK SKILL.md over 300: TWO levers, in order — (1) EXTRACT blocks (examples, algorithms, tables, rationale) → `references/` (JIT, 0 start-cost); (2) THEN compress remaining prose per `docs/terse-rule-style.md`. Extract beats compress — extracted = not loaded at all; compressed = still loaded, just smaller.
- COMPRESS prose-heavy CLAUDE.md / rules/\*.md over budget → RECOMMEND a compress pass per `docs/terse-rule-style.md`. Other `.md` → softer recommend, only if token-pressured.
- YAGNI gate: corpus < ~5 context files (1 CLAUDE.md + few configs) → SKIP context-map + SDLC-phase ceremony. Bureaucracy beyond value. Run full pipeline only when files are pile-up-ing or multi-agent.
- COLD blindness: this skill is STATIC (file topology). It can't see how the model BEHAVES (a rule the AI keeps violating in dialog passes a cold audit). Pair with warm audit → `improve-context-from-sessions`. State this when reporting: "static-clean ≠ behaviorally-followed".
- DOGFOOD: this skill's own SKILL.md + references ARE valid audit targets. Run on self/siblings periodically — caught real orphans + AUTO-conflicts in practice.

## Pipeline (8 steps, JIT-loaded)

1. **Mode detect** → READ `references/audit-modes.md` BEFORE first scan.
2. **Build dep graph + path verdicts** → READ `references/dependency-graph-algorithm.md` (extraction zones, resolution cascade, lag/churn — do NOT re-derive them here). Map ALL reachable files from CLAUDE.md/AGENTS.md/GEMINI.md. THREE extraction zones only; prose is never scanned. FIVE verdicts, never present/absent: OK · MOVED · AMBIGUOUS · EXTERNAL · BROKEN. Templates resolve as GLOBS — no match = soft "rule is dead", NOT broken. MOVED/orphan suggestions come from `git ls-files` ONLY: an untracked or locally excluded path breaks the next clone. Staleness = lag + churn; churn 0 → NO staleness finding. BEFORE marking ORPHAN: grep the bare filename across corpus (catches HTML `<a>`, dynamic paths, prose mentions without markdown link). Found anywhere → NOT orphan. NEVER propose delete on grep-regex absence alone — false orphan risk. ALSO flag absolute paths (`/Users/`, `/home/`) in any git-shared context file — useless in a colleague's clone; REQUIRE repo-root-relative links.
3. **Orphan + dedup scan** → `git ls-files` ENTIRE repo. `.git/info/exclude` only = SKIP. Find ANY duplicate/overlap — across rules↔CLAUDE.md↔docs AND across skills/commands/tools (overlapping responsibility = ASK which owns it). Sibling overlap >20 LOC = FLAG ROUTING-AMBIGUITY (double-load BANNED).
   - **Skill disambiguation check** (static): two+ skills in same domain (plan / orchestration / review / commit) → REQUIRE a when-to-use line per skill (in `docs/context-map.md` or CLAUDE.md). Missing → FLAG "ambiguous skill routing — model can't pick". Usage-history (never-called) is OUT of scope here → `improve-context-from-sessions`.
4. **Rubric scan (any context file, not just skills)** → apply ALL 15 items → READ `references/context-rubric.md`. If delegating to a subagent: MUST pass the rubric file PATH verbatim in the prompt + REQUIRE one-line verdict (`N. <title> [Tier]: PASS|FINDING`) per item — NO "rubric scan" shorthand (degrades to dedup). Surfaces contradictions, anti-bloat, format conflicts pure dedup misses.
5. **Apply caps + evacuation tiers E1/E2/E3** → READ `references/infrastructure-layers.md`. `rules/` >500 OR CLAUDE.md+rules COMBINED >500 = STOP, propose evacuation per `docs/context-budget.md`. Evacuation tiers (WHAT content moves) ≠ fire-rate tiers (HOW OFTEN a rule applies) — the latter belong to `improve-context-from-sessions`. BANNED: mixing the two in one finding.
6. **Context map** → emit/update `docs/context-map.md`: table of every skill/rule/hook + its single responsibility + overlaps. Reuse across audits. Format → `references/context-map-format.md`.
7. **SDLC skill-map workflow** → READ `references/sdlc-map-workflow.md`. Canonical map = `docs/context-map.md` (global) AND `<repo>/.claude/docs/context-map.md` (project) — merged skill-map + topology. Missing project map + repo has skills → PROPOSE create from template. Skills not in map → ASK user which phase owns each (incl. utility / not-applicable buckets). Frequently-invoked skill (from usage data) still suggest-only → PROPOSE reflex trigger in CLAUDE.md. NEVER flip suggest→auto without user consent (anti-pattern: noisy auto-invoke).
8. **Format patches** → READ `references/output-formatting.md`. Emit a drift SCORE + band per context file (fresh <20 · drifting 20–39 · stale 40–59 · broken ≥60) — a ratio, never a count, so a 40-path entry point and a 7-path rule stay comparable. Record it in `docs/context-map.md` so the NEXT audit compares instead of re-deriving. Then sort: token-waste > orphan > dup > ambiguity > novelty. TOP-3 first, rest on "show all N". Each card MUST: PLACEMENT OPTIONS + REASONING + line count.

## Caps

SINGLE SOURCE → `docs/context-budget.md`. Read it for current numbers; BANNED to restate them here (three drifting copies was a real defect).

Measure BOTH axes: LOC **and** bytes. LOC understates real cost ~2x on long lines — a corpus can pass the LOC cap and sit 3-4x over the token cap. REPORT BOTH; token breach outranks LOC pass.

BANNED: converting bytes to tokens by an invented divisor, `/4` above all — it understated this corpus by 1.65x and the cap silently passed. The cap and the bytes-per-token ratio live ONLY in `docs/context-budget.md`; read them there or run the checker.

BANNED: an inline throwaway measurement script (`python3 - <<PY`, `cat $files | wc -c`) over the corpus. A committed script in `scripts/` owns each measurement; `ls scripts/` BEFORE writing one. A measurement worth running twice belongs there, not in a heredoc.

REQUIRE: the token verdict comes from `CLAUDE_PROJECT_DIR=<repo> python3 scripts/context-check.py`, never from a hand estimate. It sums BOTH always-loaded pools — global `~/.claude` AND the repo's `CLAUDE.md` + `.claude/rules/**` — because `/context` bills them as one "Memory files" number; a global-only count never binds. Over cap → the shrink plan is a REQUIRED finding, sorted first.

## Verbosity

> **SILENT MODE**: run the graph build, orphan scan, and rubric pass silently. Do not narrate
> intermediate reasoning. Only the sorted patch cards (or an explicit ESCALATION if blocked) go to
> chat.

## Quality Gate (Self-Review)

- Dep graph: ALL reachable files mapped
- Budget: `context-check.py` RUN with `CLAUDE_PROJECT_DIR` set, its exit code quoted. Non-zero → a shrink plan is the TOP finding. BANNED: reporting a budget verdict the checker did not produce
- Step 3 dedup: ran
- Every patch: PLACEMENT OPTIONS + REASONING + LOC
- Sort applied; TOP-3 first
- NO proof = NO rule

**Gardener Protocol**: call `.claude/protocols/gardener.md`. If this run surfaced a missing rule
about the audit process itself, output a brief proposal table. Otherwise:
`🌱 Gardener: No updates needed.`

## NOT this skill

- Session retrospective → `improve-context-from-sessions`
- New-skill generation → `/init-skill`
- Full-file rewriter, memory replacement, auto-on-completion → BANNED here
