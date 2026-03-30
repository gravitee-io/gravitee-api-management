# V4 API Analytics Dashboard — User Stories & Implementation Subtasks

This document breaks workshop stories into concrete subtasks with **files**, **tests**, **pattern references**, and **documentation** touchpoints. Paths are relative to the repository root unless noted.

**Module map (naming conventions):**

| Area | Maven module(s) | Java package roots |
| --- | --- | --- |
| Repository API (contracts + DTOs) | `gravitee-apim-repository/gravitee-apim-repository-api` | `io.gravitee.repository.log.v4.api`, `io.gravitee.repository.log.v4.model.analytics` |
| Elasticsearch implementation | `gravitee-apim-repository/gravitee-apim-repository-elasticsearch` | `io.gravitee.repository.elasticsearch.v4.analytics` (+ `adapter`, `spring` subpackages) |
| Domain core | `gravitee-apim-rest-api/gravitee-apim-rest-api-service` | `io.gravitee.apim.core.analytics` (`use_case`, `query_service`, `model`) |
| Domain infra | same | `io.gravitee.apim.infra.query_service.analytics` |
| REST API v2 | `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest` | `io.gravitee.rest.api.management.v2.rest.resource.api.analytics`, `mapper` |
| OpenAPI | `gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi` | `openapi-apis.yaml` |
| REST models (generated) | `gravitee-apim-rest-api-management-v2-rest` (generated sources) | `io.gravitee.rest.api.management.v2.rest.model` |
| Legacy shared model (optional domain DTOs) | `gravitee-apim-rest-api/gravitee-apim-rest-api-model` | `io.gravitee.rest.api.model.v4.analytics` |
| Console | `gravitee-apim-console-webui` | `src/management/...`, `src/services-ngx`, `src/entities/management-api-v2`, `src/shared` |

**Test layout:** Mirror production packages under `src/test/java` with `*Test.java` (JUnit). Angular: `*.spec.ts` beside components; harnesses as `*.harness.ts`.

---

## Story B1a — Repository contracts: unified analytics query/response models

**Goal:** Define a typed repository contract (query + result models and repository methods) for unified v4 API analytics queries so the rest of the stack can depend on stable interfaces.

**Depends on:** none.

### Subtasks

1. **Supported fields: mapping + validation surface (PRD field → ES field)**
   - **Create** under `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/`:
     - `ApiAnalyticsField.java` (enum) holding:
       - **prdName**: the query parameter value (e.g. `status`, `gateway-response-time-ms`)
       - **esFieldName**: actual ES document field name (e.g. `status`, `gateway-response-time-ms`, etc.)
       - **fieldType**: numeric vs keyword (so adapters can decide `terms` vs `stats`, `.keyword` usage, etc.)
   - **Acceptance criteria:** unsupported fields must be rejected (400) at REST layer (B3); adapters should not silently accept unknown fields.
   - **Pattern:** strongly typed criteria classes in the same package (`RequestsCountQuery.java` etc.).

2. **Query criteria & result types (repository-layer)**
   - **Create** under `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/`:
     - Criteria records:
       - `ApiAnalyticsCountQuery.java` (apiId, from, to)
       - `ApiAnalyticsStatsQuery.java` (apiId, from, to, field)
       - `ApiAnalyticsGroupByQuery.java` (apiId, from, to, field, size, order)
       - `ApiAnalyticsDateHistoQuery.java` (apiId, from, to, field, interval)
     - Result aggregates:
       - `ApiAnalyticsCountAggregate.java` (count)
       - `ApiAnalyticsStatsAggregate.java` (count/min/max/avg/sum)
       - `ApiAnalyticsGroupByAggregate.java` (`values` map + `metadata` map)
       - `ApiAnalyticsDateHistoAggregate.java` (`timestamps` + `series` buckets + `metadata`)
   - **Create** enum for ordering (if required by PRD): `ApiAnalyticsGroupByOrder.java` (e.g. `COUNT_DESC`, `COUNT_ASC`).
   - **Include defaults:** size defaults to 10 (either via constructor defaulting or defaulting at REST layer; document which layer owns it).
   - **Pattern:** `TopHitsQueryCriteria.java`, `AverageAggregate.java`.
   - **Tests:** unit tests only if constructors enforce defaults/validation.

3. **Repository interface**
   - **Modify** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/api/AnalyticsRepository.java`:
     - Add methods for COUNT/STATS/GROUP_BY/DATE_HISTO using the new query criteria and aggregate types.

### Documentation

- Optional: if field mapping is non-obvious, add a short mapping note in `docs/workshop/README.md` (not required if `ApiAnalyticsField` is self-documenting).

### References (patterns)

- `RequestsCountQuery.java`, `CountAggregate.java`, `AverageAggregate.java`

---

## Story B1b — Elasticsearch adapters & repository implementation for unified analytics

**Goal:** Implement the repository contract from B1a in Elasticsearch: build ES query JSON, parse ES responses, and wire through `AnalyticsElasticsearchRepository` on `Type.V4_METRICS`.

**Depends on:** B1a.

### Subtasks

1. **Elasticsearch query adapters (JSON → ES)**
   - **Create** under `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/`:
     - `SearchApiAnalyticsStatsQueryAdapter.java`
     - `SearchApiAnalyticsGroupByQueryAdapter.java`
     - `SearchApiAnalyticsDateHistoQueryAdapter.java`
   - **Implement** required edge cases:
     - `DATE_HISTO`: `min_doc_count: 0` and `extended_bounds` so the UI gets stable buckets even when ES has gaps
     - `GROUP_BY`: implement `size` default and `order` mapping to ES `terms.order`
   - **Pattern:** `SearchRequestsCountQueryAdapter.java`, `AggregateValueCountByFieldAdapter.java`, `SearchResponseStatusOverTimeAdapter.java`.
   - **Tests:** add `*AdapterTest.java` under `.../src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/` asserting the encoded JSON contains these fields.

2. **Elasticsearch response adapters (ES → aggregates)**
   - **Create** under the same adapter package:
     - `SearchApiAnalyticsStatsResponseAdapter.java`
     - `SearchApiAnalyticsGroupByResponseAdapter.java`
     - `SearchApiAnalyticsDateHistoResponseAdapter.java`
   - **Pattern:** `SearchRequestsCountResponseAdapter.java`.
   - **Tests:** add focused parsing tests where DATE_HISTO series parsing is non-trivial.

3. **Implement `AnalyticsElasticsearchRepository` methods**
   - **Modify** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepository.java`:
     - Implement the new methods using `Type.V4_METRICS` index wildcard (same pattern as `searchRequestsCount`).
   - **Tests:** extend `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepositoryTest.java`.

4. **Spring wiring**
   - Typically **no change**: `.../v4/analytics/spring/AnalyticsConfiguration.java` already registers the repository bean.

---

## Story B2 — Core: extend `AnalyticsQueryService` + per-type use cases (resource dispatches by `type`)

**Goal:** Preserve the existing pattern (one use case per behavior) while still delivering a unified REST endpoint: the REST resource routes by query `type` to dedicated use cases.

**Depends on:** B1b.

### Subtasks

1. **Extend domain query service**
   - **Modify** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java`:
     - Add methods that correspond to each query type: COUNT, STATS, GROUP_BY, DATE_HISTO.
   - **Modify** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImpl.java`:
     - Delegate to `io.gravitee.repository.log.v4.api.AnalyticsRepository` using `executionContext.getQueryContext()`.
   - **Create** domain models under `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/` only if you need to avoid leaking repository aggregates beyond infra.
   - **Pattern:** `searchRequestsCount`, `searchResponseStatusOvertime` in `AnalyticsQueryServiceImpl.java`.

2. **Use cases (one per `type`)**
   - **Create** in `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/use_case/`:
     - `SearchApiAnalyticsCountUseCase.java`
     - `SearchApiAnalyticsStatsUseCase.java`
     - `SearchApiAnalyticsGroupByUseCase.java`
     - `SearchApiAnalyticsDateHistoUseCase.java`
   - **Each use case** must enforce v4 constraints using the same approach as `SearchRequestsCountAnalyticsUseCase.java`:
     - DefinitionVersion must be V4
     - API must not be TCP proxy (`apiDefinitionV4.isTcpProxy()`)
     - API must belong to environment (multi-tenancy)
     - **Analytics disabled edge case:** define behavior when `api.analytics.enabled = false` (prefer returning empty results; UI already checks this before calling analytics, but tests should document backend behavior).
   - **Pattern:** `SearchRequestsCountAnalyticsUseCase.java`, `SearchResponseStatusOverTimeUseCase.java`.

3. **Fake for tests**
   - **Modify** `gravitee-apim-rest-api-service/src/test/java/fakes/FakeAnalyticsQueryService.java` — add stub implementations for new interface methods.
   - **Modify** `gravitee-apim-rest-api-service/src/test/java/fakes/spring/FakeConfiguration.java` if new beans are registered.

### Tests (alongside)

- **Create** tests under `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/analytics/use_case/`:
  - `SearchApiAnalyticsCountUseCaseTest.java`
  - `SearchApiAnalyticsStatsUseCaseTest.java`
  - `SearchApiAnalyticsGroupByUseCaseTest.java`
  - `SearchApiAnalyticsDateHistoUseCaseTest.java`
  - Mirror `SearchRequestsCountAnalyticsUseCaseTest.java` for V4 validation scenarios.
- **Extend** `AnalyticsQueryServiceImplTest.java` under `gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/infra/query_service/analytics/` — verify delegation to `AnalyticsRepository`.

### Documentation

- Javadoc on each new use case `Input`/`Output` records (brief, same style as sibling use cases).

### References

- `SearchRequestsCountAnalyticsUseCase.java`
- `FakeAnalyticsQueryService.java`

---

## Story B3 — REST: `GET .../apis/{apiId}/analytics` + OpenAPI

**Goal:** Single JAX-RS entry point with query parameters `type`, `from`, `to`, optional `field`, `interval`, `size`, `order`; JSON responses per PRD; permission `API_ANALYTICS:READ`.

**Depends on:** B2.

### Subtasks

1. **OpenAPI first (recommended in this repo)**
   - **Modify** `gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-apis.yaml`:
     - Add path under `/environments/{envId}/apis/{apiId}/analytics` (follow existing analytics paths around line ~2033).
     - Define schemas for discriminated response (`oneOf` or explicit `type` enum + payloads).
   - **Regenerate** REST models (project’s Maven profile or `npm`/`task` — follow existing contributor workflow; generated files land under `gravitee-apim-rest-api-management-v2-rest/target/generated-sources` or equivalent, committed per project policy).
   - **Pattern:** Existing `ApiAnalyticsRequestsCountResponse`, `ApiAnalyticsResponseStatusOvertimeResponse` definitions in the same YAML file.

2. **Query parameter validation (explicit)**
   - Implement validation that returns **400** for:
     - Missing `type` / unsupported `type`
     - Missing `field` for `STATS` and `GROUP_BY`
     - Missing `interval` for `DATE_HISTO`
     - Unsupported `field` (not in PRD list / `ApiAnalyticsField`)
     - `from > to`
     - `interval <= 0`, `interval > (to - from)`, or an unsafe bucket count (cap buckets to prevent huge histograms)
   - Ensure `size` defaults to 10 for `GROUP_BY` if absent.

3. **Resource method**
   - **Modify** `gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`:
     - Add `@GET` without sub-`@Path` for collection-style query on the analytics sub-resource (sibling methods use `@Path("/requests-count")` etc.).
     - Annotate `@Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })` — same as existing methods in this class.
     - Dispatch by `type` to the corresponding use case(s) (B2).
     - Accept `size` and `order` query params for `GROUP_BY`.
   - **Acceptance criterion:** existing sub-path endpoints remain unchanged and existing tests continue to pass.
   - **Pattern:** Existing `getApiAnalyticsRequestCount`, `getResponseStatusOvertime` in the same class.

4. **Response mapping (keep it simple)**
   - Prefer constructing the generated response POJOs directly in the resource method for the discriminated union response. Keep MapStruct for existing simple mappings only.

5. **Optional domain DTOs in `gravitee-apim-rest-api-model`**
   - **Create** only if OpenAPI generation is not used for internal service boundaries — under `gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/`. Prefer OpenAPI-generated v2 models for REST responses.

### Tests

- **Extend** `gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResourceTest.java` — new test methods for each `type`, 400 on invalid params, 403 if permission tests exist in base class.
- **Pattern:** Existing tests in the same file for `requests-count`, `response-status-overtime`.

### Documentation

- **Update** user-facing API docs only if the product publishes `openapi-apis.yaml` as the source of truth (descriptions in YAML are the documentation).
- **Optional:** Add a short entry in `README.md` at repo root or Console docs linking to the new operation (only if the team maintains such a list).

### References

- `ApiAnalyticsResource.java`
- `ApiResource.java` (`@Path("/analytics")` sub-resource locator)
- `openapi-apis.yaml`

---

## Story F1 — Console: entities + `ApiAnalyticsV2Service`

**Goal:** Typed client for `GET .../analytics` with `type` parameter and shared time range.

**Depends on:** PRD response schema (no backend dependency). Integrate against B3 when available.

### Subtasks

1. **TypeScript entities**
   - **Create** under `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/`:
     - `analyticsUnifiedQuery.ts` — query param types / enums (`COUNT`, `STATS`, `GROUP_BY`, `DATE_HISTO`).
     - Response types or discriminated union matching backend (`analyticsUnifiedResponse.ts`).
     - Optional `*.fixture.ts` — mirror `analyticsRequestsCount.fixture.ts` pattern.
   - **Naming:** camelCase file names, matching existing `analyticsRequestsCount.ts` style.

2. **Service**
   - **Modify** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts`:
     - Add `getUnifiedAnalytics(apiId, params)` or typed helpers per `type`.
     - Base URL: `` `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics` `` with query string.
     - Keep `timeRangeFilter()` / `setTimeRangeFilter` — existing `BehaviorSubject` pattern.
   - **Pattern:** Existing `getRequestsCount`, `getResponseStatusOvertime` in the same file.

3. **Unit tests**
   - **Modify** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.spec.ts` — `HttpTestingController` expectations for unified URL and query params.

### Documentation

- None (inline TSDoc on public service methods optional).

### References

- `api-analytics-v2.service.ts`, `api-analytics-v2.service.spec.ts`
- `entities/management-api-v2/analytics/analyticsRequestsCount.ts`

---

## Story F2 — Timeframe filter bar (PRD presets)

**Goal:** Predefined ranges aligned with PRD (5 min, 1 h, 24 h, 7 d, 30 d); single refresh for all widgets; align with M1 out-of-scope (custom URL / custom picker).

**Depends on:** none (parallel F1).

### Subtasks

1. **Route-scoped PRD presets (do not touch global `timeFrames`)**
   - **Modify** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-filters-bar/api-analytics-filters-bar.configuration.ts`:
     - Export `v4AnalyticsTimeFrames` with PRD presets: **Last 5 minutes**, **Last 1 hour**, **Last 24 hours**, **Last 7 days**, **Last 30 days**.
   - **Do NOT modify** `gravitee-apim-console-webui/src/shared/utils/timeFrameRanges.ts` `timeFrames` array, to avoid impacting other screens that rely on the existing 1m/1h/1d/1w/1M presets.

2. **Filters bar (M1: predefined only)**
   - **Modify** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-filters-bar/api-analytics-filters-bar.component.ts` and `.html`:
     - Bind select options to `v4AnalyticsTimeFrames`.
     - Hide the custom date picker controls for M1 (PRD out-of-scope). Keep `ApiAnalyticsV2Service.setTimeRangeFilter(...)` as the single trigger for dashboard refresh.

3. **Tests**
   - **Modify** `api-analytics-filters-bar.component.harness.ts` / component tests — selected period emits correct `TimeRangeParams`.

### Documentation

- **Update** this `STORIES.md` or add `docs/workshop/README.md` noting PRD vs legacy time presets if both coexist behind feature flags.

### References

- `api-analytics-filters-bar.component.ts`, `timeFrameRanges.ts`

---

## Story F3 — Stats cards (four KPIs)

**Goal:** Total requests, avg gateway response time, avg upstream response time, avg content length using the unified endpoint. Owns the **dashboard-level empty state** (based on COUNT).

**Depends on:** F1, F2.

### Subtasks

1. **VM / data wiring**
   - **Modify** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`:
     - Replace or supplement `combineLatest` sources: `COUNT` for total; three `STATS` calls for `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`.
     - **Remove** the `getAverageConnectionDuration$` data source and remove the “Average Connection Duration” stat card from the proxy dashboard (PRD no longer includes it).
     - **Empty state:** if COUNT returns 0, show `gio-card-empty-state` and hide charts/widgets (PRD: “Empty state when no analytics data”).
     - **Error state:** if unified endpoint calls fail, stop loaders and display a snackbar or inline error (do not leave the UI spinning).
   - **Modify** `api-analytics-proxy.component.html` — layout order for four cards.

2. **Presentational component**
   - **Modify** `components/api-analytics-requests-stats/api-analytics-request-stats.component.ts` / `.html` / `.scss` — accept four stat items, units (`ms`, bytes if applicable).
   - **Pattern:** Current `AnalyticsRequestStats` array in `api-analytics-proxy.component.ts`.

3. **Tests**
   - **Modify** `api-analytics-request-stats.component.harness.ts` (if exists) and `api-analytics-proxy.component.spec.ts` — assert labels and values from mocked HTTP.
   - Add cases:
     - COUNT=0 renders empty state and widgets are hidden
     - service error shows error state and loaders stop

### Documentation

- None.

### References

- `api-analytics-proxy.component.ts`, `api-analytics-request-stats.component.ts`

---

## Story F4 — HTTP status pie chart (GROUP_BY `status`)

**Goal:** New widget: pie chart of individual HTTP status codes via `GROUP_BY` + `gio-chart-pie`. Replaces the old “response status ranges” pie (2xx/3xx/4xx/5xx buckets).

**Depends on:** F1, F2.

### Subtasks

1. **Component**
   - **Option A — Create** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-http-status-pie/`:
     - `api-analytics-http-status-pie.component.ts` (standalone, `OnPush` if aligned with project)
     - `.html`, `.scss`, optional `.harness.ts`
   - **Option B — Extend** shared `shared/components/api-analytics-response-status-ranges/` — only if UX is identical; PRD asks for **per-code** breakdown (distinct from range buckets).

2. **Chart**
   - **Import** `GioChartPieModule` from `src/shared/components/gio-chart-pie/gio-chart-pie.module.ts`.
   - Map `GROUP_BY.values` / `metadata` to `GioChartPieInput[]` — pattern in `api-analytics-response-status-ranges.component.ts` (`getColor`, filter zero values).
   - **Empty/error:** handle `GROUP_BY` returning empty values (show widget empty state) and HTTP errors (stop loaders + show snackbar/inline error).

3. **Parent layout + explicit replacement**
   - **Modify** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.html`:
     - Replace `api-analytics-response-status-ranges` with the new `api-analytics-http-status-pie` component.
     - Keep `api-analytics-response-status-overtime` on the right per PRD.
   - **Regression acceptance criteria:** existing `api-analytics-response-status-overtime` and `api-analytics-response-time-over-time` components still render correctly in the updated layout (no rewiring required; response-time rewiring is PRD out-of-scope).

4. **Tests**
   - **Create** `api-analytics-http-status-pie.component.spec.ts` — empty data, single bucket, error from service.
   - **Harness** — follow `gio-chart-pie.harness.ts` / `api-analytics-response-status-ranges` patterns.
   - **Modify** `api-analytics-proxy.component.spec.ts` — assert old ranges widget is removed, new widget is present, and existing line chart components still mount.

### Documentation

- None.

### References

- `api-analytics-response-status-ranges.component.ts` (pie + colors)
- `gio-chart-pie.component.ts`

---

## Dependency graph (summary)

```
B1a → B1b → B2 → B3

F1 (parallel with backend; mock interfaces)
F2 (parallel with everything)
F1 + F2 → F3
F1 + F2 → F4
```

---

## Global documentation checklist

| Artifact | When to update |
| --- | --- |
| `openapi-apis.yaml` | Story B3 — required for generated models |
| `docs/workshop/README.md` | Optional index linking to `STORIES.md` and workshop goals |
| Repository / Console contributor guides | Only if new build steps (OpenAPI codegen) are introduced |

---

## File naming quick reference

| Kind | Convention | Example |
| --- | --- | --- |
| Java use case | `Search{Feature}{Type}UseCase.java` | `SearchApiAnalyticsStatsUseCase.java` |
| Java adapter | `Search{Feature}QueryAdapter.java` | `SearchRequestsCountQueryAdapter.java` |
| Java test | `{ClassUnderTest}Test.java` | `SearchApiAnalyticsStatsUseCaseTest.java` |
| TS entity | `analytics{Feature}.ts` (camelCase) | `analyticsRequestsCount.ts` |
| Angular component | `{feature}-{role}.component.ts` | `api-analytics-http-status-pie.component.ts` |
| Harness | `{component}.harness.ts` | `api-analytics-proxy.component.harness.ts` |
| Spec | `{component}.spec.ts` | `api-analytics-proxy.component.spec.ts` |

---

## Refinement Notes

This file was refined after a second-agent review to reduce “process smells”, remove redundant stories, and make gaps explicit.

1. **Accepted**: Remove B5 and F7 test “sweep” stories.\n
   - **Why**: Each story now includes its own tests as definition-of-done; no separate end-of-project testing bucket.\n

2. **Accepted**: Remove B4 legacy compatibility story.\n
   - **Why**: The unified endpoint is additive. B3 now explicitly requires legacy sub-path endpoints remain unchanged and existing tests continue to pass.\n

3. **Accepted**: Remove F5 (line chart regression) as a standalone story.\n
   - **Why**: Rewiring response-time-over-time is PRD out-of-scope. Regression requirements were moved into F4 acceptance criteria/tests.\n

4. **Accepted**: Merge empty/error states into the owning stories.\n
   - **Why**: Empty/error states are now required in F3 (dashboard/stats) and F4 (pie widget), rather than bolted on later.\n

5. **Accepted**: Split B1 into B1a + B1b.\n
   - **Why**: Repository contracts and ES implementation/adapters are distinct work with different risk/effort.\n

6. **Accepted**: Make backend query validation explicit.\n
   - **Why**: B3 now has a dedicated validation subtask (required params, range checks, interval/bucket caps, supported fields).\n

7. **Accepted**: Make UI removals explicit.\n
   - **Why**: F3 explicitly removes the “Average Connection Duration” stat card; F4 explicitly replaces `api-analytics-response-status-ranges` with the per-code pie chart.\n

8. **Accepted (design alignment)**: Prefer per-type use cases.\n
   - **Why**: B2 now follows the existing pattern (`Search*UseCase` per behavior) while keeping the unified REST entry point.\n

9. **Accepted**: Decouple F1 from backend availability.\n
   - **Why**: F1 can be implemented against PRD shapes with hand-written TS interfaces; integration happens when B3 is available.\n

10. **Partially accepted**: MapStruct complexity note.\n
   - **Why**: B3 now recommends manual response construction for the discriminated union response, keeping MapStruct only for simple mappings.\n
