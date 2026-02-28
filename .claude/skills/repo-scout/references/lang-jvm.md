# Java / Kotlin Backend Patterns — Reference for /repo-scout

## Build Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies |
| `build.gradle` / `build.gradle.kts` | Gradle dependencies |
| `settings.gradle.kts` | Multi-module config |

## Route Registration Patterns

| Framework | Route Patterns |
|-----------|----------------|
| **Spring MVC** | `@GetMapping(`, `@PostMapping(`, `@PutMapping(`, `@DeleteMapping(`, `@RequestMapping(` |
| **Spring WebFlux** | `RouterFunction`, `route().GET(`, `route().POST(` |
| **Ktor** | `routing {`, `get(`, `post(`, `put(`, `delete(` |
| **Quarkus** | `@Path(`, `@GET`, `@POST`, `@PUT`, `@DELETE` |

### Grep String for Route Search

```text
@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@RequestMapping|@Path\(|routing \{|route\(\)\.GET
```

## Test Patterns

| Type | Indicators |
|------|------------|
| **Unit** | `*Test.java` / `*Test.kt` / `*Tests.kt`, `@Test` (JUnit 5) |
| **Integration** | `@SpringBootTest`, `@DataJpaTest`, `@Testcontainers` |
| **E2E/API** | `@AutoConfigureMockMvc`, separate test module |

## Test Frameworks (pom.xml / build.gradle)

| Library | Purpose |
|---------|---------|
| `junit-jupiter` | JUnit 5 test runner |
| `mockito-kotlin` / `mockk` | Mocking |
| `testcontainers` | Docker-based integration |
| `spring-boot-test` | Spring context testing |
| `kotest` | Kotlin assertion/test framework |

## Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **Spring MVC** | `@RestController` class with `@GetMapping` / `@PostMapping` methods |
| **Spring WebFlux** | `RouterFunction<ServerResponse>` or annotated controller with `Mono`/`Flux` |
| **Ktor** | `routing { get("/path") { ... } }` blocks |
| **Quarkus** | `@Path` class with `@GET` / `@POST` methods |

## Error Patterns

| Pattern | Purpose |
|---------|---------|
| `@ExceptionHandler` / `@ControllerAdvice` | Spring global error handling |
| `ResponseStatusException(` | Spring HTTP error |
| `throw` + custom exception | Custom exception throwing |
| `StatusPages` / `respondText(status =` | Ktor error handling |

## Validation Patterns

| Pattern | Purpose |
|---------|---------|
| `@Valid` / `@Validated` | Spring Bean Validation trigger |
| `@NotNull` / `@NotBlank` / `@Size(` / `@Pattern(` | Bean Validation (JSR 380) annotations |
| `@field:NotNull` / `@get:Size(` | Kotlin annotation use-site targets |
| `ConstraintValidator<` | Custom validator implementation |

## Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `@PreAuthorize(` / `@Secured(` / `@RolesAllowed(` | Spring Security method security |
| `SecurityContextHolder` / `Authentication` | Spring Security context |
| `JwtDecoder` / `JwtAuthenticationConverter` | Spring JWT support |
| `HttpSecurity` / `.authorizeRequests(` / `.antMatchers(` | Spring Security HTTP config |
| `ServerInterceptor` / `GrpcAuthenticationReader` | gRPC-Spring interceptor |
| `request.getHeader("Authorization"` | Manual header extraction |

### Grep String for Auth/Middleware Search

```text
@PreAuthorize|@Secured|@RolesAllowed|SecurityContextHolder|JwtDecoder|HttpSecurity|authorizeRequests|antMatchers|ServerInterceptor|Authorization
```
