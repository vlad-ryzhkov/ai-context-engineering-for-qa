package registration.tests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
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
class RegistrationSecurityTests {

    @Test
    @Link(name = "Scenario REG-SEC-01", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-SEC-01: Plain HTTP request (TLS enforcement) — connection rejected or 400/301")
    fun `REG-SEC-01 plain http connection rejected`(): Unit = runTest {
        val httpBaseUrl = RegisterApiClient.BASE_URL.replace("https://", "http://")
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        try {
            val response = RegisterApiClient.httpClient.post("$httpBaseUrl/api/v1/users/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val status = response.status.value
            assertTrue(
                status == 400 || status == 301 || status == 302 || status == 426,
                "Plain HTTP must be rejected: expected 400/301/302/426, got $status",
            )
        } catch (e: Exception) {
            assertTrue(true, "Connection refused for plain HTTP is the expected secure behavior: ${e.message}")
        }
    }

    @Test
    @Link(name = "Scenario REG-SEC-02", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-SEC-02: SQL injection in email field → 400 VALIDATION_ERROR")
    fun `REG-SEC-02 sql injection in email field`(): Unit = runTest {
        val request = RegisterRequest(
            email = "' OR 1=1--@example.com",
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for SQL injection in email")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
    }

    @Test
    @Link(name = "Scenario REG-SEC-03", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-SEC-03: XSS payload in full_name → 400 VALIDATION_ERROR")
    fun `REG-SEC-03 xss payload in full name`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "<script>alert(1)</script>",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for XSS payload in full_name")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-SEC-04", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-SEC-04: Oversized request body (potential DoS) → 400 or 413")
    fun `REG-SEC-04 oversized request body potential dos`(): Unit = runTest {
        val oversizedBody = TestData.oversizedBody()

        val response = RegisterApiClient.registerRaw(oversizedBody)

        val status = response.status.value
        assertTrue(
            status == 400 || status == 413,
            "Expected 400 Bad Request or 413 Payload Too Large for oversized body, got $status",
        )
    }
}
