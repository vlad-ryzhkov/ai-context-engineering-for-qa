# Don't mix find operators without explicit grouping

## Why this is bad

`find` evaluates `-o` (OR) with lower precedence than implicit AND. Without explicit parentheses, `-name "*.sh" -o -name "*.bash" -type f` does not do what it looks like — the `-type f` only applies to the second `-name`, not both. This leads to silently wrong file discovery, missed files, or processing unexpected file types.

## Bad Example

```bash
# ❌ BAD: -type f only applies to the -name "*.bash" branch
find . -name "*.sh" -o -name "*.bash" -type f

# ❌ BAD: -print only fires for the second condition
find . -name "*.log" -o -name "*.tmp" -print

# ❌ BAD: action applies only to last branch
find . -name "*.o" -o -name "*.a" -exec rm {} \;
```

## Good Example

```bash
# ✅ GOOD: explicit grouping with escaped parentheses
find . \( -name "*.sh" -o -name "*.bash" \) -type f

# ✅ GOOD: action applies to entire group
find . \( -name "*.log" -o -name "*.tmp" \) -print

# ✅ GOOD: delete applies to both patterns
find . \( -name "*.o" -o -name "*.a" \) -exec rm {} \;
```

## What to look for in code review

- Any `find` command with `-o` that lacks `\(` and `\)` grouping
- Actions (`-print`, `-exec`, `-delete`) after an `-o` without parentheses
- Multiple `-name` or `-path` conditions joined with `-o`
