# V4 API Analytics Dashboard — Workshop Plan (M1)

This plan reflects the **final refined stories** captured in [`docs/workshop/STORIES.md`](docs/workshop/STORIES.md).

---

## 1) Final user stories and subtasks

### Backend stories

#### B1a — Repository contracts: unified analytics query/response models

- **Create** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/ApiAnalyticsField.java` (PRD field → ES field mapping + type hint)
- **Create** repository query criteria under `.../model/analytics/`:
  - `ApiAnalyticsCountQuery.java`
  - `ApiAnalyticsStatsQuery.java`
  - `ApiAnalyticsGroupByQuery.java` (includes `size`, `order`)
  - `ApiAnalyticsDateHistoQuery.java` (includes `interval`)
- **Create** repository result aggregates under `.../model/analytics/`:
  - `ApiAnalyticsCountAggregate.java`
  - `ApiAnalyticsStatsAggregate.java`
  - `ApiAnalyticsGroupByAggregate.java` (values + metadata)
  - `ApiAnalyticsDateHistoAggregate.java` (timestamps + series + metadata)
- **Create** `ApiAnalyticsGroupByOrder.java` (if needed to encode PRD ordering)
- **Modify** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/api/AnalyticsRepository.java` to add COUNT/STATS/GROUP_BY/DATE_HISTO methods
- **Tests**: only if constructors enforce defaults/validation; otherwise covered by ES adapter tests in B1b
- **Patterns to reference**: existing v4 analytics models (e.g. `RequestsCountQuery.java`, `AverageAggregate.java`)
- **Docs**: optional note in `docs/workshop/README.md` if mapping isn’t obvious

#### B1b — Elasticsearch adapters & repository implementation for unified analytics

- **Create** ES query adapters under `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/`:
  - `SearchApiAnalyticsStatsQueryAdapter.java`
  - `SearchApiAnalyticsGroupByQueryAdapter.java` (honor `size`, `order`)
  - `SearchApiAnalyticsDateHistoQueryAdapter.java` (**must** set `min_doc_count: 0` and `extended_bounds`)
- **Create** ES response adapters under the same package:
  - `SearchApiAnalyticsStatsResponseAdapter.java`
  - `SearchApiAnalyticsGroupByResponseAdapter.java`
  - `SearchApiAnalyticsDateHistoResponseAdapter.java`
- **Modify** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepository.java` to implement the new repository methods using `Type.V4_METRICS`
- **Tests**:
  - Adapter tests under `.../src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/*AdapterTest.java`
  - Extend `.../src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepositoryTest.java`
- **Patterns to reference**: `SearchRequestsCountQueryAdapter.java`, `SearchRequestsCountResponseAdapter.java`, `AggregateValueCountByFieldAdapter.java`, `SearchResponseStatusOverTimeAdapter.java`
- **Docs**: none beyond B3 OpenAPI

#### B2 — Core: extend `AnalyticsQueryService` + per-type use cases (resource dispatches by `type`)

- **Modify** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java` to add per-type methods
- **Modify** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImpl.java` to delegate to `io.gravitee.repository.log.v4.api.AnalyticsRepository`
- **Create** use cases under `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/use_case/`:
  - `SearchApiAnalyticsCountUseCase.java`
  - `SearchApiAnalyticsStatsUseCase.java`
  - `SearchApiAnalyticsGroupByUseCase.java`
  - `SearchApiAnalyticsDateHistoUseCase.java`
- **Edge cases enforced by use cases (tests required)**:
  - API must be V4, not TCP proxy, belongs to environment
  - Decide & document behavior when `api.analytics.enabled = false` (prefer empty results; UI also guards)
- **Modify** test fakes:
  - `gravitee-apim-rest-api-service/src/test/java/fakes/FakeAnalyticsQueryService.java`
  - `gravitee-apim-rest-api-service/src/test/java/fakes/spring/FakeConfiguration.java` (if needed)
- **Tests**:
  - `SearchApiAnalytics{Type}UseCaseTest.java` (4 files) under `.../src/test/java/io/gravitee/apim/core/analytics/use_case/`
  - Extend `.../src/test/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImplTest.java`
- **Patterns to reference**: `SearchRequestsCountAnalyticsUseCase.java`, `SearchRequestsCountAnalyticsUseCaseTest.java`
- **Docs**: Javadoc on use case inputs/outputs (brief)

#### B3 — REST: `GET .../apis/{apiId}/analytics` + OpenAPI

- **Modify** OpenAPI:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-apis.yaml`
  - Add `GET /v2/apis/{apiId}/analytics` (actual path includes environment prefix in this module’s OpenAPI) with discriminated response (`type` field + payload)
  - Regenerate REST models per repo conventions
- **Add explicit validation (400)**:
  - missing/unsupported `type`
  - missing `field` for `STATS`/`GROUP_BY`
  - missing `interval` for `DATE_HISTO`
  - unsupported `field` (not in PRD list / `ApiAnalyticsField`)
  - `from > to`
  - interval <= 0, interval > (to-from), or unsafe bucket count cap
  - default `size=10` for `GROUP_BY`
- **Modify** resource:
  - `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`
  - Add root `@GET` on analytics resource and dispatch by `type` to B2 use cases
  - Accept `size` + `order` for `GROUP_BY`
  - Permission: `API_ANALYTICS:READ`
  - **Acceptance criterion**: legacy sub-path analytics endpoints remain unchanged; existing tests continue to pass
- **Response mapping**:
  - Prefer manual construction for the discriminated union response; keep MapStruct only for trivial mappings
- **Tests**:
  - Extend `gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResourceTest.java` (all types + validation failures + permission if covered)
- **Patterns to reference**: existing methods in `ApiAnalyticsResource.java` (`/requests-count`, `/response-status-overtime`), existing OpenAPI analytics schemas in `openapi-apis.yaml`
- **Docs**: OpenAPI descriptions are the documentation

---

### Frontend stories

#### F1 — Console: entities + `ApiAnalyticsV2Service` (unblocked from backend)

- **Create** typed entities under `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/`:
  - `analyticsUnifiedQuery.ts`
  - `analyticsUnifiedResponse.ts`
  - optional fixtures `*.fixture.ts`
- **Modify** service:
  - `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts` to call `GET .../apis/${apiId}/analytics` for COUNT/STATS/GROUP_BY/DATE_HISTO
- **Tests**:
  - `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.spec.ts` (URLs + query params)
- **Patterns to reference**: existing analytics entities in `src/entities/management-api-v2/analytics/`, existing service methods in `api-analytics-v2.service.ts`
- **Docs**: none

#### F2 — Timeframe filter bar (PRD presets; predefined only)

- **Modify** route-scoped config:
  - `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-filters-bar/api-analytics-filters-bar.configuration.ts`
  - Export `v4AnalyticsTimeFrames` for: Last 5 minutes, 1 hour, 24 hours, 7 days, 30 days
  - **Do not** change global `src/shared/utils/timeFrameRanges.ts` presets
- **Modify** component:
  - `.../api-analytics-filters-bar.component.ts` + `.html` to use `v4AnalyticsTimeFrames`
  - Hide custom date picker controls (M1 out-of-scope)
- **Tests**:
  - `api-analytics-filters-bar.component` tests/harness: selecting a preset emits `TimeRangeParams` and refresh triggers `ApiAnalyticsV2Service.setTimeRangeFilter(...)`
- **Patterns to reference**: existing filters bar behavior and `TimeRangeParams`
- **Docs**: optional note in `docs/workshop/README.md` about why presets are route-scoped

#### F3 — Stats cards (4 KPIs) + dashboard empty/error state + remove old connection duration card

- **Modify** container:
  - `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
  - `.../api-analytics-proxy.component.html`
  - Replace cards with PRD set (COUNT + 3×STATS); **remove** `getAverageConnectionDuration$` and the “Average Connection Duration” card
  - If COUNT=0: show `gio-card-empty-state` and hide widgets
  - Error handling: stop loaders; show snackbar/inline error
- **Modify** presentational cards:
  - `.../components/api-analytics-requests-stats/api-analytics-request-stats.component.ts` (+ template/styles)
- **Tests**:
  - `api-analytics-proxy.component.spec.ts`: KPI values, COUNT=0 empty state, error state
  - stats component harness/spec for rendering
- **Patterns to reference**: current VM composition in `api-analytics-proxy.component.ts`, `GioCardEmptyStateModule`
- **Docs**: none

#### F4 — HTTP status pie chart (GROUP_BY status) + replace old status ranges + layout integration

- **Create** widget:
  - `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-http-status-pie/`
  - `api-analytics-http-status-pie.component.ts` (+ `.html` / `.scss` / optional `.harness.ts`)
- **Use** `GioChartPieModule` and map GROUP_BY response to `GioChartPieInput[]`
- **Empty/error**: widget-level empty state + stop loaders + snackbar/inline error
- **Modify** layout:
  - `.../api-analytics-proxy.component.html` to **replace** `api-analytics-response-status-ranges` with the new pie
  - Keep `api-analytics-response-status-overtime` (right) and ensure `api-analytics-response-time-over-time` still renders (PRD out-of-scope to rewire it)
- **Tests**:
  - `api-analytics-http-status-pie.component.spec.ts` (empty, single bucket, error)
  - `api-analytics-proxy.component.spec.ts` confirms old ranges widget removed + new widget present + existing line charts mount
- **Patterns to reference**: `shared/components/api-analytics-response-status-ranges` (color mapping), `shared/components/gio-chart-pie/*`
- **Docs**: none

---

## 2) Implementation order (backend → frontend)

1. **Backend**
   - B1a → B1b → B2 → B3
2. **Frontend**
   - F1 and F2 can be done **in parallel** (do not require backend to compile)
   - Then F3 → F4 (F4 includes final layout replacement + regression assertions)
3. **Close-out**
   - OpenAPI regeneration + verify builds/tests + ensure `docs/workshop/STORIES.md` and this plan are up to date

---

## 3) Time tracking table

| Phase | Planned | Actual |
|-------|---------|--------|
| Phase 1 — Decomposition | 30-45 min | ___ |
| Phase 2 — Backend Stories | 60-75 min | ___ |
| Phase 2 — Frontend Stories | 60-75 min | ___ |
| Phase 3 — Documentation | 15-20 min | ___ |
| Phase 4 — Deploy & Test | 20-30 min | ___ |
| Phase 5 — PR | 15 min | ___ |

---

## 4) Notes / concerns / questions

- **Field mapping risk:** PRD field names must match actual v4 metrics document mapping in ES (e.g. which fields are keyword vs numeric). `ApiAnalyticsField` should encode ES field name + type to keep adapters deterministic.\n
- **DATE_HISTO correctness:** ensure adapters set `min_doc_count: 0` and `extended_bounds` or the UI timestamps/series will be jagged (missing buckets).\n
- **GROUP_BY metadata:** PRD response includes `metadata`. For M1 widgets, status-code metadata can be trivial; for other fields (application/plan) future work may require ID→name resolution.\n
- **Validation caps:** pick a safe max bucket count for DATE_HISTO and enforce it (avoid accidental “30 days with 1ms interval”).\n
- **OpenAPI generation:** confirm the repo’s expected workflow for regenerating REST models from `openapi-apis.yaml` (and whether generated sources are committed).\n
- **Angular presets:** route-scoped timeframes avoid impacting other screens but require the filters bar to reference `v4AnalyticsTimeFrames`.\n
