# Reflector Protocol — Proactive Pattern Detection

## Purpose

Analyzes accumulated execution data to detect recurring patterns and formulate
actionable rules. Two-layer architecture: bash detection (Layer 1) + LLM formulation (Layer 2, this protocol).

## When to Run

- After `bash scripts/lib/reflector.sh` produces a detection report
- Threshold: run when `tests/telemetry/events.jsonl` has >= 10 events
- Manual trigger: user says "run reflector" or "analyze patterns"

## Prerequisites

- `tests/telemetry/reflector-report.json` exists (produced by Layer 1)
- Report has >= 1 finding

## Algorithm

### Step 1: Read Detection Report

Read `tests/telemetry/reflector-report.json`. If no findings → output
"Reflector: no recurring patterns detected" and STOP.

### Step 2: Enrich with Context

For each finding, read the full `evidence` entries from `.ai-lessons/gardener-log.jsonl`
to understand the actual observations (not just keyword signatures).

### Step 3: Semantic Dedup (critical — grep is insufficient)

For each finding, perform **semantic comparison** against:

- `CLAUDE.md` (BANNED, conventions)
- `.claude/qa-antipatterns/**/*.md`
- `.claude/skills/*/SKILL.md` (BANNED, Quality Gates)
- `.claude/protocols/*.md`
- `.ai-lessons/pending.md` (existing entries)

Read candidate files. Compare **intent**, not just keywords.
Assign verdict: `COVERED` (existing rule handles this) | `NEW` (no coverage).

### Step 4: Formulate Rules

For each `NEW` finding, formulate an actionable rule:

- Use prohibition/requirement format (not wishes)
- Include cross-skill scope: "Recurs across {skills}"
- Include evidence count: "{N}x occurrences"
- For `recurring_failure`: diagnose probable root cause from error pattern

### Step 5: Append to pending.md

For each formulated rule, append to `.ai-lessons/pending.md`:

```text
- [REFLECTOR] RULE: {text}. Pattern: {detector}, {count}x across {skills}. Source: reflector, {date}
```

For `COVERED` findings: output "Existing rule covers: {ref}" — do not append.

### Step 6: Output Summary

```text
REFLECTOR ANALYSIS
|- Findings: {total} patterns detected
|- New rules: {N} appended to pending.md
|- Covered: {M} already handled by existing rules
|- Pending.md: now has {total} entries (threshold for /curate-lessons: >= 3)
```

## Relationship to Existing Protocols

| Protocol                                     | Trigger               | Scope                           |
| -------------------------------------------- | --------------------- | ------------------------------- |
| `reflection.md`                              | Reactive: per-failure | 1 rule from single failure      |
| `reflector.md` (this)                        | Proactive: batch      | N rules from cross-run patterns |
| Both feed → `pending.md` → `/curate-lessons` |

## Rules

- NEVER auto-promote rules to context files — only append to `pending.md`
- Semantic dedup is MANDATORY — grep-only dedup is BANNED for this protocol
- One rule per finding — do not merge multiple findings into one rule
- If `reflector-report.json` has 0 findings, do NOT fabricate findings
- Append only — NEVER rewrite `.ai-lessons/pending.md` (Delta Update Protocol)
