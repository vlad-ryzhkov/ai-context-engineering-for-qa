---
name: doc-lint
description: Documentation quality audit — size, structure, cross-file duplicates, SSOT violations. Use for quality control of human-readable files, finding duplication, and structure verification. Do not use for code review or source code analysis.
allowed-tools: "Read Write Edit Glob Grep Bash(wc*)"
agent: agents/auditor.md
context: fork
---

# /doc-lint — Documentation Quality Audit

<purpose>
Scans all human-readable project files, finds issues with size, structure, cross-file duplicates, and SSOT (Single Source of Truth) violations. Generates a report with prioritized findings and a refactoring plan.
</purpose>

## Before Starting

Read `.claude/qa_agent.md` and `.claude/agents/auditor.md`.

## When to Use

- After adding a new document or skill
- When cross-file content duplication is suspected
- For periodic documentation audit (once per sprint)
- Before documentation refactoring

## Input

| Parameter | Required | Description |
|-----------|:--------:|-------------|
| Scope | Optional | Specific files/directories. Default — entire project |
| Focus | Optional | Specific phases only (size, structure, duplicates) |

---

## Algorithm (6 Phases)

## Verbosity Protocol (STRICT)

**SILENT MODE ENFORCED:**
1. **NO CHAT TABLES:** Never output tables (Inventory, Findings, Stats) to chat. Only to the report file.
2. **NO LISTS:** Do not list checked files in chat.
3. **ONLY STATUS:** Output to chat **only** the final `SKILL COMPLETE` block and the report path.

**Example of the only acceptable chat output:**
> 📝 Audit Complete.
> 📊 Report: `audit/doc-lint-report.md`
> 📉 Health Score: 78/100
> 💡 Action: Run `bash audit/safe-fix.sh` to apply safe fixes.

### Phases 1-7: Detailed Algorithm

Full description of all phases (Discovery, Size Analysis, Structure Analysis, Cross-File Duplicate Detection, Content Hygiene, Report Generation, Safe-Fix Script) — in `references/phases.md`.

---

## Severity Model

| Severity | Criteria |
|----------|----------|
| **CRITICAL** | Actual limit exceeded (>700 generic, >500 SKILL); Broken links (file not found) |
| **WARNING** | Approaching limit (90% of threshold); Duplicates >10 lines; Wall-of-text >30 lines |
| **INFO** | TODO markers; Minor duplicates (3-5 lines); Stale dates; Formatting issues |

---

## Health Score Logic

Start Score: 100.

**Deductions:**
- CRITICAL: -15 points (per finding)
- WARNING: -5 points
- INFO: -0.5 points (reduced weight for noise)

**Formula:** `MAX(0, 100 - (Count_Crit * 15) - (Count_Warn * 5) - (Count_Info * 0.5))`

*No bonus points for "good behavior".*

| Range | Rating | Interpretation |
|-------|--------|----------------|
| 90-100 | Excellent | Documentation is in excellent condition |
| 70-89 | Good | Minor issues present |
| 50-69 | Needs attention | Refactoring required |
| <50 | Refactoring needed | Urgent documentation refactoring |

**Formula MUST be shown with substituted values:**
```text
Score = 100 - (2 × 15) - (5 × 5) - (8 × 0.5) = 100 - 30 - 25 - 4 = 41/100
```

---

## Output Format

### Artifact: `audit/doc-lint-report.md`

```markdown
# Doc-Lint Report

> Date: {YYYY-MM-DD}
> Scope: {scope description}
> Health Score: {N}/100 ({rating})

## Summary

| Metric | Value |
|--------|-------|
| Files scanned | N |
| CRITICAL | N |
| WARNING | N |
| INFO | N |
| Health Score | N/100 |
| Duplicate clusters | N |

## File Inventory

| # | File | Lines | Type | Size Status |
|---|------|------:|------|-------------|
| 1 | ... | ... | ... | OK/WARNING/CRITICAL |

## CRITICAL Findings

| # | File | Phase | Description | Recommendation |
|---|------|-------|-------------|----------------|

## WARNING Findings

| # | File | Phase | Description | Recommendation |
|---|------|-------|-------------|----------------|

## INFO Findings

| # | File | Phase | Description | Recommendation |
|---|------|-------|-------------|----------------|

## Duplicate Map

### Cluster D-1: {pattern name}
- **Type:** Exact / Near-duplicate / Conceptual
- **SSOT Owner:** {file}
- **Found in:** {list of files with line numbers}
- **Recommendation:** Keep in {Owner}, replace with link in the rest

### Cluster D-N: ...

## SSOT Refactoring Plan

| # | Action | File | What to do |
|---|--------|------|------------|
| 1 | REMOVE | file.md:10-25 | Remove Tech Stack copy, add link |

## Statistics

- Total documentation volume: {N} lines in {M} files
- Average file size: {N/M} lines
- Files within limits: {X}/{M} = {%}
- Health Score: {formula with substitution}
```

### Post-Check Scorecard

Post-Check format — same as `/spec-audit` (see qa_agent.md § Skill Completion Protocol, qa_agent.md § Quality Gates).

**Post-Check Scorecard:**

```markdown
## Scorecard

| Criterion | Result |
|-----------|--------|
| All files scanned | X/Y = NN% |
| Line counts verified | ✅/❌ |
| Cross-file detection completed | ✅/❌ |
| Every finding has severity + recommendation | X/Y = NN% |
| No placeholder {xxx} | ✅/❌ |
| SSOT owner assigned for every cluster | X/Y = NN% |
| Formulas with numerator/denominator | ✅/❌ |

### Final Score: NN%
```

---

## Quality Gates

- [ ] All files in scope scanned (Glob + count verification)
- [ ] Line counts verified via `wc -l`
- [ ] Cross-file duplicate detection completed (Phase 4)
- [ ] Every finding has severity + recommendation
- [ ] No placeholder `{xxx}` in the report
- [ ] SSOT owner assigned for every duplicate cluster
- [ ] Formulas shown with numerator and denominator (CLAUDE.md requirement)
- [ ] Health Score calculated and shown with substitution

---

## Anti-Patterns (BANNED)

### False Positive on Identical Headers

```text
❌ Flagging tables with identical headers but different data as "duplicate"
✅ Compare cell content, not just headers
```

### Phantom Findings

```text
❌ Generating findings based on assumptions without reading the file
✅ Every finding confirmed by file content (line, excerpt)
```

### Missing Context

```text
❌ "File is too long"
✅ "CLAUDE.md: 305 lines > CRITICAL threshold (300). Recommendation: extract section X to qa_agent.md"
```

### Over-flagging Intentional Repetition

```text
❌ Flagging pattern references as duplicates (references are not duplicates)
✅ Distinguish full copying from references and brief mentions
```

---

## Related Files

| File | Content |
|------|---------|
| `references/check-rules.md` | Size thresholds, duplicate signatures, SSOT matrix, Diataxis markers |
| `references/best-practices.md` | Industry practices: Google, Amazon, Diataxis, Microsoft, GitLab, Stripe |

---

## Completion

After creating the report and script — output the `SKILL COMPLETE` block (format in qa_agent.md § Skill Completion Protocol).

```text
✅ SKILL COMPLETE: /doc-lint
├─ Artifacts: audit/doc-lint-report.md, audit/safe-fix.sh
├─ Compilation: N/A
├─ Upstream: none
└─ Score: {Health Score}/100
```
