package registration.helpers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object MockRegistrationServer {

    private val mapper = jacksonObjectMapper()
    private var server: HttpServer? = null

    private val registeredEmails = ConcurrentHashMap.newKeySet<String>()
    private val registeredPhones = ConcurrentHashMap.newKeySet<String>()

    data class IdempotencyEntry(val requestBody: String, val responseBody: String)

    private val idempotencyCache = ConcurrentHashMap<String, IdempotencyEntry>()

    @Synchronized
    fun ensureStarted() {
        if (server != null) return
        val s = HttpServer.create(InetSocketAddress(8080), 0)
        s.executor = Executors.newFixedThreadPool(4)
        s.createContext("/") { exchange -> handleRequest(exchange) }
        s.start()
        server = s
    }

    @Synchronized
    fun stop() {
        server?.stop(0)
        server = null
        reset()
    }

    fun reset() {
        registeredEmails.clear()
        registeredPhones.clear()
        idempotencyCache.clear()
    }

    private fun handleRequest(exchange: HttpExchange) {
        try {
            val path = exchange.requestURI.path
            val method = exchange.requestMethod
            when {
                method == "POST" && path == "/api/v1/users/register" -> handleRegister(exchange)
                method == "DELETE" && path.startsWith("/api/v1/users/") -> handleDelete(exchange)
                else -> sendError(exchange, 404, "NOT_FOUND", message = "Not found")
            }
        } catch (e: Exception) {
            sendError(exchange, 500, "INTERNAL_ERROR", message = e.message ?: "Unknown error")
        }
    }

    private fun handleRegister(exchange: HttpExchange) {
        val body = exchange.requestBody.bufferedReader().readText()
        val idempotencyKey = exchange.requestHeaders.getFirst("Idempotency-Key")

        if (idempotencyKey != null) {
            val cached = idempotencyCache[idempotencyKey]
            if (cached != null) {
                if (cached.requestBody == body) {
                    sendRawResponse(exchange, 201, cached.responseBody)
                    return
                } else {
                    sendError(exchange, 400, "IDEMPOTENCY_KEY_MISMATCH",
                        message = "Request body does not match the original request for this idempotency key")
                    return
                }
            }
        }

        val tree: JsonNode
        try {
            tree = mapper.readTree(body)
            if (tree == null || tree.isNull) {
                sendError(exchange, 400, "VALIDATION_ERROR", message = "Request body is required")
                return
            }
        } catch (_: Exception) {
            sendError(exchange, 400, "VALIDATION_ERROR", message = "Invalid JSON")
            return
        }

        val requiredFields = listOf("email", "phone", "password", "full_name")
        for (field in requiredFields) {
            val node = tree.get(field)
            when {
                node == null -> {
                    sendError(exchange, 400, "VALIDATION_ERROR", field = field,
                        message = "Field '$field' is required")
                    return
                }
                node.isNull -> {
                    sendError(exchange, 400, "VALIDATION_ERROR", field = field,
                        message = "Field '$field' must not be null")
                    return
                }
                !node.isTextual -> {
                    sendError(exchange, 400, "VALIDATION_ERROR", field = field,
                        message = "Field '$field' must be a string")
                    return
                }
                node.asText().isEmpty() -> {
                    sendError(exchange, 400, "VALIDATION_ERROR", field = field,
                        message = "Field '$field' must not be empty")
                    return
                }
            }
        }

        val email = tree.get("email").asText()
        val phone = tree.get("phone").asText()
        val password = tree.get("password").asText()
        val fullName = tree.get("full_name").asText()

        validateEmail(email)?.let {
            sendError(exchange, 400, "VALIDATION_ERROR", field = "email", message = it); return
        }
        validatePhone(phone)?.let {
            sendError(exchange, 400, "VALIDATION_ERROR", field = "phone", message = it); return
        }
        validatePassword(password, email, fullName)?.let {
            sendError(exchange, 400, "VALIDATION_ERROR", field = "password", message = it); return
        }
        validateFullName(fullName)?.let {
            sendError(exchange, 400, "VALIDATION_ERROR", field = "full_name", message = it); return
        }

        if (System.getProperty("SMS_GATEWAY_URL") != null) {
            sendError(exchange, 503, "SERVICE_UNAVAILABLE", message = "Service Unavailable")
            return
        }

        val normalizedEmail = email.lowercase()
        if (!registeredEmails.add(normalizedEmail)) {
            sendError(exchange, 409, "CONFLICT", message = "email already registered")
            return
        }
        if (!registeredPhones.add(phone)) {
            registeredEmails.remove(normalizedEmail)
            sendError(exchange, 409, "CONFLICT", message = "phone already registered")
            return
        }

        val now = Instant.now()
        val exp = now.epochSecond + 900
        val expiresAt = now.plusSeconds(900).toString()
        val jwt = generateJwt(email, exp)
        val responseBody = """{"verification_token":"$jwt","expires_at":"$expiresAt"}"""

        if (idempotencyKey != null) {
            idempotencyCache[idempotencyKey] = IdempotencyEntry(body, responseBody)
        }

        sendRawResponse(exchange, 201, responseBody)
    }

    private fun handleDelete(exchange: HttpExchange) {
        sendRawResponse(exchange, 200, """{"status":"deleted"}""")
    }

    private fun validateEmail(email: String): String? {
        if (email != email.lowercase()) return "Email must be lowercase"
        if (email.length > 254) return "Email must not exceed 254 characters"
        if (!email.contains("@")) return "Invalid email format"
        return null
    }

    private fun validatePhone(phone: String): String? {
        if (!phone.startsWith("+")) return "Phone must be in E.164 format"
        if (!phone.substring(1).all { it.isDigit() }) return "Phone must contain only digits after +"
        return null
    }

    private fun validatePassword(password: String, email: String, fullName: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter"
        if (!password.any { it.isDigit() }) return "Password must contain at least one digit"
        if (!password.any { !it.isLetterOrDigit() }) return "Password must contain at least one special character"
        val piiTokens = mutableListOf<String>()
        fullName.split(" ", "-").filter { it.length >= 3 }.forEach { piiTokens.add(it.lowercase()) }
        email.substringBefore("@").split(".", "_", "-", "+")
            .filter { it.length >= 3 }.forEach { piiTokens.add(it.lowercase()) }
        val lowerPassword = password.lowercase()
        for (token in piiTokens) {
            if (lowerPassword.contains(token)) return "Password must not contain personally identifiable information"
        }
        return null
    }

    private fun validateFullName(name: String): String? {
        if (name.length < 2) return "Full name must be at least 2 characters"
        if (name.startsWith(" ")) return "Full name must not start with a space"
        if (name.endsWith(" ")) return "Full name must not end with a space"
        if (name.contains("  ")) return "Full name must not contain consecutive spaces"
        if (!Regex("^[-\\p{L} ]+$").matches(name)) return "Full name contains invalid characters"
        return null
    }

    private fun generateJwt(email: String, exp: Long): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = enc.encodeToString(
            """{"exp":$exp,"sub":"registration","email":"$email","aud":"sms-verification"}""".toByteArray()
        )
        val sig = enc.encodeToString("mock-signature".toByteArray())
        return "$header.$payload.$sig"
    }

    private fun sendError(
        exchange: HttpExchange, status: Int, code: String,
        field: String? = null, message: String
    ) {
        val json = buildString {
            append("""{"code":"$code","message":"$message"""")
            if (field != null) append(""","field":"$field"""")
            append("}")
        }
        sendRawResponse(exchange, status, json)
    }

    private fun sendRawResponse(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.add("X-Content-Type-Options", "nosniff")
        exchange.responseHeaders.add("Cache-Control", "no-store")
        exchange.responseHeaders.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
