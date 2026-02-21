# doc-lint Check Rules

## 1. Size Thresholds

| File Type | Recommended | WARNING | CRITICAL | Rationale |
|-----------|:-----------:|:-------:|:--------:|-----------|
| SKILL.md | ≤500 | N/A | >500 | Rule from qa_agent.md |
| qa_agent.md, agents/*.md | ≤300 | >300 | >500 | System prompts — denser than regular docs |
| CLAUDE.md | ≤200 | >200 | >300 | Always in context = token usage |
| docs/*.md | ≤400 | >500 | >700 | Microsoft Docs: 200-800 ideal range |
| README.md | ≤300 | >500 | >700 | Entry point + workshop guide |
| YAML config (.yaml, .yml) | ≤200 | >300 | >500 | Config, not prose |
| Generic .md (fallback) | ≤400 | >500 | >700 | Fallback for other markdown |

### File Classification

Priority (top to bottom, first match):

1. Name `SKILL.md` → SKILL.md
2. Name `qa_agent.md` or path contains `agents/` → qa_agent.md/agents/*.md
3. Name `CLAUDE.md` → CLAUDE.md
4. Name `README.md` → README.md
5. Extension `.yaml` or `.yml` → YAML config
6. Path contains `docs/` → docs/*.md
7. Extension `.md` → Generic .md

---

## 2. Known Duplicate Signatures

Pre-registered patterns for fast search via Grep:

| ID | Pattern | Grep signature | Min match |
|----|---------|----------------|-----------|
| KP-1 | Tech Stack (LOCKED) | `Компонент.*Технология.*BANNED` | 1 line |
| KP-2 | Progressive Disclosure | `Уровень 1.*YAML` or `Уровень 1.*Level 1` | 1 line |
| KP-3 | Core Principles | `Trust No One` + `Production Ready` + `Safety` | 3 lines within 10 lines |
| KP-4 | Skill Size Limit | `500 строк` or `≤500` in skill context | 1 line |
| KP-5 | Safety Protocols | `FORBIDDEN` + `MANDATORY` + `OVERRIDE` within 10 lines | 3 lines |

### KP-match Rule

A file is considered to contain the pattern if ALL lines from the "Min match" column are found.
Duplicate = pattern found in ≥2 files.

---

## 3. SSOT Ownership Matrix

| Content Category | SSOT Owner | Rationale |
|------------------|------------|-----------|
| Tech Stack, Safety, Conventions | `CLAUDE.md` | Always in context, minimal duplication |
| Mindset, Anti-Patterns, Protocols | `qa_agent.md` | Agent identity |
| Skill authoring rules | `qa_agent.md` | Common to all skills |
| Specific skill algorithm | `SKILL.md` | Scoped context |
| Tutorials, guides | `docs/*.md` | Documentation layer |
| Project overview | `README.md` | Entry point |

### SSOT Rule

If content from category X is found outside the SSOT Owner — this is WARNING (near-duplicate) or CRITICAL (exact duplicate >5 lines). Recommendation: replace with a link `→ see {SSOT Owner}`.

---

## 4. Diataxis Type Detection

Markers for determining document type:

| Type | Markers | Examples |
|------|---------|---------|
| **Tutorial** | "step 1", "let's create", step-by-step instructions with increasing complexity | Workshop guides |
| **How-to** | "how to", "to X, do Y", goal-oriented recipes | Troubleshooting |
| **Reference** | parameter tables, API signatures, enum values, pure facts without narrative | API docs, config refs |
| **Explanation** | "why", "architecture", "principle", conceptual explanations | Architecture docs |

### Diataxis Rule

One file contains markers of ≥2 types → INFO "Mixed Diataxis types". Not critical, but separation is recommended.

---

## 5. Heuristic Match Thresholds

| Level | Criterion | Severity |
|-------|----------|----------|
| **Exact** | 100% match after whitespace normalization, ≥3 lines | CRITICAL (>5 lines), WARNING (3-5 lines) |
| **Near-duplicate** | >70% element match for tables/lists (same headers + >70% rows) | WARNING |
| **Conceptual** | Same key terms, different wording | INFO (AI judgment) |

### Normalization Before Comparison

1. Remove leading/trailing whitespace
2. Collapse multiple spaces into one
3. Remove markdown formatting (`**`, `*`, `` ` ``)
4. Convert to lowercase
5. Tables: compare by cell content, ignoring `|---|` formatting

---

## 6. Structure Rules

| Rule | Criterion | Severity |
|------|----------|----------|
| Skipped heading level | H1→H3 (skipping H2) or H2→H4 | CRITICAL |
| Heading depth | >H4 used | INFO |
| Section imbalance | One section >40% of the entire file | WARNING |
| Empty section | Header → next header with no content (only blank lines) | WARNING |
| No TOC | File >200 lines without table of contents | INFO |
| Wall-of-text | >20 consecutive lines without headers/lists/blank lines/code blocks | WARNING |
| Long lines | Line >200 characters | INFO |
