#!/usr/bin/env bash
# setup-hooks.sh: installs git hooks as symlinks to scripts/
set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel)
HOOKS_DIR="$REPO_ROOT/.git/hooks"
SCRIPTS_DIR="$REPO_ROOT/scripts"

chmod +x "$SCRIPTS_DIR/pre-commit.sh"
chmod +x "$SCRIPTS_DIR/pre-push.sh"

ln -sf "../../scripts/pre-commit.sh" "$HOOKS_DIR/pre-commit"
ln -sf "../../scripts/pre-push.sh" "$HOOKS_DIR/pre-push"

echo "Git hooks installed:"
echo "  .git/hooks/pre-commit → scripts/pre-commit.sh"
echo "  .git/hooks/pre-push   → scripts/pre-push.sh"
