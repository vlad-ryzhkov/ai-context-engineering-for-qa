package registration.helpers

import net.datafaker.Faker
import java.util.UUID

object TestData {

    private val faker = Faker()

    fun uniqueEmail(): String = "test_${UUID.randomUUID().toString().replace("-", "")}@example.com"

    fun uniquePhone(): String {
        val digits = (1..9).map { faker.number().digit() }.joinToString("")
        return "+1555${digits.take(7)}"
    }

    fun validPassword(): String = "Safe_Password_2026!"

    fun minValidName(): String = "Al"

    fun validName(): String = "Alex Smith"

    fun hyphenatedUnicodeName(): String = "Marie-Élise"

    fun pass8CharsValid(): String = "aB1!xyzQ"

    fun pass64CharsValid(): String = "aB1!" + "x".repeat(60)

    fun pass7CharsValidComplexity(): String = "aB1!xyz"

    fun pass65CharsValidComplexity(): String = "aB1!" + "x".repeat(61)

    fun passAllLowerDigitSpecial(): String = "ab1!xyzqwert"

    fun passUpperLowerSpecialNoDigit(): String = "AbXyZq!@#"

    fun passUpperLowerDigitNoSpecial(): String = "AbXyZq123"

    fun name100Chars(): String = "A" + "b".repeat(99)

    fun name101Chars(): String = "A" + "b".repeat(100)

    fun cyrillicName(): String = "Александр"

    fun arabicName(): String = "أحمد علي"

    fun chineseName(): String = "张伟"

    fun emojiName(): String = "Alex 😊"

    fun htmlSpecialCharsName(): String = "Smith & Jones"

    fun oversizedBody(): String {
        val padding = "x".repeat(1_100_000)
        return """{"email":"test@example.com","phone":"+12345678","password":"Safe_Password_2026!","full_name":"$padding"}"""
    }
}
