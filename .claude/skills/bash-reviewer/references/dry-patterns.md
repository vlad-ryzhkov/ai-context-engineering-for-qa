# Don't repeat check/counter/scan blocks

## Why this is bad

Bash scripts that validate multiple conditions often repeat the same check-invoke-and-count pattern dozens of times. Each repetition is 4-6 lines of boilerplate that obscures the actual check list, inflates the file, and makes it easy to introduce inconsistencies (e.g. forgetting to set a failure flag for one check but not another).

Common duplicated patterns:

- **Check + counter block**: call function, capture `$?`, increment counter, set flag
- **Scan loop**: iterate files, grep for pattern, append to findings array
- **Format + counter**: build formatted output string and increment counter

## Bad Example

```bash
# ❌ BAD: 14 repetitions of the same 5-line block
check_yaml "$skill_dir" "$yaml_content"
check_result=$?
if [[ $check_result -ne 0 ]]; then
    failure_count=$((failure_count + 1))
    has_p0_failure=1
fi

check_required_fields "$name_field" "$desc_field"
check_result=$?
if [[ $check_result -ne 0 ]]; then
    failure_count=$((failure_count + 1))
    has_p0_failure=1
fi
# ... 12 more identical blocks

# ❌ BAD: same scan loop repeated 4 times with only the label different
for pattern in "${TOKEN_PATTERNS[@]}"; do
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    local line_content
    line_content=$(grep -inE "$pattern" "$file" 2>/dev/null | head -1 || true)
    if [[ -n "$line_content" ]]; then
      issues+=("Secret pattern in ${file#"$dir"/}: $line_content")
    fi
  done <<< "$file_list"
done
# ... same loop for obfuscation patterns, exfiltration patterns, etc.

# ❌ BAD: same format+counter repeated 14 times
findings="${findings}\n  ${CYAN}SUGGEST${NC}   [S8] ${count} lines"
((SUGGESTIONS++)) || true
# ... same pattern for S10, S15, S16, S19, S20, S21, S22, S23, S24, S25, S26, S27, S28
```

## Good Example

```bash
# ✅ GOOD: helper uses "$@" to forward arguments and bash shared scope for counters
# Severity as parameter — one function handles both blocking and non-blocking checks
run_check() {
  local check_name="$1" severity="$2"
  shift 2
  if ! "$@"; then
    failure_count=$((failure_count + 1))
    [[ "$severity" == "p0" ]] && has_p0_failure=1
  fi
}

# 14 checks become 14 single-line calls
run_check "yaml" p0 check_yaml "$skill_dir" "$yaml_content"
run_check "required-fields" p0 check_required_fields "$name_field" "$desc_field"
run_check "no-readme" warn check_no_readme "$skill_dir"

# ✅ GOOD: parameterized scan replaces 4 identical loops
# Args: label file_list exclude_pattern pattern1 [pattern2 ...]
scan_files_for_patterns() {
  local label="$1" file_list="$2" exclude="$3"
  shift 3
  for pattern in "$@"; do
    while IFS= read -r file; do
      [[ -z "$file" ]] && continue
      local line_content
      line_content=$(grep -inE "$pattern" "$file" 2>/dev/null | head -1 || true)
      if [[ -n "$line_content" ]]; then
        if [[ -n "$exclude" ]] && echo "$line_content" | grep -qiE "$exclude"; then
          continue
        fi
        issues+=("${label} ${file#"$skill_dir"/}: $line_content")
      fi
    done <<< "$file_list"
  done
}

scan_files_for_patterns "Secret pattern in" "$files" "" "${TOKEN_PATTERNS[@]}"
scan_files_for_patterns "Secret pattern in" "$files" "$EXCLUDE" "${ASSIGNMENT_PATTERNS[@]}"
scan_files_for_patterns "Obfuscation in" "$files" "" "${OBFUSCATION_PATTERNS[@]}"

# ✅ GOOD: format+counter helper eliminates 14 two-line blocks
suggest() {
  findings="${findings}\n  ${CYAN}SUGGEST${NC}   [$1] $2"
  ((SUGGESTIONS++)) || true
}

suggest "S8" "${count} lines (limit: 500)"
suggest "S10" "Missing Quality Gate section"
```

## Key Mechanism: Bash Shared Scope

These helpers work because bash functions share the caller's variable scope (no subshell). `failure_count`, `has_p0_failure`, `findings`, `issues` are modified directly in the calling function's scope. This is safe as long as:

- The helper is called directly (not in a pipe or `$(...)` subshell)
- Variables are not declared `local` inside the helper if they belong to the caller

## What to look for in code review

- 3+ identical blocks that differ only in function name, label, or arguments
- `check_result=$?` followed by `if [[ $check_result -ne 0 ]]` — use `if ! func; then` directly
- Multiple `for pattern in "${ARRAY[@]}"` loops with the same scan body
- Repeated `string="${string}..."` + `((counter++))` pairs — extract a one-line helper
- When extracting: prefer severity/mode as a parameter over separate wrapper functions
