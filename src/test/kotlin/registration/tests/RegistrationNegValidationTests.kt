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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import registration.helpers.MockServerExtension
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.ErrorResponse
import registration.requests.RegisterRequest

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("REGRESSION")
@Severity(SeverityLevel.NORMAL)
class RegistrationNegValidationTests {

    @Test
    @Link(name = "Scenario REG-07", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-07: Missing email field → 400 VALIDATION_ERROR")
    fun `REG-07 missing email field`(): Unit = runTest {
        val request = RegisterRequest(
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("email", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-08", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-08: Missing phone field → 400 VALIDATION_ERROR")
    fun `REG-08 missing phone field`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("phone", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-09", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-09: Missing password field → 400 VALIDATION_ERROR")
    fun `REG-09 missing password field`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-10", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-10: Missing full_name field → 400 VALIDATION_ERROR")
    fun `REG-10 missing full name field`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-11", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-11: email is null → 400 VALIDATION_ERROR")
    fun `REG-11 email is null`(): Unit = runTest {
        val request = RegisterRequest(
            email = null,
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("email", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-12", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-12: email is empty string → 400 VALIDATION_ERROR")
    fun `REG-12 email is empty string`(): Unit = runTest {
        val request = RegisterRequest(
            email = "",
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("email", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-13", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-13: phone is null → 400 VALIDATION_ERROR")
    fun `REG-13 phone is null`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = null,
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("phone", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-14", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-14: phone is empty string → 400 VALIDATION_ERROR")
    fun `REG-14 phone is empty string`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "",
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("phone", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-15", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-15: password is null → 400 VALIDATION_ERROR")
    fun `REG-15 password is null`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = null,
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-16", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-16: password is empty string → 400 VALIDATION_ERROR")
    fun `REG-16 password is empty string`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = "",
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-17", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-17: full_name is null → 400 VALIDATION_ERROR")
    fun `REG-17 full name is null`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = null,
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-18", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-18: full_name is empty string → 400 VALIDATION_ERROR")
    fun `REG-18 full name is empty string`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "",
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-19", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-19: full_name is single space → 400 VALIDATION_ERROR")
    fun `REG-19 full name is single space`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = " ",
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-20", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-20: Wrong type — email is integer → 400 VALIDATION_ERROR")
    fun `REG-20 email is integer wrong type`(): Unit = runTest {
        val request = RegisterRequest(
            email = 12345,
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("email", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-21", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-21: Wrong type — password is array → 400 VALIDATION_ERROR")
    fun `REG-21 password is array wrong type`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = listOf(TestData.validPassword()),
            fullName = TestData.validName(),
        )

        val response = registration.requests.RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("password", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-22", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-22: Malformed JSON body (truncated) → 400 VALIDATION_ERROR")
    fun `REG-22 malformed json body truncated`(): Unit = runTest {
        val truncatedBody = """{"email": "test@example.com", "phone": "+12345"""

        val response = registration.requests.RegisterApiClient.registerRaw(truncatedBody)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
    }

    @Test
    @Link(name = "Scenario REG-23", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-23: Empty JSON body {} → 400 VALIDATION_ERROR")
    fun `REG-23 empty json body`(): Unit = runTest {
        val response = registration.requests.RegisterApiClient.registerRaw("{}")

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
    }
}
