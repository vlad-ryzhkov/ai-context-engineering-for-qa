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
class RegistrationBvaTests {

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
    @Link(name = "Scenario REG-BVA-01", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-01: password at min boundary — 8 chars passes → 201 Created")
    fun `REG-BVA-01 password at min boundary 8 chars passes`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.pass8CharsValid(), TestData.validName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created for 8-char password")
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
        RegistrationHelper.assertDbRecordExists(email, "PENDING")
        RegistrationHelper.assertSmsGatewayCalled(1)
    }

    @Test
    @Link(name = "Scenario REG-BVA-02", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-02: password at min-1 boundary — 7 chars fails → 400 VALIDATION_ERROR")
    fun `REG-BVA-02 password at min minus 1 boundary 7 chars fails`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.pass7CharsValidComplexity(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for 7-char password")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-BVA-03", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-03: password at max boundary — 64 chars passes → 201 Created")
    fun `REG-BVA-03 password at max boundary 64 chars passes`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.pass64CharsValid(), TestData.validName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created for 64-char password")
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
        RegistrationHelper.assertDbRecordExists(email, "PENDING")
        RegistrationHelper.assertSmsGatewayCalled(1)
    }

    @Test
    @Link(name = "Scenario REG-BVA-04", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-04: password at max+1 boundary — 65 chars fails → 400 VALIDATION_ERROR")
    fun `REG-BVA-04 password at max plus 1 boundary 65 chars fails`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.pass65CharsValidComplexity(),
            fullName = TestData.validName(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for 65-char password")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-BVA-05", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-05: full_name at min boundary — 2 chars passes → 201 Created")
    fun `REG-BVA-05 full name at min boundary 2 chars passes`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), "Al")

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created for 2-char full_name")
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
        RegistrationHelper.assertDbRecordExists(email, "PENDING")
        RegistrationHelper.assertSmsGatewayCalled(1)
    }

    @Test
    @Link(name = "Scenario REG-BVA-06", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-06: full_name at min-1 boundary — 1 char fails → 400 VALIDATION_ERROR")
    fun `REG-BVA-06 full name at min minus 1 boundary 1 char fails`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "A",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for 1-char full_name")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-BVA-07", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-07: full_name at max boundary — 100 chars passes → 201 Created")
    fun `REG-BVA-07 full name at max boundary 100 chars passes`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email
        val fullName = TestData.name100Chars()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), fullName)

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created for 100-char full_name")
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
        RegistrationHelper.assertDbRecordExists(email, "PENDING")
        RegistrationHelper.assertSmsGatewayCalled(1)
    }

    @Test
    @Link(name = "Scenario REG-BVA-08", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-BVA-08: full_name at max+1 boundary — 101 chars fails → 400 VALIDATION_ERROR")
    fun `REG-BVA-08 full name at max plus 1 boundary 101 chars fails`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.name101Chars(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request for 101-char full_name")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }
}
