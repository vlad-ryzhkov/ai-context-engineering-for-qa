# Archive

Material kept for reference, no longer part of the active setup. Nothing here is
loaded by an AI assistant and nothing here is maintained.

| Folder         | What it holds                                        | Why it was archived                                                              |
| -------------- | ---------------------------------------------------- | -------------------------------------------------------------------------------- |
| `agents/`      | Two subagent definitions and the `agents-checker` skill that validated them | Skills replaced them — see [Why agents were archived](agents/README.md)           |
| `ide-wrappers/` | One sample Codex wrapper, one sample Cursor rule      | Per-IDE duplicates of the same skills; see [docs/adapting-to-other-tools.md](../docs/adapting-to-other-tools.md) |
| `superseded/`  | `/update-ai-setup`                                    | Maintained a generated inventory that [`SKILLS.md`](../SKILLS.md) replaced by hand |

Full content of everything removed here stays in git history. To read a deleted
file, find the commit that removed it and check out that path:

```bash
git log --diff-filter=D --name-only -- '.agents/**'
git show <commit>^:.agents/skills/api-tests/SKILL.md
```
