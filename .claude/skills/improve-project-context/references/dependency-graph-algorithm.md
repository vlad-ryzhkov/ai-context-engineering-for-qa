# Dependency Graph Construction — Full Algorithm

Used by Step 2 (Cold Audit Mode).

Accuracy beats coverage here. A path checker that cries wolf ten times gets ignored
after the first run, and then it catches nothing at all. Every step below exists to
keep a real finding from drowning in noise — prefer a soft verdict over BROKEN
whenever the evidence is thin.

## Step 1: Identify entry points

```bash
ls -la CLAUDE.md AGENTS.md GEMINI.md 2>/dev/null   # check if symlinks
ls -la .claude/CLAUDE.md 2>/dev/null
find .claude -maxdepth 2 -type l | while read f; do echo "$f -> $(readlink $f)"; done
```

## Step 2: Extract candidate paths — three zones, never prose

A path mentioned in a sentence carries no markup, so scanning prose produces far
more noise than findings. Read three zones only:

```bash
grep -oE '\[([^]]+)\]\(([^)]+)\)' <file> | sed 's/.*(\(.*\))/\1/'   # markdown links
grep -oE '`[^`]{3,160}`' <file> | sed 's/^`//;s/`$//'               # code spans
```

Then normalise each code span before judging it:

- strip a leading pointer verb — `Read docs/x.md`, `See docs/x.md`, `→ docs/x.md`.
  This corpus writes the verb INSIDE the backticks, so a naive first-token check
  reads `Read` as the path and the real address never resolves.
- strip a leading `<scheme>://` — `file://`, `python://`.

ASCII tree blocks are a third zone. Parse them with a stack keyed on the indent
column, and **drop the block's own root line**: keep it and every entry comes out as
`myrepo/src/…` instead of `src/…`, so the whole block reads as broken.

## Step 3: Is it a path at all?

Reject before resolving. Each of these is a token that only looks like an address:

| Reject | Example |
| --- | --- |
| URL, with or without a scheme | `https://x.dev/a.md`, `github.com/org/repo/blob/main/a.md` |
| CLI flag or command | `--ref`, `git status`, `npm run x` |
| shouty word | `CREATE`, `DROP/TRUNCATE` |
| slash command | `/review`, `/simplify` |
| the path is an assignment VALUE | `CONFIG_PATH=docs/x.md` |
| a phrase, not one address | two or more spaces after normalising |
| no separator and no known extension | a skill name, a concept |

## Step 4: Templates are globs, not paths

`{date}`, `<name>`, `$VAR`, `YYYY-MM-DD`, `*` mark a naming RULE. Expand to a glob
and match it:

- glob matches something → OK, the rule is alive.
- glob matches nothing → **TEMPLATE-UNUSED**: a soft finding meaning "this rule is
  dead", never BROKEN. `~/.claude/{rules,docs}/**` is a template, not a dead path.

## Step 5: Resolve — five verdicts, not two

Walk the cascade in order and stop at the first hit. A binary present/absent test is
what produces the false-orphan flood; the middle verdicts are where the real signal
lives.

| Verdict | Condition | Treat as |
| --- | --- | --- |
| **OK** | resolves relative to the file's own dir, its bundle, or the repo root | silent |
| **OK** | written from the repo name down (`<repo-name>/CLAUDE.md`) and resolves once the prefix is dropped | silent |
| **MOVED** | does not resolve, and the basename has EXACTLY ONE tracked match in the repo | **finding — cite the replacement** |
| **AMBIGUOUS** | resolves somewhere else, or the basename matches two or more paths | note, collapsed, labelled "works / no single replacement" |
| **EXTERNAL** | absolute, `~`-relative, or its first segment does not exist here | silent — another machine or another repo |
| **BROKEN** | none of the above | finding |

Two rules make MOVED safe to act on:

- **Tracked files only** (`git ls-files`, which also honours `.gitignore` AND
  `.git/info/exclude`). An untracked or locally excluded path exists on one machine,
  so proposing it as the new home of a shared reference breaks the next clone.
- **Exactly one match.** Two candidates means no unambiguous replacement, and
  guessing one is how a checker starts being confidently wrong.

## Step 6: Recurse (max depth 3)

For each linked file, repeat Steps 2–5. Track a visited set to avoid cycles.

## Step 7: Classify all discovered nodes

- **Always-loaded:** `alwaysApply: true` in frontmatter, or loaded as CLAUDE.md.
- **JIT:** loaded only when an explicit Read instruction fires.
- **Orphan:** in `docs/` or `.claude/` and not reachable from any entry point.

**Fragility guard (BEFORE any ORPHAN verdict):** the Step 2 regexes miss HTML
`<a href>`, dynamic paths, and bare prose mentions. A candidate orphan MUST also fail
a bare-filename grep across the corpus:

```bash
grep -rl "$(basename <candidate> .md)" ~/.claude .claude 2>/dev/null
```

Found anywhere, even in unlinked prose → NOT orphan; downgrade to "loosely
referenced, consider a proper link". NEVER propose delete on regex absence alone.

## Step 8: Staleness — lag and churn, not mtime

"The file is old" is not a finding. "The project moved on and the instructions did
not" is. Measure two numbers per context file:

```bash
git log -1 --format=%ct -- <context-file>                    # when the file last changed
git log --since=@<that-timestamp> --format=%H -- <its-scope> | wc -l   # churn since
```

- **lag** = days between the context file's last change and the newest change in the
  subtree it describes.
- **churn** = how many commits landed in that subtree since.

A frozen project cannot go stale: churn 0 → report no staleness at all, whatever the
lag. High lag with high churn is the signal worth a patch card.

## Output format

```text
ENTRY [file] [N lines] [~M tokens]
├── JIT [file] [N lines] [~M tokens] — trigger: "when writing auto tests"
│   └── JIT [file] [N lines] [~M tokens] — trigger: "conditional: if touching CoreTestCase"
├── MOVED [file:line] docs/old-guide.md → docs/guides/old-guide.md
├── ORPHAN [file] [N lines] — not linked from any entry point
└── DUPLICATE [file-a] ↔ [file-b] — same content at two paths
```

## Token Estimate

Cap and bytes-per-token ratio: `docs/context-budget.md` — SINGLE
SOURCE, BANNED to restate or re-derive here. `chars / 4` was the old guess and
is WRONG for terse rule prose: it understated the real corpus by 1.65x, so the
cap passed while the corpus sat well over it.

Verdict comes from `CLAUDE_PROJECT_DIR=<repo> python3 scripts/context-check.py`,
which reads those constants and sums the global AND project always-loaded pools.

## Realistic Task Load Example

```text
Task: "write a manual test"
  Always-loaded:   agents.md            ~4,356 tokens
  JIT step 1:      manual-test.md       ~3,980 tokens
  JIT conditional: architecture.md      ~3,031 tokens  (if creating new page object)
  Total realistic: ~11,368 tokens = 5.7% of 200k ✓

  Risk: ambiguous task ("write a test") → agent loads BOTH auto-test + manual-test
  Double-load:     ~15,348 tokens = 7.7% — flag this trigger ambiguity
```

## Ambiguity Risk Flag

If two sibling docs cover overlapping task types and the routing trigger in the
entry point is not mutually exclusive (e.g. "Writing automated UI tests?" and
"Writing manual test cases?" — what if someone says just "write a test?"), flag
it as a ROUTING-AMBIGUITY finding.
