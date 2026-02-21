package registration.helpers

import registration.requests.RegisterRequest
import java.util.UUID

object TestData {
    private var emailCounter = 0

    fun uniqueEmail(): String {
        emailCounter++
        return "user_${UUID.randomUUID()}_$emailCounter@example.com"
    }

    fun validRegistration(
        email: String = uniqueEmail(),
        phone: String = "+79991234567",
        password: String = "Safe1@Pass",
        fullName: String = "John Doe"
    ): RegisterRequest = RegisterRequest(
        email = email,
        phone = phone,
        password = password,
        fullName = fullName
    )

    fun longEmail(): String {
        val baseEmail = "user_550e8400e29b41d4a716446655440000_"
        val padding = "a".repeat(Math.max(0, 250 - baseEmail.length - 11))
        return "$baseEmail$padding@example.com"
    }

    fun tooLongEmail(): String {
        val baseEmail = "user_550e8400e29b41d4a716446655440000_"
        val padding = "a".repeat(255 - baseEmail.length - 11)
        return "$baseEmail$padding@example.com"
    }
}
