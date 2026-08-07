package normalizer

import (
	"strings"
	"testing"
)

func TestNormalizeLine_Timestamps(t *testing.T) {
	tests := []struct {
		input string
		want  string
	}{
		{"Generated at 2026-03-16T12:34:56Z", "Generated at TIMESTAMP"},
		{"Date: 2026-03-16 12:34:56", "Date: TIMESTAMP"},
		{"On 2026-03-16 something happened", "On DATE something happened"},
	}
	for _, tt := range tests {
		got := NormalizeLine(tt.input)
		if got != tt.want {
			t.Errorf("NormalizeLine(%q) = %q, want %q", tt.input, got, tt.want)
		}
	}
}

func TestNormalizeLine_UUIDs(t *testing.T) {
	input := "ID: 550e8400-e29b-41d4-a716-446655440000"
	got := NormalizeLine(input)
	if !strings.Contains(got, "UUID") {
		t.Errorf("expected UUID placeholder, got: %s", got)
	}
}

func TestNormalizeLine_Paths(t *testing.T) {
	input := "File: /Users/foo/project/src/main.go"
	got := NormalizeLine(input)
	if !strings.Contains(got, "/PATH") {
		t.Errorf("expected /PATH placeholder, got: %s", got)
	}
}

func TestNormalizeLine_IP(t *testing.T) {
	input := "Server: 192.168.1.100"
	got := NormalizeLine(input)
	if !strings.Contains(got, "IP_ADDR") {
		t.Errorf("expected IP_ADDR placeholder, got: %s", got)
	}
}

func TestNormalizeLine_Tokens(t *testing.T) {
	input := "Used 12345 tokens"
	got := NormalizeLine(input)
	if !strings.Contains(got, "N_TOKENS") {
		t.Errorf("expected N_TOKENS placeholder, got: %s", got)
	}
}

func TestNormalizeLine_Duration(t *testing.T) {
	input := "Took 12.5s to complete"
	got := NormalizeLine(input)
	if !strings.Contains(got, "DURATION") {
		t.Errorf("expected DURATION placeholder, got: %s", got)
	}
}

func TestNormalizeLine_MultipleSpaces(t *testing.T) {
	input := "  Hello   world   here"
	got := NormalizeLine(input)
	if got != "  Hello world here" {
		t.Errorf("expected collapsed spaces with preserved indent, got: %q", got)
	}
}

func TestNormalizeOutput_CodeBlockPreservation(t *testing.T) {
	input := "normal 2026-03-16 text\n```\n2026-03-16 inside code\n```\nnormal 2026-03-16 again"
	got := NormalizeOutput(input, false)

	lines := strings.Split(got, "\n")
	if !strings.Contains(lines[0], "DATE") {
		t.Errorf("line 0 should be normalized: %s", lines[0])
	}
	if strings.Contains(lines[2], "DATE") {
		t.Errorf("line inside code block should NOT be normalized: %s", lines[2])
	}
	if !strings.Contains(lines[4], "DATE") {
		t.Errorf("line 4 should be normalized: %s", lines[4])
	}
}

func TestNormalizeOutput_SortLists(t *testing.T) {
	input := "Header\n- Zebra\n- Alpha\n- Middle\nParagraph"
	got := NormalizeOutput(input, true)
	lines := strings.Split(got, "\n")

	// After sorting: Alpha, Middle, Zebra
	if lines[1] != "- Alpha" {
		t.Errorf("expected sorted first item '- Alpha', got: %s", lines[1])
	}
	if lines[2] != "- Middle" {
		t.Errorf("expected sorted second item '- Middle', got: %s", lines[2])
	}
	if lines[3] != "- Zebra" {
		t.Errorf("expected sorted third item '- Zebra', got: %s", lines[3])
	}
}
