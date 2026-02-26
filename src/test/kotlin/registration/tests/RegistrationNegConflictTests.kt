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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import registration.helpers.MockRegistrationServer
import registration.helpers.MockServerExtension
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.ErrorResponse
import registration.requests.RegisterApiClient
import registration.requests.RegisterRequest

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("CRITICAL")
@Severity(SeverityLevel.CRITICAL)
class RegistrationNegConflictTests {

    private lateinit var existingEmail: String
    private lateinit var existingPhone: String

    @BeforeEach
    fun setUp() {
        MockRegistrationServer.stubSmsGatewaySuccess()
        existingEmail = TestData.uniqueEmail()
        existingPhone = TestData.uniquePhone()
    }

    @AfterEach
    fun tearDown() {
        MockRegistrationServer.resetAll()
    }

    @Test
    @Link(name = "Scenario REG-38", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-38: Email conflict — duplicate email (case-insensitive: uppercase variant) → 409 CONFLICT")
    fun `REG-38 email conflict case insensitive duplicate`(): Unit = runTest {
        RegistrationHelper.registerUser(existingEmail, existingPhone, TestData.validPassword(), TestData.validName())

        val upperEmail = existingEmail.uppercase()
        val request = RegisterRequest(
            email = upperEmail,
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )
        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.Conflict, response.status, "Expected 409 Conflict")
        val body = response.body<ErrorResponse>()
        assertEquals("CONFLICT", body.code, "error code mismatch")
        assertEquals("email", body.field, "error field mismatch")
        RegistrationHelper.assertNoDuplicateDbRecord(existingEmail)
    }

    @Test
    @Link(name = "Scenario REG-39", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-39: Email conflict — exact duplicate email already registered → 409 CONFLICT")
    fun `REG-39 email conflict exact duplicate`(): Unit = runTest {
        RegistrationHelper.registerUser(existingEmail, existingPhone, TestData.validPassword(), TestData.validName())

        val request = RegisterRequest(
            email = existingEmail,
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )
        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.Conflict, response.status, "Expected 409 Conflict")
        val body = response.body<ErrorResponse>()
        assertEquals("CONFLICT", body.code, "error code mismatch")
        assertEquals("email", body.field, "error field mismatch")
        RegistrationHelper.assertNoDuplicateDbRecord(existingEmail)
    }

    @Test
    @Link(name = "Scenario REG-40", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-40: Phone conflict — duplicate phone number (sequential) → 409 CONFLICT")
    fun `REG-40 phone conflict duplicate sequential`(): Unit = runTest {
        RegistrationHelper.registerUser(existingEmail, existingPhone, TestData.validPassword(), TestData.validName())

        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = existingPhone,
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )
        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.Conflict, response.status, "Expected 409 Conflict")
        val body = response.body<ErrorResponse>()
        assertEquals("CONFLICT", body.code, "error code mismatch")
        assertEquals("phone", body.field, "error field mismatch")
        RegistrationHelper.assertNoDuplicateDbRecord(existingEmail)
    }
}
