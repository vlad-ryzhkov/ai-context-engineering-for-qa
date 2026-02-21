package registration.requests

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.ktor.client.request.delete
import io.ktor.http.content.TextContent

object RegisterApiClient {

    private const val BASE_URL = "http://localhost:8080"

    val client: HttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
                }
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            expectSuccess = false
        }
    }

    suspend fun registerUser(
        request: RegisterRequest,
        idempotencyKey: String? = null
    ): HttpResponse {
        return client.post("$BASE_URL/api/v1/users/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
            idempotencyKey?.let { header("Idempotency-Key", it) }
        }
    }

    suspend fun registerUserRawJson(rawJson: String): HttpResponse {
        return client.post("$BASE_URL/api/v1/users/register") {
            setBody(TextContent(rawJson, ContentType.Application.Json))
        }
    }

    suspend fun deleteUser(uuid: String): HttpResponse {
        return client.delete("$BASE_URL/api/v1/users/$uuid")
    }
}
