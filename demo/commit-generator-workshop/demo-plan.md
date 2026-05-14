# Workshop — `commit-generator` skill

## Why a skill is useful

- **Project-specific knowledge** — locks in `domain rules` and constraints that base models lack.
- **Strict contract** — replaces ambiguous prompts with deterministic, `structured` input and output.
- **Zero back-and-forth** — enables fully `autonomous` execution with no manual corrections.
- **Measurable quality** — provides binary pass/fail criteria for `automated regression testing`.
- **Token efficiency** — `reduces output volume` and token burn compared with naive chat prompting.

## When a skill is useful

- **Repetitive routine** — `automates high-frequency tasks` performed daily or weekly.
- **Strict I/O surface** — applies when data is `structured` (diffs, JSON, logs), not free-form chat.
- **Rule-based logic** — fits tasks driven by a `fixed algorithm`, not subjective judgment.
- **High ROI** — justified for tasks costing >15 min/day or where manual errors are expensive.

Example: `golang-tester` (since 2026-02-16) for Go unit-test generation:

- Input: source files.
- Output: unit tests.
- Skill assets: rules, steps, references, antipatterns.
- Might be running in CI

__

Another example: /grill-me skill for planning
https://github.com/mattpocock/skills/blob/main/skills/productivity/grill-me/SKILL.md

Live demo: build a `/commit-generator` Conventional Commits skill from scratch, run evals, ship.
Disclaimer: focus is on flows, prompts, and tools. NOT a difficult logic.
Note: Haiku will be used.

## Coverage

1. Live demo — creating a simple AI skill step by step.
2. Overview of the `ai-skills` development flow.

---

## Full guide

/ai-skills/docs/skill-creation-guide.md especially `best practices`

## Official Anthropic's skill-creator

### Think first

A skill earns its keep only when the task is repetitive, the input/output are strict, and the execution flow is simple.

Some questions:

1. "What should this skill enable Claude to do? Which inputs and outputs"
2. "When should this skill trigger?"
3. "Should we set up test cases?"

## How to start with chat

```text
/skill-creator
```

### The 4 `skill-creator` interview questions

`skill-creator` asks 4 questions before drafting `SKILL.md`. Scripted answers:

**Q1 — "What should this skill enable Claude to do?"**

> Analyze a `git diff --staged` payload and output a single Conventional Commits message — `type(scope): description`,
> max 72 chars, no preamble. The skill must handle empty diffs with a literal sentinel string.

**Q2 — "When should this skill trigger?"**

> /commit-generator
> Triggers when the user runs, or pastes a `diff --git` payload and asks for a commit message.
> Should NOT trigger for general code review or prose summarisation (e.g. not for PR descriptions).

**Q3 — "What's the expected output format?"**

> Plain text in `.md` format.

**Q4 — "Should we set up test cases?"**

> Yes. Eight fixtures are ready in `demo/commit-generator-workshop/skills/commit-generator/evals/files/`.
OR
> Take some examples from this PR / doc <path>

### Folder structure preparation

```text
/skills/
└── commit-generator/            # main skill folder
    ├── evals/
    │   ├── evals.json
    │   └── files/
    ├── references/              # optional
    └── benchmark-history.md
/scripts/
└── append_history.py            # will live in ai-skills repo
```

### TDD preparation (why we pre-built the evals)

Stop guessing with prompts. We write strict contracts. These diffs are our test suite.
If the evals fail, the skill doesn't ship. Zero exceptions.
It is faster and cheaper to author a skill when fixtures and expected outputs already exist.

| # | Fixture                          | What it tests                                          | Expected output                                                     |
|---|----------------------------------|--------------------------------------------------------|---------------------------------------------------------------------|
| 1 | `auth-token-validator.diff`      | New behaviour added in `auth/token.go`                 | `feat(auth): add length validation for JWT tokens`                  |
| 2 | `analyzer-skill-guardrails.diff` | Docs change (guardrails block added to a SKILL.md)     | `docs(skill): add strict guardrails to prompt`                      |
| 3 | `no-staged-changes.diff`         | Empty diff (negative test)                             | `abort: no changes detected`                                        |
| 4 | `billing-invoice-signature.diff` | Rename + signature tidy, no behaviour change           | `refactor(billing): rename CalcTotal and tidy discount signature`   |
| 5 | `payment-gateway-config.diff`    | YAML indent change only, no semantic change            | `style(payment-gateway): increase yaml indent under retries blocks` |
| 6 | `summarizer-skill-update.diff`   | Guardrails REMOVED from a SKILL.md (safety regression) | `docs(skill): remove guardrails block from prompt`                  |
| 7 | `stripe-webhook-handler.diff`    | A leaked Stripe live secret key in source code         | `abort: SECURITY LEAK DETECTED (API KEY)`                           |
| 8 | `chat-contract-fields.diff`      | Proto-field deletion (BREAKING change for consumers)   | `feat(chat)!: CM-XXXX: drop deprecated fields from chat contract`   |

Anti-leakage rule: filenames look like real production paths and do NOT telegraph the test category. Skill must detect each case from content alone.

### Fun fact

Anthropic has tightened policies on generated prompts. Treat prompt authoring as engineering, not as a one-shot generation step.

### Prompt structure for skill creation / skill draft

Minimal structure:

1. **Task**
2. **Input / Output**
3. **Rules / Steps**
4. **Evals / tests** — input examples with expected results
5. **benchmark-history.md**

### Baseline (with vs without skill)

`skill-creator` A/B tests every eval: with the skill vs. the raw model.
If the delta is near zero, the skill is dead weight. We don't merge code that doesn't beat the baseline,
and we don't burn company tokens for zero ROI.

### History

`skills/benchmark-history.md`

### Run skill-creator

/skill-creator use prompt demo/commit-generator-workshop/prompts/skill-creation-prompt.md

### Check wev-view

### Check skill-creator output

> demo/commit-generator-workshop/skills/commit-generator-workspace
---

## Conclusion: Treat AI skills like deterministic functions.

Build a skill only if your task hits these 4 triggers:

1. Frequency: You repeat it weekly.

2. Strict I/O: The payload is structured (diffs, JSON, logs), not an open-ended chat.

3. Rule-based: The execution flow relies on hard constraints, not subjective AI judgment.

4. High ROI: It burns >15 minutes of your day, or a junior dev would easily mess it up.

### Do NOT turn into a skill

- **One-off scripts** — write them as plain scripts and move on.
- **Unclear tasks** — define the contract first, or skip until the shape is clear.
- **Subjective evaluations** — anything without a pass/fail rubric belongs in a human review.
- **Linter-covered work** — if a deterministic tool already enforces it, do not duplicate.

### Coming soon

1. Evals (test inputs + expectations (e.g. evals.json)) will be mandatory for all new skills.
2. Evals will run on Haiku via promptfoo for every PR in `ai-skills`.
3. Skill codeowners
