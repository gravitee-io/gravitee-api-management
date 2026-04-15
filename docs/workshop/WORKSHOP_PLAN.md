# Workshop Plan: V4 API Analytics Dashboard (M1)

> Source of truth for user stories: `docs/workshop/STORIES.md`
> Refinement notes and accept/reject decisions are documented there.

---

## User Stories & Subtasks

### Sprint 1 — Foundation

#### US-01 — Endpoint skeleton + auth + COUNT `[Backend · M]`
- [ ] Create `GET /v2/apis/{apiId}/analytics` in `ApiAnalyticsResource.java`
- [ ] Define `AnalyticsType` enum (`COUNT | STATS | GROUP_BY | DATE_HISTO`)
- [ ] Wire `GetApiAnalyticsUseCase` with type-based dispatcher
- [ ] Implement COUNT query against `*-v4-metrics-*` ES index
- [ ] Enforce `API_ANALYTICS:READ` permission → 403
- [ ] Update OpenAPI spec (path, query params, COUNT response schema)
- [ ] Unit tests: COUNT happy path, 403 on missing permission

#### US-02 — Unified request validation & error model `[Backend · S]` _(depends on US-01)_
- [ ] Centralise validation in `GetApiAnalyticsUseCase` (or `AnalyticsQueryValidator`)
- [ ] Handle: missing/unknown `type`, missing `from`/`to`, `from >= to`, window > 366 days
- [ ] Handle: STATS without `field`, invalid `field` value
- [ ] Handle: DATE_HISTO without `interval`, `interval <= 0`, invalid `order`
- [ ] All 400s use a consistent JSON envelope (reuse existing `ValidationException`/`ProblemDetail`)
- [ ] `ApiAnalyticsResourceTest` covers all 12 validation cases from the ACs

#### US-05 — Angular service: unified analytics client `[Frontend · S]` _(can start against mock from US-01)_
- [ ] Create `ApiAnalyticsV2Service.getAnalytics(apiId, params)` in `src/services-ngx/`
- [ ] Define `AnalyticsQueryParams` as a discriminated union (per type)
- [ ] Define `AnalyticsResponse` discriminated union (`Count | Stats | GroupBy | DateHisto`)
- [ ] Serialise all params as query strings; no request body
- [ ] Preserve existing service methods (`getRequestsCount`, etc.) untouched
- [ ] Export from barrel; unit test all four type variants with `HttpClientTestingModule`
- [ ] Final type-alignment pass after US-02/US-03/US-04 contracts are frozen

---

### Sprint 2 — Aggregation types + Filter UI

#### US-03 — STATS and GROUP_BY support `[Backend · M]` _(depends on US-01, US-02)_
- [ ] Implement `type=STATS` → ES `stats` aggregation; return `{count, min, max, avg, sum}`
- [ ] Zero-doc case: all numeric fields return `0` (not null/absent)
- [ ] Implement `type=GROUP_BY` → ES `terms` aggregation
- [ ] Wire `size` (default 10, max 100) and `order` (ASC|DESC, default DESC)
- [ ] Tie-breaking: stable sort by bucket key ascending
- [ ] `metadata` for unknown field values → `{ "name": "<raw-value>" }` (no throw)
- [ ] `FieldMapper` utility for ES field name mapping
- [ ] Update OpenAPI spec (STATS + GROUP_BY response schemas)
- [ ] Unit tests: STATS happy path, STATS zero-docs, GROUP_BY happy path, tie-breaking, unknown metadata

#### US-04 — DATE_HISTO support `[Backend · M]` _(depends on US-01, US-02 — parallel with US-03)_
- [ ] Implement `type=DATE_HISTO` → ES `date_histogram` + nested `terms` sub-aggregation
- [ ] Return `{ timestamp: [ms…], values: [{ field, buckets, metadata }…] }`
- [ ] Gap-fill: insert `0` for ES buckets with no hits; keep timestamp/buckets arrays aligned
- [ ] Zero-doc case: return `{ timestamp: [], values: [] }` (not 404)
- [ ] Update OpenAPI spec (DATE_HISTO response schema)
- [ ] Unit tests: happy path with gap-fill, zero-doc range, array-length alignment assertion

#### US-06 — Timeframe filter bar component `[Frontend · S]` _(no dependencies — can run in parallel)_
- [ ] Check for existing range-selector component before building from scratch
- [ ] Create standalone `AnalyticsTimeframeComponent` with 5 toggle buttons
- [ ] Default selection: `Last 1 hour`
- [ ] `timeframeChange` output emits `{ from: number, to: number }` (epoch ms, computed at click time)
- [ ] Active button uses GIO design-system tokens; no hard-coded pixel widths
- [ ] `OnPush` change detection
- [ ] Unit tests: default selection, click emits correct `{from, to}`, mutual-exclusion

---

### Sprint 3 — Widgets, Dashboard Assembly & Tests

#### US-07 — Stats cards row `[Frontend · M]` _(depends on US-05, US-06)_
- [ ] Four `gio-card` stats cards in a horizontal row
- [ ] Total Requests → `type=COUNT`, display `response.count`
- [ ] Avg Gateway Response Time → `type=STATS&field=gateway-response-time-ms`, display `avg` as `XX ms`
- [ ] Avg Upstream Response Time → `type=STATS&field=endpoint-response-time-ms`, display `avg` as `XX ms`
- [ ] Avg Content Length → `type=STATS&field=request-content-length`, display `avg` as human-readable bytes
- [ ] All four requests use `forkJoin` (parallel); refresh on `timeframeChange`
- [ ] Loading skeleton per card while in-flight
- [ ] No-data state: show `—` when `count === 0`
- [ ] Partial-failure: failing card shows error icon + tooltip; other cards still show data
- [ ] Standalone, `OnPush`

#### US-08 — HTTP status pie chart widget `[Frontend · M]` _(depends on US-05, US-06)_
- [ ] Create `ApiAnalyticsStatusPieComponent`; call `type=GROUP_BY&field=status&size=10`
- [ ] Use existing `GioChartPieComponent` (or closest GIO/chart-js wrapper) — no new charting lib
- [ ] Status codes visually grouped by class (2xx / 3xx / 4xx / 5xx) using library colour conventions
- [ ] Hover tooltip: status code, count, percentage (library default format)
- [ ] Legend: each status code + count
- [ ] Empty state: GIO empty-state illustration + "No data for this timeframe"
- [ ] Error state: error card in place of chart
- [ ] Refreshes on `timeframeChange`
- [ ] Standalone, `OnPush`

#### US-09a — Dashboard layout and component composition `[Frontend · S]` _(depends on US-06, US-07, US-08)_
- [ ] Update `ApiAnalyticsV4Component` to compose: timeframe bar → stats row → 2-col row (pie + status-overtime chart) → full-width (response-time-overtime chart)
- [ ] Row 3: `grid-template-columns: 1fr 1fr` on medium+, stacked on small
- [ ] Tab label, route path, route guard unchanged
- [ ] No console errors or Angular CD warnings
- [ ] Existing chart components still present in DOM and rendering (`ApiAnalyticsRequestStatsComponent`, `ApiAnalyticsResponseStatusRangesComponent`, etc.)

#### US-09b — Dashboard refresh orchestration, non-regression & empty-page state `[Frontend · M]` _(depends on US-09a)_
- [ ] `timeframeChange` propagates to all new child widgets; all re-fetch on change
- [ ] Timeframe change does not trigger route navigation or full page reload
- [ ] Partial-failure: failing widget shows error state; other widgets show data
- [ ] All-empty state: page-level empty-state banner (in addition to per-widget states)
- [ ] Existing line-chart components continue to function after timeframe change
- [ ] Backward-compat: `getRequestsCount`, `getResponseStatusRanges`, `getResponseStatusOvertime`, `getResponseTimeOvertime` signatures not changed or removed

#### US-10 — Backend tests: all query types, validation & permissions `[Tests · M]` _(depends on US-01–04)_
- [ ] `GetApiAnalyticsUseCaseTest`: COUNT, STATS, GROUP_BY, DATE_HISTO happy paths
- [ ] STATS zero-docs: all numerics are `0`
- [ ] GROUP_BY: `size` + `order` forwarded; tie-breaking stability asserted
- [ ] DATE_HISTO: gap-fill produces zeros; array length equality asserted
- [ ] `ApiAnalyticsResourceTest`: 403 on missing permission, all 12 validation 400 cases
- [ ] Auth edge case: API-access user without `API_ANALYTICS:READ` → 403
- [ ] Unknown `field` → `ValidationException` mapped to 400
- [ ] All tests use existing mock ES infrastructure (no real ES)

#### US-11a — Frontend unit tests: service and widget components `[Tests · M]` _(depends on US-05–08)_
- [ ] `ApiAnalyticsV2Service` spec: all four type variants; existing methods still present + callable
- [ ] `AnalyticsTimeframeComponent` spec: default selection, click output, mutual-exclusion
- [ ] `ApiAnalyticsStatsCardsComponent` spec: loading / populated / empty / error states per card; partial-failure scenario
- [ ] `ApiAnalyticsStatusPieComponent` spec: GROUP_BY data renders; empty values → empty state; error → error card
- [ ] All specs use `TestBed` + `HttpClientTestingModule`; no `NO_ERRORS_SCHEMA`
- [ ] All pass in `ng test --watch=false`

#### US-11b — Frontend integration tests: dashboard & non-regression `[Tests · M]` _(depends on US-09a, US-09b)_
- [ ] Dashboard page spec: all four new child widget components present in DOM
- [ ] Simulate `timeframeChange`; assert `getAnalytics` called for each new widget
- [ ] Non-regression: existing line-chart components in DOM; existing service methods called correctly
- [ ] Partial-failure scenario: one widget 500 → error state; others show data
- [ ] All-empty scenario: page-level empty-state banner visible
- [ ] All specs use `TestBed`; no `NO_ERRORS_SCHEMA`

#### US-12 — NFR validation and instrumentation `[Cross-cutting · S]` _(depends on US-01–04, US-09b)_
- [ ] Load/perf test script (JMeter / k6) covering COUNT, STATS, GROUP_BY, DATE_HISTO; p95 < 2 s
- [ ] Degraded-ES scenario test: ES mock returns after 3 s timeout → endpoint returns 503 (no hung connection); frontend shows per-widget error state (no frozen spinner)
- [ ] Verify all four widgets refresh within the same event cycle on timeframe change
- [ ] Write `NFR_RESULTS.md` alongside the test scripts

---

## Implementation Order

```
US-01  (Backend: endpoint skeleton + COUNT)
  └─► US-02  (Backend: validation & error model)
        ├─► US-03  (Backend: STATS + GROUP_BY)           ─┐  parallel
        └─► US-04  (Backend: DATE_HISTO)                  ─┘

US-05  (Frontend: Angular service)  ← start draft against US-01 mock
  + US-06  (Frontend: timeframe filter bar)              ─┐  parallel; no backend dep
                                                           ▼
                                             US-07  (Frontend: stats cards row)   ─┐ parallel
                                             US-08  (Frontend: status pie chart)  ─┘
                                                           ▼
                                             US-09a (Frontend: dashboard layout)
                                                           ▼
                                             US-09b (Frontend: refresh + non-regression)
                                                           │
                             US-10  (Backend tests) ───────┤  parallel
                             US-11a (Frontend unit tests) ─┘
                                                           ▼
                                             US-11b (Frontend integration tests)
                                             US-12  (NFR validation)
```

---

## Time Tracking

| Phase | Story/Stories | Planned | Actual |
|-------|---------------|---------|--------|
| Phase 1 — Decomposition | — | 30–45 min | ___ |
| Phase 2 — Backend Sprint 1 | US-01, US-02 | 60–75 min | ___ |
| Phase 2 — Backend Sprint 2 | US-03, US-04 | 60–75 min | ___ |
| Phase 2 — Frontend Sprint 1 | US-05 | 20–30 min | ___ |
| Phase 2 — Frontend Sprint 2 | US-06 | 20–30 min | ___ |
| Phase 2 — Frontend Sprint 3 | US-07, US-08, US-09a, US-09b | 60–75 min | ___ |
| Phase 3 — Tests | US-10, US-11a, US-11b, US-12 | 30–45 min | ___ |
| Phase 4 — Documentation | — | 15–20 min | ___ |
| Phase 5 — Deploy & Test | — | 20–30 min | ___ |
| Phase 6 — PR | — | 15 min | ___ |

---

## Notes, Concerns & Open Questions

### Design decisions to keep in mind
- **Existing endpoints are untouched.** `getRequestsCount`, `getResponseStatusRanges`, etc. in the Angular service must not be removed or renamed. The old Angular line-chart components depend on them and are explicitly out of scope for M1.
- **Resource layer stays thin.** All business logic lives in `GetApiAnalyticsUseCase`. No null-checks or dispatching in `ApiAnalyticsResource.java`.
- **No `NO_ERRORS_SCHEMA` in tests.** Broken template bindings must surface; all component imports must be real.
- **Type freeze timing.** `AnalyticsQueryParams` / `AnalyticsResponse` types in US-05 should be considered draft until US-02/03/04 backends are merged. Do a final alignment pass before merging US-07/US-08/US-09.

### Concerns
- **ES index name.** Confirm the exact `*-v4-metrics-*` index pattern with the platform team before US-01 lands — a wrong index means all queries silently return zero.
- **Existing `GioChartPieComponent`.** Verify this component (or equivalent) exists in the console before starting US-08. If it doesn't, US-08 sizing may need to be bumped to L.
- **Timeframe component duplication.** US-06 implementation note says to check for existing patterns first. Spend 15 minutes searching before building new.
- **DATE_HISTO bucket volume.** The >1440-bucket guard is explicitly _not_ a must-have AC. Do not add it unless load testing (US-12) reveals it necessary.
- **p95 < 2 s NFR.** US-12 is the gate for this. Don't defer the perf script to after the PR is raised.

### Open questions
- [ ] Which ES aggregation builder classes does the existing codebase use? (`SearchRequestBuilder`, `AggregationBuilder` — confirm the exact imports before writing US-01.)
- [ ] Is there a shared `ValidationException` / `ProblemDetail` pattern already in use for 400 responses? (Confirm before US-02 to avoid inventing a new format.)
- [ ] What `field` values are in the allowed set for `type=STATS`? (Need to define the `FieldMapper` whitelist for US-03.)
- [ ] Does `ApiAnalyticsV4Component` already exist, or does it need to be created? (US-09a.)
- [ ] Is there a GIO empty-state component to use for the all-empty page-level banner? (US-09b.)
