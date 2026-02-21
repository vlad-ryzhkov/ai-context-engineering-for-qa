package registration.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonNaming
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy

@JsonNaming(SnakeCaseStrategy::class)
data class RegisterRequest(
    val email: Any? = null,
    val phone: Any? = null,
    val password: Any? = null,
    val fullName: Any? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegisterResponse(
    val verificationToken: String? = null,
    val expiresAt: String? = null,
    val userId: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorResponse(
    val code: String? = null,
    val field: String? = null,
    val message: String? = null
)

object Endpoints {
    const val REGISTER = "/api/v1/users/register"
}

object TestConfig {
    const val BASE_URL = "http://localhost:8080"
    const val JWT_SECRET = "test-secret-key"
}
