# Infrastructure Layers — Routing Zones, Caps, and Decomposition

Used by Step 5 (Apply Caps + Evacuation Tiers).

Two different taxonomies are called "tier" across these skills. This file owns
**evacuation tiers** — what KIND of content moves and where. Fire-rate tiers (how
often a rule applies: ≥30% / 10–30% / <10%) belong to
`improve-context-from-sessions/references/infrastructure-layers.md`. Do not mix them
in one finding.

## 4 Infrastructure Layers

| Layer             | What it is          | Examples                                        |
| ----------------- | ------------------- | ----------------------------------------------- |
| **Prompting**     | What the AI reads   | `CLAUDE.md`, `rules/*.md`, `docs/*.md`          |
| **Configuration** | Engine behavior     | `settings.json`                                 |
| **Executable**    | Terminal extensions | `commands/*.md`, `hooks/*.sh`                   |
| **Skills**        | Autonomous packages | `skills/{name}/SKILL.md`, `bin/`, `references/` |

## Routing Zones and Caps

| Zone                     | Cap               | Over-limit action                                                                                                 |
| ------------------------ | ----------------- | ----------------------------------------------------------------------------------------------------------------- |
| **rules/ AGGREGATE**     | **500 LOC total** | STOP. Compress target file, evacuate to docs/, or reject (rarity <10%). See `docs/context-budget.md`. |
| global/project CLAUDE.md | 200/150 lines     | Move to rules/. Leave 1-line Read pointer.                                                                        |
| rules/\*.md              | 100 lines         | Split into {name}-{subdomain}.md                                                                                  |
| skills/\*/SKILL.md       | 300 lines         | Examples >15L → references/. Branching shell → bin/.                                                              |
| commands/\*.md           | 150 lines         | Extract to docs/, JIT pointer stays.                                                                              |
| docs/\*.md               | no limit          | Split by domain if mixing concerns.                                                                               |

## Rules Aggregate Gate

**BEFORE** proposing a patch that touches any `~/.claude/rules/*.md`, measure:

```bash
wc -l ~/.claude/rules/*.md | tail -1
```

If patch would push total > 500, the patch MUST either:

1. **Compress elsewhere** to stay under, OR
2. **Evacuate content to `docs/`**, OR
3. Be rejected with `SKIP — would breach rules/ aggregate cap`

Never silently exceed. Load-bearing invariant: the rules/ cap keeps always-on
constraint files lean and machine-readable.

## Evacuation Tiers for Over-Cap Files

When a file threatens to exceed its cap, classify content by evacuation tier:

| Evacuation tier | Content type       | Action                                                                                                                                                |
| ---------- | ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| **E1** | Move wholesale to docs/ | Incident logs, historical rationale, multi-paragraph "why this exists" sections, skill navigation maps. Leave 1-line `Read` pointer in original file. |
| **E2** | Split files | File has both hard invariants AND rationale. Hard invariants stay in `rules/`, rationale → `docs/{name}-rationale.md`.                                |
| **E3** | Compress in place | Only `## Hard Rules` lists remain; examples, configs, command outputs move to `docs/`.                                                                |

**Example evacuation:**

File `rules/git.md` grows to 180 lines (cap is 100).

- **E1 candidate:** `## Why this exists` (35 lines) → `docs/<topic>-rationale.md`
- **E2 candidate:** Hard invariants (50 lines) stay; workflow examples (55 lines) → `docs/<topic>-workflow.md`
- **E3 candidate:** Inline command examples (20 lines) → `docs/<topic>-examples.md` (examples extracted), hard rules list remains in place

Result: `rules/git.md` = 50 lines (under cap) + 3 new docs/ files with 1-line pointers.

## File Layout Conventions

### skills/\*/SKILL.md — Compressed Algorithm (≤300 lines)

- Frontmatter (name, description, allowed-tools)
- 1–3 paragraph role manifest
- Surgical-edit invariants (2–5 bullets)
- Main algorithm: steps with JIT pointers to references/
- Guardrails (out-of-scope items)
- Quality gate (self-review checklist)

**Examples & branching shell:**

- Examples >15 lines → move to `references/` with titled `.md` files
- Branching shell scripts → move to `bin/` + call via explicit path in SKILL.md

### docs/ — Passive Knowledge (no line limit)

- JIT-loaded (loaded only when a rule or skill Read-pointer fires)
- Tutorials, schemas, ADRs, background, rationale, incident logs
- Examples, full workflows, edge cases, decision trees
- Prescriptive rules ALLOWED if JIT-triggered (e.g. `docs/context-map.md`, or a routing doc a skill reads on demand)
- Never loaded by default — only on explicit Read pointer fire
- **Style**: human-prose OK. Terse style NOT required (it's a `rules/` + `SKILL.md` style). See `docs/context-budget.md` Terse rule style section.

### rules/ — Always-On Constraints (100–500 LOC aggregate)

- REQUIRE, BANNED, TRIGGER statements
- Crisp and verifiable, no tutorials
- Navigation maps (decision trees, routing tables)
- Never tutorials or multi-paragraph rationale — those go to docs/

### docs/ vs rules/ — Decision Tree

```text
"Should this content be in rules/ or docs/?"

1. Does it define a HARD CONSTRAINT (BANNED/REQUIRE/TRIGGER)?
   YES  → rules/
   NO   → continue to 2

2. Is it loaded by default (on every session)?
   YES  → rules/ (only if also step 1)
   NO   → continue to 3

3. Is it a tutorial, example, or rationale paragraph?
   YES  → docs/
   NO   → continue to 4

4. Is it a schema, decision tree, or reference table?
   YES  → rules/ if crisp + always-applicable
       OR docs/ if niche / conditional
   NO   → docs/
```

## Progressive Disclosure Rule

Always-loaded files (CLAUDE.md, rules/) must be **navigation + constraints only** — never tutorials or examples.

**Test:** Can this section be removed from an always-loaded file without breaking any reflex trigger or hard rule? If YES, it belongs in `docs/` with a JIT pointer.

**Examples to move:**

- Multi-line "why this rule exists" narratives → `docs/{name}-rationale.md`
- Examples with output blocks → `references/` (if in skills) or `docs/` (if elsewhere)
- Historical incident logs → `docs/incidents/` (JIT pointer from rule)

## Rarity Gate for New rules/ Files

Active constraints that fire on a **NICHE task** (stacked-PR splits, cross-repo migrations, specific framework workflows) belong in `docs/` with a JIT pointer from an always-on `rules/` file, **NOT in a new always-on `rules/` file**.

**Threshold:** Fires < ~10% of applicable sessions → niche → default location is `docs/<topic>.md` + 1-line Read pointer from `rules/<related>.md`.

**Example:** "Rules for cross-repo code migrations" is niche (maybe 2–3 times per year). New rules file is overkill. Instead:

- Create `docs/<niche-topic>.md`
- Add 1-line pointer to `rules/git.md`: "→ Read docs/&lt;niche-topic&gt;.md for multi-repo workflows"

## Skill Layout Recap

```text
skills/skill-name/
├── SKILL.md              (≤300 LOC: frontmatter + role + invariants + steps with JIT pointers)
├── bin/
│   ├── run.sh            (executable script, called from SKILL.md)
│   └── lib.sh            (shared utilities)
└── references/
    ├── output-formatting.md    (examples of output card formats, tables)
    ├── algorithm-detail.md     (heavy algorithmic sections >50 LOC)
    └── {topic}-examples.md     (worked examples, edge cases)
```

Each references/ file is JIT-loaded only when explicitly Read-pointed from SKILL.md.
