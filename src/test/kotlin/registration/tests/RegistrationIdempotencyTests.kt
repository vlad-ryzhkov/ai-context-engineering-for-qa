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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.ErrorResponse
import registration.requests.RegisterRequest
import registration.requests.RegisterResponse

@Epic("User Registration")
@Feature("POST /api/v1/users/register — Idempotency")
@Tag("CRITICAL")
class RegistrationIdempotencyTests {

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
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REG-IDEM-01: Idempotency - First Request with Key")
    @Link(name = "Scenario REG-IDEM-01", url = "file://audit/test-scenarios.md")
    fun idempotencyFirstRequestWithKey() = runTest {
        val request = TestData.validRequest()
        val idempotencyKey = TestData.idempotencyKey()

        val response = RegistrationHelper.registerUser(request, idempotencyKey)

        assertEquals(201, response.status.value, "Expected 201 Created")
        RegistrationHelper.verifySecurityHeaders(response)
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        createdUserTokens.add(body.verificationToken)
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REG-IDEM-02: Idempotency - Cached Response Within 5 Minutes")
    @Link(name = "Scenario REG-IDEM-02", url = "file://audit/test-scenarios.md")
    fun idempotencyCachedResponseWithin5Minutes() = runTest {
        val request = TestData.validRequest()
        val idempotencyKey = TestData.idempotencyKey()

        val firstResponse = RegistrationHelper.registerUser(request, idempotencyKey)
        assertEquals(201, firstResponse.status.value, "First request: expected 201 Created")
        val firstBody = firstResponse.body<RegisterResponse>()
        createdUserTokens.add(firstBody.verificationToken)

        val secondResponse = RegistrationHelper.registerUser(request, idempotencyKey)
        assertEquals(201, secondResponse.status.value, "Second request: expected 201 Created")
        RegistrationHelper.verifySecurityHeaders(secondResponse)
        val secondBody = secondResponse.body<RegisterResponse>()

        assertEquals(
            firstBody.verificationToken,
            secondBody.verificationToken,
            "Cached response must return the same verification_token"
        )
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REG-IDEM-03: Idempotency - Cache Expired + Uniqueness Conflict")
    @Link(name = "Scenario REG-IDEM-03", url = "file://audit/test-scenarios.md")
    @Disabled("Time-dependent scenario: >5 minutes cache TTL. Requires testability hook (time-travel/cache-clear) or manual execution.")
    fun idempotencyCacheExpiredUniquenessConflict() = runTest {
        val request = TestData.validRequest()
        val idempotencyKey = TestData.idempotencyKey()

        val firstResponse = RegistrationHelper.registerUser(request, idempotencyKey)
        assertEquals(201, firstResponse.status.value, "First request: expected 201 Created")
        val firstBody = firstResponse.body<RegisterResponse>()
        createdUserTokens.add(firstBody.verificationToken)

        val secondResponse = RegistrationHelper.registerUser(request, idempotencyKey)
        assertEquals(409, secondResponse.status.value, "After cache expiry: uniqueness constraint applies, expected 409 Conflict")
        val errorBody = secondResponse.body<ErrorResponse>()
        assertEquals("CONFLICT", errorBody.code, "error code mismatch: uniqueness wins after idempotency cache expiry")
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REG-IDEM-04: Idempotency - Body Mismatch Within Cache Window")
    @Link(name = "Scenario REG-IDEM-04", url = "file://audit/test-scenarios.md")
    fun idempotencyBodyMismatchWithinCacheWindow() = runTest {
        val originalRequest = TestData.validRequest()
        val idempotencyKey = TestData.idempotencyKey()

        val firstResponse = RegistrationHelper.registerUser(originalRequest, idempotencyKey)
        assertEquals(201, firstResponse.status.value, "First request: expected 201 Created")
        val firstBody = firstResponse.body<RegisterResponse>()
        createdUserTokens.add(firstBody.verificationToken)

        val mismatchedRequest = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.alternativePhone(),
            password = TestData.validPassword(),
            fullName = TestData.fullName()
        )

        val secondResponse = RegistrationHelper.registerUser(mismatchedRequest, idempotencyKey)
        assertEquals(400, secondResponse.status.value, "Body mismatch with same Idempotency-Key: expected 400 Bad Request")
        val errorBody = secondResponse.body<ErrorResponse>()
        assertEquals("IDEMPOTENCY_KEY_MISMATCH", errorBody.code, "error code mismatch: same key + different body = IDEMPOTENCY_KEY_MISMATCH")
    }
}
