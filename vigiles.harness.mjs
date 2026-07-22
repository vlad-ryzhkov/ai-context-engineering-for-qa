/**
 * Starter harness test (Pillar 2) — scaffolded by `vigiles init`.
 * Proves a hook actually FIRES, deterministically and with no API key.
 *
 *   npm i -D vigiles          # the testing API this imports
 *   npx vigiles test          # or: node vigiles.harness.mjs
 *
 * Guide: https://github.com/zernie/vigiles/blob/main/docs/harness-testing.md
 */
import { runHook } from "vigiles/testing";
import assert from "node:assert/strict";

// EXAMPLE — replace with one of YOUR hooks. This PreToolUse Bash guard blocks a
// destructive command; runHook pipes it a fake event and checks the decision.
const guard =
  `CMD=$(cat | jq -r '.tool_input.command // empty'); ` +
  `case "$CMD" in *"rm -rf /"*) echo blocked >&2; exit 2 ;; esac; exit 0`;

const blocked = runHook(guard, {
  hook_event_name: "PreToolUse",
  tool_name: "Bash",
  tool_input: { command: "rm -rf / --no-preserve-root" },
});
assert.ok(blocked.blocked, "guard should block \`rm -rf /\`");

const allowed = runHook(guard, {
  hook_event_name: "PreToolUse",
  tool_name: "Bash",
  tool_input: { command: "ls -la" },
});
assert.ok(!allowed.blocked, "guard should allow a safe command");

console.log("\u2713 hook blocks rm -rf / and allows safe commands");
