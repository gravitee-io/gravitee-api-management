---
name: APIM Java Conventions
layer: local
dirs:
  - gravitee-apim-common
  - gravitee-apim-definition
  - gravitee-apim-distribution
  - gravitee-apim-distribution/gravitee-apim-distribution-integration-tests
  - gravitee-apim-gateway
  - gravitee-apim-plugin
  - gravitee-apim-reporter
  - gravitee-apim-repository
  - gravitee-apim-rest-api
  - gravitee-gamma/gravitee-gamma-module-apim
  - gravitee-gamma/gravitee-gamma-module-platform
  - gravitee-gamma/gravitee-gamma-rest-api
description: APIM-specific Java conventions - the formatter command and Lombok usage in this codebase
---

# APIM Java Conventions

- **Formatter:** Google Java Style via the Maven Prettier plugin — run `mvn prettier:write -pl <module>` on every module you changed.
- **Lombok — the shared Java rule's "already conventional" condition is met here:** this codebase uses `@Data`, `@Builder`, and `@CustomLog`; follow the module's existing conventions and inject loggers with `@CustomLog`, not `@Slf4j`.
- **Test method names are snake_case** — `should_return_error_when_api_not_found()` — the shared Java rule's "follow the repo's existing test naming style" resolved.
- **Gateway diagnostics:** in reactive Gateway and plugin code, the `ctx.interruptWith()` / `ctx.warnWith()` builder patterns are copy-ready in `.ai/guides/gateway-diagnostics.md` (a repository-root path).
