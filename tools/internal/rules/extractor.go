package rules

import (
	"regexp"
	"strings"
	"tools/internal/markdown"
)

var (
	bannedKeywords = regexp.MustCompile(
		`(?i)(BANNED|prohibited|forbidden|NEVER\s+use|NEVER\s+import|` +
			`do\s+NOT\s+use|must\s+NOT\s+contain|must\s+NOT\s+use)`)

	tableBanRe = regexp.MustCompile(`(?i)\|.*?(BANNED|❌|prohibited).*?\|`)

	requiredKeywords = regexp.MustCompile(
		`(?i)(REQUIRED|must\s+have|mandatory|must\s+contain|must\s+include|` +
			`must\s+use|always\s+use|MUST\s+be\s+present)`)

	tableReqRe = regexp.MustCompile(`(?i)\|.*?(REQUIRED|✅|mandatory).*?\|`)

	backtickRe = regexp.MustCompile("`([^`]+)`")

	qualitySectionRe = regexp.MustCompile(`(?i)^#+\s*(Quality Gate|Self-Review|Post-Check|Verification)`)
	headingRe        = regexp.MustCompile(`^#+\s`)
)

func extractBacktickPatterns(line string) []string {
	matches := backtickRe.FindAllStringSubmatch(line, -1)
	result := make([]string, 0, len(matches))
	for _, m := range matches {
		if len(m) > 1 {
			result = append(result, m[1])
		}
	}
	return result
}

// deduplicate preserves order.
func deduplicate(items []string) []string {
	seen := make(map[string]struct{}, len(items))
	result := make([]string, 0, len(items))
	for _, item := range items {
		if _, ok := seen[item]; !ok {
			seen[item] = struct{}{}
			result = append(result, item)
		}
	}
	return result
}

// ExtractBannedPatterns extracts BANNED patterns from SKILL.md content.
func ExtractBannedPatterns(content string) []string {
	var patterns []string
	lines := strings.Split(content, "\n")

	for _, line := range lines {
		if bannedKeywords.FindString(line) != "" {
			patterns = append(patterns, extractBacktickPatterns(line)...)
		}
	}

	for _, line := range lines {
		if tableBanRe.FindString(line) != "" {
			patterns = append(patterns, extractBacktickPatterns(line)...)
		}
	}

	return deduplicate(patterns)
}

// ExtractRequiredPatterns extracts REQUIRED patterns from SKILL.md content.
func ExtractRequiredPatterns(content string) []string {
	var patterns []string
	lines := strings.Split(content, "\n")

	for _, line := range lines {
		if requiredKeywords.FindString(line) != "" {
			patterns = append(patterns, extractBacktickPatterns(line)...)
		}
	}

	for _, line := range lines {
		if tableReqRe.FindString(line) != "" {
			for _, p := range extractBacktickPatterns(line) {
				if len(p) < 80 && !strings.HasPrefix(p, "http") {
					patterns = append(patterns, p)
				}
			}
		}
	}

	return deduplicate(patterns)
}

// ExtractPostCheckCommands extracts post-check commands from code blocks
// within Quality Gate / Self-Review / Post-Check / Verification sections.
func ExtractPostCheckCommands(content string) []string {
	var commands []string
	inQualitySection := false
	inCodeBlock := false
	var codeBlockContent []string
	var tracker markdown.CodeBlockTracker

	for _, line := range strings.Split(content, "\n") {
		wasInside := tracker.Inside()
		tracker.Update(line)
		isInside := tracker.Inside()

		// Only track quality sections outside of code blocks
		if !wasInside && !isInside {
			if qualitySectionRe.MatchString(line) {
				inQualitySection = true
				continue
			} else if headingRe.MatchString(line) && inQualitySection {
				inQualitySection = false
			}
		}

		if inQualitySection {
			trimmed := strings.TrimSpace(line)
			if strings.HasPrefix(trimmed, "```") {
				if inCodeBlock {
					// End of code block — collect commands
					for _, cmdLine := range codeBlockContent {
						cmdLine = strings.TrimSpace(cmdLine)
						if cmdLine != "" && !strings.HasPrefix(cmdLine, "#") {
							commands = append(commands, cmdLine)
						}
					}
					codeBlockContent = nil
					inCodeBlock = false
				} else {
					inCodeBlock = true
				}
			} else if inCodeBlock {
				codeBlockContent = append(codeBlockContent, line)
			}
		}
	}

	return commands
}
