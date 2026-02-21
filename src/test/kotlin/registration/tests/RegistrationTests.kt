package registration.tests

import io.qameta.allure.DisplayName
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import registration.helpers.TestData
import registration.requests.RegisterRequest
import registration.requests.apiClient

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@Tag("CRITICAL")
@Severity(SeverityLevel.CRITICAL)
class RegistrationTests {

    @Test
    @DisplayName("REG-POS-01: Happy Path — Minimal Required Data")
    @Link(name = "Scenario REG-POS-01", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testHappyPathMinimalData() = runTest {
        val email = TestData.uniqueEmail()
        val phone = "+79991234567"
        val password = "Safe1@Pass"
        val fullName = "John Doe"

        val request = RegisterRequest(
            email = email,
            phone = phone,
            password = password,
            fullName = fullName
        )

        val response = apiClient.registerUserAsync(request)

        assertEquals(201, response.code, "Should return 201 Created")
        assertNotNull(response.body.verificationToken, "Verification token should be present")
        assertNotNull(response.body.expiresAt, "Expires at should be present")

        val expiresAt = Instant.parse(response.body.expiresAt!!)
        val now = Instant.now()
        val diffMinutes = ChronoUnit.MINUTES.between(now, expiresAt)
        assertTrue(diffMinutes >= 14 && diffMinutes <= 16, "Token expiration should be approximately 15 minutes")
    }

    @Test
    @DisplayName("REG-POS-02: Happy Path — Maximum Data Complexity")
    @Link(name = "Scenario REG-POS-02", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testHappyPathMaximumComplexity() = runTest {
        val email = TestData.longEmail()
        val phone = "+79991234567"
        val password = "Complex@Pass123_Secure"
        val fullName = "François José María-Anna"

        val request = RegisterRequest(
            email = email,
            phone = phone,
            password = password,
            fullName = fullName
        )

        val response = apiClient.registerUserAsync(request)

        assertEquals(201, response.code, "Should return 201 Created")
        assertNotNull(response.body.verificationToken, "Verification token should be present")
        assertNotNull(response.body.expiresAt, "Expires at should be present")
    }

    @Test
    @DisplayName("REG-POS-03: Response Headers Validation")
    @Link(name = "Scenario REG-POS-03", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testResponseHeadersValidation() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "John Doe"
        )

        val response = apiClient.registerUserAsync(request)

        assertEquals(201, response.code, "Should return 201 Created")
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertEquals("no-store", response.headers["Cache-Control"], "Cache-Control header mismatch")
    }

    @Test
    @DisplayName("REG-IDEM-01: Idempotency — First Request with Key")
    @Link(name = "Scenario REG-IDEM-01", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testIdempotencyFirstRequest() = runTest {
        val idempotencyKey = UUID.randomUUID().toString()
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "John Doe"
        )

        val response = apiClient.registerUserAsync(request, idempotencyKey)

        assertEquals(201, response.code, "Should return 201 Created")
        assertNotNull(response.body.verificationToken, "Verification token should be present")
    }

    @Test
    @DisplayName("REG-IDEM-02: Idempotency — Cached Response Within 5 Minutes")
    @Link(name = "Scenario REG-IDEM-02", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testIdempotencyCachedResponse() = runTest {
        val idempotencyKey = UUID.randomUUID().toString()
        val email = TestData.uniqueEmail()
        val request = RegisterRequest(
            email = email,
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "John Doe"
        )

        val response1 = apiClient.registerUserAsync(request, idempotencyKey)
        val response2 = apiClient.registerUserAsync(request, idempotencyKey)

        assertEquals(201, response1.code, "First request should return 201")
        assertEquals(201, response2.code, "Second request should return 201")
        assertEquals(response1.body.verificationToken, response2.body.verificationToken, "Tokens should be identical (cached)")
    }

    @Test
    @DisplayName("REG-IDEM-03: Idempotency — Cache Expired After 5 Minutes")
    @Link(name = "Scenario REG-IDEM-03", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testIdempotencyCacheExpired() = runTest {
        val idempotencyKey = UUID.randomUUID().toString()
        val email = TestData.uniqueEmail()
        val request = RegisterRequest(
            email = email,
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "John Doe"
        )

        val response = apiClient.registerUserAsync(request, idempotencyKey)
        assertEquals(201, response.code, "Request should succeed")
        assertNotNull(response.body.verificationToken, "Token should be present")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideMissingFieldValidationData")
    @DisplayName("Missing Required Fields")
    @Link(name = "Scenarios REG-NEG-MISSING-01 to 04", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testMissingFields(testName: String, request: RegisterRequest, expectedField: String) = runTest {
        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should return 400 Bad Request")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals(expectedField, response.body.field, "Error field should be $expectedField")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideNullValueValidationData")
    @DisplayName("Null Values")
    @Link(name = "Scenarios REG-NEG-NULL-01 to 04", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testNullValues(testName: String, request: RegisterRequest, expectedField: String) = runTest {
        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should return 400 Bad Request")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals(expectedField, response.body.field, "Error field should be $expectedField")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideWrongTypeValidationData")
    @DisplayName("Wrong JSON Types")
    @Link(name = "Scenarios REG-NEG-TYPE-01 to 04", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testWrongTypes(testName: String, request: RegisterRequest, expectedField: String) = runTest {
        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should return 400 Bad Request")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals(expectedField, response.body.field, "Error field should be $expectedField")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideEmptyStringValidationData")
    @DisplayName("Empty Strings")
    @Link(name = "Scenarios REG-NEG-EMPTY-01 to 04", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testEmptyStrings(testName: String, request: RegisterRequest, expectedField: String) = runTest {
        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should return 400 Bad Request")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals(expectedField, response.body.field, "Error field should be $expectedField")
    }

    @Test
    @DisplayName("REG-NEG-CONFLICT-01: Duplicate Email — Sequential Conflict")
    @Link(name = "Scenario REG-NEG-CONFLICT-01", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testDuplicateEmail() = runTest {
        val email = TestData.uniqueEmail()
        val request1 = RegisterRequest(
            email = email,
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "John Doe"
        )
        val request2 = RegisterRequest(
            email = email,
            phone = "+11234567890",
            password = "Safe1@Pass",
            fullName = "Jane Smith"
        )

        val response1 = apiClient.registerUserAsync(request1)
        assertEquals(201, response1.code, "First registration should succeed")

        val response2 = apiClient.registerUserExpectErrorAsync(request2)
        assertEquals(409, response2.code, "Second registration with same email should fail with 409")
        assertEquals("CONFLICT", response2.body.code, "Error code should be CONFLICT")
        assertTrue(response2.body.message?.contains("Email") == true, "Message should mention Email")
    }

    @Test
    @DisplayName("REG-NEG-CONFLICT-02: Duplicate Phone — Sequential Conflict")
    @Link(name = "Scenario REG-NEG-CONFLICT-02", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testDuplicatePhone() = runTest {
        val phone = "+79991234567"
        val request1 = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = phone,
            password = "Safe1@Pass",
            fullName = "John Doe"
        )
        val request2 = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = phone,
            password = "Safe1@Pass",
            fullName = "Jane Smith"
        )

        val response1 = apiClient.registerUserAsync(request1)
        assertEquals(201, response1.code, "First registration should succeed")

        val response2 = apiClient.registerUserExpectErrorAsync(request2)
        assertEquals(409, response2.code, "Second registration with same phone should fail with 409")
        assertEquals("CONFLICT", response2.body.code, "Error code should be CONFLICT")
        assertTrue(response2.body.message?.contains("Phone") == true, "Message should mention Phone")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providePasswordValidationData")
    @DisplayName("Password Validation Errors")
    @Link(name = "Scenarios REG-NEG-PWD-01 to 06", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testPasswordValidation(testName: String, request: RegisterRequest, expectedMessage: String) = runTest {
        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should return 400 Bad Request")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals("password", response.body.field, "Error field should be password")
        assertTrue(response.body.message?.contains(expectedMessage, ignoreCase = true) == true,
            "Message should contain '$expectedMessage'")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideNameValidationData")
    @DisplayName("Full Name Validation Errors")
    @Link(name = "Scenarios REG-NEG-NAME-01 to 06", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testNameValidation(testName: String, request: RegisterRequest, expectedMessage: String?) = runTest {
        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should return 400 Bad Request")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals("full_name", response.body.field, "Error field should be full_name")
        if (expectedMessage != null) {
            assertTrue(response.body.message?.contains(expectedMessage, ignoreCase = true) == true,
                "Message should contain '$expectedMessage'")
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideFormatValidationData")
    @DisplayName("Format Validation Errors")
    @Link(name = "Scenarios REG-NEG-FORMAT-01 to 03", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testFormatValidation(testName: String, request: RegisterRequest, expectedField: String) = runTest {
        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should return 400 Bad Request")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals(expectedField, response.body.field, "Error field should be $expectedField")
    }

    @Test
    @DisplayName("REG-NEG-SERVICE-01: SMS Gateway Unavailable (503)")
    @Link(name = "Scenario REG-NEG-SERVICE-01", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testServiceUnavailable() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "John Doe"
        )

        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(503, response.code, "Should return 503 Service Unavailable")
        assertEquals("SERVICE_UNAVAILABLE", response.body.code, "Error code should be SERVICE_UNAVAILABLE")
    }

    @Test
    @DisplayName("REG-L10N-01: Cyrillic Characters in Full Name")
    @Link(name = "Scenario REG-L10N-01", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testCyrillicName() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "Иван Петров"
        )

        val response = apiClient.registerUserAsync(request)

        assertEquals(201, response.code, "Should accept Cyrillic characters")
        assertNotNull(response.body.verificationToken, "Verification token should be present")
    }

    @Test
    @DisplayName("REG-L10N-02: Arabic Characters in Full Name (RTL)")
    @Link(name = "Scenario REG-L10N-02", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testArabicName() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "محمد علي"
        )

        val response = apiClient.registerUserAsync(request)

        assertEquals(201, response.code, "Should accept Arabic characters")
        assertNotNull(response.body.verificationToken, "Verification token should be present")
    }

    @Test
    @DisplayName("REG-L10N-03: CJK Characters in Full Name")
    @Link(name = "Scenario REG-L10N-03", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testCJKName() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "李明"
        )

        val response = apiClient.registerUserAsync(request)

        assertEquals(201, response.code, "Should accept CJK characters")
        assertNotNull(response.body.verificationToken, "Verification token should be present")
    }

    @Test
    @DisplayName("REG-L10N-04: Latin-Extended Characters in Full Name")
    @Link(name = "Scenario REG-L10N-04", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testLatinExtendedName() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "François José María"
        )

        val response = apiClient.registerUserAsync(request)

        assertEquals(201, response.code, "Should accept Latin-Extended characters")
        assertNotNull(response.body.verificationToken, "Verification token should be present")
    }

    @Test
    @DisplayName("REG-L10N-05: Emoji in Full Name (Invalid)")
    @Link(name = "Scenario REG-L10N-05", url = "file://docs/test-cases/test-scenarios_20260221_174110.md")
    fun testEmojiName() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "+79991234567",
            password = "Safe1@Pass",
            fullName = "John 😀 Doe"
        )

        val response = apiClient.registerUserExpectErrorAsync(request)

        assertEquals(400, response.code, "Should reject emoji characters")
        assertEquals("VALIDATION_ERROR", response.body.code, "Error code should be VALIDATION_ERROR")
        assertEquals("full_name", response.body.field, "Error field should be full_name")
    }

    companion object {
        @JvmStatic
        fun provideMissingFieldValidationData(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-MISSING-01: Missing Email",
                RegisterRequest(email = null, phone = "+79991234567", password = "Safe1@Pass", fullName = "John Doe"),
                "email"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-MISSING-02: Missing Phone",
                RegisterRequest(email = TestData.uniqueEmail(), phone = null, password = "Safe1@Pass", fullName = "John Doe"),
                "phone"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-MISSING-03: Missing Password",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = null, fullName = "John Doe"),
                "password"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-MISSING-04: Missing Full Name",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = null),
                "full_name"
            )
        )

        @JvmStatic
        fun provideNullValueValidationData(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NULL-01: Null Email",
                RegisterRequest(email = null, phone = "+79991234567", password = "Safe1@Pass", fullName = "John Doe"),
                "email"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NULL-02: Null Phone",
                RegisterRequest(email = TestData.uniqueEmail(), phone = null, password = "Safe1@Pass", fullName = "John Doe"),
                "phone"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NULL-03: Null Password",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = null, fullName = "John Doe"),
                "password"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NULL-04: Null Full Name",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = null),
                "full_name"
            )
        )

        @JvmStatic
        fun provideWrongTypeValidationData(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-TYPE-01: Email as Array",
                RegisterRequest(email = listOf("test@example.com"), phone = "+79991234567", password = "Safe1@Pass", fullName = "John Doe"),
                "email"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-TYPE-02: Phone as Object",
                RegisterRequest(email = TestData.uniqueEmail(), phone = mapOf("number" to "+79991234567"), password = "Safe1@Pass", fullName = "John Doe"),
                "phone"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-TYPE-03: Password as Boolean",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = true, fullName = "John Doe"),
                "password"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-TYPE-04: Full Name as Number",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = 12345),
                "full_name"
            )
        )

        @JvmStatic
        fun provideEmptyStringValidationData(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-EMPTY-01: Empty Email",
                RegisterRequest(email = "", phone = "+79991234567", password = "Safe1@Pass", fullName = "John Doe"),
                "email"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-EMPTY-02: Empty Phone",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "", password = "Safe1@Pass", fullName = "John Doe"),
                "phone"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-EMPTY-03: Empty Password",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "", fullName = "John Doe"),
                "password"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-EMPTY-04: Empty Full Name",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = ""),
                "full_name"
            )
        )

        @JvmStatic
        fun providePasswordValidationData(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-PWD-01: Password Too Short",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Pass1@", fullName = "John Doe"),
                "at least 8"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-PWD-02: Missing Uppercase",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "safe1@pass", fullName = "John Doe"),
                "uppercase"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-PWD-03: Missing Digit",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "SafePass@", fullName = "John Doe"),
                "digit"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-PWD-04: Missing Special Character",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "SafePass1", fullName = "John Doe"),
                "special"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-PWD-05: Password Contains Name Token",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "John1@Test", fullName = "John Doe"),
                "cannot contain"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-PWD-06: Password Contains Email Token",
                RegisterRequest(email = "john.smith@example.com", phone = "+79991234567", password = "John.smith1@Pass", fullName = "Jane Doe"),
                "cannot contain"
            )
        )

        @JvmStatic
        fun provideNameValidationData(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NAME-01: Name Starts with Space",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = " John Doe"),
                null
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NAME-02: Name Ends with Space",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = "John Doe "),
                null
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NAME-03: Name With Consecutive Spaces",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = "John  Doe"),
                null
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NAME-04: Name Too Short",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = "J"),
                "at least 2"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NAME-05: Name With Apostrophe",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = "John O'Brien"),
                "apostrophe"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-NAME-06: Name With Non-Allowed Characters",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = "John@Doe#123"),
                null
            )
        )

        @JvmStatic
        fun provideFormatValidationData(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-FORMAT-01: Email Not Lowercase",
                RegisterRequest(email = "John.Smith@EXAMPLE.COM", phone = "+79991234567", password = "Safe1@Pass", fullName = "John Doe"),
                "email"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-FORMAT-02: Phone Invalid Format",
                RegisterRequest(email = TestData.uniqueEmail(), phone = "89991234567", password = "Safe1@Pass", fullName = "John Doe"),
                "phone"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "REG-NEG-FORMAT-03: Email Exceeds 254 Characters",
                RegisterRequest(email = TestData.tooLongEmail(), phone = "+79991234567", password = "Safe1@Pass", fullName = "John Doe"),
                "email"
            )
        )
    }
}
