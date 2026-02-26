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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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
import registration.requests.RegisterResponse

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("REGRESSION")
@Severity(SeverityLevel.NORMAL)
class RegistrationL10nTests {

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
    @Link(name = "Scenario REG-L10N-01", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-L10N-01: full_name with Cyrillic characters — SPEC AMBIGUITY: generated as POS (permissive interpretation)")
    fun `REG-L10N-01 full name cyrillic characters`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email
        val fullName = TestData.cyrillicName()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), fullName)

        val statusValue = response.status.value
        assertTrue(
            statusValue == 201 || statusValue == 400,
            "SPEC AMBIGUITY: Unicode block inclusion unspecified. Accepted 201 (permissive) or 400 (restrictive). Got $statusValue",
        )
        if (statusValue == 201) {
            val body = response.body<RegisterResponse>()
            assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
            assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
            assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
            assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
            assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
            RegistrationHelper.assertDbRecordExists(email, "PENDING")
            RegistrationHelper.assertSmsGatewayCalled(1)
        } else {
            val errorBody = response.body<ErrorResponse>()
            assertEquals("VALIDATION_ERROR", errorBody.code, "If rejected: error code must be VALIDATION_ERROR")
            assertEquals("full_name", errorBody.field, "If rejected: error field must be full_name")
        }
    }

    @Test
    @Link(name = "Scenario REG-L10N-02", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-L10N-02: full_name with Arabic/RTL characters — SPEC AMBIGUITY: generated as POS (permissive interpretation)")
    fun `REG-L10N-02 full name arabic rtl characters`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email
        val fullName = TestData.arabicName()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), fullName)

        val statusValue = response.status.value
        assertTrue(
            statusValue == 201 || statusValue == 400,
            "SPEC AMBIGUITY: Unicode block inclusion unspecified. Accepted 201 (permissive) or 400 (restrictive). Got $statusValue",
        )
        if (statusValue == 201) {
            val body = response.body<RegisterResponse>()
            assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
            assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
            assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
            assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
            assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
            RegistrationHelper.assertDbRecordExists(email, "PENDING")
            RegistrationHelper.assertSmsGatewayCalled(1)
        } else {
            val errorBody = response.body<ErrorResponse>()
            assertEquals("VALIDATION_ERROR", errorBody.code, "If rejected: error code must be VALIDATION_ERROR")
            assertEquals("full_name", errorBody.field, "If rejected: error field must be full_name")
        }
    }

    @Test
    @Link(name = "Scenario REG-L10N-03", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-L10N-03: full_name with CJK characters — SPEC AMBIGUITY: generated as POS (permissive interpretation)")
    fun `REG-L10N-03 full name cjk characters`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email
        val fullName = TestData.chineseName()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), fullName)

        val statusValue = response.status.value
        assertTrue(
            statusValue == 201 || statusValue == 400,
            "SPEC AMBIGUITY: Unicode block inclusion unspecified. Accepted 201 (permissive) or 400 (restrictive). Got $statusValue",
        )
        if (statusValue == 201) {
            val body = response.body<RegisterResponse>()
            assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
            assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
            assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
            assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
            assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
            RegistrationHelper.assertDbRecordExists(email, "PENDING")
            RegistrationHelper.assertSmsGatewayCalled(1)
        } else {
            val errorBody = response.body<ErrorResponse>()
            assertEquals("VALIDATION_ERROR", errorBody.code, "If rejected: error code must be VALIDATION_ERROR")
            assertEquals("full_name", errorBody.field, "If rejected: error field must be full_name")
        }
    }

    @Test
    @Link(name = "Scenario REG-L10N-04", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-L10N-04: full_name with emoji (forbidden — outside allowlist) → 400 VALIDATION_ERROR")
    fun `REG-L10N-04 full name with emoji rejected`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.emojiName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for emoji in full_name")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-L10N-05", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-L10N-05: full_name with HTML special characters & < > \" (outside allowlist) → 400 VALIDATION_ERROR")
    fun `REG-L10N-05 full name with html special chars rejected`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.htmlSpecialCharsName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for HTML special chars in full_name")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-L10N-06", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-L10N-06: full_name with Latin-Extended characters (é, ü, ñ) — explicitly allowed by spec → 201 Created")
    fun `REG-L10N-06 full name latin extended allowed`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), "José García-López")

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created for Latin-Extended full_name")
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
        RegistrationHelper.assertDbRecordExists(email, "PENDING")
        RegistrationHelper.assertSmsGatewayCalled(1)
    }
}
