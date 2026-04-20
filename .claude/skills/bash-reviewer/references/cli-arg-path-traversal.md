# CLI Argument Path Traversal Anti-Pattern

## User-Supplied CLI Arg Concatenated Into Filesystem Path

### Anti-Pattern: Unvalidated `--flag <value>` used in path construction

When a CLI flag value is pasted directly into a path (`${BASE}/${ARG}` / `find`
/ `read`), an attacker (or fat-fingered user) can read or scan arbitrary
filesystem locations by passing `..`, an absolute path, or `~`.

**Signal:** `case "$1" in --skill|--path|--file|--dir) VAR="$2"; ...` followed
somewhere by `"${BASE_DIR}/${VAR}"`, `find "$BASE/$VAR"`, `cat "$BASE/$VAR"`,
or similar, with no validation in between.

```bash
# BAD: --skill value is pasted into a find root with no guard
SKILL_FILTER=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    *) shift ;;
  esac
done

# Attacker: `bash lint.sh --skill ../../../etc`
# → find walks /etc and every file.txt gets secret-scanned / read.
find "${SKILLS_DIR}/${SKILL_FILTER}" -type f | while read -r f; do
  scan "$f"
done
```

No code is executed, but:

- Arbitrary filesystem read (sensitive files enumerated, small ones printed
  in error output).
- CI logs can exfiltrate contents unintentionally.
- If the tool later adds `rm`, `chmod`, or `cp` on matched files, the blast
  radius grows.

### Fix: Validate allowed characters OR reject traversal primitives

Reject any value containing `..`, starting with `/` (absolute), or starting
with `~` (home expansion). For simple names, a whitelist regex is stricter
and better. Put the guard in a shared lib sourced by every entry point that
accepts the flag.

```bash
# scripts/lib/validate-skill-filter.sh — sourced from every entry point.
validate_skill_filter() {
  local filter="$1"
  [[ -z "$filter" ]] && return 0
  if [[ "$filter" == *".."* ]] || [[ "$filter" == /* ]] || [[ "$filter" == ~* ]]; then
    echo "ERROR: invalid --skill value: '${filter}' (no '..', absolute, or '~' paths)" >&2
    exit 1
  fi
}
```

```bash
# In lint.sh:
source "$SCRIPT_DIR/lib/validate-skill-filter.sh"
# ...parse args...
validate_skill_filter "$SKILL_FILTER"
```

### Stricter alternative: whitelist regex

If the flag only ever takes a simple identifier (folder name, skill name),
constrain it with a regex instead of a blacklist:

```bash
if ! [[ "$SKILL_FILTER" =~ ^[a-z0-9][a-z0-9_-]*$ ]]; then
  echo "ERROR: --skill must match [a-z0-9][a-z0-9_-]*" >&2
  exit 1
fi
```

Blacklist is required only when legitimate values include slashes (e.g.
`.test-fixtures/foo`). Prefer whitelist when they don't.

### Why not just `[[ -d "$path" ]]`?

A dir-exists check stops traversal to non-existent paths but still lets the
attacker probe for known dirs (`/etc`, `/var/log`, `$HOME/.ssh`). The guard
must reject the shape of the input, not just its existence.

### Related: env vars, positional args, stdin

The same rule applies to any untrusted string that ends up in a path: `$1`,
`$LOOKUP_DIR` from env, values read from config files, or lines piped from
`git diff`. Validate at the boundary, not at the call site.
