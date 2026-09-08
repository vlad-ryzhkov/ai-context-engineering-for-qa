#!/usr/bin/env python3
"""Deterministic integrity check for the AI context corpus.

Verifies what a cold audit keeps missing by hand: that every path referenced
from a model-loaded file resolves, that skill names do not collide, that the
always-loaded token budget is respected, and that hooks point at real scripts.

Exit 0 = clean, 1 = findings. No model, no network.
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path

HOME = Path(os.environ.get("CLAUDE_HOME", Path.home() / ".claude"))

HOME_REF_RE = re.compile(
    r"(?:~/\.claude/|\$HOME/\.claude/)"
    r"((?:docs|rules|skills|hooks|scripts|commands)/[A-Za-z0-9_./-]+"
    r"\.(?:md|py|sh|json))"
)
BARE_REF_RE = re.compile(
    r"`((?:docs|rules|skills|hooks|commands)/[A-Za-z0-9_./-]+\.md)`"
)
FOREIGN_RE = re.compile(
    r"/Users/|/home/|https?://|<repo>|_local-plans/"
    r"|in the repo|repo using|repo root"
    r"|repo-local|repo local|test repos|per repo|each repo|project root"
    r"|not implemented|opts? in|e\.g\.|for example|such as"
    r"|^\s*[-*]\s*[✅❌]|✅|❌|<[a-z_]+>/",
    re.IGNORECASE,
)
SKIP_DIRS = {"plugins", ".git", "node_modules", ".codegraph", "__pycache__"}

PLACEHOLDER_RE = re.compile(
    r"/(X|Y|N|foo|bar|name|skill|topic|file)\.(md|py|sh)$"
    r"|/\.\.\./|<[a-z-]+>|\{[a-z_-]+\}",
    re.IGNORECASE,
)
OPTIONAL_RE = re.compile(
    r"optional|not shipped|absent by default|if it exists|may be absent"
    r"|create it|opt-?in|relative to the repo under review",
    re.IGNORECASE,
)
ARCHIVE_PARTS = {"plans", "specs", "fixtures", "archive"}


def loaded_md_files() -> list[Path]:
    out = [HOME / "CLAUDE.md"]
    for base in ("rules", "docs", "skills", "commands"):
        d = HOME / base
        if not d.is_dir():
            continue
        for p in d.rglob("*.md"):
            if not any(part in SKIP_DIRS for part in p.parts):
                out.append(p)
    return [p for p in out if p.is_file()]


def is_always_on(path: Path) -> bool:
    """A rules/ file with no YAML `paths:` front-matter loads every session."""
    try:
        head = path.read_text(errors="replace").split("\n", 12)[:12]
    except OSError:
        return False
    if not head or head[0].strip() != "---":
        return True
    return not any(ln.startswith("paths:") for ln in head)


def check_refs(files: list[Path]) -> list[str]:
    """Dangling path references in live context files.

    Skips illustrative placeholders and dated plan/spec archives: those cite
    paths that never existed or have since been shipped, so they are noise.
    """
    findings = []
    for f in files:
        if ARCHIVE_PARTS & set(f.parts):
            continue
        try:
            text = f.read_text(errors="replace")
        except OSError:
            continue
        lines = text.splitlines()
        for lineno, line in enumerate(lines, 1):
            if "NOT INSTALLED" in line or "BANNED" in line:
                continue
            block = "\n".join(lines[max(0, lineno - 15):lineno])
            if OPTIONAL_RE.search(block):
                continue
            refs = set(HOME_REF_RE.findall(line))
            if not FOREIGN_RE.search(block):
                refs |= set(BARE_REF_RE.findall(line))
            for ref in sorted(refs):
                if PLACEHOLDER_RE.search("/" + ref):
                    continue
                if (HOME / ref).exists() or (f.parent / ref).exists():
                    continue
                rel = f.relative_to(HOME)
                hint = suggest(ref)
                findings.append(
                    f"DANGLING  {rel}:{lineno} -> {ref}"
                    + (f"   (did you mean {hint}?)" if hint else "")
                )
    return findings


def suggest(ref: str) -> str | None:
    """Same basename elsewhere in the corpus — catches rules/<->docs/ moves."""
    name = Path(ref).name
    for base in ("rules", "docs", "scripts", "skills", "hooks", "commands"):
        d = HOME / base
        if not d.is_dir():
            continue
        for hit in d.rglob(name):
            if any(p in SKIP_DIRS for p in hit.parts):
                continue
            return str(hit.relative_to(HOME))
    return None


def budget_constants() -> tuple[int, float]:
    """Cap and bytes-per-token come from docs/aggregate-budget.md, never from a
    hardcoded duplicate here — two copies drift and the gate stops binding."""
    cap, bpt = 10000, 2.4
    try:
        text = (HOME / "docs" / "aggregate-budget.md").read_text(errors="replace")
    except OSError:
        return cap, bpt
    m = re.search(r"context-check:\s*TOKEN_CAP=(\d+)\s+BYTES_PER_TOKEN=([\d.]+)", text)
    if m:
        cap, bpt = int(m.group(1)), float(m.group(2))
    if "CONTEXT_TOKEN_CAP" in os.environ:
        cap = int(os.environ["CONTEXT_TOKEN_CAP"])
    return cap, bpt


def always_loaded_files() -> list[Path]:
    """Both pools /context sums into one "Memory files" number: the global
    ~/.claude corpus and the current repo's CLAUDE.md + .claude/rules/**."""
    roots = [HOME]
    proj = os.environ.get("CLAUDE_PROJECT_DIR")
    if proj:
        p = Path(proj)
        if p.resolve() != HOME.resolve():
            roots.append(p)
    parts: list[Path] = []
    for root in roots:
        entry = root / "CLAUDE.md"
        if entry.is_file():
            parts.append(entry)
        rules = root / "rules" if root == HOME else root / ".claude" / "rules"
        if rules.is_dir():
            parts += [p for p in sorted(rules.glob("*.md")) if is_always_on(p)]
    return parts


def check_budget() -> list[str]:
    cap, bpt = budget_constants()
    parts = always_loaded_files()
    total = 0
    for p in parts:
        try:
            total += len(p.read_bytes())
        except OSError:
            pass
    tokens = int(total / bpt)
    if tokens > cap:
        worst = sorted(parts, key=lambda p: -p.stat().st_size)[:3]
        top = ", ".join(f"{p.name} ~{int(p.stat().st_size / bpt)}" for p in worst)
        return [f"BUDGET    always-loaded ~{tokens} tok > cap {cap} "
                f"({len(parts)} files, {total} bytes @ {bpt} B/tok); "
                f"biggest: {top}"]
    return []


def check_skill_collisions() -> list[str]:
    seen: dict[str, list[str]] = {}
    for skill in (HOME / "skills").glob("*/SKILL.md"):
        try:
            text = skill.read_text(errors="replace")
        except OSError:
            continue
        m = re.search(r"^name:\s*(\S+)", text, re.MULTILINE)
        name = m.group(1) if m else skill.parent.name
        seen.setdefault(name, []).append(skill.parent.name)
    out = []
    for name, dirs in sorted(seen.items()):
        if len(dirs) > 1:
            out.append(f"COLLISION skill name {name!r} declared by {dirs}")
    return out


def check_hooks() -> list[str]:
    findings = []
    for cfg in ("settings.json", "settings.local.json"):
        p = HOME / cfg
        if not p.is_file():
            continue
        try:
            raw = p.read_text(errors="replace")
        except OSError:
            continue
        try:
            json.loads(raw)
        except (json.JSONDecodeError, ValueError) as e:
            findings.append(f"BADJSON   {cfg}: {e}")
            continue
        for script in re.findall(r"\$HOME/\.claude/([A-Za-z0-9_./-]+\.(?:sh|py))", raw):
            if not (HOME / script).exists():
                findings.append(f"HOOKMISS  {cfg} -> {script}")
    return findings


def check_scripts_executable() -> list[str]:
    findings = []
    d = HOME / "scripts"
    if not d.is_dir():
        return findings
    for p in sorted(d.glob("*.sh")):
        if not os.access(p, os.X_OK):
            findings.append(f"NOTEXEC   scripts/{p.name}")
    return findings


def check_symlinks() -> list[str]:
    findings = []
    for base in ("skills", "commands"):
        d = HOME / base
        if not d.is_dir():
            continue
        for p in sorted(d.iterdir()):
            if p.is_symlink() and not p.resolve().exists():
                findings.append(f"BROKENLNK {base}/{p.name} -> {os.readlink(p)}")
    return findings


def check_git_clean() -> list[str]:
    try:
        r = subprocess.run(["git", "status", "--porcelain"], cwd=HOME,
                           capture_output=True, text=True, timeout=15)
    except (OSError, subprocess.SubprocessError):
        return []
    if r.returncode != 0:
        return []
    tracked = [ln for ln in r.stdout.splitlines()
               if ln[:2] not in ("??",) and "plugins/" not in ln]
    if len(tracked) > 12:
        return [f"UNCOMMIT  {len(tracked)} tracked files dirty in ~/.claude"]
    return []


def calibrate(reported_tokens: int) -> int:
    """Re-derive bytes-per-token from what /context actually reported.

    The stored ratio is an empirical constant, so it drifts as the corpus
    changes shape (more Cyrillic, more arrows, more code fences). Feed it the
    "Memory files" token number from /context and it says whether the constant
    still holds and what to write into docs/aggregate-budget.md if not.
    """
    cap, stored = budget_constants()
    parts = always_loaded_files()
    total = sum(p.stat().st_size for p in parts)
    if reported_tokens <= 0:
        print("calibrate: need the positive token count /context reported")
        return 2
    actual = total / reported_tokens
    drift = abs(actual - stored) / stored
    print(f"files counted     {len(parts)}")
    print(f"bytes             {total}")
    print(f"/context tokens   {reported_tokens}")
    print(f"stored  B/tok     {stored}")
    print(f"measured B/tok    {actual:.2f}")
    print(f"drift             {drift * 100:.1f}%")
    print(f"estimate at stored ratio  {int(total / stored)} tok "
          f"(cap {cap})")
    if drift > 0.10:
        print(f"\nDRIFT >10% — update the marker in docs/aggregate-budget.md to "
              f"BYTES_PER_TOKEN={actual:.2f}")
        return 1
    print("\nratio still holds, no edit needed")
    return 0


def _selftest() -> int:
    import tempfile
    passed = failed = 0

    def check(label: str, got, want) -> None:
        nonlocal passed, failed
        if got == want:
            print(f"PASS  {label}")
            passed += 1
        else:
            print(f"FAIL  {label}  (want {want!r}, got {got!r})")
            failed += 1

    with tempfile.TemporaryDirectory() as td:
        home = Path(td) / "home"
        (home / "docs").mkdir(parents=True)
        (home / "rules").mkdir()
        proj = Path(td) / "proj" / ".claude" / "rules"
        proj.mkdir(parents=True)

        (home / "docs" / "aggregate-budget.md").write_text(
            "<!-- context-check: TOKEN_CAP=1000 BYTES_PER_TOKEN=2.0 -->\n")
        (home / "CLAUDE.md").write_text("x" * 2000)
        (home / "rules" / "always.md").write_text("y" * 400)
        (home / "rules" / "scoped.md").write_text(
            "---\npaths:\n  - '**/*.go'\n---\nz\n")
        (proj.parent.parent / "CLAUDE.md").write_text("p" * 200)
        (proj / "team.md").write_text("q" * 200)

        env = dict(os.environ, CLAUDE_HOME=str(home))
        env.pop("CONTEXT_TOKEN_CAP", None)

        def run(*args, project: str | None = None) -> tuple[int, str]:
            e = dict(env)
            if project:
                e["CLAUDE_PROJECT_DIR"] = project
            else:
                e.pop("CLAUDE_PROJECT_DIR", None)
            r = subprocess.run([sys.executable, __file__, *args],
                               capture_output=True, text=True, env=e)
            return r.returncode, r.stdout

        rc, out = run("--budget-only")
        check("constants parsed from the doc, not hardcoded",
              "cap 1000" in out, True)
        check("global pool over cap exits 1", rc, 1)
        check("path-scoped global rule excluded (2000+400)/2.0=1200",
              "~1200 tok" in out, True)

        rc, out = run("--budget-only", project=str(Path(td) / "proj"))
        check("project pool added (2000+400+200+200)/2.0=1400",
              "~1400 tok" in out, True)

        (home / "CLAUDE.md").write_text("x" * 200)
        rc, out = run("--budget-only")
        check("under cap prints nothing", out.strip(), "")
        check("under cap exits 0", rc, 0)

        rc, out = run("--calibrate", "300")
        check("calibrate reports measured ratio 600/300=2.00",
              "measured B/tok    2.00" in out, True)
        check("calibrate clean exits 0", rc, 0)

        rc, out = run("--calibrate", "150")
        check("calibrate flags drift when real ratio is 4.00", rc, 1)
        check("calibrate names the replacement constant",
              "BYTES_PER_TOKEN=4.00" in out, True)

        rc, out = run("--calibrate", "0")
        check("calibrate rejects a non-positive count", rc, 2)

    print(f"\nResult: {passed} pass / {failed} fail")
    return 0 if failed == 0 else 1


def main() -> int:
    if "--test" in sys.argv:
        return _selftest()
    if "--calibrate" in sys.argv:
        i = sys.argv.index("--calibrate")
        arg = sys.argv[i + 1] if i + 1 < len(sys.argv) else ""
        try:
            return calibrate(int(arg))
        except ValueError:
            print("usage: context-check.py --calibrate <tokens from /context>")
            return 2
    if "--budget-only" in sys.argv:
        items = check_budget()
        for it in items:
            print(it)
        return 1 if items else 0
    groups = [
        ("references", check_refs(loaded_md_files())),
        ("budget", check_budget()),
        ("skills", check_skill_collisions()),
        ("hooks", check_hooks()),
        ("scripts", check_scripts_executable()),
        ("symlinks", check_symlinks()),
        ("git", check_git_clean()),
    ]
    total = sum(len(v) for _, v in groups)
    print(f"context-check: {HOME}")
    for label, items in groups:
        status = "OK" if not items else f"{len(items)} finding(s)"
        print(f"  [{'ok ' if not items else 'FAIL'}] {label:<12} {status}")
        for it in items:
            print(f"        {it}")
    print(f"\n{'CLEAN' if total == 0 else f'{total} finding(s)'}")
    return 0 if total == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
