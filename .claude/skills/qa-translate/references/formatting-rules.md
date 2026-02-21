# Markdown Formatting Rules for Translation

> Mandatory rules for preserving markdown structure during RU→EN translation.
> Violating any rule is a translation defect.

---

## Rule 1: Headers

- Translate the header text
- Keep the header level (`#`, `##`, `###`, etc.) unchanged
- Keep any emoji prefix unchanged

```markdown
# Source:
## 🔒 СИСТЕМНЫЕ ТРЕБОВАНИЯ

# Translation:
## 🔒 SYSTEM REQUIREMENTS
```

## Rule 2: Fenced Code Blocks

- Keep the **entire content** of fenced code blocks (` ``` `) unchanged
- Do NOT translate comments, variable names, strings, or any content inside code blocks
- Preserve the language identifier (e.g., ` ```kotlin `, ` ```bash `)

```markdown
# Source:
\```bash
./gradlew test --tests "FullClassName"
\```

# Translation: IDENTICAL (no changes)
\```bash
./gradlew test --tests "FullClassName"
\```
```

## Rule 3: Inline Code

- Keep inline code (`` ` ` ``) content unchanged
- This includes: file names, paths, commands, field names, tool names

```markdown
# Source:
Используй `Read` для чтения файла `qa_agent.md`.

# Translation:
Use `Read` to read the file `qa_agent.md`.
```

## Rule 4: Tables

- Translate cell text content
- Preserve table structure: same number of columns, rows, and alignment markers
- Keep alignment (`:---`, `:---:`, `---:`) unchanged
- Keep inline code within cells unchanged

```markdown
# Source:
| Действие | Команда |
|----------|---------|
| Сборка   | `./gradlew build` |

# Translation:
| Action   | Command |
|----------|---------|
| Build    | `./gradlew build` |
```

## Rule 5: Links

- Translate the display text
- Keep the URL unchanged
- Keep reference-style link definitions unchanged

```markdown
# Source:
Подробности в [документации проекта](docs/setup.md).

# Translation:
Details in the [project documentation](docs/setup.md).
```

## Rule 6: Lists

- Translate the text content
- Preserve list markers (`-`, `*`, `1.`, `2.`)
- Preserve nesting indentation exactly

```markdown
# Source:
1. **Первый шаг:** выполни анализ
   - Подшаг A: прочитай файл
   - Подшаг B: проверь результат

# Translation:
1. **First step:** perform the analysis
   - Sub-step A: read the file
   - Sub-step B: verify the result
```

## Rule 7: YAML Frontmatter

- Translate **only** the `description` value
- Keep all other fields unchanged: `name`, `allowed-tools`, `agent`, `context`
- Keep the `---` delimiters unchanged

```markdown
# Source:
---
name: my-skill
description: Выполняет аудит документации.
allowed-tools: "Read Glob"
---

# Translation:
---
name: my-skill
description: Performs documentation audit.
allowed-tools: "Read Glob"
---
```

## Rule 8: Blockquotes

- Translate the text content
- Keep the `>` marker unchanged
- Preserve nested blockquote levels (`>>`, `>>>`)

```markdown
# Source:
> **Важно:** Действуют для ВСЕХ агентов и скиллов.

# Translation:
> **Important:** Applies to ALL agents and skills.
```

## Rule 9: HTML Tags

- Keep HTML tags unchanged (`<p>`, `<img>`, `<details>`, `<summary>`, etc.)
- Translate the text content between tags
- **CRITICAL:** Translate the text inside `alt="..."` and `title="..."` attributes — these are human-readable content (accessibility, tooltips)
- Keep purely technical attributes (`src`, `href`, `width`, `align`) STRICTLY unchanged (DNT)

```markdown
# Source:
<details>
<summary>Подробности реализации</summary>
Описание внутренней логики.
</details>

# Translation:
<details>
<summary>Implementation details</summary>
Description of internal logic.
</details>
```

## Rule 10: Emoji

- Keep ALL emoji unchanged
- Do NOT add new emoji
- Do NOT remove existing emoji

```markdown
# Source:
⛔ **ЗАПРЕЩЕНО:** `git reset --hard`

# Translation:
⛔ **FORBIDDEN:** `git reset --hard`
```

## Rule 11: Mixed-Language Content

- Translate only the Russian portions
- Keep English portions unchanged
- Keep technical terms, brand names, and acronyms in their original language

```markdown
# Source:
**Anti-Hallucination Rule:** Никогда не предполагай наличие поля.

# Translation:
**Anti-Hallucination Rule:** Never assume a field exists.
```

## Rule 12: Placeholder Tokens

- Keep ALL placeholder tokens unchanged: `$ARGUMENTS`, `{skill-name}`, `[Feature]`, `{YYYY-MM-DD}`
- These are template variables, not translatable content

```markdown
# Source:
Сохрани результат в `audit/spec-audit_{YYYY-MM-DD}.md`.

# Translation:
Save the result to `audit/spec-audit_{YYYY-MM-DD}.md`.
```

## Rule 13: JSON Examples

- Keep JSON keys unchanged
- Keep non-Russian string values unchanged
- Translate **only** Russian string values within JSON

```markdown
# Source:
```json
{
  "error": "email обязателен",
  "code": "VALIDATION_ERROR"
}
```

## Translation:

```json
{
  "error": "email is required",
  "code": "VALIDATION_ERROR"
}
```
```text
```

## Rule 14: Pseudo-XML Tags (Prompt Engineering)

- Preserve ALL pseudo-XML tags used for prompt engineering: `<purpose>`, `<system>`, `<context>`, `<constraints>`, `<example>`, `<thinking>`, `<scratchpad>`, etc.
- These tags define the LLM's cognitive framing — they are structural, not decorative
- Translate the text content inside them, but NEVER modify, rename, or remove the tags themselves
- Treat pseudo-XML tags the same as code — the tag names are DNT items

```markdown
# Source:
<purpose>
Интерактивное создание нового skill с пошаговым workflow.
</purpose>

# Translation:
<purpose>
Interactive creation of a new skill with step-by-step workflow.
</purpose>
```

---

## Verification Checklist

After translating a file, verify each rule:

| # | Rule | Check |
|---|------|-------|
| 1 | Headers | Same number, same levels, text translated |
| 2 | Fenced code blocks | Byte-identical content, same count |
| 3 | Inline code | Content unchanged |
| 4 | Tables | Same structure, cells translated |
| 5 | Links | Display text translated, URLs unchanged |
| 6 | Lists | Text translated, markers and nesting preserved |
| 7 | YAML frontmatter | Only `description` translated |
| 8 | Blockquotes | Text translated, `>` markers preserved |
| 9 | HTML tags | Tags unchanged, text content translated |
| 10 | Emoji | All preserved, none added/removed |
| 11 | Mixed-language | Only Russian portions translated |
| 12 | Placeholders | All tokens unchanged |
| 13 | JSON examples | Keys unchanged, Russian values translated |
| 14 | Pseudo-XML tags | Tags preserved, text content translated |
