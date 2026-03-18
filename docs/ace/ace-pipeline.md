# ACE Pipeline — Agentic Context Engineering

## What is ACE

**Agentic Context Engineering (ACE)** is an architecture where AI agents improve their own
context files based on task execution experience — without retraining or manual rule-writing.

### The Problem: Context Rot

Static instruction files (`CLAUDE.md`, `SKILL.md`) degrade over time:

- Rules become outdated as the project evolves
- Missing rules cause repeated mistakes
- Manual maintenance doesn't scale across 20+ skills

### The Solution: Self-Improving Context

ACE treats context files as a **living knowledge base**. After every skill run the system
captures observations, accumulates them, and periodically promotes verified patterns back
into the governing files — all through surgical delta edits, never full rewrites.

**Result:** context quality grows with every run instead of decaying.

---

## Three Roles

ACE defines three roles. In this project each role maps to a concrete implementation:

| Role          | Responsibility                                  | Our Implementation                                                                                           |
| ------------- | ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Generator** | Executes tasks, produces artifacts              | Any skill execution (`/api-tests`, `/api-test-review`, `/repo-scout`, etc.)                                  |
| **Reflector** | Analyzes execution, extracts lessons            | Gardener + Reflection + Reflector (two-layer: `scripts/lib/reflector.sh` + `.claude/protocols/reflector.md`) |
| **Curator**   | Deduplicates lessons, promotes to context files | `/curate-lessons` skill                                                                                      |

### How the roles interact

```
Generator (skill run)
    │
    ├──▶ Gardener (per-run observations → pending.md + gardener-log.jsonl)
    │
    ├──▶ Reflection (on failure → 1 rule → pending.md)
    │
    ├──▶ Reflector (batch, manual/CI)
    │      Layer 1: bash detection (gardener-log + events + pending.md → report)
    │      Layer 2: LLM formulation (semantic dedup → [REFLECTOR] rules → pending.md)
    │
    ▼ lessons accumulate across runs
    │
    ▼ when ≥ 3 entries
Curator (/curate-lessons)
    │
    ▼ delta edit to target file
Context files improved
```

---

## Glossary

| Term                         | Definition                                                                                                                                                           | File Path                                                    |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------ |
| **Gardener Protocol**        | Runs at the end of every skill. Analyzes the run for uncovered patterns and proposes new rules.                                                                      | `.claude/protocols/gardener.md`                              |
| **Reflection Protocol**      | Activates on failure (`SKILL PARTIAL` or `LOOP_GUARD_TRIGGERED`). Performs root-cause analysis and formulates exactly 1 rule.                                        | `.claude/protocols/reflection.md`                            |
| **pending.md**               | Accumulation buffer for proposed lessons. Append-only — never rewritten.                                                                                             | `.ai-lessons/pending.md`                                     |
| **graduated.md**             | Promotion log — records date, rule excerpt, and target file for every promoted lesson.                                                                               | `.ai-lessons/graduated.md`                                   |
| **/curate-lessons**          | Curator skill. Loads pending lessons, deduplicates against all context files (2-pass hybrid), shows report, promotes on approval.                                    | `.claude/skills/curate-lessons/SKILL.md`                     |
| **Delta Update Protocol**    | All context file modifications use `Edit` (surgical replacement), never `Write` (full overwrite). Enforced by `delta-guard.sh` hook.                                 | `CLAUDE.md` (Editing Conventions section)                    |
| **Context Rot**              | Progressive degradation of static instruction files — outdated rules, missing patterns, bloated content. The core problem ACE solves.                                | Concept (described in `context-evolution.md`)                |
| **Entropy Management**       | Background processes that detect and clean desynchronization between documentation and implementation.                                                               | Concept (described in `context-evolution.md`)                |
| **Reflector (two-layer)**    | Proactive pattern detection engine. Layer 1 (bash) groups observations by keyword, flags 3+ occurrences. Layer 2 (LLM) performs semantic dedup and formulates rules. | `scripts/lib/reflector.sh`, `.claude/protocols/reflector.md` |
| **gardener-log.jsonl**       | Machine-parseable append-only log of Gardener observations. Each line is a JSON object with skill, observation, proposed_rule. Fed to Reflector Layer 1.             | `.ai-lessons/gardener-log.jsonl`                             |
| **Telemetry (events.jsonl)** | Append-only JSONL log of skill execution events (skill, status, error_type, gardener_count). Fed to Reflector Layer 1 failure detector.                              | `tests/telemetry/events.jsonl`                               |

---

## Full Cycle Step-by-Step

```
┌─────────────────────────────────────────────────────────────────┐
│                        ACE FULL CYCLE                           │
└─────────────────────────────────────────────────────────────────┘

  1. SKILL EXECUTION (Generator)
  ┌──────────────────────────────┐
  │  User runs a skill           │
  │  e.g. /api-test-review       │
  │  Artifact is produced        │
  └──────────┬───────────────────┘
             │
  2. GARDENER ANALYSIS (Reflector — always runs)
  ┌──────────▼───────────────────┐
  │  Reads current SKILL.md      │
  │  Compares run vs. rules      │
  │  Finds uncovered patterns    │
  │                              │
  │  Output: table of proposals  │
  │  ┌──────────────────────┐    │
  │  │ 🌱 GARDENER ANALYSIS │    │
  │  │ Observation → Rule   │    │
  │  └──────────────────────┘    │
  └──────────┬───────────────────┘
             │
             ├── Rule is skill-specific? ──→ target: skills/{name}/SKILL.md
             ├── Rule is a QA pattern?   ──→ target: qa-antipatterns/{cat}.md
             └── Rule is cross-cutting?  ──→ target: .ai-lessons/pending.md ◄─── APPEND
                                                          │
  2a. REFLECTION (Reflector — on failure only)            │
  ┌──────────────────────────────┐                        │
  │  Triggers on SKILL PARTIAL   │                        │
  │  or LOOP_GUARD_TRIGGERED     │                        │
  │                              │                        │
  │  Root-cause analysis         │                        │
  │  Formulates exactly 1 rule   │                        │
  │  Dedup check via Grep        │                        │
  │  If new → append pending.md ─┼────────────────────────┘
  └──────────────────────────────┘

  2b. REFLECTOR (Proactive — batch pattern detection)
  ┌──────────────────────────────┐
  │  Layer 1: scripts/lib/       │
  │    reflector.sh (bash)       │
  │  Scans gardener-log.jsonl,   │
  │  events.jsonl, pending.md    │
  │  Groups by keyword signature │
  │  Flags 3+ occurrences        │
  │  Output: reflector-report.json│
  │                              │
  │  Layer 2: protocols/         │
  │    reflector.md (LLM)        │
  │  Reads report + evidence     │
  │  Semantic dedup vs context   │
  │  Formulates actionable rules │
  │  If new → append pending.md ─┼────────┐
  └──────────────────────────────┘        │
                                           │
  3. ACCUMULATION (passive — lessons collect over multiple runs)
  ┌──────────────────────────────┐
  │  .ai-lessons/pending.md      │
  │  grows with each run         │
  │  Threshold: ≥ 3 entries      │
  └──────────┬───────────────────┘
             │
  4. CURATION (/curate-lessons — Curator)
  ┌──────────▼───────────────────┐
  │  Phase 1: Load pending       │
  │  Phase 2: 2-Pass Dedup       │
  │    Pass 1: Grep narrowing    │
  │    Pass 2: Semantic compare  │
  │    Verdicts: DUPLICATE /     │
  │    OVERLAP / UNIQUE / META   │
  │                              │
  │  Phase 3: Report → STOP      │
  │    User approval required    │
  │                              │
  │  Phase 4: Delta Update       │
  │    Edit target files         │
  │    Remove from pending.md    │
  │    Log to graduated.md       │
  └──────────┬───────────────────┘
             │
  5. CONTEXT IMPROVED
  ┌──────────▼───────────────────┐
  │  CLAUDE.md                   │
  │  skills/{name}/SKILL.md      │
  │  qa-antipatterns/{cat}.md    │
  │  protocols/*.md              │
  │                              │
  │  Next skill run benefits     │
  │  from promoted rules         │
  └──────────────────────────────┘

  ┌─────────────────────────────────────────────────┐
  │  Cycle repeats: run → reflect → accumulate →    │
  │  curate → improved context → better next run    │
  └─────────────────────────────────────────────────┘
```

---

## Current Implementation Status

| Component                        | Status                                                               | File                                     |
| -------------------------------- | -------------------------------------------------------------------- | ---------------------------------------- |
| Gardener Protocol                | ✅ Active — runs at end of every skill                               | `.claude/protocols/gardener.md`          |
| Reflection Protocol              | ✅ Active — triggers on failure                                      | `.claude/protocols/reflection.md`        |
| Reflector (Layer 1: Detection)   | ✅ Implemented — bash pattern detection                              | `scripts/lib/reflector.sh`               |
| Reflector (Layer 2: Formulation) | ✅ Implemented — LLM semantic dedup + rule formulation               | `.claude/protocols/reflector.md`         |
| Telemetry Collection             | ✅ Active — events.jsonl + gardener-log.jsonl                        | `scripts/hooks/telemetry-hook.sh`        |
| pending.md accumulation buffer   | ✅ Active — append-only                                              | `.ai-lessons/pending.md`                 |
| graduated.md promotion log       | ✅ Active                                                            | `.ai-lessons/graduated.md`               |
| /curate-lessons skill            | ✅ Active — 5-phase pipeline with human approval gate                | `.claude/skills/curate-lessons/SKILL.md` |
| Delta Update Protocol            | ✅ Enforced — `delta-guard.sh` hook blocks `Write` on governed files | `CLAUDE.md`                              |

---

## Reflector Architecture (Two-Layer)

The Reflector is a proactive, pattern-detecting engine that bridges the gap between
per-run observations (Gardener/Reflection) and batch curation (`/curate-lessons`).

### Why Two Layers?

Bash is good at counting, grouping, and thresholding. LLMs are good at semantic
comparison and rule formulation. The Reflector uses each tool for what it does best.

### Layer 1: Detection (bash)

**File:** `scripts/lib/reflector.sh`

Reads three data sources and produces a detection report:

| Detector             | Data Source                      | What it Finds                                    |
| -------------------- | -------------------------------- | ------------------------------------------------ |
| `recurring_gardener` | `.ai-lessons/gardener-log.jsonl` | Same observation proposed 3+ times across skills |
| `recurring_failure`  | `tests/telemetry/events.jsonl`   | Same (skill, error_type) failure 3+ times        |
| `pending_pattern`    | `.ai-lessons/pending.md`         | Same rule proposed 3+ times (keyword grouping)   |

**Output:** `tests/telemetry/reflector-report.json` — never writes to `pending.md`.

### Layer 2: Formulation (LLM protocol)

**File:** `.claude/protocols/reflector.md`

Reads the detection report, enriches findings with full context from `gardener-log.jsonl`,
performs **semantic dedup** against all context files (CLAUDE.md, SKILL.md, antipatterns,
protocols, pending.md), and formulates actionable rules for genuinely new patterns.

**Output:** `[REFLECTOR]` entries appended to `.ai-lessons/pending.md`.

### Design Decision: No Auto-Promotion

The Reflector **never auto-promotes** rules to context files. All rules go through
`pending.md` → `/curate-lessons` → human approval. Auto-promotion is a future option
once the system has demonstrated reliable dedup accuracy over multiple curation cycles.

### Relationship to Existing Protocols

| Component                | Trigger                              | Tool | Output                  |
| ------------------------ | ------------------------------------ | ---- | ----------------------- |
| `reflection.md`          | Reactive: SKILL PARTIAL / LOOP_GUARD | LLM  | 1 rule → `pending.md`   |
| `reflector.sh` (Layer 1) | Manual / CI                          | Bash | Detection report (JSON) |
| `reflector.md` (Layer 2) | After Layer 1                        | LLM  | N rules → `pending.md`  |

---

## ACE Pipeline vs Anthropic Skill Creator

Anthropic's official [Skill Creator](https://github.com/anthropics/skills/blob/main/skills/skill-creator/SKILL.md)
is an eval-driven development tool for building individual skills. ACE is a post-deployment
evolution system. They solve different lifecycle phases and are complementary.

| Dimension                    | Skill Creator (Anthropic)                                 | ACE Pipeline                                         |
| ---------------------------- | --------------------------------------------------------- | ---------------------------------------------------- |
| **Goal**                     | Iterate one skill to high quality before deployment       | Keep all context files improving after deployment    |
| **Core loop**                | Draft → Eval → Human Review → Rewrite                     | Execute → Observe → Accumulate → Curate → Delta-edit |
| **Quality signal**           | Quantitative evals + blind A/B comparison                 | Production observations + batch pattern detection    |
| **Cross-skill learning**     | No — each skill isolated                                  | Yes — Reflector aggregates across all skills         |
| **Eval framework**           | Assertions, benchmarks, variance analysis, browser viewer | None — frequency-based detection + curation approval |
| **Description optimization** | Dedicated trigger-accuracy loop (train/test split)        | Not addressed                                        |
| **Update style**             | Full SKILL.md rewrite per iteration                       | Surgical delta edits (enforced by `delta-guard.sh`)  |
| **Lifecycle phase**          | Pre-deployment (development)                              | Post-deployment (production)                         |

**What ACE borrows from the Skill Creator philosophy:**

- Progressive disclosure (metadata → SKILL.md body → references/)
- Human gate before changes land (eval review ↔ Curator approval)
- Explain the "why" — rules carry rationale, not just imperatives

**What ACE adds:**

- Autonomous observation (Gardener runs without human prompt)
- Cross-skill pattern detection (Reflector two-layer engine)
- Failure-driven learning (Reflection Protocol on SKILL PARTIAL)
- Append-only telemetry (`gardener-log.jsonl`, `events.jsonl`)
- Never-rewrite discipline (delta edits preserve surrounding context)

---

## References

- [**Agentic Context Engineering: Evolving Contexts for Self-Improving Language Models**](https://arxiv.org/abs/2510.04618) — Zhang et al., 2025. The foundational paper defining the Generator/Reflector/Curator architecture and delta update mechanism. Deep technical details, benchmarks (+10.6% on agent tasks), formal algorithms.
- [**Effective context engineering for AI agents**](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents) — Anthropic, 2025. High-level guide to context engineering: why context is a finite resource, just-in-time retrieval, compaction, and multi-agent patterns.
- [**Context Engineering for Coding Agents**](https://martinfowler.com/articles/exploring-gen-ai/context-engineering-coding-agents.html) — Birgitta Böckeler, 2026. Practical patterns for coding agents: CLAUDE.md, Skills, MCP servers, iterative context configuration.
