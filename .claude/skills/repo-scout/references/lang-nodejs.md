# Node.js / TypeScript Backend Patterns — Reference for /repo-scout

## Build Files

| File | Purpose |
|------|---------|
| `package.json` | Dependencies, scripts |
| `tsconfig.json` | TypeScript config |
| `nest-cli.json` | NestJS config |

## Route Registration Patterns

| Framework | Route Patterns |
|-----------|----------------|
| **Express** | `router.get(`, `router.post(`, `app.get(`, `app.post(` |
| **NestJS** | `@Controller(`, `@Get(`, `@Post(`, `@Put(`, `@Delete(` |
| **Fastify** | `fastify.get(`, `fastify.post(`, `app.route(` |
| **Koa** | `router.get(`, `router.post(` |

### Grep String for Route Search

```text
router\.get\(|router\.post\(|app\.get\(|app\.post\(|@Controller\(|@Get\(|@Post\(|fastify\.get\(
```

## Test Patterns

| Type | Indicators |
|------|------------|
| **Unit** | `*.test.ts` / `*.spec.ts`, imports: `jest`, `vitest`, `mocha` |
| **Integration** | `*.integration.spec.ts`, `supertest`, `testcontainers-node` |
| **E2E** | `*.e2e-spec.ts` (NestJS convention), Playwright/Cypress for API |

## Test Frameworks (package.json)

| Library | Purpose |
|---------|---------|
| `jest` / `vitest` | Test runner |
| `@nestjs/testing` | NestJS test module |
| `supertest` | HTTP integration testing |
| `testcontainers` | Docker-based integration |

## Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **Express** | `(req, res)` or `(req, res, next)` callback |
| **NestJS** | `@Controller` class with `@Get()` / `@Post()` methods |
| **Fastify** | `(request, reply)` handler or schema-based route |

## Error Patterns

| Pattern | Purpose |
|---------|---------|
| `throw new HttpException(` | NestJS HTTP exception |
| `next(err)` / `next(new Error(` | Express error forwarding |
| `res.status(N).json(` | Express response with status |
| `reply.code(N).send(` | Fastify response with status |
| `class * extends Error` | Custom error classes |

## Validation Patterns

| Pattern | Purpose |
|---------|---------|
| `Joi.object(` / `.required()` / `.min(` | Joi schema validation |
| `z.object(` / `z.string()` / `.parse(` | Zod schema validation |
| `@IsNotEmpty()` / `@IsEmail()` / `@MinLength(` | class-validator decorators (NestJS) |
| `@UsePipes(ValidationPipe)` | NestJS validation pipe |

## Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `passport.authenticate(` / `app.use(passport.` | Passport.js middleware |
| `@UseGuards(` / `JwtAuthGuard` / `AuthGuard(` | NestJS guard decorators |
| `@Roles(` / `@Permissions(` | NestJS RBAC decorators |
| `jwt.verify(` / `jsonwebtoken` | JWT validation |
| `req.headers.authorization` | Manual header extraction |
| `middleware(` near `jwt` or `auth` | Express middleware chain |

### Grep String for Auth/Middleware Search

```text
passport\.authenticate|UseGuards|JwtAuthGuard|AuthGuard|jwt\.verify|jsonwebtoken|headers\.authorization|@Roles\(|@Permissions\(
```

## Event Publishing Patterns

### Grep String for Event Publishing Search

```text
producer\.send\(|pubsub\.publish\(|eventEmitter\.emit\(|channel\.sendToQueue\(|client\.publish\(
```
