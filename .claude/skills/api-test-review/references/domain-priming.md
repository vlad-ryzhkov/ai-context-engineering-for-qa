> Reference file for /api-test-review. Domain terminology activation.

## Target Domain Priming (Kotlin/Java Test Stack)

Activate knowledge for these domain-specific concepts to ensure comprehensive code review:

**Kotlin Testing Ecosystem:** `runTest`, `TestCoroutineScheduler`, `advanceUntilIdle`, `@JvmStatic`, `Companion object` fixtures, `suspend` test functions, `launch`, `async`, coroutine builders, `coroutineScope`

**HTTP Client & Assertion Patterns:** `ktor-client` (CIO engine), `jackson-module-kotlin` (SNAKE_CASE serialization), JUnit 5 Assertions (`assertEquals`, `assertTrue`, `assertNotNull` — primary for /api-tests output), `Kotest Assertions` (`.shouldBe()`, `.should()` — only if detected in build file), `RestAssured` (anti-pattern awareness: REST DSL, `.then()` chains — BANNED in Ktor projects), `java.net.http.HttpClient` (Java mode only), `WebTestClient`, `MockMvc`

**Async/Concurrency Antipatterns:** `Thread.sleep()` (blocking), `delay()` (in non-suspend context), `Awaitility`, `CountDownLatch`, `Thread.join()`, race conditions

**Architecture & Package Organization:** DTO isolation (`requests/` and `helpers/` packages), test data builders (`*Fixture`, `*Builder`, `*Factory`), test lifecycle (`@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`), resource cleanup (`.use {}`, AutoCloseable)

**Allure Integration:** `@Step` annotations, `@AllureId`, `@DisplayName`, `@Description`, `attachment()` API, test lifecycle reporting

**Mock & Stub Patterns:** `WireMock` (stubs, matchers), `MockK` (mocking, `every`, `coEvery` for suspend), `@MockBean`, `@WebMvcTest` (Spring context)

**Data Security in Tests:** Safe test data (`test@example.com`, faker libraries), environment variable injection, credential fixtures, no secrets in code
