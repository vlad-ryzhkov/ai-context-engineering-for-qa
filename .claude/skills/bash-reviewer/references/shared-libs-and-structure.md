# Don't duplicate utilities across scripts

## Why this is bad

When multiple scripts in the same project define the same color constants, dependency checks, or helper functions, changes must be synchronized across all copies. Missed updates cause silent divergence — one script checks for the right tool version while another doesn't. Copy-pasted code also inflates line counts and makes review harder.

Common duplicated utilities:

- ANSI color constants (RED, GREEN, NC)
- Tool dependency checks (`command -v jq`, yq version detection)
- Logging helpers (log_pass, log_fail, log_warn)
- Argument parsing boilerplate

## Bad Example

```bash
# ❌ BAD: same color definitions in 4 different scripts
# lint.sh
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# test-lint.sh (identical copy)
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# structure-check.sh (identical copy, different variable order)
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# ❌ BAD: yq dependency check duplicated in 3 scripts
if ! command -v yq >/dev/null 2>&1; then
  echo "ERROR: yq is required" >&2
  exit 1
fi
if ! yq --version 2>&1 | grep -q 'mikefarah'; then
  echo "ERROR: Requires mikefarah/yq" >&2
  exit 1
fi
```

## Good Example

```bash
# ✅ GOOD: shared color constants in one file
# scripts/lib/colors.sh
# shellcheck disable=SC2034  # variables used by callers
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ✅ GOOD: shared dependency check with mode parameter
# scripts/lib/require-yq.sh
require_yq() {
  local missing=""
  if ! command -v yq >/dev/null 2>&1; then
    missing="yq is not installed"
  elif ! yq --version 2>&1 | grep -q 'mikefarah'; then
    missing="found Python yq, need mikefarah/yq (Go version)"
  fi
  [[ -z "$missing" ]] && return 0

  # --warn mode: non-blocking (returns 1 instead of exit 1)
  if [[ "${1:-}" == "--warn" ]]; then
    echo -e "${YELLOW}WARN: ${missing} — skipping${NC}"
    return 1
  else
    echo -e "${RED}ERROR: ${missing}${NC}" >&2
    exit 1
  fi
}

# ✅ GOOD: each script sources shared libs
# shellcheck source=lib/colors.sh
source "$SCRIPT_DIR/lib/colors.sh"
# shellcheck source=lib/require-yq.sh
source "$SCRIPT_DIR/lib/require-yq.sh"
require_yq          # CI context: fail hard
require_yq --warn   # pre-commit hook: degrade gracefully
```

## When to Extract vs. When to Inline

**Extract** when:

- 3+ scripts use the same constants or function
- The utility has version-sensitive logic (dependency checks)
- Changes to the utility must apply everywhere simultaneously

**Keep inline** when:

- Only 1-2 scripts use it
- The "shared" code is a single line (`set -uo pipefail`)
- Extracting would create a dependency chain harder to understand than the duplication

## Script Structure Principles

**Single Responsibility**: each script does one thing

- `lint.sh` — P0/P1 validation checks
- `skill-structure.sh` — SUGGEST-only structural analysis
- `pre-commit.sh` — orchestrates both per staged skill

**Open for Extension**: add checks without changing orchestration

- New check = new `check_*` function + one `run_check` line
- Severity (`p0`/`warn`) is a parameter, not a separate code path

**Shared libs (`source`)** over copy-paste — but keep the dependency chain shallow:

- `lib/colors.sh` — zero dependencies
- `lib/require-yq.sh` — depends on colors.sh (for colored output)
- Scripts source both explicitly with `# shellcheck source=` directives

## What to look for in code review

- Same ANSI color variables defined in multiple .sh files
- Same `command -v` + version check appearing in multiple scripts
- Missing `# shellcheck source=` directives on `source` statements
- Deep source chains (A sources B sources C) — keep to max 2 levels
- Scripts that could share a dependency check but each implement it differently
- Functions that accept a mode/severity parameter vs. duplicated wrappers
