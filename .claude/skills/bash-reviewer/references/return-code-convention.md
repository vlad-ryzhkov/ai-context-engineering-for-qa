# Return Code Convention Anti-Patterns

## Non-Standard Return Codes

### Anti-Pattern: Inverted return convention (0=fail, non-zero=pass)

Some scripts define check functions where `return 0` means failure and `return 1` means success — the opposite of UNIX convention. This creates confusion because:

- Every bash developer expects 0=success
- `set -e` and `if cmd; then` treat 0 as success
- Contributors will misread the logic without studying the convention comment

**Signal:** Comment blocks like "return 0 = FAIL" or functions that `return 0` after error logging.

```bash
# BAD: inverted convention
check_something() {
  if [[ -z "$value" ]]; then
    echo "FAIL: value is empty"
    return 0  # Looks like success but means failure
  fi
  echo "PASS"
  return 1  # Looks like failure but means success
}
```

**Fix:** Use standard UNIX convention — 0 for success, non-zero for failure:

```bash
# GOOD: standard UNIX convention
check_something() {
  if [[ -z "$value" ]]; then
    echo "FAIL: value is empty"
    return 1
  fi
  echo "PASS"
  return 0
}
```

Then callers use the natural pattern:

```bash
if check_something; then
  echo "passed"
else
  ((failures++))
fi
```

## set -e in Informational Scripts

### Anti-Pattern: `set -e` in scripts that should always exit 0

Scripts designed to report warnings (not block CI) should never use `set -e`. If any internal command fails (e.g., `wc` on a missing file, `grep` with no matches), `set -e` aborts the entire script — hiding useful warnings from the user.

**Signal:** `set -euo pipefail` at the top of a script that ends with `exit 0` and has comments like "WARN-only" or "never blocks CI".

```bash
# BAD: set -e in informational script
set -euo pipefail
# ... various checks that print warnings ...
echo "Results: ${WARNINGS} warning(s)"
exit 0  # This line may never execute if a command above fails
```

**Fix:** Use `set -uo pipefail` (without `-e`). Handle errors explicitly where needed:

```bash
# GOOD: no set -e, explicit error handling
set -uo pipefail

count=$(grep -c "pattern" "$file" 2>/dev/null) || count=0
# Script continues even if grep finds nothing
```
