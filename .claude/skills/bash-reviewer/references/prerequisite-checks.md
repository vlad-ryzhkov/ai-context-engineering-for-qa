# Don't use commands without checking availability

## Why this is bad

Scripts that call `jq`, `yq`, `shellcheck`, `docker`, or other non-default tools without first checking if they are installed produce cryptic "command not found" errors — often after partial execution that leaves state in a broken condition. Users waste time debugging what is simply a missing dependency.

## Bad Example

```bash
# ❌ BAD: jq used without checking if installed
data=$(jq '.name' config.json)

# ❌ BAD: version-specific feature used without version check
mapfile -t lines < <(some_command)  # requires bash 4+

# ❌ BAD: tool used deep in script after other work is already done
do_setup
do_build
deploy_with_kubectl  # fails here — wasted all the build time
```

## Good Example

```bash
# ✅ GOOD: check at script start with helpful error message
if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required but not installed." >&2
  echo "Install: https://stedolan.github.io/jq/download/" >&2
  exit 1
fi

# ✅ GOOD: graceful skip with warning when tool is optional
if ! command -v shellcheck >/dev/null 2>&1; then
  echo "WARN: shellcheck not found — skipping shell lint" >&2
else
  shellcheck "$script"
fi

# ✅ GOOD: version check for minimum requirement
bash_version="${BASH_VERSINFO[0]}"
if [[ "$bash_version" -lt 4 ]]; then
  echo "ERROR: bash 4+ required (found bash ${bash_version})" >&2
  exit 1
fi
```

## What to look for in code review

- Commands from non-default packages used without a `command -v` guard
- Prerequisite checks placed after significant work instead of at script start
- Missing install instructions in error messages — the user should know how to fix it
- Bash 4+ features (`mapfile`, `readarray`, associative arrays) without version checks
