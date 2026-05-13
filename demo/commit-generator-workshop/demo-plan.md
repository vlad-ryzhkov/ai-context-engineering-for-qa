# Workshop — `commit-generator` skill

Live demo: build a Conventional Commits skill from scratch, run evals, ship.

This file is the on-stage script. Open it in the IDE. Walk top-to-bottom.

## Coverage

1. Live demo — creating a simple AI skill step by step.
2. Overview of the `ai-skills` repository structure and development flow.
3. Repository guide and best practices.
4. Linters, validation, and common pitfalls.

---

## Section 1 — Live demo (creating a skill step by step)

### Pre-flight (open these in IDE tabs before stage)

1. `prompts/skill-creation-prompt.md` — the contract; paste during `skill-creator` Q3.
2. `prompts/append-history-prompt.md` — orchestrator housekeeping after every iteration.
3. `skills/commit-generator/evals/evals.json` — 6 evals, real `skill-creator` schema.
4. `skills/commit-generator/evals/files/*.diff` — 6 fixture diffs, realistic prod-style names.
5. `expected/*.expected.txt` — paired reference outputs, eyeball-only.
6. `benchmark-history-template.md` — column layout for the cumulative log.
7. `skill-creation-checklist.md` — the engineer checklist (share at the end).
8. `possible-questions.md` — Q&A scratchpad.

### Think first

A skill earns its keep only when the task is repetitive, the input/output are strict, and the execution flow is simple. If you cannot answer "what are the binary pass/fail checks?" — the task is too broad. Decompose.

### The 4 `skill-creator` interview questions

`skill-creator` asks 4 questions before drafting `SKILL.md`. Scripted answers:

**Q1 — "What should this skill enable Claude to do?"**

> Analyze a `git diff --staged` payload and output a single Conventional Commits message — `type(scope): description`, max 72 chars, no preamble. The skill must handle empty diffs with a literal sentinel string.

**Q2 — "When should this skill trigger?"**

> Triggers when the user runs `/commit` or `/commit-message`, or pastes a `diff --git` payload and asks for a commit message. Should NOT trigger for general code review or for prose summarisation.

**Q3 — "What's the expected output format?"**

Paste the full contents of `prompts/skill-creation-prompt.md`. That file is the contract.

**Q4 — "Should we set up test cases?"**

> Yes. Six fixtures are ready in `demo/commit-generator-workshop/skills/commit-generator/evals/files/`. Three baseline + three edge cases. Eval JSON is at `skills/commit-generator/evals/evals.json` — please copy the whole `evals/` directory into the new skill as-is (paths in `files[]` are relative and assume that layout). Reference outputs live under `expected/` for stage eyeballing.

### TDD preparation (why we pre-built the evals)

It is faster and cheaper to author a skill when fixtures and expected outputs already exist. The 6 cases:

| #   | Fixture                          | What it tests                                          | Expected output                                                     |
| --- | -------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------- |
| 1   | `auth-token-validator.diff`      | New behaviour added in `auth/token.go`                 | `feat(auth): add length validation for JWT tokens`                  |
| 2   | `analyzer-skill-guardrails.diff` | Docs change (guardrails block added to a SKILL.md)     | `docs(skill): add strict guardrails to prompt`                      |
| 3   | `no-staged-changes.diff`         | Empty diff (negative test)                             | `abort: no changes detected`                                        |
| 4   | `billing-invoice-signature.diff` | Rename + signature tidy, no behaviour change           | `refactor(billing): rename CalcTotal and tidy discount signature`   |
| 5   | `payment-gateway-config.diff`    | YAML indent change only, no semantic change            | `style(payment-gateway): increase yaml indent under retries blocks` |
| 6   | `summarizer-skill-update.diff`   | Guardrails REMOVED from a SKILL.md (safety regression) | `docs(skill): remove guardrails block from prompt`                  |

Anti-leakage rule: filenames look like real production paths and do NOT telegraph the test category. Skill must detect each case from content alone.

### After the run lands

1. Open the skill-creator viewer (`generate_review.py`). Walk through case 1 (baseline pass) + case 6 (safety-removal payoff).
2. Show `benchmark.json` delta: with-skill pass rate vs without-skill pass rate.
3. Append the iteration to the cumulative history (see `prompts/append-history-prompt.md`):

```bash
python3 scripts/append_history.py \
    --benchmark <workspace>/iteration-1/benchmark.json \
    --iter iter-CG1 \
    --sha "$(git rev-parse --short HEAD)" \
    --notes "first run — 6 evals, with-vs-without baseline"
```

Show `skills/benchmark-history.md` afterwards — one new row, same column layout as the inDriver `ai-skills` eval-generator history.

### Baseline (with vs without skill)

`skill-creator` runs every eval twice: once with the skill loaded, once without. The without-skill run guards against shipping a skill that adds no value over the base model. If the delta is small, the skill is dead weight; cut it or sharpen the contract.

---

## Section 2 — `ai-skills` repository structure

```
ai-skills/
├── skills/
│   └── <skill-name>/
│       ├── SKILL.md                 # YAML frontmatter + body, the contract
│       ├── references/              # optional deep-dive docs
│       ├── scripts/                 # optional executable helpers
│       └── evals/
│           ├── evals.json           # skill-creator schema (skill_name, evals[], expectations[])
│           └── files/               # input fixtures the evals reference
├── tests/                           # cross-skill regression suites (eval-generator etc.)
├── scripts/                         # repo-level CI helpers
│   ├── lint.sh                      # canonical skill linter — runs in PR CI
│   ├── lib/                         # shared bash libs sourced by lint.sh
│   └── promptfoo/                   # promptfoo runner + report.py + l2_summary.py
└── docs/
    └── skill-creation-guide.md      # source-of-truth guide; read before authoring a skill
```

The workshop folder mirrors a subset of this structure:

```
demo/commit-generator-workshop/
├── demo-plan.md                     # this file
├── skill-creation-checklist.md      # the engineer checklist
├── possible-questions.md            # Q&A scratchpad
├── benchmark-history-template.md    # column layout reference for the cumulative log
├── expected/                        # 6 reference outputs (human eyeball)
├── prompts/
│   ├── skill-creation-prompt.md     # the skill contract (pasted into skill-creator Q3)
│   └── append-history-prompt.md     # orchestrator housekeeping (NOT part of the skill)
├── skills/
│   └── commit-generator/            # the skill itself (mirrors ai-skills/skills/<name>/)
│       └── evals/
│           ├── evals.json
│           └── files/               # 6 input fixtures
└── scripts/
    └── append_history.py            # the only script we own; appends one row per iteration
```

When the skill ships, `skills/commit-generator/` moves to `ai-skills/skills/commit-generator/` and `scripts/append_history.py` either lives alongside `ai-skills/scripts/promptfoo/` or is replaced by the canonical `l2_summary.py` once the eval rig migrates to promptfoo.

---

## Section 3 — Repository guide and best practices

Read `ai-skills/docs/skill-creation-guide.md` end-to-end before authoring a new skill. Key principles:

- **Iterate with real inputs several times.** Evals catch what introspection misses.
- **Watch token consumption.** A skill that uses 3× more tokens than the baseline and gains 5 pp of pass rate is rarely worth it. The `benchmark.json` `run_summary` columns tell you.
- **One skill, one responsibility.** Long prompt is fine; split only when there are two distinct verbs the skill performs.
- **Keep examples in the prompt.** Each decision-table branch and each edge-case rule should have one concrete input/output example inside `SKILL.md`. If you cannot write the example, the rule is too abstract.
- **Anti-leakage in fixtures.** Filenames must NOT telegraph defect category (`bad_*`, `*_negative`, `should-fail` are forbidden). The skill must detect defects from content alone — otherwise the eval measures filename-pattern recognition, not skill value.

### Rumour

Anthropic has tightened policies on generated prompts. Treat prompt authoring as engineering, not as a one-shot generation step.

### Skill codeowners

In progress. Until then, target-repo CI gates merges; reviewers ping authors manually.

### Coming soon

1. Evals will be mandatory for all new skills.
2. Evals will run on Haiku via promptfoo for every PR in `ai-skills`.

---

## Section 4 — Linters, validation, and common pitfalls

### Skill linter (in `ai-skills/scripts/lint.sh`)

The canonical linter ships in the `ai-skills` repo. It is NOT vendored into the workshop. When the skill lands in `ai-skills/skills/<name>/`, repo CI runs:

```bash
bash scripts/lint.sh --skill <name>
```

Checks structure, required frontmatter fields, security patterns, file hygiene. Severity is Anthropic-aligned: `ERROR` blocks CI, `WARN` is logged only. Workshop scope is TDD + history capture; lint happens at PR time, not on stage.

### `skill-creator` internal validator

`skill-creator` ships its own `quick_validate.py`. It runs automatically during skill draft. Pitfalls that fail it:

- `description:` frontmatter contains `<` or `>` — angle brackets are forbidden in the description string. Use the body for `<guardrails>` / `<response_format>` blocks.
- Unknown frontmatter keys. Allowed: `{name, description, license, allowed-tools, metadata, compatibility}`.
- Skill name not kebab-case, or longer than 64 characters.
- Description longer than 1024 characters.

### Commit-message linters

**Question that came up:** "do we have linters or rules for commit messages?"

Two levels exist in theory: organisation/branch-protection rules and per-repo enforcement.

After searching `inDriver/base-workflows`, `inDriver/common`, and the GitHub workflows in `base-workflows/.github/workflows/`, there is no commitlint config, no Conventional Commits checker, and no commit-message workflow. The only enforced regex lives in personal global rules at `~/.claude/rules/git.md`:

```text
^(build|chore|ci|docs|feat|feature|bugfix|fix|hotfix|perf|refactor|style|test)(\([A-Za-z0-9_.-]+\))?(!)?: [A-Z]{2,}-[1-9]\d*: .*|^Merge.*|^Revert.*
```

So **org-wide commit-message enforcement does not exist today**. Some repos add commitlint via husky on a per-repo basis; check the target repo's `package.json` / `.husky/` before assuming. Otherwise, enforcement falls to reviewers — which is exactly the gap `commit-generator` closes for the author.

### Common pitfalls in the workshop flow

| Pitfall                                             | Symptom                                                             | Fix                                                                                       |
| --------------------------------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| 6 evals × 2 configs = 12 parallel subagents         | Rate-limit error mid-run                                            | Cut to 3 evals (cases 1, 3, 6). Keep case 6 — it is the safety-removal payoff.            |
| `skill-creator` rewrites the schema of `evals.json` | `expectations[]` becomes a different shape                          | Paste back: "Use the file at `skills/commit-generator/evals/evals.json` verbatim."        |
| `description:` contains `<guardrails>`              | `quick_validate.py` fails frontmatter check                         | Move the tag into the SKILL.md body; keep description plain text.                         |
| Whitespace-only fixture invisible in IDE            | Audience sees no diff and asks "what changed?"                      | Workshop uses `payment-gateway-config.diff` (yaml indent shift) — visible at 4-space tab. |
| Filename leaks test category                        | Skill scores high because it pattern-matches filenames, not content | Realistic prod-style names only. No `smoke_`, `bad_`, `negative_`, `broken_`.             |
| `cleanup` / `simplify` in a safety-removal commit   | Reviewer skims past the dangerous change                            | Case 6 in evals enforces required vocabulary (`remove` + `guardrails`).                   |
| `claude.ai` instead of Claude Code                  | No subagents → no baseline → no benchmark                           | Run the demo on Claude Code only.                                                         |

---

## Closing

Hard contract, six evals, with-vs-without baseline, cumulative history file. The skill ships to the target repo; CI handles lint there. The workshop's job is the TDD muscle and the history discipline.

QR-code → `skill-creation-checklist.md`.
