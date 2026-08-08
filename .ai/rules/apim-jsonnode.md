---
name: APIM JsonNode Lists
layer: local
dirs:
  - gravitee-apim-rest-api
  - gravitee-apim-repository
description: The JsonNode vs ObjectNode generics-invariance pitfall in the analytics aggregation helpers
---

# APIM JsonNode Lists

- `List<ObjectNode>` cannot be assigned to `List<JsonNode>` — Java generics are invariant. Helpers that feed `JsonNode` lists (for example `Aggregation.setBuckets()`) must return `JsonNode`, not `ObjectNode`.
