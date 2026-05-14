<!-- markdownlint-disable MD013 -->

# `commit-generator` — Benchmark History

Per-run tracker for the workshop demo skill. Skill-creator runs produce one `benchmark.json` per iteration; `append_history.py` translates each into a single row below.

## Iteration table

| Iter     | Date (UTC)       | Run id      | SHA     | Tests | Pass ws                       | Pass wo                   | Δ                           | Tokens (in/out) | Cost | Notes            |
| -------- | ---------------- | ----------- | ------- | ----- | ----------------------------- | ------------------------- | --------------------------- | --------------- | ---- | ---------------- |
| iter-CG3 | 2026-05-14 06:19 | iteration-3 | d9381ec | 8     | **7 / 8 = 87.5% (avg 0.875)** | 1 / 8 = 12.5% (avg 0.125) | **+75.0 pp / +0.750 score** | — / —           | —    | ticket-MANDATORY |
