# Architecture Patterns Reference — /workflow-expert

## Call Graph Documentation

### Why Document the Call Graph

Reusable workflows create invisible dependencies. Without documentation:

- New engineers don't know which workflows are entry points vs called
- Renaming a file breaks callers in other repos
- Debugging a failure requires tracing `workflow_call` chains manually
- Deprecating a workflow risks breaking unknown consumers

### Template for `.github/workflows/README.md`

```markdown
# Workflow Architecture

## Call Graph

L0 (Entry Workflows) — triggered by events
├── ci.yml on: push, pull_request
│ ├── L1: ci-build.yml workflow_call (build + unit tests)
│ └── L1: ci-security.yml workflow_call (security scan)
├── cd-deploy.yml on: push (main), workflow_dispatch
│ └── L1: cd-common.yml workflow_call (shared deploy logic)
│ └── L2: notify.yml workflow_call (Slack notification)
└── release.yml on: workflow_dispatch
├── L1: ci-build.yml workflow_call (reused)
└── L1: cd-deploy.yml workflow_call (reused)

## Workflow Index

| File            | Level | Trigger                    | Purpose                      |
| --------------- | ----- | -------------------------- | ---------------------------- |
| ci.yml          | L0    | push, PR                   | Entry: runs build + security |
| ci-build.yml    | L1    | workflow_call              | Build, test, lint            |
| ci-security.yml | L1    | workflow_call              | SAST, dependency audit       |
| cd-deploy.yml   | L0/L1 | push(main), dispatch, call | Deploy to environment        |
| cd-common.yml   | L1    | workflow_call              | Shared deploy steps          |
| notify.yml      | L2    | workflow_call              | Slack/email notifications    |
| release.yml     | L0    | workflow_dispatch          | Cut release, tag, deploy     |
```

### Maintenance Rules

- Update README.md whenever adding, renaming, or removing a workflow file
- Include the call graph in PR descriptions when modifying workflow dependencies
- Mark deprecated workflows with `# DEPRECATED: use <replacement> instead` at file top

---

## `workflow_dispatch` Patterns

### Basic: Manual Trigger for Recovery

```yaml
on:
  push:
    branches: [main]
  workflow_dispatch: # enables manual re-run from Actions UI
```

**Rule:** All release and deploy workflows MUST include `workflow_dispatch`. Without it:

- Failed deploys require dummy commits to re-trigger
- Testing a workflow change requires pushing to the trigger branch
- Disaster recovery is blocked by CI pipeline

### With Input Parameters

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: "Target environment"
        required: true
        default: "staging"
        type: choice
        options:
          - staging
          - production
      dry-run:
        description: "Dry run (no actual deploy)"
        required: false
        default: true
        type: boolean
      version:
        description: "Version to deploy (leave empty for latest)"
        required: false
        type: string
```

### Usage in Steps

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: ${{ inputs.environment || 'staging' }}
    steps:
      - name: Deploy
        if: ${{ !inputs.dry-run }}
        run: ./scripts/deploy.sh ${{ inputs.environment }}
```

---

## Naming Conventions

### File Prefixes

| Prefix  | Purpose                                       | Examples                                     |
| ------- | --------------------------------------------- | -------------------------------------------- |
| `ci-`   | Continuous Integration (build, test, lint)    | `ci-build.yml`, `ci-security.yml`            |
| `cd-`   | Continuous Delivery (deploy, release)         | `cd-deploy.yml`, `cd-release.yml`            |
| `sec-`  | Security-specific (scanning, audits)          | `sec-sast.yml`, `sec-dependency-audit.yml`   |
| `util-` | Utility (cleanup, notifications, maintenance) | `util-stale-branches.yml`, `util-notify.yml` |
| `cron-` | Scheduled jobs                                | `cron-nightly-tests.yml`, `cron-cleanup.yml` |

### Benefits

- Alphabetical sorting groups related workflows
- File purpose is clear from name alone
- Prevents naming collisions in shared workflow repos

---

## Workflow Lifecycle

### When to Create New vs Modify Existing

**Create new workflow when:**

- New concern area (e.g., adding security scanning to a repo that had none)
- New deployment target (e.g., adding staging environment)
- Existing workflow exceeds 200 lines (split by concern)

**Modify existing workflow when:**

- Adding a step to an existing concern (e.g., new test type in CI)
- Updating action versions
- Fixing a bug or improving performance

### Deprecation Protocol

1. Add header comment to deprecated file:

```yaml
# DEPRECATED: This workflow is replaced by ci-build.yml
# Scheduled for removal: 2026-04-01
# Migration guide: see .github/workflows/README.md
```

2. Update `.github/workflows/README.md` — mark as deprecated in the index
3. Add a job that emits a warning:

```yaml
jobs:
  deprecation-notice:
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "::warning::This workflow is deprecated. Migrate to ci-build.yml by 2026-04-01."
```

4. After migration deadline: remove the file and update README.md
