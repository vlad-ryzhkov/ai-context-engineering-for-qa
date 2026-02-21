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
import java.util.stream.Stream

@Epic("User Registration")
@Feature("POST /api/v1/users/register — Validation")
@Tag("REGRESSION")
class RegistrationNegValidationTests {

    companion object {
        @JvmStatic
        fun missingFieldProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("REG-NEG-MISSING-01", "email",
                """{"phone":"+79991234567","password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-MISSING-02", "phone",
                """{"email":"${TestData.uniqueEmail()}","password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-MISSING-03", "password",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-MISSING-04", "full_name",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","password":"Safe1@Pass"}""")
        )

        @JvmStatic
        fun nullFieldProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("REG-NEG-NULL-01", "email",
                """{"email":null,"phone":"+79991234567","password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-NULL-02", "phone",
                """{"email":"${TestData.uniqueEmail()}","phone":null,"password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-NULL-03", "password",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","password":null,"full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-NULL-04", "full_name",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","password":"Safe1@Pass","full_name":null}""")
        )

        @JvmStatic
        fun emptyFieldProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("REG-NEG-EMPTY-01", "email",
                """{"email":"","phone":"+79991234567","password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-EMPTY-02", "phone",
                """{"email":"${TestData.uniqueEmail()}","phone":"","password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-EMPTY-03", "password",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","password":"","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-EMPTY-04", "full_name",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","password":"Safe1@Pass","full_name":""}""")
        )

        @JvmStatic
        fun wrongTypeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("REG-NEG-TYPE-01", "email",
                """{"email":["test@example.com"],"phone":"+79991234567","password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-TYPE-02", "phone",
                """{"email":"${TestData.uniqueEmail()}","phone":{"number":"+79991234567"},"password":"Safe1@Pass","full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-TYPE-03", "password",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","password":true,"full_name":"John Doe"}"""),
            Arguments.of("REG-NEG-TYPE-04", "full_name",
                """{"email":"${TestData.uniqueEmail()}","phone":"+79991234567","password":"Safe1@Pass","full_name":12345}""")
        )
    }

    @ParameterizedTest(name = "{0}: Missing field ''{1}''")
    @MethodSource("missingFieldProvider")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Missing required field")
    @Link(name = "Scenario REG-NEG-MISSING-01..04", url = "file://audit/test-scenarios.md")
    fun missingRequiredField(scenarioId: String, expectedField: String, rawJson: String) = runTest {
        val response = RegistrationHelper.registerUserRawJson(rawJson)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = expectedField
        )
    }

    @ParameterizedTest(name = "{0}: Null field ''{1}''")
    @MethodSource("nullFieldProvider")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Null value for required field")
    @Link(name = "Scenario REG-NEG-NULL-01..04", url = "file://audit/test-scenarios.md")
    fun nullRequiredField(scenarioId: String, expectedField: String, rawJson: String) = runTest {
        val response = RegistrationHelper.registerUserRawJson(rawJson)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = expectedField
        )
    }

    @ParameterizedTest(name = "{0}: Empty field ''{1}''")
    @MethodSource("emptyFieldProvider")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Empty string for required field")
    @Link(name = "Scenario REG-NEG-EMPTY-01..04", url = "file://audit/test-scenarios.md")
    fun emptyRequiredField(scenarioId: String, expectedField: String, rawJson: String) = runTest {
        val response = RegistrationHelper.registerUserRawJson(rawJson)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = expectedField
        )
    }

    @ParameterizedTest(name = "{0}: Wrong JSON type for ''{1}''")
    @MethodSource("wrongTypeProvider")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Wrong JSON type for field")
    @Link(name = "Scenario REG-NEG-TYPE-01..04", url = "file://audit/test-scenarios.md")
    fun wrongJsonType(scenarioId: String, expectedField: String, rawJson: String) = runTest {
        val response = RegistrationHelper.registerUserRawJson(rawJson)

        RegistrationHelper.verifyValidationError(
            response = response,
            expectedHttpStatus = 400,
            expectedCode = "VALIDATION_ERROR",
            expectedField = expectedField
        )
    }
}
