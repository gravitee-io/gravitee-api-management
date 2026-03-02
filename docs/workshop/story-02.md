# Story 02: Unified Analytics Endpoint — STATS

**Layer:** Backend
**Complexity:** M
**Dependencies:** Story 01 (shares endpoint and param infrastructure)

## Description

As an API Publisher, I want to query statistical aggregations (min/max/avg/sum) for a given metric field, so that I can display summary cards like average response time.

## Acceptance Criteria

- [ ] `GET /v2/apis/{apiId}/analytics?type=STATS&field=gateway-response-time-ms&from=...&to=...` returns `{ "type": "STATS", "count": N, "min": ..., "max": ..., "avg": ..., "sum": ... }`
- [ ] `field` is required when `type=STATS` (returns 400 if missing)
- [ ] Supported fields: `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`, `gateway-latency-ms`
- [ ] Unsupported fields return 400 with descriptive error
- [ ] Unit tests for each supported field

## Technical Notes

### Existing Code to Study

- **Legacy STATS handling:** `ApiAnalyticsResource.executeStats()` builds a `StatsQuery` with `field` set, delegates to `AnalyticsService`
- **Legacy response model:** `StatsAnalytics` has `count`, `min`, `max`, `avg`, `sum`, `rps`, `rpm`, `rph`
- **ES implementation:** Look at how the legacy `AnalyticsService` executes `StatsQuery` — it maps to an ES `stats` aggregation on the specified field

### Implementation Approach

1. Add STATS branch to the `switch(type)` dispatch in the unified endpoint (from Story 01)
2. Create a new use case that builds a stats query scoped to the API
3. Add a new ES query adapter for stats aggregation on v4-metrics index
4. Validate the `field` param against the allowlist

### Response Format

```json
{
  "type": "STATS",
  "count": 12345,
  "min": 2.0,
  "max": 1500.0,
  "avg": 125.5,
  "sum": 1549567.0
}
```
