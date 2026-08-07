package rules

// SkillRules holds all extracted rules from a SKILL.md file.
type SkillRules struct {
	SkillFile         string          `json:"skill_file"`
	Metadata          Metadata        `json:"metadata"`
	Banned            []string        `json:"banned"`
	Required          []string        `json:"required"`
	PostCheckCommands []string        `json:"post_check_commands"`
	MetadataIssues    []MetadataIssue `json:"metadata_issues"`
}

// Metadata holds YAML frontmatter fields.
type Metadata struct {
	Name        string `json:"name,omitempty"`
	Agent       string `json:"agent,omitempty"`
	Context     string `json:"context,omitempty"`
	Description string `json:"description,omitempty"`
}

// MetadataIssue represents a compliance finding for metadata.
type MetadataIssue struct {
	ID       string `json:"id"`
	Severity string `json:"severity"`
	Message  string `json:"message"`
}
