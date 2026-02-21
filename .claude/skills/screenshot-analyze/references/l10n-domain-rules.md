# L10N Domain Rules

> **Lazy Load:** Read this file ONLY when executing `/screenshot-analyze`.
> DO NOT preload into the agent.

## Core Mindset

| Principle | Essence |
|-----------|---------|
| **CLDR Compliance** | Date, currency, number formats per regional standard |
| **Cultural Sensitivity** | False friends, offensive content, taboo |
| **Non-Translateables Respect** | Addresses, POI, brands, license plates are not translated |
| **Evidence-Based** | Every bug with exact element location on screen |

## Domain Rules

| Rule | Description |
|------|-------------|
| Currency ≠ Locale | Currency is determined by region (BR → R$), not by UI language |
| False Positive on POI | Map addresses and POI are not translated — this is not a bug |
| Vague descriptions | FORBIDDEN. Be specific: "Button truncated at 'Регистрац...'" |
| Missing translation | For non-Russian texts add a translation: `"بخيل" (Скупой)` |

## LQA Severity

| Type | Severity |
|------|----------|
| False Friends | 🔴 CRITICAL |
| Wrong Context | 🔴 CRITICAL |
| Offensive/Taboo | 🔴 CRITICAL |
| Literal Translation | 🟠 MAJOR |
| Transliteration Error | 🟠 MAJOR |
| Ambiguous | 🟠 MAJOR |

## Visual Analysis: Rules

- Never fabricate findings — only what is visible in the image
- Never copy user examples as real findings
- If unsure about what is shown — state it explicitly
- Region is determined from the filename (`en_BR.png` → Brazil)
- TOP-10 issues, 1 table, translations to Russian for non-Russian texts
