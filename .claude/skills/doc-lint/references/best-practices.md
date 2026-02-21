# Industry Documentation Practices

Brief excerpts from industry standards that underpin the doc-lint rules.

---

## 1. Google Technical Writing

- **Single Source of Truth (SSOT):** Each fact — in exactly one place. The rest — reference it.
- **One idea per sentence:** One sentence = one idea. Easier to translate, easier to test.
- **Link, don't duplicate:** If the content already exists — add a link, don't copy.

> Source: [Google Technical Writing](https://developers.google.com/tech-writing)

---

## 2. Amazon 6-Pager

- **Length discipline:** A strict limit forces prioritization.
- **Narrative over slides:** Structured text > scattered bullet points.
- **"Working backwards" principle:** Start with the outcome for the reader, then details.

> Source: Amazon Leadership Principles, internal docs practice

---

## 3. Diataxis Framework

Four document types — do not mix in a single file:

| Type | Purpose | Orientation |
|------|---------|-------------|
| **Tutorial** | Teach | Learning-oriented |
| **How-to** | Solve a task | Task-oriented |
| **Reference** | Provide facts | Information-oriented |
| **Explanation** | Explain "why" | Understanding-oriented |

> Source: [diataxis.fr](https://diataxis.fr)

---

## 4. Microsoft Docs

- **200-800 lines:** Ideal range for a single document.
- **Consistent hierarchy:** H1→H2→H3, no skipped levels.
- **Scannable:** Headers, lists, tables — the reader should find what they need within 30 seconds.
- **TOC for long documents:** >200 lines require a table of contents.

> Source: [Microsoft Style Guide](https://learn.microsoft.com/style-guide)

---

## 5. GitLab Handbook

- **DRI (Directly Responsible Individual):** Every document has one responsible person.
- **Link, don't duplicate:** Strict rule: if duplication is found — merge request to delete the copy.
- **Single source of truth:** Handbook is the only source of truth, wiki is FORBIDDEN.

> Source: [GitLab Handbook](https://handbook.gitlab.com)

---

## 6. Stripe Docs

- **Cross-reference instead of copying:** Each code snippet / table lives in one place.
- **Progressive disclosure:** Basic example → advanced options → edge cases.
- **Versioning:** Documentation is tied to API version.

> Source: [Stripe API Docs](https://stripe.com/docs/api)

---

## Synthesis for doc-lint

| Practice | Rule in doc-lint |
|----------|------------------|
| SSOT (Google, GitLab) | Cross-file duplicate detection + SSOT Owner |
| Length discipline (Amazon, Microsoft) | Size thresholds per file type |
| Do not mix types (Diataxis) | Mixed Diataxis type detection |
| Consistent headings (Microsoft) | Heading hierarchy check |
| Cross-reference (Stripe, GitLab) | Recommendation: "link instead of copy" |
| Progressive Disclosure (Stripe) | Already implemented in skill architecture |
