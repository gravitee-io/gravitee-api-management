---
name: APIM Java Conventions
layer: local
description: APIM Java style, build, testing, and common pitfalls
---

> Root-scoped: Java work spans eleven top-level Maven modules that keep their own
> hand-written AGENTS.md, so the generated root file is currently the only shared vehicle
> for these conventions. Revisit when the Maven modules are migrated.

# APIM Java Conventions

## Style & Structure

- **Version**: Target Java 21+.
- **Build**: Maven.
- **Format**: Google Java Style (enforced by Maven Prettier plugin).
- **Lombok**: Use for boilerplate (`@Data`, `@Builder`, `@CustomLog`).
- **Var**: Use `var` where type is obvious.
- **Architecture**: Hexagonal / Clean where applicable.
- **BOM**: Align with `gravitee-apim-bom`.

## Build & Tooling

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

## Testing

- **Frameworks**: JUnit 5, AssertJ, Mockito, Testcontainers for integration tests.
- **Method names**: Must follow **snake_case** convention (e.g., `should_return_error_when_api_not_found()`).
- **Prefer real objects over mocks**: In clean/hexagonal architecture, instantiate real domain objects and in-memory adapters instead of mocking. Reserve mocks for external I/O boundaries (HTTP clients, databases) that are costly or impractical to instantiate.

## Common Pitfalls

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
