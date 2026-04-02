# Don't use arithmetic without guarding inputs

## Why this is bad

Bash arithmetic (`$(( ))`, `let`, `(( ))`) treats empty or non-numeric values as 0 silently — or causes fatal errors under `set -u`. Division by zero crashes the script. Unvalidated `wc -l` output piped into arithmetic can produce wrong results when files are empty or missing. These bugs are hard to reproduce and often only surface in edge cases in production.

## Bad Example

```bash
# ❌ BAD: division by zero if total is 0
percentage=$((count * 100 / total))

# ❌ BAD: wc -l on empty/missing file returns 0 or errors
lines=$(wc -l < "$file")
avg=$((total / lines))

# ❌ BAD: unset variable in arithmetic under set -u
set -u
result=$((value + 1))  # crashes if value is unset
```

## Good Example

```bash
# ✅ GOOD: guard against division by zero
if [[ "$total" -gt 0 ]]; then
  percentage=$((count * 100 / total))
else
  percentage=0
fi

# ✅ GOOD: default value for potentially empty variable
lines=$(wc -l < "$file" 2>/dev/null || echo "0")
lines="${lines// /}"  # trim whitespace from wc output
if [[ "$lines" -gt 0 ]]; then
  avg=$((total / lines))
else
  avg=0
fi

# ✅ GOOD: provide default for unset variables in arithmetic
result=$(( ${value:-0} + 1 ))
```

## What to look for in code review

- Any division (`/`) in `$(( ))` without checking the divisor is non-zero
- `wc -l` output used directly in arithmetic without validation
- Arithmetic expressions with variables that could be empty or unset
- Missing default values (`${var:-0}`) for numeric variables
