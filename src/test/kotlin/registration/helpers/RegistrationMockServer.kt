package registration.helpers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class RegistrationMockServer {

    private val mapper = ObjectMapper()
    private val registeredEmails: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val registeredPhones: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val idempotencyCache = ConcurrentHashMap<String, IdempotencyEntry>()

    private data class IdempotencyEntry(
        val bodyHash: Int,
        val status: Int,
        val responseBody: String,
    )

    private var httpServer: HttpServer? = null

    val port: Int
        get() = httpServer?.address?.port ?: error("Server not started")

    fun start() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/api/v1/users/register") { handleRegistration(it) }
        server.executor = Executors.newCachedThreadPool()
        server.start()
        httpServer = server
        System.setProperty("BASE_URL", "http://localhost:$port")
    }

    fun stop() {
        httpServer?.stop(0)
        httpServer = null
        registeredEmails.clear()
        registeredPhones.clear()
        idempotencyCache.clear()
        System.clearProperty("BASE_URL")
    }

    private fun handleRegistration(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "POST") {
                respond(exchange, 405, error("METHOD_NOT_ALLOWED", "Method not allowed", null))
                return
            }

            val bodyBytes = exchange.requestBody.readBytes()
            val bodyString = String(bodyBytes, Charsets.UTF_8)
            val idempotencyKey = exchange.requestHeaders.getFirst("Idempotency-Key")

            if (idempotencyKey != null) {
                val cached = idempotencyCache[idempotencyKey]
                if (cached != null) {
                    if (cached.bodyHash == bodyString.hashCode()) {
                        respond(exchange, cached.status, cached.responseBody)
                    } else {
                        respond(exchange, 400, error("IDEMPOTENCY_KEY_MISMATCH", "Request body does not match original", null))
                    }
                    return
                }
            }

            val root: JsonNode = try {
                mapper.readTree(bodyString)
            } catch (_: Exception) {
                respond(exchange, 400, error("VALIDATION_ERROR", "Invalid JSON body", null))
                return
            }

            val emailError = validateEmail(root)
            if (emailError != null) {
                respond(exchange, 400, error("VALIDATION_ERROR", emailError.second, emailError.first))
                return
            }

            val phoneError = validatePhone(root)
            if (phoneError != null) {
                respond(exchange, 400, error("VALIDATION_ERROR", phoneError.second, phoneError.first))
                return
            }

            val emailStr = root.get("email").asText()
            val fullNameStr = root.get("full_name")?.takeIf { it.isTextual }?.asText() ?: ""

            val passwordError = validatePassword(root, emailStr, fullNameStr)
            if (passwordError != null) {
                respond(exchange, 400, error("VALIDATION_ERROR", passwordError.second, passwordError.first))
                return
            }

            val fullNameError = validateFullName(root)
            if (fullNameError != null) {
                respond(exchange, 400, error("VALIDATION_ERROR", fullNameError.second, fullNameError.first))
                return
            }

            val normalizedEmail = emailStr.lowercase().trim()
            if (!registeredEmails.add(normalizedEmail)) {
                respond(exchange, 409, error("CONFLICT", "Email already registered", "email"))
                return
            }

            val phoneStr = root.get("phone").asText().trim()
            if (!registeredPhones.add(phoneStr)) {
                registeredEmails.remove(normalizedEmail)
                respond(exchange, 409, error("CONFLICT", "Phone already registered", "phone"))
                return
            }

            val smsGatewayUrl = System.getProperty("SMS_GATEWAY_URL")
            if (smsGatewayUrl != null && !callSmsGateway(smsGatewayUrl)) {
                registeredEmails.remove(normalizedEmail)
                registeredPhones.remove(phoneStr)
                respond(exchange, 503, error("SERVICE_UNAVAILABLE", "SMS gateway unavailable", null))
                return
            }

            val exp = Instant.now().plusSeconds(900)
            val token = generateMockJwt(emailStr, exp.epochSecond)
            val responseBody = """{"verification_token":"$token","expires_at":"$exp"}"""

            if (idempotencyKey != null) {
                idempotencyCache[idempotencyKey] = IdempotencyEntry(bodyString.hashCode(), 201, responseBody)
            }

            respond(exchange, 201, responseBody)
        } catch (e: Exception) {
            respond(exchange, 500, error("INTERNAL_ERROR", e.message ?: "Unexpected error", null))
        }
    }

    private fun validateEmail(root: JsonNode): Pair<String, String>? {
        val field = "email"
        val node = root.get(field) ?: return Pair(field, "email is required")
        if (node.isNull) return Pair(field, "email must not be null")
        if (!node.isTextual) return Pair(field, "email must be a string")
        val value = node.asText()
        if (value.isBlank()) return Pair(field, "email must not be empty")
        if (value.length > 254) return Pair(field, "email must not exceed 254 characters")
        if (!value.matches(Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}\$"))) {
            return Pair(field, "email format is invalid")
        }
        return null
    }

    private fun validatePhone(root: JsonNode): Pair<String, String>? {
        val field = "phone"
        val node = root.get(field) ?: return Pair(field, "phone is required")
        if (node.isNull) return Pair(field, "phone must not be null")
        if (!node.isTextual) return Pair(field, "phone must be a string")
        val value = node.asText()
        if (value.isBlank()) return Pair(field, "phone must not be empty")
        if (!value.matches(Regex("^\\+[1-9]\\d{6,14}\$"))) {
            return Pair(field, "phone format must be E.164")
        }
        return null
    }

    private fun validatePassword(root: JsonNode, email: String, fullName: String): Pair<String, String>? {
        val field = "password"
        val node = root.get(field) ?: return Pair(field, "password is required")
        if (node.isNull) return Pair(field, "password must not be null")
        if (!node.isTextual) return Pair(field, "password must be a string")
        val value = node.asText()
        if (value.isBlank()) return Pair(field, "password must not be empty")
        if (value.length < 8) return Pair(field, "password must be at least 8 characters")
        if (value.length > 64) return Pair(field, "password must not exceed 64 characters")
        if (!value.any { it.isUpperCase() }) return Pair(field, "password must contain an uppercase letter")
        if (!value.any { it.isDigit() }) return Pair(field, "password must contain a digit")
        if (!value.any { !it.isLetterOrDigit() }) return Pair(field, "password must contain a special character")
        val emailLocal = email.substringBefore("@").lowercase()
        if (emailLocal.length >= 4 && value.lowercase().contains(emailLocal)) {
            return Pair(field, "password must not contain email local part")
        }
        val nameTokens = fullName.split(" ", "-").filter { it.length >= 4 }
        for (token in nameTokens) {
            if (value.lowercase().contains(token.lowercase())) {
                return Pair(field, "password must not contain name token")
            }
        }
        return null
    }

    private fun validateFullName(root: JsonNode): Pair<String, String>? {
        val field = "full_name"
        val node = root.get(field) ?: return Pair(field, "full_name is required")
        if (node.isNull) return Pair(field, "full_name must not be null")
        if (!node.isTextual) return Pair(field, "full_name must be a string")
        val value = node.asText()
        if (value.isBlank()) return Pair(field, "full_name must not be empty")
        if (value.length < 2) return Pair(field, "full_name must be at least 2 characters")
        if (value.length > 100) return Pair(field, "full_name must not exceed 100 characters")
        if (value.startsWith(" ") || value.endsWith(" ")) return Pair(field, "full_name must not have leading or trailing spaces")
        if (value.contains("  ")) return Pair(field, "full_name must not have consecutive spaces")
        val validPattern = Regex("^[\\p{L}][\\p{L}\\- ]*[\\p{L}]\$")
        val singleLetterPattern = Regex("^[\\p{L}]\$")
        if (!validPattern.matches(value) && !singleLetterPattern.matches(value)) {
            return Pair(field, "full_name contains invalid characters")
        }
        return null
    }

    private fun generateMockJwt(email: String, exp: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = encoder.encodeToString(
            """{"sub":"registration","email":"$email","aud":"sms-verification","exp":$exp}""".toByteArray()
        )
        return "$header.$payload.mock-sig"
    }

    private fun callSmsGateway(baseUrl: String): Boolean =
        try {
            val connection = URL("$baseUrl/sms/send").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.doOutput = true
            connection.outputStream.use { it.write("{}".toByteArray()) }
            connection.responseCode in 200..299
        } catch (_: IOException) {
            false
        }

    private fun error(code: String, message: String, field: String?): String =
        if (field != null) {
            """{"code":"$code","message":"$message","field":"$field"}"""
        } else {
            """{"code":"$code","message":"$message"}"""
        }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.add("X-Content-Type-Options", "nosniff")
        exchange.responseHeaders.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
