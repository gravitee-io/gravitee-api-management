I'll analyze the existing codebase structure and PRD carefully before breaking this into user stories. Let me study the relevant files first.No files were uploaded, so I'll work from the detailed code references in the PRD and my knowledge of the Gravitee API Management codebase. Let me structure a thorough analysis.Now I have a thorough understanding of both the PRD and the design system. Let me build the comprehensive user story breakdown.Here's the full user story breakdown for the V4 API Analytics Dashboard. I've structured these based on careful reading of the PRD, the stated existing code patterns, and natural dependency ordering.

---

## Story map: V4 API Analytics Dashboard (M1)

The work splits across **3 Backend stories**, **5 Frontend stories**, and **2 Testing stories** — 10 stories total. Dependencies flow strictly Backend → Service → Frontend so teams can parallelize after the first backend story ships.

---

### US-01 — Unified analytics endpoint skeleton + COUNT

**Layer:** Backend | **Complexity:** M | **Depends on:** nothing

**As an** API Publisher,
**I want** a single `GET /v2/apis/{apiId}/analytics?type=COUNT&from=…&to=…` endpoint,
**so that** I can retrieve the total number of requests for my API in a given time window without hitting multiple bespoke endpoints.

**Acceptance criteria:**
1. `GET /v2/apis/{apiId}/analytics` exists in `ApiAnalyticsResource.java` alongside (not replacing) existing endpoints.
2. `type=COUNT` with valid `from`/`to` returns `{ "type": "COUNT", "count": <long> }`.
3. Missing `type` returns `400 Bad Request` with a descriptive error body.
4. Missing or invalid `from`/`to` returns `400 Bad Request`.
5. Caller without `API_ANALYTICS:READ` permission receives `403 Forbidden`.
6. Endpoint queries the `*-v4-metrics-*` ES index (not the v2 index).
7. OpenAPI spec is updated with the new path, all query params, and all response schemas (`AnalyticsResponse` union type).
8. A new `AnalyticsType` enum (`COUNT | STATS | GROUP_BY | DATE_HISTO`) drives a dispatcher inside the use-case layer.

**Implementation notes:**
The existing `ApiAnalyticsResource` already has `@Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = {READ}) })` on individual methods — apply the same annotation at the new method level. Wire through a new use-case class `GetApiAnalyticsUseCase` that accepts an `AnalyticsQuery` record and delegates to the right ES query builder based on `type`. Reuse the existing `SearchRequestBuilder` / `AggregationBuilder` infrastructure from the V4 metrics query layer — don't invent a new ES client wrapper.

---

### US-02 — STATS and GROUP_BY support on unified endpoint

**Layer:** Backend | **Complexity:** M | **Depends on:** US-01

**As an** API Publisher,
**I want** the unified endpoint to support `type=STATS` (field-level statistics) and `type=GROUP_BY` (top-N aggregation by field),
**so that** I can retrieve average response times and a ranked breakdown of HTTP status codes from a single endpoint contract.

**Acceptance criteria:**
1. `type=STATS&field=gateway-response-time-ms` returns `{ "type": "STATS", "count": …, "min": …, "max": …, "avg": …, "sum": … }`.
2. `type=STATS` without `field` returns `400`.
3. `field` values not in the allowed set (`status`, `mapped-status`, `application`, `plan`, `host`, `uri`, `gateway-latency-ms`, `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`) return `400` with the allowed list in the error body.
4. `type=GROUP_BY&field=status` returns `{ "type": "GROUP_BY", "values": { "200": …, … }, "metadata": { "200": {"name":"200"}, … } }`.
5. `size` query param (default 10, max 100) controls number of buckets in `GROUP_BY`.
6. `order` query param (`ASC` | `DESC`) is accepted and passed to the aggregation; defaults to `DESC`.
7. Both query types honour `API_ANALYTICS:READ` (covered by the method-level annotation from US-01).
8. All new response shapes are added to the OpenAPI spec.

**Implementation notes:**
Map `field` values to their Elasticsearch field names via a `FieldMapper` utility (e.g. `gateway-response-time-ms` → `gateway-response-time`). For STATS, use an ES `stats` aggregation. For GROUP_BY, use a `terms` aggregation with `size` and `order`. Metadata for HTTP status codes can be generated purely from the bucket keys — no secondary ES query needed for M1.

---

### US-03 — DATE_HISTO support on unified endpoint

**Layer:** Backend | **Complexity:** M | **Depends on:** US-02

**As an** API Publisher,
**I want** the unified endpoint to support `type=DATE_HISTO`,
**so that** I can retrieve time-bucketed counts of requests broken down by a field (e.g. HTTP status) for charting trends.

**Acceptance criteria:**
1. `type=DATE_HISTO&field=status&interval=3600000&from=…&to=…` returns a valid `DATE_HISTO` response.
2. Response shape: `{ "type": "DATE_HISTO", "timestamp": [<ms>, …], "values": [{ "field": "200", "buckets": [<count>, …], "metadata": {"name": "200"} }, …] }`.
3. The `timestamp` array and each `buckets` array have equal length.
4. `interval` is required for `DATE_HISTO`; missing it returns `400`.
5. `interval` values that produce more than 1 440 buckets (one minute over 24h at 1s resolution) return `400 Bad Request` to protect ES performance.
6. When ES returns zero documents for the range, the endpoint returns `{ "type": "DATE_HISTO", "timestamp": [], "values": [] }` (not a 404).
7. ES bucket gaps (intervals with zero hits) are filled with `0` in the `buckets` array so timestamps and buckets remain aligned.
8. OpenAPI spec updated.

**Implementation notes:**
Use an ES `date_histogram` aggregation with a nested `terms` sub-aggregation on the field. The outer aggregation drives the `timestamp` array; inner buckets populate per-series `buckets`. Gap-fill by iterating the expected timestamp range with the given interval and inserting zeros where ES returned no bucket. Reuse any existing interval-to-ES-calendar-interval mapping from the v2 analytics layer if present.

---

### US-04 — Angular service: unified analytics client

**Layer:** Frontend | **Complexity:** S | **Depends on:** US-01 (interface contract; can be mocked before backend ships)

**As a** frontend engineer,
**I want** `ApiAnalyticsV2Service` to expose a single typed `getAnalytics(apiId, params)` method calling the unified endpoint,
**so that** all dashboard widgets use one consistent HTTP client method rather than four separate service methods.

**Acceptance criteria:**
1. `ApiAnalyticsV2Service.getAnalytics(apiId: string, params: AnalyticsQueryParams): Observable<AnalyticsResponse>` exists.
2. `AnalyticsQueryParams` is a discriminated union typed to each `type` variant so TypeScript prevents `field`-less STATS calls at compile time.
3. `AnalyticsResponse` is a discriminated union (`CountResponse | StatsResponse | GroupByResponse | DateHistoResponse`) matching the OpenAPI contract from US-01–03.
4. The method calls `GET /v2/apis/{apiId}/analytics` with all params serialised as query strings; no request body.
5. Existing service methods (`getRequestsCount`, `getResponseStatusRanges`, etc.) are **not removed** — they stay intact so existing components don't break.
6. Unit test covers each of the four type variants with an `HttpClientTestingModule` mock.
7. The service is in `src/services-ngx/` and exported from its barrel.

---

### US-05 — Timeframe filter bar component

**Layer:** Frontend | **Complexity:** S | **Depends on:** US-04

**As an** API Publisher,
**I want** a horizontal filter bar at the top of the analytics dashboard with predefined time range buttons (Last 5 min, 1 hour, 24 hours, 7 days, 30 days),
**so that** I can quickly switch the time window for all widgets at once without typing timestamps.

**Acceptance criteria:**
1. A new standalone `AnalyticsTimeframeComponent` renders five labelled toggle buttons; exactly one is active at a time.
2. Default selected range is `Last 1 hour` on first load.
3. Selecting a range emits a `timeframeChange` output carrying `{ from: number, to: number }` in epoch milliseconds (computed relative to `Date.now()` at selection time).
4. Active button has a visually distinct selected state using existing GIO design-system tokens.
5. Component uses `OnPush` change detection and the Angular signals API (no `BehaviorSubject`).
6. Component has no hard-coded pixel widths; it is responsive within the dashboard container.
7. Unit test covers: default selection, button click emitting correct `{ from, to }` range, and that only one button is active at a time.

---

### US-06 — Stats cards row

**Layer:** Frontend | **Complexity:** M | **Depends on:** US-04, US-05

**As an** API Publisher,
**I want** a row of four stat cards showing Total Requests, Avg Gateway Response Time, Avg Upstream Response Time, and Avg Content Length,
**so that** I can see the key health metrics for my API at a glance when I open the Analytics tab.

**Acceptance criteria:**
1. Four `gio-card` (or equivalent GIO component) stats cards render in a single horizontal row below the timeframe filter.
2. "Total Requests" card calls the unified endpoint with `type=COUNT` and displays `response.count`.
3. "Avg Gateway Response Time" calls with `type=STATS&field=gateway-response-time-ms` and displays `response.avg` formatted as `XX ms`.
4. "Avg Upstream Response Time" calls with `type=STATS&field=endpoint-response-time-ms` and displays `response.avg` as `XX ms`.
5. "Avg Content Length" calls with `type=STATS&field=request-content-length` and displays `response.avg` formatted as a human-readable byte string (e.g. `4.2 KB`).
6. All four cards issue their requests in parallel (`forkJoin`) — not sequentially — and refresh together when `timeframeChange` fires.
7. Each card shows a loading skeleton while the request is in flight.
8. Each card shows `—` (en-dash, not "N/A") when the response `count` is `0` (no data).
9. Each card shows an error icon + tooltip with message when the API call fails.
10. Component is standalone, uses `OnPush`, and is signal-based for loading/error state.

---

### US-07 — HTTP status pie chart widget

**Layer:** Frontend | **Complexity:** M | **Depends on:** US-04, US-05

**As an** API Publisher,
**I want** a pie chart breaking down HTTP responses by status code,
**so that** I can immediately see the proportion of 2xx, 4xx, and 5xx responses for my API without having to read a table.

**Acceptance criteria:**
1. A new `ApiAnalyticsStatusPieComponent` renders in the left column of Row 3.
2. It calls the unified endpoint with `type=GROUP_BY&field=status&size=10`.
3. Status codes are visually grouped by class: 2xx slices use a green ramp, 3xx blue, 4xx amber, 5xx red — matching GIO chart colour conventions.
4. Hovering a slice shows a tooltip with `{statusCode}: {count} requests ({percent}%)`.
5. A legend below (or beside) the chart lists each status code and its count.
6. Empty state: when `response.values` is empty, the chart area is replaced by an empty-state illustration + "No data for this timeframe" message, consistent with other GIO empty states.
7. Error state: when the endpoint call fails, shows an error card in place of the chart.
8. Component refreshes when the parent emits `timeframeChange`.
9. Reuses the existing `GioChartPieComponent` (or the closest GIO/chart-js wrapper already present in the console); does not introduce a new charting library.
10. Standalone component, `OnPush`.

---

### US-08 — Dashboard page wiring and layout

**Layer:** Frontend | **Complexity:** M | **Depends on:** US-05, US-06, US-07

**As an** API Publisher,
**I want** the Analytics tab to show all widgets (filter bar, stats cards, pie chart, existing line charts) in the documented layout,
**so that** I have a complete, cohesive analytics dashboard without navigating to multiple screens.

**Acceptance criteria:**
1. The existing `ApiAnalyticsV4Component` (or its host page) is updated to compose: `AnalyticsTimeframeComponent` (Row 1) → stats cards row (Row 2) → two-column Row 3 (pie chart left, status-over-time line chart right) → full-width Row 4 (response-time-over-time chart).
2. `timeframeChange` events from the filter bar are propagated to all child widgets; all widgets re-fetch on change.
3. Existing `ApiAnalyticsRequestStatsComponent`, `ApiAnalyticsResponseStatusRangesComponent`, `ApiAnalyticsResponseStatusOvertimeComponent`, and `ApiAnalyticsResponseTimeOvertimeComponent` still render and remain functional — no regressions.
4. Row 3 uses a CSS grid with `grid-template-columns: 1fr 1fr` on medium+ viewports and stacks vertically on small.
5. The page loads in a single navigation without additional route changes.
6. An "all-empty" state (all widgets return no data) shows a page-level empty-state banner in addition to per-widget empty states.
7. No console errors or Angular CD warnings in the browser during normal operation.
8. The tab label and route remain unchanged.

---

### US-09 — Backend unit tests

**Layer:** Tests (Backend) | **Complexity:** M | **Depends on:** US-01, US-02, US-03

**As a** developer,
**I want** comprehensive JUnit tests for all four analytics query types and for authorization enforcement,
**so that** regressions in the analytics endpoint are caught before merging.

**Acceptance criteria:**
1. Test class `GetApiAnalyticsUseCaseTest` covers COUNT, STATS, GROUP_BY, and DATE_HISTO happy paths — each asserting the correct response type and shape.
2. Tests for COUNT and STATS verify that ES is called with a `value_count` / `stats` aggregation respectively.
3. GROUP_BY test asserts `size` and `order` are forwarded to the `terms` aggregation.
4. DATE_HISTO test asserts that gap-fill produces zeros for empty buckets within the requested range.
5. A test asserts that an unknown `field` value throws a `ValidationException` (or equivalent domain exception).
6. `ApiAnalyticsResourceTest` (REST layer) covers: `403` when permission missing, `400` for each missing-required-param combination (`type`, `from`, `to`, `field` for STATS, `interval` for DATE_HISTO).
7. All tests use the existing mock ES infrastructure pattern (`GraviteeJerseyTest` or equivalent) — no real ES needed.
8. Test coverage for the new `GetApiAnalyticsUseCase` is ≥ 90% line coverage.

---

### US-10 — Frontend component tests

**Layer:** Tests (Frontend) | **Complexity:** M | **Depends on:** US-04 through US-08

**As a** developer,
**I want** Angular unit and integration tests for all new dashboard components,
**so that** the UI remains correct as it evolves and regressions are caught in CI.

**Acceptance criteria:**
1. `ApiAnalyticsV2Service` spec covers all four `getAnalytics` type variants using `HttpClientTestingModule`.
2. `AnalyticsTimeframeComponent` spec covers: initial default selection, click emitting correct `{ from, to }`, and mutual-exclusion of active state.
3. `ApiAnalyticsStatsCardsComponent` spec: uses a stubbed service; asserts loading state, populated state (values rendered), empty state (en-dash displayed), and error state (error icon visible) for each card.
4. `ApiAnalyticsStatusPieComponent` spec: asserts that GROUP_BY response data is rendered as slices, that empty `values` triggers empty state, and that error triggers error state.
5. Dashboard page spec: asserts that all four child widget components are present in the DOM; asserts that simulating `timeframeChange` calls `getAnalytics` on the service for each widget; asserts existing line chart components are not removed.
6. All specs follow the `TestBed` + `HttpClientTestingModule` pattern consistent with existing console test files.
7. No `NO_ERRORS_SCHEMA` used — components are properly imported in test modules.
8. All tests pass in `ng test --watch=false`.

---

## Dependency graph and suggested sprint order

Here's how the stories sequence:---

## Complexity and sizing summary

| Story | Title | Layer | Size | Sprint |
|---|---|---|---|---|
| US-01 | Endpoint skeleton + COUNT | Backend | M | 1 |
| US-04 | Angular service client | Frontend | S | 1 |
| US-02 | STATS + GROUP_BY | Backend | M | 2 |
| US-03 | DATE_HISTO | Backend | M | 2 |
| US-05 | Timeframe filter bar | Frontend | S | 2 |
| US-06 | Stats cards row | Frontend | M | 3 |
| US-07 | HTTP status pie chart | Frontend | M | 3 |
| US-08 | Dashboard wiring and layout | Frontend | M | 3 |
| US-09 | Backend unit tests | Tests | M | 3 |
| US-10 | Frontend component tests | Tests | M | 3 |

---

## Key design decisions embedded in the stories

**Why keep existing endpoints?** US-01 deliberately preserves `getApiAnalyticsRequestCount`, `getResponseStatusRanges`, etc. The existing Angular components (`ApiAnalyticsResponseStatusOvertimeComponent` and `ApiAnalyticsResponseTimeOvertimeComponent`) are explicitly out of scope for rewiring in M1 per the PRD. Deleting the old endpoints would break them.

**Why introduce `GetApiAnalyticsUseCase`?** Gravitee's existing pattern in this module puts HTTP concerns in the `Resource` class and domain logic in a use-case class. The new use case owns the `AnalyticsType` dispatch so the resource layer stays thin. This also makes US-09 testable without an HTTP layer.

**Why discriminated union types in US-04?** The TypeScript discriminated union on `AnalyticsQueryParams` makes it impossible to call `getAnalytics({ type: 'STATS' })` without also providing `field` — preventing a class of bugs that would only surface at runtime as `400` errors from the backend.

**Parallel stream in US-06:** Using `forkJoin` for the four stats cards means the slowest card governs overall load time — but all four resolve together, so the page doesn't render in a janky staggered sequence. The loading skeletons handle the wait gracefully.

**Why no `NO_ERRORS_SCHEMA` in US-10?** Allowing `NO_ERRORS_SCHEMA` in tests is a common shortcut that lets broken template bindings pass silently. Requiring real imports catches integration issues earlier, which is especially important here since this feature composes several new components together.