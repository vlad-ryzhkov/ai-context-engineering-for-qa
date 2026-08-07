package main

import (
	"encoding/json"
	"fmt"
	"os"
	"tools/internal/rules"
)

func main() {
	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "Usage: skill-rules <SKILL.md> [--format pretty]")
		os.Exit(1)
	}

	skillPath := os.Args[1]
	pretty := false
	for i, arg := range os.Args {
		if arg == "--format" && i+1 < len(os.Args) && os.Args[i+1] == "pretty" {
			pretty = true
		}
	}

	data, err := os.ReadFile(skillPath)
	if err != nil {
		errJSON, _ := json.Marshal(map[string]string{"error": fmt.Sprintf("File not found: %s", skillPath)})
		fmt.Println(string(errJSON))
		os.Exit(1)
	}

	content := string(data)
	meta := rules.ExtractMetadata(content)

	result := rules.SkillRules{
		SkillFile:         skillPath,
		Metadata:          meta,
		Banned:            rules.ExtractBannedPatterns(content),
		Required:          rules.ExtractRequiredPatterns(content),
		PostCheckCommands: rules.ExtractPostCheckCommands(content),
		MetadataIssues:    rules.CheckMetadataCompliance(meta),
	}

	// Ensure empty slices serialize as [] not null
	if result.Banned == nil {
		result.Banned = []string{}
	}
	if result.Required == nil {
		result.Required = []string{}
	}
	if result.PostCheckCommands == nil {
		result.PostCheckCommands = []string{}
	}
	if result.MetadataIssues == nil {
		result.MetadataIssues = []rules.MetadataIssue{}
	}

	var output []byte
	if pretty {
		output, err = json.MarshalIndent(result, "", "  ")
	} else {
		output, err = json.Marshal(result)
	}
	if err != nil {
		fmt.Fprintf(os.Stderr, "JSON encoding error: %v\n", err)
		os.Exit(1)
	}

	fmt.Println(string(output))
}
