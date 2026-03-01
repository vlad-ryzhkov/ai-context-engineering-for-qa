---
name: update-ai-setup
description: Scans project AI files and updates the docs/ai-setup.md Registry with current data. Use to synchronize the Registry after adding/removing Skills, patterns, or configs. Do not use to create a Registry from scratch — create docs/ai-setup.md manually instead.
allowed-tools: "Read Write Edit Glob Grep Bash(wc*) Bash(ls*)"
agent: agents/auditor.md
context: fork
---

# /update-ai-setup — AI Configuration Registry Update

<purpose>
Automatic update of docs/ai-setup.md — the Registry of all AI patterns, Skills, and configuration files in the project. Scans the actual file state and synchronizes the Registry.
</purpose>

## Prerequisites

Read `.claude/qa_agent.md` and `.claude/agents/auditor.md`.

## When to Use

- After adding/removing a Skill
- After changing qa_agent.md, agents/*.md, or CLAUDE.md
- After adding/removing Anti-patterns
- After changing plugins in `.claude/settings.json`
- After changing MCP servers in `.mcp.json`
- Periodic Registry freshness check

## Input

None required. The Skill runs automatically based on the current file state.

## Algorithm

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the Artifact (MD/HTML), not into chat.

**Chat output (limits):**
- Brief Summary: max 5 lines (what was found, how many, result)
- Findings table: max 15 lines (top by severity)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress per file. Checkpoint only on:
- Phase transition (Phase N → Phase N+1)
- Warning detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, no "Now I will grep..."
- Read → analyze → report, no "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist), not a separate file.

### Step 1: Read Current Registry

Read `docs/ai-setup.md`. If the file does not exist:

```text
⚠️ WARNING: docs/ai-setup.md not found — creating a new file.
```

The Skill creates an empty Registry based on the template `.claude/skills/init-skill/references/skill-template.md` and continues scanning.

Record current data: file list, line counts, number of patterns, Skills, Anti-patterns.

### Step 2: Scan Files

Run Glob across all AI files:

```text
Required:
  - CLAUDE.md
  - .claude/qa_agent.md
  - .claude/settings.json
  - .mcp.json
  - .claude/skills/*/SKILL.md
  - .claude/qa-antipatterns/_index.md
  - .claude/qa-antipatterns/*/*.md
  - .claude/skills/*/references/*
  - docs/ai-files-handbook.md
  - docs/ai-setup.md
```

For each found file, count lines via `wc -l`.

### Step 2b: Scan Plugins and MCP Servers

1. Read `.claude/settings.json` → extract keys from `enabledPlugins`
2. Read `.mcp.json` → extract keys from `mcpServers`
3. Compare with "Plugins" and "MCP Servers" tables in `docs/ai-setup.md`
4. Delta: add new / remove missing / update changed rows

### Step 3: Detect Delta

Compare scan results with the current Registry:

| Check | Action |
|-------|--------|
| New file (exists on disk, missing from Registry) | Add to the corresponding table |
| Deleted file (exists in Registry, missing from disk) | Remove from table |
| Size change > 20% | Update line count |
| New Skill | Add to Skills table + verify CLAUDE.md |
| New Anti-pattern | Add to Anti-patterns table |

If no delta — notify the user and finish:

```text
✅ Registry is up to date. No changes detected.
```

### Step 4: Update Document

Update `docs/ai-setup.md`:

1. **Architecture diagram** — update line counts in layers
2. **Inventory tables** — add/remove rows, update counts
3. **Pattern catalog** — add new patterns (if detected), update references

Changelog entry format:

```markdown
| YYYY-MM-DD | [Description: what was added/removed/updated] |
```

### Step 5: Validation and Diff Display

Before saving:

1. Verify Quality Gates (see below)
2. Show the user the list of changes:

```text
📊 DELTA REPORT
├─ Added: [N files/patterns]
├─ Removed: [N files/patterns]
├─ Updated: [N line counts]
├─ Health Metrics: [updated / no data]
└─ Changelog: [brief entry description]
```

3. Save the updated `docs/ai-setup.md`

## Output Format

```text
✅ SKILL COMPLETE: /update-ai-setup
├─ Artifacts: docs/ai-setup.md
├─ Delta: [+N added, -N removed, ~N updated | "no changes"]
├─ Quality Gates: PASS
└─ Changelog: [brief description]
```

## Quality Gates

- [ ] All paths in the document exist on disk (verify with Glob)
- [ ] Line counts match `wc -l`
- [ ] No placeholders `[xxx]` in the final document
- [ ] Changelog contains a new entry with the current date
- [ ] Skill count in Registry = number of `.claude/skills/*/SKILL.md`
- [ ] Anti-pattern count in Registry = number of `.claude/qa-antipatterns/*/*.md` (files in subdirectories, excluding `_index.md`)
- [ ] Plugin count in table = number of keys in `settings.json → enabledPlugins`
- [ ] MCP server count in table = number of keys in `.mcp.json → mcpServers`

## Anti-Patterns

| Anti-Pattern | Why It Breaks | Fix |
|---|---|---|
| Line count not verified with `wc -l` | Registry documents stale sizes; capacity tracking wrong | After scanning each file, record `wc -l` result. Compare with Registry. |
| New skill added to `.claude/skills/` but not in Registry | Skill exists but AI setup docs don't list it; hidden from users | Skill count in Registry MUST equal number of `.claude/skills/*/SKILL.md` files. |
| Plugins/MCP servers changed but Registry unchanged | Docs lag behind reality; users unaware of available integrations | After reading settings.json and .mcp.json, update both tables in Registry. |
| No changelog entry for the update | No audit trail; can't track when Registry was last refreshed | Every Registry update includes changelog entry with date and delta (e.g., "+1 skill"). |
| `[xxx]` placeholders left in final Registry document | Template cruft misleads users; unprofessional | All `[xxx]` fields MUST be replaced with actual data. Verify before saving. |
| Delta detection skipped (assumes no changes) | Missed files may be undocumented; Registry degrades over time | Always compare scan results with current Registry. Never assume no delta. |

## Quality Gate (Self-Review)

Before saving the updated Registry:

- [ ] All paths on disk verified via Glob
- [ ] Line counts match `wc -l` results
- [ ] No `[xxx]` placeholders in final document
- [ ] Changelog contains new entry with current date
- [ ] Skill count = number of `.claude/skills/*/SKILL.md` files
- [ ] Anti-pattern count = number of `.claude/qa-antipatterns/*/*.md` files
- [ ] Plugin table count = keys in `settings.json → enabledPlugins`
- [ ] MCP server count = keys in `.mcp.json → mcpServers`

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

---

## Related Files

- Registry: `docs/ai-setup.md`
- Guide: `docs/ai-files-handbook.md`
- Skill template: `.claude/skills/init-skill/references/skill-template.md`
