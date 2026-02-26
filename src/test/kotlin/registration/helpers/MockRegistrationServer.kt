package registration.helpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

object MockRegistrationServer {

    private val wireMockServer: WireMockServer = WireMockServer(wireMockConfig().dynamicPort())

    fun start() {
        if (!wireMockServer.isRunning) {
            wireMockServer.start()
        }
    }

    fun stop() {
        if (wireMockServer.isRunning) {
            wireMockServer.stop()
        }
    }

    fun port(): Int = wireMockServer.port()

    fun stubSmsGatewaySuccess() {
        wireMockServer.stubFor(
            post(urlEqualTo("/sms/send"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"status":"queued"}"""),
                ),
        )
    }

    fun stubSmsGatewayUnavailable() {
        wireMockServer.stubFor(
            post(urlEqualTo("/sms/send"))
                .willReturn(
                    aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error":"service unavailable"}"""),
                ),
        )
    }

    fun resetAll() {
        wireMockServer.resetAll()
    }
}
