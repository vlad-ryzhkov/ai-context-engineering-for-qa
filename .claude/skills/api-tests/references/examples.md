# API Tests: Code Examples

## Models
```kotlin
@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegisterRequest(
    val email: Any?,
    val password: Any?,
    val phone: Any?
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegisterResponse(
    val id: String? = null,
    val email: String? = null,
    val status: String? = null
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorResponse(
    val code: String? = null,
    val message: String? = null
)
```

## Config
```kotlin
object Endpoints { const val REGISTRATION = "/api/v2/registration" }
object Msgs {
    const val STATUS_MISMATCH = "HTTP status mismatch"
    const val ERROR_CODE_MISMATCH = "Error code mismatch"
}
```

## API Client (DRY — single method returning HttpResponse)
```kotlin
object RegistrationClient {
    private val client by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(Logging) { level = LogLevel.ALL }
        }
    }

    suspend fun register(body: RegisterRequest, headers: Map<String, String> = emptyMap()): HttpResponse =
        client.post(Config.baseUrl + Endpoints.REGISTRATION) {
            contentType(ContentType.Application.Json)
            setBody(body)
            headers.forEach { (k, v) -> header(k, v) }
        }
}
```

## Helpers (@Step methods, body extraction via response.body<T>())
```kotlin
object RegistrationHelper {
    @Step("Register new user")
    suspend fun registerUser(request: RegisterRequest = RegistrationTestData.validRequest()): RegisterResponse {
        val response = RegistrationClient.register(request)
        assertEquals(201, response.status.value, Msgs.STATUS_MISMATCH)
        return response.body<RegisterResponse>()
    }

    @Step("Delete user {userId}")
    suspend fun deleteUser(userId: String) {
        val response = RegistrationClient.delete(userId)
        assertEquals(204, response.status.value, "Cleanup failed for user $userId")
    }
}
```

## TestData (UUID-based, no mutable counters)
```kotlin
object RegistrationTestData {
    private val faker = Faker()

    fun validRequest() = RegisterRequest(
        email = "${UUID.randomUUID()}@test.com",
        password = faker.internet().password(8, 20, true, true),
        phone = uniquePhone()
    )

    fun uniquePhone(): String = "+7${ThreadLocalRandom.current().nextLong(9_000_000_000L, 9_999_999_999L)}"

    fun invalidEmailRequest() = validRequest().copy(email = "not-an-email")
}
```

## Tests — POS with try/finally cleanup + HSTS check
```kotlin
@Epic("Registration")
@Feature("POST /api/v2/registration")
class RegistrationPositiveTests {

    private var createdUserId: String? = null

    @AfterEach
    fun cleanup(): Unit = runTest {
        createdUserId?.let { RegistrationHelper.deleteUser(it) }
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    @DisplayName("201: Successful registration with valid data")
    @Link(name = "Scenario POS-01", url = "file://audit/test-scenarios.md")
    fun successfulRegistration(): Unit = runTest {
        val request = RegistrationTestData.validRequest()

        val response = RegistrationClient.register(request)

        assertEquals(201, response.status.value, Msgs.STATUS_MISMATCH)
        assertEquals("application/json", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header missing")
        assertEquals(
            "max-age=31536000; includeSubDomains",
            response.headers["Strict-Transport-Security"],
            "Strict-Transport-Security header missing"
        )

        val body = response.body<RegisterResponse>()
        assertNotNull(body.id, "Response ID must not be null")
        assertEquals(request.email, body.email, "Email mismatch")
        createdUserId = body.id
    }
}
```

## Tests — NEG with @ParameterizedTest + @MethodSource
```kotlin
@Epic("Registration")
@Feature("POST /api/v2/registration")
class RegistrationNegativeTests {

    @ParameterizedTest(name = "400: {2}")
    @MethodSource("provideValidationData")
    @Severity(SeverityLevel.NORMAL)
    @Tag("REGRESSION")
    @Link(name = "Scenario NEG-{index}", url = "file://audit/test-scenarios.md")
    fun validationErrors(request: RegisterRequest, expectedCode: String, scenario: String): Unit = runTest {
        val response = RegistrationClient.register(request)

        assertEquals(400, response.status.value, Msgs.STATUS_MISMATCH)
        val error = response.body<ErrorResponse>()
        assertEquals(expectedCode, error.code, Msgs.ERROR_CODE_MISMATCH)
    }

    companion object {
        @JvmStatic
        fun provideValidationData(): Stream<Arguments> = Stream.of(
            Arguments.of(RegistrationTestData.invalidEmailRequest(), "INVALID_EMAIL", "invalid email format"),
            Arguments.of(RegistrationTestData.validRequest().copy(password = "short"), "WEAK_PASSWORD", "password too short"),
            Arguments.of(RegistrationTestData.validRequest().copy(phone = null), "MISSING_PHONE", "phone is null")
        )
    }
}
```
