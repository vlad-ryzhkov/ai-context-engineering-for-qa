---
name: screenshot-analyze
description: Analyzes mobile app screenshots for L10N defects (translations, CLDR formats, RTL). Use for UI localization verification when you need to find translation errors, date/currency format issues, or RTL layout problems. Do not use for functional UI testing or code analysis.
allowed-tools: "Read Write Edit Glob Grep Bash(open*)"
agent: agents/auditor.md
context: fork
---

# L10n & I18n UI Audit (Ride-Hailing)

Visual cross-localization UI analysis for ride-hailing applications.

**Focus:** Money, Time, Geolocation, RTL + **TRANSLATION SEMANTICS**.

## Argument Parsing (MUST — Step 0)

Look for the screenshot path by priority — stop at the first match:

**1. Skill args** — any text after the command name. Examples:
- `/screenshot-analyze resources/screenshots/brazil_passenger_main_screen` → `resources/screenshots/brazil_passenger_main_screen`
- `/screenshot-analyze /abs/path/screens` → `/abs/path/screens`

**2. Glob search (fallback)** — if args do not contain a path, run `Glob("**/*.{png,jpg,jpeg,gif,webp}")`, group by directories, pick the directory with the most images.

**3. Ask the user** — only if steps 1-2 yielded no result.

**STRICTLY FORBIDDEN** to ask about the path if steps 1-2 found a path or files.

---

## Scope (What We Check)

```text
1. SEMANTICS (Translation Quality) ← TOP PRIORITY
   - Semantic errors (false friends, wrong context)
   - Offensive/dangerous content
   - Literal translation of compound words

2. LAYOUT
   - Truncation, Overflow, Overlap
   - RTL Mirroring

3. FORMATS (CLDR Compliance)
   - Numbers, Currency, Date, Time, Distance

4. CONSISTENCY
   - Single language, numeral system, terminology
```

---

## Core Principles

### 1. Translation Verification FIRST

**PRIMARY OBJECTIVE — VERIFY TRANSLATIONS FOR SEMANTIC ERRORS.** Actively look for:
- **False Friends** — similar words with different meanings
- **Wrong Context** — correct word, wrong context
- **Literal Translation** — literal translation of idioms/terms
- **Offensive Content** — offensive/taboo content

### 2. Container vs Content

Besides translation, check: containers (does the text fit) and formats (CLDR). Do not nitpick style unless there is a semantic error.

### 3. RTL Mirroring

In RTL locales (ar, he, fa, ur) the entire UI is mirrored. Mirror: margins, navigation arrows, progress bars. DO NOT mirror: cars, maps.

### 4. Currency ≠ Locale

Currency is determined by **region**, not language. inDrive Brazil → R$ (BRL) in ANY UI language. DO NOT flag R$ in a ru-RU interface as a bug.

---

## Input Requirements

| Parameter | Required | Description |
|-----------|----------|-------------|
| Screenshot path | **Required** | File or directory (JPEG, PNG, GIF, WebP) |
| Region | **Auto / Ask** | From filename (`_BR`, `_RU`) or ask |
| Target locale | Auto | From filename (`ru_BR.png` → locale=ru, region=BR) |

### Image Requirements

| Parameter | Limit |
|-----------|-------|
| File size | ≤5 MB |
| Resolution | ≤8000×8000 px |
| Optimal | ≤1568 px on the longest side |
| Minimum | ≥200 px |

---

## Region Detection

### Filename Format

```text
{locale}_{REGION}.png

Examples:
- en_BR.png → Locale: en, Region: BR (Brazil)
- ru_BR.png → Locale: ru, Region: BR (Brazil)
- ar_SA.png → Locale: ar, Region: SA (Saudi Arabia)
```

**If region is detected from files — DO NOT ask questions.**

### If region is NOT detected — ask:

"What region are these screenshots from?"
- Brazil (BR) — R$, km
- Russia (RU) — ₽, km
- Saudi Arabia (SA) — SAR, RTL, km
- USA (US) — $, mi

---

## Analysis Algorithm (4-Step)

### Step 1: Reference Check

1. Find EN screenshot — this is the Base Layout
2. Memorize the structure: element count, positions, sizes

### Step 2: Layout Stress Test

1. Find languages with maximum length: DE, RU, ES, PT-BR
2. Check each for: Overflow, Truncation, Line Break Issues

### Step 3: Bi-Directional Check (RTL)

For AR/HE/FA/UR:
1. Margins/Paddings mirroring
2. Navigation arrow direction
3. Back/Close button position
4. Cars are NOT mirrored

### Step 4: Data Format Validation (CLDR)

1. Numbers: thousands and decimal separators
2. Currency: symbol, position, spacing
3. Time: 12h vs 24h

**Reference tables:** `references/cldr-tables.md`

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the artifact (MD/HTML), not into chat.

**Chat output (limits):**
- Brief Summary: max 5 lines (what was found, count, conclusion)
- Findings table: max 15 lines (top by severity)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress per file. Checkpoint only on:
- Phase transition (Phase N → Phase N+1)
- Blocker detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, without "Now I will grep..."
- Read → analyze → report, without "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist), not a separate file.

**Steps 1-4:** Silent. **Output:** Master Issues Table (max 10) + HTML report opened.

---

## LQA & Domain-Specific Checks

Full check tables (semantic error types, currencies, time, RTL) — in `references/lqa-rules.md`.

**Key priorities:**
- **CRITICAL:** False Friends, Wrong Context, Offensive Content, RTL Price Concatenation
- **ERROR:** Literal Translation, CLDR violations, incorrect time formats
- **WARNING:** Stylistics, minor format issues

---

## Limits (Safety Limits)

### Max Issues: 10

Limit the report to **TOP-10 most critical issues**.
Priority: CRITICAL → ERROR → WARNING.

### Grouping

Same error across multiple screenshots = **1 entry** listing all files.

### Translation to Russian

For non-Russian texts **add a translation**:
```text
✅ "إيجاد الضحايا" (Найти жертв) — dangerous translation
```

### Currency Focus

**MUST** compare currency format:
- Symbol (R$ vs BRL)
- Position (R$13 vs 13R$)
- Number format (R$13 vs R$13.00)

---

## Severity Guidelines

| Severity | Criterion |
|----------|-----------|
| **CRITICAL** | Offensive content, complete functionality breakage |
| **ERROR** | Incorrect translation changing meaning, truncated CTA |
| **WARNING** | Stylistics, minor format issues |
| **INFO** | Improvement recommendations |

---

## Anti-Patterns (BANNED)

1. **Vague Descriptions** → specify element + details: "Button text truncated at 'Регистрац...'"
2. **Currency Region False Positive** → regional currency (R$ in BR) is correct in any UI language
3. **Translating Non-Translateables** → addresses, POI, brands from maps are not translated
4. **Missing Location** → every bug with element location: "CTA button (bottom of screen)"

---

## Output Format

Output to chat only:
```text
📊 L10n: {N} issues (CRITICAL: X, ERROR: Y) → analysis-report.html
```

**Limits:**
- 0 errors → *"✅ No issues found"*

### HTML Report

Generated in the screenshots directory.

**Title:** `L10n Report — {COUNTRY_NAME}`
**Structure:** Summary Cards → Context Banner → Per-Screenshot Sections

**Template:** `references/html-template.md`

**MUST:** Open the report with `open {path}/analysis-report.html`

---

## Cross-Screenshot Comparison

For multiple locales — comparison table:

```markdown
| Element | EN (ref) | RU | AR | Issue |
|---------|----------|----|----|-------|
| CTA Button | Find offers | Найти ✅ | ❌ | AR: Literal translation |
| Price | R$13 | R$13 ✅ | R$13 ✅ | — |
| Back Button | < | < ✅ | ❌ < | AR: Should be > |
```

---

## Self-Check Protocol

Before completion, verify:
- [ ] Locale specified for all issues
- [ ] Location specified for all issues
- [ ] Severity assigned
- [ ] No false positives (regional currency)
- [ ] Non-Russian texts with translation

**Full checklist:** `references/checklists.md`

---

## Non-Translatable Elements

| Element | Rule |
|---------|------|
| Usernames | As entered |
| Car makes/models | Global brands |
| Map addresses | Local language of the place |
| POI | Language of the place |
| License plates | Regional format |

---

## Related Files

| File | Contents |
|------|----------|
| `references/cldr-tables.md` | CLDR tables: currencies, numbers, time, plurals |
| `references/checklists.md` | Full checklists: RTL, Layout, CLDR, Semantics |
| `references/html-template.md` | HTML report template |
