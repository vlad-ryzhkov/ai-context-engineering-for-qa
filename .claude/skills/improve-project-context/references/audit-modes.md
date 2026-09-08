# Audit Modes — Detailed Algorithms

Loaded by Step 1 (Mode detect), and holds the long form of the work Steps 2–8 do.

The `Step 1a…5` numbering below is LOCAL to this file and does not match the
pipeline in SKILL.md. Map before quoting a step number in a finding:

| Here | SKILL.md pipeline |
| --- | --- |
| 1a Git diff + .gitignore filter | 1 Mode detect (precondition) |
| 1b Dependency graph + orphan detection | 2 Build dep graph |
| 1b Orphan classification | 3 Orphan + dedup scan |
| 1c Token budget audit | 5 Apply caps |
| 1d Content overlap + language audit | 3 Orphan + dedup scan |
| 2 Generate atomic rules | — (belongs to `improve-context-from-sessions`) |
| 3 Semantic deduplication | 3 Orphan + dedup scan |
| 4 Routing algorithm | 6–7 Context map + SDLC map |
| 5 Progressive disclosure + human-in-the-loop | 8 Format patches |

## Step 1 — Collect Context (Full Algorithm)

### Step 1a — Git Diff + .gitignore Filter

```bash
git status --porcelain | head -n 50
# For each file, check if gitignored:
git check-ignore -v <path>   # gitignored → skip entirely
```

**Symlink Detection:**

```bash
find .claude -maxdepth 2 -type l 2>/dev/null | while read f; do
  echo "$f -> $(readlink $f)"
done
```

`.claude/` entries that are symlinks → real edit target is the symlink source.
Report symlink topology before proposing any Edit.

### Step 1b — Dependency Graph + Orphan Detection

**Build complete dependency tree** by following inline links recursively (max depth 3).

**Entry points:** `CLAUDE.md`, `AGENTS.md`, `GEMINI.md` (check `ls -la` for symlinks), `.claude/` agent/orchestrator files.

Full algorithm: See `./references/dependency-graph-algorithm.md` BEFORE first scan.

**Orphan scope — search EVERYWHERE, not just `docs/`:**

```bash
git ls-files "*.md" 2>/dev/null
git ls-files --others --exclude-standard "*.md" 2>/dev/null
```

**Critical .gitignore and .git/info/exclude handling:**

| Status                                                       | Classification         | Action                                                     |
| ------------------------------------------------------------ | ---------------------- | ---------------------------------------------------------- |
| **Tracked** (no output from `git check-ignore -v`)           | Eligible for ORPHAN-\* | Classify and propose patches                               |
| **`.gitignore`-ignored** (output from check-ignore)          | LOCAL-IGNORED          | Note only; never propose linking from tracked entry points |
| **`.git/info/exclude`-only** (in exclude, not in .gitignore) | LOCAL-IGNORED          | Do NOT classify as orphans; entirely out of scope          |

**Never propose a patch linking a `.gitignore`-ignored file from a tracked entry point — this creates a broken link for every other developer.**

When classifying agent/orchestrator files vs skill files (entry-point detection above), distinguish their roles → `references/skills-vs-agents.md`.

### Step 1b (continued) — Orphan Classification

Compare every found `.md` against the dependency graph. Any file not reachable from any entry point → classify:

- **ORPHAN-USEFUL** — domain knowledge / tutorials / runbooks that should be linked. Propose adding JIT pointer from nearest relevant entry point.
- **ORPHAN-ARTIFACT** — build artifact, backup (`*.original.md`), stale report, review output (`review-QC-*.md`). Propose deletion.
- **ORPHAN-MISPLACED** — file belongs elsewhere (e.g. report in `docs/agents/` that should be in `docs/reports/`).

**Special cases:**

- `.zip`, lock files (`*.lock`), binary files in context folders → ORPHAN-ARTIFACT regardless of linkage
- Reporto outputs (`review-*.md`) → ORPHAN-ARTIFACT
- `.bak` / `.backup` / `.orig` files → ORPHAN-ARTIFACT

### Step 1c — Token Budget Audit

```bash
CLAUDE_PROJECT_DIR=<repo> python3 scripts/context-check.py
```

That is the verdict for the always-loaded pool. It reads the cap and the
bytes-per-token ratio from `docs/context-budget.md` and sums BOTH
pools `/context` bills together — global `~/.claude` and the repo's `CLAUDE.md`
+ `.claude/rules/**`. BANNED: a hand `wc -c` ÷ 4 estimate as the verdict; that
divisor understated the real corpus by 1.65x.

For JIT docs, which the checker does not gate, `wc -c` ÷ the same ratio:

| Scenario                                 | Target       | Hard cap     |
| ---------------------------------------- | ------------ | ------------ |
| Single JIT doc                           | ≤40k bytes   | ≤50k bytes   |
| Realistic task load (always + JIT chain) | ≤80k bytes   | ≤100k bytes  |

**Always include "after" column** showing projected improvement from proposed patches. Compute "after" by applying the specific patch.

**Flag ROUTING-AMBIGUITY** when two sibling docs cover overlapping task types and the routing trigger in the entry point is not mutually exclusive — ambiguous request loads both, doubling token cost.

### Step 1d — Content Overlap + Language Audit

**Overlap detection:**

For sibling docs covering the same domain, extract headings and compare. Shared sections > 20 lines across 2+ files → propose extraction to `docs/common/<topic>.md` with a link replacing the duplicate content.

**Language audit:**

Non-English in instruction files (rules, SKILL.md, agents.md, CLAUDE.md) → BANNED.

```bash
grep -rl "[а-яА-ЯёЁ]" .claude/ docs/agents/ 2>/dev/null
```

Pure data files (reports, config dumps) are exempt.

## Step 2 — Generate Atomic Rules

```text
TYPE: REQUIRE | BANNED | TRIGGER | TOOL-INSIGHT | NEW-ARTIFACT
RULE: {concrete, verifiable}
REASON: {token waste / orphan / duplication / routing-ambiguity / progressive-disclosure violation}
SOURCE: cold-audit | git-diff
```

**Quality bar:**

- GOOD: `BANNED: tutorials in always-loaded rules/. Move to docs/ + JIT pointer.`
- GOOD: `TRIGGER: disambiguation guard — "write a test" must route to one of {auto, manual}.`
- BAD: `Docs should be cleaner.` → discard

**NEW-ARTIFACT example:**

```text
TYPE: NEW-ARTIFACT
TARGET: docs/common/git-hygiene.md
REASON: 28 shared lines between rules/git.md and rules/branches.md — extract.
SEED RULES:
  - REQUIRE: Both files Read this on demand instead of duplicating.
```

## Step 3 — Semantic Deduplication Algorithm

1. Read `~/.claude/CLAUDE.md`
2. `ls ~/.claude/rules/ 2>/dev/null` → read related files
3. Inside project: `Glob .claude/rules/**/*.md` + `.claude/skills/**/*.md`
4. Compare semantically per this table:

| Match                          | Action                                              |
| ------------------------------ | --------------------------------------------------- |
| Exact duplicate                | SKIP — content already exists, no patch needed      |
| Same intent, different wording | PATCH: extend existing rule with additional context |
| Related but different angle    | ADD as new subsection in existing file              |
| No match                       | Proceed to Step 4                                   |

**Deduplication goal:** Never ask the user to maintain the same constraint twice.

## Step 4 — Routing Algorithm (Progressive Disclosure)

For each rule generated in Step 2:

1. **Identify target zone** using infrastructure-layers routing table (read first: `./references/infrastructure-layers.md`)
2. **List PLACEMENT OPTIONS:** all viable zones for this content (usually 3–5)
3. **Mark 1–2 as RECOMMENDED** with reasoning; mark others as SKIP + reason why ruled out
4. **Include line count + cap check:** `TARGET FILE: <path> (<N> lines, cap <M>, safe: yes/no)`
5. **Ambiguous global vs project?** → ask user: "Applies to all projects or only this one?"

**Each patch must include:**

```text
PLACEMENT OPTIONS: [1..8 zones, each SKIP/RECOMMENDED]
REASONING: <why recommended; why each SKIP ruled out>
TARGET FILE: <path> (<N> lines, cap <M>, safe: yes/no)
```

**Example:** Rule about git stash discipline

```text
PLACEMENT OPTIONS:
  [1 RECOMMENDED] — rules/git.md (stash is a core git invariant)
  [2 SKIP]        — docs/git-workflow.md (too niche for always-on)
  [3 SKIP]        — CLAUDE.md (not specific enough to deserve global prominence)

REASONING:
  • rules/git.md already covers git safety; stash discipline is a core invariant
  • "Never let stashes accumulate" is BANNED-like, belongs in rules/
  • Project-specific only if git setup differs; default global

TARGET FILE: rules/git.md (187 lines, cap 100, safe: no → evacuate 35 lines to docs/git-rationale.md first)
```

## Step 5 — Progressive Disclosure + Human-in-the-loop

**Sort by impact:**

```text
Impact ranking:
  1. Token waste (chars × likelihood)
  2. Orphan severity (ARTIFACT > MISPLACED > USEFUL)
  3. Duplication (overlap size × maintenance cost)
  4. Routing-ambiguity (double-load tokens)
  5. Novelty (new content, lower priority)
```

**Show top-3 first.** Format: see `./references/output-formatting.md`.

**On "y/1 apply":**

- File exists → `Edit` anchored to section header
- New file → `Write`

**On "a / show all N":** reveal rest, same format (no truncation).

**After patches:**

```bash
git -C <dir> rev-parse --git-dir 2>/dev/null
```

- Exit 0 → propose commit
- Exit ≠ 0 → "Not a git repo — commit skipped."

## Mode-Specific Workflows

### Cold Scan (Deep Audit)

Entry: User says `/improve-project-context`, "cold audit", "review AI setup".

1. Run Steps 1–5 in full
2. Dependency graph: max depth 3
3. Token budget: measure always-loaded + realistic task load
4. Content overlap: full sibling comparison
5. Output all patches (progressive disclosure, top-3 first)

### Orphan Detection (Focused)

Entry: User says "orphan detection", "find dead docs", "list unused files".

1. Run Steps 1a–1b (Git filter + dependency graph) only
2. Output: dependency table + orphan list
3. Skip Steps 2–5 (rule generation / routing) unless user asks for patches

### Dedup Pass (Content Overlap)

Entry: User says "find duplication", "consolidate docs", "reduce overlap".

1. Run Steps 1d + 3 only (content overlap + deduplication)
2. Output: duplicate-findings table + dedup candidates
3. Skip orphan detection (Step 1b) unless highly relevant

### Routing Audit (Progressive Disclosure)

Entry: User says "fix routing", "simplify how docs load", "ambiguity audit".

1. Run Steps 1–4 in full
2. Focus on routing-ambiguity findings + placement reasoning
3. Output: top-3 patches by routing impact
