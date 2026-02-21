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
@Feature("POST /api/v1/users/register — Name Validation")
@Tag("REGRESSION")
class RegistrationNegNameTests {

    companion object {
        @JvmStatic
        fun nameValidationProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("REG-NEG-NAME-01", " John Doe", null),
            Arguments.of("REG-NEG-NAME-02", "John Doe ", null),
            Arguments.of("REG-NEG-NAME-03", "John  Doe", null),
            Arguments.of("REG-NEG-NAME-04", "J", "at least 2"),
            Arguments.of("REG-NEG-NAME-05", "John O'Brien", null),
            Arguments.of("REG-NEG-NAME-06", "John@Doe#123", null)
        )
    }

    @ParameterizedTest(name = "{0}: fullName=''{1}''")
    @MethodSource("nameValidationProvider")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Full name validation rules")
    @Link(name = "Scenario REG-NEG-NAME-01..06", url = "file://audit/test-scenarios.md")
    fun fullNameValidationRules(
        scenarioId: String,
        fullName: String,
        expectedMessageContains: String?
    ) = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = fullName
        )

        val response = RegistrationHelper.registerUser(request)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "full_name",
            expectedMessageContains = expectedMessageContains
        )
    }
}
