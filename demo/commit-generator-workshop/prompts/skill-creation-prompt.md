I want to create a skill called **commit-generator**.

**Task:** Analyze a git diff and output a single, concise Conventional Commits message.

**Input:** A standard git diff in the user prompt (format `git diff --staged`).

**Output:** Return ONLY the commit message. No explanations, no markdown fences, no greetings, no trailing whitespace.

**Rules:**

1. Strictly follow Conventional Commits format: `type(scope): description`.
2. Max length is 72 characters total.
3. Type selection (apply in order — first matching rule wins):
   - Diff changes only whitespace, indentation, or formatting (no token changes) → `style`.
   - Diff renames symbols, reorders parameters, or rearranges code with no behaviour change → `refactor`.
   - Diff fixes a defect that altered behaviour for users → `fix`.
   - Diff adds new user-visible behaviour → `feat`.
   - Diff modifies tests only → `test`.
   - Diff modifies docs, prompts, `SKILL.md`, or `README.md` → `docs`.
   - Diff modifies build, CI, or tooling files → `chore`.
4. Scope is the affected subdirectory or top-level domain (e.g., `auth`, `skill`, `billing`, `server`). Omit if the change spans many areas.
5. Description is imperative mood, lowercase first letter, no trailing period.
6. **Safety-removal rule:** If the diff REMOVES a `<guardrails>` block, a security check, or any other protective code, the description MUST contain the verb `remove` / `delete` / `drop` AND the noun `guardrails` / `protection` / `safety` (or the closest applicable term). Never use cosmetic verbs like `cleanup`, `simplify`, or `tidy` when protection is being removed — the reviewer needs an unambiguous signal.
7. **Negative rule:** If the diff is empty or invalid, output EXACTLY: `abort: no changes detected`. No conventional commit prefix.

## Examples and evals

- Examples of inputs: `demo/commit-generator-workshop/skills/commit-generator/evals/files/`
- Evals draft: `demo/commit-generator-workshop/skills/commit-generator/evals/evals.json`

## Working details

1. If a without-skill (baseline) launch has already executed once and no new files / inputs / tests / fixtures were added, do NOT rerun the without-skill baseline on every iteration.
2. You may spawn the same LLM for subagents (e.g. main Haiku → subagents also Haiku) for each "input + skill" pair.
3. You may spawn a separate grader LLM subagent for reviewing results.
4. Context isolation: for baseline execution agents shouldn't have access to evals
5. Show results in official anthropic's skill-creator web-view

"Haiku" here is just an example; CI will use it. Feel free to use any model for skill creation itself.

## After-run housekeeping (instruction to the orchestrator running the evals, NOT to the commit-generator skill itself)

When skill-creator finishes an iteration and produces `<workspace>/iteration-N/benchmark.json`, append a row to the workshop history file:

```bash
python3 demo/commit-generator-workshop/scripts/append_history.py \
    --benchmark <workspace>/iteration-N/benchmark.json \
    --iter iter-CG<N> \
    --sha "$(git rev-parse --short HEAD)" \
    --notes "<one-line summary of what changed since previous iteration>"
```

Target file: `demo/commit-generator-workshop/skills/benchmark-history.md` (default). Script reads the canonical `skill-creator` `benchmark.json` schema and emits one row per iteration. Do not hand-edit the history file.
