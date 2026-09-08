# Live model comparison — raw notes

Notes taken while running the workshop demo. The point of the exercise was to show
what changes when an AI assistant has engineered context versus none: the same two
tasks were given to several models against the same registration API specification.

These are unedited observations from the room, not a benchmark. No repeated runs,
no scoring rubric, single prompt per model. Treat them as an anecdote that
motivated the talk, not as a measurement.

## Task 1 — generate test cases from the specification

**Gemini 3 (fast)**

> Severity: HIGH.
>
> Quantitatively: AI without context generated 14 tests, 2 of them with critical
> bugs — under 30% coverage.
>
> Qualitatively: did not find a single contradiction in the requirements.

## Task 2 — generate API automated tests

**Gemini 3 (fast)**

> Creates only 4 test examples.
> Does not find contradictions in requirements.

**Claude Haiku**

> Thinks for 8 minutes.
> Spends 60k tokens.
> Creates less than half of the required tests.
> Does not find contradictions in requirements.

## What the room concluded

The failure mode was consistent and it was not about model capability: without the
specification audit step, no model questioned the requirements it was handed. It
generated tests *for the spec as written*, including the parts of the spec that
contradicted each other. That is the gap `/spec-audit` exists to close.
