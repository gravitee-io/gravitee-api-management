# V4 API Analytics Dashboard — Workshop Plan

## Objective

Build a unified analytics endpoint and enhanced Console dashboard for V4 HTTP Proxy APIs in Gravitee API Management. A single `GET /v2/apis/{apiId}/analytics?type=...` endpoint replaces separate per-metric REST endpoints on the frontend, and the dashboard gains new stats cards and a GROUP_BY-powered status pie chart.

---

## Scope

### In Scope
- Backend unified endpoint: COUNT, STATS, GROUP_BY, DATE_HISTO (✅ complete)
- Frontend: Angular service methods for unified endpoint
- Frontend: 4 stats cards (Total Requests, Avg GW Response Time, Avg Upstream Response Time, Avg Content Length)
- Frontend: HTTP status pie chart sourced from GROUP_BY
- Frontend: Full dashboard assembly with loading/empty/error states

### Out of Scope
- Query filter parameter (`?query=...` Lucene filter)
- URL-persisted timeframe (shareable links)
- Response status over time rewire to unified endpoint
- Response time over time rewire to unified endpoint
- Top Applications / Top Plans tables
- Documentation story

---

## Progress

| Story | Title | Layer | Status |
|-------|-------|-------|--------|
| 01 | Unified Endpoint + COUNT | Backend | ✅ Complete |
| 02 | STATS type | Backend | ✅ Complete |
| 03 | GROUP_BY type | Backend | ✅ Complete |
| 04 | DATE_HISTO type | Backend | ✅ Complete |
| 05 | Frontend Service for Unified Endpoint | Frontend | ⬜ Pending |
| 06 | Stats Cards Row | Frontend | ⬜ Pending |
| 07 | HTTP Status Pie Chart | Frontend | ⬜ Pending |
| 08 | Dashboard Assembly & States | Frontend | ⬜ Pending |

---

## Time Tracking

| Phase | Planned | Actual | Notes |
|-------|---------|--------|-------|
| Phase 2a — Backend API | 60–75 min | ✅ Done | Stories 01–04 |
| Phase 2b — Frontend | 45–60 min | ___ | Stories 05–08 |
| Phase 4 — Deploy & Test | 20–30 min | ___ | Build, docker, smoke test |
| Phase 5 — PR | 15 min | ___ | Create PR |
| **Total** | **~2.5–3 hours** | ___ | |

---

## Implementation Order

### Phase 2a: Backend (Stories 01–04) ✅ COMPLETE

All four types implemented and tested. Key files:
- `ApiAnalyticsResource.java` — unified `GET /analytics` with switch dispatch
- `SearchAnalytics{Count,Stats,GroupBy,DateHisto}UseCase.java` — core domain use cases
- `AnalyticsQueryService` / `AnalyticsQueryServiceImpl` — new query methods
- `{Stats,GroupBy,DateHisto}Query` / `{Stats,GroupBy,DateHisto}Aggregate` — repo API records
- ES adapters in `gravitee-apim-repository-elasticsearch/.../adapter/`

Test counts: 6+8+10+10 use case tests, 4+2+2+2 REST tests. All pass (23 total in `ApiAnalyticsResourceTest`).

---

### Phase 2b: Frontend (Stories 05–08)

Stories 05 and 07 are independent of each other. Execute sequentially (05 → 06 → 07 → 08) since 06 and 07 both modify the same proxy component.

#### Story 05: Frontend Service
| # | Subtask | Action | File |
|---|---------|--------|------|
| 05.1 | Define TypeScript interfaces | **Create** | `entities/management-api-v2/analytics/analyticsUnified.ts` |
| 05.2 | Write service tests (RED) | **Modify** | `api-analytics-v2.service.spec.ts` |
| 05.3 | Add 3 service methods (GREEN) | **Modify** | `api-analytics-v2.service.ts` |

New methods: `getAnalyticsCount(apiId)`, `getAnalyticsStats(apiId, field)`, `getAnalyticsGroupBy(apiId, field, size?)`.
All follow the existing `timeRangeFilter$.pipe(filter(Boolean), switchMap)` pattern.

#### Story 06: Stats Cards Row
| # | Subtask | Action | File |
|---|---------|--------|------|
| 06.1 | Update proxy spec for 4 cards (RED) | **Modify** | `api-analytics-proxy.component.spec.ts` |
| 06.2 | Replace 2 legacy calls with 4 unified calls (GREEN) | **Modify** | `api-analytics-proxy.component.ts` |

Replace: `getRequestsCount` → `getAnalyticsCount` (Total Requests)
Replace: `getAverageConnectionDuration` → `getAnalyticsStats(gateway-response-time-ms)` (Avg GW Response Time)
Add: `getAnalyticsStats(endpoint-response-time-ms)` (Avg Upstream Response Time)
Add: `getAnalyticsStats(request-content-length)` (Avg Content Length)

#### Story 07: HTTP Status Pie Chart
| # | Subtask | Action | File |
|---|---------|--------|------|
| 07.1 | Update proxy spec for GROUP_BY data source (RED) | **Modify** | `api-analytics-proxy.component.spec.ts` |
| 07.2 | Replace `getResponseStatusRanges` with `getAnalyticsGroupBy('status')` (GREEN) | **Modify** | `api-analytics-proxy.component.ts` |

Requires a bucketing mapper: individual codes (200, 404...) → range keys (`"200.0-300.0"`, `"400.0-500.0"`...) compatible with the existing `ApiAnalyticsResponseStatusRangesComponent`.

#### Story 08: Dashboard Assembly & States
| # | Subtask | Action | File |
|---|---------|--------|------|
| 08.1 | Write full proxy spec (RED) | **Modify** | `api-analytics-proxy.component.spec.ts` |
| 08.2 | Wire `ApiAnalyticsVM`, update template (GREEN) | **Modify** | `api-analytics-proxy.component.ts/html` |
| 08.3 | Verify harness locators still work | **Verify** | `api-analytics-proxy.component.harness.ts` |

Full HTTP call inventory for enabled analytics:
```
GET /apis/{id}
GET /apis/{id}/analytics?type=COUNT
GET /apis/{id}/analytics?type=STATS&field=gateway-response-time-ms
GET /apis/{id}/analytics?type=STATS&field=endpoint-response-time-ms
GET /apis/{id}/analytics?type=STATS&field=request-content-length
GET /apis/{id}/analytics?type=GROUP_BY&field=status
GET /apis/{id}/analytics/response-status-overtime   ← existing, unchanged
GET /apis/{id}/analytics/response-time-over-time    ← existing, unchanged
```

Dashboard layout (unchanged structure, updated data sources):
```
┌──────────────────────────────────────────────────────┐
│ [1h] [24h] [7d] [30d]                               │  Row 1: Filter bar
├──────────────┬───────────────┬──────────┬────────────┤
│ Total Reqs   │ Avg GW RT     │ Avg Up RT│ Avg Content│  Row 2: Stats cards (4)
├──────────────┴───────────────┼──────────┴────────────┤
│ HTTP Status Pie (GROUP_BY)   │ Status Over Time      │  Row 3: Charts
├──────────────────────────────┴───────────────────────┤
│ Response Time Over Time (full width)                 │  Row 4: Chart
└──────────────────────────────────────────────────────┘
```

---

### Phase 4: Deploy & Test

| Step | Command | What to verify |
|------|---------|----------------|
| 1 | `mvn test -pl :gravitee-apim-rest-api-management-v2-rest` | All 23 backend tests pass |
| 2 | `cd gravitee-apim-console-webui && npm test -- --include=api-analytics` | All frontend tests pass |
| 3 | `task docker-backend && task docker-ui` | Docker images build |
| 4 | Start docker-compose with ES | Full stack comes up |
| 5 | Manual smoke test | Hit unified endpoint, verify dashboard renders correctly |

---

### Phase 5: PR

| Step | Action |
|------|--------|
| 1 | Ensure branch is `workshop/v4-analytics-base` |
| 2 | Commit with semantic messages per story |
| 3 | Push + create draft PR via `gh pr create` |
| 4 | Link to issue, self-assign, add Copilot reviewer |
| 5 | Mark ready after CI green |

---

## Dependency Graph

```
✅ Story 01 (COUNT) ─────┐
✅ Story 02 (STATS) ─────┤
✅ Story 03 (GROUP_BY) ──┤──→ ⬜ Story 05 (FE Service) ─┬─→ ⬜ Story 06 (Stats Cards) ─┐
✅ Story 04 (DATE_HISTO) ┘                               └─→ ⬜ Story 07 (Pie Chart)   ─┤
                                                                                          └─→ ⬜ Story 08 (Assembly)
```

---

## Key Risks & Notes

### Frontend

1. **Existing tests will break mid-story:** The proxy component spec currently expects `/requests-count`, `/average-connection-duration`, and `/response-status-ranges` HTTP calls. Stories 06 and 07 will replace these. Update the spec alongside the component in the same story commit.

2. **Status bucketing:** Backend `GROUP_BY` returns individual codes (`"200"`, `"404"`). The existing `ApiAnalyticsResponseStatusRangesComponent` expects range keys like `"200.0-300.0"`. A small mapper function is needed in the proxy component (not in the service or the child component).

3. **`combineLatest` vs incremental loading:** The current proxy uses `combineLatest` — all cards show loading until all 4 streams emit. This matches existing behaviour. Keep it.

4. **Existing charts stay untouched:** `ApiAnalyticsResponseStatusOvertimeComponent` and `ApiAnalyticsResponseTimeOverTimeComponent` each call their own endpoints internally. They are **not** rewired to the unified endpoint in this workshop. Do not modify these components.

5. **`ChangeDetectionStrategy.OnPush`:** The proxy component does not currently use OnPush. Do not add it as a side effect — only change what the stories require.

---

## Reference Documents

| Document | Path |
|----------|------|
| Detailed subtasks | [`docs/workshop/STORIES.md`](./STORIES.md) |
| Individual stories | [`docs/workshop/story-05.md`](./story-05.md) through [`story-08.md`](./story-08.md) |
