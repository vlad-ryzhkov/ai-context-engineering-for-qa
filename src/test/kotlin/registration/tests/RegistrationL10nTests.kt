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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.RegisterRequest
import registration.requests.RegisterResponse

@Epic("User Registration")
@Feature("POST /api/v1/users/register — L10N")
class RegistrationL10nTests {

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

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "REG-L10N-01|Cyrillic Characters|\u0418\u0432\u0430\u043d \u041f\u0435\u0442\u0440\u043e\u0432",
            "REG-L10N-02|Arabic Characters RTL|\u0645\u062d\u0645\u062f \u0639\u0644\u064a",
            "REG-L10N-03|CJK Characters|\u674e\u660e",
            "REG-L10N-04|Latin-Extended Characters|Fran\u00e7ois Jos\u00e9 Mar\u00eda"
        ]
    )
    @Tag("REGRESSION")
    @Severity(SeverityLevel.NORMAL)
    @Link(name = "Scenario REG-L10N-01..04", url = "file://audit/test-scenarios.md")
    fun validUnicodeFullName(
        scenarioId: String,
        description: String,
        fullName: String
    ) = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = fullName
        )

        val response = RegistrationHelper.registerUser(request)

        assertEquals(201, response.status.value, "Expected 201 Created for $scenarioId")
        RegistrationHelper.verifySecurityHeaders(response)
        val body = response.body<RegisterResponse>()
        assertTrue(body.verificationToken.isNotBlank(), "verification_token must not be blank")
        assertTrue(body.expiresAt.isNotBlank(), "expires_at must not be blank")
        createdUserTokens.add(body.verificationToken)
    }

    @Test
    @Tag("REGRESSION")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REG-L10N-05: Emoji in Full Name (Invalid)")
    @Link(name = "Scenario REG-L10N-05", url = "file://audit/test-scenarios.md")
    fun emojiInFullNameInvalid() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = "John \uD83D\uDE00 Doe"
        )

        val response = RegistrationHelper.registerUser(request)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "full_name"
        )
    }
}
