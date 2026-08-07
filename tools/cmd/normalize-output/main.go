package main

import (
	"fmt"
	"io"
	"os"
	"tools/internal/normalizer"
)

func main() {
	sortLists := false
	for _, arg := range os.Args[1:] {
		if arg == "--sort-lists" {
			sortLists = true
		}
	}

	stat, _ := os.Stdin.Stat()
	if (stat.Mode()&os.ModeCharDevice) != 0 && len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "Usage: cat output.md | normalize-output [--sort-lists]")
		fmt.Fprintln(os.Stderr, "       normalize-output < output.md")
		os.Exit(1)
	}

	data, err := io.ReadAll(os.Stdin)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error reading stdin: %v\n", err)
		os.Exit(1)
	}

	normalized := normalizer.NormalizeOutput(string(data), sortLists)
	fmt.Print(normalized)
}
