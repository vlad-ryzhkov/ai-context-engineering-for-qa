---
name: init-project
description: Generates CLAUDE.md for a QA project — scans the repository, analyzes tech stack, creates an onboarding document. Use for a new QA project without CLAUDE.md or setting up AI-assisted workflow. Do not use if CLAUDE.md is already configured — edit manually.
agent: agents/sdet.md
context: fork
---

# /init-project — CLAUDE.md Generator

> **SILENT MODE**: Execute all phases silently. Do not output intermediate scan results
> or file findings. Only the final CLAUDE.md artifact and SKILL COMPLETE block go to chat.

<purpose>
Automatic creation of CLAUDE.md (AI onboarding into the project) based on repository analysis.
Focus: QA projects (API tests, UI tests, load testing).
</purpose>

## Before Starting

Read `.claude/qa_agent.md`.

## When to Use

- New QA project without CLAUDE.md
- Migrating an existing project to AI-assisted workflow
- Standardizing CLAUDE.md across the team

## Execution Algorithm

## Verbosity Protocol

**Structured Output Priority:** All analysis goes into the artifact (MD/HTML), not into chat.

**Chat output (constraints):**
- Brief Summary: max 5 lines (what was found, how many, result)
- Findings table: max 15 lines (top by severity)
- Full report: `📊 Full report: {path}` + open file

**Iterative steps:** Do not output progress for each file. Checkpoint only on:
- Phase transition (Phase N → Phase N+1)
- Blocker detected
- Completion (SKILL COMPLETE)

**Tools first:**
- Grep → table → report, no "Now I will grep..."
- Read → analyze → report, no "The file shows..."

**Post-Check:** Inline before SKILL COMPLETE (5-7 line checklist), not a separate file.

### Step 1: Scan the Project

Find and analyze:

```text
1. Build files:
   - build.gradle.kts / build.gradle → Kotlin/Java + dependencies
   - pom.xml → Maven + dependencies
   - package.json → Node.js + dependencies
   - requirements.txt / pyproject.toml → Python + dependencies

2. Test structure:
   - src/test/ → standard JVM structure
   - tests/ → Python/JS structure
   - __tests__/ → Jest structure

3. Configurations:
   - allure.properties → Allure reporting
   - pytest.ini → pytest config
   - jest.config.js → Jest config

4. CI/CD:
   - .github/workflows/ → GitHub Actions
   - .gitlab-ci.yml → GitLab CI
   - Jenkinsfile → Jenkins

5. Default branch:
   ```bash
   git symbolic-ref refs/remotes/origin/HEAD | sed 's|refs/remotes/origin/||'
   # If empty — run: git remote set-head origin --auto
   # If still unclear — ask the user: "What is the main branch? (main / master)"
   ```
   Document result as `Main branch: <name>` in Git Guidelines.
```

### Step 1 Error Handling

**Build files not found** → Ask the user:

```text
Could not determine tech stack automatically. Specify manually:
- Language/runtime: (Kotlin / Java / Python / JS / Go / other)
- HTTP client for tests:
- Testing framework:
- Reporting:
```

**Non-standard test structure** (no `src/test/`, `tests/`) → Ask to specify the test root directory.

**CI/CD configs missing** → Skip the CI section in CLAUDE.md, mark as TODO.

### Step 1.5: Determine Project Type

Based on Step 1, determine the type:

| Indicator | Type | Include in CLAUDE.md |
|-----------|------|----------------------|
| `src/test/` exists | **QA project** | QA Skills (if `.claude/skills/` exists) |
| No `src/test/`, has Helm/Terraform/k8s | **Infra project** | Architecture, Key Values |
| Both indicators | **Mixed** | All sections |

**QA Skills section:** include only if `.claude/skills/` exists in the target project. If `.claude/skills/` is absent — do not add the section.

**Architecture section:** include if the project is infra/backend (no `src/test/`). Describe key design decisions from the code: components, dependencies between them, non-trivial configurations.

**CI/CD Flow:** include if CI configs are found (`.github/workflows/`, `.gitlab-ci.yml`, `Jenkinsfile`). Format — diagram as a code block.

### Step 1.7: API Documentation Discovery

Scan for machine-readable API specs and link them in CLAUDE.md.

```text
Glob: **/swagger.json, **/swagger.yaml, **/swagger.yml
      **/openapi.json, **/openapi.yaml, **/openapi.yml, **/*.swagger.json
      **/*.proto
      **/*.graphql, **/schema.graphqls
      **/*.http, **/api.http
      **/postman_collection.json, **/*.postman_collection.json
```

For each found file — record: type (OpenAPI / gRPC / GraphQL / HTTP / Postman) and relative path.
Do NOT read file contents. Discovery only.

If nothing found — skip the `## API Documentation` section in CLAUDE.md entirely.

### Step 2: Determine Tech Stack

Based on dependencies, determine:

| Category | What to look for | BANNED alternatives |
|----------|------------------|---------------------|
| HTTP Client | ktor-client, requests, axios | retrofit, okhttp, urllib |
| Serialization | jackson, pydantic, zod | gson, moshi |
| Assertions | kotest, pytest, jest | junit assertEquals, unittest |
| Test Framework | junit5, pytest, jest | testng, nose |
| Reporting | allure | — |

### Step 3: Generate CLAUDE.md

Read and use the template from `references/claude-md-template.md`.

Fill in all `[xxx]` placeholders with data from Steps 1-2. Select the appropriate Tech Stack by project language ("Tech Stack by languages" section in the template).

**API Documentation section:** fill from Step 1.7 results. Include only if spec files were found; omit entirely otherwise.

**Key Values:** if the project has configuration files with non-trivial defaults (`values.yaml`, `.env.example`, `application.yml`) — add a `## Key Values` section explaining critical settings (not obvious from the name).

**Architecture (for infra/backend projects):** describe key design decisions — components, interaction schema, non-trivial implementation details. Use prose format.

### Step 4: Validation

Before saving, verify:

- [ ] Tech Stack matches actual dependencies
- [ ] Commands work (check for gradlew/npm/pytest presence)
- [ ] Structure reflects actual directories
- [ ] No `[xxx]` or TODO placeholders in the final file
- [ ] No HTML comments `<!-- -->` from the template in the final file
- [ ] Heading `#` = project name (not "CLAUDE.md")
- [ ] QA Skills present **only if** `.claude/skills/` exists
- [ ] CI/CD Flow present if CI configs were found
- [ ] Architecture present for infra/backend projects
- [ ] API Documentation section present **only if** spec files were found

## Output

Save the result to `CLAUDE.md` in the project root.

## Anti-Patterns

| Anti-Pattern | Why It Breaks | Fix |
|---|---|---|
| Tech Stack inferred from only one source (package.json only) | Misses critical dependencies in lock files or transitive deps | Check both manifest (package.json) AND lock files (yarn.lock, package-lock.json) |
| Hardcoded tech stack that doesn't match actual project | CLAUDE.md contradicts reality; AI gets confused | Scan build files, import statements; verify before writing |
| `[xxx]` placeholders left unfilled (e.g., `[Language]`, `[MainBranch]`) | Template cruft confuses AI; interpreted as literal values | Replace all placeholders via Step 1 data; remove if unknown |
| QA Skills section added when `.claude/skills/` doesn't exist | Empty section wastes tokens; creates confusion | **Only** add QA Skills section if `.claude/skills/` actually exists |
| No API Documentation section when spec files exist | AI doesn't know about the API spec; generates tests without reference | Always scan for OpenAPI/gRPC/GraphQL specs in Step 1.7 |
| CI/CD section missing when workflows are present | Incomplete context on how tests are run; CD assumptions break | Include CI/CD section if `.github/workflows/`, `.gitlab-ci.yml`, or `Jenkinsfile` found |

## Quality Gate (Self-Review)

Before saving the generated `CLAUDE.md`:

- [ ] Tech Stack matches actual project dependencies
- [ ] Commands work (`./gradlew`, `npm`, `pytest`, etc. all exist)
- [ ] Structure reflects actual directories (`src/test/`, `tests/`, etc.)
- [ ] **No** `[xxx]` or TODO placeholders remain
- [ ] **No** HTML comments `<!-- -->` from template
- [ ] Heading `#` = project name (not "CLAUDE.md")
- [ ] QA Skills section present **only if** `.claude/skills/` exists in project
- [ ] CI/CD Flow section present if CI configs were found
- [ ] API Documentation section present **only if** spec files were found

**Gardener Protocol**: Call `.claude/protocols/gardener.md`. If you identified missing rules
or inefficiencies during this run, output a brief proposal table. Otherwise: `🌱 Gardener: No updates needed.`

## Example Dialog

```text
User: /init-project

AI: Scanning the project...

Found:
- build.gradle.kts → Kotlin 1.9
- Dependencies: ktor-client, jackson, kotest, allure
- Structure: src/test/kotlin/
- CI: .github/workflows/test.yml

Generating CLAUDE.md...

[Shows the generated file]

Save to ./CLAUDE.md? (y/n)
```

## Related Files

- Template: `.claude/skills/init-project/references/claude-md-template.md`
- Full guide: `docs/ai-files-handbook.md`
- Examples: existing `CLAUDE.md` in the project (if any)
