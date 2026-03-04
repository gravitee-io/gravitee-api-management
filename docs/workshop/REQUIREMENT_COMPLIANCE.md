# V4 API Analytics Dashboard – Requirement Compliance

This document maps the full PRD and acceptance criteria to the current implementation (post-workshop).

## Acceptance criteria (1–12)

| # | Criterion | Status | Implementation |
|---|-----------|--------|----------------|
| 1 | [Backend] GET /v2/apis/{apiId}/analytics returns COUNT data (total hits) | ✅ | `ApiAnalyticsResource.getApiV4Analytics()` with `type=COUNT`; returns `{ "type": "COUNT", "count": <long> }`. |
| 2 | [Backend] Endpoint returns STATS data (min/max/avg/sum for a given field) | ✅ | Same endpoint, `type=STATS` + `field`; returns type, count, min, max, avg, sum. |
| 3 | [Backend] Endpoint returns GROUP_BY data (top-N by field with metadata) | ✅ | Same endpoint, `type=GROUP_BY` + `field`, optional `size` (default 10), `order`; returns values map + metadata. |
| 4 | [Backend] Endpoint returns DATE_HISTO data (time-bucketed histogram) | ✅ | Same endpoint, `type=DATE_HISTO` + `field` + `interval` (1000–1000000000 ms); returns timestamp array + values (field, buckets, metadata). |
| 5 | [Backend] Endpoint enforces API_ANALYTICS:READ permission | ✅ | `@Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })` on `getApiV4Analytics`; resource test `should_return_403_when_no_API_ANALYTICS_READ`. |
| 6 | [Frontend] Angular service calls the unified endpoint for all 4 query types | ✅ | `ApiAnalyticsV2Service.getV4Analytics(apiId, params)` with `type`, `from`, `to`, `field`, `interval`, `size`, `order`; used for COUNT, STATS, GROUP_BY (DATE_HISTO available; line charts still use existing endpoints per PRD). |
| 7 | [Frontend] Stats cards show total requests, avg gateway response time, avg upstream response time, avg content length | ✅ | Proxy uses unified COUNT + 3× STATS (gateway-response-time-ms, endpoint-response-time-ms, request-content-length); `app-api-analytics-request-stats` shows all four. |
| 8 | [Frontend] Dashboard shows HTTP status pie chart (new widget) | ✅ | `api-analytics-response-status-ranges` fed from GROUP_BY on `status`; same component used as “HTTP Status” pie. |
| 9 | [Frontend] Dashboard shows existing line charts (not broken by changes) | ✅ | `api-analytics-response-status-overtime` and `api-analytics-response-time-over-time` still use existing endpoints (`/response-status-overtime`, `/response-time-over-time`). |
| 10 | [Frontend] Empty state displayed when no analytics data exists | ✅ | When `count === 0` and no error: `gio-card-empty-state` “No analytics data for this period” (subtitle: try another period). Spec: `should display empty state when no analytics data for period (count 0)`. |
| 11 | [Tests] Unit tests for all backend analytics query types | ✅ | `ApiAnalyticsResourceTest.UnifiedV4Analytics`: COUNT (200 + zero), STATS, GROUP_BY, DATE_HISTO (200 + body), 403, 400 (missing type/from/to, from≥to, invalid type, STATS/GROUP_BY/DATE_HISTO without field, DATE_HISTO without interval / interval out of range). `AnalyticsQueryServiceImplTest`: SearchV4AnalyticsCount, SearchV4AnalyticsStats, SearchV4AnalyticsGroupBy, SearchV4AnalyticsDateHisto. |
| 12 | [Tests] Angular component tests for dashboard and widgets | ✅ | `api-analytics-proxy.component.spec.ts`: loading, analytics disabled empty panel, request stats from unified (COUNT+STATS), empty state when count 0, HTTP Status pie from GROUP_BY, refresh re-fetch, error state (Analytics unavailable), query params time range. `api-analytics-v2.service.spec.ts`: getV4Analytics for COUNT, STATS, GROUP_BY, DATE_HISTO and error propagation. |

## PRD “What to Build” checklist

### Backend: Management API v2 analytics endpoint

- **Endpoint:** `GET /v2/apis/{apiId}/analytics` ✅  
- **Query params:** type (required), from/to (required), field (for STATS/GROUP_BY), interval (for DATE_HISTO), size, order ✅  
- **Response types:** COUNT, STATS, GROUP_BY, DATE_HISTO with described JSON shapes ✅  
- **Authorization:** API_ANALYTICS:READ ✅  
- **Data source:** v4-metrics (handled in repository/query service) ✅  

### Frontend: Console analytics dashboard

- **Location:** Console > APIs > [v4 API] > API Traffic > Analytics (existing tab) ✅  
- **Row 1:** Timeframe filter bar (predefined ranges) ✅ (existing `api-analytics-filters-bar`)  
- **Row 2:** Stats cards (Total Requests, Avg Gateway/Upstream Response Time, Avg Content Length) ✅  
- **Row 3:** HTTP Status pie (GROUP_BY status) + Response status over time line chart ✅  
- **Row 4:** Response time over time line chart ✅  
- **Interactions:** Timeframe triggers re-fetch; empty state when no data; error state when analytics unavailable ✅  
- **Existing endpoints:** Separate endpoints (e.g. requests-count, response-status-overtime) still present and used where specified ✅  

### Out of scope (unchanged)

- Query filter parameter, custom date picker, URL-persisted timeframe, Top Applications/Plans, response-time-over-time rewire to new endpoint, filter dropdowns, custom dashboards, v4 Message API analytics, platform-level analytics, export ✅ (none implemented, as intended)

## Files touched (summary)

- **Backend:** `ApiAnalyticsResource.java` (v2, unified GET), `AnalyticsQueryService` / Impl, repository/ES adapters, `ApiAnalyticsResourceTest`, `AnalyticsQueryServiceImplTest`, OpenAPI `openapi-apis.yaml` (unified path + schemas).  
- **Frontend:** `api-analytics-proxy.component.ts/html` (unified data + empty/error states), `api-analytics-v2.service.ts` (getV4Analytics), `api-analytics-proxy.component.spec.ts`, `api-analytics-v2.service.spec.ts`.

---

*Last aligned with the full PRD and acceptance criteria (1–12) and existing STORIES.md / workshop implementation.*
