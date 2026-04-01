# Story 03: Unified Analytics Endpoint — GROUP_BY

**Layer:** Backend
**Complexity:** L
**Dependencies:** Story 01 (shares endpoint and param infrastructure)

## Description

As an API Publisher, I want to query top-N aggregations grouped by a field (status, application, plan), so that I can see status distributions and top consumers.

## Acceptance Criteria

- [ ] `GET /v2/apis/{apiId}/analytics?type=GROUP_BY&field=status&from=...&to=...` returns `{ "type": "GROUP_BY", "values": {"200": N, ...}, "metadata": {...} }`
- [ ] `field` is required (returns 400 if missing)
- [ ] Supported fields: `status`, `mapped-status`, `application`, `plan`, `host`, `uri`
- [ ] `size` param controls top-N (default 10)
- [ ] `order` param controls sort direction
- [ ] `metadata` includes resolved names for `application` and `plan` fields (looked up from their respective services)
- [ ] Unsupported fields return 400 with descriptive error
- [ ] Unit tests for each supported field, metadata resolution, and size/order params

## Technical Notes

### Existing Code to Study

- **Legacy GROUP_BY handling:** `ApiAnalyticsResource.executeGroupBy()` builds a `GroupByQuery` with `field`, `order`, `ranges`, delegates to `AnalyticsService` which returns `TopHitsAnalytics`
- **Legacy response model:** `TopHitsAnalytics` has `Map<String,Long> values` and `Map<String,Map<String,String>> metadata`
- **Metadata resolution:** The legacy `AnalyticsService` resolves application/plan names by looking up IDs from the values map. Study `AnalyticsServiceImpl` for this pattern.
- **ES implementation:** Uses `terms` aggregation on the specified field

### Implementation Approach

1. Add GROUP_BY branch to the unified endpoint dispatch
2. Create a new use case that:
   - Builds a terms aggregation query scoped to the API
   - Executes via `AnalyticsQueryService` / ES repository
   - For `application` and `plan` fields: resolves IDs to display names via `ApplicationService` / `PlanService` and populates `metadata`
3. Add a new ES query adapter for terms aggregation on v4-metrics index
4. Support `size` and `order` params in the ES query

### Response Format

```json
{
  "type": "GROUP_BY",
  "values": {
    "200": 10000,
    "404": 1500,
    "500": 845
  },
  "metadata": {
    "200": { "name": "200" },
    "404": { "name": "404" },
    "500": { "name": "500" }
  }
}
```

For `application` field:
```json
{
  "type": "GROUP_BY",
  "values": {
    "app-uuid-1": 5000,
    "app-uuid-2": 3000
  },
  "metadata": {
    "app-uuid-1": { "name": "My Mobile App" },
    "app-uuid-2": { "name": "Partner Integration" }
  }
}
```
