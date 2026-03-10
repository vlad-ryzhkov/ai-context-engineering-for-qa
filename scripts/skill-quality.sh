#!/usr/bin/env bash
# Main entrypoint for the Skill Quality Pipeline.
# Orchestrates agnix (generic AI config lint) + custom repo-specific checks.
#
# Usage:
#   bash scripts/skill-quality.sh                    # Full: agnix + structure + budget
#   bash scripts/skill-quality.sh --check structure   # Tier 1 Baseline only
#   bash scripts/skill-quality.sh --check budget      # Token budget report only
#   bash scripts/skill-quality.sh --check regression  # Regression detection only
#   bash scripts/skill-quality.sh --snapshot          # Update baseline snapshot
#   bash scripts/skill-quality.sh --diff              # Compare vs baseline
#   bash scripts/skill-quality.sh --ci                # CI mode: agnix + structure + diff (fail on ERROR)
#   bash scripts/skill-quality.sh --skill api-tests   # Single skill scope
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="${SCRIPT_DIR}/lib"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

MODE="full"
CHECK=""
SKILL_ARG=""
EXIT_CODE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --check) CHECK="$2"; shift 2 ;;
    --snapshot) MODE="snapshot"; shift ;;
    --diff) MODE="diff"; shift ;;
    --ci) MODE="ci"; shift ;;
    --skill) SKILL_ARG="--skill $2"; shift 2 ;;
    -h|--help)
      echo "Usage: bash scripts/skill-quality.sh [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --check structure   Tier 1 Baseline checks only"
      echo "  --check budget      Token budget report only"
      echo "  --check regression  Regression detection only"
      echo "  --snapshot          Save current state as baseline"
      echo "  --diff              Compare current vs baseline"
      echo "  --ci                CI mode (agnix + structure + regression, fail on ERROR)"
      echo "  --skill <name>      Scope to a single skill"
      echo "  -h, --help          Show this help"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

run_agnix() {
  echo -e "\n${CYAN}═══ agnix Lint (AI Config Validator) ═══${NC}\n"
  if command -v npx >/dev/null 2>&1; then
    npx agnix --target claude-code . || {
      echo -e "${RED}agnix found issues${NC}"
      EXIT_CODE=1
    }
  else
    echo -e "${RED}npx not found — install Node.js and run 'npm ci' first${NC}"
    EXIT_CODE=1
  fi
}

run_structure() {
  echo ""
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/skill-structure.sh" $SKILL_ARG || EXIT_CODE=1
}

run_budget() {
  echo ""
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/token-budget.sh" $SKILL_ARG
}

run_regression() {
  echo ""
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/regression-detect.sh" $SKILL_ARG || EXIT_CODE=1
}

run_snapshot() {
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/token-budget.sh" --snapshot $SKILL_ARG
}

run_diff() {
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/token-budget.sh" --diff $SKILL_ARG || EXIT_CODE=1
}

case "$MODE" in
  full)
    if [[ -n "$CHECK" ]]; then
      case "$CHECK" in
        structure) run_structure ;;
        budget) run_budget ;;
        regression) run_regression ;;
        *) echo "Unknown check: $CHECK"; exit 1 ;;
      esac
    else
      run_agnix
      run_structure
      run_budget
    fi
    ;;
  snapshot)
    run_snapshot
    ;;
  diff)
    run_diff
    ;;
  ci)
    echo -e "${CYAN}═══ CI Mode: Skill Quality Pipeline ═══${NC}"
    run_agnix
    run_structure
    if [[ -f ".claude/baselines/skill-snapshot.json" ]]; then
      run_regression
      run_diff
    else
      echo -e "\n${CYAN}No baseline found — skipping regression + diff checks${NC}"
    fi
    echo ""
    if [[ "$EXIT_CODE" -eq 0 ]]; then
      echo -e "${GREEN}✅ All checks passed${NC}"
    else
      echo -e "${RED}❌ Quality checks failed${NC}"
    fi
    ;;
esac

exit "$EXIT_CODE"
