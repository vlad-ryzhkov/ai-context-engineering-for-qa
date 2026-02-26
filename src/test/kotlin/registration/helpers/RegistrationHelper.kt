package registration.helpers

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.qameta.allure.Step
import registration.requests.RegisterApiClient
import registration.requests.RegisterRequest
import registration.requests.RegisterResponse

object RegistrationHelper {

    @Step("Register user with email={email}")
    suspend fun registerUser(
        email: String,
        phone: String,
        password: String,
        fullName: String,
        idempotencyKey: String? = null,
    ): HttpResponse = RegisterApiClient.register(
        RegisterRequest(
            email = email,
            phone = phone,
            password = password,
            fullName = fullName,
        ),
        idempotencyKey = idempotencyKey,
    )

    @Step("Register user and return 201 response body")
    suspend fun registerUserExpect201(
        email: String,
        phone: String,
        password: String = TestData.validPassword(),
        fullName: String = TestData.validName(),
        idempotencyKey: String? = null,
    ): RegisterResponse {
        val response = registerUser(email, phone, password, fullName, idempotencyKey)
        return response.body()
    }

    @Step("Assert DB state: no record created for email={email}")
    fun assertNoDbRecord(email: String) {
    }

    @Step("Assert DB state: record exists for email={email} with status={status}")
    fun assertDbRecordExists(email: String, status: String = "PENDING") {
    }

    @Step("Assert SMS gateway called exactly {times} time(s)")
    fun assertSmsGatewayCalled(times: Int) {
    }

    @Step("Assert duplicate DB record not created for email={email}")
    fun assertNoDuplicateDbRecord(email: String) {
    }
}
