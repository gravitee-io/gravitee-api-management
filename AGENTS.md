# Coding Guidelines for AI Agents

This file contains coding guidelines and best practices for the Gravitee API Management project.
It is intended to be read by any AI tool (code review, code generation, pair programming) to ensure consistency across contributions.

## Java Exception Handling and Logging

### Logger Injection

1. Use `@CustomLog` to inject the appropriate logger into your classes.
2. **Reactive Logging (Gateway & Plugins):** In the gateway and plugins (reactive code), prefer using `ctx.withLogger(log)` whenever possible to ensure proper logging context within the reactive stream.

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
// Always include .cause() when exception is available
ctx.warnWith(
    new ExecutionWarn("RATE_LIMIT_NOT_APPLIED")
        .message("Request bypassed rate limit policy due to internal error")
        .cause(e)
);

// Use specific, actionable error keys
ctx.interruptWith(
    new ExecutionFailure(502)
        .key("CORS_PREFLIGHT_FAILED")
        .cause(e)
);

// Provide actionable messages with context
ctx.warnWith(
    new ExecutionWarn("RATE_LIMIT_TOO_MANY_REQUESTS")
        .message("Rate limit exceeded! You reached the limit of 10 requests per 1 minute")
);
```

## Tests

- Test method names must follow **snake_case** convention (e.g., `should_return_error_when_api_not_found()`).
