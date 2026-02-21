# L10n Checklists

> Checklists for screenshot verification. Used by skill `/screenshot-analyze`.

---

## Geometry and Layout

- [ ] **Text Overflow:** Text does not extend beyond the container boundaries
- [ ] **Truncation:** Text is not truncated ("..." or cut off), especially in CTA buttons
- [ ] **Vertical Truncation:** Diacritical marks are not clipped — relevant for Thai, Hindi, Arabic, Myanmar
- [ ] **Line Wrapping:** Line breaks do not split number+unit (`15 <br> min` — error)
- [ ] **Element Overlap:** Elements do not overlap each other
- [ ] **Spacing Consistency:** Spacing is uniform, visual rhythm is preserved
- [ ] **Alignment:** Elements are aligned (left edge, center, right edge)
- [ ] **Font Support:** No tofu boxes (font supports all locale characters)
- [ ] **UI Scaling:** Elements are not shifted due to font differences

---

## RTL Specific (ar/he/fa/ur)

- [ ] **Text Direction:** All text flows right to left
- [ ] **Back Button Position:** On the right, arrow points right (>)
- [ ] **Navigation Icons:** Chevrons, arrows are mirrored
- [ ] **Car Icons:** NOT mirrored (car drives forward)
- [ ] **Route Direction:** Route line is mirrored
- [ ] **Progress Bars:** Fill from right to left
- [ ] **List Bullets:** To the right of text
- [ ] **Input Alignment:** Input fields are right-aligned
- [ ] **Number Consistency:** One numeral system per screen
- [ ] **Margins/Paddings:** Mirrored (left margin → right margin)
- [ ] **BiDi Isolation:** Phone numbers, emails, promo codes do not "break apart"

---

## CLDR Data Formats

- [ ] **Number Separators:** Thousands/decimal match the locale
- [ ] **Currency Position:** Prefix/suffix per CLDR
- [ ] **Currency Symbol:** Symbol or code matches the region
- [ ] **Currency Spacing:** Space between symbol and number per CLDR
- [ ] **Date Format:** DMY/MDY/YMD per locale
- [ ] **Time Format:** 12h/24h per locale
- [ ] **Distance Units:** km/mi matches the region
- [ ] **Unit Spacing:** Space between number and unit (`2.5 km`, not `2.5km`)

---

## Semantics (Meaning Errors)

- [ ] **False Friends:** Words similar in sound, different in meaning
- [ ] **Literal Translation:** Literal translation of compound words
- [ ] **Wrong Context:** Word is correct, context is wrong
- [ ] **Offensive Content:** Translation may be offensive in the culture
- [ ] **Ambiguous Terms:** Ambiguous words
- [ ] **Transliteration Errors:** Errors in name transliteration
- [ ] **Industry Terms:** Non-standard terms
- [ ] **Placeholder Variables:** `{name}`, `%s` are not displayed as text

---

## Consistency

- [ ] **Language Consistency:** All UI texts are in one language
- [ ] **Currency Consistency:** One currency symbol per screen
- [ ] **Format Consistency:** Consistent price format
- [ ] **Number System Consistency:** One numeral system
- [ ] **Terminology Consistency:** Consistent terms
- [ ] **Register Consistency:** Consistent style (formal/informal)

---

## UX Quality

- [ ] **Readable Text:** Text is readable (contrast, size)
- [ ] **Close/Back Available:** There is a way to close the screen or go back
- [ ] **Clear CTA:** Action button is clear and correctly translated
- [ ] **Error Context:** Errors with explanation of what to do
- [ ] **Loading State:** Loading state with indicator

---

## N/A Markers (Non-Applicable Checks)

Mark as **N/A** with reason:

| Check | When N/A |
|-------|----------|
| Keyboard Overlap | Keyboard is not shown |
| Empty State | Screen has data |
| Error Context | No errors on screen |
| Loading State | No loading state |
| Date Format | No dates on screen |
| Time Format | No time on screen |
| RTL Checks | Not an RTL locale |

**N/A does not reduce coverage score** — count only relevant items.

---

## Self-Check Protocol (before completion)

- [ ] **Locale specified:** Do all issues specify the target locale?
- [ ] **Location provided:** Does every issue have a screen location?
- [ ] **Severity assigned:** Does every issue have a severity?
- [ ] **Suggestion actionable:** Is every suggestion specific?
- [ ] **No guessing:** Are all conclusions based on visible data?
- [ ] **RTL checked:** For RTL locales, have all RTL-specific items been checked?
- [ ] **CLDR validated:** Have formats been checked against CLDR tables?
- [ ] **No false positives:** Is regional currency not flagged as a bug?
- [ ] **No style nitpicking:** No nitpicking on style without a semantic error?
- [ ] **Cross-comparison done:** If multiple locales — is there a comparison table?
