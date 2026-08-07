package normalizer

import "regexp"

type normPattern struct {
	re          *regexp.Regexp
	replacement string
}

var patterns = []normPattern{
	// ISO timestamps: 2026-03-16T12:34:56Z, 2026-03-16 12:34:56
	{regexp.MustCompile(`\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?`), "TIMESTAMP"},
	// Date only: 2026-03-16
	{regexp.MustCompile(`\b\d{4}-\d{2}-\d{2}\b`), "DATE"},
	// UUIDs: 550e8400-e29b-41d4-a716-446655440000
	{regexp.MustCompile(`(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b`), "UUID"},
	// Short hashes (git-like): abc1234
	{regexp.MustCompile(`\b[0-9a-f]{7,12}\b`), "HASH"},
	// Absolute paths: /Users/foo/bar, /home/user/project
	{regexp.MustCompile(`/(?:Users|home|tmp|var|opt|etc)/[^\s|` + "`" + `\]>]+`), "/PATH"},
	// IP addresses
	{regexp.MustCompile(`\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b`), "IP_ADDR"},
	// Port numbers in URLs (e.g. :8080/ or :8080 at word boundary)
	{regexp.MustCompile(`:\d{4,5}(?:[/\s]|$)`), ":PORT"},
	// Token counts with specific numbers
	{regexp.MustCompile(`(?i)\b\d{3,7}\s*tokens?\b`), "N_TOKENS"},
	// Duration: 12.5s, 3.2 seconds
	{regexp.MustCompile(`(?i)\b\d+\.?\d*\s*(?:seconds?|s|ms|minutes?|min)\b`), "DURATION"},
}

var multiSpaceRe = regexp.MustCompile(`  +`)
