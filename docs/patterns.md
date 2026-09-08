# Pattern catalogue

The design decisions behind this setup. Each entry names a real failure mode and
the mechanism that addresses it. This is the "why" layer; for what exists, see
[`SKILLS.md`](../SKILLS.md).

## Context loading

### 1. Layered context

**Problem:** the assistant forgets project rules between conversations.
**Mechanism:** three files loaded at different times — `CLAUDE.md` (always),
`.claude/qa_agent.md` (QA work), `SKILL.md` (that task only). Each adds detail
without paying for it on every turn.

### 2. Progressive disclosure

**Problem:** loading every instruction at once wastes tokens and dilutes
attention.
**Mechanism:** the YAML `description` is always in the prompt; the `SKILL.md`
body loads on invocation; `references/` loads only when the body points at it. A
500-line skill costs a one-line description until it is actually needed.

### 3. Keep the always-on file minimal

**Problem:** a bloated or model-generated `CLAUDE.md` measurably lowers success
rate and raises cost, because it is re-read every turn.
**Mechanism:** hand-write it; restrict it to tech stack, commands, and banned
alternatives. No codebase overviews, no duplicated docs. See
[`claudemd-instructions.md`](claudemd-instructions.md), and
[`context-budget.md`](context-budget.md) for the caps.

### 4. Skill size limit

**Problem:** a large `SKILL.md` wastes tokens and buries its own instructions.
**Mechanism:** 500 lines maximum. Over that, extract to `references/`,
`scripts/`, or `.claude/qa-antipatterns/`. Extraction beats compression —
extracted content is not loaded at all, compressed content is still loaded.

## Output quality

### 5. Anti-pattern library

**Problem:** the assistant repeats the same mistakes — hardcoded data, missing
assertions, `Thread.sleep()`.
**Mechanism:** 31 pattern files in `.claude/qa-antipatterns/` across four
categories, with an index the skill reads before generating. The generated code
is checked against them before the skill finishes.

### 6. Locked tech stack with an explicit BANNED column

**Problem:** left free, the assistant picks whatever library it saw most in
training — Gson instead of Jackson, TestNG instead of JUnit.
**Mechanism:** a fixed stack table in `CLAUDE.md` where every row names both the
required choice and the banned alternatives. Naming the banned option matters:
"use Jackson" is weaker than "use Jackson, never Gson or Moshi".

### 7. Compilation gate

**Problem:** generated test code does not compile.
**Mechanism:** the skill must run the compile command before reporting done, with
a hard retry cap. Three failures stop the run instead of looping.

### 8. Independent output review

**Problem:** a skill's own completion block is self-certification. A skill that
scored itself 90% is reporting its intent, not its result.
**Mechanism:** `/output-review` audits an artifact against the producing skill's
own checklist, in a separate run that never sees the producer's reasoning.

### 9. Traceability

**Problem:** no link between the test scenario and the automated test that
implements it.
**Mechanism:** `@Link` annotations pointing at the scenario ID in the source
matrix, emitted at generation time.

### 10. Security by default

**Problem:** generated tests ignore security cases entirely.
**Mechanism:** OWASP categories, personally identifiable data checks, injection,
cross-site scripting, and broken-object-authorisation cases are built into the
role definition and the anti-pattern files, so they are generated rather than
requested.

## Pipeline

### 11. Cross-skill artifacts

**Problem:** skills run in isolation and each re-derives what the last one
already knew.
**Mechanism:** each skill writes a file the next one reads —
`/repo-scout` → `/spec-audit` → `/api-test-cases` → `/api-tests` →
`/api-test-review`. Results stay consistent and every test traces back to a
scenario, and that to a spec finding.

### 12. Architecture auto-detection

**Problem:** output paths differ between a single-module project and a Gradle
multi-module one, and guessing wrong scatters files.
**Mechanism:** a three-step detection algorithm runs before generation and pins
`ARCH_MODE`; all paths derive from it. Ambiguous case asks rather than guesses.
See `.claude/skills/_shared/api-tests-shared.md`.

### 13. Loop guard

**Problem:** the assistant retries the same failing action indefinitely.
**Mechanism:** a hard cap of three attempts on any fix-retry cycle, then a
mandatory stop with the failure details. Documented in `CLAUDE.md`.

### 14. Meta-skill bootstrap

**Problem:** writing AI config files from scratch is tedious and easy to get
wrong.
**Mechanism:** `/init-project`, `/init-agent`, and `/init-skill` generate them
interactively, so a new repository starts from a working baseline.

### 15. Gardener protocol

**Problem:** the assistant finishes the task but never mentions the rule that
was missing and would have prevented the detour.
**Mechanism:** a shared protocol at `.claude/protocols/gardener.md` that every
skill calls before its completion block. It proposes; it never writes. The
persistence and auto-graduation half of this idea was tried and parked — see
[`in-progress/ace/`](../in-progress/ace/README.md).

## Tooling

### 16. Context budget as a measured number

**Problem:** "the context files are getting big" is not actionable, and hand
estimates are wrong — dividing bytes by four understated this corpus by 1.65x.
**Mechanism:** [`scripts/context-check.py`](../scripts/context-check.py) owns the
measurement and returns an exit code. The caps live in one file,
[`context-budget.md`](context-budget.md), because three drifting copies was a
real defect.

### 17. Effective-rate cost tracking

**Problem:** absolute spend tracks how much you worked, which says nothing about
whether the setup is efficient.
**Mechanism:** [`scripts/ai-efficiency.py`](../scripts/ai-efficiency.py) reports
dollars per million tokens for day, week, and month. The rate falls as more
context arrives as cache reads, so it measures context reuse directly.

### 18. Git hooks as the enforcement layer

**Problem:** a documented rule that nothing checks is a suggestion.
**Mechanism:** `pre-commit` blocks forbidden files and secret patterns;
`pre-push` adds branch naming, compilation, and markdownlint; `skill-lint` runs
on every skill edit. Install with `bash scripts/setup-hooks.sh`.

### 19. Egress fence

**Problem:** a skill that can read local files, run commands, and reach the
network holds all three legs of the "lethal trifecta" — a prompt injection hidden
in a specification or a test file could read a secret and send it out.

**Mechanism, in three layers, weakest claim first:**

1. **`disallowed-tools:` on every skill.** All 22 deny `WebFetch` and
   `WebSearch`; the 9 that need no shell also deny `Bash`, which closes the
   exfiltration leg outright for those 9.
2. **A deny list in `.claude/settings.json`** for the egress binaries —
   `curl`, `wget`, `nc`, `socat`, `ssh`, `scp`, `rsync`, `gh gist`.
3. **`.claude/hooks/no-egress.sh`**, a `PreToolUse` hook on `Bash` that inspects
   the command and exits 2 on anything matching those binaries. It catches what
   a glob pattern misses: quoted names (`"curl"`), a second command after `&&`,
   and `openssl s_client`. Tested by `.claude/hooks/no-egress.test.sh` — 20
   cases, 10 that must block and 10 that must pass.

**What this does not buy — stated rather than hidden.** Thirteen skills
legitimately need `Bash` for `./gradlew`, `wc`, `npx`, `gh`, or `python3`. Any
interpreter can open a socket without naming a blocked binary, so no allowlist of
command names is airtight while those skills work at all. Narrowing the grant
does not help either: `vigiles` treats a `Bash(...)` grant as bounded only when
it pins a program that cannot read a file or speak a protocol — its list is
`echo`, `printf`, `true`, `false`, `:`, `pwd`, `sleep`, `date`, `uname`,
`hostname`, `whoami`, `id`, `basename`, `dirname` — and nothing these skills need
is on it, deliberately. `wc` and `ls` are excluded by construction because they
take a file operand.

So layer 3 raises the cost of the casual exfiltration path. It is not a
containment boundary. Treat a specification from an untrusted source as
untrusted input.

### 20. Model Context Protocol servers

`context7` supplies current library documentation instead of training-cutoff
memory; `sequential-thinking` supports step-by-step analysis. Declared in
`.mcp.json`, enabled in `.claude/settings.json`.

## `/init-skill` versus the official `skill-creator`

Anthropic ships [`skill-creator`](https://github.com/anthropics/skills/tree/main/skills/skill-creator).
Both generate skills; they optimise for different things.

|                          | `/init-skill` (this repo)                   | `skill-creator` (official)                    |
| ------------------------ | ------------------------------------------- | --------------------------------------------- |
| Infrastructure           | None — bash and the filesystem              | Python 3 plus the `claude -p` CLI             |
| Style                    | Strict protocol, six checkpoints            | Conversational, flexible                      |
| Validation               | Static checklist plus bash checks           | Live A/B evals with a grading file            |
| Improve an existing skill | Yes — Phase 0 mode                         | Yes — a core feature                          |
| Description optimisation | No                                          | Yes                                           |
| Review viewer            | No                                          | Yes — generates an HTML report                |
| Best for                 | QA skills, fast iteration, zero setup       | High-reuse skills, eval-driven quality, scale |

If you want eval-driven quality, use the official one. `/init-skill` is also a
[private successor](../SKILLS.md#what-private-successor-means-here) skill: the
author's non-public version closes exactly this gap by shipping an eval suite
with each generated skill.

## Known limitations

Honest list of places where this setup does not meet its own rules.

Measured with `python3 scripts/context-check.py` and a section sweep over
`.claude/skills/*/SKILL.md`; 8 of 22 skills have a gap, listed in full below.

| Item | Status | Why |
| ---- | ------ | --- |
| `/repo-scout` (516 lines) and `/workflow-expert` (521 lines) exceed the 500-line cap | Known | Both are multi-language dispatchers; the next extraction pass should move per-language blocks into `references/` |
| `/api-test-review` has no self-review section | Known | An eight-phase review pipeline needs the density; its output *is* the quality artifact |
| `/spec-audit` has no self-review section | By design | The audit report is the quality gate |
| `/fix-markdown` (36 lines) and `/pr` (90 lines) skip most baseline sections | By design | Utility skills where the full baseline would exceed the logic |
| `/api-tests` and `/api-tests-java` have no inline completion block | By design | Both delegate to `.claude/skills/_shared/api-tests-shared.md`, which carries it. A naive grep over `SKILL.md` alone reports a false positive here |
| 13 of 22 skills still hold all three "lethal trifecta" legs | Accepted risk | They need `Bash` for gradle, `wc`, `npx`, `gh`, or `python3`, and an interpreter can open a socket without naming a blocked binary. Mitigated in three layers, none of them a containment boundary — see [19. Egress fence](#19-egress-fence) |
