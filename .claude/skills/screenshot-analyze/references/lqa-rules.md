# LQA Rules and Check Tables

## Semantic Error Types

| Type | Severity | Example |
|------|----------|---------|
| **False Friends** | CRITICAL | Economy → Скупость |
| **Wrong Context** | CRITICAL | Comfort → Соболезнования |
| **Literal Translation** | ERROR | Kettlebell → Чайник+Колокол |
| **Transliteration Error** | ERROR | Ingram → Преступление |
| **Offensive/Taboo** | CRITICAL | — |
| **Ambiguous** | ERROR | Auto → Автомобиль vs Автоматически |

## Ride-Hailing Specific Checks

### Money (Currency & Pricing)

| Check | Severity |
|-------|----------|
| CLDR Currency Position (prefix/suffix) | ERROR |
| CLDR Number Separators | ERROR |
| Currency Symbol Consistency | ERROR |
| Price Format Consistency | WARNING |
| Fare/Tariff Names (do not translate literally) | ERROR |
| RTL Price Concatenation | CRITICAL |

### Time (Time & Duration)

| Check | Severity |
|-------|----------|
| ETA Format (space between number and unit) | WARNING |
| Time Format (12h/24h per locale) | ERROR |
| min vs m (m may mean meters) | ERROR |
| Complex Plurals (1/2/5 minutes) | ERROR |

### RTL Layout

| Element | RTL Behavior |
|---------|--------------|
| Back Button | Right side, arrow pointing right (>) |
| Car Icons | Do NOT mirror |
| Progress Bar | Fills from right to left |
