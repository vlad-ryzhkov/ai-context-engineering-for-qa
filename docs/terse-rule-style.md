# Terse rule style — mandatory for rules/ + SKILL.md + CLAUDE.md JIT blocks

Reader = LLM, NOT human. Cuts ~40–60% tokens. Applies to `~/.claude/rules/**`, `~/.claude/skills/**/SKILL.md`, JIT blocks in `~/.claude/CLAUDE.md`. `docs/` = human-prose OK.

- Imperative ONLY. `CHECK branch FIRST` not "the assistant should check".
- BANNED words: please, should, carefully, as mentioned, you must, in order to, the model, the assistant.
- Arrows + caps: `→`, `MUST`, `BANNED`, `STOP`, `ONLY`, `FIRST`.
- Anchors = file paths, regexes, exact tool names. NO vague refs.
- Drop articles. `Read file` not "Read the file".
- Sentences <12 words. Lists > prose. Tables for caps/maps.
- ONE example MAX per rule. Extras → `docs/`.
- English ONLY in `rules/` + `SKILL.md`.

Example: `"Before proposing rule, output verification"` → `BEFORE rule proposal → OUTPUT <verification_process>. NO proof = NO rule.`

## Two levers to shrink an oversized file (order matters)

1. **EXTRACT to references/** (structural) — move examples, algorithms, tables, rationale out of SKILL.md into `references/X.md`. Extracted content is JIT (0 start-cost). This is the FIRST lever for SKILL.md over 300 LOC. Always-on files (CLAUDE.md/rules) extract to `docs/` + a 1-line pointer.
2. **COMPRESS in place** — for what MUST stay loaded. Rewrite the prose to the style rules above by hand; keep code, URLs and structure byte-identical.

**Extract beats compress:** extracted = not loaded at all; compressed = still loaded, just smaller. Compress only the residue that can't be extracted.

## When to compress

CLAUDE.md / rules/\*.md / SKILL.md → compress after extraction. Other `.md` (docs, plans) → SOFTER: only if prose-heavy and token-pressured; docs tolerate human-prose.

Table alignment padding is pure waste in any model-loaded file; strip it when a file is over budget.
