---
name: skill-audit
description: Audit SKILL.md and qa_agent.md for bloat, duplication, harmful patterns ("DO NOT FIX", bloated templates). Use to optimize AI setup and reduce token usage. Do not use for documentation audit — use /doc-lint instead.
allowed-tools: "Read Write Edit Glob Grep Bash(wc*)"
agent: agents/auditor.md
context: fork
---

# Skill & Agent Audit

Audit AI instructions for efficiency: detect bloat, duplication, harmful patterns.

## Before You Start

Read:
1. `.claude/qa_agent.md` and `.claude/agents/auditor.md`
2. `.claude/skills/init-skill/references/validation-checklist.md` — line thresholds and required sections
3. `.claude/skills/init-skill/references/yaml-reference.md` — YAML frontmatter rules

---

## When to Use

- After creating a new skill via `/init-skill`
- When a skill is suspected of excessive token usage
- Periodically (once per sprint) for all skills
- After updating CLAUDE.md or qa_agent.md

---

## Input

| Parameter | Required | Description |
|-----------|:--------:|-------------|
| Scope | Optional | Path to a specific skill or "all". Defaults to all skills |

---

## Algorithm (9 Checks)

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the artifact (MD/HTML), not into chat.

**Chat output (constraints):**
- Brief Summary: max 5 lines (what was found, count, verdict)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress per file. Checkpoint only on:
- Phase transition (Phase N → Phase N+1)
- Blocker detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, no "Now I will grep..."
- Read → analyze → report, no "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5–7 line checklist), not a separate file.

### Check 0: Standards Drift

Verify that thresholds in this SKILL.md match `init-skill/references/validation-checklist.md`:
- SKILL.md line limit (current checklist value: ≤500)
- Required YAML frontmatter fields
- Required content sections

If drift found → **ERROR** "Standards Drift: {field} in audit={X}, in checklist={Y}".
Recommendation: update thresholds in skill-audit/SKILL.md per checklist.

### Check 1: Line Count

For each SKILL.md — `wc -l`. Threshold taken from `init-skill/references/validation-checklist.md` (section "Structure", field SKILL.md ≤ N lines):

| Threshold (per checklist) | Severity |
|---------------------------|----------|
| ≤ threshold | OK |
| threshold+1 … threshold×1.1 | WARNING |
| > threshold×1.1 | CRITICAL |

*Current threshold per checklist: **500 lines***.

For qa_agent.md: OK ≤200, WARNING 201-300, CRITICAL >300.

### Check 1a: YAML Compliance

For each SKILL.md verify frontmatter against rules from `init-skill/references/yaml-reference.md`:

- `name` in kebab-case, matches folder name, no "claude"/"anthropic"
- `description` contains three parts: **What / When / When NOT**
- `description` < 1024 chars, no XML characters (`<`, `>`), single-line
- If `agent:` is present — referenced file exists

Severity: **ERROR** (required field missing), **WARNING** (description format violation).

### Check 1b: Verbosity Protocol

Grep: `## Verbosity Protocol`, `SILENT MODE`, `NO CHAT TABLES` in SKILL.md files.

- Severity: **CRITICAL** (if absent)
- Why: Agents without this protocol pollute chat, output intermediate tables and lists, waste tokens on chatter
- **Exception:** If the skill has `agent:` in frontmatter — check for Verbosity Protocol in the agent file (`agents/{name}.md`). No need to duplicate in SKILL.md — flag only if absent from both SKILL.md and the agent.
- Recommendation: Add Verbosity Protocol to the agent file (not to SKILL.md)

### Check 2: Self-Review Protocol (Bloated)

Grep: `Self-Review`, `self_review`, `_self_review.md`, report templates with `Scorecard`.

- Severity: **WARNING** (only if self-review template >50 lines or lacks Scorecard)
- Why: bloated templates waste tokens; compact Scorecards are a useful tracking tool
- Recommendation: optimize template to ≤50 lines with mandatory Scorecard
- **Exceptions:**
  - `*_self_review.md` files with Scorecard — valuable progress tracking artifacts. Do not flag

### Check 3: "DO NOT FIX" Instruction

Grep: `НЕ ИСПРАВЛЯТЬ`, `не исправляй`, `только анализ` — in the context of review/check sections.

- Severity: **CRITICAL**
- Why: AI documents problems instead of fixing them
- Recommendation: replace with "FIX the code/audit, recompile"

### Check 4: Tech Stack Duplication

1. Read CLAUDE.md → find Tech Stack
2. Grep each SKILL.md for stack keywords (Ktor, Jackson, Kotest, etc.)
3. If SKILL.md contains a full stack table (≥4 rows with `|`) → duplication

- Severity: **WARNING**
- Recommendation: replace table with `Stack LOCKED in CLAUDE.md → Tech Stack` + additions

### Check 5: Code Examples >50 Lines

Find code blocks (```kotlin, ```python, etc.) in SKILL.md. Count lines in each.

- Severity: **WARNING** (if block >50 lines)
- Recommendation: extract to `references/examples.md`, keep 3–4 spec lines + link

### Check 6: Decorative Code Blocks

Find ``` blocks that do NOT contain code:
- No language identifier
- Content = text with emoji/bullet points/markdown formatting

- Severity: **INFO**
- Recommendation: replace with plain lists/bold text

### Check 7: Anti-Patterns Verbosity

Find BANNED/Anti-Patterns sections. Count lines and paired Bad/Good blocks (❌/✅ with code).

- Severity: **WARNING** (if pairs >3 and lines >30)
- Recommendation: replace with one-liners, details → `qa-antipatterns/*.md` or skill-specific references/

### Check 8: Cross-Reference Staleness

1. Collect references from qa_agent.md to skill sections/patterns
2. Verify that referenced sections exist in current SKILL.md files
3. Check Skill Completion Protocol for references to deleted patterns

- Severity: **ERROR**
- Recommendation: update qa_agent.md

### Check 9: Rarely-Used Sections Inline

Find sections with:
- "prompts for customization/generation/adaptation"
- Meta-instructions for the user (not for AI during execution)
- Content used once per project but loaded on every invocation

- Severity: **INFO**
- Recommendation: extract to `references/`

### Check 9a: Artifact Timestamping

For skills that generate file artifacts (e.g., `/spec-audit`, `/test-cases`, `/api-tests`), verify:
1. Output Template specifies **timestamped filenames** in format `{filename}_{timestamp}` (e.g., `test-scenarios_{timestamp}.md`, `spec-audit_{timestamp}.md`)
2. Completion Contract mentions that **each invocation creates a new file** (see spec-audit/SKILL.md for reference)
3. If skill outputs multiple files, **each gets a unique timestamp** or **shared timestamp prefix**

**Why:** Prevents accidental overwrites and maintains audit history per invocation.

- Severity: **CRITICAL** (if artifact-generating skill lacks timestamping)
- Skills to check: `/spec-audit`, `/test-cases`, `/api-tests`, `/repo-scout`
- Recommendation: Add timestamp format `YYYYMMDD_HHMMSS` to Output Template and Completion Contract

---

## Report Format

Write the full report to `audit/skill-audit-report.md` (findings table + Summary).

Output to chat only:
```text
📊 Skill Audit: {N} CRITICAL, {N} WARNING, {N} INFO → audit/skill-audit-report.md
```

---

## Severity Model

| Severity | What it catches |
|----------|-----------------|
| **CRITICAL** | "DO NOT FIX", SKILL.md >500 lines, Artifact-generating skills without timestamping (spec-audit, test-cases, api-tests, repo-scout) |
| **ERROR** | Stale cross-references in qa_agent.md |
| **WARNING** | Self-Review Protocol (bloated >50 lines), Tech Stack duplication, code >50 lines inline, Anti-Patterns >30 lines, 300–500 lines |
| **INFO** | Decorative ``` blocks, rarely-used sections inline |

---

## Post-Audit Check (to chat, DO NOT create a file)

- [ ] All skills in scope checked?
- [ ] Line counts verified via `wc -l`?
- [ ] No false positives (context of each finding verified)?
- [ ] Recommendations are specific (what → where)? ← each recommendation names a specific file and action, not "improve" or "reconsider"

**If you found an error in the audit → fix it.**
DO NOT create *_self_review.md.

---

### Completion

After Post-Audit Check — print the `SKILL COMPLETE` block (format in qa_agent.md § Skill Completion Protocol).
