# External Rules — Anthropic / inDriver ai-skills

Source distillation:

- `inDriver/ai-skills/docs/skill-creation-guide.md`
- `inDriver/ai-skills/.github/workflows/skill_review_prompt.md`

Only rules NOT already in Checks 0–16 are recorded here. Use this file from Checks 17–22.

---

## R1. Description must declare triggers and scope

| Field             | Required pattern                                                                |
| ----------------- | ------------------------------------------------------------------------------- |
| `Use when …`      | Explicit trigger phrase (use case + signal). Absence → CRITICAL                 |
| `Do NOT use for…` | Negative scope boundary. Absence → WARNING                                      |
| Brand words       | `claude`, `anthropic` (any case) forbidden in `name` / `description` → CRITICAL |

## R2. Imperative voice, no hedging-as-instruction

Forbidden as load-bearing directive:

- `should`, `you should`, `would be`, `it is recommended`
- `your judgment`, `as appropriate`, `based on context`, `where suitable`

Allowed in rationale / explanation lines, NEVER in the Algorithm section.

Severity: WARNING. Rationale: deterministic skills require imperatives; hedges leak ambiguity into LLM execution.

## R3. Out-of-scope refusal template

Skills that perform analysis or generation MUST include a literal refusal phrase for off-topic queries:

```text
> I review {SCOPE} only — that question is out of scope. Use {OTHER_SKILL} instead.
```

Absence → WARNING.

## R4. PII placeholders only in examples

Code/data examples MUST use placeholder tokens, not plausible values.

| Bad (CRITICAL)             | Good                |
| -------------------------- | ------------------- |
| `jane.doe@example.com`     | `<email>`           |
| `+1-555-0103`              | `<phone>`           |
| `4111 1111 1111 1111` (PAN)| `<card>` / `XXXX…`  |
| Real-looking IBAN / SSN    | `<iban>` / `<ssn>`  |

Detection: regex for emails, e164 phones, 13–19-digit numbers, IBAN / SSN patterns inside fenced code blocks AND example tables.

## R5. Section size discipline

- Longest section ≤40% of total SKILL.md lines. Else: WARNING (recommend split into `references/`).
- Heading hierarchy increments by one (`#` → `##` → `###`). Skipping levels → INFO (PP-S6 sibling).

## R6. Network access section

If SKILL.md or any script under the skill calls `curl`, `wget`, `fetch`, MCP HTTP tools, or any outbound request:

- Required `## Network Access` section listing domains + justification. Absence → ERROR.
- `curl | bash` / `wget | sh` / `source <(curl …)` patterns require pinned SHA + explicit consent verbiage. Absence → CRITICAL.

## R7. Security / Override

Forbidden directives in any instruction block:

- "ignore", "bypass", "override safety", "disable lint", "disable TLS", "skip cert verification" — without narrow, justified scope → CRITICAL.
- Symlink install into `.git/hooks/` or `.claude/settings.json` → CRITICAL (use copy + explicit update step).
- Global dev-state modification (`~/.bashrc`, `.git/hooks/`, global registries) → must list exact changes + consent. Else WARNING.

## R8. Scope discipline (one skill = one output)

| Pattern                                                                  | Severity | Action                          |
| ------------------------------------------------------------------------ | -------- | ------------------------------- |
| Single SKILL.md performs analysis AND generation as branched modes       | WARNING  | Split into 2 skills             |
| `if user asks X do A; if Y do B` branching by user intent                | WARNING  | Multi-skill bundle — decompose  |
| Two distinct deterministic output types in one Output Format block      | WARNING  | One skill, one artifact type    |

## R9. Prerequisites for MCP / external tools

If `allowed-tools` contains `mcp__*` or skill depends on a non-built-in CLI:

- `## Prerequisites` section required, listing: server / tool name, install link, expected `.claude/settings.local.json` keys.
- Absence → ERROR.

## R10. PromptPrism semantic audit

Full taxonomy in `references/promptprism.md`. Check 17 wires it in.

---

## Quick mapping to new Checks

| Rule | Check |
| ---- | ----- |
| R1   | 24a (Description hardening — extends Check 2)  |
| R2   | 24b (Imperative voice — extends Check 15)      |
| R3   | 24c (Refusal template)                         |
| R4   | 25a (PII placeholders)                         |
| R5   | 27  (Section size + heading hierarchy)         |
| R6   | 25b (Network Access)                           |
| R7   | 25c (Security override / hooks symlink)        |
| R8   | 26  (Scope discipline)                         |
| R9   | 28  (Prerequisites for MCP)                    |
| R10  | 23  (PromptPrism)                              |

---

## Out of scope (intentionally NOT adopted)

- PR-template / README "Available Skills" rules — handled by repo conventions, not skill-audit.
- Lock files inside skill directory — this project doesn't ship runnable skill packages.
- Eval-suite expectation grammar (em-dash, ≥4 char tokens) — eval framework not yet standardised here; revisit when `tests/<skill>/evals/` settles.
- Cross-IDE smoke test — single-runtime project (Claude Code).
