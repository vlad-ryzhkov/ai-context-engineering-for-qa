# Don't use platform-specific tool flags

## Why this is bad

macOS ships BSD versions of `sed`, `grep`, `find`, and other core utilities. Linux uses GNU versions. Scripts that rely on GNU-only or BSD-only flags break silently or with cryptic errors on the other platform. CI environments (usually Linux) will fail on macOS-specific syntax and vice versa.

## Bad Example

```bash
# ❌ BAD: sed -i '' is macOS-only; GNU sed requires sed -i (no argument)
sed -i '' 's/foo/bar/' file.txt

# ❌ BAD: sed -i without argument is GNU-only; macOS sed treats next arg as backup suffix
sed -i 's/foo/bar/' file.txt

# ❌ BAD: grep -P (Perl regex) is GNU-only, not available on macOS
grep -P '\d{3}-\d{4}' file.txt

# ❌ BAD: readarray/mapfile not available in bash 3.x (macOS default)
readarray -t lines < file.txt
```

## Good Example

```bash
# ✅ GOOD: portable in-place sed using temp file
sed 's/foo/bar/' file.txt > file.txt.tmp && mv file.txt.tmp file.txt

# ✅ GOOD: detect platform for sed -i
if sed --version 2>/dev/null | grep -q 'GNU'; then
  sed -i 's/foo/bar/' file.txt
else
  sed -i '' 's/foo/bar/' file.txt
fi

# ✅ GOOD: use grep -E (POSIX extended) instead of grep -P
grep -E '[0-9]{3}-[0-9]{4}' file.txt

# ✅ GOOD: portable alternative to readarray for bash 3.x
lines=()
while IFS= read -r line; do
  lines+=("$line")
done < file.txt
```

## What to look for in code review

- `sed -i` without platform detection — neither `sed -i ''` nor `sed -i` is portable alone
- `grep -P` — use `grep -E` for extended regex or `grep -oE` for extraction
- `readarray` or `mapfile` — not available in bash 3.x (macOS default)
- `find -regex` type differences between GNU and BSD find
- `date` format differences (`date -d` vs `date -j -f`)
