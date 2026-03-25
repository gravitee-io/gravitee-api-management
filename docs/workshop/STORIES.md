# V4 API Analytics Dashboard — User Stories

## Story Map → Acceptance Criteria Traceability

| Story | AC Coverage |
|-------|-------------|
| S1. Unified endpoint scaffolding + COUNT | AC 1, 5, 11 |
| S2. STATS endpoint + ES adapter | AC 2, 5, 11 |
| S3. GROUP_BY endpoint + ES adapter | AC 3, 5, 11 |
| S4. DATE_HISTO endpoint + ES adapter | AC 4, 5, 11 |
| S5. Angular unified service | AC 6 |
| S6. Enhanced stats cards + dashboard layout | AC 7, 9, 10, 12 |
| S7. HTTP status pie chart | AC 8, 12 |

---

## Backend Stories

### Story 1: Unified Endpoint Scaffolding + COUNT

**As an** API Publisher, **I want** a unified analytics endpoint that supports flexible querying, starting with total request counts, **so that** dashboards use a single API surface for all analytics queries.

**Layer:** Backend
**Complexity:** M
**Dependencies:** None
**AC:** 1, 5, 11

#### Subtasks

1. **New REST method on `ApiAnalyticsResource`** — Add a `GET` handler at the root path accepting query params `type` (required enum: COUNT|STATS|GROUP_BY|DATE_HISTO), `from` (required), `to` (required), `field` (optional), `interval` (optional), `size` (optional), `order` (optional). Dispatch to the appropriate use case based on `type`. Return 400 for invalid/missing params.
   - File: `gravitee-apim-rest-api-management-v2/.../api/analytics/ApiAnalyticsResource.java`
   - Permission: `@Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })`

2. **New `SearchAnalyticsCountUseCase`** — Follows one-use-case-per-operation pattern. Orchestrates validation (V4 check, not TCP, multi-tenancy) and delegates to existing `analyticsQueryService.searchRequestsCount()`. Reuse validation pattern from `SearchRequestsCountAnalyticsUseCase`.
   - File: `gravitee-apim-rest-api-service/.../analytics/use_case/SearchAnalyticsCountUseCase.java`
   - Pattern: `@UseCase` annotation, `Input`/`Output` records

3. **Field whitelist validation** — Define an enum/set of allowed fields per query type. Validate `field` parameter in the resource layer before dispatching. Return 400 for unsupported fields.

4. **Parameter validation** — `from` < `to` (inverted range → 400), missing required params per type → 400.

5. **Response DTO** — Polymorphic response model with `type` discriminator. For COUNT: `{ type: "COUNT", count: long }`.
   - File: `gravitee-apim-rest-api-management-v2/.../rest/model/` (new response classes)

6. **Mapper** — Map domain model → response DTO.
   - File: extend `ApiAnalyticsMapper.java`

7. **Tests**:
   - Use case test: `SearchAnalyticsCountUseCaseTest.java` — COUNT, V4 check, TCP rejection
   - Resource test: `@Nested class UnifiedCountAnalytics` in `ApiAnalyticsResourceTest.java` — 200 response, 403 permission, missing `type` → 400, invalid `from`/`to` → 400
   - Pattern: `FakeAnalyticsQueryService`, `MAPIAssertions`, extends `ApiResourceTest`

---

### Story 2: STATS Analytics Endpoint + ES Adapter

**As an** API Publisher, **I want** to query statistical aggregations (min/max/avg/sum) for metrics like response time on my v4 API, **so that** dashboards can display average response times and content lengths.

**Layer:** Backend
**Complexity:** L
**Dependencies:** Story 1 (shared endpoint scaffolding)
**AC:** 2, 5, 11

#### Subtasks

1. **New `SearchAnalyticsStatsUseCase`** — Handle STATS with required `field` parameter.
   - Supported fields: `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`, `gateway-latency-ms`
   - File: `gravitee-apim-rest-api-service/.../analytics/use_case/SearchAnalyticsStatsUseCase.java`

2. **New `AnalyticsQueryService` method** — `searchStats(ExecutionContext, String apiId, String field, Instant from, Instant to)` → returns `StatsResult(count, min, max, avg, sum)`.
   - File: `AnalyticsQueryService.java` (interface) + `AnalyticsQueryServiceImpl.java` (implementation)

3. **New `AnalyticsRepository` method** — `searchStats(QueryContext, StatsQuery)` → returns `StatsAggregate`.
   - File: `gravitee-apim-repository-api/.../log/v4/api/AnalyticsRepository.java` (interface)
   - New query model: `gravitee-apim-repository-api/.../log/v4/model/analytics/StatsQuery.java`
   - New aggregate model: `gravitee-apim-repository-api/.../log/v4/model/analytics/StatsAggregate.java`

4. **ES implementation** — Build ES `extended_stats` aggregation on the specified field against `*-v4-metrics-*` index.
   - File: `gravitee-apim-repository-elasticsearch/.../ElasticsearchAnalyticsRepository.java`

5. **NoOp implementation** — Stub returning `Optional.empty()`.
   - File: `gravitee-apim-repository-noop/.../NoOpAnalyticsRepository.java`

6. **Response DTO** — STATS variant: `{ type: "STATS", count, min, max, avg, sum }`.

7. **Tests**:
   - Use case test: `SearchAnalyticsStatsUseCaseTest.java`
   - Resource test: `@Nested class StatsAnalytics` — valid field, missing field → 400, permission check
   - Fake: extend `FakeAnalyticsQueryService` with stats method

---

### Story 3: GROUP_BY Analytics Endpoint + ES Adapter

**As an** API Publisher, **I want** to query top-N aggregations by field (e.g., status code distribution) for my v4 API, **so that** dashboards can display HTTP status pie charts.

**Layer:** Backend
**Complexity:** L
**Dependencies:** Story 1 (shared endpoint scaffolding)
**AC:** 3, 5, 11

#### Subtasks

1. **New `SearchAnalyticsGroupByUseCase`** — Handle GROUP_BY with required `field`, optional `size` (default 10), `order`.
   - Supported fields: `status`, `mapped-status`, `application`, `plan`, `host`, `uri`
   - File: `gravitee-apim-rest-api-service/.../analytics/use_case/SearchAnalyticsGroupByUseCase.java`

2. **New `AnalyticsQueryService` method** — `searchGroupBy(ExecutionContext, String apiId, String field, int size, Instant from, Instant to)` → returns `Map<String, Long>` + metadata.

3. **New `AnalyticsRepository` method** — `searchGroupBy(QueryContext, GroupByQuery)` → returns `GroupByAggregate`.
   - File: `AnalyticsRepository.java` (interface)
   - New query model: `GroupByQuery.java`
   - New aggregate model: `GroupByAggregate.java`

4. **ES implementation** — Build ES `terms` aggregation on the specified field, with `size` limit and optional ordering against `*-v4-metrics-*`.
   - File: `ElasticsearchAnalyticsRepository.java`

5. **NoOp implementation** — Stub.

6. **Response DTO** — GROUP_BY variant: `{ type: "GROUP_BY", values: {key: count}, metadata: {key: {name: label}} }`.

7. **Tests**:
   - Use case test: `SearchAnalyticsGroupByUseCaseTest.java`
   - Resource test: `@Nested class GroupByAnalytics` — by status, missing field → 400, permission
   - Verify metadata is correctly populated

---

### Story 4: DATE_HISTO Analytics Endpoint + ES Adapter

**As an** API Publisher, **I want** to query time-bucketed histogram data for my v4 API, **so that** dashboards can display time-series charts of request distribution by status code.

**Layer:** Backend
**Complexity:** L
**Dependencies:** Story 1 (shared endpoint scaffolding)
**AC:** 4, 5, 11

#### Subtasks

1. **New `SearchAnalyticsDateHistoUseCase`** — Handle DATE_HISTO with required `field` and `interval` (ms). Validate `interval > 0`.
   - File: `gravitee-apim-rest-api-service/.../analytics/use_case/SearchAnalyticsDateHistoUseCase.java`

2. **New `AnalyticsQueryService` method** — `searchDateHistogram(ExecutionContext, String apiId, String field, Instant from, Instant to, Duration interval)` → returns timestamps + series data.

3. **New `AnalyticsRepository` method** — `searchDateHistogram(QueryContext, DateHistogramQuery)` → returns `DateHistogramAggregate`.
   - New query model: `DateHistogramQuery.java`
   - New aggregate model: `DateHistogramAggregate.java`

4. **ES implementation** — Build ES `date_histogram` aggregation with nested `terms` sub-aggregation on the specified field.
   - File: `ElasticsearchAnalyticsRepository.java`

5. **NoOp implementation** — Stub.

6. **Response DTO** — DATE_HISTO variant: `{ type: "DATE_HISTO", timestamp: [...], values: [{field, buckets, metadata}] }`.

7. **Tests**:
   - Use case test: `SearchAnalyticsDateHistoUseCaseTest.java` — valid response, `interval=0` → 400
   - Resource test: `@Nested class DateHistoAnalytics`
   - Pattern: follow existing `ResponseStatusOvertimeAnalytics` test structure

---

## Frontend Stories

### Story 5: Angular Unified Analytics Service

**As a** frontend developer, **I want** an Angular service that calls the unified `/analytics` endpoint for all 4 query types, **so that** all new dashboard widgets use a single data source.

**Layer:** Frontend
**Complexity:** S
**Dependencies:** Backend contract defined (can develop in parallel with S2–S4 using known response shapes)
**AC:** 6

#### Subtasks

1. **Extend `ApiAnalyticsV2Service`** — Add four new methods:
   - `getCount(apiId, from, to): Observable<CountResponse>`
   - `getStats(apiId, field, from, to): Observable<StatsResponse>`
   - `getGroupBy(apiId, field, from, to, size?): Observable<GroupByResponse>`
   - `getDateHisto(apiId, field, from, to, interval): Observable<DateHistoResponse>`
   - Each method uses the existing `timeRangeFilter()` pipe pattern
   - File: `src/services-ngx/api-analytics-v2.service.ts`

2. **New TypeScript interfaces** — Define response types matching the backend DTOs.
   - File: `src/entities/management-api-v2/analytics/` (new files: `analyticsCount.ts`, `analyticsStats.ts`, `analyticsGroupBy.ts`, `analyticsDateHisto.ts`)

3. **Tests**:
   - File: `src/services-ngx/api-analytics-v2.service.spec.ts` (extend existing)
   - Test each new method makes the correct HTTP call with query params

---

### Story 6: Enhanced Stats Cards + Dashboard Layout

**As an** API Publisher, **I want** to see total requests, average gateway response time, average upstream response time, and average content length as stats cards in a well-organized dashboard, **so that** I can quickly assess my API's performance.

**Layer:** Frontend
**Complexity:** M
**Dependencies:** Story 5 (unified service)
**AC:** 7, 9, 10, 12

#### Subtasks

1. **Update `ApiAnalyticsProxyComponent`** — Replace existing `getRequestsCount$` + `getAverageConnectionDuration$` with calls to the unified service (`getCount` + `getStats` for each metric). Build 4 stats cards:
   - Total Requests (COUNT)
   - Avg Gateway Response Time (STATS on `gateway-response-time-ms`)
   - Avg Upstream Response Time (STATS on `endpoint-response-time-ms`)
   - Avg Content Length (STATS on `request-content-length`)
   - File: `api-analytics-proxy.component.ts`

2. **Update dashboard layout** — Arrange widgets per PRD:
   - Row 1: Timeframe filter bar (already exists)
   - Row 2: 4 stats cards (using existing `ApiAnalyticsRequestStatsComponent`)
   - Row 3: Status pie chart placeholder (left) + Response status over time line chart (right)
   - Row 4: Response time over time line chart (full width)
   - File: `api-analytics-proxy.component.html` + `.scss`

3. **Empty state** — When analytics is enabled but no data exists (count = 0), show `<gio-card-empty-state>` with a helpful message. The analytics-disabled empty state already exists.

4. **Error state** — Ensure `catchError` handlers produce graceful fallback, not blank page.

5. **Verify existing line charts not broken** — `ApiAnalyticsResponseStatusOvertimeComponent` and `ApiAnalyticsResponseTimeOverTimeComponent` continue using legacy endpoints. Don't rewire (stretch goal).

6. **Tests**:
   - Verify all 4 stats cards render with correct values
   - Verify empty state when count = 0
   - Verify error state handled gracefully
   - Verify existing line charts render (regression)
   - Verify all widgets refresh on timeframe change
   - Use `DivHarness` from `@gravitee/ui-particles-angular/testing`

---

### Story 7: HTTP Status Pie Chart Widget

**As an** API Publisher, **I want** to see an HTTP status code distribution as a pie chart, **so that** I can quickly identify error rates and the distribution of response codes.

**Layer:** Frontend
**Complexity:** S
**Dependencies:** Story 5 (unified service with GROUP_BY), Story 6 (dashboard layout placeholder)
**AC:** 8, 12

#### Subtasks

1. **Reuse existing `GioChartPieModule`** — The component `GioChartPieComponent` already exists at `src/shared/components/gio-chart-pie/` with:
   - `GioChartPieInput` interface: `{ label: string, value: number, color: string }`
   - `GioChartPieHarness` for testing
   - Already used by `ApiAnalyticsResponseStatusRangesComponent` and home dashboard components
   - Note: it is `standalone: false` — must import via `GioChartPieModule`, NOT directly

2. **Add `getStatusGroupBy$` observable in `ApiAnalyticsProxyComponent`** — Call `getGroupBy(apiId, 'status', from, to)` and transform response to `GioChartPieInput[]`:
   - Map status codes to colors (2xx → green, 3xx → blue, 4xx → orange, 5xx → red)
   - Reference existing color mapping in `ApiAnalyticsResponseStatusRangesComponent`
   - File: `api-analytics-proxy.component.ts`

3. **Add `<gio-chart-pie>` to template** — Place inside a `<mat-card>` in the Row 3 left column. Show `<gio-card-empty-state>` when no status data.
   - File: `api-analytics-proxy.component.html`

4. **Add `GioChartPieModule` to imports** — In the component's `imports` array.

5. **Tests**:
   - Verify pie chart receives correct data from GROUP_BY response
   - Verify empty state when no status data
   - Use `GioChartPieHarness` for assertions

---

## Implementation Order

```
S1 (scaffolding + COUNT)
├── S2 (STATS + ES)      ─┐
├── S3 (GROUP_BY + ES)   ─┼── S5 (FE service) ──┬── S6 (stats cards + layout)
└── S4 (DATE_HISTO + ES) ─┘                      └── S7 (pie chart)
```

S2/S3/S4 are independent of each other (only depend on S1).
S5 can start in parallel with S2–S4 (only needs the contract, not running backend).
S6 and S7 are independent of each other.

**Recommended serial order for a single developer:**
```
S1 → S2 → S3 → S4 → S5 → S6 → S7
```

---

## Key Codebase Patterns

- **Use case pattern:** One `@UseCase` per operation, `Input`/`Output` records, validation (V4 check, TCP reject, multi-tenancy)
- **Resource test pattern:** `@Nested` inner classes, `FakeAnalyticsQueryService`, `MAPIAssertions`, extends `ApiResourceTest`
- **Repository layer:** `AnalyticsRepository` (interface in `gravitee-apim-repository-api`) → `ElasticsearchAnalyticsRepository` (ES impl) + `NoOpAnalyticsRepository` (noop stub)
- **Frontend service pattern:** `BehaviorSubject<TimeRangeParams>` with `switchMap` on `timeRangeFilter()`
- **Frontend component pattern:** `combineLatest` → VM observable with `isLoading`/`startWith`/`catchError`
- **Chart library:** Highcharts wrapped via `GioChartLineModule` (line) and `GioChartPieModule` (pie)
- **UI components:** `GioLoaderModule`, `GioCardEmptyStateModule`, `MatCardModule`, `GioIconsModule`
- **Test harnesses:** `DivHarness`, `GioChartPieHarness` from `@gravitee/ui-particles-angular/testing`

---

## Refinement Notes (Phase 1.3 — Fresh Agent Review)

### 🔴 ACCEPTED: GioChartPieComponent Already Exists
**Original claim:** "NO existing pie/donut chart component in the codebase. Need to create one from scratch."
**Reality:** `GioChartPieComponent` exists at `src/shared/components/gio-chart-pie/` with module, harness, and is already used by 4+ components.
**Fix applied:** Story 7 completely reworked to reuse `GioChartPieModule`. Resized from M → S.

### 🔴 ACCEPTED: ES Repository Layer Under-Scoped
**Original:** Stories 2–4 mentioned "New AnalyticsRepository method" as a bullet point, hiding the ES aggregation implementation work.
**Reality:** `AnalyticsRepository` (v4) has 10 purpose-built methods, zero generic aggregation methods. Writing `extended_stats`, `terms`, `date_histogram` queries is non-trivial.
**Fix applied:** Stories 2–4 resized from M → L, with explicit subtasks for repository API module, ES implementation module, NoOp stub, and complete file paths.

### 🟡 ACCEPTED: Use One-Use-Case-Per-Operation Pattern
**Original:** Proposed a single `SearchAnalyticsUseCase` dispatching on `type`.
**Review:** Existing codebase has 6 separate use cases, one per endpoint. Creating a dispatcher breaks this convention.
**Fix applied:** Changed to 4 separate use cases (`SearchAnalyticsCountUseCase`, `SearchAnalyticsStatsUseCase`, `SearchAnalyticsGroupByUseCase`, `SearchAnalyticsDateHistoUseCase`). Type dispatch stays in the resource layer.

### 🟡 ACCEPTED: Merge Stories 6 + 8
**Original:** Story 6 (stats cards) and Story 8 (dashboard integration) modified the same files.
**Fix applied:** Merged into Story 6 "Enhanced Stats Cards + Dashboard Layout". Total is now 7 stories instead of 8.

### 🟡 ACCEPTED: S1 Resized from S → M
Story 1 creates the entire unified endpoint scaffolding (JAX-RS handler, param parsing, type dispatch, DTOs, field validation) AND the COUNT implementation. This is two concerns — kept together but resized.

### 🟡 ACCEPTED: Dependency Graph Loosened
S2–S4 are now shown as independent of each other (all depend on S1 only). S5 can start in parallel with S2–S4.

### 🟡 ACCEPTED: Parameter Validation
Added input validation subtasks: `from > to` → 400, missing required field per type → 400, `interval ≤ 0` → 400.

### 🟡 ACCEPTED: Empty Response ≠ 404
Existing endpoints throw `NotFoundException` on empty ES results. The new unified endpoint should return successful empty responses (count=0, empty values map) instead — per PRD "empty state" requirement.

### 🟢 NOTED: Backend 503 for ES Unavailability
Good point but lower priority for workshop scope. The PRD says "graceful degradation" but the existing endpoints already let ES exceptions bubble up. We'll rely on the frontend `catchError` for now and note this as a follow-up.
