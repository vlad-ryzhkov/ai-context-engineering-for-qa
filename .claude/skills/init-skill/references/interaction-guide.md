# Interactive Interaction Guide

## Phase 2: Structural Proposals by Category

### Analysis Skills

```text
I suggest adding:
- [ ] Severity levels (Critical/Major/Minor)?
- [ ] Export to JSON/Markdown?
- [ ] Checklist of checks in references/?
```

### Generation Skills

```text
I suggest clarifying:
- [ ] Output language (Kotlin/Python/TypeScript)?
- [ ] Need dry-run mode (preview without saving)?
- [ ] Templates in references/?
```

### Validation Skills

```text
I suggest adding:
- [ ] Output format (Pass/Fail, list of violations)?
- [ ] Auto-fix for simple cases?
- [ ] Rules in references/?
```

### Transformation Skills

```text
I suggest clarifying:
- [ ] Input format?
- [ ] Output format?
- [ ] Need input data validation?
```

## Phase 5: Editing Options

```text
What's next?
1. ✅ Looks great, save it
2. ✏️ Change description
3. ➕ Add a step to the algorithm
4. ➖ Remove a step from the algorithm
5. 📝 Change the output format
6. ✓ Change Quality Gates
```

## Refinement Cycle

```text
REPEAT until the user selects "Save":

  User selects an action →

  IF "Change description":
    Show current → Ask for new → Update → Show result

  IF "Add step":
    Ask: Which step? After which step?
    Add → Renumber → Show result

  IF "Remove step":
    Show list of steps with numbers
    Ask: Which one to remove?
    Remove → Renumber → Show result

  IF "Change output format":
    Show current → Ask for new → Update → Show result

  IF "Change Quality Gates":
    Show current → Ask for changes → Update → Show result

  → Return to CHECKPOINT 5
```

## Step 7.4: Refinement Cycle After First Use

```text
Recommended refinement cycle:
1. Invoke /[skill-name] on a real task
2. Pay attention: where does Claude "stall" or deviate from expectations?
3. Come back here with feedback — we'll update SKILL.md
4. Repeat until stable results

Common issues on first run:
- Instructions too abstract → add specific examples
- Important step missing → add to the algorithm
- Unnecessary steps → simplify the workflow
- Inaccurate description → Claude doesn't activate the skill on the right phrases
```
