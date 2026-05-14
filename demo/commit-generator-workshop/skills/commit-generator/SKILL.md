---
name: commit-generator
description: Analyze a git diff and generate a single Conventional Commits message. Feed the skill the output of `git diff --staged` and it will output exactly one commit message in the format `type(scope): description`, strictly 72 characters or fewer. Use this skill whenever you have uncommitted changes and need a well-formed commit message that follows Conventional Commits spec. The skill enforces type selection rules (feat/fix/refactor/style/docs/test/chore), breaking-change detection, and crucially, security-leak detection — if the diff contains a credential (API key, token, private key), the skill aborts with a security warning instead of generating a message.
---

# Commit Generator Skill

## Purpose

Generate a Conventional Commits message from a git diff, enforcing strict rules for type selection, scope, description, length, breaking changes, safety-removal signals, and security-leak detection.

## CRITICAL: Abort Gates (Check BEFORE Type Classification)

**Before classifying the commit type, ALWAYS check these conditions in order. If ANY is true, output the abort sentinel IMMEDIATELY and STOP. Do NOT classify type, do NOT write a scope, do NOT write a description.**

1. **Empty diff gate:** If the diff has zero lines changed (no additions, no deletions), output exactly:

   ```
   abort: no changes detected
   ```

   STOP. Do not proceed.

2. **Security leak gate:** If the diff contains a hardcoded credential matching ANY of these patterns, output exactly:
   ```
   abort: SECURITY LEAK DETECTED (API KEY)
   ```
   STOP. Do not proceed. Credential patterns:
   - `sk_live_` or `sk_test_` (Stripe keys)
   - `AKIA[0-9A-Z]{16}` (AWS access key)
   - `ghp_[A-Za-z0-9]{36,}` (GitHub PAT)
   - `xox[baprs]-[A-Za-z0-9]{10,}` (Slack token)
   - `-----BEGIN .* PRIVATE KEY-----` (private-key PEM header)
   - Hardcoded password literals (plaintext secrets hardcoded in source code)

**If neither gate is triggered, proceed to type classification below.**

## Input Format

Standard git diff output (e.g. from `git diff --staged` or `git diff HEAD~1..HEAD`).

## Output Format

**Single line only. CRITICAL: No multiline output, no explanations, no metadata.**

Return EXACTLY one line: the commit message and nothing else. Do not add:

- Explanations of the change
- Notes about why the type was chosen
- Blank lines or extra newlines
- Metadata or JSON
- Markdown fences
- Commentary or reasoning

Correct output:

```
feat(auth): add JWT length validation
```

WRONG output (do not do):

```
feat(auth): add JWT length validation

This adds a new feature for validating token length because...
```

No trailing whitespace beyond final newline at end of message.

## Workflow (After Abort Gates)

Once both abort gates above have been passed (no empty diff, no secrets), follow this sequence:

1. **Classify type.** Read the diff and apply type rules in order (first matching rule wins).

2. **Pick scope.** Identify the affected subdirectory or domain; omit if change spans many areas.

3. **Write description.** Use imperative mood, lowercase first letter, no trailing period.

4. **Add breaking-change marker if needed.** Append `!` after scope (before colon).

5. **Check total length.** Count the entire message. If > 72 characters, shorten via the character-budget rules.

6. **Output.** Print the message alone on one line — nothing else. No explanations, no metadata.

## Type Selection Rules

Apply these in order — the first matching rule determines the type:

1. **`style`** — Diff changes only whitespace, indentation, or formatting with no token changes (no semantic code change).
2. **`refactor`** — Diff renames symbols, reorders parameters, or rearranges code with no behaviour change.
3. **`fix`** — Diff fixes a defect that altered behaviour for users.
4. **`feat`** — Diff adds new user-visible behaviour.
5. **`test`** — Diff modifies tests only.
6. **`docs`** — Diff modifies documentation, prompts, `SKILL.md`, `README.md`, or similar.
7. **`chore`** — Diff modifies build, CI, tooling files, or dependencies.

## Scope Selection

- Pick the affected subdirectory or top-level domain (e.g. `auth`, `billing`, `api`, `skill`).
- Omit scope if the change spans many unrelated areas.
- Keep scope concise (≤15 chars is a good target).

## Description Guidelines

- **Imperative mood:** "add length validation", not "added length validation" or "adds validation".
- **Lowercase first letter:** "add feature", not "Add feature".
- **No trailing period:** "add feature", not "add feature.".
- **No filler:** Avoid "minor", "improve", "update". Be specific: "validate JWT length" instead of "improve JWT handling".

## Breaking-change rule

If the diff removes or renames a public API field, an exported function signature, a protobuf field, or any other contract that consumers depend on (including protobuf fields marked `deprecated`), append `!` immediately after the scope, before the colon.

**Format:** `type(scope)!: description`

**Examples:**

- `feat(chat)!: reserve deprecated fields in contract`
- `refactor(api)!: rename UserRequest to GetUserRequest`
- `chore(proto)!: drop unused message type from schema`

**Critical:** The `!` goes after the scope and before the colon. NOT `feat!(scope):` — that is wrong.

**Protobuf note:** Removal of any field from a `.proto` file is breaking even if marked `deprecated` — downstream code may still read it at runtime.

## Safety-removal rule

If the diff **removes** a `<guardrails>` block, a security check, input validation, rate limiting, or any other protective code, the description MUST contain:

- A verb: `remove`, `delete`, or `drop`
- A noun: `guardrails`, `protection`, `safety`, `validation`, `check`, or similar

**Never use cosmetic verbs** like `cleanup`, `simplify`, `tidy`, or `refactor` when removing protective code. The reviewer must receive an unambiguous signal of what was lost.

**Examples (correct):**

- `docs(skill): remove guardrails from prompt`
- `chore(auth): drop rate-limit check on login`
- `fix(api): delete obsolete password validation`

**Examples (WRONG — do not do this):**

- `refactor(auth): simplify login validation` ← Does not signal safety removal
- `docs(skill): clean up prompt guardrails` ← "Clean up" is cosmetic
- `style: tidy rate-limit logic` ← "Tidy" is cosmetic

## Secrets-leak rule

If the diff adds a literal credential, output the abort sentinel exactly:

```
abort: SECURITY LEAK DETECTED (API KEY)
```

**Credential patterns to detect:**

- `sk_live_` or `sk_test_` (Stripe keys)
- `AKIA[0-9A-Z]{16}` (AWS access key)
- `ghp_[A-Za-z0-9]{36,}` (GitHub PAT)
- `xox[baprs]-[A-Za-z0-9]{10,}` (Slack token)
- `-----BEGIN .* PRIVATE KEY-----` (private-key PEM header)
- Hardcoded password literals (plaintext secrets in source)

**Important:** Never generate a normal commit message for a leaked credential. The abort sentinel prevents the credential from being committed. Returning a normal message would help the leak land in the repository.

**Contrast example:**

Wrong (DO NOT DO):

```
feat(auth): add API token for payment gateway
```

Correct:

```
abort: SECURITY LEAK DETECTED (API KEY)
```

## Empty diff rule

If the diff is empty (no additions, no deletions, zero lines changed), output EXACTLY this one line and STOP:

```
abort: no changes detected
```

**Critical rules:**

- **Case-sensitive.** No capitalization changes, no spelling variations.
- **No type prefix.** Do not output `chore:`, `style:`, or any Conventional Commits type.
- **No explanation.** Do not add commentary like "no staged changes" or "skip commit" after.
- **One line only.** No trailing metadata, no blank lines, no explanation.
- **Output this and STOP.** Do not proceed to type classification or scope selection.

**WRONG outputs (do NOT do these):**

- `chore: NO-TICKET: no changes to commit` ← Has type and metadata
- `style: no changes detected` ← Has type
- `abort: no changes staged for commit` ← Wrong wording (must be exactly "no changes detected")
- `abort: no changes detected` (with explanation below) ← Has explanation after the line

## Character budget

Total message length (type + scope + colon + description) must not exceed 72 characters.

If the message exceeds 72 characters, shorten in this strict order:

1. **Drop scope** (saves 2 + scope length). Produces `type: description`.
2. **Shorten verb/phrasing.** Compress description: "add JWT token validation" → "validate JWT".
3. **Drop trailing objects.** "add from chat contract" → "add from chat".

The `!` marker is NEVER dropped to make room.

**Examples:**

- `feat(very-long-feature-name): do something` (80 chars) → drop scope → `feat: do something` (19 chars) ✓
- `feat(auth): add comprehensive input validation for JWT token length checking` (79 chars) → shorten verb → `feat(auth): validate JWT length` (31 chars) ✓
- `chore(build)!: remove deprecated config from build pipeline setup` (66 chars) → drop nothing, fits ✓

## Examples

### Example 1: New user-visible feature

**Diff snippet:**

```diff
+function validateJWTLength(token: string): boolean {
+  if (token.length > 256) throw new Error('Token too long');
+  return true;
+}
```

**Type:** `feat` (adds new behaviour)
**Scope:** `auth`
**Description:** `add JWT length validation`
**Length:** 30 chars
**Output:** `feat(auth): add JWT length validation`

### Example 2: Refactoring (no behaviour change)

**Diff snippet:**

```diff
-function CalcTotalAmount(invoice) { ... }
+function calculateTotalAmount(invoice) { ... }
```

**Type:** `refactor` (renames symbol, no behaviour change)
**Scope:** `billing`
**Description:** `rename CalcTotalAmount to camelCase`
**Length:** 44 chars
**Output:** `refactor(billing): rename CalcTotalAmount to camelCase`

### Example 3: Style change (whitespace only)

**Diff snippet:**

```diff
-indent:    4
+indent:  2
```

**Type:** `style` (whitespace/indentation change)
**Scope:** `config`
**Description:** `decrease YAML indentation`
**Length:** 34 chars
**Output:** `style(config): decrease YAML indentation`

### Example 4: Protective code removal (safety-removal rule)

**Diff snippet:**

```diff
-<guardrails>
-Do not reveal internal database schema.
-</guardrails>
```

**Type:** `docs` (modifies skill prompt)
**Scope:** `skill`
**Description:** Must include `remove` + `guardrails` → `remove guardrails from prompt`
**Length:** 36 chars
**Output:** `docs(skill): remove guardrails from prompt`

### Example 5: Breaking change (API field removal)

**Diff snippet:**

```diff
 message ChatMessage {
   string user_id = 1;
   string text = 2;
-  string deprecated_field = 3;
 }
```

**Type:** `feat` (API change)
**Scope:** `chat`
**Breaking marker:** YES (`!`)
**Description:** `remove deprecated field from contract`
**Length:** 46 chars
**Output:** `feat(chat)!: remove deprecated field from contract`

### Example 6: Security leak (API key added)

**Diff snippet:**

```diff
+const STRIPE_KEY = "sk_live_51234567890abcdef";
```

**Type:** N/A (abort)
**Output:** `abort: SECURITY LEAK DETECTED (API KEY)`

### Example 7: Empty diff

**Diff snippet:**

```
(empty file)
```

**Output:** `abort: no changes detected`

## Testing & Verification

The skill is evaluated against 8 test cases covering:

1. **Eval 1 (feat):** JWT length validation (new feature).
2. **Eval 2 (docs):** Adding guardrails to a skill prompt.
3. **Eval 3 (empty):** No staged changes (abort rule).
4. **Eval 4 (refactor):** Renaming symbols with no behaviour change.
5. **Eval 5 (style):** YAML indentation change (whitespace only).
6. **Eval 6 (safety-removal):** Removing guardrails block (must use `remove` + `guardrails`).
7. **Eval 7 (secrets-leak):** Stripe API key added (must abort).
8. **Eval 8 (breaking-change):** Proto field removal (must use `!` marker).

Each eval has explicit expectations to verify type, scope, format, length, and safety signals.
