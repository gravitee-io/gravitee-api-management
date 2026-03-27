# V4 API Analytics Dashboard

## Overview

This feature adds a unified analytics endpoint and enhanced dashboard for V4 APIs in the Gravitee Console. It replaces multiple legacy per-metric endpoints with a single, flexible `GET /apis/{apiId}/analytics` endpoint supporting four query types.

## API Endpoint

```
GET /v2/apis/{apiId}/analytics?type={TYPE}&from={from}&to={to}[&field=...][&interval=...][&size=...]
```

### Query Types

| Type | Required Params | Description |
|------|----------------|-------------|
| `COUNT` | `from`, `to` | Total request count for the time range |
| `STATS` | `from`, `to`, `field` | Statistical aggregation (avg, min, max, sum, count) |
| `GROUP_BY` | `from`, `to`, `field` | Top-N grouping by field value |
| `DATE_HISTO` | `from`, `to`, `field`, `interval` | Time-series histogram with nested grouping |

### Supported Fields

`status`, `mapped-status`, `application`, `plan`, `host`, `uri`, `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`

### Response Examples

```json
// COUNT
{ "type": "COUNT", "count": 1234 }

// STATS
{ "type": "STATS", "avg": 150.5, "min": 10, "max": 500, "sum": 1505, "count": 10 }

// GROUP_BY
{ "type": "GROUP_BY", "values": { "200": 100, "404": 5, "500": 2 } }

// DATE_HISTO
{ "type": "DATE_HISTO", "timestamps": [1000, 2000, 3000], "values": { "200": [10, 20, 30] } }
```

## Dashboard Widgets

| Widget | Data Source | Description |
|--------|-----------|-------------|
| Total Requests | `COUNT` | Total request count for selected time range |
| Avg Gateway Response Time | `STATS` on `gateway-response-time-ms` | Average gateway-level response time |
| Avg Upstream Response Time | `STATS` on `endpoint-response-time-ms` | Average upstream/backend response time |
| Avg Content Length | `STATS` on `request-content-length` | Average request payload size |
| HTTP Status Pie Chart | `GROUP_BY` on `status` | Distribution of HTTP status codes (color-coded: 2xx green, 3xx blue, 4xx orange, 5xx red) |

## Architecture

```
Frontend (Angular)                    Backend (Java)
┌─────────────────────┐     ┌──────────────────────────────────┐
│ ApiAnalyticsV2      │     │ ApiAnalyticsResource             │
│   .getCount()       │────>│   → SearchAnalyticsCountUseCase  │
│   .getStats()       │────>│   → SearchAnalyticsStatsUseCase  │
│   .getGroupBy()     │────>│   → SearchAnalyticsGroupByUseCase│
│   .getDateHisto()   │────>│   → SearchAnalyticsDateHistoUC   │
└─────────────────────┘     └──────────┬───────────────────────┘
                                       │
                            ┌──────────▼───────────────────────┐
                            │ AnalyticsQueryService            │
                            │   → AnalyticsRepository (ES)     │
                            └──────────────────────────────────┘
```

## Files Changed

### Backend — Repository Layer
- `DateHistogramQuery.java`, `DateHistogramAggregate.java`, `GroupByQuery.java`, `GroupByAggregate.java`, `StatsQuery.java`, `StatsAggregate.java` — Repository models
- `AnalyticsRepository.java` — Interface with `searchStats()`, `searchGroupBy()`, `searchDateHistogram()`
- `SearchStatsAdapter.java`, `SearchGroupByAdapter.java`, `SearchDateHistogramAdapter.java` — ES query builders
- `AnalyticsElasticsearchRepository.java` — ES implementations

### Backend — Service & Use Case Layer
- `StatsResult.java`, `GroupByResult.java`, `DateHistogramResult.java` — Domain models
- `AnalyticsQueryService.java` + `Impl` — Service layer
- `SearchAnalyticsCountUseCase.java`, `SearchAnalyticsStatsUseCase.java`, `SearchAnalyticsGroupByUseCase.java`, `SearchAnalyticsDateHistoUseCase.java` — Use cases

### Backend — REST API
- `ApiAnalyticsResource.java` — Unified endpoint dispatcher

### Frontend — Service & Types
- `analyticsCount.ts`, `analyticsStats.ts`, `analyticsGroupBy.ts`, `analyticsDateHisto.ts` — TypeScript interfaces + fixtures
- `api-analytics-v2.service.ts` — 4 new methods

### Frontend — Dashboard
- `api-analytics-proxy.component.ts` — 4 stats cards + pie chart integration
- `api-analytics-proxy.component.html` — Updated layout with pie chart widget
