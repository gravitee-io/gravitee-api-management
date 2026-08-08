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
