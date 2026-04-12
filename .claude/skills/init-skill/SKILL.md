---
name: init-skill
description: Generates new skills with interactive workflow, checkpoints, and iterative refinement. Use when you need to create a new skill, standardize a QA process, or automate routine checks. Use when creating a new skill or improving an existing one.
allowed-tools: "Read Write Edit Glob Grep Bash"
agent: sdet
context: fork
---

# /init-skill — New Skill Generator

<purpose>
Interactive creation of a new skill with step-by-step workflow, checkpoints, and a refinement cycle.
Focus: QA tasks (testing, analysis, automation).
</purpose>

## Before Starting

Read `.claude/qa_agent.md` and `.claude/agents/sdet.md`.

## When to Use

- Creating a new tool for a recurring QA task
- Standardizing a process within the team
- Automating routine checks

---

## Progressive Disclosure Principle

YAML header (always in the prompt) → SKILL.md body (on activation) → scripts/references (on demand).

Full diagram: `references/skill-template.md` → section "Progressive Disclosure".

## Writing Style

Use **imperative style** in skill instructions:

| Correct | Incorrect |
|---------|-----------|
| Generate test cases | You should generate test cases |
| Validate input data | It is recommended to validate input data |
| Read the specification | One needs to read the specification |

---

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the artifact (MD/HTML), not into chat.

**Chat output (constraints):**
- Brief Summary: max 5 lines (what was found, how many, result)
- Findings table: max 15 lines (top by severity)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress for each file. Checkpoint only on:
- Phase transition (Phase N → Phase N+1)
- Blocker detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, no "Now I will grep..."
- Read → analyze → report, no "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist), not a separate file.

---

## INTERACTIVE WORKFLOW

## Phase 0: Mode Selection

Ask the user:

```text
What do we do?
1. New skill — create from scratch
2. Improve existing — refine a skill that already exists
```

**If "New skill"** → proceed to Phase 1.

**If "Improve existing":**
1. Ask for the skill name: `which skill? (e.g. /api-tests)`
2. Read `.claude/skills/{name}/SKILL.md`
3. Run through `references/validation-checklist.md` — list issues found
4. ✅ CHECKPOINT: "Found N issues: [list]. Fix all? (yes / select)"
5. Jump directly to Phase 5 (edit SKILL.md) → Phase 6 → Phase 7

---

## Phase 1: Define Purpose

### Step 1.1: Ask for the purpose

```text
What should the new skill do?

Examples for QA:
- Generate test cases for [area]
- Analyze [what] for [what to look for]
- Create automated tests for [API/UI type]
- Validate [artifact] against [standard]
```

### Step 1.2: Determine the category

Categories: **Analysis** (report), **Generation** (code/document), **Validation** (pass/fail), **Transformation** (conversion).

Full table with examples: `references/skill-template.md` → section "Skill Categories".

### Step 1.3: Collect specific use case examples

Ask the user for **2-3 specific examples**:

```text
Before designing the skill, I need specific examples:

1. **Trigger phrases** — what will the user say to invoke the skill?
Example: "check the screenshot for L10N bugs", "generate tests for /api/v1/users"

2. **Use cases** — describe 2-3 real usage scenarios:
- What input data?
- What expected output?
- What context (project, stage, team)?

3. **Anti-examples** — when should the skill NOT be used?
```

**Why:** Specific examples define the skill scope more precisely than an abstract description. Trigger phrases help write an accurate YAML description.

### ✅ CHECKPOINT 1: Purpose Confirmation

```text
Understood the task as:
- Purpose: [what it does]
- Category: [Analysis/Generation/Validation/Transformation]
- Name: /[skill-name]

Use case examples:
1. [use case 1]
2. [use case 2]

Trigger phrases: "[phrase 1]", "[phrase 2]"

Is this correct? (yes / no, I'll clarify)
```

**⚠️ DO NOT CONTINUE without user confirmation!**

---

## Phase 2: Design Structure

### Step 2.1: Propose structure based on category

Proposals depend on the skill category (Analysis/Generation/Validation/Transformation).

Full list of questions for each category — in `references/interaction-guide.md` → section "Structural Proposals by Category"

### Step 2.2: Define file structure

```text
.claude/skills/{skill-name}/
├── SKILL.md              # Mandatory (case-sensitive!)
├── scripts/              # Executable — automation and utilities
│   └── [name].py/.sh
├── references/           # Loaded into context — references, checklists
│   └── [name].md/.json
└── assets/               # Used in output, NOT loaded — templates, icons
└── [name].md/.png
```

**Critical rules:**
- Directory: only kebab-case (`my-skill` ✅, `My_Skill` ❌)
- File: exactly `SKILL.md` (case-sensitive, not `skill.md`)
- **DO NOT create README.md inside the skill directory** — all documentation goes in SKILL.md or references/

### ✅ CHECKPOINT 2: Structure Confirmation

```text
Skill structure:
- Main file: SKILL.md
- Scripts: [yes/no] — [purpose]
- References: [yes/no] — [purpose]
- Assets: [yes/no] — [purpose]

Additional features:
- [list of selected options]

Continue? (yes / change)
```

**⚠️ DO NOT CONTINUE without user confirmation!**

---

## Phase 3: Create YAML Header

Read `references/yaml-reference.md` for the full reference on fields, constraints, and examples.

### Step 3.1: Generate name and description

**Key rules:**
- `name`: kebab-case, matches the directory name, no "claude"/"anthropic"
- `description`: formula `[What it does]. [When to use]. [When NOT to use]`, < 1024 characters, no XML tags
- `allowed-tools`: REQUIRED — declare tools the skill uses (e.g., `"Read Write Edit Glob Grep"`)
- `disable-model-invocation`: set to `true` if skill has side effects (pushes code, creates PRs, writes persistent files outside `audit/`)

**Use trigger phrases from Checkpoint 1** to formulate "When to use".

**Scaffold standard for generated skills:**

```yaml
---
name: [skill-name]
description: [What it does]. [When to use]. [When NOT to use]
allowed-tools: "Read Write Edit Glob Grep Bash(npx*)"
disable-model-invocation: false   # set true for side-effect skills
agent: sdet                       # omit if no dedicated agent
context: fork
---
```

### Step 3.2: Determine if skill needs STOP/WARN/INFORM checkpoints

If the skill takes parameters (framework, file path, entity name), add a Phase Checkpoints block at the entry:

```markdown
## Phase Checkpoints

\`\`\`text
STOP  if: [required param missing] | [required file not found]
WARN  if: [optional convention files unfilled] | [recommended prerequisite absent]
INFORM: [what was detected/resolved]
\`\`\`
```

### ✅ CHECKPOINT 3: YAML Frontmatter Confirmation

```text
YAML Frontmatter (will be visible in the system prompt):

---
name: [skill-name]
description: [your variant]
allowed-tools: [tools]
disable-model-invocation: [true|false]
---

Acceptable? (yes / suggest your variant)
```

**⚠️ DO NOT CONTINUE without user confirmation!**

---

## Phase 4: Prepare Resources (scripts, references, assets)

Create the resources selected in Checkpoint 2:
- **scripts/** — executable utilities (Python/Bash)
- **references/** — references loaded into context
- **assets/** — templates for output (NOT loaded into context)

**✅ CHECKPOINT 4:** Confirm the list of created files before proceeding to SKILL.md

---

## Phase 5: Write SKILL.md Body

### Step 5.1: Generate the full SKILL.md

Read and use the template from `references/skill-template.md` → section "Template".

**Style:** imperative (see "Writing Style" above).

**Persuasion principles:** Before writing the skill body, consult `references/persuasion-principles.md` to select the right combination of persuasion principles for the skill type. Match principle intensity to skill category (discipline-enforcing → strong Authority + Commitment; utility → clarity only).

When writing instructions, **reference the actual resources** prepared in Phase 4:
- `Read references/checklist.md` — not an abstract "use the checklist"
- `Run scripts/validate.sh` — not "validate"

### Step 5.2: Apply Tier 1 Baseline Injections

Before finalizing SKILL.md, ensure all **Tier 1 Baseline** features are present:

| Feature | Requirement | Code location |
|---------|-------------|---|
| **SILENT MODE** | Explicit rule suppressing intermediate chat output | After main heading or in Phase 1 |
| **Quality Gate + Gardener** | Self-review checklist + Gardener Protocol call | Before SKILL COMPLETE block |
| **Self-review checklist** | `- [ ]` items (3–7 checks) specific to the skill | Inside Quality Gate |
| **Verification Gate** | Proof command executed before completion claim | Before SKILL COMPLETE, after Quality Gate |
| **Input/Output format** | Clear specification of input and output | "Input" and "Output Format" sections |
| **Output template** | SKILL COMPLETE block with status code | At completion |

**Conditional injections (by skill type):**

| Skill Type | Inject | Location |
|-----------|--------|----------|
| File-reading / analysis | **Context discovery phase** | Phase 0 or Phase 1 |
| Code generation / audit | **Anti-patterns section** | After Algorithm or before Quality Gates |
| Testing / compilation | **Loop Guard** | Inside execution phase |
| Multi-file scan | **Status Reporting** | Inside heaviest processing phase |
| ALL skills | **Verification Gate** | Before SKILL COMPLETE, after Quality Gate |
| Pipeline consumer (skill consumes output of another skill) | **Cross-skill improvement section** (`## 💡 {Source} Improvements (Gardener)`) | Inside Completion Contract, after SKILL COMPLETE block |

See `references/skill-template.md` → section "Tier 1 Baseline Injection" for exact injectable blocks.

### ✅ CHECKPOINT 5: SKILL.md Review

Show the full SKILL.md and offer editing options (see `references/interaction-guide.md` → "Editing Options").

**⚠️ You MUST show the file and wait for the user's choice!**

---

## Phase 6: Iterative Refinement

The refinement cycle is described in `references/interaction-guide.md` → "Refinement Cycle"

---

## Phase 6.5: Pressure Test (Optional)

For discipline-enforcing skills: **required**. For code generation: **recommended**. For utility: **skip**.

Full protocol: `references/skill-testing.md`

Quick summary:
1. **RED** — Run a realistic scenario WITHOUT the skill. Document failures and agent rationalizations verbatim.
2. **GREEN** — Load the skill, re-run. Verify failures are now caught.
3. **REFACTOR** — Remove dead instructions, strengthen weak spots, add captured rationalizations to anti-patterns.

### ✅ CHECKPOINT 6.5: Pressure Test Results (if applicable)

```text
Pressure test:
- Scenario: [description]
- RED failures: [N issues observed without skill]
- GREEN fixes: [N/N issues now caught with skill]
- Rationalizations captured: [N added to anti-patterns]

Proceed to save? (yes / iterate)
```

---

## Phase 7: Save and Validate

### ✅ CHECKPOINT 6: Final Confirmation

```text
Ready to save:

.claude/skills/[skill-name]/
├── SKILL.md ✅
├── scripts/[name].* ✅ (if any)
├── references/[name].* ✅ (if any)
└── assets/[name].* ✅ (if any)

Save? (yes / return to editing)
```

**⚠️ DO NOT SAVE without user confirmation!**

### Step 7.1: Save files

Create the directory and all files.

**Tip:** Use `scripts/init_skill.sh` to generate the template structure:
```bash
bash .claude/skills/init-skill/scripts/init_skill.sh [skill-name]
```

### Step 7.2: Validation and completion

- Go through `references/validation-checklist.md`
- Run bash self-check:

```bash
skill_file=".claude/skills/{skill-name}/SKILL.md"
lines=$(wc -l < "$skill_file")
[ "$lines" -gt 500 ] && echo "❌ Too long: $lines lines (max 500)" && exit 1
grep -q "^name:" "$skill_file" || { echo "❌ Missing: name"; exit 1; }
grep -q "^description:" "$skill_file" || { echo "❌ Missing: description"; exit 1; }
echo "✅ Validation passed ($lines lines)"
```

- Show the result: path to skill, invocation command
- Suggest a refinement cycle after first use (see `references/interaction-guide.md`)

---

## Related Files

- Init script: `.claude/skills/init-skill/scripts/init_skill.sh`
- Template: `references/skill-template.md`
- Examples: `.claude/skills/*/SKILL.md`
