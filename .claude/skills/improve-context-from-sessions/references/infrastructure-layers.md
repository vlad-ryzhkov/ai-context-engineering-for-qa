# Infrastructure Layers — Routing, Caps, Tier Classification

Loaded by Step 4. Defines the 4-layer model and routing zones with capacity caps.

## 4 Infrastructure Layers

| Layer             | What it is          | Examples                                        |
| ----------------- | ------------------- | ----------------------------------------------- |
| **Prompting**     | What the AI reads   | `CLAUDE.md`, `rules/*.md`, `docs/*.md`          |
| **Configuration** | Engine behaviour    | `settings.json`                                 |
| **Executable**    | Terminal extensions | `commands/*.md`, `hooks/*.sh`                   |
| **Skills**        | Autonomous packages | `skills/{name}/SKILL.md`, `bin/`, `references/` |

## docs/ vs rules/

- **`docs/`** = Passive JIT knowledge (load on demand). Human-prose OK — examples, rationale, incident logs, multi-paragraph decision trees, even prescriptive rules IF JIT-triggered.
- **`rules/`** = Always-on active constraints (BANNED/REQUIRE/TRIGGER). Terse rule style MANDATORY.
- **Anti-pattern**: reference-heavy prose / rationale / examples in `rules/` → MUST evacuate to `docs/`.
- **NOT an anti-pattern**: prescriptive rules in `docs/` if JIT-triggered (e.g. `docs/context-map.md`). Niche prescriptive content → `docs/` + JIT pointer beats new always-on `rules/` file.
- **Style**: `docs/` may be human-prose; Terse style NOT required there. See `docs/context-budget.md` Terse rule style section.

## Rarity Gate for New rules/ Files

Niche constraints (applicable in <10% of sessions) belong in `docs/` + JIT pointer, NOT in a new always-on `rules/` file. This keeps `rules/` focused and prevents trigger explosion.

## Routing Zones, Caps, Decomposition

| Zone                     | Cap               | Over-limit action                                                                                   |
| ------------------------ | ----------------- | --------------------------------------------------------------------------------------------------- |
| **rules/ AGGREGATE**     | **500 LOC total** | STOP. Compress target file, evacuate to docs/, or reject. See `docs/context-budget.md`. |
| global/project CLAUDE.md | 200/150 lines     | Move to rules/. Leave 1-line Read pointer.                                                          |
| rules/\*.md              | 100 lines         | Split into `{name}-{subdomain}.md`.                                                                 |
| skills/\*/SKILL.md       | 300 lines         | Examples >15L → references/. Branching shell → bin/.                                                |
| commands/\*.md           | 150 lines         | Extract to docs/, JIT pointer stays.                                                                |
| docs/\*.md               | no limit          | Split by domain if mixing concerns.                                                                 |

## rules/ Aggregate Gate

BEFORE proposing a delta-patch adding lines to any `~/.claude/rules/*.md`:

1. **Measure current state — BOTH axes.** LOC alone understates real cost ~2x when lines are long:
   - LOC: `wc -l ~/.claude/rules/*.md | tail -1`
   - CHARS (authoritative for token budget): `cat ~/.claude/CLAUDE.md <always-on rules> | wc -c`, then `/4` for tokens. Cap ~3.5k tok.
   - A corpus can pass the LOC cap and still be 3-4x over the token cap. Report BOTH; token breach outranks LOC pass.
2. **Check against 500 LOC cap** — if patch would push total > 500, one of:
   - (a) Compress elsewhere to stay under
   - (b) Evacuate content to `docs/`
   - (c) Reject with `SKIP — rules/ aggregate cap breach`
3. **Use Tier classification** (see below) to pick best path.

### Fire-Rate Tiers T1/T2/T3

This file OWNS fire-rate tiers — how often a rule applies. The other "tier" taxonomy,
evacuation tiers E1/E2/E3 (what KIND of content moves and where), lives in
`improve-project-context/references/infrastructure-layers.md`. Never mix them in one
finding. Apply the tiers below to patches competing for rules/ space:

- **Tier 1 (Universal, always-on):** Fire ≥30% of sessions, apply across all projects. Examples: git safety, branching, commit format. **Protect in rules/.**
- **Tier 2 (Project-common, reflex-eligible):** Fire 10–30% of sessions within a project, or useful across 2+ projects. May live in global rules/ OR in project CLAUDE.md reflex triggers. Decide per patch.
- **Tier 3 (Niche, on-demand):** Fire <10% of sessions, highly specific to one project. Put in `docs/` + JIT pointer in CLAUDE.md. Keep rules/ lean.

Example: "Bash pipes must redirect logs to /tmp before grep" fires in ~80% of sessions, so it is Tier 1 — a CLAUDE.md line plus `docs/large-command-output.md` for the recipes. A single-project "run golangci-lint after every .go Edit" is Tier 2–3; belongs in project `.claude/CLAUDE.md` trigger OR `docs/<project-lint-loop>.md` + JIT pointer.

## Size Pressure Summary

**Caps are soft until they exceed their limit.** At >limit:

- **500 LOC rules/** → Patch MUST cut elsewhere OR get rejected. No exceptions.
- **100 LOC rules/X.md** → Split via `-{subdomain}` naming.
- **300 LOC skills/Y/SKILL.md** → Heavy sections to references/. Examples to bin/.
- **200/150 LOC CLAUDE.md** → Compress, move to rules/, or leave 1-line Read pointer.

When in doubt about "compress vs. evacuate vs. reject," check the Tier. Tier 1 never rejects; Tier 3 rarely stays in rules/.

## Each Patch MUST Include

```text
PLACEMENT OPTIONS: [1..8 zones, each SKIP/RECOMMENDED]
REASONING: <why recommended; why each SKIP ruled out>
TARGET FILE: <path> (<N> lines, cap <M>, safe: yes/no)
```

Ambiguous global vs project-specific → ask user: "Applies to all projects or only this one?"
