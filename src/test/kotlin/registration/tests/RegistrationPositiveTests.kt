package registration.tests

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.ktor.client.call.body
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import registration.helpers.JwtHelper
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.RegisterRequest
import registration.requests.RegisterResponse

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
class RegistrationPositiveTests {

    private val createdUserTokens = mutableListOf<String>()

    @AfterEach
    fun cleanup() = runTest {
        createdUserTokens.forEach { token ->
            try {
                val uuid = RegistrationHelper.extractUserUuid(token)
                if (uuid.isNotBlank()) {
                    RegistrationHelper.deleteUser(uuid)
                }
            } catch (_: Exception) {
            }
        }
        createdUserTokens.clear()
    }

    @Test
    @Tag("CRITICAL")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REG-POS-01: Happy Path - Minimal Required Data")
    @Link(name = "Scenario REG-POS-01", url = "file://audit/test-scenarios.md")
    fun happyPathMinimalRequiredData() = runTest {
        val request = TestData.validRequest()

        val response = RegistrationHelper.registerUser(request)

        assertEquals(201, response.status.value, "Expected 201 Created")
        RegistrationHelper.verifySecurityHeaders(response)
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        createdUserTokens.add(body.verificationToken)

        val jwtPayload = JwtHelper.verifyTokenClaims(
            token = body.verificationToken,
            expectedEmail = request.email
        )
        assertTrue(jwtPayload.exp > 0, "JWT exp must be a positive Unix timestamp")
        JwtHelper.verifyExpTimeWindow(body.verificationToken)
        RegistrationHelper.verifyExpiresAtTimeWindow(body.expiresAt)
        JwtHelper.verifySensitiveFieldsAbsent(body.verificationToken)
    }

    @Test
    @Tag("CRITICAL")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REG-POS-02: Happy Path - Maximum Data Complexity")
    @Link(name = "Scenario REG-POS-02", url = "file://audit/test-scenarios.md")
    fun happyPathMaximumDataComplexity() = runTest {
        val request = RegisterRequest(
            email = TestData.longEmail(250),
            phone = "+79991234567",
            password = "Complex@Pass123_Secure",
            fullName = "Fran\u00e7ois Jos\u00e9 Mar\u00eda-Anna"
        )

        val response = RegistrationHelper.registerUser(request)

        assertEquals(201, response.status.value, "Expected 201 Created")
        RegistrationHelper.verifySecurityHeaders(response)
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        createdUserTokens.add(body.verificationToken)

        JwtHelper.verifyTokenClaims(
            token = body.verificationToken,
            expectedEmail = request.email
        )
    }

    @Test
    @Tag("REGRESSION")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REG-POS-03: Response Headers Validation")
    @Link(name = "Scenario REG-POS-03", url = "file://audit/test-scenarios.md")
    fun responseHeadersValidation() = runTest {
        val request = TestData.validRequest()

        val response = RegistrationHelper.registerUser(request)

        assertEquals(201, response.status.value, "Expected 201 Created")
        assertEquals(
            "application/json; charset=utf-8",
            response.headers["Content-Type"],
            "Content-Type header mismatch"
        )
        assertEquals(
            "nosniff",
            response.headers["X-Content-Type-Options"],
            "X-Content-Type-Options header mismatch"
        )
        assertEquals(
            "no-store",
            response.headers["Cache-Control"],
            "Cache-Control header mismatch"
        )
        RegistrationHelper.verifySecurityHeaders(response)

        val body = response.body<RegisterResponse>()
        createdUserTokens.add(body.verificationToken)
    }
}
