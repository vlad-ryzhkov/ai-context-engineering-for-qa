---
name: agents-checker
description: "Verifies structural integrity and compliance of .claude/agents/ and .claude/qa_agent.md against init-agent standards."
allowed-tools: "Read Glob Grep Edit Write"
---

# /agents-checker — AI Agent Setup Validator

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


### Phase 5: Self-Healing (auto_fix=true)

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

## Output Format

Write the full report to `audit/agents-checker-report.md` using the Write tool. Do NOT output the full report body to chat — only the summary block and completion block go to chat.

**File structure** (`audit/agents-checker-report.md`):

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

```text
🛡️ AGENT SETUP AUDIT
├─ qa_agent.md:   [✅ PASS / ⚠️ WARNINGS / ❌ FAIL]
├─ auditor.md:    [✅ PASS / ⚠️ WARNINGS / ❌ FAIL]
├─ sdet.md:       [✅ PASS / ⚠️ WARNINGS / ❌ FAIL]
└─ Cross-refs:    [✅ PASS / ⚠️ N broken refs]

📝 Decision: [APPROVE / PASS WITH WARNINGS / ACTION RECOMMENDED]
→ Full report: audit/agents-checker-report.md
```

Finish with the standard completion block:

```text
✅ SKILL COMPLETE: /agents-checker
├─ Artifacts: audit/agents-checker-report.md
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
- `auto_fix=true`: all safe issues repaired with logged rationale; re-validation passes; non-fixable issues remain in report with `manual fix required` label
