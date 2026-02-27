---
name: pr
description: Create a pull request — runs tests, commits changes, pushes branch, opens PR with conventional commit title. Use when ready to submit work for review.
allowed-tools: "Bash Read Edit Glob Grep"
---

# /pr — Pull Request Creator

## When to Use

- All planned changes are implemented and locally verified
- Ready to submit work for code review
- Branch does not yet exist on remote

## Steps

### 1. Confirm Target Branch

Ask the user for the target branch (never assume `main` vs `master`).

### 2. Run Quality Gates

```bash
./scripts/pre-push.sh
```

Stop and report failures before proceeding.

### 3. Commit Changes

Stage relevant files (not `.env`, secrets, or build output). Commit with conventional commit format:

```
<type>: <description in English>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

Types: `feat`, `fix`, `test`, `chore`, `docs`, `refactor`

### 4. Push Branch

```bash
git push -u origin <branch-name>
```

### 5. Create PR

PR title: conventional commits format, Latin only, under 70 characters.

PR body: summarize changes in English. Include test coverage note if applicable.

```bash
gh pr create --title "<type>: <description>" --body "$(cat <<'EOF'
## Summary
- <bullet 1>
- <bullet 2>

## Test plan
- [ ] compileTestKotlin passes
- [ ] ktlintCheck passes
- [ ] Tests cover the changed scenario

🤖 Generated with Claude Code
EOF
)"
```

## Quality Gates

- [ ] `./scripts/pre-push.sh` → all checks passed
- [ ] PR title: conventional commits, Latin only (CI enforced)

## Rules

- Never push without confirming target branch name with the user
- Never skip `./scripts/pre-push.sh` — broken code must not reach PR
- Never use `--no-verify` or `--force` without explicit user instruction
