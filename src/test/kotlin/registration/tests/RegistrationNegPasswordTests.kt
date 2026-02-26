package registration.tests

import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import org.junit.jupiter.api.Tag
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import registration.helpers.MockServerExtension
import registration.helpers.TestData
import registration.requests.ErrorResponse
import registration.requests.RegisterApiClient
import registration.requests.RegisterRequest

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("CRITICAL")
@Severity(SeverityLevel.CRITICAL)
class RegistrationNegPasswordTests {

    @Test
    @Link(name = "Scenario REG-24", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-24: password too short — 7 chars (below min:8) → 400 VALIDATION_ERROR")
    fun `REG-24 password too short 7 chars`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.pass7CharsValidComplexity(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-25", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-25: password too long — 65 chars (above max:64) → 400 VALIDATION_ERROR")
    fun `REG-25 password too long 65 chars`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.pass65CharsValidComplexity(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-26", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-26: password missing uppercase letter → 400 VALIDATION_ERROR")
    fun `REG-26 password missing uppercase`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.passAllLowerDigitSpecial(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-27", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-27: password missing digit → 400 VALIDATION_ERROR")
    fun `REG-27 password missing digit`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.passUpperLowerSpecialNoDigit(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-28", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-28: password missing special character → 400 VALIDATION_ERROR")
    fun `REG-28 password missing special char`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.passUpperLowerDigitNoSpecial(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-29", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-29: password contains token from full_name — basic PII sad path → 400 VALIDATION_ERROR")
    fun `REG-29 password contains token from full name`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = "Smith_Safe2026!",
            fullName = "Smith Jones",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-30", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-30: password contains token from email local-part — basic PII sad path → 400 VALIDATION_ERROR")
    fun `REG-30 password contains token from email local part`(): Unit = runTest {
        val request = RegisterRequest(
            email = "johndoe@example.com",
            phone = TestData.uniquePhone(),
            password = "Johndoe_123!",
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-31", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-31: password PII check is case-insensitive — uppercase token variant → 400 VALIDATION_ERROR")
    fun `REG-31 password pii check case insensitive`(): Unit = runTest {
        val request = RegisterRequest(
            email = "alex@example.com",
            phone = TestData.uniquePhone(),
            password = "ALEX_Safe2026!",
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }
}
