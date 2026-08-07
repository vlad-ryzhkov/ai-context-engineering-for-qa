package rules

import (
	"os"
	"path/filepath"
	"testing"
)

func TestExtractBannedPatterns_Inline(t *testing.T) {
	content := "BANNED: `Thread.sleep`, `delay(`\nAllowed: `runTest`\nNEVER use `runBlocking`"
	got := ExtractBannedPatterns(content)

	want := map[string]bool{"Thread.sleep": true, "delay(": true, "runBlocking": true}
	for _, p := range got {
		delete(want, p)
	}
	if len(want) > 0 {
		t.Errorf("missing banned patterns: %v", want)
	}
}

func TestExtractBannedPatterns_Table(t *testing.T) {
	content := "| Pattern | Status |\n| `Gson` | ❌ BANNED |\n| `Jackson` | ✅ OK |"
	got := ExtractBannedPatterns(content)

	found := false
	for _, p := range got {
		if p == "Gson" {
			found = true
		}
	}
	if !found {
		t.Errorf("expected 'Gson' in banned patterns, got: %v", got)
	}
}

func TestExtractRequiredPatterns(t *testing.T) {
	content := "REQUIRED: `@DisplayName`, `@Step`\nmust include `HttpTimeout`"
	got := ExtractRequiredPatterns(content)

	want := map[string]bool{"@DisplayName": true, "@Step": true, "HttpTimeout": true}
	for _, p := range got {
		delete(want, p)
	}
	if len(want) > 0 {
		t.Errorf("missing required patterns: %v", want)
	}
}

func TestExtractPostCheckCommands(t *testing.T) {
	content := `## Quality Gate

Some intro text.

` + "```bash" + `
grep -r "Thread.sleep" src/
echo "done"
` + "```" + `

## Other Section

` + "```bash" + `
echo "not in quality gate"
` + "```"

	got := ExtractPostCheckCommands(content)
	if len(got) != 2 {
		t.Fatalf("expected 2 commands, got %d: %v", len(got), got)
	}
	if got[0] != `grep -r "Thread.sleep" src/` {
		t.Errorf("unexpected command[0]: %s", got[0])
	}
}

func TestDeduplication(t *testing.T) {
	content := "BANNED: `foo`, `bar`\nprohibited: `foo`, `baz`"
	got := ExtractBannedPatterns(content)

	count := 0
	for _, p := range got {
		if p == "foo" {
			count++
		}
	}
	if count != 1 {
		t.Errorf("expected 'foo' once, got %d times in %v", count, got)
	}
}

func TestExtractAgainstRealSkill(t *testing.T) {
	// Try to find a real SKILL.md for integration testing
	candidates := []string{
		"../../.claude/skills/api-tests/SKILL.md",
		"../../.claude/skills/api-test-review/SKILL.md",
	}

	var skillPath string
	for _, c := range candidates {
		abs, _ := filepath.Abs(c)
		if _, err := os.Stat(abs); err == nil {
			skillPath = abs
			break
		}
	}

	if skillPath == "" {
		t.Skip("No real SKILL.md found for integration test")
	}

	data, err := os.ReadFile(skillPath)
	if err != nil {
		t.Fatalf("failed to read %s: %v", skillPath, err)
	}
	content := string(data)

	banned := ExtractBannedPatterns(content)
	required := ExtractRequiredPatterns(content)
	meta := ExtractMetadata(content)

	t.Logf("Skill: %s", skillPath)
	t.Logf("Metadata: name=%q agent=%q", meta.Name, meta.Agent)
	t.Logf("Banned patterns: %d", len(banned))
	t.Logf("Required patterns: %d", len(required))

	// Sanity: a real SKILL.md should have at least some patterns
	if len(banned)+len(required) == 0 {
		t.Error("expected at least some banned or required patterns from a real SKILL.md")
	}
}
