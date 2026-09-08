#!/usr/bin/env bash
# ACE Kit Setup — creates symlinks and IDE integration files.
#
# Usage:
#   bash ace-kit/setup.sh                # Interactive IDE selection
#   bash ace-kit/setup.sh --claude       # Claude Code only
#   bash ace-kit/setup.sh --cursor       # Cursor only
#   bash ace-kit/setup.sh --copilot      # Copilot only
#   bash ace-kit/setup.sh --codex        # Codex only
#   bash ace-kit/setup.sh --jetbrains    # JetBrains AI (Junie) only
#   bash ace-kit/setup.sh --gemini       # Gemini Code Assist only
#   bash ace-kit/setup.sh --all          # All six IDEs
set -euo pipefail

# --- Resolve paths ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || (cd "$SCRIPT_DIR/.." && pwd))"

# Compute relative path from $1 (from dir) to $2 (to dir)
compute_relpath() {
  local from="$1" to="$2"
  if command -v python3 >/dev/null 2>&1; then
    python3 -c "import os.path, sys; print(os.path.relpath(sys.argv[1], sys.argv[2]))" "$to" "$from"
  elif command -v realpath >/dev/null 2>&1; then
    realpath --relative-to="$from" "$to"
  else
    echo "$to"
  fi
}

ACE_KIT_DIR="$SCRIPT_DIR"
REL_KIT="$(compute_relpath "$REPO_ROOT" "$ACE_KIT_DIR")"

# --- Colors ---
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

# --- Parse flags ---
SETUP_CLAUDE=false
SETUP_CURSOR=false
SETUP_COPILOT=false
SETUP_CODEX=false
SETUP_JETBRAINS=false
SETUP_GEMINI=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --claude)     SETUP_CLAUDE=true; shift ;;
    --cursor)     SETUP_CURSOR=true; shift ;;
    --copilot)    SETUP_COPILOT=true; shift ;;
    --codex)      SETUP_CODEX=true; shift ;;
    --jetbrains)  SETUP_JETBRAINS=true; shift ;;
    --gemini)     SETUP_GEMINI=true; shift ;;
    --all)        SETUP_CLAUDE=true; SETUP_CURSOR=true; SETUP_COPILOT=true; SETUP_CODEX=true; SETUP_JETBRAINS=true; SETUP_GEMINI=true; shift ;;
    -h|--help)
      echo "ACE Kit Setup — creates symlinks and IDE integration files."
      echo ""
      echo "Usage:"
      echo "  bash ace-kit/setup.sh                # Interactive IDE selection"
      echo "  bash ace-kit/setup.sh --claude       # Claude Code only"
      echo "  bash ace-kit/setup.sh --cursor       # Cursor only"
      echo "  bash ace-kit/setup.sh --copilot      # Copilot only"
      echo "  bash ace-kit/setup.sh --codex        # Codex only"
      echo "  bash ace-kit/setup.sh --jetbrains    # JetBrains AI (Junie) only"
      echo "  bash ace-kit/setup.sh --gemini       # Gemini Code Assist only"
      echo "  bash ace-kit/setup.sh --all          # All six IDEs"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# Interactive selection if no flags
if [[ "$SETUP_CLAUDE" == false && "$SETUP_CURSOR" == false && "$SETUP_COPILOT" == false && "$SETUP_CODEX" == false && "$SETUP_JETBRAINS" == false && "$SETUP_GEMINI" == false ]]; then
  echo -e "${CYAN}ACE Kit Setup${NC}"
  echo ""
  echo "Select IDEs to configure (space-separated numbers, or 'a' for all):"
  echo "  1) Claude Code"
  echo "  2) Cursor"
  echo "  3) Copilot"
  echo "  4) Codex"
  echo "  5) JetBrains AI (Junie)"
  echo "  6) Gemini Code Assist"
  echo "  a) All"
  echo ""
  read -rp "Choice: " choice
  case "$choice" in
    *a*|*A*) SETUP_CLAUDE=true; SETUP_CURSOR=true; SETUP_COPILOT=true; SETUP_CODEX=true; SETUP_JETBRAINS=true; SETUP_GEMINI=true ;;
    *)
      [[ "$choice" == *1* ]] && SETUP_CLAUDE=true
      [[ "$choice" == *2* ]] && SETUP_CURSOR=true
      [[ "$choice" == *3* ]] && SETUP_COPILOT=true
      [[ "$choice" == *4* ]] && SETUP_CODEX=true
      [[ "$choice" == *5* ]] && SETUP_JETBRAINS=true
      [[ "$choice" == *6* ]] && SETUP_GEMINI=true
      ;;
  esac

  if [[ "$SETUP_CLAUDE" == false && "$SETUP_CURSOR" == false && "$SETUP_COPILOT" == false && "$SETUP_CODEX" == false && "$SETUP_JETBRAINS" == false && "$SETUP_GEMINI" == false ]]; then
    echo "No IDE selected. Exiting."
    exit 0
  fi
fi

echo -e "${CYAN}=== ACE Kit Setup ===${NC}"
echo "  Repo root: $REPO_ROOT"
echo "  ACE kit:   $ACE_KIT_DIR"
echo ""

cd "$REPO_ROOT"

# --- Helper: create symlink (relative) ---
make_symlink() {
  local target="$1"  # relative path from REPO_ROOT to ace-kit file
  local link="$2"    # relative path from REPO_ROOT to link location
  local link_dir
  link_dir="$(dirname "$link")"
  local rel_target
  rel_target="$(compute_relpath "$REPO_ROOT/$link_dir" "$ACE_KIT_DIR/$(echo "$target" | sed "s|^$REL_KIT/||")")"

  mkdir -p "$link_dir"
  ln -sf "$rel_target" "$link"
  echo -e "  ${GREEN}+${NC} $link -> $rel_target"
}

# --- Claude Code ---
if [[ "$SETUP_CLAUDE" == true ]]; then
  echo -e "${CYAN}[Claude Code]${NC}"
  mkdir -p .claude/protocols .claude/hooks .claude/skills

  # Protocol symlinks
  for proto in gardener.md reflection.md reflector.md; do
    make_symlink "$REL_KIT/protocols/$proto" ".claude/protocols/$proto"
  done

  # Hook symlink
  make_symlink "$REL_KIT/hooks/delta-guard.sh" ".claude/hooks/delta-guard.sh"

  # Skill directory symlink
  # Remove existing directory/symlink first to avoid nesting
  if [[ -e .claude/skills/curate-lessons ]]; then
    rm -rf .claude/skills/curate-lessons
  fi
  local_rel="$(compute_relpath "$REPO_ROOT/.claude/skills" "$ACE_KIT_DIR/skills/curate-lessons")"
  ln -sf "$local_rel" .claude/skills/curate-lessons
  echo -e "  ${GREEN}+${NC} .claude/skills/curate-lessons -> $local_rel"

  echo ""
fi

# --- Cursor ---
if [[ "$SETUP_CURSOR" == true ]]; then
  echo -e "${CYAN}[Cursor]${NC}"
  mkdir -p .cursor/rules

  # Generate .mdc wrapper for each protocol
  gen_mdc() {
    local name="$1"
    local desc="$2"
    local src="$ACE_KIT_DIR/protocols/${name}.md"
    local dst=".cursor/rules/ace-${name}.mdc"

    cat > "$dst" <<MDCEOF
---
description: "$desc"
alwaysApply: true
---

$(cat "$src")
MDCEOF
    echo -e "  ${GREEN}+${NC} $dst"
  }

  gen_mdc "gardener" "ACE Gardener Protocol — runs after every task to detect missing rules"
  gen_mdc "reflection" "ACE Reflection Protocol — structured failure analysis on skill errors"
  gen_mdc "reflector" "ACE Reflector Protocol — proactive batch pattern detection (Layer 2)"

  echo ""
fi

# --- Copilot ---
if [[ "$SETUP_COPILOT" == true ]]; then
  echo -e "${CYAN}[Copilot]${NC}"
  mkdir -p .github

  cat > .github/copilot-instructions.md <<'COPILOT_EOF'
## ACE Pipeline — Continuous Improvement

After completing any task:
1. Analyze what rules were missing (Gardener Protocol)
2. Append observations to `.ai-lessons/pending.md`
3. On failure: formulate 1 root-cause rule (Reflection Protocol)

When `.ai-lessons/pending.md` has >=3 entries, run /curate-lessons to promote rules.

Delta Update Protocol: Use Edit (surgical replace), never Write (full overwrite) on context files.

### Gardener Output Format

```text
GARDENER ANALYSIS
| # | Observation | Proposed rule | Section | Target file |
```

If no proposals: "GARDENER: no proposals for this run"

### Target File Selection
- Skill-specific rule -> `skills/{name}/SKILL.md`
- Global QA pattern -> `qa-antipatterns/{category}.md`
- Cross-cutting rule -> `.ai-lessons/pending.md`

### Reflection (on failure only)
- Identify root cause (not symptom)
- Formulate exactly 1 rule
- Dedup check before appending to pending.md
COPILOT_EOF

  echo -e "  ${GREEN}+${NC} .github/copilot-instructions.md"
  echo ""
fi

# --- Codex ---
if [[ "$SETUP_CODEX" == true ]]; then
  echo -e "${CYAN}[Codex]${NC}"

  if [[ ! -f AGENTS.md ]]; then
    cat > AGENTS.md <<'CODEX_EOF'
# AGENTS.md — Project Context Bridge

## CORE INSTRUCTION

**YOU MUST READ AND FOLLOW `CLAUDE.md` AT THE ROOT OF THIS PROJECT.**

`CLAUDE.md` is the **Single Source of Truth** for:
1. **Tech Stack:** Kotlin, JUnit 5, Allure, ktlint — LOCKED.
2. **Safety Protocols:** No destructive commands, no .env leaks.
3. **Code Style:** Formatting, naming conventions, assertion rules.
4. **Communication Protocol:** CLI-mode, no preambles, tool-first.

## QA AGENT PERSONA

**YOU MUST ALSO READ:** `.claude/qa_agent.md`

## CRITICAL BEHAVIOR

- If `CLAUDE.md` conflicts with any other instruction, `CLAUDE.md` WINS.
- Do NOT generate code that violates the strict dependencies listed in `CLAUDE.md`.
- All documentation and skill content must be written in **English**.
CODEX_EOF
    echo -e "  ${GREEN}+${NC} AGENTS.md"
  else
    echo -e "  ${YELLOW}~${NC} AGENTS.md (already exists, skipped)"
  fi

  echo ""
fi

# --- JetBrains AI (Junie) ---
if [[ "$SETUP_JETBRAINS" == true ]]; then
  echo -e "${CYAN}[JetBrains AI]${NC}"
  mkdir -p .junie

  cat > .junie/guidelines.md <<'JUNIE_EOF'
# JetBrains AI (Junie) — Project Context Bridge

## CORE INSTRUCTION

**YOU MUST READ AND FOLLOW `CLAUDE.md` AT THE ROOT OF THIS PROJECT.**

`CLAUDE.md` is the **Single Source of Truth** for:
1. **Tech Stack:** Kotlin, JUnit 5, Allure, ktlint — LOCKED.
2. **Safety Protocols:** No destructive commands, no .env leaks.
3. **Code Style:** Formatting, naming conventions, assertion rules.
4. **Communication Protocol:** CLI-mode, no preambles, tool-first.

## QA AGENT PERSONA

**YOU MUST ALSO READ:** `.claude/qa_agent.md`

## CRITICAL BEHAVIOR

- If `CLAUDE.md` conflicts with any other instruction, `CLAUDE.md` WINS.
- Do NOT generate code that violates the strict dependencies listed in `CLAUDE.md`.
- All documentation and skill content must be written in **English**.

## AVAILABLE SKILLS

Skills are defined in `.claude/skills/`. Read the `SKILL.md` file in each directory for the full protocol.

**Recommended Workflow:** `repo-scout` → `api-test-cases` → `api-tests` → `api-test-review`
JUNIE_EOF

  echo -e "  ${GREEN}+${NC} .junie/guidelines.md"
  echo ""
fi

# --- Gemini Code Assist ---
if [[ "$SETUP_GEMINI" == true ]]; then
  echo -e "${CYAN}[Gemini Code Assist]${NC}"

  if [[ ! -L GEMINI.md ]]; then
    ln -sf CLAUDE.md GEMINI.md
    echo -e "  ${GREEN}+${NC} GEMINI.md -> CLAUDE.md (symlink)"
  else
    echo -e "  ${YELLOW}~${NC} GEMINI.md (already exists, skipped)"
  fi

  echo ""
fi

# --- Common: starter files and script symlinks ---
echo -e "${CYAN}[Common]${NC}"

# Create directories
mkdir -p .ai-lessons tests/telemetry scripts/hooks scripts/lib

# Copy starter files (no overwrite)
for starter in pending.md graduated.md gardener-log.jsonl; do
  target=".ai-lessons/$starter"
  if [[ ! -f "$target" ]]; then
    cp "$ACE_KIT_DIR/starters/$starter" "$target"
    echo -e "  ${GREEN}+${NC} $target (copied from starter)"
  else
    echo -e "  ${YELLOW}~${NC} $target (already exists, skipped)"
  fi
done

# Script symlinks
make_symlink "$REL_KIT/scripts/hooks/telemetry-hook.sh" "scripts/hooks/telemetry-hook.sh"
make_symlink "$REL_KIT/scripts/lib/reflector.sh" "scripts/lib/reflector.sh"

# Make shell scripts executable
chmod +x "$ACE_KIT_DIR/hooks/delta-guard.sh" \
         "$ACE_KIT_DIR/scripts/hooks/telemetry-hook.sh" \
         "$ACE_KIT_DIR/scripts/lib/reflector.sh" \
         "$ACE_KIT_DIR/setup.sh"

echo ""

# --- Post-setup guidance ---
echo -e "${CYAN}=== Setup Complete ===${NC}"
echo ""

echo -e "${YELLOW}Add to .gitignore:${NC}"
echo "  .ai-lessons/gardener-log.jsonl"
echo "  tests/telemetry/"
echo ""

if [[ "$SETUP_CLAUDE" == true ]]; then
  echo -e "${YELLOW}Claude Code — add hooks to .claude/settings.json:${NC}"
  cat <<'HOOKEOF'
  {
    "hooks": {
      "PostToolUse": [{
        "matcher": "Write|Edit",
        "hooks": [{
          "type": "command",
          "command": ".claude/hooks/delta-guard.sh",
          "timeout": 10,
          "async": true
        }]
      }]
    }
  }
HOOKEOF
  echo ""
fi

echo -e "${GREEN}Done.${NC} See ace-kit/docs/ace/ace-pipeline.md for full documentation."
