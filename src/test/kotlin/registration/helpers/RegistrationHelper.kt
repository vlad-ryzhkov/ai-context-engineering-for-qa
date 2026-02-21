package registration.helpers

import io.qameta.allure.Step
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import registration.requests.RegisterApiClient
import registration.requests.RegisterRequest
import registration.requests.RegisterResponse
import registration.requests.ErrorResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

object RegistrationHelper {

    @Step("Register user with email={request.email}, phone={request.phone}")
    suspend fun registerUser(
        request: RegisterRequest,
        idempotencyKey: String? = null
    ): HttpResponse {
        return RegisterApiClient.registerUser(request, idempotencyKey)
    }

    @Step("Register user with raw JSON body")
    suspend fun registerUserRawJson(rawJson: String): HttpResponse {
        return RegisterApiClient.registerUserRawJson(rawJson)
    }

    @Step("Delete user with uuid={uuid}")
    suspend fun deleteUser(uuid: String): HttpResponse {
        return RegisterApiClient.deleteUser(uuid)
    }

    @Step("Verify security headers on response")
    fun verifySecurityHeaders(response: HttpResponse) {
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
            "max-age=31536000; includeSubDomains",
            response.headers["Strict-Transport-Security"],
            "Strict-Transport-Security header missing or incorrect"
        )
    }

    @Step("Verify successful registration response contract")
    suspend fun verifySuccessContract(response: HttpResponse): RegisterResponse {
        assertEquals(201, response.status.value, "Expected 201 Created")
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        return body
    }

    @Step("Verify expires_at is approximately now + {expectedOffsetSeconds}s")
    fun verifyExpiresAtTimeWindow(
        expiresAt: String,
        expectedOffsetSeconds: Long = 900,
        driftToleranceSeconds: Long = 5
    ) {
        val parsedTime = java.time.Instant.parse(expiresAt)
        val expectedTime = java.time.Instant.now().plusSeconds(expectedOffsetSeconds)
        val drift = kotlin.math.abs(
            java.time.Duration.between(parsedTime, expectedTime).seconds
        )
        assertTrue(
            drift < driftToleranceSeconds,
            "expires_at drift: expected ~$expectedTime (now + ${expectedOffsetSeconds}s), got $parsedTime, drift=${drift}s exceeds tolerance ${driftToleranceSeconds}s"
        )
    }

    @Step("Verify error response: expectedCode={expectedCode}, expectedField={expectedField}")
    suspend fun verifyValidationError(
        response: HttpResponse,
        expectedHttpStatus: Int,
        expectedCode: String,
        expectedField: String? = null,
        expectedMessageContains: String? = null
    ): ErrorResponse {
        assertEquals(expectedHttpStatus, response.status.value, "HTTP status mismatch")
        val body = response.body<ErrorResponse>()
        assertEquals(expectedCode, body.code, "error code mismatch")
        expectedField?.let {
            assertEquals(it, body.field, "error field mismatch")
        }
        expectedMessageContains?.let {
            assertTrue(
                body.message.lowercase().contains(it.lowercase()),
                "error message '${body.message}' does not contain '$it'"
            )
        }
        return body
    }

    @Step("Extract user UUID from verification token (JWT email claim)")
    fun extractUserUuid(verificationToken: String): String {
        val parts = verificationToken.split(".")
        assertTrue(parts.size == 3, "JWT must have 3 parts, got ${parts.size}")
        val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
        val emailRegex = """"email"\s*:\s*"([^"]+)"""".toRegex()
        val match = emailRegex.find(payload)
        return match?.groupValues?.get(1) ?: ""
    }
}
