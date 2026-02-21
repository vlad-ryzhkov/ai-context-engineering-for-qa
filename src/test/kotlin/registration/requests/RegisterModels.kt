package registration.requests

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy

@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegisterRequest(
    val email: String,
    val phone: String,
    val password: String,
    val fullName: String
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RegisterResponse(
    val verificationToken: String = "",
    val expiresAt: String = ""
)

@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorResponse(
    val code: String = "",
    val field: String = "",
    val message: String = ""
)
