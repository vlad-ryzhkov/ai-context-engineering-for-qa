# Don't ignore set -euo pipefail edge cases

## Why this is bad

`set -euo pipefail` is a good defensive default, but each flag has edge cases that cause surprising script failures. `set -u` crashes on empty arrays in bash < 4.4. `set -e` does not trigger inside `if` conditions or `||`/`&&` chains. `pipefail` causes failures from intermediate pipe stages that are intentionally non-zero (like `grep` returning 1 for no match). Misunderstanding these leads to either false security or spurious crashes.

## Bad Example

```bash
# ❌ BAD: empty array crashes under set -u (bash < 4.4)
set -u
files=()
echo "${files[@]}"  # unbound variable error

# ❌ BAD: grep no-match kills script under pipefail
set -eo pipefail
count=$(cat file.txt | grep "pattern" | wc -l)  # exits if pattern not found

# ❌ BAD: set -e doesn't catch this failure
set -e
if failing_command; then  # set -e is disabled inside if condition
  subsequent_command       # runs even if failing_command... fails
fi

# ❌ BAD: || true suppresses ALL errors, not just expected ones
set -e
risky_command || true  # hides real errors too
```

## Good Example

```bash
# ✅ GOOD: guard empty arrays for compatibility with bash < 4.4
set -u
files=()
if [[ ${#files[@]} -gt 0 ]]; then
  echo "${files[@]}"
fi

# ✅ GOOD: alternative empty-array-safe expansion
echo ${files[@]+"${files[@]}"}

# ✅ GOOD: handle grep no-match explicitly
count=$(grep -c "pattern" file.txt) || count=0

# ✅ GOOD: capture exit code instead of || true
set -e
rc=0
risky_command || rc=$?
if [[ "$rc" -ne 0 ]]; then
  echo "risky_command failed with exit code $rc" >&2
fi

# ✅ GOOD: use process substitution to avoid pipefail issues
while IFS= read -r line; do
  process "$line"
done < <(grep "pattern" file.txt || true)
```

## What to look for in code review

- Empty arrays accessed under `set -u` without length check or `${arr[@]+"${arr[@]}"}`
- `grep` in a pipeline under `set -eo pipefail` without handling the no-match case
- `|| true` used as a blanket error suppressor — prefer capturing `$?` for specific handling
- Assumptions that `set -e` catches errors inside `if`, `while`, or `&&`/`||` chains
- Missing `pipefail` when pipe output is used for decisions (silently drops upstream errors)
