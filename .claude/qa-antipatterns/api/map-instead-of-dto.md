# Map Instead of DTO

## Why this is bad

Using `Map<String, Any>` instead of typed models:
- Compiler does not catch typos in field names
- No autocomplete in IDE
- API refactoring requires searching strings across the entire project
- Data structure is impossible to understand without documentation

## Bad Example

```kotlin
// ❌ BAD: Map — compiler won't help
@Test
fun `user can register`() {
    val payload = mapOf(
        "email" to "test@example.com",
        "phone" to "+79991234567",
        "pasword" to "Test123!",  // Typo! Compiler is silent
        "full_name" to "Test User"
    )

    val response = client.post("/register") {
        setBody(payload)
    }
}
```

## Good Example

```kotlin
// ✅ GOOD: Data class with annotations
@Serializable
data class RegisterRequest(
    val email: String,
    val phone: String,
    val password: String,  // Typo = compilation error
    @SerialName("full_name")
    val fullName: String
)

@Test
fun `user can register`() {
    val payload = RegisterRequest(
        email = "test@example.com",
        phone = "+79991234567",
        password = "Test123!",  // IDE provides hints
        fullName = "Test User"
    )

    val response = apiClient.register(payload)
}
```

## What to look for in code review

- `mapOf()`, `mutableMapOf()`, `hashMapOf()` for request/response
- `Map<String, Any>`, `Map<String, String>` in signatures
- JSON strings assembled via string interpolation
- Missing models in `models/` or `dto/` directory
