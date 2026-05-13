<!-- markdownlint-disable MD013 -->

# `commit-generator` — Benchmark History

Per-run tracker for the workshop demo skill. Skill-creator runs produce one `benchmark.json` per iteration; `append_history.py` translates each into a single row below.

## Iteration table

| Iter     | Date (UTC) | SHA | Tests | Pass ws          | Pass wo      | Δ   | Duration ws | Cost | Cache | Notes                                                                                                                                          |
| -------- | ---------- | --- | ----- | ---------------- | ------------ | --- | ----------- | ---- | ----- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| iter-CG0 | 2026-05-13 | —   | 6     | **— / 6 = TBD%** | — / 6 = TBD% | TBD | —           | —    | —     | Placeholder row — replace with real iter-CG1 output after first skill-creator run. Removed once `append_history.py` writes its first real row. |

## How rows are produced

After every `skill-creator` iteration finishes:

```bash
python3 scripts/append_history.py \
    --benchmark <workspace>/iteration-N/benchmark.json \
    --iter iter-CG<N> \
    --sha "$(git rev-parse --short HEAD)" \
    --notes "first run — single prompt, 6 evals, with-vs-without baseline"
```

Cost and cache fields are optional; pass them when known. Duration is read automatically from `benchmark.json` (`run_summary.with_skill.time_seconds.mean`).

## Notes on columns

- **Pass ws** — pass rate with the skill loaded.
- **Pass wo** — baseline pass rate without the skill.
- **Δ** — percentage-point and score delta (ws − wo). Positive means the skill helps.
- **Duration ws** — mean wall-clock per eval, with skill, in seconds.
- **Tests** — count of evals in this iteration (expect 6 for the full set, 3 for the cut-down rate-limited demo).
- **Notes** — what changed since the previous iteration. Reference the failing case IDs.
