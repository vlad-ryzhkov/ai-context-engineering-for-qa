# Security Scanner False-Negative Anti-Patterns

Three common ways a regex-based secret/exfil scanner gives a false sense of
safety: substring exclusions that match partial placeholders, case-restricted
character classes, and extension filters that skip binaries.

## 1. Substring Placeholder-Exclude

### Anti-Pattern: `grep -iE 'your_|example_|placeholder'` as a post-filter

Post-filtering assignment-pattern matches with an unanchored placeholder
regex lets secrets through whenever a placeholder prefix happens to appear
anywhere in the line.

**Signal:** `SECURITY_PLACEHOLDER_EXCLUDE='(your_|example_|<[a-z]|placeholder|...)'`
used as `grep -iE "$exclude"` on the whole match line.

```bash
# BAD: `your_` is a substring → whole line skipped
SECURITY_PLACEHOLDER_EXCLUDE='(your_|example_|<[a-z]|placeholder|\$\{)'

scan_line() {
  local line="$1"
  echo "$line" | grep -qiE "$SECURITY_PLACEHOLDER_EXCLUDE" && return 0
  report_finding "$line"
}

# All of these are skipped — only the first is a legit placeholder:
scan_line 'password=your_password'         # OK to skip (placeholder)
scan_line 'password=your_real_secret_123'  # LEAK — `your_` substring → skipped
scan_line 'note: copy your_key into .env'  # FP — skipped too (acceptable FP)
```

### Fix: Extract the RHS and compare against a strict whitelist

Pull the value after `=`, strip quotes, then check against explicit
placeholder shapes. Real secrets (high-entropy, digits, mixed case) fall
through to the real check.

```bash
# Return 0 if the assigned value is a documentation placeholder.
is_placeholder_rhs() {
  local line="$1" rhs
  rhs=$(echo "$line" | sed -E \
    -e 's/^[0-9]+://' \
    -e 's/^[^=]*=[[:space:]]*//' \
    -e 's/^["'"'"']//' \
    -e 's/["'"'"'[:space:]].*$//')
  [[ -z "$rhs" ]] && return 1

  case "$rhs" in
    your_*|example_*|my_*|dummy_*|test_*|sample_*)
      # Tail must be lowercase+underscore only, ≤30 chars — digits or
      # mixed case imply a real secret hiding behind a placeholder prefix.
      [[ "$rhs" =~ ^[a-z_]+$ ]] && [[ ${#rhs} -le 30 ]] && return 0
      return 1 ;;
    placeholder|changeme|xxx|xxxxxx|TODO|FIXME|foo|bar) return 0 ;;
    '<'*) return 0 ;;       # <apikey>
    '$'*) return 0 ;;       # ${VAR} / $VAR
    *) return 1 ;;
  esac
}
```

The anti-pattern generalizes: any allowlist post-filter on a scanner should
be strict (exact or strictly-anchored) rather than substring.

## 2. Case-Restricted Character Classes in Security Regex

### Anti-Pattern: Pattern scans only `[A-Z_]` variable names

Exfiltration regexes that target env-var interpolation (`$TOKEN`, `$API_KEY`)
often restrict the character class to uppercase. Real scripts use lowercase
(`$api_token`, `$secret`) freely — they pass the scanner untouched.

**Signal:** security patterns containing `[A-Z_]`, `[A-Z]+`, or
`\$\{?[A-Z_]` without case-insensitivity.

```bash
# BAD: only catches $AWS_ACCESS_KEY, misses $aws_access_key
SECURITY_EXFIL=(
  'curl.*\$\{?(AWS|SECRET|TOKEN|KEY|PASS|CRED)[A-Z_]*'
  'curl\s+.*-H\s+.*\$\{?(AUTH|TOKEN|KEY|SECRET|CRED)[A-Z_]*'
)
grep -inE "${SECURITY_EXFIL[0]}" scripts/  # silently skips `curl -d "$api_token"`
```

### Fix: Explicitly allow both cases

Use `[A-Za-z_]` and keep keywords in lowercase (grep already runs with `-i`
in most scanner harnesses, but making the class explicit documents intent
and survives copy-paste into case-sensitive grep calls).

```bash
# GOOD: catches $AWS_*, $aws_*, $Aws_*
SECURITY_EXFIL=(
  'curl.*\$\{?(aws|secret|token|key|pass|cred|api|auth)[A-Za-z_]*'
  'curl\s+.*-H\s+.*\$\{?(auth|token|key|secret|cred)[A-Za-z_]*'
)
```

### Why `grep -i` alone is not enough

Even with `grep -i`, a character class like `[A-Z_]` only matches its
literal members — `-i` makes the match case-insensitive at the literal
level, not at the class level in all grep implementations. Make the class
explicit.

## 3. Text-Extension Filter Misses Binary Files

### Anti-Pattern: Scan only `*.md|*.sh|*.json|*.yaml|*.txt`

Secret scanners that `find -name '*.md' -o -name '*.sh' -o ...` skip every
file without one of those extensions. A committed `.wasm`, `.bin`, `.pfx`,
`id_rsa`, or an extension-less config file walks past untouched.

**Signal:** `find "$dir" -type f \( -name '*.md' -o -name '*.sh' -o ... \)`
wrapping a secret-pattern grep.

```bash
# BAD: narrow extension filter
text_files=$(find "$dir" -type f \( \
  -name '*.md' -o -name '*.sh' -o -name '*.py' \
  -o -name '*.yaml' -o -name '*.yml' -o -name '*.json' \
  -o -name '*.txt' -o -name '*.toml' \) 2>/dev/null)
scan_for_secrets "$text_files"
# → secret in references/keys.pfx or scripts/id_rsa walks through.
```

### Fix: Two-layered approach

1. **Forbidden-extension blacklist** (P0, unconditional) — reject known
   secret-shaped files by name/extension without scanning their content.

2. **Opportunistic text detection** — use `file --mime-type` or a text-char
   heuristic to decide whether to grep a file of unknown extension.

```bash
# Layer 1: forbid secret-shaped filenames outright.
SECURITY_FORBIDDEN_EXTENSIONS=(
  '\.env$' '\.pem$' '\.p12$' '\.pfx$' '\.key$' '\.keystore$' '\.jks$' '\.asc$'
  'id_rsa' 'id_ed25519' 'id_ecdsa'
)

# Layer 2: detect text files by content, not extension.
scan_all_text_files() {
  local dir="$1"
  while IFS= read -r file; do
    if file --mime-type -b "$file" 2>/dev/null | grep -q '^text/'; then
      scan_for_secrets "$file"
    fi
  done < <(find "$dir" -type f)
}
```

Multi-line secrets split across lines (`password = \` → next line) are still
out of reach for a grep-based scanner; document this explicitly in the scan
function's comment so a reader doesn't assume coverage. Catching them
requires an AST-aware or context-window AI reviewer.
