package registration.tests

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.RegisterRequest
import java.util.stream.Stream

@Epic("User Registration")
@Feature("POST /api/v1/users/register — Password Validation")
@Tag("CRITICAL")
class RegistrationNegPasswordTests {

    companion object {
        @JvmStatic
        fun passwordValidationProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("REG-NEG-PWD-01", "Pass1@", "at least 8"),
            Arguments.of("REG-NEG-PWD-02", "safe1@pass", "uppercase"),
            Arguments.of("REG-NEG-PWD-03", "SafePass@", "digit"),
            Arguments.of("REG-NEG-PWD-04", "SafePass1", "special")
        )

        @JvmStatic
        fun piiTokenProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "REG-NEG-PWD-05", "John1@Test", "John Doe", "user_pii05@example.com"
            ),
            Arguments.of(
                "REG-NEG-PWD-06", "John.smith1@Pass", "Jane Doe", "john.smith@example.com"
            )
        )
    }

    @ParameterizedTest(name = "{0}: password=''{1}'' expects message containing ''{2}''")
    @MethodSource("passwordValidationProvider")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Password validation rules")
    @Link(name = "Scenario REG-NEG-PWD-01..04", url = "file://audit/test-scenarios.md")
    fun passwordValidationRules(
        scenarioId: String,
        password: String,
        expectedMessageContains: String
    ) = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = password,
            fullName = TestData.fullName()
        )

        val response = RegistrationHelper.registerUser(request)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "password",
            expectedMessageContains = expectedMessageContains
        )
    }

    @ParameterizedTest(name = "{0}: password=''{1}'' contains PII token from ''{2}''/''{3}''")
    @MethodSource("piiTokenProvider")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Password contains PII token")
    @Link(name = "Scenario REG-NEG-PWD-05..06", url = "file://audit/test-scenarios.md")
    fun passwordContainsPiiToken(
        scenarioId: String,
        password: String,
        fullName: String,
        email: String
    ) = runTest {
        val request = RegisterRequest(
            email = email,
            phone = TestData.uniquePhone(),
            password = password,
            fullName = fullName
        )

        val response = RegistrationHelper.registerUser(request)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "password"
        )
    }
}
