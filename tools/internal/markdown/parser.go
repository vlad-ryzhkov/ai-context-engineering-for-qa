package markdown

import "strings"

// IsInsideCodeBlock tracks whether a line is inside a fenced code block.
// Call for each line sequentially; returns true if the line is inside a code block.
type CodeBlockTracker struct {
	inside bool
}

func (t *CodeBlockTracker) Update(line string) bool {
	trimmed := strings.TrimSpace(line)
	if strings.HasPrefix(trimmed, "```") {
		t.inside = !t.inside
	}
	return t.inside
}

func (t *CodeBlockTracker) Inside() bool {
	return t.inside
}

// IsHeading returns true if the line starts with one or more '#' characters.
func IsHeading(line string) bool {
	trimmed := strings.TrimSpace(line)
	return len(trimmed) > 0 && trimmed[0] == '#'
}

// ExtractFrontmatter extracts YAML frontmatter from content starting with "---".
// Returns frontmatter string and the remaining content.
func ExtractFrontmatter(content string) (frontmatter string, rest string, ok bool) {
	if !strings.HasPrefix(content, "---") {
		return "", content, false
	}
	idx := strings.Index(content[3:], "---")
	if idx < 0 {
		return "", content, false
	}
	return strings.TrimSpace(content[3 : 3+idx]), content[3+idx+3:], true
}
