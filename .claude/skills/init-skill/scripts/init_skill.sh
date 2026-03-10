#!/usr/bin/env bash
# Generates a template directory structure for a new skill.
# Usage: bash init_skill.sh <skill-name> [--scripts] [--references] [--assets]

set -euo pipefail

SKILLS_DIR=".claude/skills"

# --- Validation ---

if [[ $# -lt 1 ]]; then
  echo "Usage: bash $0 <skill-name> [--scripts] [--references] [--assets]"
  echo ""
  echo "Examples:"
  echo "  bash $0 my-skill"
  echo "  bash $0 my-skill --scripts --references"
  exit 1
fi

SKILL_NAME="$1"
shift

# Validate kebab-case
if [[ ! "$SKILL_NAME" =~ ^[a-z][a-z0-9-]*$ ]]; then
  echo "❌ Error: skill name must be kebab-case (only a-z, 0-9, -)"
  echo "   Got: '$SKILL_NAME'"
  echo "   Example: 'my-skill', 'api-tests', 'lint-check'"
  exit 1
fi

# Reject reserved names
if [[ "$SKILL_NAME" == *claude* || "$SKILL_NAME" == *anthropic* ]]; then
  echo "❌ Error: names containing 'claude' or 'anthropic' are reserved"
  exit 1
fi

SKILL_DIR="$SKILLS_DIR/$SKILL_NAME"

# Check if already exists
if [[ -d "$SKILL_DIR" ]]; then
  echo "❌ Error: directory '$SKILL_DIR' already exists"
  exit 1
fi

# --- Parse flags ---

CREATE_SCRIPTS=false
CREATE_REFERENCES=false
CREATE_ASSETS=false

for arg in "$@"; do
  case "$arg" in
    --scripts)    CREATE_SCRIPTS=true ;;
    --references) CREATE_REFERENCES=true ;;
    --assets)     CREATE_ASSETS=true ;;
    *)
      echo "❌ Error: unknown flag: $arg"
      echo "   Allowed: --scripts, --references, --assets"
      exit 1
      ;;
  esac
done

# --- Create structure ---

echo "📁 Creating skill: $SKILL_NAME"

mkdir -p "$SKILL_DIR"

# SKILL.md template
cat > "$SKILL_DIR/SKILL.md" << 'TEMPLATE'
---
name: SKILL_NAME_PLACEHOLDER
description: TODO — formula: [What it does]. [When to use]. Do not use for [anti-examples].
---

# /SKILL_NAME_PLACEHOLDER — TODO Title

<purpose>
TODO: 1-2 sentences — what it does and for whom.
</purpose>

## When to Use

- TODO: trigger 1
- TODO: trigger 2

## Input

- TODO: what is needed from the user

## Algorithm

### Step 1: TODO
TODO: specific actions

### Step 2: TODO
TODO: specific actions

## Output Format

TODO: result template

## Quality Gates

- [ ] TODO: check 1
- [ ] TODO: check 2
TEMPLATE

# Replace placeholder with actual name
sed -i '' "s/SKILL_NAME_PLACEHOLDER/$SKILL_NAME/g" "$SKILL_DIR/SKILL.md"

# Optional directories
if [[ "$CREATE_SCRIPTS" == true ]]; then
  mkdir -p "$SKILL_DIR/scripts"
  echo "# TODO: script for $SKILL_NAME" > "$SKILL_DIR/scripts/.gitkeep"
  echo "  ├── scripts/"
fi

if [[ "$CREATE_REFERENCES" == true ]]; then
  mkdir -p "$SKILL_DIR/references"
  echo "# TODO: reference for $SKILL_NAME" > "$SKILL_DIR/references/.gitkeep"
  echo "  ├── references/"
fi

if [[ "$CREATE_ASSETS" == true ]]; then
  mkdir -p "$SKILL_DIR/assets"
  echo "# TODO: assets for $SKILL_NAME" > "$SKILL_DIR/assets/.gitkeep"
  echo "  ├── assets/"
fi

echo ""
echo "✅ Skill created: $SKILL_DIR/"
echo ""
echo "Structure:"
echo "  $SKILL_DIR/"
echo "  ├── SKILL.md"
[[ "$CREATE_SCRIPTS" == true ]]    && echo "  ├── scripts/"
[[ "$CREATE_REFERENCES" == true ]] && echo "  ├── references/"
[[ "$CREATE_ASSETS" == true ]]     && echo "  └── assets/"
echo ""
echo "Next step: edit $SKILL_DIR/SKILL.md (replace all TODOs)"
