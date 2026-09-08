#!/usr/bin/env python3
"""Report the blended effective price of your Claude Code token usage.

Reads the local Claude Code transcripts (~/.claude/projects/**/*.jsonl) and
reports ONE number per period: dollars per million tokens, modelled from the
published per-model rates.

It deliberately does NOT report what you spent. Absolute spend tracks how much
you worked; the $/1M rate tracks how efficiently that work was priced. A session
that reads 200k of cached context every turn costs a tenth of one that re-sends
it as fresh input, and only the rate makes that visible.

Usage:
  ai-efficiency.py                     # day / week / month
  ai-efficiency.py --by-model          # add the per-model rate breakdown
  ai-efficiency.py --cache-ttl 1h      # price cache writes at the 1-hour rate
  ai-efficiency.py --json

Rates below are list prices from platform.claude.com/docs/en/about-claude/pricing
as published on 2026-09-08. They ignore subscription plans, batch discounts, and
negotiated terms, so treat the output as a modelled rate, not an invoice.
"""
import argparse
import collections
import datetime
import glob
import json
import os
import pathlib

PROJECTS = pathlib.Path.home() / ".claude" / "projects"

BASE_INPUT = {
    "fable-5-1": 10.0,
    "mythos-5-1": 10.0,
    "fable-5": 10.0,
    "mythos-5": 10.0,
    "opus-5": 5.0,
    "opus-4-8": 5.0,
    "opus-4-7": 5.0,
    "opus-4-6": 5.0,
    "opus-4-5": 5.0,
    "opus-4-1": 15.0,
    "opus-4": 15.0,
    "sonnet-5": 2.0,
    "sonnet-4-6": 3.0,
    "sonnet-4-5": 3.0,
    "sonnet-4": 3.0,
    "haiku-4-5": 1.0,
    "haiku-3-5": 0.8,
}

OUTPUT_MULTIPLIER = 5.0
CACHE_WRITE_MULTIPLIER = {"5m": 1.25, "1h": 2.0}
CHEAP_CACHE_READ_MODELS = ("fable-5-1", "mythos-5-1")

PERIODS = (("day", 1), ("week", 7), ("month", 30))


def normalise(model: str) -> str:
    return model.replace("[1m]", "").replace("_", "-").lower()


def match_key(model: str) -> str | None:
    m = normalise(model)
    for key in sorted(BASE_INPUT, key=len, reverse=True):
        if key in m:
            return key
    return None


def rates_for(key: str, cache_ttl: str) -> dict[str, float]:
    base = BASE_INPUT[key]
    read_multiplier = 0.025 if key in CHEAP_CACHE_READ_MODELS else 0.1
    return {
        "input_tokens": base,
        "output_tokens": base * OUTPUT_MULTIPLIER,
        "cache_creation_input_tokens": base * CACHE_WRITE_MULTIPLIER[cache_ttl],
        "cache_read_input_tokens": base * read_multiplier,
    }


def iter_transcripts(days: int):
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=days)
    for path in glob.glob(str(PROJECTS / "**" / "*.jsonl"), recursive=True):
        mtime = datetime.datetime.fromtimestamp(os.path.getmtime(path), datetime.timezone.utc)
        if mtime >= cutoff:
            yield path


def record_time(record: dict):
    stamp = record.get("timestamp")
    if not isinstance(stamp, str):
        return None
    try:
        return datetime.datetime.fromisoformat(stamp.replace("Z", "+00:00"))
    except ValueError:
        return None


def collect(days: int):
    """Sum token counts per model per token-type over the window.

    Each record is filtered by its OWN timestamp, not the file's mtime: a long
    session touched today would otherwise drop its entire history into the
    one-day bucket.
    """
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=days)
    per_model = collections.defaultdict(collections.Counter)
    skipped_models = collections.Counter()
    for path in iter_transcripts(days):
        with open(path, errors="ignore") as fh:
            for line in fh:
                if '"usage"' not in line:
                    continue
                try:
                    record = json.loads(line)
                except json.JSONDecodeError:
                    continue
                when = record_time(record)
                if when is None or when < cutoff:
                    continue
                message = record.get("message")
                if not isinstance(message, dict):
                    continue
                usage = message.get("usage")
                if not isinstance(usage, dict):
                    continue
                model = message.get("model") or ""
                if not model or model.startswith("<"):
                    continue
                key = match_key(model)
                if key is None:
                    skipped_models[model] += 1
                    continue
                for field in (
                    "input_tokens",
                    "output_tokens",
                    "cache_creation_input_tokens",
                    "cache_read_input_tokens",
                ):
                    per_model[key][field] += usage.get(field, 0) or 0
    return per_model, skipped_models


def summarise(per_model, cache_ttl: str):
    cost = tokens = cache_read = 0.0
    rows = []
    for key, counts in per_model.items():
        rates = rates_for(key, cache_ttl)
        model_cost = sum(counts[f] * rates[f] / 1_000_000 for f in rates)
        model_tokens = sum(counts.values())
        if not model_tokens:
            continue
        cost += model_cost
        tokens += model_tokens
        cache_read += counts["cache_read_input_tokens"]
        rows.append(
            {
                "model": key,
                "tokens": model_tokens,
                "cache_read_pct": 100 * counts["cache_read_input_tokens"] / model_tokens,
                "rate_per_mtok": 1_000_000 * model_cost / model_tokens,
            }
        )
    rows.sort(key=lambda r: r["tokens"], reverse=True)
    return {
        "tokens": int(tokens),
        "cache_read_pct": (100 * cache_read / tokens) if tokens else 0.0,
        "rate_per_mtok": (1_000_000 * cost / tokens) if tokens else 0.0,
        "by_model": rows,
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--cache-ttl", choices=sorted(CACHE_WRITE_MULTIPLIER), default="5m",
                    help="which cache-write rate to model (default: 5m)")
    ap.add_argument("--by-model", action="store_true", help="break the rate down per model")
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    if not PROJECTS.is_dir():
        raise SystemExit(f"No transcripts found at {PROJECTS}")

    results = {}
    skipped = collections.Counter()
    for label, days in PERIODS:
        per_model, unknown = collect(days)
        results[label] = summarise(per_model, args.cache_ttl)
        skipped.update(unknown)

    if args.json:
        print(json.dumps({"cache_ttl": args.cache_ttl, "periods": results,
                          "unpriced_models": dict(skipped)}, indent=2))
        return

    print(f"# Effective token price — $ per 1M tokens (cache writes modelled at {args.cache_ttl})\n")
    print(f"{'period':8} {'tokens':>16} {'cache read':>11} {'$/1M tokens':>13}")
    for label, _ in PERIODS:
        r = results[label]
        if not r["tokens"]:
            print(f"{label:8} {'—':>16} {'—':>11} {'—':>13}")
            continue
        print(f"{label:8} {r['tokens']:>16,} {r['cache_read_pct']:>10.1f}% "
              f"{r['rate_per_mtok']:>12.2f}")

    if args.by_model:
        for label, _ in PERIODS:
            rows = results[label]["by_model"]
            if not rows:
                continue
            print(f"\n## {label}")
            print(f"{'model':14} {'tokens':>16} {'cache read':>11} {'$/1M tokens':>13}")
            for row in rows:
                print(f"{row['model']:14} {row['tokens']:>16,} "
                      f"{row['cache_read_pct']:>10.1f}% {row['rate_per_mtok']:>12.2f}")

    print("\nLower is better. The rate falls as more of your context arrives as cache")
    print("reads (priced at 0.1x input) instead of fresh input, so it measures how well")
    print("your setup reuses context — not how much you worked.")

    if skipped:
        print(f"\nUnpriced models skipped: {', '.join(sorted(skipped))}")
        print("Add them to BASE_INPUT if the rate looks off.")


if __name__ == "__main__":
    main()
