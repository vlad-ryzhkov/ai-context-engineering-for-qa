---
name: commit-generator
description: |
  Analyze a git diff and generate a single Conventional Commits message. Input a standard git diff (e.g., from `git diff --staged`). Output ONLY the commit message—no explanations, no markdown, no preamble.

  Use this skill to generate commit messages from diffs. Always generate message in Conventional Commits format with mandatory ticket ID, type, optional scope, and description. Detects breaking changes, security leaks, and empty diffs. Respects 72-character budget after ticket slot insertion.
---

## Output Format

Output ONLY the commit message. No markdown fences, no explanations, no trailing whitespace. Single line, exactly as written to `git commit -m "..."`.

## Ticket-ID Block

**Ticket ID is mandatory.** Format: `type(scope): TICKET-ID: description` where `TICKET-ID` matches `[A-Z]{2,}-[1-9]\d*` (e.g., `CORE-1234`, `JWT-42`, `PAY-7`).

If no ticket ID can be inferred from the diff (file paths, branch hints, code comments), substitute the literal placeholder `NO-TICKET` — never make one up.

**Ticket slot is non-optional even when scope is omitted:**

- `feat: JWT-42: add length validation` ✓
- `chore: NO-TICKET: tidy logging` ✓
- `feat: add length validation` ✗ (missing ticket)

**Examples:**

- `feat(auth): JWT-42: add length validation for JWT tokens`
- `refactor(billing): NO-TICKET: rename CalcTotal and tidy signature`

## Type Selection

Apply in order—first matching rule wins:

1. **style** — Diff changes only whitespace, indentation, or formatting (no token changes).
2. **refactor** — Diff renames symbols, reorders parameters, or rearranges code with no behaviour change.
3. **fix** — Diff fixes a defect that altered behaviour for users.
4. **feat** — Diff adds new user-visible behaviour.
5. **test** — Diff modifies tests only.
6. **docs** — Diff modifies docs, prompts, `SKILL.md`, or `README.md`.
7. **chore** — Diff modifies build, CI, or tooling files.

## Scope (Optional)

Scope is the affected subdirectory or top-level domain (e.g., `auth`, `skill`, `billing`, `server`). Omit if the change spans many areas. Format: `type(scope): TICKET-ID: description`.

## Description Rules

- Imperative mood: `add`, not `adds` or `added`.
- Lowercase first letter.
- No trailing period.
- Concise: focus on the what, not the why (why goes in commit body).

## Breaking-change rule

If the diff removes or renames a **public API field**, an **exported function signature**, a **protobuf field** (including those marked `deprecated`), or any other **contract consumers depend on**, append `!` **AFTER the scope and BEFORE the colon**.

**Format:** `feat(chat)!: TICKET-NNN: ...` (NOT `feat!(chat):` or `feat!: TICKET-NNN:`)

**Examples of breaking changes:**

- Removing a field from a `.proto` file (wire-level API change, even if marked `deprecated`).
- Renaming an exported function: `func OldName()` → `func NewName()`.
- Removing a query parameter from a REST endpoint.
- Changing the return type of a public method.

**Non-breaking changes that resemble breaking:**

- Removing a private method (not exported).
- Reordering fields in a struct with the same serialization (JSON field order doesn't matter).
- Removing unused function parameters if the function is internal-only.

**Output examples:**

- `feat(chat)!: CM-1933: drop deprecated fields from chat contract`
- `refactor(api)!: API-2847: rename PublicFoo to PublicBar across all endpoints`

## Secrets-leak rule

If the diff ADDS a literal credential matching any of these patterns, output **EXACTLY**:

```
abort: SECURITY LEAK DETECTED (API KEY)
```

**NEVER write a normal commit message for a leaked secret.** The output serves as a blocker signal.

**Credential patterns (abort if found):**

- `sk_live_` (Stripe live key)
- `sk_test_` (Stripe test key)
- `AKIA[0-9A-Z]{16}` (AWS access key)
- `ghp_[A-Za-z0-9]{36,}` (GitHub personal access token)
- `xox[baprs]-…` (Slack token)
- `-----BEGIN .* PRIVATE KEY-----` (PEM-format private key)
- Hardcoded password literal (plaintext string assigned to `password`, `secret`, `apiKey`, or similar variable)

**Worked example:**

- ❌ Wrong: `feat(auth): add API token for payment gateway` (accepts the secret silently)
- ✓ Correct: `abort: SECURITY LEAK DETECTED (API KEY)` (blocks the commit, alerts reviewer)

## Safety-removal rule

If the diff **REMOVES** a `<guardrails>` block, a security check, a safety assertion, or any other protective code, the description MUST contain:

- **A removal verb:** `remove`, `delete`, or `drop`.
- **A safety noun:** `guardrails`, `protection`, `safety`, `check`, or `assertion`.

**Never use cosmetic verbs like `cleanup`, `simplify`, or `tidy` when protection is being removed.** The reviewer needs an unambiguous signal.

**Examples:**

- ✓ `docs(skill): NO-TICKET: remove guardrails block from prompt`
- ✓ `chore(auth): AUTH-508: drop password complexity check`
- ❌ `docs(skill): NO-TICKET: tidy prompt formatting` (if removing guardrails—too vague)
- ❌ `chore(auth): AUTH-508: simplify validation` (if removing a check—obfuscates the change)

## Character budget

The ticket prefix typically eats 8–12 characters. Max total: **72 characters**. After inserting the ticket slot, if the message exceeds 72 chars, shorten in this strict order:

1. **Drop scope** — `type: TICKET-NNN: description` (saves 2 + scope length).
2. **Shorten description verb/phrasing** — `add length validation for JWT tokens` → `validate JWT length`.
3. **Drop trailing object words** — `remove deprecated fields from chat contract` → `remove deprecated fields from chat`.

**The ticket slot and `!` marker are NEVER dropped to make room.**

**Examples of fitting to 72 chars:**

- `feat(auth): JWT-42: validate JWT length` (38 chars) ✓
- `docs: NO-TICKET: add guardrails to prompt template` (51 chars) ✓
- `feat(chat)!: CM-1933: drop deprecated fields from chat` (54 chars) ✓

## Workflow

1. **Analyze the diff** — Identify files changed, lines added/removed, type of change.
2. **Classify type** — Apply Type Selection rules in order.
3. **Pick scope** — Subdirectory or domain. Omit if change spans many areas.
4. **Write description** — Imperative mood, lowercase, no period.
5. **Add `!` if breaking** — Breaking change detected? Append `!` after scope.
6. **Insert ticket slot** — Infer ticket ID from diff (branch hints, file paths, comments) or use `NO-TICKET`.
7. **Check total length** — Recompute after ticket insertion. If > 72 chars, shorten via Character Budget rules.
8. **Output the message** — One line, no markdown, no preamble.

## Edge Cases

### Empty Diff

If the diff is empty or invalid (no staged changes, no file deltas), output **exactly**:

```
abort: no changes detected
```

No Conventional Commits prefix. No exception to this rule.

### Ticket ID Inference

Look for ticket IDs in the diff in this order:

1. Branch name hints (`feat/CORE-1702-add-...` → `CORE-1702`).
2. File paths or directories (`src/CORE-1702/` → `CORE-1702`).
3. Code comments (`// CORE-1702: validate tokens` → `CORE-1702`).
4. Commit message body (if user provided context).
5. If no ID found, use `NO-TICKET`.

### Multiple Files

If the diff touches files in unrelated domains (e.g., both `src/auth/` and `docs/`), examine the dominant change:

- **If most lines are in one domain** — scope to that domain.
- **If lines are evenly split** — omit scope. Keep description general: `feat: TICKET-NNN: update auth and docs`.

### Whitespace-only Lines

Lines that are indentation changes only (no token changes) are `style`. Lines that reformat code structure (e.g., extract a function, reorder parameters) with no semantic change are `refactor`.

## SILENT MODE

When this skill runs, output ONLY the commit message. No "I will now analyze...", no "Here's my reasoning:", no "The diff shows...". Zero preamble. If the output format requires explanation, the skill has failed the user.

## Gardener Protocol

After completing the analysis and outputting the commit message, perform these checks silently (do not output them):

1. **Character count** — Re-verify the message is ≤72 chars. If over, recompute via budget rules and regenerate.
2. **Ticket presence** — Confirm the ticket slot is present (either a real ID or `NO-TICKET`).
3. **Type correctness** — Type selection rule matched correctly (check Type Selection rules again).
4. **Security signal** — If a credential detected, confirm output is the abort sentinel (not a normal commit message).
5. **Breaking signal** — If a breaking change detected, confirm `!` is present after scope (not before, not missing).

If any check fails, regenerate the output until all checks pass, then output the final message.

## Agent Collaboration Protocol

**Input:** Raw git diff (text or file path).

**Output:** Single-line Conventional Commits message.

**Handoff:** If the diff contains obvious bugs or architectural issues, the commit message itself is correct, but the reviewer may ask "should this be two commits?" or "is this scope right?" — that's expected and not a skill failure. The skill's job is to generate a grammatically correct, format-compliant message.

**Integration:** Used by developers and CI automation to auto-generate commit messages from staged diffs. Works with `git diff --staged` input. Can be invoked via CLI tool or chatbot interface.
