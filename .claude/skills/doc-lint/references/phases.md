# Documentation Analysis Phases

## Phase 1: Discovery & File Inventory

**Goal:** Collect the file catalog, excluding heavy and service directories.

1. **Glob Pattern:** `**/*.md`, `**/*.yaml`, `**/*.yml`, `**/*.txt`
2. **Exclusions (Blacklist):**
   - System: `node_modules/`, `.git/`, `.gradle/`, `build/`, `dist/`, `vendor/`, `.claude/`
   - Binary/Lock: `*.lock`, `*.bin`, `*.jar`, `*.png`, `*.jpg`
   - **Generated reports (IMPORTANT):** `audit/` (to avoid linting reports from previous runs)
   - **Archive/Specifications:** `specifications/` (historical data), `legacy/`
3. **Smart Filtering:**
   - If the user did not specify `Scope`, ignore files in root `.github/` (usually templates)
4. **Inventory Step:**
   - Use `wc -l` for line counting (DO NOT USE `read` for this step, save tokens).
   - Classify files by path.
   - For each file determine:
     - Path (relative)
     - Line count (`wc -l`)
     - Type classification (per rules from `references/check-rules.md` § 1)
5. Build the inventory table:

```markdown
| # | File | Lines | Type | Status |
|---|------|------:|------|--------|
| 1 | CLAUDE.md | 107 | CLAUDE.md | — |
```

**Checkpoint:** All files in scope found, line counts verified.

---

## Phase 2: Size Analysis

**Goal:** Identify files exceeding thresholds.

1. Load thresholds from `references/check-rules.md` § 1
2. For each file in the inventory:
   - Determine applicable threshold by file type
   - Compare line count against thresholds
   - Assign severity: **OK** / **WARNING** / **CRITICAL**
3. Update the Status column in the inventory

**Formula:**
```text
If lines > CRITICAL threshold → CRITICAL
If lines > WARNING threshold → WARNING
Otherwise → OK
```

---

## Phase 3: Structure Analysis

**Goal:** Verify the internal structure of each file.

For each .md file:

**3.1 Heading Hierarchy**
- Extract all headings (`# `, `## `, `### `, ...)
- Check for skipped levels: H1→H3 (skipping H2) → **CRITICAL**
- Check depth: >H4 → **INFO** "Consider restructuring"

**3.2 Section Balance**
- Count lines between headings
- If one section > 40% of the entire file → **WARNING**

**3.3 Empty Sections**
- Header → next header with no content (only whitespace) → **WARNING**

**3.4 TOC Check**
- File >200 lines without `## Table of Contents`, `## Содержание`, `## TOC` → **INFO**

**3.5 Readability**
- Wall-of-text: >20 consecutive lines without headers/lists/blank lines/code blocks → **WARNING**
- Lines >200 characters → **INFO**

---

## Phase 4: Cross-File Duplicate Detection

**Goal:** Find content duplication between files. Key phase.

### 4.1 Block Extraction

For each file extract semantic blocks:
- Tables (from `|` to end of table)
- Code blocks (from ``` to ```)
- Lists (consecutive lines with `- `, `* `, `1. `)
- Paragraphs (>3 consecutive lines)

### 4.2 Known Pattern Matching (fast pass)

Load patterns from `references/check-rules.md` § 2.
For each pattern KP-1..KP-5:

1. Grep by signature
2. Collect files with matches
3. If files ≥2 → record duplicate cluster

### 4.3 Heuristic Cross-Comparison

**IMPORTANT:** Compare content ONLY for files that fell into the same cluster in step 4.2 (Grep match). Do not perform full pairwise comparison of the entire project (risk of combinatorial token explosion).

For tables (within cluster):
1. Compare header rows (lines with `|`)
2. If headers match >70% → compare content
3. Content matches >70% → **WARNING** near-duplicate

For code blocks and lists (within cluster):
1. Normalize per rules from `references/check-rules.md` § 5
2. Exact match ≥5 lines → **CRITICAL**
3. Exact match 3-5 lines → **WARNING**

### 4.4 Intra-file Duplicates

Within a single file:
- Repeating sections (identical headings + similar content)
- Repeating tables
- Copy-paste paragraphs

### 4.5 SSOT Owner Assignment

For each duplicate cluster:
1. Determine content category per `references/check-rules.md` § 3
2. Assign SSOT Owner
3. Formulate recommendation: "Keep in {Owner}, replace with link in the rest"

---

## Phase 5: Content Hygiene

**Goal:** Find content issues.

**5.1 Markers**
- `TODO`, `FIXME`, `HACK`, `XXX`, `TEMP` → **INFO**

**5.2 Broken Internal Links**
- Find all `[text](path)` where path is a relative path
- Check file existence → not found → **CRITICAL**
- Empty links `[text]()` or `[](path)` → **WARNING**

**5.3 Stale Dates**
- Dates in YYYY-MM-DD format older than 6 months from the current date → **INFO** "Potentially stale"

**5.4 Diataxis Type Mix**
- Load markers from `references/check-rules.md` § 4
- If file contains markers of ≥2 types → **INFO**

---

## Phase 6: Report Generation

**Goal:** Compile all findings into a structured report.

### 6.2 Safe Fix Script Generation

Generate a Bash script `audit/safe-fix.sh` with safe automatic fixes.

**Safe (automatic):**
- Adding `## Table of Contents` (if missing and file >200 lines)
- Creating empty stub files for broken links (marked with `# TODO: Content needed`)
- Removing trailing spaces

**Manual (require human):**
- Removing duplicates (risk of losing context)
- Splitting files into parts
- Content refactoring

The script MUST contain:
1. Shebang `#!/usr/bin/env bash`
2. Safety header with warning
3. Dry-run mode by default (`--apply` flag to execute)
4. Each action with a comment and echo before execution

**Example structure:**
```bash
#!/usr/bin/env bash
set -euo pipefail

echo "🔧 Safe Fix Script for Doc-Lint Report"
echo "Run with --apply to execute changes"

DRY_RUN=true
[[ "${1:-}" == "--apply" ]] && DRY_RUN=false

# Fix 1: Add TOC to large files
if [ "$DRY_RUN" = false ]; then
  # actual fix command
else
  echo "[DRY-RUN] Would add TOC to file.md"
fi
```

---

## Phase 7: Generate Safe-Fix Script

**Goal:** Create `audit/safe-fix.sh` that orchestrates fixes using reliable tools.

1. Create the file `audit/safe-fix.sh` with shebang `#!/usr/bin/env bash` and `set -euo pipefail`.
2. **TOC (Table of Contents) logic:**
   - Check if the utility `.claude/scripts/generate-toc.sh` exists.
   - If the utility exists: add a command to the script to invoke it for all files where Warning "No TOC" was found.
     Example: `.claude/scripts/generate-toc.sh "$file" || echo "⚠️  Failed to generate TOC for $file"`
   - If the utility DOES NOT exist: add a command to insert *only* a placeholder using simple `sed`.
     Example: insert `## Table of Contents\n\n*TODO: Auto-generate TOC*\n` after the H1 heading.
3. **Broken links logic:**
   - If broken links were found (CRITICAL), add commands `mkdir -p $(dirname path/to/missing.md) && touch path/to/missing/file.md` and `echo "# TODO: Created by doc-lint" > ...`.
4. Make the script executable (`chmod +x`).

**Important:** Do not attempt to generate complex Bash code for parsing Markdown headings inside this script. Use an external utility (`.claude/scripts/generate-toc.sh`) or leave this task to the IDE (via placeholder).

**Checkpoint:** Script created, executable permissions set, uses static utilities where possible.
