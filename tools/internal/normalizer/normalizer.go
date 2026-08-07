package normalizer

import (
	"regexp"
	"sort"
	"strings"
)

var listRe = regexp.MustCompile(`^(\s*)([-*+]|\d+\.)\s`)

// NormalizeLine applies normalization patterns to a single line.
func NormalizeLine(line string) string {
	for _, p := range patterns {
		line = p.re.ReplaceAllString(line, p.replacement)
	}

	// Collapse multiple spaces (preserve leading whitespace for indentation)
	leading := 0
	for leading < len(line) && (line[leading] == ' ' || line[leading] == '\t') {
		leading++
	}
	content := multiSpaceRe.ReplaceAllString(line[leading:], " ")
	return line[:leading] + content
}

// NormalizeOutput normalizes LLM output while preserving markdown structure.
func NormalizeOutput(text string, sortLists bool) string {
	lines := strings.Split(text, "\n")
	var result []string
	inCodeBlock := false
	var currentListItems []string
	listIndent := 0

	flushList := func() {
		if len(currentListItems) > 0 {
			if sortLists {
				sort.Strings(currentListItems)
			}
			result = append(result, currentListItems...)
			currentListItems = nil
		}
	}

	for _, line := range lines {
		stripped := strings.TrimSpace(line)

		// Track code blocks — don't normalize inside them
		if strings.HasPrefix(stripped, "```") {
			flushList()
			inCodeBlock = !inCodeBlock
			result = append(result, line)
			continue
		}

		if inCodeBlock {
			result = append(result, line)
			continue
		}

		// Normalize the line
		normalized := NormalizeLine(line)

		// Track list items for optional sorting
		match := listRe.FindStringSubmatch(normalized)
		if match != nil && sortLists {
			indent := len(match[1])
			if len(currentListItems) > 0 && indent != listIndent {
				flushList()
			}
			listIndent = indent
			currentListItems = append(currentListItems, normalized)
		} else {
			flushList()
			result = append(result, normalized)
		}
	}

	flushList()
	return strings.Join(result, "\n")
}
