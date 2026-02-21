# Translation Examples (Few-Shot Pairs)

> Reference file for `/qa-translate`. Each example shows a Russian source segment,
> its English translation, and annotations explaining key translation decisions.

---

## Example 1: Protocol Description (Narrative + Table)

### Source (Russian)

```markdown
## Протокол вербозности

**Молчание золота:** Минимум объяснительного текста. Выводи только инструменты и блоки завершения.

**Режимы коммуникации:**

| Режим | Когда | Формат |
|------|------|--------|
| **ГОТОВО** | Задача выполнена | `✅ SKILL COMPLETE: ...` блок |
| **ПРЕДУПРЕЖДЕНИЕ** | Проблема, но продолжаю | `⚠️ WARNING: [Проблема]` |
| **СТАТУС** | Смена фазы | `🤖 Orchestrator Status` (только при смене агента/фазы) |

**Без чата:**
- Нет "Прочитаю файл" — только инструмент Read
- Нет "Сейчас выполню" — только инструмент Bash
```

### Translation (English)

```markdown
## Verbosity Protocol

**VERBOSITY: MINIMAL.** Output only tool invocations and completion blocks.

**Communication modes:**

| Mode | When | Format |
|------|------|--------|
| **DONE** | Task completed | `✅ SKILL COMPLETE: ...` block |
| **WARNING** | Problem, but continuing | `⚠️ WARNING: [Problem]` |
| **STATUS** | Phase change | `🤖 Orchestrator Status` (only on agent/phase change) |

**No chat:**
- No "I'll read the file" — only the Read tool
- No "I'll execute now" — only the Bash tool
```

### Key Decisions

1. **"Молчание золота"** → "VERBOSITY: MINIMAL" — idiom converted to direct LLM constraint (not "Silence is golden" — that's human-readable, not AI-optimal)
2. **"Режим"** → "Mode" — standard technical term (not "Regime")
3. **"Смена фазы"** → "Phase change" — concise; "change of phase" would be unnecessarily verbose
4. **Table structure** preserved exactly: same column count, same row count, same alignment
5. **Emoji** (`✅`, `⚠️`, `🤖`) kept unchanged
6. **Tool names** (Read, Bash) kept as DNT items
7. **"блок"** → "block" — lowercase, matching the informal style of the source

---

## Example 2: Algorithm Step (Imperative Instructions)

### Source (Russian)

```markdown
### 1. Статический анализ (Deep Cross-Check)
* **Key-to-Key Mapping (Метод Списков):** Ты ОБЯЗАН физически выписать два отсортированных списка:
    * **Список A:** все ключи из JSON-примера (построчно, в алфавитном порядке).
    * **Список B:** все поля из Таблицы параметров (построчно, в алфавитном порядке).
    Вычисли дельту посимвольно: `A \ B` (в JSON есть, в таблице нет) и `B \ A` (в таблице есть, в JSON нет). Любое непустое множество дельты — **Дефект 9**. Пропустить построение списков нельзя — неполный список делает анализ недействительным.
* **Constraint Verification:** Возьми каждое значение из Example Payload и проверь его против ВСЕХ ограничений таблицы (min/max длина, тип, формат, regex). Если в таблице `max: 100`, а строка в примере длиннее — **Дефект 9**.
```

### Translation (English)

```markdown
### 1. Static Analysis (Deep Cross-Check)
* **Key-to-Key Mapping (List Method):** You MUST physically write out two sorted lists:
    * **List A:** all keys from the JSON example (line by line, in alphabetical order).
    * **List B:** all fields from the Parameters table (line by line, in alphabetical order).
    Compute the delta character by character: `A \ B` (present in JSON, missing from table) and `B \ A` (present in table, missing from JSON). Any non-empty delta set is a **Defect 9**. Skipping list construction is not allowed — an incomplete list invalidates the analysis.
* **Constraint Verification:** Take each value from the Example Payload and verify it against ALL table constraints (min/max length, type, format, regex). If the table specifies `max: 100` and the example string is longer — **Defect 9**.
```

### Key Decisions

1. **"Ты ОБЯЗАН"** → "You MUST" — preserves the imperative force; RFC 2119 style
2. **"Метод Списков"** → "List Method" — parenthetical clarification, direct translation
3. **"построчно"** → "line by line" — not "per line" (matches the instructional tone)
4. **"Пропустить построение списков нельзя"** → "Skipping list construction is not allowed" — passive voice matches the prohibition style
5. **"Дефект 9"** → "Defect 9" — glossary-compliant term + unchanged number
6. **"Таблицы параметров"** → "Parameters table" — capitalized as a proper noun reference
7. **Code references** (`A \ B`, `max: 100`) preserved byte-identical
8. **Indentation and list nesting** preserved exactly

---

## Example 3: YAML Frontmatter + Mixed Content

### Source (Russian)

```markdown
---
name: spec-audit
description: Проводит глубокий QA-аудит спецификации на основе стандартов ISTQB, BABOK и OWASP. Выявляет не только архитектурные дыры, но и логические противоречия между Требованиями, Схемой данных и Примерами (Dry Run). Используй перед написанием тестов, при ревью требований или анализе спецификации на противоречия. Не используй для code review или анализа тестового кода.
allowed-tools: "Read Write Glob"
agent: agents/auditor.md
context: fork
---

## 🔒 SYSTEM REQUIREMENTS

Перед выполнением агент ОБЯЗАН:
1. Загрузить `.claude/protocols/gardener.md`
2. **ВСЕ выходные артефакты (`.md` файлы, таблицы, заголовки, примеры) — исключительно на русском языке.** Никакого English в report'е. Headers таблиц, названия колонок, примеры — всё по-русски.
```

### Translation (English)

```markdown
---
name: spec-audit
description: Performs deep QA audit of specifications based on ISTQB, BABOK, and OWASP standards. Identifies not only architectural gaps but also logical contradictions between Requirements, Data schema, and Examples (Dry Run). Use before writing tests, during requirements review, or when analyzing specifications for contradictions. Do not use for code review or test code analysis.
allowed-tools: "Read Write Glob"
agent: agents/auditor.md
context: fork
---

## 🔒 SYSTEM REQUIREMENTS

Before execution the agent MUST:
1. Load `.claude/protocols/gardener.md`
2. **ALL output artifacts (`.md` files, tables, headers, examples) — exclusively in Russian.** No English in the report. Table headers, column names, examples — all in Russian.
```

### Key Decisions

1. **YAML frontmatter** — only `description` value is translated; all other fields (`name`, `allowed-tools`, `agent`, `context`) remain unchanged
2. **"Проводит глубокий QA-аудит"** → "Performs deep QA audit" — active voice, concise
3. **"архитектурные дыры"** → "architectural gaps" — professional term (not "holes")
4. **"Используй... Не используй"** → "Use... Do not use" — imperative preserved
5. **"агент ОБЯЗАН"** → "agent MUST" — RFC 2119 style, consistent with Example 2
6. **"Никакого English в report'е"** → "No English in the report" — the mixed RU/EN slang in the source is normalized to clean English
7. **File paths** (`.claude/protocols/gardener.md`) kept as DNT
8. **Emoji** (`🔒`) preserved
9. **Inline code** (`.md`) preserved unchanged
10. **Sentence about Russian language** is translated literally — even though the instruction refers to Russian output, the *instruction itself* is translated to English as requested
