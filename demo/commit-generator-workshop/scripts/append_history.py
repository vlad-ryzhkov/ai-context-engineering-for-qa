#!/usr/bin/env python3
"""Append one iteration row to benchmark-history.md.

Reads a skill-creator benchmark.json (schema in ~/skills/skills/skill-creator/references/schemas.md)
and appends a row to demo/commit-generator-workshop/skills/benchmark-history.md.

Usage:
    python3 scripts/append_history.py \\
        --benchmark <workspace>/iteration-N/benchmark.json \\
        --iter iter-CG1 \\
        [--sha <git-sha>] \\
        [--cost <usd>] \\
        [--cache <pct>] \\
        [--notes "free-form note"] \\
        [--history skills/benchmark-history.md]

If --history file does not exist, it is created with the canonical header.
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

HEADER = """<!-- markdownlint-disable MD013 -->
# `commit-generator` — Benchmark History

Per-run tracker for the workshop demo skill. Skill-creator runs produce one `benchmark.json` per iteration; `append_history.py` translates each into a single row below.

## Iteration table

| Iter | Date (UTC) | SHA | Tests | Pass ws | Pass wo | Δ | Duration ws | Cost | Cache | Notes |
|------|------------|-----|-------|---------|---------|---|-------------|------|-------|-------|
"""


def fmt_pass(summary: dict, total: int) -> str:
    pr = summary.get("pass_rate", {})
    mean = pr.get("mean", 0.0)
    passed = round(mean * total)
    return f"{passed} / {total} = {mean*100:.1f}% (avg {mean:.3f})"


def delta_pp(ws_mean: float, wo_mean: float) -> str:
    pp = (ws_mean - wo_mean) * 100
    score = ws_mean - wo_mean
    sign = "+" if pp >= 0 else ""
    return f"**{sign}{pp:.1f} pp / {sign}{score:.3f} score**"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--benchmark", required=True, type=Path, help="Path to skill-creator benchmark.json")
    ap.add_argument("--iter", required=True, help="Iteration id, e.g. iter-CG1")
    ap.add_argument("--sha", default="", help="Skill commit SHA (optional)")
    ap.add_argument("--cost", default="", help="Total cost in USD, e.g. 0.42 (optional)")
    ap.add_argument("--cache", default="", help="Cache hit rate %% (e.g. 98.5) — optional")
    ap.add_argument("--notes", default="", help="Free-form notes for the row")
    ap.add_argument(
        "--history",
        default=Path("skills/benchmark-history.md"),
        type=Path,
        help="Path to history markdown (created if missing)",
    )
    args = ap.parse_args()

    if not args.benchmark.exists():
        print(f"ERROR: benchmark.json not found: {args.benchmark}", file=sys.stderr)
        return 1

    data = json.loads(args.benchmark.read_text())
    meta = data.get("metadata", {})
    summary = data.get("run_summary", {})
    ws = summary.get("with_skill", {})
    wo = summary.get("without_skill", {})

    total = len(meta.get("evals_run", []))
    if total == 0:
        print("ERROR: metadata.evals_run empty — cannot count tests", file=sys.stderr)
        return 1

    pass_ws = fmt_pass(ws, total)
    pass_wo = fmt_pass(wo, total)
    delta = delta_pp(ws.get("pass_rate", {}).get("mean", 0.0), wo.get("pass_rate", {}).get("mean", 0.0))

    time_mean = ws.get("time_seconds", {}).get("mean")
    duration_ws = f"{time_mean:.1f}s" if isinstance(time_mean, (int, float)) and time_mean > 0 else "—"

    date = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    sha = args.sha or "—"
    cost = f"${args.cost}" if args.cost else "—"
    cache = f"{args.cache}%" if args.cache else "—"
    notes = args.notes.replace("|", "\\|") or "—"

    row = f"| {args.iter} | {date} | {sha} | {total} | **{pass_ws}** | {pass_wo} | {delta} | {duration_ws} | {cost} | {cache} | {notes} |\n"

    history = args.history
    history.parent.mkdir(parents=True, exist_ok=True)
    if not history.exists():
        history.write_text(HEADER)

    with history.open("a") as f:
        f.write(row)

    print(f"Appended row to {history}")
    print(row.rstrip())
    return 0


if __name__ == "__main__":
    sys.exit(main())
