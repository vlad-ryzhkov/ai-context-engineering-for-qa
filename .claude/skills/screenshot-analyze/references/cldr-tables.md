# CLDR Reference Tables

> Reference tables for format verification. Used by skill `/screenshot-analyze`.

---

## Currency Formats by Region

| Region | Code | Currency | Format | Example |
|--------|------|----------|--------|---------|
| Brazil | BR | BRL | R$ #.###,## | R$ 1.234,56 |
| Russia | RU | RUB | # ###,## ₽ | 1 234,56 ₽ |
| USA | US | USD | $#,###.## | $1,234.56 |
| Germany | DE | EUR | #.###,## € | 1.234,56 € |
| UK | GB | GBP | £#,###.## | £1,234.56 |
| Saudi Arabia | SA | SAR | # ###.## ر.س | ١٬٢٣٤٫٥٦ ر.س |
| Mexico | MX | MXN | $#,###.## | $1,234.56 |
| Indonesia | ID | IDR | Rp #.### | Rp 1.234.567 |
| India | IN | INR | ₹#,##,###.## | ₹1,23,456.78 |

---

## Number Formats by Locale

| Locale | Thousands | Decimal | Example |
|--------|-----------|---------|---------|
| en-US | , | . | 1,234.56 |
| de-DE | . | , | 1.234,56 |
| ru-RU | (space) | , | 1 234,56 |
| fr-FR | (space) | , | 1 234,56 |
| pt-BR | . | , | 1.234,56 |
| ar-SA | ٬ | ٫ | ١٬٢٣٤٫٥٦ |

---

## Time Formats by Locale

| Locale | Format | Example |
|--------|--------|---------|
| en-US | 12h | 2:00 PM |
| en-GB | 24h | 14:00 |
| de-DE | 24h | 14:00 Uhr |
| ru-RU | 24h | 14:00 |
| ar-SA | 12h/24h | ٢:٠٠ م / ١٤:٠٠ |

---

## Address Formats by Region

| Region | Format | Example |
|--------|--------|---------|
| BR | {street}, {number} - {district} | Rua Augusta, 123 - Consolação |
| RU | {street}, д. {number} | ул. Пушкина, д. 10 |
| US | {number} {street} | 123 Main Street |
| DE | {street} {number} | Hauptstraße 42 |
| SA | {number} {street} | ٢٠ شارع الملك فهد |

---

## Numeral Systems (Critical for RTL)

| System | Symbols | Locales |
|--------|---------|---------|
| Western Arabic | 0 1 2 3 4 5 6 7 8 9 | Most |
| Eastern Arabic | ٠ ١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩ | ar-SA, ar-EG |
| Persian | ۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹ | fa-IR |

**RULE: Do not mix numeral systems on a single screen!**

---

## Layout Expansion Reference

| Language | Typical expansion vs EN | Typical issue |
|----------|-------------------------|---------------|
| German (de) | +30-40% | Truncation in buttons |
| Russian (ru) | +20-30% | Truncation in labels |
| Spanish (es) | +20-30% | Overflow in headers |
| Portuguese (pt) | +20-30% | Overflow in CTA |
| Arabic (ar) | ~same length | RTL issues, semantic errors |
| Chinese (zh) | -30-50% shorter | Requires different line-height |
| Japanese (ja) | ~same/-10% | Mixing 3 writing systems |
| Korean (ko) | ~same | Spacing issues |

**Layout Stress Test Priority:**
1. DE (maximum expansion)
2. RU, ES, PT-BR (significant expansion)
3. AR (RTL + semantic issues)
4. ZH, JA (typography issues)

---

## Plural Rules Reference (CLDR)

| Locale | Forms | Rule | Example (minutes) |
|--------|-------|------|-------------------|
| en | 2 | one, other | 1 minute, 2 minutes |
| ru | 3 | one, few, many | 1 минута, 2 минуты, 5 минут |
| ar | 6 | zero, one, two, few, many, other | دقيقة, دقيقتان, دقائق... |
| pl | 3 | one, few, many | 1 minuta, 2 minuty, 5 minut |
| uk | 3 | one, few, many | 1 хвилина, 2 хвилини, 5 хвилин |
| zh/ja/ko | 1 | other (no plural) | 1分钟, 5分钟 |

**Typical error:** `if (n > 1) + "s"` — for Russian this produces "5 минуты" instead of "5 минут".

**Numbers for testing:** 1, 2, 5, 11, 21, 22, 25 — cover all forms.

---

## Quick Reference: RTL Locales

| Code | Language | Digits | Notes |
|------|----------|--------|-------|
| ar-SA | Arabic (Saudi) | ٠١٢٣٤٥٦٧٨٩ or 0123456789 | Full RTL, Eastern Arabic digits optional |
| ar-EG | Arabic (Egypt) | 0123456789 more common | RTL, Western digits more widespread |
| he-IL | Hebrew | 0123456789 | RTL, Western digits only |
| fa-IR | Persian | ۰۱۲۳۴۵۶۷۸۹ | RTL, Persian digits |
| ur-PK | Urdu | ۰۱۲۳۴۵۶۷۸۹ | RTL, Persian digits |

---

## Quick Reference: Text Expansion

| Source language | Target language | Expansion Factor |
|-----------------|-----------------|------------------|
| EN | DE | 1.3-1.4x |
| EN | RU | 1.2-1.3x |
| EN | ES | 1.2-1.3x |
| EN | PT-BR | 1.2-1.3x |
| EN | FR | 1.15-1.25x |
| EN | AR | 0.9-1.1x |
| EN | ZH | 0.5-0.7x |
| EN | JA | 0.8-1.0x |

**Rule of Thumb:** If DE translation is NOT 30% longer than EN — check for truncation.

---

## Country Code Mapping

| Code | Name | Currency |
|------|------|----------|
| BR | Brazil | R$ (BRL) |
| RU | Russia | ₽ (RUB) |
| SA | Saudi Arabia | SAR |
| US | United States | $ (USD) |
| MX | Mexico | $ (MXN) |
| ID | Indonesia | Rp (IDR) |
| IN | India | ₹ (INR) |
| EG | Egypt | E£ (EGP) |
| PK | Pakistan | Rs (PKR) |
