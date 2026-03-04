# V4 API Analytics Dashboard – Pull Request Description

Use the content below as the GitHub PR description. Reference: **APIM-7991** (epic).

---

## Summary

This PR implements the **V4 API Analytics Dashboard** for HTTP Proxy APIs: a unified Management API v2 analytics endpoint and Console dashboard that show total requests, average response times, average content length, and HTTP status distribution. Data is read from the **v4-metrics** Elasticsearch index only. Existing analytics endpoints and line charts (response status over time, response time over time) remain unchanged and continue to work.

### Backend
- **Unified endpoint:** `GET /management/v2/environments/{envId}/apis/{apiId}/analytics` with query param `type` = `COUNT` | `STATS` | `GROUP_BY` | `DATE_HISTO`, plus `from`/`to`, and when applicable `field`, `interval`, `size`, `order`.
- **Authorization:** `API_ANALYTICS:READ` required; 403 when missing; 400 for invalid or missing parameters.
- **Query service:** `AnalyticsQueryService` (and `AnalyticsQueryServiceImpl`) delegates to the existing v4 `AnalyticsRepository` for COUNT, STATS, GROUP_BY, and DATE_HISTO over the v4-metrics index.
- **Validation:** `type`/`from`/`to` required; `from` < `to`; `field` required for STATS/GROUP_BY/DATE_HISTO; `interval` required for DATE_HISTO (1000–1000000000 ms).

### Frontend
- **Console:** APIs → [V4 API] → API Traffic → Analytics (existing tab). Dashboard uses the unified endpoint for stats cards and HTTP status pie; line charts still use existing endpoints.
- **Stats cards:** Total Requests (COUNT), Avg Gateway Response Time, Avg Upstream Response Time, Avg Content Length (3× STATS).
- **HTTP Status:** Pie chart fed from GROUP_BY on `status`.
- **States:** Loading, analytics disabled (empty state with “Enable Analytics”), no data (“No analytics data for this period”), error (“Analytics unavailable”).
- **Timeframe:** Existing filters bar (predefined ranges + custom From/To + Refresh).

---

## Acceptance criteria (APIM-7991)

| # | Criterion | Status |
|---|-----------|--------|
| 1 | [Backend] GET .../analytics returns COUNT data (total hits) | ✅ Done |
| 2 | [Backend] Endpoint returns STATS (min/max/avg/sum for a field) | ✅ Done |
| 3 | [Backend] Endpoint returns GROUP_BY (top-N by field + metadata) | ✅ Done |
| 4 | [Backend] Endpoint returns DATE_HISTO (time-bucketed histogram) | ✅ Done |
| 5 | [Backend] Endpoint enforces API_ANALYTICS:READ | ✅ Done |
| 6 | [Frontend] Angular service calls unified endpoint for all 4 query types | ✅ Done |
| 7 | [Frontend] Stats cards: total requests, avg gateway/upstream RT, avg content length | ✅ Done |
| 8 | [Frontend] Dashboard shows HTTP status pie chart | ✅ Done (see [known limitation](#known-limitation) below) |
| 9 | [Frontend] Existing line charts not broken | ✅ Done |
| 10 | [Frontend] Empty state when no analytics data | ✅ Done |
| 11 | [Tests] Unit tests for all backend analytics query types | ✅ Done |
| 12 | [Tests] Angular component tests for dashboard and widgets | ✅ Done |

**Known limitation (AC#8):** The HTTP status pie is fed from GROUP_BY on `status` (raw codes 200, 201, 404, 500, etc.). The shared pie component maps each to a label (2xx, 4xx, 5xx) but does not aggregate by status class, so multiple slices can share the same label (e.g. two “2xx” segments). A follow-up can aggregate into 1xx/2xx/3xx/4xx/5xx before rendering.

---

## Testing approach

### Unit tests
- **Backend – Resource:** `ApiAnalyticsResourceTest.UnifiedV4Analytics`: COUNT (200 + zero), STATS (200 + 400 when field missing), GROUP_BY (200 + 400 when field missing), DATE_HISTO (200 + 400 when field/interval missing or interval out of range), 403 when no API_ANALYTICS:READ, 400 for missing/invalid `type`, `from`/`to`, and `from` ≥ `to`.
- **Backend – Service:** `AnalyticsQueryServiceImplTest`: `searchV4AnalyticsCount` (empty, map from repository, correct query), `searchV4AnalyticsStats` (empty, map, query), `searchV4AnalyticsGroupBy` (empty, map, query), `searchV4AnalyticsDateHisto` (empty, map, query). Repository is mocked.
- **Frontend – Service:** `api-analytics-v2.service.spec.ts`: `getV4Analytics` for COUNT, STATS, GROUP_BY, DATE_HISTO (request params and response handling), and error propagation.
- **Frontend – Component:** `api-analytics-proxy.component.spec.ts`: loading, analytics disabled empty panel, request stats from unified (COUNT + STATS), empty state when count 0, HTTP Status pie from GROUP_BY, refresh re-fetch, error state when one request fails, query params time range.

### Integration tests
- No new integration test suite for the unified analytics endpoint (no E2E against real Elasticsearch in this PR). Existing analytics-related integration tests (e.g. logging, metrics, tracing) are unchanged.
- The v4-metrics index and repository layer are exercised by existing integration/functional tests where the full stack is run.

### Manual testing
- **Docs:** `docs/workshop/MANUAL_TEST_REBUILD.md` describes how to rebuild and restart the Management API and Console to pick up changes and manually verify the dashboard (timeframe, stats, pie, line charts, empty/error states).
- **Test data:** `scripts/generate-test-data.sh` creates a v4 HTTP Proxy API, deploys it, sends 100+ diverse requests (methods, paths, status codes), and verifies ES count; `docs/workshop/TEST_DATA_PROFILE.md` describes expected widget behaviour for that data.

---

## Design decisions

1. **Single endpoint, multiple response shapes**  
   One `GET .../analytics` with `type` instead of separate paths per metric. Reduces API surface and keeps permission and validation in one place; frontend calls the same URL with different query params.

2. **Stats cards and pie use unified endpoint; line charts keep existing endpoints**  
   Per PRD, “response status over time” and “response time over time” continue to use existing endpoints (`/response-status-overtime`, `/response-time-over-time`). No rewire to DATE_HISTO in this PR, so existing behaviour and contracts are preserved.

3. **Dashboard shows full error if any unified request fails**  
   The proxy uses `forkJoin` for COUNT + 3× STATS + GROUP_BY. If any call fails, the whole dashboard shows “Analytics unavailable” (no partial data). Keeps the UX simple for v1; partial results could be considered later.

4. **No time range → show loader and filters bar**  
   Until the filters bar has set a time range, the dashboard shows a loader so the bar is visible and can set from/to. Avoids blank content and ensures the first emission has a valid range.

5. **Empty state when count === 0**  
   When all unified requests succeed and COUNT is 0, we show “No analytics data for this period” and hide the grid (stats/pie/line charts). Subtitle suggests trying another period.

6. **Interval bounds for DATE_HISTO**  
   `interval` is constrained to 1000–1000000000 ms to avoid unbounded bucket counts or sub-millisecond buckets; 400 with a clear message when out of range.

7. **V4-metrics only**  
   All unified analytics (COUNT, STATS, GROUP_BY, DATE_HISTO) are backed by the repository’s v4-metrics index only, as specified in the PRD.

---

## Out of scope (not in this PR)

- **Query filter parameter** (e.g. filter by application/plan in the request). Backend supports `field=application`, `plan`, etc. for GROUP_BY/STATS; no UI filter dropdowns.
- **URL-persisted timeframe** (time range in query params for deep-linking). Filters bar does not sync to URL.
- **Top Applications / Top Plans widgets.** Not implemented.
- **Rewiring response-time-over-time to DATE_HISTO.** Line chart still uses existing endpoint.
- **Custom dashboards, platform-level analytics, export.** Not in scope.
- **V4 Message API analytics.** This dashboard is for V4 HTTP Proxy APIs; Message API analytics unchanged.
- **Latency percentiles (p50/p95/p99), rate (e.g. requests/min), response size metric.** Not in this PR; possible future enhancements (see `docs/workshop/PO_REVIEW.md`).

---

## References

- **Epic:** APIM-7991  
- **Acceptance criteria / user stories:** `docs/v4-api-analytics-dashboard-user-stories.md`  
- **Requirement compliance:** `docs/workshop/REQUIREMENT_COMPLIANCE.md`  
- **PO review (pass/fail, UX, competitors):** `docs/workshop/PO_REVIEW.md`  
- **README (feature, API examples, dashboard layout, extension):** root `README.md` (V4 API Analytics Dashboard section)
