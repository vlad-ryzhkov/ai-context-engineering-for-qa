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
import org.junit.jupiter.api.Assertions.assertFalse
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
import registration.requests.RegisterResponse
import java.time.Instant
import java.time.temporal.ChronoUnit

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("CRITICAL")
@Severity(SeverityLevel.CRITICAL)
class RegistrationPositiveTests {

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
    @Link(name = "Scenario REG-01", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-01: Happy Path — minimal valid registration")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    fun `REG-01 happy path minimal valid registration`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.minValidName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
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
    @Link(name = "Scenario REG-01h", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-01h: Response headers — Happy Path")
    @Severity(SeverityLevel.NORMAL)
    @Tag("REGRESSION")
    fun `REG-01h response headers happy path`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header must be nosniff")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
    }

    @Test
    @Link(name = "Scenario REG-02", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-02: Happy Path — full_name with hyphen and Latin-Extended chars")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    fun `REG-02 happy path full name hyphenated unicode`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email
        val fullName = TestData.hyphenatedUnicodeName()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), fullName)

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
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
    @Link(name = "Scenario REG-03", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-03: Happy Path — password at minimum valid complexity (8 chars)")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    fun `REG-03 happy path password minimum length`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.pass8CharsValid(), TestData.validName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
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
    @Link(name = "Scenario REG-04", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-04: Happy Path — password at maximum valid length (64 chars)")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    fun `REG-04 happy path password maximum length`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, TestData.pass64CharsValid(), TestData.validName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
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
    @Link(name = "Scenario REG-05", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-05: Happy Path — full_name at maximum length (100 chars)")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    fun `REG-05 happy path full name maximum length`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email
        val fullName = TestData.name100Chars()

        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), fullName)

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
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
    @Link(name = "Scenario REG-06", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-06: verification_token is a valid JWT with required claims; expires_at = request_time + 900s")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    fun `REG-06 verification token jwt claims and expires at`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        createdEmail = email

        val requestTime = Instant.now()
        val response = RegistrationHelper.registerUser(email, phone, TestData.validPassword(), TestData.validName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
        val body = response.body<RegisterResponse>()

        val alg = registration.helpers.JwtHelper.getAlgorithm(body.verificationToken)
        assertEquals("HS256", alg, "JWT algorithm must be HS256")

        val payload = registration.helpers.JwtHelper.decodePayload(body.verificationToken)
        assertEquals("registration", payload.sub, "JWT sub claim must be 'registration'")
        assertEquals(email, payload.email, "JWT email claim must match request email")
        assertEquals("sms-verification", payload.aud, "JWT aud claim must be 'sms-verification'")
        assertEquals(null, payload.phone, "JWT must NOT contain phone claim")
        assertEquals(null, payload.password, "JWT must NOT contain password claim")

        val expectedExp = requestTime.epochSecond + 900L
        val drift = kotlin.math.abs(payload.exp - expectedExp)
        assertTrue(drift < 10L, "JWT exp drift must be < 10 seconds, actual drift: $drift")

        val expiresAt = Instant.parse(body.expiresAt)
        val driftSeconds = ChronoUnit.SECONDS.between(requestTime.plusSeconds(900), expiresAt)
        assertTrue(kotlin.math.abs(driftSeconds) < 10L, "expires_at drift must be < 10 seconds, actual drift: $driftSeconds")

        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
        RegistrationHelper.assertSmsGatewayCalled(1)
    }

    @Test
    @Link(name = "Scenario REG-42", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-42: password field must not appear in response body")
    @Severity(SeverityLevel.CRITICAL)
    @Tag("CRITICAL")
    fun `REG-42 password and phone not in response body`(): Unit = runTest {
        val email = TestData.uniqueEmail()
        val phone = TestData.uniquePhone()
        val password = TestData.validPassword()
        createdEmail = email

        val response = RegistrationHelper.registerUser(email, phone, password, TestData.validName())

        assertEquals(HttpStatusCode.Created, response.status, "Expected 201 Created")
        val rawBody = response.body<String>()
        assertFalse(rawBody.contains("password"), "Response body must NOT contain key 'password'")
        assertFalse(rawBody.contains(password), "Response body must NOT contain the password value")
        assertFalse(rawBody.contains(phone), "Response body must NOT contain phone value")

        assertEquals("application/json; charset=utf-8", response.headers["Content-Type"], "Content-Type header mismatch")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"], "X-Content-Type-Options header mismatch")
        assertNotNull(response.headers["Strict-Transport-Security"], "Strict-Transport-Security header must be present")
        RegistrationHelper.assertDbRecordExists(email, "PENDING")
        RegistrationHelper.assertSmsGatewayCalled(1)
    }
}
