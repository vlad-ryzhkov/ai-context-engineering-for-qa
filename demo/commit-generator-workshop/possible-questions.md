# Possible questions — rehearsal checklist

Speaker's own open questions to resolve before stage. Audience Q&A goes elsewhere.

| #  | Question / uncertainty                                                                                                                                                                                                                                                                                            | Where it surfaces                 | Resolution                                                        |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|-------------------------------------------------------------------|
| Q1 | Does `skill-creator` accept our pre-built `skills/commit-generator/evals/evals.json` verbatim, or will it rewrite the schema?                                                                                                                                                                                     | Section 1, Q4 handover            | Match — paste back the verification turn if it tries to reformat. |
| Q2 | What's the expected with-skill pass rate on the 6 evals using `prompts/skill-creation-prompt.md`? Rehearse once before stage. Most likely 5–6 / 6. Case 5 (yaml indent) carries highest variance.                                                                                                                 | Section 1, after-run              | TBD — measure once.                                               |
| Q3 | Baseline (`without_skill`) pass rate — what to expect? Likely 2–3 / 6. Case 1 (`auth-token-validator`) probably passes since model defaults to `feat`/`fix`. Case 6 (`summarizer-skill-update`) safety-removal almost certainly fails baseline. This is the demo's value claim — verify with one rehearsal run.   | Section 1, after-run              | TBD — measure once.                                               |
| Q4 | 6 evals × 2 configs (with-skill + without-skill) = 12 parallel subagents. Token + wall-clock cost. Confirm Claude Code quota allows. If rate-limited → cut to cases 1, 3, 6. Keep case 6.                                                                                                                         | Section 1, pre-flight             | Pre-flight rehearsal needed.                                      |
| Q5 | `description:` frontmatter must NOT contain `<` or `>`. Pre-empt by writing description in plain text; keep `<guardrails>` and `<response_format>` in the body.                                                                                                                                                   | Section 4, validator pitfalls     | Documented — interaction script reminds.                          |
| Q6 | Stage runs on Claude Code, not `claude.ai`. `claude.ai` skips baseline and benchmarking (no subagents). Verify environment before walking on stage.                                                                                                                                                               | Section 1, environment check      | Pre-flight verification.                                          |
| Q7 | Does `<guardrails>` markdown-lint cleanly in this repo? `~/.claude/rules/markdown-lint.md` allows only `<br>`. Check repo's `.markdownlint.json` for an MD033 disable.                                                                                                                                            | Section 4, lint pitfalls          | Verify locally.                                                   |
| Q8 | Audience may ask: "isn't this expensive?" — answer ready. With-skill uses ~40% fewer output tokens than naive prompting because output is constrained to one line and the negative case short-circuits. Quote the real number from the rehearsal `benchmark.json`.                                                | Section 1, after-run narration    | Use real number, not estimate.                                    |
| Q9 | Audience may ask: "do we have org-wide commit-message linters?" — answer ready. Searched `inDriver/base-workflows`, `inDriver/common`, GitHub workflows. None exist. Only personal global regex in `~/.claude/rules/git.md`. Per-repo husky/commitlint possible — check target repo's `package.json` / `.husky/`. | Section 4, commit-message linters | Answered in `demo-plan.md` Section 4.                             |
---

## Audience Q&A — likely questions from backend engineers

### Concept

**Q: A skill is just a prompt. Why is it better than a normal function in a shared library? Where's the boundary between "needs a skill", "needs regular code", "needs an MCP tool"?**

This is the main question. A skill is a pre-saved, reviewed, version-controlled prompt — and right now we have no other standard way to store prompts as engineering artefacts. Use a skill when the task is fuzzy (natural-language input, judgement output) and a function when it is deterministic. MCP tools are the bridge: a skill calls a tool when it needs side effects (file system, HTTP, DB).

**Q: Is a skill a Claude lock-in? What if we move to GPT or Gemini tomorrow?**

The skill format is portable Markdown + frontmatter. Any LLM we use can consume it. We will not block any provider at the format level.

**Q: How do you version skills?**

In progress. The plan is SemVer on the skill folder plus a frozen eval set per minor version, so rollback is a `git revert` on the skill directory.

### Useful skills beyond commit-generator

**Q: I write Go and Kotlin. Which skills are actually useful in my daily flow?**

`golang-tester`, `golang-codereview`, `mysql-designer`, `bash-reviewer`, the API test family (`api-test-cases`, `api-tests`, `api-test-review`). The roadmap targets recurring, rule-driven backend tasks.

### Tests / evals

**Q: Evals — unit tests or integration tests? What are we actually covering — the model, the prompt, or the combination?**

The combination. Smoke as the minimum bar; full coverage is possible because LLM-as-judge lets us assert on semantic properties, not exact strings. Treat evals like contract tests against the model + prompt.

**Q: How do you measure coverage when there are no code branches to count?**

Open question; in the next step we'll define coverage as "expected behaviours covered by at least one eval" rather than line coverage. Not solved yet.

**Q: Why 6 evals and not 60? Statistical significance?**

Six is the smoke set — fast feedback during authoring. A larger curated set lives alongside in production usage; the demo uses six so the stage run finishes in workshop time.

**Q: With-vs-without baseline — what if the delta is 5 pp but cost is 3×? Where's the tipping point?**

Then the skill is dead weight; cut it or sharpen the contract. The benchmark's `run_summary` columns show pass-rate AND token cost so you can see both axes at once.

### Cost / latency

**Q: Token cost per call × N calls per day = ? Where does the skill budget come from?**

Skills should run cheaply on Haiku. Cheaper and faster than developer time on the same task — that's the economic argument.

### Security

**Q: Prompt injection — diff contains user input (commit body, filenames). What stops an attacker from injecting "ignore previous instructions"?**

The `ai-skills` repo includes security checks in skill review; promptfoo adds more security checks at eval time. Both layers fire before a skill ships.

**Q: PII / proprietary code in diffs goes to Anthropic. Compliance? GDPR? Inner sensitive code?**

Safety by contract — the same boundary as any other AI usage in the company. If a flow is not approved for external AI today, it is not approved for skills either.

### Integration into the dev flow

**Q: IDE, CLI, pre-commit hook, or CI? Where do we wire `/commit-generator` in?**

It is an example skill. The prompt can be wired in as a pre-commit hook (or anywhere else the team prefers); the workshop does not prescribe one integration point.

### Repo / process

**Q: Where does a skill live — monorepo `ai-skills` or per-service?**

Initially in `ai-skills`. There is a flow for using skills from individual service repos once they're stable.

**Q: Codeowners — who can change a skill?**

The author plus designated editors. CI on the target repo gates merges; reviewers ping authors manually until the codeowners file is finalised.

**Q: CI gate — does an eval failure block merge? What's the threshold?**

Work in progress. Rough target is ~70% pass rate, but each eval is unique — a skill that ships with a hard contract (commit-generator) needs a higher bar than an exploratory one.

