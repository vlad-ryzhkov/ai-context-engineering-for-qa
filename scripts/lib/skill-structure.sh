#!/usr/bin/env bash
# Tier 1 Baseline Validator — repo-specific checks that agnix cannot cover.
# Usage: bash scripts/lib/skill-structure.sh [--skill <name>]
set -euo pipefail

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

SKILLS_DIR=".claude/skills"
ERRORS=0
WARNINGS=0
SKILL_FILTER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skill) SKILL_FILTER="$2"; shift 2 ;;
    *) shift ;;
  esac
done

check_skill() {
  local skill_dir="$1"
  local skill_name
  skill_name=$(basename "$skill_dir")
  local skill_file="${skill_dir}/SKILL.md"

  if [[ ! -f "$skill_file" ]]; then
    return
  fi

  local findings=""
  local line_count
  line_count=$(wc -l < "$skill_file" | tr -d ' ')

  # S8: Line count
  if [[ "$line_count" -gt 500 ]]; then
    findings="${findings}\n  ${RED}ERROR${NC}  [S8] ${line_count} lines (limit: 500). Split content to references/"
    ((ERRORS++)) || true
  elif [[ "$line_count" -gt 400 ]]; then
    findings="${findings}\n  ${YELLOW}WARN${NC}   [S8] ${line_count} lines (recommended: ≤400, limit: 500)"
    ((WARNINGS++)) || true
  fi

  # S10: Quality Gate / Self-Review section
  if ! grep -qiE 'quality gate|self-review|post-check' "$skill_file" 2>/dev/null; then
    findings="${findings}\n  ${YELLOW}WARN${NC}   [S10] Missing Quality Gate / Self-Review section"
    ((WARNINGS++)) || true
  fi

  # S11: Gardener reference
  if ! grep -qi 'gardener' "$skill_file" 2>/dev/null; then
    findings="${findings}\n  ${YELLOW}WARN${NC}   [S11] Missing Gardener protocol reference"
    ((WARNINGS++)) || true
  fi

  # S12: SILENT MODE / Verbosity Protocol
  if ! grep -qiE 'silent mode|verbosity|token economy|machine mode' "$skill_file" 2>/dev/null; then
    findings="${findings}\n  ${YELLOW}WARN${NC}   [S12] Missing SILENT MODE / Verbosity Protocol"
    ((WARNINGS++)) || true
  fi

  # S13: Completion block
  if ! grep -qiE 'skill complete|completion' "$skill_file" 2>/dev/null; then
    findings="${findings}\n  ${YELLOW}WARN${NC}   [S13] Missing SKILL COMPLETE / Completion block"
    ((WARNINGS++)) || true
  fi

  # S15: Cross-reference validation — check LOCAL references/ files exist
  # Only match standalone references/ paths (not prefixed by .claude/skills/other-skill/)
  local refs
  refs=$(grep -oE '(^|[^/])references/[a-zA-Z0-9/_-]+\.md' "$skill_file" 2>/dev/null \
    | grep -v '\.claude/skills/' \
    | sed 's/^[^r]*//' \
    | sort -u || true)
  if [[ -n "$refs" ]]; then
    while IFS= read -r ref; do
      [[ -z "$ref" ]] && continue
      local ref_path="${skill_dir}/${ref}"
      if [[ ! -f "$ref_path" ]]; then
        findings="${findings}\n  ${RED}ERROR${NC}  [S15] Referenced file not found: ${ref}"
        ((ERRORS++)) || true
      fi
    done <<< "$refs"
  fi

  # S16: Agent file validation — check agent: field in YAML frontmatter only
  local agent_ref=""
  if head -1 "$skill_file" | grep -q '^---$'; then
    agent_ref=$(sed -n '/^---$/,/^---$/p' "$skill_file" | grep -m1 -E '^agent:' | sed 's/^agent:[[:space:]]*//' | tr -d '"' | tr -d "'" | xargs || true)
  fi
  if [[ -n "$agent_ref" ]]; then
    local agent_path=".claude/${agent_ref}"
    if [[ ! -f "$agent_path" ]]; then
      findings="${findings}\n  ${RED}ERROR${NC}  [S16] Agent file not found: ${agent_path}"
      ((ERRORS++)) || true
    fi
  fi

  # Output
  if [[ -n "$findings" ]]; then
    echo -e "${CYAN}${skill_name}${NC} (${line_count} lines)${findings}"
  else
    echo -e "${GREEN}✓${NC} ${skill_name} (${line_count} lines)"
  fi
}

# S17: Anti-pattern index completeness
check_antipattern_index() {
  local index_file=".claude/qa-antipatterns/_index.md"
  if [[ ! -f "$index_file" ]]; then
    echo -e "\n${RED}ERROR${NC}  [S17] Anti-pattern index not found: ${index_file}"
    ((ERRORS++)) || true
    return
  fi

  local missing=0
  while IFS= read -r md_file; do
    local relative
    relative=$(echo "$md_file" | sed 's|.claude/qa-antipatterns/||')
    if [[ "$relative" == "_index.md" ]]; then continue; fi
    if ! grep -q "$relative" "$index_file" 2>/dev/null; then
      if [[ "$missing" -eq 0 ]]; then
        echo -e "\n${CYAN}Anti-pattern Index [S17]${NC}"
      fi
      echo -e "  ${YELLOW}WARN${NC}   Not listed in _index.md: ${relative}"
      ((WARNINGS++)) || true
      ((missing++)) || true
    fi
  done < <(find .claude/qa-antipatterns -name "*.md" -type f | sort)

  if [[ "$missing" -eq 0 ]]; then
    echo -e "\n${GREEN}✓${NC} Anti-pattern index complete"
  fi
}

# Main
echo -e "${CYAN}═══ Tier 1 Baseline Validator ═══${NC}\n"

if [[ -n "$SKILL_FILTER" ]]; then
  skill_path="${SKILLS_DIR}/${SKILL_FILTER}"
  if [[ -d "$skill_path" ]]; then
    check_skill "$skill_path"
  else
    echo -e "${RED}Skill not found: ${SKILL_FILTER}${NC}"
    exit 1
  fi
else
  for skill_dir in "${SKILLS_DIR}"/*/; do
    [[ -d "$skill_dir" ]] && check_skill "$skill_dir"
  done
  check_antipattern_index
fi

echo ""
echo -e "Results: ${RED}${ERRORS} error(s)${NC}, ${YELLOW}${WARNINGS} warning(s)${NC}"

if [[ "$ERRORS" -gt 0 ]]; then
  exit 1
fi
exit 0
