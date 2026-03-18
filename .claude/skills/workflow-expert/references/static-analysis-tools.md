# Static Analysis Tools Reference — /workflow-expert

## Zizmor

**Purpose:** Security-focused static analysis for GitHub Actions workflows. Detects deep structural vulnerabilities: template injection, excessive permissions, credential leakage, impostor commits.

### Installation

```bash
# macOS
brew install zizmor

# Cargo
cargo install zizmor

# pip
pip install zizmor
```

### Usage

```bash
# Scan all workflows
zizmor .github/workflows/

# Scan single file
zizmor .github/workflows/ci.yml

# JSON output for CI integration
zizmor --format json .github/workflows/ > zizmor-report.json

# SARIF output for GitHub Security tab
zizmor --format sarif .github/workflows/ > results.sarif
```

### CI Integration Step

```yaml
- name: Run Zizmor
  run: |
    pip install zizmor
    zizmor --format sarif .github/workflows/ > results.sarif
  continue-on-error: true

- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@<SHA>
  with:
    sarif_file: results.sarif
```

### Key Rules Detected

| Rule                     | Severity | Description                                       |
| ------------------------ | -------- | ------------------------------------------------- |
| `template-injection`     | CRITICAL | Context variables interpolated in `run:` blocks   |
| `excessive-permissions`  | MAJOR    | `GITHUB_TOKEN` with unnecessary write scopes      |
| `unpinned-uses`          | CRITICAL | Actions referenced by mutable tag instead of SHA  |
| `credential-persistence` | MAJOR    | Credentials stored in insecure locations          |
| `impostor-commit`        | CRITICAL | Tag references that may point to impostor commits |
| `dangerous-triggers`     | MAJOR    | `pull_request_target` or `workflow_run` misuse    |

---

## Actionlint

**Purpose:** Syntax and semantic linting for GitHub Actions workflow files. Integrates with shellcheck for `run:` block validation.

### Installation

```bash
# macOS
brew install actionlint

# Go
go install github.com/rhysd/actionlint/cmd/actionlint@latest

# Download binary
curl -sL https://github.com/rhysd/actionlint/releases/latest/download/actionlint_linux_amd64.tar.gz | tar xz
```

### Usage

```bash
# Lint all workflow files
actionlint

# Lint specific file
actionlint .github/workflows/ci.yml

# JSON output
actionlint -format '{{json .}}'

# With shellcheck integration (auto-detected if installed)
actionlint  # shellcheck runs automatically on `run:` blocks
```

### CI Integration Step

```yaml
- name: Run Actionlint
  run: |
    bash <(curl -s https://raw.githubusercontent.com/rhysd/actionlint/main/scripts/download-actionlint.bash)
    ./actionlint -color
```

### Key Checks

| Category          | Examples                                                  |
| ----------------- | --------------------------------------------------------- |
| YAML syntax       | Indentation, key duplication, type errors                 |
| Expression syntax | `${{ }}` typos, undefined context variables               |
| Action inputs     | Invalid `with:` parameters (when action.yml is available) |
| Shell scripts     | Shellcheck integration for `run:` blocks                  |
| Job dependencies  | Invalid `needs:` references, circular dependencies        |
| Matrix validation | Type mismatches, empty matrices                           |

---

## Checkov

**Purpose:** IaC security scanner with GitHub Actions-specific checks. Detects missing security controls, unpinned versions, and policy violations.

### CI Integration Step

```yaml
- name: Run Checkov
  uses: bridgecrewio/checkov-action@<SHA>
  with:
    directory: .github/workflows
    framework: github_actions
    output_format: sarif
    output_file_path: checkov-results.sarif
```

### Key GitHub Actions Checks

| Check ID    | Description                                                               |
| ----------- | ------------------------------------------------------------------------- |
| `CKV_GHA_1` | Ensure workflows do not have `pull_request_target` with insecure checkout |
| `CKV_GHA_2` | Ensure workflows pin actions to SHA                                       |
| `CKV_GHA_3` | Ensure workflows have explicit permissions                                |
| `CKV_GHA_4` | Ensure `GITHUB_TOKEN` permissions are restrictive                         |
| `CKV_GHA_7` | Ensure actions are not referencing `HEAD` or `main`                       |

---

## Trivy

**Purpose:** Vulnerability scanner for containers, filesystems, and IaC. Generates SBOMs and detects CVEs in container images built by workflows.

### CI Integration for Container Scanning

```yaml
- name: Build image
  run: docker build -t app:${{ github.sha }} .

- name: Scan with Trivy
  uses: aquasecurity/trivy-action@<SHA>
  with:
    image-ref: app:${{ github.sha }}
    format: sarif
    output: trivy-results.sarif
    severity: CRITICAL,HIGH

- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@<SHA>
  with:
    sarif_file: trivy-results.sarif
```

### SBOM Generation

```yaml
- name: Generate SBOM
  uses: aquasecurity/trivy-action@<SHA>
  with:
    image-ref: app:${{ github.sha }}
    format: cyclonedx
    output: sbom.json
```

---

## Recommended Multi-Layer Scanning Pipeline

```yaml
jobs:
  lint-workflows:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@<SHA>
      - name: Actionlint (syntax)
        run: actionlint
      - name: Zizmor (security)
        run: zizmor --format sarif .github/workflows/ > zizmor.sarif
      - name: Checkov (policy)
        uses: bridgecrewio/checkov-action@<SHA>
        with:
          directory: .github/workflows
          framework: github_actions

  scan-containers:
    needs: lint-workflows
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@<SHA>
      - run: docker build -t app:${{ github.sha }} .
      - name: Trivy (vulnerabilities)
        uses: aquasecurity/trivy-action@<SHA>
        with:
          image-ref: app:${{ github.sha }}
          severity: CRITICAL,HIGH
```
