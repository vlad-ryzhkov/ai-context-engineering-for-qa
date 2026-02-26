# Anti-Pattern: Not Testing Batch Partial Failure

## Problem

Batch endpoint tests only cover two extremes: all items valid (expect 200) and all items
invalid (expect 400). They never test the mixed case — some valid, some invalid — which is
where the most dangerous bugs hide: silent data loss, partial commits without rollback,
or incorrect error propagation.

## Bad Example

```kotlin
// ❌ BAD: only tests all-valid and all-invalid
@Test
fun `batch create - all valid`() = runTest {
    val items = listOf(validItem1, validItem2, validItem3)
    val response = apiClient.batchCreate(items)
    assertEquals(HttpStatusCode.OK, response.status)
}

@Test
fun `batch create - all invalid`() = runTest {
    val items = listOf(invalidItem1, invalidItem2)
    val response = apiClient.batchCreate(items)
    assertEquals(HttpStatusCode.BadRequest, response.status)
}
// Missing: mixed valid + invalid items
```

## Good Example

```kotlin
// ✅ GOOD: tests mixed batch to verify error propagation strategy
@Test
fun `batch create - mixed valid and invalid items returns partial result`() = runTest {
    val items = listOf(validItem1, invalidItem2, validItem3)
    val response = apiClient.batchCreate(items)

    // Strategy A: API returns 207 Multi-Status with per-item results
    assertEquals(HttpStatusCode.MultiStatus, response.status,
        "Mixed batch should return 207 Multi-Status")
    val body = response.body<BatchResponse>()
    assertEquals(2, body.succeeded.size, "Valid items should succeed")
    assertEquals(1, body.failed.size, "Invalid items should fail with details")
    assertEquals("VALIDATION_ERROR", body.failed[0].error.code,
        "Failed item should carry specific error code")

    // Strategy B: API rejects entire batch (atomic)
    // assertEquals(HttpStatusCode.BadRequest, response.status,
    //     "Atomic batch should reject entire request on any invalid item")
    // Verify NO items were created (rollback)
    // val allItems = apiClient.listItems()
    // assertEquals(0, allItems.body<ListResponse>().items.size,
    //     "Atomic batch rollback: no items should persist")
}

@Test
fun `batch create - verify error details per failed item`() = runTest {
    val items = listOf(
        validItem,
        itemWithInvalidField,
        itemWithMissingRequired,
    )
    val response = apiClient.batchCreate(items)
    val body = response.body<BatchResponse>()

    body.failed.forEach { failure ->
        assertNotNull(failure.index, "Failed item must reference its position in the batch")
        assertNotNull(failure.error.code, "Failed item must have an error code")
        assertTrue(failure.error.message.isNotBlank(),
            "Failed item at index ${failure.index} must have error message")
    }
}
```

## Why

- Batch APIs have two common strategies: atomic (all-or-nothing) and partial (per-item result)
- Without testing mixed input, you don't know which strategy the API implements
- Silent data loss: valid items succeed, invalid items silently drop — no error returned
- Partial commit without rollback: some items persist, others fail, leaving inconsistent state
- Error propagation: does the API return per-item error details or just a generic 400?

## Detection

```bash
grep -rn "batch\|Batch\|bulk\|Bulk" src/test/kotlin/ | grep -i "test\|spec"
```

Check: do batch test files contain tests with mixed valid/invalid input? If only `allValid` and `allInvalid` test names — this anti-pattern applies.

## References

- (ref: api/batch-partial-failure.md)
- Related: `common/no-cleanup-pattern.md`
- Related: `api/missing-business-error-assertion.md`
