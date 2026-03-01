# Review Examples

## Example 1: Security Issue Detected
```
🔴 CRITICAL BLOCKER: Hardcoded JWT token found

File: src/test/kotlin/users/tests/UserTests.kt:45
Pattern: val token = "Bearer eyJhbGciOi..."

Recommendation:
Replace with: val token = System.getenv("API_TOKEN") ?: error("Missing API_TOKEN")
Reference: CLAUDE.md → Tech Stack → Secrets Never Commit
```

## Example 2: DTO Isolation Issue
```
🟠 MAJOR: DTO declared inline in test class

File: src/test/kotlin/users/tests/UserTests.kt:12
Pattern:
    class UserTests {
        data class UserRequest(val name: String, val email: String)
    }

Recommendation:
Move to: src/test/kotlin/users/models/requests/UserRequest.kt
Add annotation: @JsonNaming(SnakeCaseStrategy::class)
Benefits: Reusable across tests, contract-driven validation
```

## Example 3: Assertion Without Message
```
🟠 MAJOR: Assertion lacks semantic message

File: src/test/kotlin/users/tests/UserTests.kt:123
Pattern: response.status.shouldBe(201)

Recommendation (Optional):
Add message: response.status.shouldBe(201, "User creation response should return 201 Created")
Or: assertThat(response.status).as("User creation status").isEqualTo(201)
```
