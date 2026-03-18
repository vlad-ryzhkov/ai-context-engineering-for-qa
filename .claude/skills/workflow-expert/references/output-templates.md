# Output Templates Reference — /workflow-expert

## Compact Issue Format (All Modes)

Every finding uses this 4-line format:

```
[emoji] [SEVERITY]: [Title]
📍 .github/workflows/[file].yml:[line]
[1 sentence: why this is dangerous or inefficient]
Fix: [inline code or "See SEARCH/REPLACE below"]
```

**Examples:**

```
🔴 CRITICAL: Direct context interpolation in run block
📍 .github/workflows/ci.yml:47
PR title is interpolated directly into shell — enables command injection by any PR author.
Fix: Move `github.event.pull_request.title` to `env:` block, reference as `$PR_TITLE`.

🟠 MAJOR: Missing workflow-level permissions
📍 .github/workflows/deploy.yml:1
No `permissions:` block — GITHUB_TOKEN gets broad default access including write.
Fix: Add `permissions: { contents: read }` at workflow level.
```

---

## Analyze Mode — Full Audit Report

```markdown
# Workflow Audit Report

## 🔴 CRITICAL Issues

[4-line compact format per issue, ordered by severity then confidence]

## 🟠 MAJOR Issues

[4-line compact format per issue]

## ✅ Passing Categories

- ✅ [Category]: No issues found.
- ✅ [Category]: No issues found.

## 📊 Summary

[1-2 sentences: overall security posture, top recommendation]

## 📋 Inventory

| Metric                           | Value       |
| -------------------------------- | ----------- |
| Workflows scanned                | N           |
| Total `uses:` directives         | N           |
| SHA-pinned                       | N/M (X%)    |
| Deprecated syntax                | N instances |
| Static analysis tools integrated | Yes/No      |
```

---

## Fix Mode — Root Cause Analysis + Patch

```markdown
# Workflow Fix Report

## Root Cause

[2-3 sentences: what failed, why it failed, what the structural issue is]

## Affected Files

| File                                 | Change Type       |
| ------------------------------------ | ----------------- |
| `.github/workflows/ci.yml`           | Primary fix       |
| `.github/workflows/shared-build.yml` | Propagated change |

## Changes

### 📍 .github/workflows/ci.yml

<<<<<<< SEARCH
[exact old code block with surrounding context]
=======
[new code block]

> > > > > > > REPLACE

### 📍 .github/workflows/shared-build.yml (Propagation)

<<<<<<< SEARCH
[exact old code block]
=======
[new code block]

> > > > > > > REPLACE

## Verification

[How to verify the fix works — e.g., "Re-run the workflow; the `deploy` job should no longer fail with permission denied."]
```

---

## Modify Mode — Scope + Propagation + Patch

```markdown
# Workflow Modification Report

## Requested Change

[1 sentence: what the user asked for]

## Scope Analysis

| Aspect             | Detail                                    |
| ------------------ | ----------------------------------------- |
| Target workflow(s) | `ci.yml`, `deploy.yml`                    |
| Call graph depth   | L0 → L1                                   |
| Propagation needed | Yes — caller `ci.yml` must pass new input |
| New inputs/secrets | `input: cache-key` (string, optional)     |

## Changes

### 📍 .github/workflows/deploy.yml (Called Workflow)

<<<<<<< SEARCH
[old code]
=======
[new code]

> > > > > > > REPLACE

### 📍 .github/workflows/ci.yml (Caller — Propagation)

<<<<<<< SEARCH
[old code]
=======
[new code with new `with:` parameter]

> > > > > > > REPLACE
```

---

## Improve Mode — Rationale + Patch

```markdown
# Workflow Improvement Report

## Optimizations

### 1. [Title] — Impact: High

**Current state:** [1 sentence describing the problem]
**Improvement:** [1 sentence describing the solution]
**Estimated savings:** [e.g., "~3 min per run" or "~60% fewer redundant runs"]

#### 📍 .github/workflows/ci.yml

<<<<<<< SEARCH
[old code]
=======
[new code]

> > > > > > > REPLACE

### 2. [Title] — Impact: Medium

[Same structure]

## Summary

| #   | Optimization | Impact | Files                  |
| --- | ------------ | ------ | ---------------------- |
| 1   | [Title]      | High   | `ci.yml`               |
| 2   | [Title]      | Medium | `ci.yml`, `deploy.yml` |
```

---

## Passing Category Line Format

When an entire audit category has no findings:

```
✅ Secret Management: No issues found.
✅ Dependency Pinning: No issues found.
✅ Injection Prevention: No issues found.
```

**Rule:** Never output verbose "PASS" blocks with details for passing checks. One line per clean category.

---

## Completion Block Templates

```
SKILL COMPLETE — /workflow-expert (Analyze)
Workflows scanned: 5
Findings: 2 CRITICAL, 3 MAJOR
Top risk: Direct context interpolation in ci.yml:47 enables command injection.
```

```
SKILL COMPLETE — /workflow-expert (Fix)
Root cause: Reusable workflow missing `id-token: write` permission for OIDC.
Files modified: 2
Propagation: ci.yml → shared-deploy.yml (added permission)
```

```
SKILL COMPLETE — /workflow-expert (Modify)
Change: Added `node-version` input to reusable build workflow.
Files modified: 3
Propagation: ci.yml, release.yml → shared-build.yml
```

```
SKILL COMPLETE — /workflow-expert (Improve)
Optimizations: 3 improvements proposed
Estimated impact: High (caching), Medium (concurrency), Low (timeout)
Files modified: 2
```
