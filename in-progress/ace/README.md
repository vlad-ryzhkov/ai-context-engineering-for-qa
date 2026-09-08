# ACE — Adaptive Context Evolution (parked)

An attempt at a self-improving instruction set: the AI notices, during a run, that
a rule was missing, records the observation, and a later pass turns recurring
observations into actual edits to the skill files.

## The four moving parts

| Part | Where | Job |
| ---- | ----- | --- |
| **Gardener** | `.claude/protocols/gardener.md` — **graduated, still in use** | After a run, propose a missing rule as a table in chat |
| **Reflection** | `ace-kit/protocols/reflection.md` | On `SKILL PARTIAL` or a loop-guard trip, write exactly one rule to a pending list |
| **Reflector** | `ace-kit/protocols/reflector.md` + `ace-kit/scripts/lib/reflector.sh` | Scan accumulated observations for a pattern worth promoting |
| **Curation** | `ace-kit/skills/curate-lessons/SKILL.md` | Deduplicate the pending list and graduate rules into the real files |

`lessons-data/` holds the accumulated pending / graduated / log files.
`docs/` holds the design write-up: the pipeline, the context-rot argument behind
it, and an inventory of participating files.

## Why it is parked, not shipped

- **The loop never closed.** Gardener reliably *proposes*; Reflection and Reflector
  collected observations; curation into real rule edits stayed manual. The
  automated half was the whole point.
- **It never recorded a single lesson.** After months of use, all three data files
  in `lessons-data/` still contain only their header and format comment:
  `pending.md` has zero entries, `graduated.md` has zero promoted rules, and
  `gardener-log.jsonl` is a zero-byte file. The pipeline was built, wired, and
  documented, and then produced nothing to curate.
- **Two of the three protocols never fired.** Reflection triggers on
  `SKILL PARTIAL` / `LOOP_GUARD_TRIGGERED`; Reflector needs ≥10 telemetry events.
  Neither threshold was reached often enough to validate the design.
- **Cost was real, benefit was not measured.** Every skill carried a Gardener
  block, a telemetry call, and a JSONL append. That is tokens and file writes on
  every run, against a graduation rate of approximately zero.

## What survived

Only Gardener, and only its proposal half. It sits in `.claude/protocols/gardener.md`,
is referenced by every skill, and writes nothing to disk. The logging, telemetry,
and graduation machinery was stripped out of it when this folder was created.

## `ace-kit/` — the portable copy

`ace-kit/` was packaged to drop the whole experiment into another repository via
`setup.sh`. Its paths assume the pre-reorganisation layout (`.ai-lessons/` at the
repo root, protocols symlinked into `.claude/`), so **it will not install cleanly
as-is.** Treat it as a design record, not a working installer.

It also illustrates the failure it was meant to avoid. `ace-kit/docs/ace/` and
`docs/` hold the same three design documents, and the two copies have drifted
apart — as have `ace-kit/protocols/gardener.md` and the live
`.claude/protocols/gardener.md`. Bundling a copy for portability created exactly
the duplicate-that-diverges problem this repository now keeps one source of truth
to prevent. Both copies are kept as the record; neither is authoritative.

`in-progress/` is excluded from markdownlint (see `.markdownlintignore`) because
nothing here is maintained.

## If picked up again

The honest next step is not more machinery — it is a measurement: instrument how
often a proposed rule would have prevented a real defect on the following run. If
that number is near zero, the loop should not be automated at all.
