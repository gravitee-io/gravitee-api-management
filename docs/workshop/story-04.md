# Story 04: Unified Analytics Endpoint — DATE_HISTO

**Layer:** Backend
**Complexity:** L
**Dependencies:** Story 01 (shares endpoint and param infrastructure)

## Description

As an API Publisher, I want to query time-bucketed histograms of a field, so that I can render line charts showing trends over time.

## Acceptance Criteria

- [ ] `GET /v2/apis/{apiId}/analytics?type=DATE_HISTO&field=status&from=...&to=...&interval=...` returns the DATE_HISTO response format
- [ ] `interval` is required when `type=DATE_HISTO` (returns 400 if missing)
- [ ] `interval` must be between 1000ms and 1,000,000,000ms
- [ ] Supported fields: `status`, `gateway-response-time-ms`, `endpoint-response-time-ms`
- [ ] Values array contains one entry per unique field value, each with a `buckets` array aligned to `timestamp`
- [ ] Unit tests for each supported field and interval validation

## Technical Notes

### Existing Code to Study

- **Legacy DATE_HISTO handling:** `ApiAnalyticsResource.executeDateHisto()` builds a `DateHistogramQuery` with `aggregations` list, delegates to `AnalyticsService` which returns `HistogramAnalytics`
- **Legacy response model:** `HistogramAnalytics` has `Timestamp timestamp` (from/to/interval) and `List<Bucket> values` where each `Bucket` has `field`, `name`, `data[]`, and `metadata`
- **Existing V2 overtime endpoints:** `SearchResponseStatusOverTimeUseCase` and `SearchResponseTimeUseCase` already build date histogram queries — study these for the v4-metrics index patterns
- **ES implementation:** Uses `date_histogram` aggregation on `@timestamp` with nested `terms` or `avg` aggregations

### Implementation Approach

1. Add DATE_HISTO branch to the unified endpoint dispatch
2. Create a new use case that:
   - Builds a date_histogram query with the specified interval and field
   - For `status` field: nested terms aggregation to get per-status-code buckets
   - For response time fields: nested avg aggregation to get average per time bucket
3. Adapt existing ES query adapters (e.g., `SearchResponseStatusOverTimeAdapter`, `ResponseTimeRangeQueryAdapter`) or create new ones
4. Validate interval bounds

### Response Format

```json
{
  "type": "DATE_HISTO",
  "timestamp": [1625000000000, 1625003600000, 1625007200000],
  "values": [
    {
      "field": "200",
      "buckets": [120, 130, 115],
      "metadata": { "name": "200" }
    },
    {
      "field": "500",
      "buckets": [2, 5, 1],
      "metadata": { "name": "500" }
    }
  ]
}
```
