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
