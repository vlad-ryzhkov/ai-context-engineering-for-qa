---
name: fix-markdown
description: Fix all markdownlint errors in .md files across the repository. Use when markdown linting fails in CI or before committing documentation changes.
allowed-tools: "Read Edit Bash(npx*)"
---

# Fix Markdown Lint Skill

Fix all markdownlint errors in .md files across the repository.

## Rules

- Only fix formatting issues
- Do not change content meaning
- Do not add tables or restructure sections
- Do not rewrite or rephrase text

## Steps

1. Auto-fix what's possible:

   ```bash
   npx markdownlint-cli --fix "**/*.md" --ignore node_modules --ignore ".gradle" --ignore build --ignore audit
   ```

2. Re-run to find remaining issues:

   ```bash
   npx markdownlint-cli "**/*.md" --ignore node_modules --ignore ".gradle" --ignore build --ignore audit
   ```

3. Fix remaining errors manually using Edit tool (line length, heading levels, inline HTML)
4. Verify: re-run step 2, expect zero errors

> **Gardener**: If you noticed rule drift or improvements during this run, briefly note it here.
