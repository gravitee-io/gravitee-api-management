---
name: APIM Java Conventions
layer: local
description: APIM-specific Java conventions - formatter and per-module Maven commands, Lombok usage in this codebase, and the JsonNode pitfall
---

# APIM Java Conventions

- **Format before tests, with this exact command:** the build fails on the format check (Google Java Style via the Maven Prettier plugin). Run `mvn prettier:write -pl <module>` on every module you changed.
- **Test one module** with the two-step form — `-am test` would run tests on every upstream module too:

  ```bash
  mvn -pl <module> -am -DskipTests install
  mvn -pl <module> test
  ```

- **Lombok is established here:** this codebase already uses Lombok, including `@Data`, `@Builder`, and `@CustomLog`; follow the module's existing conventions. Inject loggers with `@CustomLog`, not `@Slf4j`.
- **JsonNode vs ObjectNode in lists:** `List<ObjectNode>` cannot be assigned to `List<JsonNode>` — Java generics are invariant. Helpers that feed `JsonNode` lists (for example `Aggregation.setBuckets()`) must return `JsonNode`, not `ObjectNode`.
