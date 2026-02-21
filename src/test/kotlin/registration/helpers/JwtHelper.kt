package registration.helpers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.qameta.allure.Step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@JsonNaming(SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class JwtPayload(
    val exp: Long = 0,
    val sub: String = "",
    val email: String = "",
    val aud: String = ""
)

object JwtHelper {

    private val mapper = jacksonObjectMapper()

    @Step("Decode JWT token and extract payload")
    fun decodePayload(token: String): JwtPayload {
        val parts = token.split(".")
        assertTrue(parts.size == 3, "JWT must have 3 parts, got ${parts.size}")
        val payloadJson = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
        return mapper.readValue(payloadJson)
    }

    @Step("Verify JWT claims: sub={expectedSub}, aud={expectedAud}, email={expectedEmail}")
    fun verifyTokenClaims(
        token: String,
        expectedEmail: String,
        expectedSub: String = "registration",
        expectedAud: String = "sms-verification"
    ): JwtPayload {
        val payload = decodePayload(token)
        assertEquals(expectedSub, payload.sub, "JWT sub claim mismatch")
        assertEquals(expectedAud, payload.aud, "JWT aud claim mismatch")
        assertEquals(expectedEmail, payload.email, "JWT email claim mismatch")
        assertTrue(payload.exp > 0, "JWT exp must be a positive Unix timestamp")
        return payload
    }

    @Step("Verify JWT exp is approximately now + {expectedOffsetSeconds}s (drift tolerance: {driftToleranceSeconds}s)")
    fun verifyExpTimeWindow(
        token: String,
        expectedOffsetSeconds: Long = 900,
        driftToleranceSeconds: Long = 5
    ) {
        val payload = decodePayload(token)
        val nowEpoch = java.time.Instant.now().epochSecond
        val expectedExp = nowEpoch + expectedOffsetSeconds
        val drift = kotlin.math.abs(payload.exp - expectedExp)
        assertTrue(
            drift < driftToleranceSeconds,
            "JWT exp timestamp drift: expected ~$expectedExp (now + ${expectedOffsetSeconds}s), got ${payload.exp}, drift=${drift}s exceeds tolerance ${driftToleranceSeconds}s"
        )
    }

    @Step("Verify JWT does not contain sensitive fields (password, phone)")
    fun verifySensitiveFieldsAbsent(token: String) {
        val parts = token.split(".")
        val payloadJson = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
        assertTrue(
            !payloadJson.contains("\"password\""),
            "JWT must not contain password field"
        )
        assertTrue(
            !payloadJson.contains("\"phone\""),
            "JWT must not contain phone field"
        )
    }
}
