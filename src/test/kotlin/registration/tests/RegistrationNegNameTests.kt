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
import registration.helpers.TestData
import registration.requests.ErrorResponse
import registration.requests.RegisterApiClient
import registration.requests.RegisterRequest

@Epic("User Registration")
@Feature("POST /api/v1/users/register")
@ExtendWith(MockServerExtension::class)
@Tag("REGRESSION")
@Severity(SeverityLevel.NORMAL)
class RegistrationNegNameTests {

    @Test
    @Link(name = "Scenario REG-32", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-32: full_name too short — 1 char (below min:2) → 400 VALIDATION_ERROR")
    fun `REG-32 full name too short 1 char`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "A",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-33", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-33: full_name too long — 101 chars (above max:100) → 400 VALIDATION_ERROR")
    fun `REG-33 full name too long 101 chars`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.name101Chars(),
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-34", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-34: full_name contains apostrophe (explicitly forbidden by spec) → 400 VALIDATION_ERROR")
    fun `REG-34 full name contains apostrophe`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "O'Brien",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-35", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-35: full_name contains consecutive spaces → 400 VALIDATION_ERROR")
    fun `REG-35 full name consecutive spaces`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "John  Smith",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-36", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-36: full_name has leading space → 400 VALIDATION_ERROR")
    fun `REG-36 full name leading space`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = " John Smith",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }

    @Test
    @Link(name = "Scenario REG-37", url = "file://docs/api-isolated-tests/test-scenarios_20260226_120000.md")
    @DisplayName("REG-37: full_name has trailing space → 400 VALIDATION_ERROR")
    fun `REG-37 full name trailing space`(): Unit = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "John Smith ",
        )

        val response = RegisterApiClient.register(request)

        assertEquals(HttpStatusCode.BadRequest, response.status, "Expected 400 Bad Request")
        val body = response.body<ErrorResponse>()
        assertEquals("VALIDATION_ERROR", body.code, "error code mismatch")
        assertEquals("full_name", body.field, "error field mismatch")
    }
}
