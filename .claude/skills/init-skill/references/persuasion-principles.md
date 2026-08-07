# Persuasion Principles for Skill Authors

> Based on Meincke et al. 2025 (N=28,000): LLMs are "parahuman" — trained on compliance patterns, responsive to the same persuasion principles that work on humans.

## Principle-to-Skill-Type Mapping

| Skill Type | Primary Principles | Why This Combination |
|---|---|---|
| **Discipline-enforcing** (`/api-test-review`, `/skill-audit`) | Authority + Commitment + Social Proof | Bright-line rules reduce rationalization; implementation intentions create automatic behavior |
| **Code generation** (`/api-tests`, `/api-mocks`) | Authority + Commitment | Clear constraints prevent drift; commitment to checklist prevents shortcuts |
| **Guidance / analytical** (`/spec-audit`, `/repo-scout`) | Moderate Authority + Unity | Collaborative framing ("we find gaps") + expert stance without rigidity |
| **Utility / micro** (`/fix-markdown`, `/pr`) | Clarity only | Minimal persuasion needed — task is mechanical |

## The 7 Principles (Quick Reference)

| Principle | Mechanism | Skill Application | Risk |
|---|---|---|---|
| **Authority** | Expert/rule-based compliance | FORBIDDEN, MUST, severity tables, role assignment ("You are the Auditor") | Overuse → noise, ignored |
| **Commitment** | Consistency with stated goals | Checklists (`- [ ]`), Quality Gates, "Before SKILL COMPLETE verify..." | None if honest |
| **Social Proof** | Conforming to norms | "All skills in this project follow...", anti-patterns ("others made this mistake") | Fabricated norms → distrust |
| **Scarcity** | Urgency / limited resources | Token Economy ("budget is finite"), Loop Guard ("max 3 attempts") | False urgency → rushing |
| **Unity** | Shared identity / team | "We are the QA team", collaborative framing | Overuse → sycophancy |
| **Reciprocity** | Exchange / obligation | Providing context → expecting quality output | Manipulation risk — use sparingly |
| **Liking** | Positive relationship | Friendly tone, acknowledgment | **AVOID** — triggers sycophancy in LLMs |

## Decision Checklist for Skill Authors

When writing a new skill body (Step 5.1), answer:

1. **What type is this skill?** → Look up primary principles in mapping table above
2. **What behavior needs enforcement?** → Match to principle mechanism
3. **Am I using more than 3 principles?** → Reduce. Overloading dilutes each one
4. **Am I using Liking or excessive Reciprocity?** → Remove. These trigger sycophancy
5. **Does each MUST/FORBIDDEN have a "Why" line?** → Rationale-driven phrasing > raw imperatives

## Why Bright-Line Rules Work

Bright-line rules ("NEVER report findings below Confidence 80") outperform nuanced guidelines ("use your judgment on low-confidence findings") because:

- **No rationalization surface** — the agent cannot argue "this case is special"
- **Implementation intention** — specific trigger ("Confidence < 80") → automatic action ("discard")
- **Reduces decision fatigue** — fewer judgment calls = more consistent output

Use bright-line rules for: quality thresholds, format requirements, prohibited patterns.
Use flexible guidelines for: analytical depth, contextual decisions, edge case handling.

## Ethical Test

Before finalizing persuasion in a skill, verify:
- The rules serve output quality, not just compliance theater
- The agent can still escalate genuine edge cases (ESCALATION block)
- No principle creates a perverse incentive (e.g., Scarcity causing rushed analysis)
