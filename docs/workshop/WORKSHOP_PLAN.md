# V4 API Analytics Dashboard — Workshop Plan

## Stories Reference

Full story breakdown with subtasks, file paths, patterns, and tests:
[STORIES.md](STORIES.md)

**10 stories total:** BE-1 through BE-6 (backend), FE-1 through FE-4 (frontend).

## Implementation Order

### Phase 2a — Backend (BE-1 → BE-6)

1. **BE-1** — Query parameter model (`AnalyticsType`, `AnalyticsParam`, `AnalyticsFieldParam`)
2. **BE-2** — COUNT query type + unified endpoint scaffolding (OpenAPI, use case, query service, ES adapter, REST resource)
3. **BE-3 / BE-4 / BE-5** — STATS, GROUP_BY, DATE_HISTO (parallel; each follows same vertical slice pattern as BE-2)
4. **BE-6** — Integration tests for all 4 query types + validation + edge cases

### Phase 2b — Frontend (FE-1 → FE-4)

1. **FE-1** — Angular service method + TypeScript response models + fixtures
2. **FE-2 / FE-3** — Enhanced stats cards + HTTP status pie chart (parallel)
3. **FE-4** — Dashboard layout, integration, removal of legacy response-status-ranges, and component tests

### Key risks to address early

- Verify `endpoint-response-time-ms` exists in live `*-v4-metrics-`* index before starting FE-2 (see STORIES.md "Known risks")
- Unified GET endpoint must not collide with existing sub-path endpoints (`/requests-count`, `/response-status-ranges`, etc.)

## Time Tracking


| Phase                       | Planned          | Actual |
| --------------------------- | ---------------- | ------ |
| Phase 1 — Decomposition     | 30-45 min        | 60 min |
| Phase 2a — Backend Stories  | 60-75 min        | ___    |
| Phase 2b — Frontend Stories | 60-75 min        | ___    |
| Phase 3 — Documentation     | 15-20 min        | ___    |
| Phase 4 — Deploy & Test     | 20-30 min        | ___    |
| Phase 5 — PR                | 15 min           | ___    |
| **Total**                   | **~3.5-4.5 hrs** | ___    |


## Notes / Concerns / Questions

- **Endpoint routing:** The unified `GET /analytics` sits at the same path as the parent resource. Existing endpoints are sub-paths (`/analytics/requests-count`). JAX-RS should route correctly since sub-paths take priority, but verify no ambiguity.
- **OpenAPI code generation:** Response models (`ApiAnalyticsCountResponse`, etc.) are generated from `openapi-apis.yaml` via maven plugin. Run `mvn generate-sources` after editing the spec to regenerate before compiling.
- `**endpoint-response-time-ms`:** If the field is absent from the V4 metrics index, fall back to `gateway-latency-ms` or show "N/A". Decision needed before FE-2.
- **Deferred scope:** `order` parameter for GROUP_BY and timeframe range alignment (5m vs 1m) are explicitly deferred post-M1.
- **Existing endpoints preserved:** The 6 existing specific endpoints (`/requests-count`, `/average-connection-duration`, etc.) are not deprecated or removed. The unified endpoint is additive.

