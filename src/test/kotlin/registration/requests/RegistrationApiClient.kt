package registration.requests

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runTest
import kotlinx.serialization.json.Json

class HttpResponseWrapper<T>(
    val code: Int,
    val headers: Map<String, String>,
    val body: T,
    val rawResponse: String = ""
)

object RegistrationApiClient {
    private val client: HttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
            install(Logging) {
                logger = io.ktor.client.plugins.logging.Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
    }

    suspend fun registerUserAsync(
        request: RegisterRequest,
        idempotencyKey: String? = null
    ): HttpResponseWrapper<RegisterResponse> {
        return try {
            val response = client.post(TestConfig.BASE_URL + Endpoints.REGISTER) {
                contentType(ContentType.Application.Json)
                setBody(request)
                if (idempotencyKey != null) {
                    header("Idempotency-Key", idempotencyKey)
                }
            }

            val headers = response.headers.toMap().mapValues { (_, v) -> v.first() }
            val statusCode = response.status.value
            val body = try {
                Json.decodeFromString<RegisterResponse>(response.toString())
            } catch (e: Exception) {
                RegisterResponse()
            }

            HttpResponseWrapper(
                code = statusCode,
                headers = headers,
                body = body
            )
        } catch (e: Exception) {
            HttpResponseWrapper(
                code = 0,
                headers = emptyMap(),
                body = RegisterResponse(),
                rawResponse = e.message ?: ""
            )
        }
    }

    suspend fun registerUserExpectErrorAsync(
        request: RegisterRequest,
        idempotencyKey: String? = null
    ): HttpResponseWrapper<ErrorResponse> {
        return try {
            val response = client.post(TestConfig.BASE_URL + Endpoints.REGISTER) {
                contentType(ContentType.Application.Json)
                setBody(request)
                if (idempotencyKey != null) {
                    header("Idempotency-Key", idempotencyKey)
                }
            }

            val headers = response.headers.toMap().mapValues { (_, v) -> v.first() }
            val statusCode = response.status.value
            val body = try {
                Json.decodeFromString<ErrorResponse>(response.toString())
            } catch (e: Exception) {
                ErrorResponse()
            }

            HttpResponseWrapper(
                code = statusCode,
                headers = headers,
                body = body
            )
        } catch (e: Exception) {
            HttpResponseWrapper(
                code = 0,
                headers = emptyMap(),
                body = ErrorResponse(),
                rawResponse = e.message ?: ""
            )
        }
    }
}

val apiClient = RegistrationApiClient
