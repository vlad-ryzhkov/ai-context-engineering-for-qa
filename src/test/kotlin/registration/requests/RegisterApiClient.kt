package registration.requests

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson

object RegisterApiClient {

    val BASE_URL: String get() = System.getProperty("BASE_URL", "http://localhost:8080")

    val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

    val httpClient: HttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    registerKotlinModule()
                }
            }
            install(Logging) {
                level = LogLevel.ALL
            }
            expectSuccess = false
        }
    }

    suspend fun register(
        request: RegisterRequest,
        idempotencyKey: String? = null,
    ): HttpResponse = httpClient.post("$BASE_URL/api/v1/users/register") {
        contentType(ContentType.Application.Json)
        setBody(request)
        applyIdempotencyKey(idempotencyKey)
    }

    suspend fun registerRaw(
        body: String,
        idempotencyKey: String? = null,
    ): HttpResponse = httpClient.post("$BASE_URL/api/v1/users/register") {
        contentType(ContentType.Application.Json)
        setBody(body)
        applyIdempotencyKey(idempotencyKey)
    }

    private fun HttpRequestBuilder.applyIdempotencyKey(key: String?) {
        if (key != null) header("Idempotency-Key", key)
    }
}
