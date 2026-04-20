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
| CLI arg into path w/o guard   | `--\w+.*"\$2".*\|\$\{[A-Z_]+\}/\$\{`  | Security    |
| Substring placeholder-exclude | `your_\|example_\|placeholder`        | Security    |
| Case-restricted security cls  | `\[A-Z_\].*(TOKEN\|KEY\|SECRET)`      | Security    |
| Text-only extension filter    | `find.*-name.*\.md.*-o.*-name.*\.sh`  | Security    |
| Suppression w/o allowlist     | `is_nolint\|is_suppressed\|# noqa`    | Security    |

### Step 3: Read References

For each detected anti-pattern, **READ** the corresponding reference file:

| Category    | Anti-Pattern                           | Reference                                                             |
| ----------- | -------------------------------------- | --------------------------------------------------------------------- |
| Security    | eval with untrusted input              | [eval-injection.md](references/eval-injection.md)                     |
| Security    | Unvalidated command expansion          | [eval-injection.md](references/eval-injection.md)                     |
| Portability | Platform-specific sed flags            | [portable-sed-and-tools.md](references/portable-sed-and-tools.md)     |
| Portability | grep -P (Perl regex)                   | [portable-sed-and-tools.md](references/portable-sed-and-tools.md)     |
| Portability | readarray/mapfile without bash 4 check | [portable-sed-and-tools.md](references/portable-sed-and-tools.md)     |
| Portability | find -o without grouping               | [find-operator-precedence.md](references/find-operator-precedence.md) |
| Robustness  | Division without zero check            | [arithmetic-guards.md](references/arithmetic-guards.md)               |
| Robustness  | wc output in arithmetic                | [arithmetic-guards.md](references/arithmetic-guards.md)               |
| Robustness  | Missing prerequisite checks            | [prerequisite-checks.md](references/prerequisite-checks.md)           |
| Robustness  | set -euo edge cases                    | [set-euo-pitfalls.md](references/set-euo-pitfalls.md)                 |
| Robustness  | grep in pipeline under pipefail        | [set-euo-pitfalls.md](references/set-euo-pitfalls.md)                 |
| Robustness  | Inverted return codes (0=fail)         | [return-code-convention.md](references/return-code-convention.md)     |
| Robustness  | set -e in informational scripts        | [return-code-convention.md](references/return-code-convention.md)     |
| Security    | Variable content used as regex         | [variable-as-regex.md](references/variable-as-regex.md)               |
| Robustness  | Relative symlinks in git hooks         | [variable-as-regex.md](references/variable-as-regex.md)               |
| Robustness  | sed for YAML/JSON parsing              | [variable-as-regex.md](references/variable-as-regex.md)               |
| Security    | Suppression DSL w/o allowlist          | [variable-as-regex.md](references/variable-as-regex.md)               |
| Security    | CLI arg path traversal                 | [cli-arg-path-traversal.md](references/cli-arg-path-traversal.md)     |
| Security    | Substring placeholder-exclude          | [security-scanner-fn.md](references/security-scanner-fn.md)           |
| Security    | Case-restricted security regex         | [security-scanner-fn.md](references/security-scanner-fn.md)           |
| Security    | Text-only extension filter             | [security-scanner-fn.md](references/security-scanner-fn.md)           |

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
- **MAJOR**: Portability issues that break on common platforms, division by zero
- **MINOR**: Missing prerequisite checks, set -euo edge cases in non-critical paths

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

Gardener checks:

- **Severity as parameter**: two near-identical functions where the only
  difference is a literal (`p0` vs `warn`, `FAIL` vs `WARN`) — fold into
  one function with severity as a parameter.
- **Stringly-typed mode switch**: a function takes a string arg that
  branches its behavior into 3+ modes (`""` / `"use_whitelist"` / regex).
  Split into 2–3 named functions calling a shared internal helper.
- **Dead placeholder state**: a variable declared and initialized (usually
  `FOO=0`) but never assigned again; often commented as "reserved for
  future" — delete it and the line that prints it. Re-add when actually
  needed.
- **Function mutates caller-scope locals**: a helper reads/writes
  variables from its caller's scope (`failure_count`, `has_p0_failure`,
  etc.) via bash dynamic scoping. Convert the helper to a pure function
  returning a status code (0/1/2); let the caller do the counter update
  inline or via a tiny local wrapper. Keeps the mutation surface small
  and explicit.
- **`$?` capture when not needed later**: `func; rc=$?; if [[ $rc -ne 0 ]]`
  → `if ! func; then`.

---

## SKILL COMPLETE

```text
✅ SKILL COMPLETE: /bash-reviewer
├─ Files reviewed: [count]
├─ Issues found: [count] (CRITICAL: [n], MAJOR: [n], MINOR: [n])
├─ Suggestions: [count]
└─ Status: CLEAN / HAS ISSUES
```
