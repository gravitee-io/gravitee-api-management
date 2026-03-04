# V4 API Analytics Dashboard – Workshop Plan

This plan refines the story decomposition from the review and provides implementation order, time tracking, and open points. For file-level subtasks, see [STORIES.md](./STORIES.md).

---

## 1. Final list of user stories and subtasks (refined)

### Backend

| ID | Story | Key subtasks | Deps |
|----|--------|--------------|------|
| **B1** | **Unified endpoint + COUNT + permission + validation (type, from, to)** | 1.1 Repository: COUNT over v4-metrics only (verify/complete). 1.2 Service: expose COUNT. 1.3 REST: GET …/analytics with type=COUNT; **permission API_ANALYTICS:READ from day one**; validate type required, from/to required and from < to; invalid API → 404/403. 1.4 OpenAPI: path + COUNT schema. | — |
| **B2** | **STATS analytics type** | 2.1 Repository: STATS aggregate/query + ES adapter (verify/complete). 2.2 Service + REST: STATS branch; field required; **unsupported field → 400**. 2.3 OpenAPI: STATS schema. | B1 |
| **B3** | **GROUP_BY analytics type** | 3.1 Repository: GROUP_BY aggregate/query + ES adapter (verify/complete). 3.2 Service + REST: GROUP_BY branch; field required; **size cap (e.g. 1–100)**; **invalid order → 400 or default**. 3.3 OpenAPI: GROUP_BY schema. | B1 |
| **B4** | **DATE_HISTO analytics type** | 4.1 Repository: DATE_HISTO aggregate/query + ES adapter (verify/complete). 4.2 Service + REST: DATE_HISTO branch; field and interval required; interval range (e.g. 1000–1e9 ms). 4.3 OpenAPI: DATE_HISTO schema. | B1 |
| **B5** | **Backend tests** | **5a** Resource: 403 when no API_ANALYTICS:READ; COUNT, STATS, GROUP_BY, DATE_HISTO response shape; all validation 400s (missing type, from/to, field, interval, invalid type, unsupported field, size/order). **5b** Service: searchV4AnalyticsCount/Stats/GroupBy/DateHisto with mocked repository. **5c** Adapters: ES query shape and response mapping for STATS, GROUP_BY, DATE_HISTO (v4-metrics index). | B1–B4 |

### Frontend

| ID | Story | Key subtasks | Deps |
|----|--------|--------------|------|
| **F6** | **Angular service + time range filter** | 6.1 ApiAnalyticsV2Service: getV4Analytics(apiId, params) calling GET …/analytics with type, from, to, field?, interval?, size?, order?; typed response (COUNT \| STATS \| GROUP_BY \| DATE_HISTO); errors propagated. 6.2 **Time range filter**: ensure filter bar sets from/to and all widgets use the same range (verify/complete). | B1–B4 |
| **F7** | **Stats cards + line charts unchanged** | 7.1 Wire four stats from getV4Analytics: COUNT, STATS gateway-response-time-ms, endpoint-response-time-ms, request-content-length. 7.2 Loading state; missing/empty → "-" or 0. 7.3 **Line charts**: ensure "response status over time" and "response time over time" still render and refresh with selected time range (existing or DATE_HISTO). | F6 |
| **F8** | **HTTP status pie chart** | 8.1 Data from getV4Analytics(type=GROUP_BY, field=status, size=10). 8.2 Map to shape for api-analytics-response-status-ranges; **empty GROUP_BY → empty pie, no crash**. 8.3 Layout: pie + "response status over time" in two-column row. | F6 |
| **F9** | **Empty and error states** | 9.1 Empty state when no analytics data (e.g. count 0, empty GROUP_BY); message e.g. "No analytics data for this period". 9.2 Error state when any request fails; message e.g. "Analytics unavailable"; **filter bar stays visible** (retry/change range). 9.3 **Loading**: show loading while any request in flight. **Partial failure**: if any call fails, show error state. | F7, F8 |
| **F10** | **Frontend tests** | **10a** ApiAnalyticsV2Service: getV4Analytics URL/params/response/error for each type. **10b** Proxy: stats cards, pie chart, line charts receive data and render; no regression. **10c** Empty state, error state, harness selectors (loader, empty, error, stats, pie). | F6–F9 |

**Refinements applied (from review):**

- Permission and type/from/to validation are part of B1 (not a separate story).
- Unsupported `field` → 400 in B2/B3; GROUP_BY `size` cap and `order` validation in B3; invalid API → 404/403 in B1.
- Story 9 (line charts unchanged) merged into F7; Story 5 (permission/validation) merged into B1–B4 and B5a.
- Backend tests split into B5a (resource), B5b (service), B5c (adapters); frontend tests into F10a, F10b, F10c.
- Time range filter ownership and “empty GROUP_BY” / loading / partial failure called out in F6, F8, F9.
- Where code already exists (unified endpoint, repository, service, proxy), treat subtasks as **verify/complete** and see [STORIES.md](./STORIES.md) for file paths.

---

## 2. Implementation order

**Backend (sequential for B1–B4, then tests)**

1. **B1** — Unified endpoint + COUNT + permission + validation (type, from, to).
2. **B2** — STATS type.
3. **B3** — GROUP_BY type.
4. **B4** — DATE_HISTO type.
5. **B5** — Backend tests: **B5a** first (resource + permission + validation), then **B5b** (service) and **B5c** (adapters) in parallel if desired.

**Frontend (F6 first, then widgets, then tests)**

6. **F6** — Angular service + time range filter.
7. **F7** and **F8** — Stats cards + line charts (F7) and HTTP status pie (F8); can be done in parallel.
8. **F9** — Empty and error states (and loading / partial failure).
9. **F10** — Frontend tests: **F10a** (service), then **F10b** (proxy/charts) and **F10c** (empty/error + harness) in parallel if desired.

**Summary:** B1 → B2 → B3 → B4 → B5a → (B5b ∥ B5c) → F6 → (F7 ∥ F8) → F9 → F10a → (F10b ∥ F10c).

---

## 3. Time tracking

| Phase | Planned | Actual |
|-------|---------|--------|
| Phase 1 — Decomposition | 30–45 min | ___ |
| Phase 2 — Backend Stories | 60–75 min | ___ |
| Phase 2 — Frontend Stories | 60–75 min | ___ |
| Phase 3 — Documentation | 15–20 min | ___ |
| Phase 4 — Deploy & Test | 20–30 min | ___ |
| Phase 5 — PR | 15 min | ___ |

---

## 4. Notes, concerns, and questions

### Current codebase state

- The unified endpoint (`getApiV4Analytics`), repository methods (`searchStats`, `searchGroupBy`, `searchDateHisto`), `AnalyticsQueryService` V4 methods, and frontend `getV4Analytics` + proxy wiring **may already exist**. Treat B1–B4 and F6–F8 as **verify, complete, and fix gaps** (e.g. unsupported field → 400, size cap, OpenAPI schemas) rather than greenfield. Use [STORIES.md](./STORIES.md) for “create/modify” as “create **if missing**” or “verify/complete.”

### OpenAPI

- OpenAPI already defines `GET /environments/{envId}/apis/{apiId}/analytics` and `getApiV4Analytics`. Confirm all four response schemas (COUNT, STATS, GROUP_BY, DATE_HISTO) and query params are documented; do any updates in one pass (e.g. at end of B4 or in Phase 3).

### Edge cases to confirm

- **GROUP_BY `order`:** Allowed values (e.g. asc/desc) and behaviour for invalid value (400 vs default).
- **Time range cap:** Whether to limit max range (e.g. max 90 days) to protect ES; if yes, add validation in B1 and document.

### Out of scope (by design)

- No batch analytics endpoint (dashboard uses multiple GET calls).
- No new GROUP_BY metadata beyond what’s needed for the pie chart unless PRD requires it.

### Questions for product/tech lead

1. Exact allowlist for `field` (STATS/GROUP_BY/DATE_HISTO) and error message format for unsupported field.
2. Max allowed `from`–`to` range (if any).
3. Whether line charts must stay on existing endpoints or should be migrated to DATE_HISTO for consistency.

---

*For detailed file paths and package names, see [STORIES.md](./STORIES.md). For original user-story text and AC mapping, see [../v4-api-analytics-dashboard-user-stories.md](../v4-api-analytics-dashboard-user-stories.md).*
