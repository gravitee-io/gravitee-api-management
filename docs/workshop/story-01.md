# Story 01: Unified Analytics Endpoint — COUNT

**Layer:** Backend
**Complexity:** M
**Dependencies:** None

## Description

As an API Publisher, I want to query the total request count for my V4 API via a single unified endpoint, so that the frontend can use one consistent API for all analytics queries.

## Acceptance Criteria

- [ ] `GET /v2/apis/{apiId}/analytics?type=COUNT&from=...&to=...` returns `{ "type": "COUNT", "count": N }`
- [ ] Queries the `*-v4-metrics-*` Elasticsearch index
- [ ] Requires `API_ANALYTICS:READ` permission (returns 403 otherwise)
- [ ] Validates API is V4 and not TCP (returns appropriate error)
- [ ] `from` and `to` are required (returns 400 if missing)
- [ ] Unit tests for the use case and REST resource

## Technical Notes

### Existing Code to Study

- **Legacy unified endpoint (V1):** `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/src/main/java/io/gravitee/rest/api/management/rest/resource/ApiAnalyticsResource.java`
  - Uses `@BeanParam AnalyticsParam` with a `switch(type)` dispatch pattern
- **Current V2 resource:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`
  - Has separate endpoints per metric; this story replaces/supplements with a unified `GET /`
- **Existing COUNT use case:** `SearchRequestsCountAnalyticsUseCase` in `gravitee-apim-rest-api-service/.../analytics/use_case/`
- **ES repository:** `AnalyticsElasticsearchRepository` in `gravitee-apim-repository-elasticsearch/.../v4/analytics/`

### Implementation Approach

1. Create a new `@BeanParam` class (e.g., `AnalyticsParam`) with query params: `type` (enum: COUNT|STATS|GROUP_BY|DATE_HISTO), `from`, `to`, `field`, `interval`, `query`, `size`, `order`
2. Add a new `GET /` method on `ApiAnalyticsResource` that accepts the param bean and dispatches by `type`
3. For COUNT: create a new use case (or adapt `SearchRequestsCountAnalyticsUseCase`) that returns the simplified `{ type, count }` response
4. Follow the V2 pattern: `Input`/`Output` records, permission annotation, V4+non-TCP validation

### Response Format

```json
{
  "type": "COUNT",
  "count": 12345
}
```
