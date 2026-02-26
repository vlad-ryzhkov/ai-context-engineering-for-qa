package registration.helpers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import registration.requests.RegisterApiClient
import java.util.Base64

@JsonIgnoreProperties(ignoreUnknown = true)
data class JwtHeader(
    val alg: String = "",
    val typ: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JwtPayload(
    val exp: Long = 0L,
    val sub: String = "",
    val email: String = "",
    val aud: String = "",
    val phone: String? = null,
    val password: String? = null,
)

object JwtHelper {

    fun decodePayload(jwt: String): JwtPayload {
        val parts = jwt.split(".")
        require(parts.size == 3) { "Invalid JWT structure: expected 3 parts, got ${parts.size}" }
        val payloadJson = String(Base64.getUrlDecoder().decode(padBase64(parts[1])))
        return RegisterApiClient.objectMapper.readValue(payloadJson, JwtPayload::class.java)
    }

    fun getAlgorithm(jwt: String): String {
        val parts = jwt.split(".")
        require(parts.size == 3) { "Invalid JWT structure: expected 3 parts, got ${parts.size}" }
        val headerJson = String(Base64.getUrlDecoder().decode(padBase64(parts[0])))
        val header = RegisterApiClient.objectMapper.readValue(headerJson, JwtHeader::class.java)
        return header.alg
    }

    private fun padBase64(input: String): String {
        val padded = input.replace('-', '+').replace('_', '/')
        return when (padded.length % 4) {
            2 -> "$padded=="
            3 -> "$padded="
            else -> padded
        }
    }
}
