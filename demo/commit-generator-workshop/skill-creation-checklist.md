# How to build an AI skill without losing your mind

## 1. Constrain the scope (Testability Gate)

- [ ] I can describe **input** and **output format** without phrases like "and so on" or "use AI judgement".
- [ ] I can write **3 binary (pass/fail) checks** that prove the output is correct. If I cannot — the task is too broad. Decompose it.

## 2. Prepare the tests (Golden standard)

- [ ] I have a **smoke-test** fixture — the ideal happy-path scenario.
- [ ] I have a **variant** fixture — a meaningfully different but still valid input (e.g., different domain, different file type).
- [ ] I have a **negative-test** fixture, and I know exactly how the skill should respond (typically a literal sentinel string).
- [ ] I have a written **expected output** for each of the three fixtures.

## 3. Write the mega-prompt (Contract)

Instead of dragging the LLM through long Q&A sessions, ship one hard contract. The mega-prompt is the SOLE source of truth for skill behaviour — every rule that earns its keep ships here, every rule that doesn't gets cut. Below is the structure that survives contact with evals.

### 3.1 Task and I/O surface

- [ ] **Task description** (1–2 sentences, no fluff). Names the verb the skill performs.
- [ ] **Input definition** — exact format and source. Not "a diff" but "a `git diff --staged` payload in the user prompt". If input is a file path, state path-relative-to-what.
- [ ] **Output definition** — exact shape, what to suppress (no preamble, no markdown fences, no greetings, no trailing whitespace).

### 3.2 Format rules

- [ ] **Hard structural constraint** — concrete pattern (e.g., `type(scope): description`), not "a commit message".
- [ ] **Hard length limit in characters** (e.g., `≤72`). "Short" / "concise" are not constraints, they're hopes.
- [ ] **Casing and mood rules** — imperative mood, lowercase first letter, no trailing period. Reviewers shouldn't have to debate style on each diff.

### 3.3 Decision logic (the hardest part)

This is where most prompts fail. The LLM has to pick ONE answer when the input is ambiguous. The contract must remove the ambiguity for it.

- [ ] **Decision table** — explicit type/category selection rules covering every realistic input. Not "use fix for bug fixes" alone; also rules for rename-only, whitespace-only, tests-only, docs-only, build/CI files.
- [ ] **Priority order** — when two rules match (e.g., a rename inside a test file), state the tie-breaker. The mega-prompt uses **"apply in order, first matching rule wins"**.
- [ ] **Scope rules** — when to include scope, when to omit (e.g., "omit if change spans many areas"). Otherwise the model invents inconsistent scopes.

### 3.4 Edge-case behaviour (positive AND negative)

Pure "negative rules" (abort/error sentinels) are only half the story. The other half is **positive-action-with-required-vocabulary** — when something dangerous is happening, force the output to scream about it.

- [ ] **Negative-output rule** — if input is empty/invalid, output EXACTLY a literal sentinel string. No type prefix, no explanation, no creative paraphrase. The skill must fail loudly and identically every time.
- [ ] **Safety-signal rule** — if input touches protective code (guardrails, auth checks, rate limits, validators), output MUST contain specific verb + noun tokens (e.g., `remove` + `guardrails`). Forbid cosmetic verbs like `cleanup` / `simplify` / `tidy` for safety-removal changes — they hide signal from reviewers.
- [ ] **Variant-input rules** — if the same task has a meaningfully different valid shape (e.g., docs vs code, single-file vs multi-file), each gets its own rule, not "use judgement".

### 3.5 In-prompt examples (load-bearing, not decoration)

Examples in the prompt itself anchor the model. Each example covers a decision-logic branch from §3.3 or an edge case from §3.4. If you can't write an example for a rule, the rule is too abstract.

- [ ] **One example per decision-table branch** that the smoke set tests.
- [ ] **One negative example** showing the abort sentinel verbatim.
- [ ] **One safety-signal example** showing required-vocabulary output.
- [ ] **Examples use realistic inputs**, not "Foo bar baz". Match the production data the skill will actually see.

### 3.6 Out-of-band rules

The mega-prompt also documents instructions to the orchestrator (the agent running the skill), not the skill itself:

- [ ] **After-run housekeeping block** — what to do with `benchmark.json` after each iteration (e.g., call `append_history.py` to log the iteration into the cumulative history file). Marked clearly so the skill doesn't try to execute it.

### 3.7 Cross-check against this workshop's mega-prompt

Open `prompts/skill-creation-prompt.md` side-by-side with this checklist. Every item above maps to a section of that file:

| Checklist item                | skill-creation-prompt section      |
| ----------------------------- | ---------------------------------- |
| 3.1 Task / I/O                | "Task" + "Input" + "Output"        |
| 3.2 Format rules              | Rules 1 + 2 + 5                    |
| 3.3 Decision logic            | Rule 3 (with first-match)          |
| 3.4 Edge cases                | Rules 6 + 7                        |
| 3.5 Examples                  | "Examples and evals" section       |
| 3.6 Orchestrator housekeeping | `prompts/append-history-prompt.md` |

If your own skill's prompt is missing a row, either add it OR write down why this skill doesn't need it. Don't skip silently.

## 4. Automate the boilerplate via `skill-creator`

- [ ] I used `skill-creator` instead of hand-writing `SKILL.md`.
- [ ] `skills/commit-generator/evals/evals.json` was generated (or hand-authored as backup) with checks bound to my fixtures.
- [ ] I ran a **Baseline Comparison** — my skill outperforms the base model on quality, cost, or both. If not, the skill is dead weight.

## 5. Ship discipline

- [ ] PR is **draft** by default.
- [ ] Commit message follows Conventional Commits (the skill itself should help here).
- [ ] No `.env`, `__pycache__`, or build artifacts tracked.
- [ ] Tests pass locally before pushing.
- [ ] Skill pushed to the target repo as a draft PR — that repo's CI handles lint and gates merge. Workshop scope is TDD + history capture only.
- [ ] After every iteration, `python3 scripts/append_history.py --benchmark <workspace>/iteration-N/benchmark.json --iter iter-CG<N>` appended one row to `skills/benchmark-history.md`.

## Anti-patterns — if any of these apply, stop and revisit

- "I'll add tests after the skill works" → no. Tests are the contract.
- "The prompt is long; I'll split it into multiple skills" → split by **single responsibility**, not by prompt length.
- "Skill outputs vary run-to-run, that's fine" → not fine. Either constrain via `<response_format>` or add a determinism check.
- "Baseline comparison failed but the skill feels better" → trust the numbers, not the feeling.
