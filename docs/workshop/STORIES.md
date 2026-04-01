# V4 API Analytics Dashboard — Detailed Implementation Plan

> Each story lists subtasks in **test-first** order: write the failing test, then implement to green.
> Stories 01–04 are **complete**. Stories 05–08 are the remaining frontend work.

## Key Paths Reference

```
# Backend — REST resource (V2)
REST_RESOURCE = gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest
REST_SRC      = ${REST_RESOURCE}/src/main/java/io/gravitee/rest/api/management/v2/rest
REST_TEST     = ${REST_RESOURCE}/src/test/java/io/gravitee/rest/api/management/v2/rest

# Backend — Core domain (use cases, query services)
CORE          = gravitee-apim-rest-api/gravitee-apim-rest-api-service
CORE_SRC      = ${CORE}/src/main/java/io/gravitee/apim/core/analytics
CORE_TEST     = ${CORE}/src/test/java/io/gravitee/apim/core/analytics

# Backend — Repository API
REPO_API      = gravitee-apim-repository/gravitee-apim-repository-api
REPO_API_SRC  = ${REPO_API}/src/main/java/io/gravitee/repository/log/v4

# Backend — Elasticsearch implementation
REPO_ES       = gravitee-apim-repository/gravitee-apim-repository-elasticsearch
REPO_ES_SRC   = ${REPO_ES}/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics

# Backend — Test fakes
FAKES         = ${CORE}/src/test/java/fakes

# Frontend
FE            = gravitee-apim-console-webui/src
FE_ANALYTICS  = ${FE}/management/api/api-traffic-v4/analytics
FE_PROXY      = ${FE_ANALYTICS}/api-analytics-proxy
FE_COMPONENTS = ${FE_ANALYTICS}/components
FE_SERVICE    = ${FE}/services-ngx
FE_SHARED     = ${FE}/shared
FE_ENTITIES   = ${FE}/entities/management-api-v2/analytics
```

---

## ✅ Story 01: Unified Analytics Endpoint — COUNT (COMPLETE)

**Layer:** Backend | **Complexity:** S

**What was built:**
- `AnalyticsParam` BeanParam with `validate()` method
- `AnalyticsCountResponse` core model record
- `SearchAnalyticsCountUseCase` with API validation (V4, not TCP, correct env)
- `GET /v2/apis/{apiId}/analytics?type=COUNT` dispatched via switch in `ApiAnalyticsResource`
- 6 use case tests + 4 REST tests

---

## ✅ Story 02: Unified Analytics Endpoint — STATS (COMPLETE)

**Layer:** Backend | **Complexity:** S

**What was built:**
- `AnalyticsStatsResponse` core model record: `long count, double min, double max, double avg, double sum`
- `SearchAnalyticsStatsUseCase` with field allowlist validation
- Supported fields: `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`, `gateway-latency-ms`
- `StatsQuery` / `StatsAggregate` in repo-api; `SearchStatsQueryAdapter` / `SearchStatsResponseAdapter` in ES
- `case STATS` in `ApiAnalyticsResource` switch
- 8 use case tests + 2 REST tests

---

## ✅ Story 03: Unified Analytics Endpoint — GROUP_BY (COMPLETE)

**Layer:** Backend | **Complexity:** M

**What was built:**
- `AnalyticsGroupByResponse` core model record: `Map<String,Long> values, Map<String,Map<String,String>> metadata`
- `SearchAnalyticsGroupByUseCase` with field allowlist + metadata resolution via `ApplicationCrudService`/`PlanCrudService`
- Supported fields: `status`, `mapped-status`, `application`, `plan`, `host`, `uri`
- `GroupByQuery` / `GroupByAggregate` in repo-api; ES `terms` aggregation adapters
- `case GROUP_BY` in `ApiAnalyticsResource` switch
- 10 use case tests + 2 REST tests

---

## ✅ Story 04: Unified Analytics Endpoint — DATE_HISTO (COMPLETE)

**Layer:** Backend | **Complexity:** M

**What was built:**
- `AnalyticsDateHistoResponse` core model record: `List<Long> timestamps, List<DateHistoBucket> values`
- `SearchAnalyticsDateHistoUseCase` with field allowlist + interval validation [1000, 1_000_000_000]
- Supported fields: `status`, `gateway-response-time-ms`, `endpoint-response-time-ms`
- `DateHistoQuery` / `DateHistoAggregate` in repo-api; ES `date_histogram` adapters
  - `status` → nested `terms` sub-agg; metric fields → nested `avg` sub-agg
- `case DATE_HISTO` in `ApiAnalyticsResource` switch (reads `interval` param, defaults to 3,600,000ms)
- 10 use case tests + 2 REST tests

---

## ⬜ Story 05: Frontend Service for Unified Endpoint

**Layer:** Frontend | **Complexity:** S | **Dependencies:** Stories 01–04

**As a** frontend developer,
**I want** `ApiAnalyticsV2Service` to expose typed methods for the unified endpoint,
**so that** dashboard components call a clean, reactive API instead of raw HTTP.

### Subtask 05.1: Define TypeScript response interfaces

**Create:**
- `${FE_ENTITIES}/analyticsUnified.ts`

```typescript
export interface AnalyticsCount  { count: number; }
export interface AnalyticsStats  { count: number; min: number; max: number; avg: number; sum: number; }
export interface AnalyticsGroupBy {
  values: Record<string, number>;
  metadata: Record<string, Record<string, string>>;
}
```

### Subtask 05.2: Write service tests (RED)

**Modify:** `${FE_SERVICE}/api-analytics-v2.service.spec.ts`

**Tests to write:**
- `should call unified endpoint with type=COUNT`
- `should call unified endpoint with type=STATS and field param`
- `should call unified endpoint with type=GROUP_BY, field and size params`
- `should not fetch when timeRangeFilter is null`
- `should re-fetch on timeRangeFilter change`

**Pattern reference:** Use `HttpTestingController` (no mocks). Same setup as `api-analytics-proxy.component.spec.ts`.

### Subtask 05.3: Implement service methods (GREEN)

**Modify:** `${FE_SERVICE}/api-analytics-v2.service.ts`

**Add methods:**
```typescript
getAnalyticsCount(apiId: string): Observable<AnalyticsCount>
getAnalyticsStats(apiId: string, field: string): Observable<AnalyticsStats>
getAnalyticsGroupBy(apiId: string, field: string, size?: number): Observable<AnalyticsGroupBy>
```

**Pattern:** Follow existing methods exactly — `timeRangeFilter$.pipe(filter(Boolean), switchMap(({ from, to }) => http.get(...)))`. New endpoint is `${v2BaseURL}/apis/${apiId}/analytics` with query params `type`, `field`, `size`, `from`, `to`.

---

## ⬜ Story 06: Stats Cards Row

**Layer:** Frontend | **Complexity:** S | **Dependencies:** Story 05

**As an** API Publisher,
**I want** four stats cards showing Total Requests, Avg GW Response Time, Avg Upstream Response Time, and Avg Content Length,
**so that** I see key performance indicators sourced from the unified endpoint.

### Subtask 06.1: Write proxy component tests for new cards (RED)

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.spec.ts`

**What changes:**
- Replace HTTP expectations for `/requests-count` and `/average-connection-duration` with `?type=COUNT` and `?type=STATS&field=gateway-response-time-ms`, `?type=STATS&field=endpoint-response-time-ms`, `?type=STATS&field=request-content-length`
- Update assertion from 2 card values to 4 card values
- All existing test scenarios (loading, analytics disabled, refresh) must still pass

**Tests to update:**
- `should display HTTP Proxy Entrypoint - Request Stats` → assert 4 cards with correct labels and values
- `should refresh` → assert all 4 cards go loading and re-fetch via unified endpoint

### Subtask 06.2: Update proxy component to use unified endpoint (GREEN)

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.ts`

**What changes:**
- Replace `getRequestsCount$` → call `getAnalyticsCount(apiId)`, extract `.count`
- Replace `getAverageConnectionDuration$` → call `getAnalyticsStats(apiId, 'gateway-response-time-ms')`, extract `.avg`
- Add `getAnalyticsStats(apiId, 'endpoint-response-time-ms')`, extract `.avg` for new card
- Add `getAnalyticsStats(apiId, 'request-content-length')`, extract `.avg` for new card
- Update `requestStats` array construction from 2 entries to 4 entries:
  ```typescript
  [
    { label: 'Total Requests',              value: count.count,              isLoading },
    { label: 'Avg GW Response Time', unitLabel: 'ms', value: gwStats.avg,   isLoading },
    { label: 'Avg Upstream Response Time', unitLabel: 'ms', value: upStats.avg, isLoading },
    { label: 'Avg Content Length',  unitLabel: 'B',  value: clStats.avg,    isLoading },
  ]
  ```
- Keep `combineLatest` of 4 streams, using `catchError(() => of({ isLoading: false }))` for each

---

## ⬜ Story 07: HTTP Status Pie Chart

**Layer:** Frontend | **Complexity:** S | **Dependencies:** Story 05

**As an** API Publisher,
**I want** the HTTP status pie chart sourced from the unified `GROUP_BY` endpoint,
**so that** I see an accurate breakdown of status code distribution without relying on the legacy `response-status-ranges` endpoint.

### Subtask 07.1: Write proxy component tests for new pie data source (RED)

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.spec.ts`

**What changes:**
- Replace HTTP expectation for `/response-status-ranges` with `?type=GROUP_BY&field=status`
- New fixture: `{ values: { '200': 60, '404': 1, '500': 1 }, metadata: {} }`
- Assert pie chart still renders with correct data after response

**Tests to update:**
- `should display Response Status` → flush `?type=GROUP_BY&field=status` response, assert pie renders

### Subtask 07.2: Update proxy component to use GROUP_BY for pie (GREEN)

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.ts`

**What changes:**
- Replace `getResponseStatusRanges$` → call `getAnalyticsGroupBy(apiId, 'status')`
- Add a bucketing mapper function to transform individual codes to range keys:
  ```typescript
  function bucketStatusCodes(values: Record<string, number>): Record<string, number> {
    const ranges: Record<string, number> = {};
    for (const [code, count] of Object.entries(values)) {
      const n = Number(code);
      const key = n < 200 ? '100.0-200.0'
                : n < 300 ? '200.0-300.0'
                : n < 400 ? '300.0-400.0'
                : n < 500 ? '400.0-500.0'
                           : '500.0-600.0';
      ranges[key] = (ranges[key] ?? 0) + count;
    }
    return ranges;
  }
  ```
- Pass bucketed ranges to `responseStatusRanges.data` as `Object.entries(bucketed).map(([label, value]) => ({ label, value }))`

---

## ⬜ Story 08: Dashboard Assembly & States

**Layer:** Frontend | **Complexity:** M | **Dependencies:** Stories 06, 07

**As an** API Publisher,
**I want** a fully assembled analytics dashboard with correct empty, loading, and error states,
**so that** the page is reliable and coherent regardless of data availability.

### Subtask 08.1: Write full proxy component spec (RED)

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.spec.ts`

**Tests to write/update:**
- `should display loading` — assert global spinner shown before API call resolves
- `GIVEN analytics.enabled=false` → assert empty state panel (no changes to existing test)
- `GIVEN analytics.enabled=true` → loading → stats cards (4) load → pie loads; assert all HTTP calls succeed
- `should refresh` → timeframe change triggers re-fetch of all 4 unified endpoint calls + pie call
- `should show graceful degradation when analytics call fails` → one stat card fails (`catchError`) → other cards still show

**All HTTP calls in the "analytics enabled" flow:**
```
GET /apis/{id}
GET /apis/{id}/analytics?type=COUNT
GET /apis/{id}/analytics?type=STATS&field=gateway-response-time-ms
GET /apis/{id}/analytics?type=STATS&field=endpoint-response-time-ms
GET /apis/{id}/analytics?type=STATS&field=request-content-length
GET /apis/{id}/analytics?type=GROUP_BY&field=status
GET /apis/{id}/analytics/response-status-overtime  ← existing, unchanged
GET /apis/{id}/analytics/response-time-over-time   ← existing, unchanged
```

### Subtask 08.2: Wire dashboard & update template (GREEN)

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.ts`

**`ApiAnalyticsVM` type:**
```typescript
type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;          // 4 cards
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;  // pie
};
```

**Observable wiring:**
- `analyticsData$` = `combineLatest([count$, gwStats$, upStats$, clStats$, groupByStatus$])` all wrapped with `catchError`
- `apiAnalyticsVM$` combines API fetch + `analyticsData$` under the `isAnalyticsEnabled` guard (same pattern as current code)

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.html`

**Layout (top to bottom, no new rows vs existing):**
```html
<api-analytics-filters-bar />
<app-api-analytics-request-stats title="Request Stats" [requestsStats]="vm.requestStats" />
<div class="gridContent">
  <api-analytics-response-status-ranges title="Response Status" [responseStatusRanges]="vm.responseStatusRanges" />
  <api-analytics-response-status-overtime />
</div>
<api-analytics-response-time-over-time class="full-bleed" />
```

### Subtask 08.3: Update harness if needed

**Modify:** `${FE_PROXY}/api-analytics-proxy.component.harness.ts`
- Verify existing harness locators still work after template changes (no new harness entries expected)

---

## ❌ Out of Scope

| Original Story | Title | Reason |
|---|---|---|
| 05 | Query Filter Parameter | Out of scope per PRD |
| 06 | Timeframe Filter Bar URL Persistence | Out of scope per PRD |
| 10 | Response Status Over Time rewire to DATE_HISTO | Out of scope per PRD |
| 11 | Top Applications & Top Plans tables | Out of scope per PRD |
| 12 | Response Time Over Time rewire to DATE_HISTO | Out of scope per PRD |
| 14 | Documentation | Out of scope per PRD |

---

## Dependency Graph

```
✅ Story 01 (COUNT)
✅ Story 02 (STATS)
✅ Story 03 (GROUP_BY)    → ⬜ Story 05 (FE Service) ─┬─ ⬜ Story 06 (Stats Cards) ─┐
✅ Story 04 (DATE_HISTO)  ↗                            └─ ⬜ Story 07 (Pie Chart)   ─┤
                                                                                       └─ ⬜ Story 08 (Assembly)
```

---

## File Summary

### Modified Files (Frontend, Stories 05–08)

| File | Stories |
|------|---------|
| `${FE_SERVICE}/api-analytics-v2.service.ts` | 05 |
| `${FE_SERVICE}/api-analytics-v2.service.spec.ts` | 05 |
| `${FE_PROXY}/api-analytics-proxy.component.ts` | 06, 07, 08 |
| `${FE_PROXY}/api-analytics-proxy.component.html` | 08 |
| `${FE_PROXY}/api-analytics-proxy.component.spec.ts` | 06, 07, 08 |
| `${FE_PROXY}/api-analytics-proxy.component.harness.ts` | 08 (verify only) |

### New Files (Frontend, Stories 05–08)

| File | Story |
|------|-------|
| `${FE_ENTITIES}/analyticsUnified.ts` | 05 |
