package registration.helpers

import net.datafaker.Faker
import java.util.UUID

object TestData {

    private val faker = Faker()

    fun uniqueEmail(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "").take(12)
        return "user_$uuid@example.com"
    }

    fun longEmail(targetLength: Int): String {
        val domain = "@example.com"
        val localPartLength = targetLength - domain.length
        val uuid = UUID.randomUUID().toString().replace("-", "")
        val padding = "a".repeat((localPartLength - uuid.length).coerceAtLeast(0))
        return (uuid + padding).take(localPartLength) + domain
    }

    fun uniquePhone(): String {
        val digits = (1000000..9999999).random()
        return "+7999$digits"
    }

    fun alternativePhone(): String {
        val digits = (1000000..9999999).random()
        return "+1212$digits"
    }

    fun validPassword(): String = "Safe1@Pass"

    fun fullName(): String = "${faker.name().firstName()} ${faker.name().lastName()}"

    fun idempotencyKey(): String = UUID.randomUUID().toString()

    fun validRequest(): registration.requests.RegisterRequest {
        return registration.requests.RegisterRequest(
            email = uniqueEmail(),
            phone = uniquePhone(),
            password = validPassword(),
            fullName = fullName()
        )
    }
}
