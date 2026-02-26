package registration.tests

import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import org.junit.jupiter.api.Tag
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import registration.helpers.MockRegistrationServer
import registration.helpers.MockServerExtension
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.RegisterApiClient
import registration.requests.RegisterRequest

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("CRITICAL")
@Severity(SeverityLevel.CRITICAL)
class RegistrationCriticalTests {

    private lateinit var concurrentEmail: String

    @BeforeEach
    fun setUp() {
        MockRegistrationServer.stubSmsGatewaySuccess()
    }

    @AfterEach
    fun tearDown() {
        if (::concurrentEmail.isInitialized) {
            RegistrationHelper.assertNoDuplicateDbRecord(concurrentEmail)
        }
        MockRegistrationServer.resetAll()
    }

    @Test
    @Link(name = "Scenario REG-CRIT-01", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-CRIT-01: Concurrent double registration — same email in parallel — exactly one 201, one 409")
    fun `REG-CRIT-01 concurrent double registration same email`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        concurrentEmail = email

        val requestA = RegisterRequest(
            email = email,
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )
        val requestB = RegisterRequest(
            email = email,
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val results = awaitAll(
            async { RegisterApiClient.register(requestA) },
            async { RegisterApiClient.register(requestB) },
        )

        val statuses = results.map { it.status }
        val successCount = statuses.count { it == HttpStatusCode.Created }
        val conflictCount = statuses.count { it == HttpStatusCode.Conflict }

        assertEquals(1, successCount, "Exactly one request must succeed with 201 Created")
        assertEquals(1, conflictCount, "Exactly one request must fail with 409 Conflict")

        RegistrationHelper.assertNoDuplicateDbRecord(email)
    }
}
