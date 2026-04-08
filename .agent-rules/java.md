# Shared Java Coding Standards

## 1. Style & Structure (Gravitee Standard)

- **Version**: Target Java 21+.
- **Build**: Maven.
- **Format**: Google Java Style (enforced by Maven Prettier plugin).
- **Lombok**: Use for boilerplate (`@Data`, `@Builder`, `@CustomLog`).
- **Var**: Use `var` where type is obvious.
- **Architecture**: Hexagonal / Clean where applicable.
- **BOM**: Align with `gravitee-apim-bom`.

## 2. Exception Handling and Logging

### Logger Injection

1. Use `@CustomLog` to inject the appropriate logger into your classes.

### Logging Exceptions

1. **Log Once Rule (Centralization):** Log exceptions only once, typically at the highest-level `catch` block (e.g., the REST layer) to avoid **redundant logging** (e.g., logging at the repository, service, and REST layers simultaneously). This reduces log noise and eases troubleshooting by providing a single, complete context.
2. **Intermediary Logging Exceptions:** Logging at an intermediary level is acceptable only if:
    * The exception is **non-propagated** (caught and fully handled, not rethrown).
    * You need to add **additional context information**. In this case, **wrap the original exception** in a new one, including the new context, instead of logging the full stack trace redundantly.
3. **Contextual Log Messages:** Never log an exception with a generic message like `log.error("error", e)`. Always include context: what object, what operation, what identifier.

### Log Level Selection

1. **Error Level:** Use the **Error** level *only* when the exception indicates a **failure in the operation** (i.e., prevents an operation from completing successfully).
2. **Warn or Info Levels:** Use **Warn** or **Info** when exceptions are **expected** or **handled gracefully**. For instance, a user authentication exception resulting in a `401` response should *not* be logged as an Error, as it is an anticipated outcome, not a system failure.

### Throw Exceptions

1. **Preserve the Original Exception:** Always keep the **original exception** when throwing a new exception. This ensures that the root cause is preserved for logging and debugging.
2. **No Empty Catch Blocks:** A try-catch block must never be empty. If the exception is not rethrown, it must be logged.

### Diagnostic Reporting (Gateway Context)

When working with gateway or API flow control:

1. **Errors vs. Warnings:**
    * Use `ctx.interruptWith()` to report **errors that interrupt the request flow**.
    * Use `ctx.warnWith()` to report **warnings that do not interrupt the request flow**.
2. **Reporting Details:**
    * Always include `.cause(e)` when an exception is available.
    * Use **specific, actionable error keys** (e.g., `"CORS_PREFLIGHT_FAILED"`).
    * Provide **actionable messages with context** (e.g., `"Rate limit exceeded! You reached the limit of 10 requests per 1 minute"`).

Example:

```java
ctx.warnWith(
    new ExecutionWarn("RATE_LIMIT_NOT_APPLIED")
        .message("Request bypassed rate limit policy due to internal error")
        .cause(e)
);

ctx.interruptWith(
    new ExecutionFailure(502)
        .key("CORS_PREFLIGHT_FAILED")
        .cause(e)
);

ctx.warnWith(
    new ExecutionWarn("RATE_LIMIT_TOO_MANY_REQUESTS")
        .message("Rate limit exceeded! You reached the limit of 10 requests per 1 minute")
);
```

## 3. Build & Tooling

### Maven Multi-Module Builds

Run tests from the **parent** module so dependencies compile first:

```bash
mvn -f gravitee-apim-repository/pom.xml test -pl gravitee-apim-repository-elasticsearch -Dtest=... -am
```

Or build the full reactor; running `mvn test` on a child alone can fail with "cannot find symbol" for types from sibling modules.

### Formatting Before Tests

Run the Maven Prettier plugin on modified modules before tests — the build fails on formatting checks:

```bash
mvn prettier:write -pl <module>
```

## 4. Testing

- **Frameworks**: JUnit 5, AssertJ, Mockito, Testcontainers for integration tests.
- **Method names**: Must follow **snake_case** convention (e.g., `should_return_error_when_api_not_found()`).
- **Prefer real objects over mocks**: In clean/hexagonal architecture, instantiate real domain objects and in-memory adapters instead of mocking. Reserve mocks for external I/O boundaries (HTTP clients, databases) that are costly or impractical to instantiate.

## 5. Common Pitfalls

### JsonNode vs ObjectNode in Lists

**Problem**: `List<ObjectNode>` cannot be assigned to `List<JsonNode>` — Java generics are invariant.

```java
// ❌ BAD – compilation error: incompatible types
private static ObjectNode createBucket(String key, long docCount) { ... }
var buckets = List.of(createBucket("200", 1500L));
agg.setBuckets(buckets);  // List<ObjectNode> ≠ List<JsonNode>

// ✅ GOOD
private static JsonNode createBucket(String key, long docCount) { ... }
List<JsonNode> buckets = List.of(createBucket("200", 1500L));
agg.setBuckets(buckets);
```

When creating nodes for `Aggregation.setBuckets()`, return `JsonNode` (not `ObjectNode`) so the list type matches.
