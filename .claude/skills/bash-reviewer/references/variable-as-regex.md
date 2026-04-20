# Variable Content as Regex Anti-Pattern

## grep with Variable as Pattern

### Anti-Pattern: Using variable content as a regex pattern

When a variable is used directly in `grep` without `-F`, its content is treated as a regular expression. If the value contains regex metacharacters (`.`, `*`, `[`, `]`, `^`, `$`, `+`, `?`, `{`, `}`), the match may be incorrect or grep may error out.

**Signal:** `grep "$var"` or `grep "^${var}="` where the variable comes from user input, file content, or YAML parsing.

```bash
# BAD: name_field is treated as regex
name_field=$(yq '.name' "$file")
# If name_field is "test.skill" → the dot matches any char
# If name_field is "foo[bar]" → invalid regex, grep errors
existing=$(grep "^${name_field}=" "$map_file")
```

**Fix:** Use `grep -F` (fixed string) to treat the pattern as a literal:

```bash
# GOOD: -F treats pattern as literal string, not regex
existing=$(grep -F "${name_field}=" "$map_file" | grep -F -m1 "${name_field}=" | cut -d= -f2)
```

For patterns that need anchoring (start-of-line), use `awk` instead:

```bash
# GOOD: awk with string comparison, no regex
existing=$(awk -F= -v name="$name_field" '$1 == name {print $2; exit}' "$map_file")
```

### Related: sed with variable content

Same issue applies to `sed` when interpolating variables into patterns:

```bash
# BAD: var content interpreted as regex in sed
sed -n "/${var}/p" "$file"

# GOOD: use awk for literal matching
awk -v pat="$var" 'index($0, pat)' "$file"
```

## Relative Symlinks Across Git Boundaries

### Anti-Pattern: Relative symlinks in git hooks

Git hooks installed via `ln -s ../../path` break in git worktrees because the git directory structure differs:

- Normal repo: `.git/hooks/` → `../../` reaches repo root
- Worktree: `.git/worktrees/<name>/hooks/` → `../../` reaches `.git/worktrees/`, not repo root

**Signal:** `ln -s` with `../` in hook installation scripts.

```bash
# BAD: relative path breaks in worktrees
GIT_DIR=$(git rev-parse --git-dir)
ln -s "../../scripts/hooks/pre-commit.sh" "${GIT_DIR}/hooks/pre-commit"
```

**Fix:** Use absolute paths for symlink targets:

```bash
# GOOD: absolute path works everywhere
REPO_ROOT="$(git rev-parse --show-toplevel)"
GIT_DIR="$(git rev-parse --git-dir)"
ln -s "${REPO_ROOT}/scripts/hooks/pre-commit.sh" "${GIT_DIR}/hooks/pre-commit"
```

## sed-Based Structured Data Parsing

### Anti-Pattern: Using sed to parse YAML/JSON

Parsing YAML frontmatter with `sed -n '/^---$/,/^---$/p'` breaks when:

- The frontmatter contains multiline strings with `---` inside them
- There are `---` in code blocks within the file body
- The file has unusual whitespace around delimiters

**Signal:** `sed -n '/^---$/,/^---$/p'` followed by `grep` for field extraction.

```bash
# BAD: fragile sed-based YAML parsing
agent=$(sed -n '/^---$/,/^---$/p' "$file" | grep '^agent:' | sed 's/^agent://')
```

**Fix:** Use `yq` with native frontmatter support (if available as a dependency):

```bash
# GOOD: yq handles all YAML edge cases correctly
agent=$(yq --front-matter=extract '.agent // ""' "$file" 2>/dev/null || true)
```

If yq is not available, document the limitation and use a more robust awk approach:

```bash
# ACCEPTABLE: awk-based extraction (handles most cases)
agent=$(awk '/^---$/{n++; next} n==1 && /^agent:/{sub(/^agent:[[:space:]]*/, ""); print; exit}' "$file")
```

## Suppression DSL Without a Critical-Check Allowlist

### Anti-Pattern: `<!-- nolint:... -->` directives that can silence P0 scanners

Any linter that accepts an inline suppression directive (`<!-- nolint:X -->`,
`# noqa: X`, `// eslint-disable X`, etc.) is a trust boundary: anyone with
commit rights to a file can disable a check by adding a comment. If the
dispatch treats every check name equally, a PR can legally land a commit
that turns off the secret scanner.

**Signal:** `if is_suppressed "$check_name"; then return 0; fi` at the top
of the dispatch, with no filter on which checks are suppressible.

```bash
# BAD: any check can be suppressed, including security.
# Commit message: "chore: fix false positive in docs"
# Actual diff: `+<!-- nolint:security -->` in SKILL.md
#              `+AKIAIOSFODNN7EXAMPLE000000000000000000` in same file
run_check() {
  local check_name="$1"
  if is_nolint "$nolint_list" "$check_name"; then
    log_pass "$check_name (nolint)"   # silent, no audit trail
    return 0
  fi
  "$@"
}
```

Two failures compound:

1. **No allowlist of unsuppressible checks** — secret scanning is now
   opt-out with one line.
2. **No audit log** — the suppressed check prints as PASS, indistinguishable
   from a real pass. Reviewers reading CI output won't notice.

### Fix: Deny-list the checks that must always run; log every suppression

Maintain a tight allow-list of checks that cannot be suppressed. When a PR
tries to suppress one, fail the check AND still run the real scanner — so
both the intent and (if present) the actual secret are surfaced. For
suppressible checks, log the suppression with the file path.

```bash
is_never_suppressible() {
  case "$1" in
    security) return 0 ;;   # secret scanning — never suppressible
    *) return 1 ;;
  esac
}

run_check() {
  local check_name="$1" severity="$2" nolint_list="$3" rel_path="$4"
  shift 4

  local forced_fail=0
  if is_nolint "$nolint_list" "$check_name"; then
    if is_never_suppressible "$check_name"; then
      log_fail "$check_name" "nolint is not allowed for this check (ignored)"
      forced_fail=1
      # Fall through — the real check still runs, so an actual secret in
      # the same file is also reported. A malicious PR gets two FAIL lines.
    else
      log_warn "$check_name" "SUPPRESSED via nolint in $rel_path"
      return 0
    fi
  fi

  if "$@" && [[ $forced_fail -eq 0 ]]; then
    return 0
  fi
  [[ "$severity" == "p0" || $forced_fail -eq 1 ]] && return 2
  return 1
}
```

### Generalizes beyond `nolint`

Same pattern applies to:

- `# type: ignore` / `mypy: disable-error-code` for type checkers
- `// eslint-disable-next-line` for ESLint
- `@SuppressWarnings` in Java
- Custom `skip-ci: <job>` directives in YAML headers

Every one of them needs a hardcoded list of unsuppressible checks (security,
data-loss prevention, license compliance). The allowlist lives in the tool,
not in the config, so a repo-level config change can't disable it.
