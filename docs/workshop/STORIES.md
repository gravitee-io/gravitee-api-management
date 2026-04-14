# Story Map: V4 API Analytics Dashboard (M1) — Refined

The work splits across **4 Backend stories**, **6 Frontend stories**, and **3 Testing/NFR stories** — 13 stories total. Dependency ordering is documented per story; US-03 and US-04 can be developed in parallel after US-01 ships.

---

## Backend Stories

---

### US-01 — Endpoint skeleton + auth + COUNT

**Layer:** Backend | **Complexity:** M | **Sprint:** 1 | **Depends on:** nothing

**As an** API Publisher,
**I want** a single `GET /v2/apis/{apiId}/analytics?type=COUNT&from=…&to=…` endpoint,
**so that** I can retrieve the total number of requests for my API in a given time window without hitting multiple bespoke endpoints.

**Acceptance criteria:**
1. `GET /v2/apis/{apiId}/analytics` exists in `ApiAnalyticsResource.java` alongside (not replacing) existing endpoints.
2. `type=COUNT` with valid `from`/`to` returns `{ "type": "COUNT", "count": <long> }`.
3. Caller without `API_ANALYTICS:READ` permission receives `403 Forbidden`; a user who has API access but not `API_ANALYTICS:READ` in a mixed-role context also receives `403` (not `404`).
4. Endpoint queries the `*-v4-metrics-*` ES index (not the v2 index).
5. A new `AnalyticsType` enum (`COUNT | STATS | GROUP_BY | DATE_HISTO`) drives a dispatcher inside `GetApiAnalyticsUseCase`.
6. OpenAPI spec updated with the new path, all query params documented, and the `COUNT` response schema added.
7. Basic unit test covers: COUNT happy path, 403 on missing permission. (Full validation and error-model tests are in US-02; comprehensive test coverage in US-10.)

**Implementation notes:**
Wire a new `GetApiAnalyticsUseCase` that accepts an `AnalyticsQuery` record and delegates to the right ES query builder based on `type`. Reuse the existing `SearchRequestBuilder` / `AggregationBuilder` infrastructure. The resource layer stays thin — no business logic beyond parameter extraction.

---

### US-02 — Unified request validation & error model

**Layer:** Backend | **Complexity:** S | **Sprint:** 1 | **Depends on:** US-01

**As a** developer integrating the analytics endpoint,
**I want** consistent, machine-readable `400` responses for all invalid input combinations,
**so that** frontend clients and third-party callers can handle errors uniformly without guessing the error payload shape.

**Acceptance criteria:**
1. Missing `type` → `400` with `{ "message": "type is required", "parameters": ["type"] }`.
2. Unknown `type` value → `400` with the list of valid values in the error body.
3. Missing `from` or `to` → `400`.
4. `from >= to` → `400` with a descriptive message (not a 500).
5. Extremely large time windows (e.g. `to - from > 366 days`) → `400` with a message indicating the max supported range.
6. Future `to` timestamps are accepted (valid use case for monitoring dashboards).
7. `type=STATS` without `field` → `400`.
8. `field` not in the allowed set → `400` with the allowed list in the error body.
9. `type=DATE_HISTO` without `interval` → `400`.
10. `interval <= 0` → `400`.
11. `order` with an invalid value (not `ASC`/`DESC`) → `400`.
12. All `400` responses share the same JSON envelope shape; no endpoint returns a `400` with an HTML body or an empty body.
13. `ApiAnalyticsResourceTest` covers each of the above parameter combinations.

**Implementation notes:**
Centralise validation in `GetApiAnalyticsUseCase` (or a dedicated `AnalyticsQueryValidator`) before any ES query is issued. Do not scatter null-checks across the resource and use-case layers. The error envelope reuses whatever `ValidationException` or `ProblemDetail` pattern the existing codebase uses for 400 responses — do not invent a new format.

---

### US-03 — STATS and GROUP_BY support

**Layer:** Backend | **Complexity:** M | **Sprint:** 2 | **Depends on:** US-01, US-02

**As an** API Publisher,
**I want** the unified endpoint to support `type=STATS` and `type=GROUP_BY`,
**so that** I can retrieve average response times and a ranked breakdown of HTTP status codes from a single endpoint contract.

**Acceptance criteria:**
1. `type=STATS&field=gateway-response-time-ms` returns `{ "type": "STATS", "count": …, "min": …, "max": …, "avg": …, "sum": … }`.
2. `type=STATS` with `count=0` (no data in range) returns the STATS envelope with all numeric fields as `0` (not null, not absent).
3. `type=GROUP_BY&field=status` returns `{ "type": "GROUP_BY", "values": { "200": …, … }, "metadata": { "200": {"name":"200"}, … } }`.
4. `size` query param (default 10, max 100) controls number of buckets.
5. `order` param (`ASC` | `DESC`) is forwarded to the aggregation; defaults to `DESC`. Ties in bucket counts are broken deterministically (by bucket key ascending) so response order is stable across identical requests.
6. GROUP_BY `metadata` for unknown or non-status field values (e.g. custom plan IDs) returns `{ "name": "<raw-value>" }` rather than throwing.
7. Both query types honour `API_ANALYTICS:READ`.
8. OpenAPI spec updated with `STATS` and `GROUP_BY` response schemas.
9. Unit tests cover: STATS happy path, STATS zero-docs, GROUP_BY happy path, GROUP_BY tie-breaking, GROUP_BY unknown metadata key.

**Implementation notes:**
Map `field` values to ES field names via a `FieldMapper` utility. For STATS use an ES `stats` aggregation. For GROUP_BY use a `terms` aggregation with `size` and `order`. Metadata for HTTP status codes can be generated from bucket keys — no secondary ES query needed for M1.

---

### US-04 — DATE_HISTO support

**Layer:** Backend | **Complexity:** M | **Sprint:** 2 | **Depends on:** US-01, US-02 (parallel with US-03)

**As an** API Publisher,
**I want** the unified endpoint to support `type=DATE_HISTO`,
**so that** I can retrieve time-bucketed request counts broken down by a field for charting trends.

**Acceptance criteria:**
1. `type=DATE_HISTO&field=status&interval=3600000&from=…&to=…` returns a valid `DATE_HISTO` response.
2. Response shape: `{ "type": "DATE_HISTO", "timestamp": [<ms>, …], "values": [{ "field": "200", "buckets": [<count>, …], "metadata": {"name": "200"} }, …] }`.
3. The `timestamp` array and each `buckets` array have equal length.
4. ES bucket gaps (intervals with zero hits) are filled with `0` so timestamps and buckets remain aligned.
5. When ES returns zero documents for the range, the endpoint returns `{ "type": "DATE_HISTO", "timestamp": [], "values": [] }` (not `404`).
6. OpenAPI spec updated with `DATE_HISTO` response schema.
7. Unit tests cover: happy path with gap-fill, zero-document range, timestamp/buckets length alignment assertion.

**Implementation notes:**
Use an ES `date_histogram` aggregation with a nested `terms` sub-aggregation. The outer aggregation drives the `timestamp` array; inner buckets populate per-series `buckets`. Gap-fill by iterating the expected timestamp range at the given interval and inserting zeros where ES returned no bucket.

> **Optional guardrail (not must-have AC):** If `interval` produces more than 1 440 buckets over the requested range, consider returning `400` to protect ES performance. This is not required for M1 and should be added only if load testing reveals it necessary — treating it as a must-have AC risks blocking valid long-range queries.

---

## Frontend Stories

---

### US-05 — Angular service: unified analytics client

**Layer:** Frontend | **Complexity:** S | **Sprint:** 1 | **Depends on:** US-01 contract (can start with mock; freeze types after US-02/US-03/US-04 contract stabilises)

**As a** frontend engineer,
**I want** `ApiAnalyticsV2Service` to expose a single typed `getAnalytics(apiId, params)` method,
**so that** all dashboard widgets share one consistent HTTP client method.

**Acceptance criteria:**
1. `ApiAnalyticsV2Service.getAnalytics(apiId: string, params: AnalyticsQueryParams): Observable<AnalyticsResponse>` exists.
2. `AnalyticsQueryParams` is a discriminated union typed per `type` variant so TypeScript prevents field-less STATS calls at compile time.
3. `AnalyticsResponse` is a discriminated union (`CountResponse | StatsResponse | GroupByResponse | DateHistoResponse`) matching the OpenAPI contract from US-01–04.
4. The method calls `GET /v2/apis/{apiId}/analytics` with all params serialised as query strings; no request body.
5. Existing service methods (`getRequestsCount`, `getResponseStatusRanges`, etc.) are **not removed** — they remain intact so existing components don't break. This is a hard backward-compatibility requirement.
6. Unit test covers each of the four type variants with `HttpClientTestingModule`.
7. Service is in `src/services-ngx/` and exported from its barrel.

**Note on dependency:** US-05 can begin development against a typed mock based on US-01's response shape. The discriminated union types should be considered draft until US-02/US-03/US-04 are complete and the contract is frozen. A final type-alignment pass against the OpenAPI spec is required before US-07/US-08/US-09 are merged.

---

### US-06 — Timeframe filter bar component

**Layer:** Frontend | **Complexity:** S | **Sprint:** 2 | **Depends on:** nothing (pure UI component)

**As an** API Publisher,
**I want** a horizontal filter bar with predefined time range buttons (Last 5 min, 1 hour, 24 hours, 7 days, 30 days),
**so that** I can quickly switch the time window for all widgets at once.

**Acceptance criteria:**
1. A new standalone `AnalyticsTimeframeComponent` renders five labelled toggle buttons; exactly one is active at a time.
2. Default selected range is `Last 1 hour` on first load.
3. Selecting a range emits a `timeframeChange` output carrying `{ from: number, to: number }` in epoch milliseconds computed relative to `Date.now()` at selection time.
4. Active button has a visually distinct selected state using existing GIO design-system tokens.
5. Component uses `OnPush` change detection.
6. Component has no hard-coded pixel widths; it is responsive within the dashboard container.
7. Unit test covers: default selection, button click emitting correct `{ from, to }`, and mutual-exclusion of active state.

**Implementation notes:**
Check whether an existing timeframe/range-selector component pattern exists in the console before building from scratch — if a sufficiently similar component is found, prefer extending or wrapping it over creating a net-new component.

> The original story prescribed "Angular signals, no BehaviorSubject." This internal state constraint has been removed. Use whatever reactive pattern fits the component's needs; `OnPush` remains required for change-detection performance.

---

### US-07 — Stats cards row

**Layer:** Frontend | **Complexity:** M | **Sprint:** 3 | **Depends on:** US-05, US-06

**As an** API Publisher,
**I want** a row of four stat cards showing Total Requests, Avg Gateway Response Time, Avg Upstream Response Time, and Avg Content Length,
**so that** I can see the key health metrics for my API at a glance.

**Acceptance criteria:**
1. Four `gio-card` stats cards render in a single horizontal row below the timeframe filter.
2. "Total Requests" calls `type=COUNT` and displays `response.count`.
3. "Avg Gateway Response Time" calls `type=STATS&field=gateway-response-time-ms` and displays `response.avg` formatted as `XX ms`.
4. "Avg Upstream Response Time" calls `type=STATS&field=endpoint-response-time-ms` and displays `response.avg` as `XX ms`.
5. "Avg Content Length" calls `type=STATS&field=request-content-length` and displays `response.avg` as a human-readable byte string (e.g. `4.2 KB`).
6. All four cards issue their requests in parallel (`forkJoin`) and refresh together when `timeframeChange` fires.
7. Each card shows a loading skeleton while the request is in flight.
8. Each card shows `—` (en-dash) when `response.count === 0` (no data in range).
9. Each card shows an error icon + tooltip when its individual API call fails; the other cards continue to display their data (partial failure does not blank the whole row).
10. Component is standalone, uses `OnPush`.

---

### US-08 — HTTP status pie chart widget

**Layer:** Frontend | **Complexity:** M | **Sprint:** 3 | **Depends on:** US-05, US-06

**As an** API Publisher,
**I want** a pie chart breaking down HTTP responses by status code,
**so that** I can see the proportion of 2xx, 4xx, and 5xx responses at a glance.

**Acceptance criteria:**
1. A new `ApiAnalyticsStatusPieComponent` calls `type=GROUP_BY&field=status&size=10`.
2. Status codes are visually grouped by class (2xx / 3xx / 4xx / 5xx) using the existing chart library's colour conventions — no custom colour palette is required for M1.
3. A tooltip on hover shows the status code, count, and percentage using the existing chart library's default tooltip format.
4. A legend lists each status code and its count.
5. Empty state: when `response.values` is empty, the chart area is replaced by a GIO-standard empty-state illustration + "No data for this timeframe" message.
6. Error state: when the endpoint call fails, shows an error card in place of the chart.
7. Component refreshes when the parent emits `timeframeChange`.
8. Reuses the existing `GioChartPieComponent` (or the closest GIO/chart-js wrapper present); does not introduce a new charting library.
9. Standalone component, `OnPush`.

> Color ramp specifics (green/blue/amber/red ramps per status class) and exact tooltip format strings were removed from the original ACs — they are not in the PRD and are too design-detailed for M1. These can be addressed as a UX-enhancement story after M1 ships.

---

### US-09a — Dashboard layout and component composition

**Layer:** Frontend | **Complexity:** S | **Sprint:** 3 | **Depends on:** US-06, US-07, US-08

**As an** API Publisher,
**I want** the Analytics tab to display all widgets in the documented layout,
**so that** I see a coherent dashboard in a single navigation.

**Acceptance criteria:**
1. The existing `ApiAnalyticsV4Component` (or its host page) is updated to compose: `AnalyticsTimeframeComponent` (Row 1) → stats cards row (Row 2) → two-column Row 3 (pie chart left, status-over-time chart right) → full-width Row 4 (response-time-over-time chart).
2. Row 3 uses `grid-template-columns: 1fr 1fr` on medium+ viewports and stacks vertically on small.
3. The page loads in a single navigation without additional route changes.
4. The tab label, route path, and route guard remain unchanged.
5. No console errors or Angular CD warnings during normal operation.
6. Existing `ApiAnalyticsRequestStatsComponent`, `ApiAnalyticsResponseStatusRangesComponent`, `ApiAnalyticsResponseStatusOvertimeComponent`, and `ApiAnalyticsResponseTimeOvertimeComponent` are still present in the DOM and render correctly — no regressions. The existing service methods they rely on are not removed.

---

### US-09b — Dashboard refresh orchestration, non-regression, and empty-page state

**Layer:** Frontend | **Complexity:** M | **Sprint:** 3 | **Depends on:** US-09a

**As an** API Publisher,
**I want** all widgets to refresh simultaneously when I change the timeframe, and to see a clear page-level state when nothing has data,
**so that** the dashboard always reflects a coherent view of the selected time window.

**Acceptance criteria:**
1. `timeframeChange` events from the filter bar propagate to all child widgets; all new widgets re-fetch on change.
2. Changing the timeframe does not trigger a full page reload or route navigation.
3. If a single widget's query fails and others succeed, the page degrades gracefully: the failing widget shows its error state while other widgets display their data normally.
4. An "all-empty" state (all new widgets return no data) shows a page-level empty-state banner in addition to per-widget empty states.
5. Existing line chart components continue to function after a timeframe change (they use their own data-fetch mechanisms; this story must not break them).
6. Backward-compatibility assertion: `getRequestsCount`, `getResponseStatusRanges`, `getResponseStatusOvertime`, and `getResponseTimeOvertime` service methods are not modified in signature or removed.

---

## Testing & NFR Stories

---

### US-10 — Backend tests: all query types, validation, and permissions

**Layer:** Tests (Backend) | **Complexity:** M | **Sprint:** 3 | **Depends on:** US-01, US-02, US-03, US-04

**As a** developer,
**I want** comprehensive JUnit tests for all four analytics query types, the validation layer, and authorization enforcement,
**so that** regressions are caught before merging.

**Acceptance criteria:**
1. `GetApiAnalyticsUseCaseTest` covers COUNT, STATS, GROUP_BY, and DATE_HISTO happy paths — each asserting the correct response type and shape.
2. STATS zero-docs case: asserts all numeric fields are `0`.
3. GROUP_BY: asserts `size` and `order` are forwarded to the `terms` aggregation; asserts tie-breaking stability.
4. DATE_HISTO: asserts gap-fill produces zeros for empty buckets within the requested range; asserts `timestamp` and `buckets` array lengths are equal.
5. `ApiAnalyticsResourceTest` covers `403` when permission missing, and every `400` validation case from US-02 AC 1–12.
6. Auth edge case: user with API-level access but without `API_ANALYTICS:READ` receives `403`.
7. An unknown `field` value test asserts a `ValidationException` (or equivalent) is thrown and mapped to `400`.
8. All tests use the existing mock ES infrastructure — no real ES needed.

> The ≥ 90% line-coverage target from the original US-09 has been removed. Coverage is not in the PRD/AC and creates process friction. The scenarios listed above are the mandatory cases; coverage follows from implementing them.

---

### US-11a — Frontend unit tests: service and widget components

**Layer:** Tests (Frontend) | **Complexity:** M | **Sprint:** 3 | **Depends on:** US-05, US-06, US-07, US-08

**As a** developer,
**I want** Angular unit tests for the service and each new widget component,
**so that** individual component contracts are verified in isolation.

**Acceptance criteria:**
1. `ApiAnalyticsV2Service` spec covers all four `getAnalytics` type variants using `HttpClientTestingModule`; asserts existing methods are still present and callable.
2. `AnalyticsTimeframeComponent` spec: initial default selection, click emitting correct `{ from, to }`, mutual-exclusion of active state.
3. `ApiAnalyticsStatsCardsComponent` spec: loading state, populated state (values rendered correctly), empty state (en-dash displayed), error state (error icon visible) for each card; partial-failure scenario (one card errors, others show data).
4. `ApiAnalyticsStatusPieComponent` spec: GROUP_BY data renders as slices; empty `values` triggers empty state; error triggers error card.
5. All specs use `TestBed` + `HttpClientTestingModule`; no `NO_ERRORS_SCHEMA`.
6. All tests pass in `ng test --watch=false`.

---

### US-11b — Frontend integration tests: dashboard and non-regression

**Layer:** Tests (Frontend) | **Complexity:** M | **Sprint:** 3 | **Depends on:** US-09a, US-09b

**As a** developer,
**I want** integration-level Angular tests for the full dashboard page and a non-regression suite for existing chart components,
**so that** cross-component wiring and existing behaviour are verified together.

**Acceptance criteria:**
1. Dashboard page spec asserts all four child widget components are present in the DOM.
2. Simulating `timeframeChange` asserts `getAnalytics` is called for each new widget.
3. Existing line-chart components (`ApiAnalyticsResponseStatusOvertimeComponent`, `ApiAnalyticsResponseTimeOvertimeComponent`) are present in the DOM and their existing service methods are called correctly — explicit non-regression assertion.
4. Partial-failure scenario: one widget's HTTP request returns `500`; asserts that widget shows error state and other widgets still display data.
5. All-empty scenario: all widgets return empty data; asserts page-level empty-state banner is visible.
6. All specs use `TestBed`; no `NO_ERRORS_SCHEMA`.

---

### US-12 — NFR validation and instrumentation

**Layer:** Cross-cutting | **Complexity:** S | **Sprint:** 3 | **Depends on:** US-01–US-04 (backend), US-09b (frontend wired)

**As a** platform engineer,
**I want** the analytics endpoint and dashboard verified against the PRD's non-functional requirements,
**so that** we can confirm <2 s response times and graceful degradation before shipping M1.

**Acceptance criteria:**
1. A load/perf test (can be a JMeter, k6, or equivalent script in the repo) exercises the `COUNT`, `STATS`, `GROUP_BY`, and `DATE_HISTO` endpoints against a representative dataset; p95 latency must be < 2 s.
2. A degraded-ES scenario test (e.g. using an ES mock that returns after 3 s timeout) asserts the endpoint returns a `503` or timeout error — not a hung connection — and the frontend displays a per-widget error state rather than a frozen spinner.
3. All four widgets refresh together: a test or manual verification script confirms that changing the timeframe triggers all four widget queries within the same event cycle (no staggered refresh).
4. Findings documented in a short `NFR_RESULTS.md` alongside the test scripts.

---

## Dependency graph

```
US-01 ──────────────────────────────────────────────────────────────────────► US-05 (draft)
  │                                                                                │
  └─► US-02 ──┬─► US-03 ─────────────────────────────────────────────────────────┤
               └─► US-04 (parallel with US-03) ────────────────────────────────► US-05 (freeze)
                                                                                   │
                                                           US-06 (independent) ───►│
                                                                                   ▼
                                                                        US-07, US-08 (parallel)
                                                                                   │
                                                                                   ▼
                                                                              US-09a
                                                                                   │
                                                                                   ▼
                                                                              US-09b
                                                                                   │
                                                              US-10, US-11a ───────┤
                                                                                   ▼
                                                                              US-11b, US-12
```

---

## Complexity and sizing summary

| Story | Title | Layer | Size | Sprint |
|---|---|---|---|---|
| US-01 | Endpoint skeleton + auth + COUNT | Backend | M | 1 |
| US-02 | Unified request validation & error model | Backend | S | 1 |
| US-05 | Angular service: unified analytics client | Frontend | S | 1 |
| US-03 | STATS + GROUP_BY | Backend | M | 2 |
| US-04 | DATE_HISTO | Backend | M | 2 |
| US-06 | Timeframe filter bar | Frontend | S | 2 |
| US-07 | Stats cards row | Frontend | M | 3 |
| US-08 | HTTP status pie chart | Frontend | M | 3 |
| US-09a | Dashboard layout + composition | Frontend | S | 3 |
| US-09b | Dashboard refresh orchestration + non-regression | Frontend | M | 3 |
| US-10 | Backend tests | Tests | M | 3 |
| US-11a | Frontend unit tests: service + widgets | Tests | M | 3 |
| US-11b | Frontend integration tests: dashboard + non-regression | Tests | M | 3 |
| US-12 | NFR validation and instrumentation | Cross-cutting | S | 3 |

---

## Key design decisions

**Why keep existing endpoints?** US-05 deliberately preserves `getRequestsCount`, `getResponseStatusRanges`, etc. The existing Angular line-chart components are explicitly out of scope for rewiring in M1. Deleting the old endpoints would break them with no user-visible gain.

**Why introduce `GetApiAnalyticsUseCase`?** Gravitee's existing pattern puts HTTP concerns in the `Resource` class and domain logic in use-case classes. The new use case owns the `AnalyticsType` dispatch so the resource layer stays thin and US-10 is testable without an HTTP layer.

**Why discriminated union types in US-05?** The TypeScript discriminated union on `AnalyticsQueryParams` makes it impossible to call `getAnalytics({ type: 'STATS' })` without also providing `field`, preventing a class of bugs that would only surface as runtime `400` errors.

**Why `forkJoin` in US-07?** Parallel fetching means the slowest card governs total load time, but all cards resolve together — no janky staggered rendering. Loading skeletons handle the wait gracefully.

**Why no `NO_ERRORS_SCHEMA` in US-11?** Allowing `NO_ERRORS_SCHEMA` lets broken template bindings pass silently. Requiring real component imports in tests catches integration failures early, which matters especially when composing several new components together.

---

## Refinement Notes

This section documents every change made from the original `docs/Stories.md` and the reasoning for each accept/reject decision.

### Accepted changes

**[ACCEPTED] New US-02: Unified request validation & error model**
The original stories scattered validation ACs across US-01/02/03. This created fragmented testability and meant no single story "owned" the error contract. A dedicated story makes the 400-error surface a first-class deliverable with its own test coverage. Edge cases added: `from >= to`, time windows > 366 days, `interval <= 0`, invalid `order` value.

**[ACCEPTED] Split US-08 → US-09a + US-09b**
The original US-08 mixed structural layout work (grid, component placement) with behavioral orchestration (event propagation, partial failure, all-empty state). These have different implementation risks and reviewers. US-09a delivers a shippable layout; US-09b adds behavior on top of it.

**[ACCEPTED] Split US-10 → US-11a + US-11b**
Service unit tests, widget component tests, and full dashboard integration tests are genuinely different scopes with different dependencies. Splitting reduces PR size and allows widget tests (US-11a) to be merged as soon as individual widgets ship.

**[ACCEPTED] US-03 and US-04 as parallel (both depend on US-01 only)**
DATE_HISTO does not need STATS/GROUP_BY to be implemented first — they are independent aggregation types. Running them in parallel compresses Sprint 2. Both still depend on US-02 (validation) being merged first so the validation layer is in place.

**[ACCEPTED] Remove US-03 bucket-limit rule as must-have AC**
The >1440-bucket guard is not in the PRD and can block valid long-range queries. Retained as an optional guardrail note in US-04's implementation notes.

**[ACCEPTED] Remove "no BehaviorSubject" constraint from US-06**
The PRD requires OnPush and signals broadly, but prescribing internal reactive primitives is over-specification. `OnPush` is retained; internal state management is left to the implementer.

**[ACCEPTED] Relax US-08 (now US-08) color ramp and tooltip specifics**
Exact green/blue/amber/red ramps and specific tooltip format strings are not in the PRD. Reduced to "use existing chart library conventions" for M1; UX-enhancement pass deferred.

**[ACCEPTED] Remove US-09 (now US-10) ≥ 90% line-coverage target**
Line-coverage targets are not in the PRD, are not tied to any AC, and create CI friction (e.g. blocking merge for an unrelated uncovered utility method). Replaced with mandatory scenario-based test cases tied directly to ACs.

**[ACCEPTED] New US-12: NFR validation and instrumentation**
The PRD has hard NFRs (<2 s p95, graceful degradation, all widgets refresh together). No original story included measurable validation tasks for these. US-12 is scoped deliberately small (perf test script + degraded-ES scenario + documentation) to avoid over-engineering.

**[ACCEPTED] US-05 dependency clarified**
US-05 (Angular service) now explicitly notes it can start against a mock from US-01, but types should be frozen only after US-02/US-03/US-04 contract stabilises. This avoids costly type-rework mid-sprint.

**[ACCEPTED] Edge cases added to existing stories**
- Auth edge case (API access without `API_ANALYTICS:READ`) → US-01 AC 3, US-10 AC 6
- Partial-failure behavior (one widget fails, others succeed) → US-09b AC 3, US-07 AC 9, US-11b AC 4
- No-data semantics (STATS zero-docs returns 0 not null) → US-03 AC 2, US-10 AC 2
- Metadata robustness for unknown GROUP_BY values → US-03 AC 6
- Ordering determinism for GROUP_BY ties → US-03 AC 5
- Backward-compat assertion (existing methods not removed) → US-05 AC 5, US-09b AC 6, US-11a AC 1, US-11b AC 3

### Rejected changes

**[REJECTED] Split US-01 into US-01a (skeleton) + US-01b (dispatcher/OpenAPI)**
The endpoint skeleton, `AnalyticsType` enum, use-case dispatcher, permission annotation, and OpenAPI update for COUNT are all tightly coupled to the same class hierarchy. Splitting produces an artificial half-story (US-01a) that cannot be demonstrated in a sprint review — the dispatcher and OpenAPI are what make the endpoint testable and contractually usable by frontend. US-01 is already sized "M" which is deliverable in one increment. Rejected in favour of keeping US-01 coherent.

**[REJECTED] Merge US-06 (timeframe filter bar) into US-09a**
The timeframe component is a reusable, independently testable piece of UI. Keeping it as a standalone story allows it to be developed and reviewed before US-09a starts, and gives US-07/US-08 a clear component to wire against. The reviewer's concern about "duplicating existing behavior" is addressed by an implementation note to check for existing patterns first — but the story itself is valid regardless of whether it ends up as a new component or a light wrapper.

**[REJECTED] Merge US-05 (Angular service) into US-07 or US-08**
The service is a shared dependency for US-07, US-08, US-09a, and US-09b. Merging it into one widget story would serialize all widget development on that one PR, eliminating parallelism. Keeping it standalone lets the service be merged first and all three widget stories developed in parallel against it.

**[REJECTED] Separate "OpenAPI contract finalization" story**
The reviewer's suggestion for a standalone US-05 (backend) dedicated to OpenAPI finalization was rejected because OpenAPI updates are deliverable artifacts of each backend story (US-01/03/04 each include an OpenAPI AC). A standalone "finalization" story would either duplicate work already done or create a documentation-only story with no implementation — poor sprint hygiene. Contract stability is enforced by the type-freeze note in US-05 (frontend) instead.
