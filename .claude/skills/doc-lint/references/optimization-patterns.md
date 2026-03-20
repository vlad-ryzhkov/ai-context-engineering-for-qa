# Document Optimization Patterns

Catalog of field-tested patterns for keeping documents concise, consistent, duplicate-free, and readable by both humans and LLMs. Extracted from real audit sessions on skill and pipeline documentation.

---

## Category A: Eliminate Structural Duplication

### A1. Scattered Related Instructions

**Detect:** Same topic described across N separate sections.
**Example:** Output rules split across "Output Results", "Output Contract", "Output Discipline", and "Completion" sections.
**Fix:** Consolidate all instructions about a single topic into ONE section.

### A2. Overlapping Verification Sections

**Detect:** Multiple checklists/gates verifying the same properties under different names.
**Example:** "Self-Review" (13 items), "Definition of Done" (8 items), and "Defect Consolidation" all checking "did I find all defects and write them correctly?"
**Fix:** Merge into a single quality gate. Different names for the same check = duplication.

### A3. Algorithm Restated in Checklist

**Detect:** Quality gate items that verbatim repeat what the algorithm section already demands.
**Fix:** Quality gates should check meta-properties ("all passes completed", "sorted correctly", "score calculated after finalization") — not re-list algorithmic steps.

### A4. Same Rule in Two Files

**Detect:** Identical or near-identical rule in both a primary and secondary file without cross-reference.
**Example:** Anti-Hallucination Rule in both SKILL.md and analysis-procedures.md with slightly different wording.
**Fix:** State the rule once in the canonical location. In the other file, reference it.

### A5. Two Representations of Same Data

**Detect:** Each item appears once in verbose per-item sections AND again in a summary table.
**Fix:** Choose one representation. Tables with footnotes for complex cases beat verbose sections + summary table.

---

## Category B: Detect Contradictions

### B1. Exhaustive Enumeration Mismatch

**Detect:** A definition says "N kinds only" but N+1 kinds exist elsewhere in the document set.
**Example:** "Blocker has two kinds only" but a third kind (Secret Exposure) introduced in a reference file.
**Fix:** Audit all references when a definition uses limiting language ("only", "exactly", "three kinds").

### B2. Aspirational Language Posing as Factual

**Detect:** Status labels that don't match implementation reality.
**Example:** "Active: Gardener embedded in every skill" when 0/3 skills have a Gardener section.
**Fix:** Labels must match implementation status. "Planned" ≠ "Active". Verify claims against codebase.

### B3. Context-Dependent Rules in Flat Lists

**Detect:** Same identifier in both BANNED and REQUIRED lists — banned in one context, required in another.
**Fix:** Context-dependent rules need explicit scoping, not placement in both lists.

### B4. Self-Contradicting SLOs

**Detect:** Measurement method implied by an SLO contradicts another stated policy.
**Example:** "Zero-shot compilation rate >= 90%" implies repeated runs, but the same doc rejects N-run determinism testing.
**Fix:** Clarify measurement methodology. "Measured across different fixtures (10 inputs, 9 compile), not repeated runs of same input."

### B5. Reading Order vs. Writing Order Ambiguity

**Detect:** A section is first in reading order but must be composed last.
**Fix:** Add explicit parenthetical: "sections in reading order (Executive Summary is composed last — see Verification Gate)."

### B6. Unspecified Continuation Policy

**Detect:** Multi-step process with no guidance on what to do when an early step finds a critical issue.
**Fix:** Make continuation/halting policy explicit: "Always complete all 4 passes regardless of findings."

---

## Category C: Conciseness Patterns

### C1. Single-Row Tables

**Detect:** A table with only one data row.
**Fix:** Express as inline text. Tables justify their overhead only with multiple rows.

### C2. Deferred Scope Inflation

**Detect:** Multi-paragraph descriptions of items explicitly marked "Deferred" or "Planned".
**Fix:** Deferred items get one line each: name + one-sentence description. Attach scaling triggers: "invest when >15 skills."

### C3. Content That Stales Instantly

**Detect:** Point-in-time snapshots requiring constant manual updates (e.g., "Production Skills" list, activity summaries).
**Fix:** Remove or replace with a query: "Run `ls skills/` for current list."

### C4. Usage Examples in Reference Docs

**Detect:** Multi-line usage examples in a document whose role is reference, not how-to.
**Fix:** Replace with a table row. Usage examples belong in tutorials/how-to guides.

### C5. Verbose Rationale Blocks

**Detect:** "Principle + Rationale + Current State" format repeated for each item, tripling the line count.
**Fix:** Collapse into a table: | Principle | Rationale | or inline rationale as a parenthetical.

### C6. Fragmented File Info

**Detect:** Same set of files described in 5+ separate small tables (hooks, scripts, tools, workflows, config).
**Fix:** Consolidate into a single inventory table or diagram.

### C7. Dual-Purpose Artifacts

**Detect:** Two separate sections serving overlapping purposes (e.g., a flow diagram AND a file inventory).
**Fix:** Design one artifact that serves both purposes. A "Pipeline Map" text diagram can simultaneously show flow + file paths.

---

## Category D: Terminology Consistency

### D1. Undefined Notation

**Detect:** Notation used before being defined.
**Example:** "Defect 9" used throughout, but `N = priority number` mapping never stated.
**Fix:** Add a one-line definition at first use: `"Defect N" means a defect at Priority N.`

### D2. Synonym Drift

**Detect:** Different words for the same concept used across sections.
**Example:** "Finding" and "Defect" used interchangeably.
**Fix:** Pick one term, use it everywhere. Add to a terminology table if needed.

### D3. Inconsistent Labeling

**Detect:** Some references use labeled form ("Defect 10 (Blocker)"), others use bare form ("Defect 10").
**Fix:** First mention in each section includes full label. Subsequent mentions use short form.

### D4. Internal Jargon in Running Text

**Detect:** Opaque codes (T0/T1/T2, AC1-AC7) scattered throughout prose.
**Fix:** Use human-readable labels in running text. Keep codes only in reference tables where they're defined.

---

## Category E: LLM-Friendly Document Design

### E1. Verbose Intermediate Output Instructions

**Detect:** Procedures that force writing out every check result, including passes.
**Example:** "Silence is not allowed" — forces narrating every passing check.
**Fix:** "Output only failures/deltas; summarize passes as counts."

### E2. Forced Physical Enumeration

**Detect:** Instructions to "physically write out" or "list every item" when only mismatches matter.
**Fix:** "Mentally construct" + output only the delta.

### E3. Missing Output Length Caps

**Detect:** Intermediate analysis sections with no length guidance.
**Fix:** Add explicit caps: "This section must not exceed 40% of total artifact length."

### E4. Missing Cognitive Anchors

**Detect:** Multi-pass analysis without re-read instructions before consolidation.
**Fix:** Add explicit re-read step: "Re-read Classification rules before assigning priorities."

### E5. Judgment Calls Where Deterministic Rules Should Exist

**Detect:** Decision points with multiple options but no selection criteria.
**Example:** Three verdicts listed but no rules for choosing between them.
**Fix:** Replace with deterministic rules: `Blocked = any P10; Approved = P6-9 exist; Ready = only P4-5.`

### E6. Missing Show-Your-Work Instructions

**Detect:** Formula calculations without instruction to show substituted values.
**Fix:** Require showing the formula with values: `max(0, 100 - 20×0 - 10×1) = 90%`. Add cross-check: "sum of band counts must equal total defect count."

### E7. Token Budget Unawareness

**Detect:** No estimation of how much context a document + its references consume.
**Fix:** Measure SKILL.md + loaded references as `chars/4` to estimate token cost. Flag anomalies.

---

## Category F: Document Architecture

### F1. Hub-and-Spoke Structure

**Principle:** Primary doc (SKILL.md) is the hub — contains WHAT. Reference files contain HOW. Hub links to spokes.
**Check:** Every reference file is linked from the hub. Every hub link resolves.

### F2. Progressive Disclosure

**Principle:** Level 1 = YAML header (always loaded). Level 2 = SKILL.md body (always loaded). Level 3 = references (loaded on demand).
**Check:** Heavy procedural content in Level 2 should be extracted to Level 3.

### F3. Role Identity

**Principle:** Every document has a declared purpose. Every section must justify its presence against that purpose.
**Check:** Strategic content in operational docs → demote to link. Operational content in strategic docs → extract to reference.

### F4. Inline Before Delete

**Principle:** Before deleting a redundant section, check for unique data not captured elsewhere.
**Fix:** Inline unique data into the surviving section, then delete.

### F5. Stable Anchors Over Section Numbers

**Principle:** Prefer `#compliance-c` over `#12-check-id-reference` — survives renumbering.
**Check:** Grep for `§N` and numbered anchor references after any restructuring.

---

## Category G: Cross-File Integrity

### G1. Post-Restructuring Verification Protocol

After any restructuring:

1. Run linter — must pass
2. Grep all `filename.md#` links — verify no broken anchors
3. Grep all `§` references — verify no stale section numbers
4. Verify ToC matches actual sections

### G2. Definition-Usage Alignment

When editing a definition, cross-read every file that references it. "N kinds only" edits must update all consumers.

### G3. Path Prefix Consistency

All internal paths must use the same base prefix. `qa-antipatterns/` vs `.claude/qa-antipatterns/` in different files = inconsistency.

### G4. Reject False Positives

An audit must distinguish real problems from acceptable design tensions. Document rejection reasons when dismissing findings.

---

## Category H: Process Patterns

### H1. Audit Before Optimizing

Compare docs to codebase reality first. Delete claims about non-existent features before optimizing structure.

### H2. Plan Before Editing

Write a structured plan (context, numbered changes, safety justifications, verification steps) before touching files.

### H3. Set Explicit Line Budgets

Set numeric targets per file before starting. Track compression ratio after each pass.

### H4. Delta Updates Only

Use Edit, never Write, on governed files. Prevents accidental content destruction.

### H5. Phase-Gated Approval

Report and modification must be separate steps with user approval between them.
