# Python Backend Patterns — Reference for /repo-scout

## Build Files

| File | Purpose |
|------|---------|
| `requirements.txt` / `pyproject.toml` | Dependencies |
| `setup.py` / `setup.cfg` | Package metadata |
| `Makefile` / `tox.ini` | Build/test runners |

## Route Registration Patterns

| Framework | Route Patterns |
|-----------|----------------|
| **FastAPI** | `@app.get(`, `@app.post(`, `@router.get(`, `@router.post(` |
| **Flask** | `@app.route(`, `@bp.route(` |
| **Django** | `path(`, `re_path(`, `url(` in `urls.py` |
| **Starlette** | `Route(`, `routes=[` |

### Grep String for Route Search

```text
@app\.get\(|@app\.post\(|@router\.get\(|@router\.post\(|@app\.route\(|@bp\.route\(|path\(|re_path\(
```

## Test Patterns

| Type | Indicators |
|------|------------|
| **Unit** | `test_*.py` / `*_test.py`, imports: `pytest`, `unittest` |
| **Integration** | `@pytest.mark.integration`, `TestCase` with DB/Docker |
| **E2E/API** | Separate test repo, or `tests/e2e/` |

## Test Frameworks (requirements.txt)

| Library | Purpose |
|---------|---------|
| `pytest` | Test runner + assertions |
| `pytest-asyncio` | Async test support |
| `pytest-mock` / `unittest.mock` | Mocking |
| `httpx` / `requests` | HTTP client for API tests |
| `testcontainers` | Docker-based integration |

## Handler Patterns

| Type | Signature / Pattern |
|------|-------------------|
| **FastAPI** | `@app.get("/path")` / `@router.post("/path")` decorated `async def func_name(...)` |
| **Flask** | `@app.route("/path", methods=["GET"])` decorated `def func_name()` |
| **Django** | `def view_name(request)` in `views.py`, class-based `class ViewName(APIView)` |

## Error Patterns

| Pattern | Purpose |
|---------|---------|
| `HTTPException(status_code=` | FastAPI/Starlette HTTP error |
| `raise` + custom exception class | Custom error raising |
| `abort(` | Flask error response |
| `Response(status=` | DRF/Django response with status |

## Validation Patterns

| Pattern | Purpose |
|---------|---------|
| `Field(` / `field_validator` / `model_validator` | Pydantic v2 validation |
| `@validator` / `@root_validator` | Pydantic v1 validation |
| `serializers.CharField(` / `validators=[` | DRF serializer validation |
| `wtforms` / `Form` | Flask-WTF form validation |

## Auth / Middleware Patterns

| Pattern | Purpose |
|---------|---------|
| `Depends(` / `Security(` / `HTTPBearer(` | FastAPI dependency injection auth |
| `OAuth2PasswordBearer` / `OAuth2AuthorizationCodeBearer` | FastAPI OAuth2 scheme |
| `@login_required` / `permission_classes` / `authentication_classes` | Django REST auth |
| `@jwt_required` / `verify_jwt_in_request(` | Flask-JWT / Flask-JWT-Extended |
| `request.headers.get("Authorization"` | Manual header extraction |
| `token.split("Bearer ")` | Bearer token parsing |

### Grep String for Auth/Middleware Search

```text
Depends\(|Security\(|HTTPBearer|OAuth2|login_required|permission_classes|jwt_required|verify_jwt|Authorization|Bearer
```

## Event Publishing Patterns

### Grep String for Event Publishing Search

```text
producer\.send\(|publisher\.publish\(|basic_publish|redis\.publish\(|emit\(.*event
```
