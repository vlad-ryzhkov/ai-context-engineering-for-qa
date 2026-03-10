---
name: agents-checker
description: "Verifies structural integrity and compliance of .claude/agents/ and .claude/qa_agent.md against init-agent standards. Use after modifying agent files. Do not use for SKILL.md audit — use /skill-audit."
allowed-tools: "Read Glob Grep Edit Write"
---

# /agents-checker — AI Agent Setup Validator

> **SILENT MODE**: Execute all validation phases silently. Do not output intermediate
> findings or progress per file. Only the final audit report and SKILL COMPLETE block go to chat.

> **Status Reporting**: After Phase 2 (qa_agent.md checks) and Phase 3 (each agent file),
> emit a single-line JSON progress: `{"phase": N, "files_scanned": N, "issues_found": N}`

**Type:** Audit Utility
**Category:** QA Setup / Compliance

Checks that the project's Claude agent files comply with the standards produced by `/init-agent`. Validates structure, required sections, cross-references, and absence of stale placeholders.

## When to Use

- After manually editing `qa_agent.md` or any file in `.claude/agents/`
- After adding a new agent
- Before running `/update-ai-setup`
- As a pre-flight check in the QA pipeline

## Scope

| Target | Path |
|--------|------|
| QA Lead profile | `.claude/qa_agent.md` |
| Agent files | `.claude/agents/*.md` |
| Skill cross-refs | `.claude/skills/*/SKILL.md` frontmatter |

## Input Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `auto_fix` | No | `true` | When `true`, auto-repairs safe deterministic issues after full audit and logs each change with rationale. Unsafe issues (missing files, role-specific content) are always flagged only. |

---

## Workflow

### Phase 1: File Existence

Check each required file is present:
- `.claude/qa_agent.md`
- `.claude/agents/auditor.md`
- `.claude/agents/sdet.md`

**If any file is missing → 🔴 CRITICAL. Stop and report. Do not proceed to Phase 2.**

### Phase 2: qa_agent.md Structure

Load `.claude/skills/init-agent/references/qa-agent-template.md` as the reference standard.

Verify `qa_agent.md` contains all required sections:

| Check | Rule | Severity |
|-------|------|----------|
| `## Core Mindset` present | Must have 3–5 principles listed | 🔴 CRITICAL |
| `## Anti-Patterns (BANNED)` present | Table with ≥ 5 rows | 🔴 CRITICAL |
| `## Quality Gates` present | Must contain Before Commit + Before PR sections | 🟠 MAJOR |
| `## Cross-Skill Protocol` present | Must reference `/spec-audit` → `/api-isolated-tests` → `/api-tests` | 🟠 MAJOR |
| No placeholders | Grep for `[xxx]` pattern — none allowed | 🟠 MAJOR |
| Anti-patterns match role | qa_agent.md is an Orchestrator — anti-patterns must cover orchestration failures (e.g. micro-management, blind approval, silent looping). Universal code-level patterns (Thread.sleep, assertEquals) belong in agent files, NOT here. | 🟡 MINOR |

### Phase 3: Agent File Validation

For **each** `.md` file in `.claude/agents/`:

| Check | Rule | Severity |
|-------|------|----------|
| Role heading present | `## Role` or `# [Name] Agent` | 🔴 CRITICAL |
| `## Core Mindset` present | At least 3 principles | 🔴 CRITICAL |
| `## Anti-Patterns (BANNED)` present | Table with ≥ 3 rows | 🟠 MAJOR |
| `## Verbosity Protocol` present | Must define output format | 🟠 MAJOR |
| `## Quality Gates` present | At least Commit + PR gates | 🟠 MAJOR |
| Skills list defined | `## Skills` section lists at least 1 skill | 🟠 MAJOR |
| `## Output Contract` present | Defines artifact format | 🟡 MINOR |
| No placeholders | Grep for `[xxx]` — none allowed | 🟠 MAJOR |

### Phase 4: Cross-Reference Integrity

1. **Skills referenced in agent files exist:**
   - Extract all `/skill-name` mentions from each agent file
   - For each: check `.claude/skills/{skill-name}/SKILL.md` exists
   - Missing skill → 🟠 MAJOR

2. **Agent references in SKILL.md frontmatter are valid:**
   - Glob all `.claude/skills/*/SKILL.md`
   - Extract `agent:` field from frontmatter (if present)
   - Verify the referenced file exists under `.claude/agents/`
   - Broken reference → 🟠 MAJOR


### Phase 5: Agent File Duplication

Check for content in `.claude/agents/*.md` that duplicates `qa_agent.md` verbatim or near-verbatim.

Algorithm:
1. Read all files in `.claude/agents/` and `.claude/qa_agent.md`
2. LLM step: for each agent file, compare each section against the corresponding section in `qa_agent.md`. Flag content that is duplicated word-for-word or with only minor rephrasing
3. Severity: **WARNING** — "Content in `agents/{file}.md § {section}` duplicates `qa_agent.md § {section}`. Canonical location: `qa_agent.md`. Consider removing from agent file and adding `→ see qa_agent.md`."
4. Exception: `## Anti-Patterns (BANNED)` may intentionally contain agent-specific rules — flag only exact/verbatim copies, not agent-specific content

**Not fixable via auto_fix** — removing content requires understanding of intent; always flag for manual review.

---

### Phase 6: Self-Healing (auto_fix=true)

**Skipped if `auto_fix=false`.** Runs after Phase 4 only if audit found fixable issues.

**Fixable issues — safe to auto-repair:**

| Finding | Fix Action | Rationale |
|---------|------------|-----------|
| Placeholder patterns (`[Profile]`, `[Language]`, `[Actor]`, `[xxx]`) in any agent file | Remove the containing line via Edit | Placeholders are unfilled template slots — they add noise and confuse the AI reading the file |
| `## Anti-Patterns (BANNED)` missing in `qa_agent.md` | Append section verbatim from `init-agent/references/qa-agent-template.md` | Section is fully standardized in the template; copying it is deterministic and safe |
| `## Cross-Skill Protocol` missing in `qa_agent.md` | Append section from template | Same rationale — canonical content, no role-specific judgment needed |
| Broken `agent:` reference in SKILL.md frontmatter (target file missing) | Remove the `agent:` line from frontmatter | A broken ref causes skill routing errors; removing it degrades to default gracefully |

**NOT fixable — always flagged, never auto-repaired:**

| Issue | Reason |
|-------|--------|
| Required file missing (`qa_agent.md`, `auditor.md`, `sdet.md`) | Cannot fabricate role-specific content |
| `## Core Mindset` missing | Content is role-specific — wrong defaults are worse than absence |
| `## Role` heading missing in agent file | Requires understanding of agent purpose |
| Skill cross-references to non-existent skills | May be intentional (planned future skill) |

**After fixes:** Re-run Phases 2–3 on modified files to confirm healing was effective. Report delta.

---

## Anti-Patterns

| Anti-Pattern | Why It Breaks | Fix |
|---|---|---|
| Agent file missing `## Role` heading | AI doesn't know which agent this is; routing fails | Add explicit role heading: `## Role: API Test Specialist` |
| Identical content in agent file and `qa_agent.md` (verbatim copy) | Duplication wastes tokens; conflicting updates | Keep canonical content in `qa_agent.md`; use reference in agent file if needed |
| Placeholder text `[Profile]`, `[Language]`, `[xxx]` left unfilled | Template cruft confuses the AI; treated as actual rules | Remove all placeholder lines after initial setup |
| Broken `agent:` reference in SKILL.md | Skill cannot find its agent profile; falls back to defaults | Verify agent file exists at `.claude/agents/{name}.md` |
| `## Anti-Patterns` table with < 3 rows | Insufficient guidance for that role | Add role-specific patterns from the agent's domain |
| Missing `## Verbosity Protocol` in agent file | AI outputs verbose intermediate findings; wastes tokens | Add protocol section explaining output format |

---

### Quality Gate (Self-Review)

Before finalizing the audit:
- [ ] All 3 required files scanned (qa_agent.md + all agents/*.md)
- [ ] Phases 1–5 completed without early exit
- [ ] No critical issues left unflagged or auto-repaired unsafely
- [ ] Report artifact generated with timestamp
- [ ] JSON progress emitted after Phases 2–3

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

---

## Output Format

Derive the `{agent}` slug from the skill argument:
- Specific agent file (e.g., `agents/sdet.md`) → basename without extension: `sdet`
- Directory or multiple targets (e.g., `.claude/agents`, `all`) → `all`
- Sanitize: replace `/`, spaces, and dots with `-`, lowercase

Obtain timestamp via `date +%Y%m%d_%H%M%S`. Use it as the suffix.
Write the full report to `audit/agents-checker-report_{agent}_{YYYYMMDD_HHMMSS}.md` using the Write tool. Do NOT output the full report body to chat — only the summary block and completion block go to chat.

**File structure** (`audit/agents-checker-report_{agent}_{YYYYMMDD_HHMMSS}.md`):

```markdown
# Agent Setup Audit Report
Date: {YYYY-MM-DD}

## Summary
| File | Status |
|------|--------|
| qa_agent.md | ✅ PASS / ⚠️ WARNINGS / ❌ FAIL |
| auditor.md  | ✅ PASS / ⚠️ WARNINGS / ❌ FAIL |
| sdet.md     | ✅ PASS / ⚠️ WARNINGS / ❌ FAIL |
| Cross-refs  | ✅ PASS / ⚠️ N broken refs      |

## Findings

| # | Severity | File | Issue | Rule |
|---|----------|------|-------|------|
| 1 | 🔴 CRITICAL | .claude/qa_agent.md | Missing ## Anti-Patterns table | init-agent template |
| 2 | 🟠 MAJOR | .claude/agents/auditor.md | /health-check skill not found | Phase 4 cross-ref |
| 3 | 🟡 MINOR | .claude/agents/sdet.md | ## Output Contract missing | Phase 3 check |

## Auto-Fix Log
- .claude/qa_agent.md — Appended ## Anti-Patterns from template
  → Reason: ...

## Recommendations (Manual Fix Required)

### 1. [🟠] .claude/qa_agent.md — <issue title>
**What:** ...
**How to fix:** ...

### 2. [🟡] .claude/agents/auditor.md — <issue title>
**What:** ...
**How to fix:** ...

## Decision
📝 [APPROVE / PASS WITH WARNINGS / ACTION RECOMMENDED]
```

**Chat output** (summary only):

**All-pass shortcut:** If all files pass with ✅ PASS and cross-refs are clean → replace the 4-row status table with a single line:
```text
✅ All files compliant (qa_agent.md, auditor.md, sdet.md, cross-refs)
```
Only expand the status table when ≥1 file has ⚠️ WARNINGS or ❌ FAIL.

```text
🛡️ AGENT SETUP AUDIT
├─ qa_agent.md:   [✅ PASS / ⚠️ WARNINGS / ❌ FAIL]
├─ auditor.md:    [✅ PASS / ⚠️ WARNINGS / ❌ FAIL]
├─ sdet.md:       [✅ PASS / ⚠️ WARNINGS / ❌ FAIL]
└─ Cross-refs:    [✅ PASS / ⚠️ N broken refs]

📝 Decision: [APPROVE / PASS WITH WARNINGS / ACTION RECOMMENDED]
→ Full report: audit/agents-checker-report_{agent}_{YYYYMMDD_HHMMSS}.md
```

Finish with the standard completion block:

```text
✅ SKILL COMPLETE: /agents-checker
├─ Artifacts: audit/agents-checker-report_{agent}_{YYYYMMDD_HHMMSS}.md
├─ Compilation: N/A
├─ Upstream: .claude/skills/init-agent/references/qa-agent-template.md
└─ Coverage: [X/Y checks passed]
```

## Definition of Done

- All 3 required files confirmed present
- `qa_agent.md` contains all 5 required sections from template
- Each agent file contains Role + Mindset + Anti-Patterns + Quality Gates + Skills
- Zero broken cross-references to non-existent skills
- No `[placeholder]` text in any agent file
- Agent file duplication check completed: verbatim duplicates of `qa_agent.md` content flagged
- `auto_fix=true`: all safe issues repaired with logged rationale; re-validation passes; non-fixable issues remain in report with `manual fix required` label
