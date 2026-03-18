#!/usr/bin/env bash
# Main entrypoint for the Skill Quality Pipeline.
# Orchestrates agnix (generic AI config lint) + custom repo-specific checks.
#
# Layer 1 (Skill Definition Linting): structure, budget, regression, density, compliance
# Layer 2 (Skill Output Linting): contract, golden, compliance --output-dir
#
# Usage:
#   bash scripts/skill-quality.sh                    # Full: agnix + structure + budget
#   bash scripts/skill-quality.sh --check structure   # Tier 1 Baseline only
#   bash scripts/skill-quality.sh --check budget      # Token budget report only
#   bash scripts/skill-quality.sh --check regression  # Regression detection only
#   bash scripts/skill-quality.sh --check contract    # SKILL COMPLETE + cross-skill contracts
#   bash scripts/skill-quality.sh --check compliance  # Definition compliance (BANNED/REQUIRED)
#   bash scripts/skill-quality.sh --check density     # Context density (SLKD)
#   bash scripts/skill-quality.sh --check golden      # Golden file structural tests (T1)
#   bash scripts/skill-quality.sh --check reflector   # Reflector Layer 1 detection
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
      echo "  --check contract    SKILL COMPLETE + cross-skill contracts"
      echo "  --check compliance  Definition compliance (BANNED/REQUIRED extraction)"
      echo "  --check density     Context density (SLKD signal-to-noise)"
      echo "  --check golden      Golden file structural tests (T1)"
      echo "  --check reflector   Reflector Layer 1 detection (recurring patterns)"
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

run_contract() {
  echo ""
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/contract-validator.sh" $SKILL_ARG || EXIT_CODE=1
}

run_compliance() {
  echo ""
  if [[ -n "$SKILL_ARG" ]]; then
    local skill_name
    skill_name=$(echo "$SKILL_ARG" | sed 's/--skill //')
    bash "${LIB_DIR}/compliance-checker.sh" --skill "$skill_name" --rules-only || EXIT_CODE=1
  else
    echo -e "${CYAN}═══ Compliance Check ═══${NC}"
    echo -e "Requires --skill <name>. Run: bash scripts/skill-quality.sh --check compliance --skill <name>"
  fi
}

run_density() {
  echo ""
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/skill-structure.sh" --density $SKILL_ARG || EXIT_CODE=1
}

run_golden() {
  echo ""
  # shellcheck disable=SC2086
  bash "${SCRIPT_DIR}/golden-test.sh" $SKILL_ARG || EXIT_CODE=1
}

run_reflector() {
  echo ""
  # shellcheck disable=SC2086
  bash "${LIB_DIR}/reflector.sh" $SKILL_ARG || EXIT_CODE=1
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
        contract) run_contract ;;
        compliance) run_compliance ;;
        density) run_density ;;
        golden) run_golden ;;
        reflector) run_reflector ;;
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
