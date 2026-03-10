> Reference file for /api-test-review. Quick grep scan patterns.

## Initial Reconnaissance (Concrete Search Patterns)

Before analyzing code deeply, use these quick grep patterns to locate potential issues:

```bash
# Security scan
grep -rn -E "(bearer\s+[a-zA-Z0-9]+|\"secret\"|\"password\"|api[_-]?key)" --include="*.kt" --include="*.java" src/test

# Blurry HTTP assertions
grep -rn -E "(statusCode|status).*(<|>|in.*\\.\\.|between)" --include="*.kt" --include="*.java" src/test
grep -rn "\.then()\.statusCode" --include="*.kt" --include="*.java" src/test  # RestAssured anti-pattern

# Blocking delays
grep -rn "Thread\.sleep|delay(" --include="*.kt" --include="*.java" src/test

# Missing cleanup
grep -rn "@Test" --include="*.kt" --include="*.java" src/test | wc -l  # Count tests
grep -rn "@AfterEach" --include="*.kt" --include="*.java" src/test | wc -l  # Count cleanup

# Inline DTOs
grep -rn "data class.*Request\|data class.*Response" --include="*.kt" src/test/**/tests/  # Inside test classes

# Missing Allure steps
grep -rn "@Test\|fun test" --include="*.kt" --include="*.java" src/test | wc -l  # Total tests
grep -rn "@Step\|step(" --include="*.kt" --include="*.java" src/test | wc -l  # Instrumented tests
```
