# ACE Pipeline Inventory

All files participating in the ACE (Agentic Context Engineering) pipeline.
**Canonical source:** `ace-kit/` — IDE-specific paths are symlinks or generated wrappers.

## Data Flow

```text
Generator (Skills/Agents)
    |
    +---> Gardener observations --> gardener-log.jsonl / pending.md
    |                                      |
    +---> Telemetry events -------> events.jsonl
    |                                      |
    |                              +-------+-------+
    |                              |  Reflector     |
    |                              |  L1: bash      |
    |                              |  L2: LLM       |
    |                              +-------+-------+
    |                                      |
    |                              +-------+-------+
    |                              |  Curator       |
    |                              |  /curate-lessons|
    |                              +-------+-------+
    |                                      |
    +--------------------------------------+
              Context Targets (CLAUDE.md, qa_agent.md, antipatterns)
```

## Canonical Source (ace-kit/)

### Protocols (3 files)

| File                    | Canonical Path                    | Symlinked to (Claude Code)        |
| ----------------------- | --------------------------------- | --------------------------------- |
| Gardener Protocol       | `ace-kit/protocols/gardener.md`   | `.claude/protocols/gardener.md`   |
| Reflection Protocol     | `ace-kit/protocols/reflection.md` | `.claude/protocols/reflection.md` |
| Reflector Protocol (L2) | `ace-kit/protocols/reflector.md`  | `.claude/protocols/reflector.md`  |

### Hooks (1 file)

| File        | Canonical Path                 | Symlinked to (Claude Code)     |
| ----------- | ------------------------------ | ------------------------------ |
| Delta Guard | `ace-kit/hooks/delta-guard.sh` | `.claude/hooks/delta-guard.sh` |

### Skills (1 directory)

| File            | Canonical Path                   | Symlinked to (Claude Code)       |
| --------------- | -------------------------------- | -------------------------------- |
| /curate-lessons | `ace-kit/skills/curate-lessons/` | `.claude/skills/curate-lessons/` |

### Scripts (2 files)

| File                | Canonical Path                            | Symlinked to                      |
| ------------------- | ----------------------------------------- | --------------------------------- |
| Telemetry Hook      | `ace-kit/scripts/hooks/telemetry-hook.sh` | `scripts/hooks/telemetry-hook.sh` |
| Reflector L1 (bash) | `ace-kit/scripts/lib/reflector.sh`        | `scripts/lib/reflector.sh`        |

### Starters (3 files — copied, not symlinked)

| File              | Canonical Path                        | Copied to                        |
| ----------------- | ------------------------------------- | -------------------------------- |
| Pending Lessons   | `ace-kit/starters/pending.md`         | `.ai-lessons/pending.md`         |
| Graduated Lessons | `ace-kit/starters/graduated.md`       | `.ai-lessons/graduated.md`       |
| Gardener Log      | `ace-kit/starters/gardener-log.jsonl` | `.ai-lessons/gardener-log.jsonl` |

### Documentation (3 files)

| File               | Canonical Path                                       |
| ------------------ | ---------------------------------------------------- |
| ACE Pipeline       | `ace-kit/docs/ace/ace-pipeline.md`                   |
| Context Evolution  | `ace-kit/docs/ace/context-evolution.md`              |
| Pipeline Inventory | `ace-kit/docs/ace/pipeline-inventory.md` (this file) |

### Setup Script (1 file)

| File      | Path               |
| --------- | ------------------ |
| IDE Setup | `ace-kit/setup.sh` |

## IDE Integration

### Claude Code (symlinks)

```
.claude/protocols/gardener.md    -> ace-kit/protocols/gardener.md
.claude/protocols/reflection.md  -> ace-kit/protocols/reflection.md
.claude/protocols/reflector.md   -> ace-kit/protocols/reflector.md
.claude/hooks/delta-guard.sh     -> ace-kit/hooks/delta-guard.sh
.claude/skills/curate-lessons/   -> ace-kit/skills/curate-lessons/
```

### Cursor (generated .mdc wrappers)

```
.cursor/rules/ace-gardener.mdc       # frontmatter + gardener.md content
.cursor/rules/ace-reflection.mdc     # frontmatter + reflection.md content
.cursor/rules/ace-reflector.mdc      # frontmatter + reflector.md content
```

### Copilot (generated instructions)

```
.github/copilot-instructions.md      # Compact ACE summary
```

## Telemetry & Data (project-local, not in ace-kit)

| File              | Path                                    | Format                     |
| ----------------- | --------------------------------------- | -------------------------- |
| Gardener Log      | `.ai-lessons/gardener-log.jsonl`        | JSONL (append-only)        |
| Pending Lessons   | `.ai-lessons/pending.md`                | Markdown (append-only)     |
| Graduated Lessons | `.ai-lessons/graduated.md`              | Markdown (promotion log)   |
| Execution Events  | `tests/telemetry/events.jsonl`          | JSONL (append-only)        |
| Reflector Report  | `tests/telemetry/reflector-report.json` | JSON (overwritten per run) |

---

**Total ace-kit files:** 14 content + 1 script + 3 starters = **18 files**

Dependencies: `jq` (for `ace-kit/scripts/lib/reflector.sh`).
