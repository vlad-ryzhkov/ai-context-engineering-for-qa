package registration.tests

import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Link
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import registration.helpers.RegistrationHelper
import registration.helpers.TestData
import registration.requests.RegisterRequest

@Epic("User Registration")
@Feature("POST /api/v1/users/register — Format Validation")
@Tag("REGRESSION")
class RegistrationNegFormatTests {

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REG-NEG-FORMAT-01: Email Not Lowercase")
    @Link(name = "Scenario REG-NEG-FORMAT-01", url = "file://audit/test-scenarios.md")
    fun emailNotLowercase() = runTest {
        val request = RegisterRequest(
            email = "John.Smith@EXAMPLE.COM",
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.fullName()
        )

        val response = RegistrationHelper.registerUser(request)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "email",
            expectedMessageContains = "lowercase"
        )
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REG-NEG-FORMAT-02: Phone Invalid Format (Not E.164)")
    @Link(name = "Scenario REG-NEG-FORMAT-02", url = "file://audit/test-scenarios.md")
    fun phoneInvalidFormatNotE164() = runTest {
        val request = RegisterRequest(
            email = TestData.uniqueEmail(),
            phone = "89991234567",
            password = TestData.validPassword(),
            fullName = TestData.fullName()
        )

        val response = RegistrationHelper.registerUser(request)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "phone"
        )
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("REG-NEG-FORMAT-03: Email Exceeds 254 Characters")
    @Link(name = "Scenario REG-NEG-FORMAT-03", url = "file://audit/test-scenarios.md")
    fun emailExceeds254Characters() = runTest {
        val request = RegisterRequest(
            email = TestData.longEmail(255),
            phone = TestData.uniquePhone(),
            password = TestData.validPassword(),
            fullName = TestData.fullName()
        )

        val response = RegistrationHelper.registerUser(request)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = "email"
        )
    }
}
