# Skill Audit — Deterministic Checks 0–28

Bodies of each Check moved here from `SKILL.md` to satisfy R5 / Check 27a (longest section ≤40% of file). SKILL.md retains a summary index + severity table; agent reads this file when it needs the full algorithm for a given Check.

---

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

### Check 2: YAML Compliance

For each SKILL.md verify frontmatter against rules from `init-skill/references/yaml-reference.md`:

- `name` in kebab-case, matches folder name, no "claude"/"anthropic"
- `description` contains three parts: **What / When / When NOT**
- `description` < 1024 chars, no XML characters (`<`, `>`), single-line
- If `agent:` is present — referenced file exists

Severity: **ERROR** (required field missing), **WARNING** (description format violation, missing `allowed-tools`).

### Check 3: Verbosity Protocol / SILENT MODE

Grep: `## Verbosity Protocol`, `SILENT MODE`, `NO CHAT TABLES` in SKILL.md files.

- Severity: **CRITICAL** (if absent)
- Why: Agents without this protocol pollute chat, output intermediate tables and lists, waste tokens on chatter
- **Exception:** If the skill has `agent:` in frontmatter — check for Verbosity Protocol in the agent file (`agents/{name}.md`). No need to duplicate in SKILL.md — flag only if absent from both SKILL.md and the agent.
- Recommendation: Add Verbosity Protocol or Block A (SILENT MODE) from `init-skill/references/skill-template.md` to the agent file (not to SKILL.md).
- Tier 1 Baseline status: WARNING during V2 migration; CRITICAL after V2 completion.

### Check 4: Side-Effect Skill Safety

Grep for `disable-model-invocation: true` in skills that have side effects.

**Side-effect skills** (MUST have `disable-model-invocation: true`):
- Skills that push code, create PRs, or call `gh pr create` (`/pr`)
- Skills that generate persistent files outside `audit/` (`/init-project`, `/init-skill`)
- Skills that deploy, release, or modify shared infrastructure

Algorithm:
1. Grep each SKILL.md `allowed-tools` for `Bash(git*` or `Bash(gh*`
2. If found AND `disable-model-invocation: true` absent → **ERROR**
3. Grep for `Write` in `allowed-tools` AND skill creates files outside `audit/` → check `disable-model-invocation`

Severity: **ERROR** (side-effect skill without `disable-model-invocation: true`)

### Check 5: STOP/WARN/INFORM Checkpoints

Grep each SKILL.md for structured phase checkpoints.

Pattern to detect: `STOP if:` or `STOP  if:` block at skill entry point.

- Severity: **WARNING** if absent from parametrized skills (`/api-tests`, `/api-isolated-tests`, `/spec-audit`, `/repo-scout`)
- Why: Without explicit STOP conditions, AI may proceed silently past missing required inputs
- Recommendation: Add Phase Checkpoints block (STOP/WARN/INFORM) per `init-skill/references/yaml-reference.md`

### Check 6: Self-Review Protocol (Bloated)

Grep: `Self-Review`, `self_review`, `_self_review.md`, report templates with `Scorecard`.

- Severity: **WARNING** (only if self-review template >50 lines or lacks Scorecard)
- Why: bloated templates waste tokens; compact Scorecards are a useful tracking tool
- Recommendation: optimize template to ≤50 lines with mandatory Scorecard
- **Exceptions:**
  - `*_self_review.md` files with Scorecard — valuable progress tracking artifacts. Do not flag

### Check 7: "DO NOT FIX" Instruction

Grep: `НЕ ИСПРАВЛЯТЬ`, `не исправляй`, `только анализ` — in the context of review/check sections.

- Severity: **CRITICAL**
- Why: AI documents problems instead of fixing them
- Recommendation: replace with "FIX the code/audit, recompile"

### Check 8: Tech Stack Duplication

1. Read CLAUDE.md → find Tech Stack
2. Grep each SKILL.md for stack keywords (Ktor, Jackson, Kotest, etc.)
3. If SKILL.md contains a full stack table (≥4 rows with `|`) → duplication

- Severity: **WARNING**
- Recommendation: replace table with `Stack LOCKED in CLAUDE.md → Tech Stack` + additions

### Check 9: Code Examples >50 Lines

Find code blocks (```kotlin, ```python, etc.) in SKILL.md. Count lines in each.

- Severity: **WARNING** (if block >50 lines)
- Recommendation: extract to `references/examples.md`, keep 3–4 spec lines + link

### Check 10: Decorative Code Blocks

Find ``` blocks that do NOT contain code:
- No language identifier
- Content = text with emoji/bullet points/markdown formatting

- Severity: **INFO**
- Recommendation: replace with plain lists/bold text

### Check 11: Anti-Patterns Verbosity

Find BANNED/Anti-Patterns sections. Count lines and paired Bad/Good blocks (❌/✅ with code).

- Severity: **WARNING** (if pairs >3 and lines >30)
- Recommendation: replace with one-liners, details → `qa-antipatterns/*.md` or skill-specific references/

### Check 12: Cross-Reference Staleness

1. Collect references from qa_agent.md to skill sections/patterns
2. Verify that referenced sections exist in current SKILL.md files
3. Check Skill Completion Protocol for references to deleted patterns

- Severity: **ERROR**
- Recommendation: update qa_agent.md

### Check 13: Rarely-Used Sections + Progressive Disclosure

Find sections with:
- "prompts for customization/generation/adaptation"
- Meta-instructions for the user (not for AI during execution)
- Content used once per project but loaded on every invocation

- Severity: **INFO**
- Recommendation: extract to `references/`

**Progressive Disclosure sub-check:** If SKILL.md > 400 lines, verify presence of `scripts/` or `references/` subdirectory in the skill folder.

- Severity: **WARNING** (large file with no off-load structure)
- Recommendation: "SKILL.md exceeds 400 lines but uses no Progressive Disclosure. Move deterministic logic to `scripts/`, bulky instructions to `references/`."

### Check 14: Artifact Timestamping

For skills that generate file artifacts (e.g., `/spec-audit`, `/api-isolated-tests`, `/api-tests`), verify:
1. Output Template specifies **timestamped filenames** in format `{filename}_{timestamp}` (e.g., `test-scenarios_{timestamp}.md`, `spec-audit_{timestamp}.md`)
2. Completion Contract mentions that **each invocation creates a new file** (see spec-audit/SKILL.md for reference)
3. If skill outputs multiple files, **each gets a unique timestamp** or **shared timestamp prefix**

**Why:** Prevents accidental overwrites and maintains audit history per invocation.

- Severity: **CRITICAL** (if artifact-generating skill lacks timestamping)
- Skills to check: `/spec-audit`, `/api-isolated-tests`, `/api-tests`, `/repo-scout`
- Recommendation: Add timestamp format `YYYYMMDD_HHMMSS` to Output Template and Completion Contract

### Check 15: Rigid Prompting

Grep each SKILL.md for uppercase directives: `\bALWAYS\b`, `\bNEVER\b`, `\bMUST\b` (case-sensitive, uppercase only).

Count total occurrences per file.

| Count | Severity |
|-------|----------|
| ≤ 5 | OK |
| 6–10 | WARNING |
| > 10 | WARNING (elevated) |

- Why: LLMs perform better when they understand the *reason* behind a rule. Excessive uppercase imperatives add noise without improving compliance.
- Recommendation: "Excessive rigid constraints detected ({N} occurrences of ALWAYS/NEVER/MUST). Replace with rationale-driven phrasing — explain *why* the rule exists."

### Check 16: Paired Skill Drift (Legacy — still CRITICAL)

`api-tests/SKILL.md` and `api-tests-java/SKILL.md` are paired skills — intentionally similar. Divergence is allowed only where the language differs (Kotlin vs Java syntax, types, tooling). All behavioral rules (numbered labels `2a`–`2l`, Quality Gates, Workflow, Completion Contract) must be synchronized.

Algorithm:
1. Grep both files for numbered rule labels (`2a.`, `2b.`, ..., `2l.`, `3.`, `4.`, etc.) — build a label list from each file
2. Labels present in one file but absent in the other → **WARNING** "Rule `{label}` exists in `{fileA}` but not in `{fileB}`"
3. LLM step: for labels present in both — read both rule texts, compare intent. If meaning has diverged (not a language adaptation, but a different rule) → **WARNING** "Rule `{label}` has diverged meaning between paired skills"

- Severity: **WARNING**
- Scope: always run when auditing `api-tests` or `api-tests-java`; skip for all other skills

### Check 17: Intra-doc Redundancy

For SKILL.md files > 200 lines — LLM step:
- Read sections: Protocol, Quality Gates, Post-Check, Completion Contract
- Find any rule or requirement stated in 2+ sections of the same file
- Severity: **INFO** (repetition across sections can be intentional — flag for manual review)
- Recommendation: "Rule about `{topic}` appears in sections `{A}` and `{B}`. Consider keeping only in `{primary section}` and removing from `{secondary}`."

### Check 18: Self-Review Checklist (Tier 1 Baseline — WARNING during V2 migration)

Grep: `- [ ]` checklist or equivalent verification step (Quality Gate section) before SKILL COMPLETE block.

- Severity: **WARNING** (if absent during migration; CRITICAL after V2 completion)
- Why: Structured self-review prevents accidental oversights and improves output quality
- Recommendation: Add Block B or Block B-Lite (Quality Gate) from `init-skill/references/skill-template.md`

### Check 19: Gardener Integration (Tier 1 Baseline — WARNING during V2 migration)

Grep: explicit reference to `.claude/protocols/gardener.md` or Gardener call in Quality Gate section.

- Severity: **WARNING** (if absent during migration; CRITICAL after V2 completion)
- Why: Gardener Protocol closes the feedback loop on rule drift and process improvements
- Recommendation: Add Block B (Quality Gate + Gardener) from `init-skill/references/skill-template.md`

### Check 20: Anti-Patterns Section (Tier 2 Recommended — SUGGESTION)

Grep: "Anti-patterns", "BANNED", "Common Mistakes", or ❌/✅ pairs.

- Severity: **SUGGESTION** (if missing from code-generating or analysis skills)
- Applicable only if skill generates code or complex text
- Recommendation: Add anti-patterns section or reference `qa-antipatterns/` folder with skill-specific examples

### Check 21: Loop Guard / Escalation (Tier 3 Specialized — SUGGESTION)

Grep: "Loop Guard", "Escalation", "3-Strike", or `> **Loop Guard**`.

- Severity: **SUGGESTION** (if missing from testing/compilation/iterative skills)
- Applicable only if skill involves code testing, compilation, or iterative fixing (api-tests, api-mocks, api-isolated-tests)
- Recommendation: Add Block C (Loop Guard) from `init-skill/references/skill-template.md`

### Check 22: Cross-Skill Improvement Section (Tier 2)

Pipeline consumer skills (skills that consume structured artifacts produced by another skill,
e.g., `/api-tests` consuming `test-scenarios.md` from `/api-test-cases`) must have a
`## 💡 {Source} Improvements (Gardener)` section embedded inside the Completion Contract.

- Grep for `Scenario Source Improvements\|Blueprint Source Improvements\|Generator Improvements` in SKILL.md
- If skill reads `audit/test-scenarios.md` as primary input AND no such section exists → **WARNING** (Tier 2)
- If skill is a pure auditor of another skill's output AND no such section exists → **WARNING** (Tier 2)
- Omit this check for generative skills that produce artifacts without consuming upstream skill output

### Check 23: PromptPrism Semantic Audit

Scope: prompt artifacts only — `SKILL.md`, `.claude/agents/*.md`, `.claude/qa_agent.md`, `.claude/CLAUDE.md`, model-loaded `references/*.md` referenced from a SKILL.md.

Run three passes from `references/promptprism.md`:

1. **Pass A — Slot Map.** Verify Layer 1 Functional Components (Task Specification, Context, Examples, Constraints, Output Format). Missing slot → `PP-F1..F5` (WARNING). ≥2 missing → `PP-F0` (CRITICAL). Component leakage between blocks → `PP-F6` (WARNING). Missing negative boundaries when constraints required → `PP-F7` (CRITICAL).
2. **Pass B — Sensitivity.** Negation stacking, unitless thresholds, implicit else, conflicting constraints, sequential misalignment → `PP-S1..S6`.
3. **Pass C — Refinement.** Advisory only → `PP-R*` (INFO).

Linguistic checks throughout: Syntax mood / delimiter collision / visual-vs-structural formatting / aspirational fuzziness → `PP-L1..L7`. Morphology drift → `PP-M*`.

Findings tagged `[PromptPrism]` in report. Severity matrix and detection checklist in `references/promptprism.md`.

### Check 24: Description & Voice Hardening

Reference: `references/external-rules.md` § R1, R2.

- **24a — Description triggers.** Frontmatter `description` requires explicit `Use when …` AND `Do NOT use for …` clauses. Missing `Use when` → CRITICAL; missing `Do NOT use for` → WARNING.
- **24b — Imperative voice.** Grep Algorithm sections for hedging directives: `should\b`, `you should`, `would be`, `is recommended`, `your judgment`, `as appropriate`, `based on context`. Each match in load-bearing instruction → WARNING.
- **24c — Out-of-scope refusal.** Analysis/generation skills require literal refusal template ("review X only — out of scope"). Absence → WARNING.

### Check 25: Security & Network Hardening

Reference: `references/external-rules.md` § R4, R6, R7.

- **25a — PII placeholders.** Scan code blocks and example tables for plausible emails, e164 phones, 13–19-digit numbers, IBAN/SSN. Real-looking values → CRITICAL. Use `<email>`, `<phone>`, `XXXX…` placeholders.
- **25b — Network Access section.** If SKILL.md or its scripts call `curl`, `wget`, `fetch`, MCP HTTP tools → require `## Network Access` section listing domains + justification. Absence → ERROR. `curl | bash` / `wget | sh` / `source <(curl …)` without pinned SHA + explicit consent → CRITICAL.
- **25c — Security override.** Grep for "ignore", "bypass", "override safety", "disable lint", "disable TLS", "skip cert verification" without narrow justified scope → CRITICAL. Symlink install into `.git/hooks/` or `.claude/settings.json` → CRITICAL (use copy + explicit update step).

### Check 26: Scope Discipline

Reference: `references/external-rules.md` § R8.

Detect multi-skill bundles disguised as one skill:

- Single SKILL.md performs analysis AND generation as branched modes → WARNING.
- `if user asks X do A; if Y do B` branching by user intent → WARNING (multi-skill bundle).
- Two distinct deterministic output types in one Output Format block → WARNING.

Recommendation: split into separate skills with sharper `Use when` triggers.

### Check 27: Section Size & Heading Hierarchy

Reference: `references/external-rules.md` § R5.

- **27a — Section size.** Compute longest H2 section length. If > 40% of total SKILL.md lines → WARNING. Move bulk to `references/`.
- **27b — Heading hierarchy.** Headings increment by one (`#` → `##` → `###`). Skipping a level (e.g. `##` → `####`) → INFO.

### Check 28: Prerequisites for MCP / External Tools

Reference: `references/external-rules.md` § R9.

If `allowed-tools` contains `mcp__*` or skill depends on a non-built-in CLI tool — require `## Prerequisites` section listing: server / tool name, install link, expected `.claude/settings.local.json` keys. Absence → ERROR.
