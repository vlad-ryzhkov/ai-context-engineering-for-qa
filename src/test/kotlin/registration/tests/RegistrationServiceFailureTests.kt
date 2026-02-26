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

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("CRITICAL")
@Severity(SeverityLevel.CRITICAL)
class RegistrationServiceFailureTests {

    @BeforeEach
    fun setUp() {
        MockRegistrationServer.stubSmsGatewayUnavailable()
    }

    @AfterEach
    fun tearDown() {
        MockRegistrationServer.resetAll()
    }

    @Test
    @Link(name = "Scenario REG-41", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-41: SMS gateway unavailable — 503 + transaction rollback, no DB record persisted")
    fun `REG-41 sms gateway unavailable transaction rollback`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName())

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status, "Expected 503 Service Unavailable")
        val body = response.body<ErrorResponse>()
        assertEquals("SERVICE_UNAVAILABLE", body.code, "error code mismatch")
        RegistrationHelper.assertNoDbRecord(email)
    }

    @Test
    @Link(name = "Scenario REG-CRIT-02", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-CRIT-02: Partial failure — SMS gateway fails mid-request: DB write rolled back")
    @Tag("CRITICAL")
    @Severity(SeverityLevel.CRITICAL)
    fun `REG-CRIT-02 sms gateway fails db rolled back`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName())

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status, "Expected 503 Service Unavailable")
        val body = response.body<ErrorResponse>()
        assertEquals("SERVICE_UNAVAILABLE", body.code, "error code mismatch")
        RegistrationHelper.assertNoDbRecord(email)
    }

    @Test
    @Link(name = "Scenario REG-CRIT-03", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-CRIT-03: SmsGateway failure mock — [CRITICAL] upstream 503, no record created")
    @Tag("CRITICAL")
    @Severity(SeverityLevel.CRITICAL)
    fun `REG-CRIT-03 sms gateway failure mock no record created`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName())

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status, "Expected 503 Service Unavailable")
        val body = response.body<ErrorResponse>()
        assertEquals("SERVICE_UNAVAILABLE", body.code, "error code mismatch")
        RegistrationHelper.assertNoDbRecord(email)
    }
}
