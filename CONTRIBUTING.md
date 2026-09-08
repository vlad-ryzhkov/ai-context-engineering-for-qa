# Contributing

## Prerequisites

- Node.js 20+ (for `markdownlint` and `vigiles`)
- Python 3.11+ (for `scripts/context-check.py` and `scripts/ai-efficiency.py`)
- Bash 4+
- Git hooks: `bash scripts/setup-hooks.sh`

## Creating a skill

1. Run `/init-skill` — it follows the template and checklist.
2. **Add it to [`SKILLS.md`](SKILLS.md) in the same change.** A skill missing from
   that table is a defect; it is the only skill list in the repository, and
   `/improve-project-context` will flag the skill as an orphan.
3. Validate structurally: `npx vigiles lint`.
4. Audit the content: `/skill-audit`.

Skills go flat in `.claude/skills/<name>/`. Do **not** group them into category
subdirectories — Claude Code only discovers `SKILL.md` exactly one level deep, so
`.claude/skills/api-testing/api-tests/SKILL.md` would never load. Grouping is
editorial and lives in `SKILLS.md`.

## Quality requirements

| Check | Command | Threshold |
| ----- | ------- | --------- |
| Harness structure | `npx vigiles lint` | Zero errors |
| Harness grade | `npx vigiles audit` | No regression |
| Context budget | `python3 scripts/context-check.py` | Exit code 0 |
| Skill size | `/skill-audit` | ≤500 lines (warn over 400) |
| Markdown | `npx markdownlint -c .markdownlint.yaml '**/*.md'` | Zero errors |

Two skills currently exceed the size cap — see
[Known limitations](docs/patterns.md#known-limitations). Do not add a third.

### Baseline sections required in every `SKILL.md`

Enforced by `.claude/hooks/skill-lint.sh` on every edit:

- **Quality Gate / Self-Review** — an inline checklist before output
- **Gardener** — a reference to `.claude/protocols/gardener.md`
- **SILENT MODE / Verbosity** — token economy compliance
- **SKILL COMPLETE** — a structured completion block

Utility skills under ~100 lines may skip most of these; the full baseline would
exceed their logic. Record the exemption in
[Known limitations](docs/patterns.md#known-limitations).

## Anti-patterns

QA anti-patterns for generated code live in `.claude/qa-antipatterns/`. To add one:

1. Create `.claude/qa-antipatterns/{category}/{problem-name}.md`
2. Add an entry to `_index.md` — the index must list every pattern file
3. Run `npx vigiles lint` to verify nothing dangles

## Git hooks

| Hook | What it checks |
| ---- | -------------- |
| `pre-commit` | Forbidden files, secret patterns, staged `SKILL.md` structure |
| `pre-push` | Branch naming, forbidden files, Kotlin compilation, markdownlint |
| `skill-lint` (post-edit) | Line count, baseline sections, forbidden patterns |
| `delta-guard` (post-edit) | Warns when a governed context file is fully rewritten instead of edited |
| `no-egress` (pre-Bash) | Blocks commands that move bytes off the machine — see [19. Egress fence](docs/patterns.md#19-egress-fence) |

Install the git hooks: `bash scripts/setup-hooks.sh`
The three assistant hooks are wired in `.claude/settings.json` and need no install.

Changing `no-egress.sh` means running its tests:

```bash
shellcheck .claude/hooks/no-egress.sh && bash .claude/hooks/no-egress.test.sh
```

20 cases — 10 that must block, 10 that must pass. Both must be green.

## The Gradle build

`./gradlew compileTestKotlin` works from a clean clone with **no Gradle and no
JDK 17 installed**. Two things make that true, and both were broken before:

- `gradle/wrapper/gradle-wrapper.jar` is committed. A blanket `*.jar` rule in
  `.gitignore` used to swallow it, so `./gradlew` died with
  `Unable to access jarfile` in every fresh clone. The rule now carries an
  explicit negation for that one path.
- `settings.gradle.kts` applies the Foojay toolchain resolver, so Gradle
  downloads the JDK that `build.gradle.kts` asks for. Without it,
  `kotlin { jvmToolchain(17) }` failed with
  `Cannot find a Java installation ... matching {languageVersion=17}` on any
  machine whose JDK was not 17 — which is most of them.

Only one JDK is needed to bootstrap: whatever runs Gradle itself. Verified on
JDK 24 with no JDK 17 present.

If you ever regenerate the wrapper, the version must match `distributionUrl` in
`gradle/wrapper/gradle-wrapper.properties`:

```bash
gradle wrapper --gradle-version 9.2.1
```

## Where things go

| Adding | Goes in |
| ------ | ------- |
| A working skill | `.claude/skills/` **and** `SKILLS.md` |
| An unfinished experiment | `in-progress/`, referenced from nothing in `.claude/` |
| A design decision worth explaining | `docs/patterns.md` |
| Something being retired | `archive/`, with a README saying why |

## PR checklist

- [ ] `npx vigiles lint` passes
- [ ] `python3 scripts/context-check.py` exits 0
- [ ] `/skill-audit` reports zero errors on any skill you touched
- [ ] New or renamed skill is in `SKILLS.md`
- [ ] `SKILL.md` ≤500 lines; overflow moved to `references/`
- [ ] No internal company names, hostnames, or private tooling contracts
