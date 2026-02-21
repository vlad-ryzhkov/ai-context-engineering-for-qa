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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.ErrorResponse
import registration.requests.RegisterRequest
import registration.requests.RegisterResponse

@Epic("User Registration")
@Feature("POST /api/v1/users/register — Conflict")
@Tag("CRITICAL")
class RegistrationNegConflictTests {

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
    @DisplayName("REG-NEG-CONFLICT-01: Duplicate Email - Sequential Conflict")
    @Link(name = "Scenario REG-NEG-CONFLICT-01", url = "file://audit/test-scenarios.md")
    fun duplicateEmailSequentialConflict() = runTest {
        val existingRequest = TestData.validRequest()
        val setupResponse = RegistrationHelper.registerUser(existingRequest)
        assertEquals(201, setupResponse.status.value, "Setup: expected 201 Created")
        val setupBody = setupResponse.body<RegisterResponse>()
        createdUserTokens.add(setupBody.verificationToken)

        val conflictRequest = RegisterRequest(
            email = existingRequest.email,
            phone = TestData.alternativePhone(),
            password = TestData.validPassword(),
            fullName = TestData.fullName()
        )

        val response = RegistrationHelper.registerUser(conflictRequest)

        assertEquals(409, response.status.value, "Expected 409 Conflict")
        val errorBody = response.body<ErrorResponse>()
        assertEquals("CONFLICT", errorBody.code, "error code mismatch")
        assertTrue(
            errorBody.message.lowercase().contains("email already registered"),
            "error message '${errorBody.message}' does not contain 'email already registered'"
        )
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("REG-NEG-CONFLICT-02: Duplicate Phone - Sequential Conflict")
    @Link(name = "Scenario REG-NEG-CONFLICT-02", url = "file://audit/test-scenarios.md")
    fun duplicatePhoneSequentialConflict() = runTest {
        val existingRequest = TestData.validRequest()
        val setupResponse = RegistrationHelper.registerUser(existingRequest)
        assertEquals(201, setupResponse.status.value, "Setup: expected 201 Created")
        val setupBody = setupResponse.body<RegisterResponse>()
        createdUserTokens.add(setupBody.verificationToken)

        val conflictRequest = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = existingRequest.phone,
            password = TestData.validPassword(),
            fullName = TestData.fullName()
        )

        val response = RegistrationHelper.registerUser(conflictRequest)

        assertEquals(409, response.status.value, "Expected 409 Conflict")
        val errorBody = response.body<ErrorResponse>()
        assertEquals("CONFLICT", errorBody.code, "error code mismatch")
        assertTrue(
            errorBody.message.lowercase().contains("phone already registered"),
            "error message '${errorBody.message}' does not contain 'phone already registered'"
        )
    }
}
