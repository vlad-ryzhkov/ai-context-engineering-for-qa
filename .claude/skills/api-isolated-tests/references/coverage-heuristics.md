# Coverage Heuristics (DEFAULT_HEURISTICS)

> Reference file for /api-isolated-tests. Loaded when mode: api-integration.

3a. **API Integration Test Focus (DEFAULT — `mode: api-integration`):**
   **Prevention-first:** Do NOT generate unit-level scenarios — apply heuristics as generation constraints, not post-generation filters. Skipped when `mode: full-matrix`.
   **Testing Level Classification:**
   | API-level (ALWAYS generate) | Unit-level (1 representative) |
   |---|---|
   | Contract: status codes, response schema, headers | Same validation × N fields, same error code |
   | Business logic: state transitions, side effects, named rules | M sub-rules of one regex/validator |
   | Cross-field / state-dependent validation | BVA for infrastructure fields (no business rule) |
   | Auth/authz (SEC), Idempotency (IDEM), L10N round-trip | Whitespace/spacing/format variants of same check |
   | Error code routing (distinct codes = distinct scenarios) | NEG duplicating a BVA boundary |
   Principle: 1 representative proves the validator is wired; proving it for field B after A — unit-test scope.
   **Precedence:** Spec markers (`EXCLUDED_SCENARIOS`) > DEFAULT_HEURISTICS > `mode: full-matrix` override.

   **DEFAULT_HEURISTICS:**
   | ID | Heuristic | Action |
   |---|---|---|
   | DH-01 | Repeating Validation Pattern — null/empty/wrong-type across N fields, same error code | Keep 1 representative field per check type (e.g., 1 for missing, 1 for null, 1 for wrong-type — not per-field) |
   | DH-02 | Complexity Rule Combinatorics — M sub-rules for one field | Keep 1 representative NEG |
   | DH-03 | Infrastructure BVA — schema-only length, no named business rule | Keep 1 BVA pair for 1 representative field |
   | DH-04 | Format/Whitespace Consolidation — 3+ regex/spacing variants for same field | Keep 1 representative |
   | DH-05 | Length Boundary Deduplication — NEG row = same boundary as BVA row | Keep BVA row, remove NEG duplicate |

   **Exceptions (heuristic does NOT reduce):**
   - Scenario tests an explicitly named business rule from spec (e.g., "Apostrophes are not allowed")
   - Scenario produces a different error code / HTTP status from others in its group
   - Scenario tests state-dependent validation
   - Endpoint is `[CRITICAL]` + scenario tests security/data-integrity
   **API Coverage Guarantee (heuristics NEVER reduce):** All POS, SEC, IDEM, L10N, HEADERS; scenarios with distinct error code/HTTP status; explicitly named business rules; state-dependent and cross-field validation; `[CRITICAL]` security/data-integrity; ≥1 NEG per validation class; ≥1 BVA pair for field with business rule.
