# Context map — format for docs/context-map.md

A reusable responsibility table of the whole context corpus. Generated/refreshed by `improve-project-context` step 6. Lets the next audit start from a known topology instead of rebuilding the dep graph from scratch.

## Shape

```markdown
# Context map — generated <YYYY-MM-DD> by /improve-project-context

> Single source of responsibility per artifact. Overlaps flagged → resolve via dedup.

## Always-on (loaded every session)

| Artifact          | Owns (single responsibility)                            | LOC | Overlaps with                                                 |
| ----------------- | ------------------------------------------------------- | --: | ------------------------------------------------------------- |
| CLAUDE.md         | entry + JIT routing pointers + short always-on guidance | 127 | —                                                             |
| rules/git.md      | git safety, commit format, Co-authored ban              |  33 | docs/commit-messages.md (commit format — git.md = hook truth) |
| rules/security.md | hardcode/secret bans (path-scoped)                      |   7 | —                                                             |
| …                 | …                                                       |   … | …                                                             |

## JIT (loaded on trigger)

| Artifact                     | Owns                        | Triggered by                |
| ---------------------------- | --------------------------- | --------------------------- |
| docs/commit-messages.md      | full commit authoring rules | `git commit -m` reflex      |
| docs/code-review-patterns.md | review patterns             | review trigger in CLAUDE.md |
| …                            | …                           | …                           |

## Skills / commands

| Skill                         | Owns                          | Conflicts/overlaps                                |
| ----------------------------- | ----------------------------- | ------------------------------------------------- |
| improve-context-from-sessions | session-delta context patches | improve-project-context (cold vs warm — distinct) |
| docs/commit-messages.md       | commit message STYLE          | rules/git.md overrides (Co-authored, TICKET)      |
| …                             | …                             | …                                                 |

## Open overlaps (need resolution)

- <artifact A> ↔ <artifact B>: <what overlaps> → proposed owner: <X>
```

## Rules

- ONE responsibility sentence per artifact. If you can't state it in one line, the artifact does too much → flag.
- "Overlaps with" column is the dedup memory — populated from step 3 + step 4 findings.
- Regenerate only when topology changes (new rule/skill/hook, or evacuation). Stale map = delete + rebuild.
- Map itself is a docs/ file → JIT, never always-on. Does NOT count against the combined cap.
