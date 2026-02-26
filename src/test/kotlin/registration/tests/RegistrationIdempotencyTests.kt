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
import org.junit.jupiter.api.Disabled
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
import registration.requests.RegisterResponse
import java.util.UUID

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("CRITICAL")
@Severity(SeverityLevel.CRITICAL)
class RegistrationIdempotencyTests {

    private lateinit var createdEmail: String

    @BeforeEach
    fun setUp() {
        MockRegistrationServer.stubSmsGatewaySuccess()
    }

    @AfterEach
    fun tearDown() {
        if (::createdEmail.isInitialized) {
            RegistrationHelper.assertNoDbRecord(createdEmail)
        }
        MockRegistrationServer.resetAll()
    }

    @Test
    @Link(name = "Scenario REG-IDEM-01", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-IDEM-01: Same Idempotency-Key within cache window — returns cached result, no duplicate")
    fun `REG-IDEM-01 same idempotency key within cache window returns cached token`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        val idempotencyKey = UUID.randomUUID().toString()
        createdEmail = email

        val firstResponse = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName(), idempotencyKey)
        assertEquals(HttpStatusCode.Created, firstResponse.status, "First request: expected 201 Created")
        val firstBody = firstResponse.body<RegisterResponse>()

        val secondResponse = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName(), idempotencyKey)
        assertEquals(HttpStatusCode.Created, secondResponse.status, "Second request: expected 201 Created (cached)")
        val secondBody = secondResponse.body<RegisterResponse>()

        assertEquals(firstBody.verificationToken, secondBody.verificationToken, "Cached response must return same verification_token")
        RegistrationHelper.assertNoDuplicateDbRecord(email)
        RegistrationHelper.assertSmsGatewayCalled(1)
    }

    @Test
    @Link(name = "Scenario REG-IDEM-02", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-IDEM-02: Repeated request without Idempotency-Key — second attempt finds PENDING user → 409")
    fun `REG-IDEM-02 no idempotency key second attempt conflict`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val firstResponse = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName())
        assertEquals(HttpStatusCode.Created, firstResponse.status, "First request: expected 201 Created")

        val secondResponse = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName())
        assertEquals(HttpStatusCode.Conflict, secondResponse.status, "Second request: expected 409 Conflict")
        val errorBody = secondResponse.body<ErrorResponse>()
        assertEquals("CONFLICT", errorBody.code, "error code mismatch")
        RegistrationHelper.assertNoDuplicateDbRecord(email)
    }

    @Test
    @Disabled("Time-dependent scenario: >5 minutes cache expiry. Requires testability hook (time-travel/cache-clear) or manual execution.")
    @Link(name = "Scenario REG-IDEM-03", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-IDEM-03: Same Idempotency-Key, cache expired (>5 min) + PENDING record → 409 (NOT 201)")
    fun `REG-IDEM-03 same idempotency key cache expired pending record conflict`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        val idempotencyKey = UUID.randomUUID().toString()
        createdEmail = email

        val firstResponse = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName(), idempotencyKey)
        assertEquals(HttpStatusCode.Created, firstResponse.status, "First request: expected 201 Created")

        val secondResponse = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName(), idempotencyKey)
        assertEquals(HttpStatusCode.Conflict, secondResponse.status, "After cache expiry: expected 409 Conflict (uniqueness wins)")
        val errorBody = secondResponse.body<ErrorResponse>()
        assertEquals("CONFLICT", errorBody.code, "error code mismatch — uniqueness constraint must win over cache-expiry restart")
        RegistrationHelper.assertNoDuplicateDbRecord(email)
    }

    @Test
    @Link(name = "Scenario REG-IDEM-04", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-IDEM-04: Same Idempotency-Key within cache window, different request body → 400 IDEMPOTENCY_KEY_MISMATCH")
    fun `REG-IDEM-04 same idempotency key different body mismatch`(): Unit = runTest {
        val emailA = TestData.uniqueEmail()
        val emailB = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        val idempotencyKey = UUID.randomUUID().toString()
        createdEmail = emailA

        val firstRequest = RegisterRequest(
            email = emailA,
            phone = phone,
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )
        val firstResponse = RegisterApiClient.register(firstRequest, idempotencyKey)
        assertEquals(HttpStatusCode.Created, firstResponse.status, "First request: expected 201 Created")

        val secondRequest = RegisterRequest(
            email = emailB,
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )
        val secondResponse = RegisterApiClient.register(secondRequest, idempotencyKey)
        assertEquals(HttpStatusCode.BadRequest, secondResponse.status, "Expected 400 Bad Request on body mismatch")
        val errorBody = secondResponse.body<ErrorResponse>()
        assertEquals("IDEMPOTENCY_KEY_MISMATCH", errorBody.code, "error code mismatch")
        RegistrationHelper.assertNoDuplicateDbRecord(emailA)
    }
}
