---
name: APIM Java Conventions
layer: local
description: APIM-specific Java conventions - formatter and per-module Maven commands, Lombok usage in this codebase, and the JsonNode pitfall
---

# APIM Java Conventions

- **Formatter:** Google Java Style via the Maven Prettier plugin — run `mvn prettier:write -pl <module>` on every module you changed.
- **Lombok — the shared Java rule's "already conventional" condition is met here:** this codebase uses `@Data`, `@Builder`, and `@CustomLog`; follow the module's existing conventions and inject loggers with `@CustomLog`, not `@Slf4j`.
- **JsonNode vs ObjectNode in lists:** `List<ObjectNode>` cannot be assigned to `List<JsonNode>` — Java generics are invariant. Helpers that feed `JsonNode` lists (for example `Aggregation.setBuckets()`) must return `JsonNode`, not `ObjectNode`.
