# Adapting these skills to other tools

The skills here target Claude Code. They are plain markdown, so any assistant
that can read a file can run them — but earlier versions of this repository
shipped a generated wrapper per skill per IDE (`.cursor/rules/*.mdc`,
`.agents/skills/*`, `.junie/guidelines.md`, `copilot-instructions.md`,
`GEMINI.md`). That was 25 near-duplicates of the same content, each drifting
from the original at its own pace. They were removed; one sample of each format
is kept under [`archive/ide-wrappers/`](../archive/ide-wrappers/) and the rest
is in git history.

This page replaces them.

## The one thing you need to know

A skill is a markdown file with YAML frontmatter:

```text
.claude/skills/<name>/
├── SKILL.md          # frontmatter + instructions
└── references/       # loaded only when SKILL.md points at them
```

Frontmatter carries `name` and `description`; everything below it is the
instruction body. Nothing is compiled and nothing is tool-specific. Adapting a
skill to another assistant means answering one question: **how does this tool
receive a body of instructions, and when?**

## Per-tool

| Tool | How to run a skill |
| ---- | ------------------ |
| **Claude Code** | Native. `/skill-name`, or the model picks it from the `description`. |
| **Codex** | Reads [`AGENTS.md`](../AGENTS.md), which points here and at `SKILLS.md`. Reference a skill by path in your prompt: "follow `.claude/skills/api-tests/SKILL.md`". Codex also supports its own [agent skills](https://developers.openai.com/codex/skills/) if you want native invocation. |
| **Cursor** | Has [native skills](https://cursor.com/docs/context/skills). Point it at `.claude/skills/`, or paste a `SKILL.md` into a `.cursor/rules/*.mdc` file with `alwaysApply: false` so it loads on demand. |
| **GitHub Copilot** (VS Code, JetBrains) | Uses [custom instructions](https://code.visualstudio.com/docs/copilot/customization/custom-instructions). Put the always-on part of `CLAUDE.md` in `.github/copilot-instructions.md`; attach a `SKILL.md` per request with `#file:`. |
| **JetBrains AI / Junie** | Reads `.junie/guidelines.md`. Put a pointer to `CLAUDE.md` there; attach `SKILL.md` files to the chat as context. |
| **Gemini Code Assist** | Reads `GEMINI.md`. Symlink or copy `CLAUDE.md` to it; paste skills manually. |
| **Anything else** | Paste `SKILL.md` into the chat. It is a prompt. |

## Two rules that survive the port

**Keep the always-on file small.** Whatever your tool calls its always-loaded
instruction file, it is re-read every turn. Bloated or model-generated context
files measurably *reduce* success rate and raise cost. Tech stack, commands,
banned alternatives — nothing else. See
[`claudemd-instructions.md`](claudemd-instructions.md).

**Load skills on demand, not always.** The value of the
`SKILL.md` + `references/` split is that a 500-line skill costs nothing until
it is needed. A tool that concatenates every skill into one always-on prompt
throws that away and will cost more for worse results. If your tool cannot load
on demand, attach skills per request by hand rather than pre-loading them all.

## Token cost warning

Non-Claude tools often re-send attached context on every turn with no prompt
caching. Check token usage after running a skill; if it is significantly higher
than expected, fall back to that tool's own native structure rather than
attaching these files verbatim.
