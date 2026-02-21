package registration.tests

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.ktor.client.call.body
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.ErrorResponse
import registration.requests.RegisterApiClient

@Epic("User Registration")
@Feature("POST /api/v1/users/register — Service Failure")
@Tag("CRITICAL")
class RegistrationServiceFailureTests {

    private lateinit var wireMockServer: WireMockServer

    @BeforeEach
    fun setUp() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()
        wireMockServer.stubFor(
            post(urlPathEqualTo("/sms/send"))
                .willReturn(
                    aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "Service Unavailable"}""")
                )
        )
        System.setProperty("SMS_GATEWAY_URL", "http://localhost:${wireMockServer.port()}")
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
        System.clearProperty("SMS_GATEWAY_URL")
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REG-NEG-SERVICE-01: SMS Gateway Unavailable (503)")
    @Link(name = "Scenario REG-NEG-SERVICE-01", url = "file://audit/test-scenarios.md")
    fun smsGatewayUnavailable503() = runTest {
        val request = TestData.validRequest()

        val response = RegistrationHelper.registerUser(request)

        assertEquals(503, response.status.value, "Expected 503 Service Unavailable")
        val errorBody = response.body<ErrorResponse>()
        assertEquals("SERVICE_UNAVAILABLE", errorBody.code, "error code mismatch")
    }
}
