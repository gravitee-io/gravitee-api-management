# Gateway diagnostics patterns

Depth behind the **Operator-visible Gateway behaviour** section of the root `AGENTS.md`, which carries the rules (when to interrupt vs warn, `.cause(e)`, actionable keys and messages). Use these in reactive Gateway and plugin code; the builder chains below are the shapes to copy.

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
