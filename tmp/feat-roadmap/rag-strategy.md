# RAG Context Strategy

## Approach

Light RAG via direct queries against existing APIM domain query services. No vector search for v0.1 — the context is deterministic lookups of existing resources, not semantic similarity.

## Strategy Pattern

Each resource type has its own `RagContextStrategy` implementation that knows what context is most useful for generating that type of resource.

```java
public interface RagContextStrategy {
    ZeeResourceType resourceType();
    String retrieveContext(String envId, String orgId, Map<String, Object> contextData);
}
```

## Per-Resource Strategies

### FlowRagContextAdapter (Phase 1 — first implementation)

**Retrieves**:

- Existing flows for the current API (top 5 most recently modified)
- Available policy plugins with their descriptions
- API listener configuration (to know path patterns)

**Domain services used**:

- `FlowQueryService` or equivalent (existing APIM service)
- Plugin registry for available policies
- `ApiQueryService` for API metadata

```java
@Component
public class FlowRagContextAdapter implements RagContextStrategy {
    private final FlowQueryService flowQueryService;
    private final PolicyQueryService policyQueryService;
    private final ApiQueryService apiQueryService;

    @Override
    public ZeeResourceType resourceType() { return ZeeResourceType.FLOW; }

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        var apiId = (String) contextData.get("apiId");
        var sb = new StringBuilder();

        // Existing flows (max 5)
        var flows = flowQueryService.findByApi(envId, apiId);
        if (!flows.isEmpty()) {
            sb.append("### Existing Flows\n");
            flows.stream().limit(5).forEach(f ->
                sb.append("- ").append(f.getName()).append(": ").append(f.getDescription()).append("\n"));
        }

        // Available policies
        var policies = policyQueryService.findAvailable(envId);
        sb.append("\n### Available Policies\n");
        policies.forEach(p ->
            sb.append("- ").append(p.getId()).append(": ").append(p.getDescription()).append("\n"));

        return sb.toString();
    }
}
```

### PlanRagContextAdapter (Phase 3)

**Retrieves**: Existing plans, security types, API subscription model

### EndpointRagContextAdapter (Phase 3)

**Retrieves**: Existing endpoint groups, connector types, backend URLs

### ApiRagContextAdapter (Phase 3)

**Retrieves**: Organization's existing APIs (names + descriptions), common patterns

## Important Rules

1. **All queries MUST be tenant-scoped** — use `envId` and `orgId` to scope queries. Never return data from other tenants.
2. **Use existing domain query services** — never write raw MongoDB queries. This follows the Onion Architecture and ensures permission checks are applied.
3. **Hard limit on results** — max 5 items per category to prevent prompt window blowout.
4. **Graceful fallback** — if a strategy fails or finds no context, return empty string. Never block generation.
5. **NoOpRagStrategy** — default for resource types without a specific strategy. Returns empty context.
