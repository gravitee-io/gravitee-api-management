## Guidelines for Java Exception Handling and Logging (Based on style.md)

When discussing Java exception handling, logging, or API Gateway diagnostics, adhere to the following best practices:

### Logging

1.  **Logger Injection:** Use `@CustomLog` to inject the appropriate logger into your classes.
2.  **Reactive Logging (Gateway & Plugins):** In the gateway and plugins (reactive code), prefer using `ctx.withLogger(log)` whenever possible to ensure proper logging context within the reactive stream.

### Logging Exceptions

1.  **Log Once Rule (Centralization):** Log exceptions only once, typically at the highest-level `catch` block (e.g., the REST layer) to avoid **redundant logging** (e.g., logging at the repository, service, and REST layers simultaneously). This reduces log noise and eases troubleshooting by providing a single, complete context.
2.  **Intermediary Logging Exceptions:** Logging at an intermediary level is acceptable only if:
    * The exception is **Non-propagated** (caught and fully handled, not rethrown).
    * You need to add **Additional Context Information**. In this case, **wrap the original exception** in a new one, including the new context, instead of logging the full stack trace redundantly.

### Log Level Selection

1.  **Error Level:** Use the **Error** level *only* when the exception indicates a **failure in the operation** (i.e., prevents an operation from completing successfully).
2.  **Warn or Info Levels:** Use **Warn** or **Info** when exceptions are **expected** or **handled gracefully**. For instance, a user authentication exception resulting in a `401` response should *not* be logged as an Error, as it is an anticipated outcome, not a system failure.

### Throw Exceptions

1.  **Throw Exceptions:** Always keep the **original exception** when throwing a new exception. This ensures that the original exception is logged and that the new exception is not lost.
2.  **Catch Exceptions:** A try-catch block shouldn’t be empty. If the exception is not handled, it should be caught and logged.

### Diagnostic Reporting (Gateway Context)

When discussing gateway or API flow control:

1.  **Errors vs. Warnings:**
    * Use `ctx.interruptWith()` to report **errors that interrupt the request flow**.
    * Use `ctx.warnWith()` to report **warnings that do not interrupt the request flow**.
2.  **Reporting Details:**
    * Always include `.cause(e)` when an exception is available.
    * Use **specific, actionable error keys** (e.g., `"CORS_PREFLIGHT_FAILED"`).
    * Provide **actionable messages with context** (e.g., `"Rate limit exceeded! You reached the limit of 10 requests per 1 minutes"`).

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
        .message("Rate limit exceeded! You reached the limit of 10 requests per 1 minutes")
);
```

## Tests

- Tests methods names should follow snake case.
