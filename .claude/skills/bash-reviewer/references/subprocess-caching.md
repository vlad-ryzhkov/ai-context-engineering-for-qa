# Subprocess Caching Anti-Pattern

## External Binary Called Multiple Times with Same Input

### Anti-Pattern: Forking expensive binaries (yq, jq, aws, kubectl) repeatedly

Each `$(yq ...)`, `$(jq ...)`, or `$(aws ...)` call forks a new process. On macOS,
Go binaries like yq take ~100-250ms per fork. Calling the same binary 10+ times per
iteration in a loop creates measurable slowdowns (seconds per item).

**Signal:** `$(yq ...)` or `$(jq ...)` appearing multiple times with the same source
file/input, especially inside a `while` or `for` loop.

```bash
# BAD: 5 yq calls per skill, each forks a new process (~1 sec per skill)
for skill_dir in skills/*/; do
  yaml=$(yq --front-matter=extract '.' "$skill_dir/SKILL.md")
  name=$(printf '%s\n' "$yaml" | yq -r '.name')
  desc=$(printf '%s\n' "$yaml" | yq -r '.description')
  agent=$(printf '%s\n' "$yaml" | yq -r '.agent')
  tools=$(printf '%s\n' "$yaml" | yq -r '.["allowed-tools"]')
  # ... validate each field
done
```

**Fix:** Extract all needed fields in one call, or extract raw YAML once and pass it:

```bash
# GOOD: 1 yq call for frontmatter, 1 for all fields (tab-separated)
for skill_dir in skills/*/; do
  yaml=$(yq --front-matter=extract '.' "$skill_dir/SKILL.md") || continue
  name=$(printf '%s\n' "$yaml" | yq -r '.name // ""')
  desc=$(printf '%s\n' "$yaml" | yq -r '.description // ""')
  # Pass cached values to functions instead of re-extracting
  validate_name "$name"
  validate_description "$desc"
done
```

```bash
# BEST: Extract multiple fields in one yq call using JSON output
fields=$(yq --front-matter=extract -o json '{"name": .name, "desc": .description}' "$file")
name=$(printf '%s' "$fields" | yq -r '.name')
desc=$(printf '%s' "$fields" | yq -r '.desc')
```

### Same Pattern with Other Tools

```bash
# BAD: aws CLI called in a loop (network + process overhead per call)
for bucket in "${buckets[@]}"; do
  size=$(aws s3 ls "s3://$bucket" --summarize | grep "Total Size")
  count=$(aws s3 ls "s3://$bucket" --summarize | grep "Total Objects")
done

# GOOD: one call, parse locally
for bucket in "${buckets[@]}"; do
  summary=$(aws s3 ls "s3://$bucket" --summarize)
  size=$(echo "$summary" | grep "Total Size")
  count=$(echo "$summary" | grep "Total Objects")
done
```

### How to Detect

Look for:

1. `$(yq ...)` or `$(jq ...)` appearing 3+ times with the same source file
2. External binary calls inside `while`/`for` loops that could be hoisted
3. Functions that extract the same data their caller already extracted
4. Multiple `extract_field "$yaml" "fieldname"` calls that each fork yq

### Impact

Real example: 8 AI skills × 13 yq calls per skill = 104 forks × ~0.1s = **~10 seconds**.
After caching: 8 × 3 calls = 24 forks = **~2.5 seconds** (4x faster).
