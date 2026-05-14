I want to create a skill called **commit-generator**.

**Task:** Analyze a git diff and output a single, concise Conventional Commits message.

**Input:** A standard git diff in the user prompt (format `git diff --staged`).

**Output:** Return ONLY the commit message. No explanations, no markdown fences, no greetings, no trailing whitespace.

**Rules:**

1. Strictly follow Conventional Commits format: `type(scope): TICKET-NNN: description`.
2. Max length is 72 characters total (including the ticket-ID prefix).
3. **Ticket ID is mandatory.** The literal placeholder `TICKET-NNN` stands for the real project ticket (e.g. `CORE-1234`, `AIS-12`, `PAY-7`) and must match `[A-Z]{2,}-[1-9]\d*`. Always emit a ticket slot in the message; never collapse it. Try to infer the ticket ID from the diff (file paths, branch hints, code comments, PR references). If no ticket ID can be inferred from the diff, substitute the literal placeholder `NO-TICKET` — do NOT make one up.

   Examples (placeholders shown in `<...>`):
   - `feat(auth): JWT-42: add length validation for JWT tokens`
   - `refactor(billing): NO-TICKET: rename CalcTotal and tidy signature`
   - `feat(chat)!: CM-1933: drop deprecated fields from chat contract`
4. Type selection (apply in order — first matching rule wins):
   - Diff changes only whitespace, indentation, or formatting (no token changes) → `style`.
   - Diff renames symbols, reorders parameters, or rearranges code with no behaviour change → `refactor`.
   - Diff fixes a defect that altered behaviour for users → `fix`.
   - Diff adds new user-visible behaviour → `feat`.
   - Diff modifies tests only → `test`.
   - Diff modifies docs, prompts, `SKILL.md`, or `README.md` → `docs`.
   - Diff modifies build, CI, or tooling files → `chore`.
5. Scope is the affected subdirectory or top-level domain (e.g., `auth`, `skill`, `billing`, `server`). Omit if the change spans many areas.
6. Description is imperative mood, lowercase first letter, no trailing period.
7. **Breaking-change rule:** If the diff removes or renames a public API field, an exported function signature, a protobuf field, or any other contract consumers depend on, append `!` after the scope (e.g. `feat(chat)!:`). Removal of fields from a `.proto` file is ALWAYS breaking even when the field was marked `deprecated` — downstream code may still read it.
8. **Safety-removal rule:** If the diff REMOVES a `<guardrails>` block, a security check, or any other protective code, the description MUST contain the verb `remove` / `delete` / `drop` AND the noun `guardrails` / `protection` / `safety` (or the closest applicable term). Never use cosmetic verbs like `cleanup`, `simplify`, or `tidy` when protection is being removed — the reviewer needs an unambiguous signal.
9. **Secrets-leak rule:** If the diff ADDS a literal credential (a value matching `sk_live_`, `sk_test_`, `AKIA[0-9A-Z]{16}`, `ghp_[A-Za-z0-9]{36,}`, `xox[baprs]-…`, a private-key header `-----BEGIN .* PRIVATE KEY-----`, or a hardcoded password literal), output EXACTLY: `abort: SECURITY LEAK DETECTED (API KEY)`. NEVER write a normal commit message for a leaked secret — that would help land the leak.
10. **Negative rule:** If the diff is empty or invalid, output EXACTLY: `abort: no changes detected`. No conventional commit prefix.

## Examples and evals

- Examples of inputs: `demo/commit-generator-workshop/skills/commit-generator/evals/files/`
- Evals draft: `demo/commit-generator-workshop/skills/commit-generator/evals/evals.json`

## SKILL.md content requirements

The generated `SKILL.md` MUST internalise every Rules item above. The Rules in this prompt are the contract; do NOT leave any of them implicit. Concretely, the generated SKILL.md MUST contain dedicated sections for:

a. **Ticket-ID block** between Output and Type Selection. Include a literal regex (`[A-Z]{2,}-[1-9]\d*`) and two examples — one with a real ticket (`feat(auth): JWT-42: ...`) and one with the fallback (`chore: NO-TICKET: ...`). State that the ticket slot is non-optional even when scope is omitted (`chore: NO-TICKET: tidy logging`).

b. **Breaking-change rule.** Add the literal Markdown heading `### Breaking-change rule`. Spell out the trigger list verbatim: removed/renamed public API field, exported function signature, protobuf field (including those marked `deprecated`), any contract field consumers may read. Format requirement: append `!` AFTER the scope and BEFORE the colon (`feat(chat)!:`, NOT `feat!(chat):`).

c. **Secrets-leak rule.** Add the literal Markdown heading `### Secrets-leak rule`. List the credential patterns (`sk_live_`, `sk_test_`, `AKIA[0-9A-Z]{16}`, `ghp_[A-Za-z0-9]{36,}`, `xox[baprs]-…`, `-----BEGIN .* PRIVATE KEY-----`, hardcoded password literal). Output sentinel verbatim: `abort: SECURITY LEAK DETECTED (API KEY)`. Worked example showing a wrong AI output (`feat(auth): add API token for payment gateway`) next to the correct sentinel so the contrast is visible to reviewers.

d. **Character-budget rule (NEW — 72 chars is tight after the ticket slot).** Add the literal Markdown heading `### Character budget`. The ticket prefix typically eats 8–12 chars (`CORE-1234: ` is 11). If the message exceeds 72 chars, shorten in this strict order:
   1. Drop scope (cheapest — saves 2 + scope length).
   2. Shorten description verb/phrasing (e.g. `add length validation for JWT tokens` → `validate JWT length`).
   3. Drop trailing object words (`from chat contract` → `from chat`).
   The ticket slot and `!` marker are NEVER dropped to make room.

e. **Workflow ordering.** SKILL.md's Workflow section MUST recompute the 72-char budget AFTER inserting the ticket slot — not before. Step 5 of the existing Workflow ("Check total length ≤ 72") moves to step 6, AFTER the new step 5 "Insert ticket slot (real ID or NO-TICKET)". Order: classify type → pick scope → write description → add `!` if breaking → insert ticket slot → recompute length → shorten via (d).

These are non-negotiable. If the generated SKILL.md omits any of a–e, the skill cannot pass evals 1, 2, 4, 5, 6, 7, 8 because every numbered eval's expectations regex requires the ticket slot, and evals 7/8 require the secrets/breaking-change behaviour.

## Working details

1. If a without-skill (baseline) launch has already executed once and no new files / inputs / tests / fixtures were added, do NOT rerun the without-skill baseline on every iteration.
2. You may spawn the same LLM for subagents (e.g. main Haiku → subagents also Haiku) for each "input + skill" pair.
3. You may spawn a separate grader LLM subagent for reviewing results.
4. Context isolation: for baseline execution, agents must not have access to evals.
5. Show results in Anthropic's official `skill-creator` web view.

"Haiku" here is just an example; CI will use it. Feel free to use any model for skill creation itself.

## Use the original `skill-creator` plumbing — do NOT rewrite it

This iteration's benchmarking, grading, aggregation, and web-view rendering MUST go through the canonical `skill-creator` skill bundled with Claude Code. Do not hand-build a viewer, do not write a private grading script, do not invent your own `benchmark.json` schema. Every artefact the viewer needs is already produced by `skill-creator`'s built-in scripts:

- `skill-creator/agents/grader.md` — grading subagent, writes `grading.json` with the `summary.{passed,failed,total}` block the viewer reads.
- `skill-creator/scripts/aggregate_benchmark` — aggregates per-run `grading.json` + `timing.json` into iteration-level `benchmark.json` and `benchmark.md`.
- `skill-creator/eval-viewer/generate_review.py` — serves the viewer on `localhost:<port>`; renders Outputs tab + Benchmark tab + previous-iteration diff.

If a workspace doesn't render correctly in the viewer, fix the workspace layout to match `skill-creator`'s convention (`iteration-N/eval-<id>/<cfg>/{outputs/,eval_metadata.json,grading.json,timing.json}`) — never patch the viewer.

## MANDATORY final steps (orchestrator only — NOT the commit-generator skill)

After EVERY iteration, BEFORE reporting completion to the user, the orchestrator MUST run BOTH commands below in order. Either step missing = the iteration is incomplete. Do not declare success without proof from step B (the printed `localhost` URL).

### Step A — append one row to the workshop history file

```bash
python3 demo/commit-generator-workshop/scripts/append_history.py \
    --benchmark <workspace>/iteration-N/benchmark.json \
    --iter iter-CG<N> \
    --sha "$(git rev-parse --short HEAD)" \
    --history demo/commit-generator-workshop/skills/benchmark-history.md \
    --notes "<one-line summary of what changed since previous iteration>"
```

Then print the appended row in your final report so the user can confirm the row landed. If the script exits non-zero, surface the error verbatim and stop — do not silently swallow the failure. This script is the ONLY supported way to add rows. Do not hand-edit `benchmark-history.md`.

### Step B — open the canonical eval-viewer in a browser

Launch the viewer from the installed `skill-creator` plugin path (do NOT clone it, do NOT vendor it):

```bash
VIEWER=$(python3 -c "import pathlib, glob; print(glob.glob(str(pathlib.Path.home()) + '/.claude/plugins/*/claude-plugins-official/skill-creator/*/skills/skill-creator/eval-viewer/generate_review.py')[0])")
WS=demo/commit-generator-workshop/skills/commit-generator-workspace/iteration-<N>
PREV=demo/commit-generator-workshop/skills/commit-generator-workspace/iteration-<N-1>
nohup python3 "$VIEWER" "$WS" \
    --skill-name commit-generator \
    --benchmark "$WS/benchmark.json" \
    $( [ -d "$PREV" ] && echo "--previous-workspace $PREV" ) \
    > /tmp/eval-viewer-CG<N>.log 2>&1 &
echo "VIEWER_PID=$!"
sleep 2
grep -Eo "http://localhost:[0-9]+" /tmp/eval-viewer-CG<N>.log | head -1
```

Print the resolved `http://localhost:<port>` URL in your final report. The iteration is NOT complete until the user can click that URL. If `grading.json` files are missing or malformed, do NOT patch the HTML — re-run grading via `skill-creator`'s grader subagent so the canonical schema (`summary.passed`, `summary.failed`, `summary.total`, `expectations[].passed`) lands on disk.
