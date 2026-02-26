# QA Translation Glossary (RU → EN)

> **SSOT** for all `/qa-translate` term mappings.
> When translating, ALWAYS use the English term specified here.
> If a Russian term is not in this glossary — translate it naturally, but flag it for review.

---

## QA and Testing

| Russian | English | Notes |
|---------|---------|-------|
| тест-кейс | Test case | Two words, not "testcase" |
| тестовый сценарий | Test scenario | |
| дефект | Defect | Not "bug" in formal docs |
| баг | Bug | Informal contexts only |
| блокер | Blocker | |
| критический | Critical | Priority/severity level |
| основной | Major | Priority/severity level |
| минорный | Minor | Priority/severity level |
| граничные значения | Boundary values | |
| граничный сценарий | Boundary scenario | |
| покрытие | Coverage | |
| тестовое покрытие | Test coverage | |
| регрессионное тестирование | Regression testing | |
| приёмочное тестирование | Acceptance testing | |
| функциональное тестирование | Functional testing | |
| нефункциональные требования (НФТ) | Non-functional requirements (NFR) | Keep abbreviation in parentheses |
| автотесты | Automated tests | Not "autotests" |
| ручное тестирование | Manual testing | |
| фаззинг | Fuzzing | |
| мысленная песочница | Mental sandbox | Dry-run simulation concept |
| прогон | Run / Dry run | Context-dependent |
| счастливый путь | Happy path | |
| негативный сценарий | Negative scenario | |
| матрица отсутствия | Null matrix | Project-specific term |
| проверка типов | Type checking | |
| валидация | Validation | |
| верификация | Verification | |
| чек-лист | Checklist | One word |
| отчёт | Report | |

## Architecture and Process

| Russian | English | Notes |
|---------|---------|-------|
| спецификация | Specification | |
| требование | Requirement | |
| неоднозначность | Ambiguity | |
| идемпотентность | Idempotency | |
| конкурентность | Concurrency | |
| эндпоинт | Endpoint | |
| схема данных | Data schema | |
| бизнес-правило | Business rule | |
| бизнес-логика | Business logic | |
| ветка (логики) | Branch (logic) | |
| ветка (git) | Branch (git) | |
| пробел данных | Data gap | |
| противоречие | Contradiction | |
| зависимость | Dependency | |
| конфликт статуса | Status conflict | |
| распределённые системы | Distributed systems | |
| наблюдаемость | Observability | |
| обратная совместимость | Backward compatibility | |

## AI Setup and Project Structure

| Russian | English | Notes |
|---------|---------|-------|
| скилл | Skill | Refers to SKILL.md-based commands |
| агент | Agent | AI agent definition |
| оркестратор | Orchestrator | |
| артефакт | Artifact | |
| контекст | Context | |
| контекстное окно | Context window | |
| промпт | Prompt | |
| системный промпт | System prompt | |
| токен | Token | |
| токен-расход | Token usage | Not "token consumption" |
| галлюцинация | Hallucination | |
| прогрессивное раскрытие | Progressive disclosure | |
| самоулучшение | Self-improvement | |
| мета-скилл | Meta-skill | |
| справочник | Reference | As in reference file |

## Roles and Actions

| Russian | English | Notes |
|---------|---------|-------|
| роль | Role | |
| запрещено | Forbidden / Banned | Use "Forbidden" for protocols, "Banned" for tech stack |
| обязательно | Mandatory / Required | Context-dependent |
| рекомендуется | Recommended | |
| анти-паттерн | Anti-pattern | Hyphenated |
| разрешено | Allowed | |
| деструктивные команды | Destructive commands | |
| эскалация | Escalation | |
| подтверждение | Confirmation | |
| доработка | Refinement | |
| итеративная доработка | Iterative refinement | |

## Report and Audit

| Russian | English | Notes |
|---------|---------|-------|
| матрица рисков | Risk matrix | |
| вердикт | Verdict | |
| приоритет | Priority | |
| категория | Category | |
| рекомендация | Recommendation | |
| оценка качества | Quality score | |
| готово для разработки | Ready for development | |
| одобрено с исправлениями | Approved with corrections | |
| заблокировано | Blocked | |
| аудит | Audit | |
| реестр | Registry | |
| дублирование | Duplication | |
| раздутость | Bloat | |

## Prompt Engineering Directives

> These mappings apply specifically to AI prompt/instruction files (SKILL.md, agent definitions, protocols).
> Use RFC 2119 keywords (MUST, MUST NOT, SHOULD) in UPPERCASE for maximum LLM compliance.

| Russian | English | Notes |
|---------|---------|-------|
| ты ОБЯЗАН | You MUST | RFC 2119 — absolute requirement |
| обязан | MUST | Uppercase for LLM emphasis |
| запрещено | MUST NOT / FORBIDDEN | "MUST NOT" for constraints, "FORBIDDEN" for headers |
| нельзя | MUST NOT | In imperative prompt context |
| рекомендуется | SHOULD / RECOMMENDED | RFC 2119 — strong recommendation |
| не рекомендуется | SHOULD NOT | RFC 2119 |
| игнорируй | IGNORE / DROP | "DROP" for data, "IGNORE" for instructions |
| выведи / выводи | Output | Not "Print" or "Write" (avoids tool name collision) |
| шаг за шагом | Step by step | Or "Chain of Thought" in methodology context |
| молчание золота | VERBOSITY: MINIMAL | Convert idiom to directive for LLM |
| без прелюдий | No preamble | Direct constraint |
| без анонсов | No announcements | Direct constraint |
| сначала действие | Tool-first / Action-first | Prompt engineering pattern |
| краткость | Concise output / Brevity | |

## Do Not Translate [DNT]

These items MUST remain exactly as-is in the translation:

### File and Directory Names

- `SKILL.md`, `CLAUDE.md`, `README.md`, `qa_agent.md`
- `.claude/`, `.claude/skills/`, `.claude/agents/`, `.claude/protocols/`
- `references/`, `scripts/`, `assets/`, `audit/`
- `copilot-instructions.md`, `AGENTS.md`

### Skill Commands

- `/spec-audit`, `/api-isolated-tests`, `/api-tests`, `/repo-scout`
- `/screenshot-analyze`, `/doc-lint`, `/skill-audit`
- `/init-skill`, `/init-agent`, `/init-project`
- `/update-ai-setup`, `/qa-translate`

### Tool Names

- `Read`, `Write`, `Edit`, `Glob`, `Grep`, `Bash`
- `AskUserQuestion`, `Task`

### Frameworks and Libraries

- JUnit 5, Allure, Awaitility, Hamcrest, Jackson
- ktlint, Gradle, Kotlin
- OWASP, ISTQB, BABOK

### Acronyms and Technical Terms

- SSOT, PII, IDOR, NFR, DNT
- API, REST, HTTP, JSON, YAML, XML
- CI/CD, PR, CLI, IDE
- kebab-case, snake_case, SNAKE_CASE

### Brand Names

- Claude Code, Cursor, VS Code, IntelliJ IDEA
- GitHub Copilot, OpenCode, Codex
- Anthropic
