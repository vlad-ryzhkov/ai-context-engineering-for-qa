# PromptPrism Lens — Prompt & Skill Audit Reference

Source: Jeoung, Chen, Zhang, Wang, Ding, Cheong. *PromptPrism: A Linguistically-Inspired Taxonomy for Prompts.* arXiv:2505.12592v2.

Applied ONLY when target file is a prompt artifact:

- `.claude/skills/**/SKILL.md`
- `.claude/skills/**/references/*.md` (when referenced from SKILL.md as model-loaded context)
- `.claude/agents/*.md`
- `.claude/qa_agent.md`, `.claude/CLAUDE.md`
- `prompts/*.md`, `*.prompt.md`
- Any file with frontmatter `name:` + `description:` + `agent:` (skill signature)

Skip for human-only docs (README, CONTRIBUTING, ADRs, runbooks).

---

## Taxonomy (3 layers, audit each prompt against all)

### Layer 1 — Functional Components

Every prompt MUST have these slots filled (or explicit reason for omission):

| Component          | Definition                                  | Doc-lint check                                                                  |
| ------------------ | ------------------------------------------- | ------------------------------------------------------------------------------- |
| Task Specification | What the model must do (verb + object)      | First H1/purpose block names a single concrete action                           |
| Context            | Background, role, domain                    | `<purpose>` or "Before Starting" / role anchor present                          |
| Examples           | Input→output demonstrations (few-shot)      | At least 1 worked example for non-trivial output formats                        |
| Constraints        | Hard rules, banned actions, scope limits    | Explicit BANNED / MUST NOT / Do not use section                                 |
| Output Format      | Structure of model response                 | `Output Format` / `Artifact` block with template, not prose                     |

Missing slot → **WARNING** (PP-F1..F5).
Two or more missing → **CRITICAL** (PP-F0: skill underspecified).

**PP-F6 (WARNING) — Component Leakage.** Instructions inlined inside a Context or Output Format block (or vice versa). Each block must serve one functional purpose. Example violation: "Output Format" section that also re-states task instructions.

**PP-F7 (CRITICAL) — Missing Negative Boundaries.** When Constraints slot is required (skill has destructive ops, security scope, or strict output), it MUST contain explicit `MUST NOT` / `BANNED` / `Do not` lines, not only positive rules. Positive-only constraints leak into hallucinated freedom.

### Layer 2 — Linguistic Dimensions

| Dimension  | Audit lens                                                              | Failure mode                                                |
| ---------- | ----------------------------------------------------------------------- | ----------------------------------------------------------- |
| Syntax     | Imperative mood, parallel structure, list/table consistency             | Mixed mood (imperative + declarative + question) in one block |
| Semantics  | Unambiguous nouns/verbs, single referent per term                       | Pronouns ("it", "this") with unclear antecedent             |
| Pragmatics | Speech act clarity (instruction vs. description vs. negotiation)        | "You should consider" instead of MUST / SHOULD / MAY        |

Findings:

- **PP-L1 (WARNING)** — Syntax: mixed mood within instruction block.
- **PP-L2 (WARNING)** — Semantics: synonym drift (term X used as Y elsewhere).
- **PP-L3 (CRITICAL)** — Pragmatics: hedging on hard constraint ("try to never commit secrets").
- **PP-L4 (INFO)** — Pronoun without antecedent within 2 lines.
- **PP-L5 (CRITICAL)** — Syntax: delimiter collision / nesting error. Mixed structural delimiters for same logical group (`<tag>` vs `[tag]` vs `**tag**`), unclosed XML/code fences, table inside list inside blockquote without consistent nesting.
- **PP-L6 (INFO)** — Visual-vs-structural formatting: bold/italic used to convey logic (decision branches, severity, sequencing) instead of XML tags / numbered lists / tables. LLM parses structure, not typography.
- **PP-L7 (WARNING)** — Aspirational / fuzzy semantics: non-measurable directive on a load-bearing step ("analyze deeply", "be creative", "write well", "think carefully"). Replace with concrete algorithmic step.

### Layer 3 — Morphological Features

| Feature     | Audit lens                                       | Anti-pattern                                                  |
| ----------- | ------------------------------------------------ | ------------------------------------------------------------- |
| Inflection  | Tense/number consistency                         | "Scan files. Scanned all? Scanning continues." in one section |
| Derivation  | Term family discipline (test/tester/testing)     | Coining new derivative ("auditation") not in glossary         |
| Composition | Compound terms used consistently                 | "test-case" vs "testcase" vs "test case" mixed in one file    |

Findings: **PP-M1..M3 (INFO)** unless ambiguity changes meaning → upgrade to WARNING.

---

## Three Application Passes (run in order)

### Pass A — Prompt Analysis (structural)

For each prompt file:

1. Slot-fill table of Layer 1 components — mark `present / partial / missing`.
2. Note explicit role anchor (`agent:` field, "You are…").
3. Verify Output Format is a TEMPLATE (code block / table), not prose description.

Emit `### PromptPrism Slot Map` section in report:

```markdown
| Component | Status | Location |
| --------- | ------ | -------- |
| Task Specification | present | L9 (`<purpose>`) |
| Context            | partial | L17 ("Before Starting") |
| Examples           | missing | — |
| Constraints        | present | L204 ("Anti-Patterns BANNED") |
| Output Format      | present | L97 ("Output Format") |
```

### Pass B — Sensitivity Analysis (robustness)

Flag fragility points where small rewrites likely change model behavior:

- Negation stacking ("do not fail to avoid not skipping") → **CRITICAL PP-S1**.
- Numeric thresholds without units (`>500` — chars? lines? tokens?) → **WARNING PP-S2**.
- Conditional with implicit else ("If X, do Y." — no else branch) → **WARNING PP-S3**.
- Order-dependent steps not numbered → **WARNING PP-S4**.
- Conflicting constraints (Constraint A bans what Constraint B requires) → **CRITICAL PP-S5**.
- Sequential misalignment: physical document order contradicts logical execution flow (Output Format defined before Context that drives it; Quality Gate before Algorithm) → **WARNING PP-S6**.

### Pass C — Optimization / Refinement (concision)

Suggest, do not auto-rewrite:

- Replace prose constraint with table when ≥3 rules.
- Promote inline example into its own labeled block when reused.
- Demote chatty preamble (>3 lines before first instruction) to one line.
- Collapse synonym variants to canonical term per Layer 2 Semantics.

Emit `### PromptPrism Refinement Suggestions` table — each row optional, advisory only.

---

## Severity Mapping (merge into existing model)

| Code         | Default severity | Notes                                                |
| ------------ | ---------------- | ---------------------------------------------------- |
| PP-F0        | CRITICAL         | ≥2 functional slots missing                          |
| PP-F1..F5    | WARNING          | Single slot missing                                  |
| PP-F6        | WARNING          | Component leakage between blocks                     |
| PP-F7        | CRITICAL         | Missing negative boundaries (MUST NOT) when required |
| PP-L1, PP-L2 | WARNING          | Syntax / semantics drift                             |
| PP-L3        | CRITICAL         | Pragmatic failure on a hard rule                     |
| PP-L4        | INFO             | Local ambiguity                                      |
| PP-L5        | CRITICAL         | Delimiter collision / nesting error                  |
| PP-L6        | INFO             | Visual formatting carrying logic                     |
| PP-L7        | WARNING          | Aspirational / fuzzy directive on load-bearing step  |
| PP-M1..M3    | INFO             | Morphology — upgrade if it shifts meaning            |
| PP-S1, PP-S5 | CRITICAL         | Robustness landmines                                 |
| PP-S2..S4    | WARNING          | Fragile spec                                         |
| PP-S6        | WARNING          | Sequential misalignment (doc order ≠ execution flow) |
| PP-R*        | INFO             | Refinement suggestions, advisory                     |

Apply existing Health Score weights (CRITICAL -15, WARNING -5, INFO -0.5).

---

## Quick Detection Checklist (copy into Phase 5.5)

```text
For each prompt-class file:
  [ ] Layer 1 slots: 5/5 present? Missing → PP-F*
  [ ] First imperative within first 20 lines? else PP-S preamble bloat
  [ ] Output Format is a template block? else PP-F5
  [ ] Hedge words ("try", "consider", "maybe") on hard constraints? PP-L3
  [ ] Numeric thresholds carry units? else PP-S2
  [ ] Term consistency: scan for split spelling (camel/snake/kebab on same noun) → PP-M3
  [ ] Negation depth ≤1 per sentence? else PP-S1
  [ ] Conflicting constraints across sections? PP-S5
  [ ] Each block (Context / Instruction / Output) single-purpose? else PP-F6
  [ ] Negative boundaries explicit (MUST NOT / BANNED)? else PP-F7
  [ ] Delimiters consistent, no unclosed tags / fences? else PP-L5
  [ ] No bold/italic carrying decision logic? else PP-L6
  [ ] No fuzzy directives ("analyze deeply", "be creative") on load-bearing step? else PP-L7
  [ ] Document order matches execution flow? else PP-S6
```

---

## Out of Scope

- Model-specific prompt tuning (Claude vs GPT phrasing).
- Token counting (handled by size thresholds in `check-rules.md`).
- Markdown lint (handled by markdownlint config).

PromptPrism is structural — it does not score "prompt quality" as a single number. Use it to surface concrete defects and feed them into the existing Health Score.
