---
name: init-skill
description: Generates new skills with interactive workflow, checkpoints, and iterative refinement. Use when you need to create a new skill, standardize a QA process, or automate routine checks. Do not use for editing existing skills.
allowed-tools: "Read Write Edit Glob Grep Bash"
agent: agents/sdet.md
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

# INTERACTIVE WORKFLOW

## Phase 1: Define Purpose

### Step 1.1: Ask for the purpose

```
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

```
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

```
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

```
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

```
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
- `description`: formula `[What it does]. [When to use]`, < 1024 characters, no XML tags

**Use trigger phrases from Checkpoint 1** to formulate "When to use".

### ✅ CHECKPOINT 3: YAML Frontmatter Confirmation

```
YAML Frontmatter (will be visible in the system prompt):

---
name: [skill-name]
description: [your variant]
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

When writing instructions, **reference the actual resources** prepared in Phase 4:
- `Read references/checklist.md` — not an abstract "use the checklist"
- `Run scripts/validate.sh` — not "validate"

### ✅ CHECKPOINT 5: SKILL.md Review

Show the full SKILL.md and offer editing options (see `references/interaction-guide.md` → "Editing Options").

**⚠️ You MUST show the file and wait for the user's choice!**

---

## Phase 6: Iterative Refinement

The refinement cycle is described in `references/interaction-guide.md` → "Refinement Cycle"

---

## Phase 7: Save and Validate

### ✅ CHECKPOINT 6: Final Confirmation

```
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
- If SKILL.md > 500 lines — suggest splitting
- Show the result: path to skill, invocation command
- Suggest a refinement cycle after first use (see `references/interaction-guide.md`)

---

## Related Files

- Init script: `.claude/skills/init-skill/scripts/init_skill.sh`
- Template: `references/skill-template.md`
- Full guide: `docs/ai-files-handbook.md`
- Examples: `.claude/skills/*/SKILL.md`
