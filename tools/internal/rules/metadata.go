package rules

import (
	"fmt"
	"regexp"
	"strings"
	"tools/internal/markdown"
)

var triggerRe = regexp.MustCompile(`(?i)(when|use this|trigger|invoke|run this)`)
// xmlInjectionCheck detects non-safe HTML tags in description.
// Go regexp doesn't support lookaheads, so we check manually.
var safeHTMLTags = map[string]bool{"br": true, "em": true, "strong": true, "code": true, "pre": true}
var htmlTagRe = regexp.MustCompile(`</?([a-zA-Z][a-zA-Z0-9]*)`)

func hasXMLInjection(s string) bool {
	matches := htmlTagRe.FindAllStringSubmatch(s, -1)
	for _, m := range matches {
		tag := strings.ToLower(m[1])
		if !safeHTMLTags[tag] {
			return true
		}
	}
	return false
}

var reservedWords = []string{"test", "debug", "temp", "tmp", "foo", "bar", "example"}

// ExtractMetadata parses YAML frontmatter and returns Metadata.
func ExtractMetadata(content string) Metadata {
	fm, _, ok := markdown.ExtractFrontmatter(content)
	if !ok {
		return Metadata{}
	}

	meta := Metadata{}
	for _, line := range strings.Split(fm, "\n") {
		if !strings.Contains(line, ":") {
			continue
		}
		key, value, _ := strings.Cut(line, ":")
		key = strings.TrimSpace(key)
		value = strings.TrimSpace(value)
		value = strings.Trim(value, "\"'")

		switch key {
		case "name":
			meta.Name = value
		case "agent":
			meta.Agent = value
		case "context":
			meta.Context = value
		case "description":
			meta.Description = value
		}
	}
	return meta
}

// CheckMetadataCompliance performs M1-M3 checks on metadata.
func CheckMetadataCompliance(meta Metadata) []MetadataIssue {
	var issues []MetadataIssue

	// M1: Description should contain trigger conditions
	if meta.Description != "" && !triggerRe.MatchString(meta.Description) {
		issues = append(issues, MetadataIssue{
			ID:       "M1",
			Severity: "WARN",
			Message:  "Description lacks trigger conditions (when to use this skill)",
		})
	}

	// M2: Reserved words in skill name
	if meta.Name != "" {
		parts := strings.Split(strings.ToLower(meta.Name), "-")
		for _, word := range reservedWords {
			for _, part := range parts {
				if part == word {
					issues = append(issues, MetadataIssue{
						ID:       "M2",
						Severity: "WARN",
						Message:  fmt.Sprintf("Skill name contains reserved word: %s", word),
					})
				}
			}
		}
	}

	// M3: Check for potential XML/HTML injection in description
	if hasXMLInjection(meta.Description) {
		issues = append(issues, MetadataIssue{
			ID:       "M3",
			Severity: "ERROR",
			Message:  "Description contains potential XML/HTML injection",
		})
	}

	return issues
}
