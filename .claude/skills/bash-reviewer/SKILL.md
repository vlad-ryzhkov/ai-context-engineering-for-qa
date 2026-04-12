---
name: bash-reviewer
description: >-
  Reviews shell scripts (.sh) for security, portability, and robustness anti-patterns.
  Provides structured fixes citing reference documentation.
  Use when reviewing bash/shell scripts or when asking to check shell script quality.
  Do not use for Python/Ruby/Go scripts, GitHub Actions YAML, or issues already caught by shellcheck.
allowed-tools: "Read Glob Grep"
context: fork
---

# /bash-reviewer — Shell Script Anti-Pattern Reviewer

<purpose>
Reviews shell scripts for security vulnerabilities, cross-platform portability issues,
and robustness anti-patterns that static analysis tools like shellcheck cannot detect.
Produces a structured report with fixes sourced from reference documentation.
</purpose>

> **SILENT MODE**: Execute all analytical phases silently. Do not output intermediate
> reasoning, progress updates, or conversational filler. Output ONLY the structured
> issue blocks and summary. Exception: if no issues found, output a single line:
> `✅ No anti-patterns detected`

---

## When to Use

- User asks to review, audit, or check shell scripts for quality
- Before merging PRs that add or modify `.sh` files
- When diagnosing flaky or platform-dependent script failures
- When hardening scripts that run in CI/CD pipelines

## When NOT to Use

- Python, Ruby, Go, or other non-shell scripts
- GitHub Actions YAML — use `/workflow-expert` instead
- Issues already caught by shellcheck (quoting, SC2068, SC2086)
- One-liner shell commands in Makefiles or Dockerfiles

---

## Scope

**DO review:**

- `.sh` and `.bash` files
- Security: eval injection, unvalidated command expansion
- Portability: sed -i, grep -P, readarray/mapfile, find -o precedence
- Robustness: division by zero, wc in arithmetic, missing prerequisite checks, set -euo pitfalls
- DRY: repeated check/counter blocks, duplicated scan loops, shared constants across files

**DON'T review:**

- Style preferences (indentation, naming conventions)
- ShellCheck-detectable issues (quoting, word splitting)
- Logic correctness of the script's business domain

---

## Detection Workflow

### Step 1: Discover Scripts

Use `Glob` to find all `.sh` and `.bash` files in the target path.
If user specified a single file, skip discovery.

### Step 2: Scan for Anti-Patterns

For each script, use `Grep` to scan for pattern signatures:

| Signal                        | Grep Pattern                          | Category    |
| ----------------------------- | ------------------------------------- | ----------- |
| `eval`                        | `eval\s`                              | Security    |
| `$command` used as execution  | `^\s*\$\w+`                           | Security    |
| `sed -i`                      | `sed\s+-i`                            | Portability |
| `grep -P`                     | `grep\s+.*-P`                         | Portability |
| `readarray` / `mapfile`       | `readarray\|mapfile`                  | Portability |
| `find` with `-o`              | `find\s.*-o\s`                        | Portability |
| Division in arithmetic        | `\$((.*/.*)`                          | Robustness  |
| `wc -l` in arithmetic context | `wc\s+-l`                             | Robustness  |
| `set -.*u` with arrays        | `set\s+-.*u`                          | Robustness  |
| `pipefail` with grep          | `pipefail`                            | Robustness  |
| Tool usage without guard      | `jq\|yq\|shellcheck\|docker\|kubectl` | Robustness  |
| Inverted return codes         | `return 0.*fail\|return 0.*error`     | Robustness  |
| `set -e` in WARN-only script  | `set -.*e.*pipefail`                  | Robustness  |
| Variable as regex in grep     | `grep ".*\$\w\|grep \$\{`             | Security    |
| Relative symlink in hooks     | `ln -s .*\.\./`                       | Robustness  |
| sed for YAML/JSON parsing     | `sed.*---.*---\|sed.*^[a-z]*:`        | Robustness  |
| Repeated check+counter block  | `check_result=\$\?`                   | DRY         |
| Repeated scan/grep loop       | `while.*read.*grep.*done`             | DRY         |
| Repeated format+counter       | `\(\(.*\+\+\)\).*\|\| true`           | DRY         |
| Duplicated constants/utils    | `RED=.*033` in multiple files         | DRY         |
| Missing shellcheck source     | `source.*\.sh`                        | Robustness  |

### Step 3: Read References

For each detected anti-pattern, **READ** the corresponding reference file:

| Category    | Anti-Pattern                           | Reference                                                               |
| ----------- | -------------------------------------- | ----------------------------------------------------------------------- |
| Security    | eval with untrusted input              | [eval-injection.md](references/eval-injection.md)                       |
| Security    | Unvalidated command expansion          | [eval-injection.md](references/eval-injection.md)                       |
| Portability | Platform-specific sed flags            | [portable-sed-and-tools.md](references/portable-sed-and-tools.md)       |
| Portability | grep -P (Perl regex)                   | [portable-sed-and-tools.md](references/portable-sed-and-tools.md)       |
| Portability | readarray/mapfile without bash 4 check | [portable-sed-and-tools.md](references/portable-sed-and-tools.md)       |
| Portability | find -o without grouping               | [find-operator-precedence.md](references/find-operator-precedence.md)   |
| Robustness  | Division without zero check            | [arithmetic-guards.md](references/arithmetic-guards.md)                 |
| Robustness  | wc output in arithmetic                | [arithmetic-guards.md](references/arithmetic-guards.md)                 |
| Robustness  | Missing prerequisite checks            | [prerequisite-checks.md](references/prerequisite-checks.md)             |
| Robustness  | set -euo edge cases                    | [set-euo-pitfalls.md](references/set-euo-pitfalls.md)                   |
| Robustness  | grep in pipeline under pipefail        | [set-euo-pitfalls.md](references/set-euo-pitfalls.md)                   |
| Robustness  | Inverted return codes (0=fail)         | [return-code-convention.md](references/return-code-convention.md)       |
| Robustness  | set -e in informational scripts        | [return-code-convention.md](references/return-code-convention.md)       |
| Security    | Variable content used as regex         | [variable-as-regex.md](references/variable-as-regex.md)                 |
| Robustness  | Relative symlinks in git hooks         | [variable-as-regex.md](references/variable-as-regex.md)                 |
| Robustness  | sed for YAML/JSON parsing              | [variable-as-regex.md](references/variable-as-regex.md)                 |
| DRY         | Repeated check+counter blocks          | [dry-patterns.md](references/dry-patterns.md)                           |
| DRY         | Repeated scan/grep loops               | [dry-patterns.md](references/dry-patterns.md)                           |
| DRY         | Repeated format+counter pairs          | [dry-patterns.md](references/dry-patterns.md)                           |
| DRY         | Duplicated constants across scripts    | [shared-libs-and-structure.md](references/shared-libs-and-structure.md) |
| DRY         | Duplicated dependency checks           | [shared-libs-and-structure.md](references/shared-libs-and-structure.md) |
| Robustness  | Missing shellcheck source directive    | [shared-libs-and-structure.md](references/shared-libs-and-structure.md) |

### Step 4: Classify and Report

For each confirmed issue, extract the fix from the reference and format the output.
**Never suggest fixes from memory alone — always cite the reference.**

---

## Output Format

For EACH issue found:

```text
**Issue:** [Anti-pattern name]
**Severity:** CRITICAL | MAJOR | MINOR
**Reference:** [filename.md]
**Location:** file:line
**Current code:**
[offending snippet]
**Fix:**
[corrected code from reference]
**Why:** [1-sentence explanation from reference]
```

Severity classification:

- **CRITICAL**: Security issues (eval injection, command expansion)
- **MAJOR**: Portability issues that break on common platforms, division by zero, DRY violations with 5+ repetitions
- **MINOR**: Missing prerequisite checks, set -euo edge cases in non-critical paths, DRY violations with 3-4 repetitions, missing shellcheck source directives

After all issues, append suggestions (if any) in this format:

```text
**Suggestion:** [Brief description]
**Location:** file:line
**Improvement:** [Proposed change]
```

---

## Quality Gate

Before completing the review, verify:

- [ ] Every issue cites a specific reference file that was READ
- [ ] Every fix comes from the reference, not from memory
- [ ] No false positives — the anti-pattern MUST actually be present in the code
- [ ] Fixes include surrounding context so the user can apply them
- [ ] Security issues classified as CRITICAL, not MINOR
- [ ] No ShellCheck-detectable issues reported (quoting, SC2068, SC2086)

---

## Gardener

After all anti-pattern issues, look for opportunities to simplify or improve
readability that do not fall into the categories above. Present these as
**Suggestions** (not errors) — they are optional improvements, not blockers.

Gardener checks (DRY / KISS / structure):

- **Repeated blocks**: 3+ identical multi-line blocks → extract a helper with `"$@"` forwarding
- **Scan loops**: same grep-iterate-append body repeated → parameterized scan function
- **Format+counter**: same string-append + counter-increment → one-line helper
- **Shared utilities**: same constants/functions in multiple .sh files → `source` a shared lib
- **Severity as parameter**: separate wrapper functions for p0/warn → one function with severity arg
- **`$?` capture**: `func; rc=$?; if [[ $rc -ne 0 ]]` → `if ! func; then` (when return code isn't needed later)
- **Missing `source` guards**: `source foo.sh` without `# shellcheck source=foo.sh` directive

When proposing DRY improvements, verify the extraction is safe:

- Helpers that modify caller variables must NOT run in subshells (`$(...)` or pipes)
- `"$@"` forwarding preserves all quoting — prefer it over manual argument passing
- Only extract when 3+ repetitions exist — premature extraction hurts readability

---

## SKILL COMPLETE

```text
✅ SKILL COMPLETE: /bash-reviewer
├─ Files reviewed: [count]
├─ Issues found: [count] (CRITICAL: [n], MAJOR: [n], MINOR: [n])
├─ Suggestions: [count]
└─ Status: CLEAN / HAS ISSUES
```
