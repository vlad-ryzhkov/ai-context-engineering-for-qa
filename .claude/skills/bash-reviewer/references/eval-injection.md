# Don't use eval with untrusted input

## Why this is bad

`eval` executes arbitrary strings as shell commands. If the string contains user input, command substitution, or any variable not fully controlled by the script, an attacker (or a bug) can inject arbitrary commands. This is the shell equivalent of SQL injection.

## Bad Example

```bash
# ❌ BAD: variable is executed as code
eval "$user_input"

# ❌ BAD: command name built from variable
eval "${cmd_prefix}_handler"

# ❌ BAD: eval with command substitution
eval "$(get_config_command)"
```

## Good Example

```bash
# ✅ GOOD: use a case dispatch instead of eval
case "$action" in
  start)  do_start  ;;
  stop)   do_stop   ;;
  status) do_status ;;
  *) echo "Unknown action: $action" >&2; exit 1 ;;
esac

# ✅ GOOD: use an associative array for dynamic dispatch
declare -A handlers=(
  [start]=do_start
  [stop]=do_stop
)
if [[ -n "${handlers[$action]+x}" ]]; then
  "${handlers[$action]}"
else
  echo "Unknown action: $action" >&2; exit 1
fi

# ✅ GOOD: if eval is truly necessary, whitelist the input
if [[ "$var_name" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
  eval "$var_name=\$value"
else
  echo "Invalid variable name: $var_name" >&2; exit 1
fi
```

## What to look for in code review

- Any use of `eval` — treat every occurrence as suspicious until proven safe
- Variables or command substitutions inside `eval` arguments
- Dynamic command construction via string concatenation
- `eval` used to work around quoting — usually a sign that arrays or `printf %q` should be used instead
