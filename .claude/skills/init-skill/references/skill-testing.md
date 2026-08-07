# Skill Testing: RED/GREEN/REFACTOR for Skills

> Adapted from obra/superpowers TDD approach. Goal: verify a skill actually changes agent behavior before deploying it.

## Core Concept

A skill that hasn't been pressure-tested is a hypothesis, not a tool. The RED/GREEN/REFACTOR cycle for skills:

1. **RED** — Run the scenario WITHOUT the skill loaded. Document agent failures (wrong output, missing checks, format violations, rationalization).
2. **GREEN** — Write/update the skill addressing observed failures. Re-run the same scenario. Verify the failures are fixed.
3. **REFACTOR** — Tighten the skill: remove redundant instructions, strengthen weak spots, add anti-patterns from observed rationalizations.

## When to Pressure Test

| Skill Type | Testing Recommended? | Why |
|---|---|---|
| **Discipline-enforcing** (`/api-test-review`, `/skill-audit`) | **Required** | These skills exist to prevent mistakes — if they don't change behavior, they're dead weight |
| **Code generation** (`/api-tests`, `/api-mocks`) | **Recommended** | Verify generated code meets quality gates without the skill being bypassed |
| **Analytical** (`/spec-audit`, `/repo-scout`) | **Optional** | Output quality is visible; pressure testing helps but is less critical |
| **Utility** (`/fix-markdown`, `/pr`) | **Skip** | Mechanical tasks — correctness is immediately obvious |

## Pressure Test Protocol

### Step 1: Design a Scenario

Pick a realistic input that exercises the skill's core behavior:
- For review skills: a test file with 2-3 planted violations
- For generation skills: a specification with edge cases
- For audit skills: a setup with known issues

### Step 2: Run Without Skill (RED)

Execute the scenario in a clean context (no skill loaded). Record:
- **Failures:** What the agent got wrong
- **Rationalizations:** Exact quotes of agent excuses ("should be fine", "not critical")
- **Missing checks:** What the skill should have caught

### Step 3: Write/Update Skill (GREEN)

Address each observed failure:
- Failure → add explicit instruction or checklist item
- Rationalization → add to anti-patterns table with counter
- Missing check → add to Quality Gate

### Step 4: Re-Run With Skill (GREEN verification)

Same scenario, skill loaded. Verify:
- [ ] Previously observed failures are now caught
- [ ] Rationalizations are blocked by anti-pattern rules
- [ ] Output format matches specification

### Step 5: Tighten (REFACTOR)

- Remove instructions that didn't affect behavior
- Strengthen weak spots (vague → bright-line rule)
- Add rationalization counters from Step 2 verbatim

## Rationalization Capture Pattern

When the agent rationalizes during RED phase, capture it verbatim and create an explicit counter:

| Agent Rationalization (verbatim) | Counter Rule |
|---|---|
| "This is probably fine for now" | Run the proof command. "Probably" is not evidence. |
| "I already checked this earlier" | Prior runs are stale. Re-verify with current state. |
| "It's a simple change, unlikely to break" | Simple changes cause the sneakiest bugs. Verify. |
| "The user didn't ask for this check" | The skill requires it. User intent doesn't override skill protocol. |

Add captured rationalizations to the skill's anti-patterns table — they become the most effective guardrails because they counter real observed behavior.

## Integration with /init-skill

This protocol is available as **Phase 6.5 (Pressure Test)** in `/init-skill`.
- Required for discipline-enforcing skills
- Recommended for code generation skills
- Optional for analytical and utility skills
