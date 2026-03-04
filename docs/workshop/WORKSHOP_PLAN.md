# V4 API Analytics Dashboard - Workshop Plan

## 1) Final User Stories and Subtasks (Refined)

### BE-1a - Unified Endpoint Contract and Validation
- Add unified `GET /v2/apis/{apiId}/analytics` endpoint signature and query param object.
- Enforce query compatibility matrix (`type` vs required params), invalid ranges (`from > to`), and missing bounds handling.
- Define explicit empty-data payload contract for `COUNT`, `STATS`, `GROUP_BY`, `DATE_HISTO`.

### BE-1b - Authorization Semantics
- Enforce `API_ANALYTICS:READ` on unified endpoint.
- Lock and test `401` vs `403` semantics for unauthenticated vs insufficient permissions.

### BE-1c - Consistent V4 API Scoping Across Query Types
- Centralize API eligibility checks (V4-only scope, environment ownership, TCP proxy rejection).
- Ensure all query branches (`COUNT`, `STATS`, `GROUP_BY`, `DATE_HISTO`) use the same guard path.

### BE-2 - COUNT Query Type
- Implement unified COUNT branch using existing request-count query service path.
- Map domain result to unified response shape `{ type, count }`.

### BE-3 - STATS Query Type
- Add repository query/aggregate contracts for STATS.
- Implement Elasticsearch STATS adapter and repository method.
- Wire through query service and unified use case with field validation.

### BE-4 - GROUP_BY Query Type
- Add repository query/aggregate contracts for GROUP_BY.
- Implement Elasticsearch GROUP_BY adapter with deterministic tie ordering.
- Wire through service/use case/resource mapping, including metadata output.

### BE-5 - DATE_HISTO Query Type
- Add repository query/aggregate contracts for DATE_HISTO.
- Implement Elasticsearch DATE_HISTO adapter with timezone/gap-bucket policy.
- Wire through service/use case/resource mapping for final unified payload.

### BE-T1 - Query-Type Backend Tests (Feature-Aligned)
- Add use-case tests for COUNT/STATS/GROUP_BY/DATE_HISTO success and empty responses.
- Add adapter/repository tests for all new query builders and response adaptors.

### BE-T2 - Permission, Validation, and Contract Hardening Tests
- Add resource-level auth and validation matrix tests.
- Add regression tests for empty payload contract by query type.

### FE-1 - Unified Angular Analytics Service (Contract-First)
- Add TypeScript models + fixtures for unified analytics response types.
- Add unified API methods in `ApiAnalyticsV2Service` for each query type.
- Ensure rapid timeframe changes are safe (`latest request wins` / stale response protection).

### FE-2 - Dashboard Data-State Orchestration
- Build single VM orchestration flow for proxy dashboard refresh/loading lifecycle.
- Support partial widget failure without blanking the whole dashboard.
- Implement explicit dashboard empty and error states.
- Scope timeframe presets to workshop/PRD ranges (`5m`, `1h`, `24h`, `7d`, `30d`).

### FE-3 - Enhanced Stats Cards
- Bind KPI cards to COUNT + STATS calls:
  - Total Requests
  - Avg Gateway Response Time
  - Avg Upstream Response Time
  - Avg Content Length
- Update proxy layout for final cards row.

### FE-4 - HTTP Status Pie Chart Widget
- Create dedicated exact-status pie widget component (+ harness/spec).
- Integrate into proxy dashboard using GROUP_BY(status) data.

### FE-5a - Response Status Over Time Rewire
- Rewire status-over-time line chart to unified DATE_HISTO.
- Keep chart bucket alignment and options stable (`pointStart`, `pointInterval`).

### FE-5b - Existing Line Chart Regression Compatibility
- Add explicit inventory and baseline checks for both existing line charts:
  - Response status over time
  - Response time over time
- Add smoke regression for response-time-over-time widget.

### FE-T2 - Frontend Test Hardening
- Complete dashboard-level harness tests (refresh, partial failure, empty/error, stale response handling).
- Complete service-level tests for all unified query shapes and query params.

---

## 2) Implementation Order (Backend -> Frontend)

### Backend Sequence
1. BE-1a (contract/validation)
2. BE-1b (authorization)
3. BE-1c (shared V4 scoping guard)
4. BE-2 (COUNT)
5. BE-3 (STATS)
6. BE-4 (GROUP_BY)
7. BE-5 (DATE_HISTO)
8. BE-T1 (feature-aligned query tests)
9. BE-T2 (auth/validation/contract hardening tests)

### Frontend Sequence
10. FE-1 (contract-first unified service/models)
11. FE-2 (orchestration and data states)
12. FE-3 (stats cards, depends on COUNT+STATS)
13. FE-4 (status pie, depends on GROUP_BY)
14. FE-5a (status-over-time rewire, depends on DATE_HISTO)
15. FE-5b (line-chart compatibility checks)
16. FE-T2 (frontend hardening tests)

---

## 3) Time Tracking

| Phase | Planned | Actual |
|-------|---------|--------|
| Phase 1 — Decomposition | 30-45 min | ___ |
| Phase 2 — Backend Stories | 60-75 min | ___ |
| Phase 2 — Frontend Stories | 60-75 min | ___ |
| Phase 3 — Documentation | 15-20 min | ___ |
| Phase 4 — Deploy & Test | 20-30 min | ___ |
| Phase 5 — PR | 15 min | ___ |

---

## 4) Notes, Concerns, Questions

- OpenAPI updates are treated as implementation-critical in this repo because generated management-v2 models depend on it.
- Keep old split analytics endpoints callable during migration; do not expand this workshop with full deprecation/removal.
- Confirm backend decision on GROUP_BY field scope for this iteration:
  - current plan keeps broad PRD field support
  - status-only can be a deliberate scope cut if needed for timeline
- Confirm agreed DATE_HISTO behavior in backend contract:
  - timezone reference
  - zero-filled buckets for gaps
- Confirm tie-break policy for GROUP_BY with equal counts (for deterministic UI order).
- Confirm expected behavior for auth failures in UI (`401` vs `403`) and whether UI messaging should differ.
- Frontend should keep "latest timeframe wins" semantics to avoid stale chart updates during rapid filter changes.
