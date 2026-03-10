# CLAUDE.md — What to Include and What to Avoid

> Based on research: [Evaluating AGENTS.md: Are Repository-Level Context Files Helpful for Coding Agents?](https://arxiv.org/abs/2602.11988) (ETH Zurich, Feb 2026)

## What to put in CLAUDE.md

- **Only the bare minimum requirements.** Keep instructions minimal and focused.
- **Repository-specific tooling.** If the project requires specific tools (e.g., `uv` or custom repo utilities), mention them explicitly. Research shows that agents follow tool-usage instructions very accurately when they are stated in the context file.
- **Human-written instructions only.** Developers should write context files by hand — this yields a small but measurable improvement in task success rate (approximately +4% on average).

## What NOT to put in CLAUDE.md

- **Do not use LLM-generated context files.** Research shows that AI-generated context files decrease task success rate (approximately −3% on average) and increase inference cost by over 20%.
- **Do not add codebase overviews.** Listing directories and describing architecture does not help the AI find the right files faster.
- **Avoid duplicating existing documentation.** Generated instructions often repeat what is already documented elsewhere. This forces the AI to spend more reasoning tokens, making the task artificially harder.
