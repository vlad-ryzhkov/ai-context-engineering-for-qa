# Workshop — `commit-generator` skill

<!-- start skill with prompt -->

Why and when skill useful:
- 
Example: `golang-tester` (since Feb 16 2026) for go unit test creation:
input - some skills
outputs - unit tests
skill: rules, steps, referencies, antipatterns

Another example: /grill-me
https://github.com/mattpocock/skills/blob/main/skills/productivity/grill-me/SKILL.md 

Live demo: build a Conventional Commits skill from scratch, run evals, ship.
Disclaimer: Focus on flows, prompts, tools

## Coverage

1. Live demo — creating a simple AI skill step by step.
2. Overview of the `ai-skills` development flow.

---

## Full guide

/ai-skills/docs/skill-creation-guide.md

## Official Anthropic's skill-creator

### Think first

A skill earns its keep only when the task is repetitive, the input/output are strict, and the execution flow is simple. 

Some questions:

1. **Question 1** — "What should this skill enable Claude to do?"
2. **Question 2** — "When should this skill trigger?"
4. **Question 3** — "Should we set up test cases?"

## How to start with chat

```
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
> Should NOT trigger for general code review or for prose summarisation, e.g. NOT PR-description

**Q3 — "What's the expected output format?"**

> The plain text in `.md` format
 
**Q4 — "Should we set up test cases?"**

> Yes. Six fixtures are ready in `demo/commit-generator-workshop/skills/commit-generator/evals/files/`. 
OR
> Take some examples from this PR / doc <path>

### Folder structure

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

It is faster and cheaper to author a skill when fixtures and expected outputs already exist. The 6 cases:

| # | Fixture                          | What it tests                                          | Expected output                                                     |
|---|----------------------------------|--------------------------------------------------------|---------------------------------------------------------------------|
| 1 | `auth-token-validator.diff`      | New behaviour added in `auth/token.go`                 | `feat(auth): add length validation for JWT tokens`                  |
| 2 | `analyzer-skill-guardrails.diff` | Docs change (guardrails block added to a SKILL.md)     | `docs(skill): add strict guardrails to prompt`                      |
| 3 | `no-staged-changes.diff`         | Empty diff (negative test)                             | `abort: no changes detected`                                        |
| 4 | `billing-invoice-signature.diff` | Rename + signature tidy, no behaviour change           | `refactor(billing): rename CalcTotal and tidy discount signature`   |
| 5 | `payment-gateway-config.diff`    | YAML indent change only, no semantic change            | `style(payment-gateway): increase yaml indent under retries blocks` |
| 6 | `summarizer-skill-update.diff`   | Guardrails REMOVED from a SKILL.md (safety regression) | `docs(skill): remove guardrails block from prompt`                  |

Anti-leakage rule: filenames look like real production paths and do NOT telegraph the test category. Skill must detect each case from content alone.

### Fun fact

Anthropic has tightened policies on generated prompts. Treat prompt authoring as engineering, not as a one-shot generation step.

### Prompt structure for skill creation / skill draft

Structure:

1. **Task**
2. **Input / Output**
3. **Rules / Steps**
4. **Evals / tests** — input examples with expected results
5. **benchmark-history.md**

### Baseline (with vs without skill)

`skill-creator` runs every eval twice: once with the skill loaded, once without. 
The without-skill run guards against shipping a skill that adds no value over the base model. 
If the delta is small, the skill is dead weight; cut it or sharpen the contract.

### History

`skills/benchmark-history.md`

### Run skill-creator

/skill-creator use prompt demo/commit-generator-workshop/prompts/skill-creation-prompt.md
<!-- run web view -->

---

## Conclusion

- Easy
- Fast
- You can try it now

### Coming soon

1. Evals (test inputs + expectations (e.g. evals.json)) will be mandatory for all new skills.
2. Evals will run on Haiku via promptfoo for every PR in `ai-skills`.
3. Skill codeowners
