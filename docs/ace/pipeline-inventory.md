# ACE Pipeline Inventory

All files participating in the ACE (Agentic Context Engineering) pipeline.

## Data Flow

```text
Generator (Skills/Agents)
    │
    ├─→ Gardener observations ──→ gardener-log.jsonl / pending.md
    │                                      │
    ├─→ Telemetry events ──────→ events.jsonl
    │                                      │
    │                              ┌───────┴───────┐
    │                              │  Reflector     │
    │                              │  L1: bash      │
    │                              │  L2: LLM       │
    │                              └───────┬───────┘
    │                                      │
    │                              ┌───────┴───────┐
    │                              │  Curator       │
    │                              │  /curate-lessons│
    │                              └───────┬───────┘
    │                                      │
    └──────────────────────────────────────┘
              Context Targets (CLAUDE.md, qa_agent.md, antipatterns)
```

## Generator Layer (26 files)

| Category          | Count | Location                                         |
| ----------------- | ----: | ------------------------------------------------ |
| Skills (SKILL.md) |    22 | `.claude/skills/*/SKILL.md`                      |
| Agents            |     3 | `.claude/agents/{auditor,sdet,perf-engineer}.md` |
| QA Agent          |     1 | `.claude/qa_agent.md`                            |

## Observation Layer (3 files)

| File                | Path                              |
| ------------------- | --------------------------------- |
| Gardener Protocol   | `.claude/protocols/gardener.md`   |
| Reflection Protocol | `.claude/protocols/reflection.md` |
| Telemetry Hook      | `scripts/hooks/telemetry-hook.sh` |

## Detection Layer (2 files)

| File                | Path                             |
| ------------------- | -------------------------------- |
| Reflector L1 (bash) | `scripts/lib/reflector.sh`       |
| Reflector L2 (LLM)  | `.claude/protocols/reflector.md` |

## Curation Layer (1 file)

| File            | Path                                     |
| --------------- | ---------------------------------------- |
| /curate-lessons | `.claude/skills/curate-lessons/SKILL.md` |

## Quality Pipeline (10 files)

| File                  | Path                                | Purpose                     |
| --------------------- | ----------------------------------- | --------------------------- |
| skill-quality.sh      | `scripts/skill-quality.sh`          | Pipeline orchestrator       |
| golden-test.sh        | `scripts/golden-test.sh`            | Golden output tests         |
| build-tools.sh        | `scripts/build-tools.sh`            | Build toolchain             |
| skill-structure.sh    | `scripts/lib/skill-structure.sh`    | Tier 1 Baseline checks      |
| token-budget.sh       | `scripts/lib/token-budget.sh`       | Token counting + snapshots  |
| regression-detect.sh  | `scripts/lib/regression-detect.sh`  | Section removal detection   |
| compliance-checker.sh | `scripts/lib/compliance-checker.sh` | Compliance validation       |
| contract-validator.sh | `scripts/lib/contract-validator.sh` | Cross-skill contract checks |
| runtime-budget.sh     | `scripts/lib/runtime-budget.sh`     | Runtime budget enforcement  |
| reflector.sh          | `scripts/lib/reflector.sh`          | Reflector L1 engine         |

## Hooks (5 files)

| File              | Path                              | Trigger                              |
| ----------------- | --------------------------------- | ------------------------------------ |
| delta-guard.sh    | `.claude/hooks/delta-guard.sh`    | Write tool — warns on full overwrite |
| skill-lint.sh     | `.claude/hooks/skill-lint.sh`     | Edit tool — lints SKILL.md changes   |
| telemetry-hook.sh | `scripts/hooks/telemetry-hook.sh` | Skill completion — logs events       |
| pre-commit.sh     | `scripts/pre-commit.sh`           | Git pre-commit — blocks secrets      |
| pre-push.sh       | `scripts/pre-push.sh`             | Git pre-push — blocks secrets        |

## Telemetry & Data (4 files)

| File              | Path                             | Format                   |
| ----------------- | -------------------------------- | ------------------------ |
| Gardener Log      | `.ai-lessons/gardener-log.jsonl` | JSONL (append-only)      |
| Pending Lessons   | `.ai-lessons/pending.md`         | Markdown (append-only)   |
| Graduated Lessons | `.ai-lessons/graduated.md`       | Markdown (promotion log) |
| Execution Events  | `tests/telemetry/events.jsonl`   | JSONL (append-only)      |

## CI/CD (2 files)

| File                  | Path                                  |
| --------------------- | ------------------------------------- |
| Quality Workflow      | `.github/workflows/skill-quality.yml` |
| Cross-Skill Contracts | `cross-skill-contracts.yaml`          |

## Context Targets (35 files)

| Category                | Count | Location                                            |
| ----------------------- | ----: | --------------------------------------------------- |
| Root context            |     2 | `CLAUDE.md`, `.claude/qa_agent.md`                  |
| Antipattern index       |     1 | `.claude/qa-antipatterns/_index.md`                 |
| Antipatterns — security |     3 | `.claude/qa-antipatterns/security/`                 |
| Antipatterns — platform |    11 | `.claude/qa-antipatterns/platform/` (incl. `java/`) |
| Antipatterns — common   |     7 | `.claude/qa-antipatterns/common/`                   |
| Antipatterns — API      |    11 | `.claude/qa-antipatterns/api/` (incl. `java/`)      |

## Documentation (5 files)

| File                       | Path                                         |
| -------------------------- | -------------------------------------------- |
| ACE Pipeline               | `docs/ace/ace-pipeline.md`                   |
| Context Evolution          | `docs/ace/context-evolution.md`              |
| Pipeline Inventory         | `docs/ace/pipeline-inventory.md` (this file) |
| AI Setup Registry          | `docs/ai-setup.md`                           |
| Behavioral Testing Roadmap | `docs/behavioral-testing-roadmap.md`         |

---

**Total: ~90 unique files** (some appear in multiple categories by design — e.g., reflector.sh is both Detection and Quality Pipeline).
