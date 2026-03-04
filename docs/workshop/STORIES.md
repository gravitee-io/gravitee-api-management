# V4 API Analytics Dashboard – Implementation Subtasks

This document breaks each user story into implementation subtasks with files to create/modify, tests, reference patterns, and documentation updates. Package and directory names follow the gravitee-api-management monorepo conventions.

**Dependencies (high level):** Stories 1–4 depend on nothing; Story 5 depends on 1–4; Story 6 depends on 1–4; Stories 7–9 depend on 6; Story 10 depends on 7–9; Story 11 depends on 1–5; Story 12 depends on 6–10.

---

## Story 1: Backend – Unified analytics endpoint and COUNT

**Dependencies:** None

### Subtask 1.1 – Repository API: COUNT contract and query model (if not already present)

- **Create/Modify:**
  - **Create** (if missing): `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/CountAggregate.java` – already exists; ensure it has `total` (or equivalent) for COUNT response.
  - **Reference:** `RequestsCountQuery.java` in same package; `CountAggregate.java` (existing).
- **Tests:** None at repository-api (model only). Repository impl tests in Story 11.
- **Documentation:** None.

### Subtask 1.2 – Repository: COUNT over v4-metrics only

- **Create/Modify:**
  - **Modify:** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/api/AnalyticsRepository.java` – add method e.g. `Optional<CountAggregate> searchV4Count(QueryContext, String apiId, long from, long to)` **or** document that existing `searchRequestsCount` is used with v4-metrics index only.
  - **Modify:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepository.java` – ensure COUNT path uses only `Type.V4_METRICS` index (via `indexNameGenerator.getWildcardIndexName(..., Type.V4_METRICS, clusters)`).
  - **Reference:** `SearchRequestsCountQueryAdapter.java`, `SearchRequestsCountResponseAdapter.java` in `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/.../v4/analytics/adapter/`.
- **Tests:** Unit test that COUNT query/build uses v4-metrics index only (e.g. in adapter test or repository test). See Story 11.
- **Documentation:** None.

### Subtask 1.3 – Service: expose COUNT for unified endpoint

- **Create/Modify:**
  - **Modify:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java` – add `Optional<V4AnalyticsCount> searchV4AnalyticsCount(ExecutionContext, String apiId, long from, long to)`.
  - **Modify:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImpl.java` – implement by calling repository `searchRequestsCount` (or new COUNT method) and map to `V4AnalyticsCount`.
  - **Create** (if missing): `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/V4AnalyticsCount.java` – DTO with `count` (long).
  - **Reference:** `AnalyticsQueryServiceImpl.searchRequestsCount` and mapping to `RequestsCount`.
- **Tests:** In `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImplTest.java` – add nested class or tests for `searchV4AnalyticsCount` (mock repository, assert returned count).
- **Documentation:** None.

### Subtask 1.4 – REST: GET .../analytics with type=COUNT

- **Create/Modify:**
  - **Modify:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java` – add `@GET` method (no `@Path`) that accepts `type`, `from`, `to`; when `type=COUNT` call `analyticsQueryService.searchV4AnalyticsCount` and return `{ "type": "COUNT", "count": <number> }` (e.g. via `Response.ok(body).build()`).
  - **Reference:** Existing `getApiAnalyticsRequestCount` in same class; `AbstractResource` for permission helpers.
- **Tests:** In `ApiAnalyticsResourceTest.java` – add nested class e.g. `UnifiedAnalytics` with test `should_return_COUNT_when_type_is_COUNT` (mock/fake `AnalyticsQueryService.searchV4AnalyticsCount`, assert 200 and JSON shape). See Story 11.
- **Documentation:** None.

### Subtask 1.5 – OpenAPI: path and schema for unified analytics (COUNT)

- **Create/Modify:**
  - **Modify:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-apis.yaml` – under `paths`, add `GET /environments/{envId}/apis/{apiId}/analytics` with query params `type`, `from`, `to`; under `components/schemas` add `ApiV4AnalyticsCountResponse` with `type` (enum COUNT), `count` (integer int64).
  - **Reference:** Existing `GET .../analytics/requests-count` and `ApiAnalyticsRequestsCountResponse` in same file.
- **Tests:** None (spec only).
- **Documentation:** None.

---

## Story 2: Backend – STATS analytics type

**Dependencies:** Story 1 (same unified endpoint and permission).

### Subtask 2.1 – Repository API: STATS aggregate and query

- **Create/Modify:**
  - **Create:** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/StatsAggregate.java` – fields: count, min, max, avg, sum.
  - **Create:** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/ApiAnalyticsStatsQuery.java` – apiId, from, to, field.
  - **Modify:** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/api/AnalyticsRepository.java` – add `Optional<StatsAggregate> searchStats(QueryContext, ApiAnalyticsStatsQuery)`.
  - **Reference:** `CountAggregate`, `RequestsCountQuery` in same package.
- **Tests:** None at API layer (POJOs).
- **Documentation:** None.

### Subtask 2.2 – Repository ES: STATS adapter and v4-metrics query

- **Create/Modify:**
  - **Create:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/V4ApiAnalyticsFieldMapping.java` – map API field names (e.g. `gateway-response-time-ms`) to ES field names for v4-metrics index.
  - **Create:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/V4ApiAnalyticsStatsAdapter.java` – build ES query (bool filter: api-id + range @timestamp; aggs: stats on field), adapt response to `StatsAggregate`.
  - **Modify:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepository.java` – implement `searchStats` using index `Type.V4_METRICS` and `V4ApiAnalyticsStatsAdapter`.
  - **Modify:** `gravitee-apim-repository/gravitee-apim-repository-noop/src/main/java/io/gravitee/repository/noop/log/v4/NoOpAnalyticsRepository.java` – add `searchStats` returning `Optional.empty()`.
  - **Reference:** `SearchRequestsCountQueryAdapter` (filter + aggs); `StatsQueryCommand` in `gravitee-apim-repository/.../analytics/query/` for stats response handling.
- **Tests:** **Create** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/V4ApiAnalyticsStatsAdapterTest.java` – test query JSON contains api-id and range and stats agg; test response adaptation from mock aggregation (pattern: `SearchRequestsCountQueryAdapterTest`, `SearchRequestsCountResponseAdapterTest`).
- **Documentation:** None.

### Subtask 2.3 – Service and REST: STATS in unified endpoint

- **Create/Modify:**
  - **Create** (if missing): `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/V4AnalyticsStats.java` – count, min, max, avg, sum.
  - **Modify:** `AnalyticsQueryService.java` – add `Optional<V4AnalyticsStats> searchV4AnalyticsStats(ExecutionContext, String apiId, long from, long to, String field)`.
  - **Modify:** `AnalyticsQueryServiceImpl.java` – implement using `AnalyticsRepository.searchStats`, map to `V4AnalyticsStats`.
  - **Modify:** `ApiAnalyticsResource.java` – in unified GET handler, add case `STATS`: require `field`, call `searchV4AnalyticsStats`, return `{ type, count, min, max, avg, sum }`.
  - **Reference:** Story 1 service and resource changes.
- **Tests:** `AnalyticsQueryServiceImplTest` – test `searchV4AnalyticsStats`; `ApiAnalyticsResourceTest` – test unified endpoint returns STATS shape and 400 when field missing (Story 11).
- **Documentation:** None.

### Subtask 2.4 – OpenAPI: STATS response schema

- **Create/Modify:** `openapi-apis.yaml` – add schema `ApiV4AnalyticsStatsResponse` (type enum STATS, count, min, max, avg, sum); reference in unified path response oneOf if using oneOf.
- **Tests:** None.
- **Documentation:** None.

---

## Story 3: Backend – GROUP_BY analytics type

**Dependencies:** Story 1.

### Subtask 3.1 – Repository API: GROUP_BY aggregate and query

- **Create/Modify:**
  - **Create:** `GroupByAggregate.java` and `ApiAnalyticsGroupByQuery.java` in `io.gravitee.repository.log.v4.model.analytics` (values map, metadata map; query: apiId, from, to, field, size, order).
  - **Modify:** `AnalyticsRepository.java` – add `Optional<GroupByAggregate> searchGroupBy(QueryContext, ApiAnalyticsGroupByQuery)`.
  - **Reference:** `TopHitsAggregate`, `TopHitsQueryCriteria`; `AggregateValueCountByFieldAdapter`.
- **Tests:** None at API.
- **Documentation:** None.

### Subtask 3.2 – Repository ES: GROUP_BY adapter

- **Create/Modify:**
  - **Create:** `V4ApiAnalyticsGroupByAdapter.java` in same adapter package – build ES query (filter api-id + range; terms agg on field with size/order), adapt response to `GroupByAggregate` (values + metadata with name).
  - **Modify:** `AnalyticsElasticsearchRepository.java` – implement `searchGroupBy` with `Type.V4_METRICS`; **Modify** `NoOpAnalyticsRepository.java` – add `searchGroupBy` returning empty.
  - **Reference:** `AggregateValueCountByFieldAdapter`, `V4ApiAnalyticsFieldMapping`.
- **Tests:** **Create** `V4ApiAnalyticsGroupByAdapterTest.java` – query structure and response adaptation.
- **Documentation:** None.

### Subtask 3.3 – Service and REST: GROUP_BY in unified endpoint

- **Create/Modify:** `V4AnalyticsGroupBy.java` in rest-api-model; `AnalyticsQueryService` + impl `searchV4AnalyticsGroupBy`; `ApiAnalyticsResource` case GROUP_BY (field required, size default 10, order optional).
- **Tests:** Service test + resource test for GROUP_BY response and validation (Story 11).
- **Documentation:** None.

### Subtask 3.4 – OpenAPI: GROUP_BY response schema

- **Create/Modify:** `openapi-apis.yaml` – add `ApiV4AnalyticsGroupByResponse` (type, values object, metadata object).
- **Tests:** None.
- **Documentation:** None.

---

## Story 4: Backend – DATE_HISTO analytics type

**Dependencies:** Story 1.

### Subtask 4.1 – Repository API: DATE_HISTO aggregate and query

- **Create/Modify:** `DateHistoAggregate.java` (timestamp list, values list of { field, buckets, metadata }); `ApiAnalyticsDateHistoQuery.java` (apiId, from, to, field, interval ms). Add `Optional<DateHistoAggregate> searchDateHisto(QueryContext, ApiAnalyticsDateHistoQuery)` to `AnalyticsRepository`.
  - **Reference:** `ResponseStatusOverTimeAggregate`, `SearchResponseStatusOverTimeAdapter`.
- **Tests:** None at API.
- **Documentation:** None.

### Subtask 4.2 – Repository ES: DATE_HISTO adapter

- **Create/Modify:** `V4ApiAnalyticsDateHistoAdapter.java` – date_histogram on @timestamp with interval, sub-aggregation terms on field; adapt to `DateHistoAggregate`. Use `ElasticsearchInfo` for fixed_interval vs interval. **Modify** `AnalyticsElasticsearchRepository` and `NoOpAnalyticsRepository`.
  - **Reference:** `SearchResponseStatusOverTimeAdapter` (date_histogram + terms), `ResponseTimeRangeQueryAdapter`.
- **Tests:** **Create** `V4ApiAnalyticsDateHistoAdapterTest.java` – query and response adaptation.
- **Documentation:** None.

### Subtask 4.3 – Service and REST: DATE_HISTO in unified endpoint

- **Create/Modify:** `V4AnalyticsDateHisto.java` in model; `AnalyticsQueryService.searchV4AnalyticsDateHisto`; `ApiAnalyticsResource` case DATE_HISTO (field and interval required, interval range validation).
- **Tests:** Service + resource tests (Story 11).
- **Documentation:** None.

### Subtask 4.4 – OpenAPI: DATE_HISTO response schema

- **Create/Modify:** `openapi-apis.yaml` – add `ApiV4AnalyticsDateHistoResponse` (type, timestamp array, values array of { field, buckets, metadata }).
- **Tests:** None.
- **Documentation:** None.

---

## Story 5: Backend – Permission and validation

**Dependencies:** Stories 1–4.

### Subtask 5.1 – Permission on unified endpoint

- **Create/Modify:** `ApiAnalyticsResource.java` – ensure the unified `@GET` method is annotated with `@Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })` (same as existing analytics methods in same class).
  - **Reference:** `getApiAnalyticsRequestCount`, `getResponseStatusRanges` in same file.
- **Tests:** In `ApiAnalyticsResourceTest` – add test `should_return_403_when_no_API_ANALYTICS_READ` for the unified endpoint (e.g. GET with type=COUNT, assert 403 when permission denied). **Reference:** nested class `RequestsCountAnalytics.should_return_403_if_incorrect_permissions`.
- **Documentation:** None.

### Subtask 5.2 – Validation: type, from, to, field, interval

- **Create/Modify:** `ApiAnalyticsResource.java` – validate: type required and one of COUNT|STATS|GROUP_BY|DATE_HISTO; from and to required and from < to; for STATS and GROUP_BY require field; for DATE_HISTO require field and interval (and interval in range e.g. 1000–1_000_000_000). Throw `BadRequestException` or return 400 with message.
  - **Reference:** v1 `AnalyticsParam.validate()` in `gravitee-apim-rest-api-management/.../param/AnalyticsParam.java` for similar rules.
- **Tests:** In `ApiAnalyticsResourceTest` – tests: missing type → 400; missing from/to → 400; from >= to → 400; STATS without field → 400; GROUP_BY without field → 400; DATE_HISTO without field or interval → 400; invalid interval range → 400.
- **Documentation:** None.

---

## Story 6: Frontend – Angular service for unified endpoint

**Dependencies:** Stories 1–4 (backend available).

### Subtask 6.1 – Service method and types

- **Create/Modify:**
  - **Modify:** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts` – add types `V4AnalyticsType`, `V4AnalyticsParams` (from, to, type, field?, interval?, size?, order?), `V4AnalyticsResponse` (discriminated union: COUNT | STATS | GROUP_BY | DATE_HISTO). Add method `getV4Analytics(apiId: string, params: V4AnalyticsParams): Observable<V4AnalyticsResponse>` that builds URL `{v2BaseURL}/apis/${apiId}/analytics` with URLSearchParams and returns `this.http.get<V4AnalyticsResponse>(url)`.
  - **Reference:** Existing `getRequestsCount`, `getResponseStatusRanges` in same file (URL pattern, timeRangeFilter usage).
- **Tests:** **Modify** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.spec.ts` – add tests for `getV4Analytics`: call with each type, expect correct URL and params; mock HTTP response and assert returned shape (COUNT, STATS, GROUP_BY, DATE_HISTO). **Reference:** existing tests in same spec file.
- **Documentation:** None.

---

## Story 7: Frontend – Stats cards (four metrics)

**Dependencies:** Story 6.

### Subtask 7.1 – Wire stats to unified endpoint

- **Create/Modify:**
  - **Modify:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts` – replace or extend data source so that four stats come from `getV4Analytics`: (1) COUNT → Total Requests, (2) STATS field `gateway-response-time-ms` → Avg Gateway Response Time, (3) STATS field `endpoint-response-time-ms` → Avg Upstream Response Time, (4) STATS field `request-content-length` → Avg Content Length. Use `timeRangeFilter()` and `forkJoin` (or equivalent) to call `getV4Analytics` four times (or batch if backend supports), then map to `requestStats` array for `app-api-analytics-request-stats`.
  - **Reference:** Existing `api-analytics-proxy.component.ts` use of `getRequestsCount$`, `getAverageConnectionDuration$`; `ApiAnalyticsRequestStatsComponent` input `requestsStats: AnalyticsRequestStats` (array of { label, value?, unitLabel?, isLoading }).
- **Tests:** In `api-analytics-proxy.component.spec.ts` – ensure after flushing time range and HTTP: four requests to `.../analytics` with type=COUNT and type=STATS with respective fields; stats cards show correct labels and values (or harness). **Reference:** existing spec that expects `getRequestsCount` and `getAverageConnectionDuration` requests.
- **Documentation:** None.

### Subtask 7.2 – Loading and empty values

- **Create/Modify:** Same component – show loading state while requests in flight; when a stat has no data, show "-" or 0 as per existing `api-analytics-request-stats` component (which uses `isNumber` for value display).
  - **Reference:** `api-analytics-request-stats.component.html` and `.ts`.
- **Tests:** Assert loading state; assert that missing/zero values do not break the template.
- **Documentation:** None.

---

## Story 8: Frontend – HTTP status pie chart

**Dependencies:** Story 6.

### Subtask 8.1 – Data from GROUP_BY status

- **Create/Modify:** `api-analytics-proxy.component.ts` – add (or keep) a stream that calls `getV4Analytics(apiId, { type: 'GROUP_BY', from, to, field: 'status', size: 10 })` when time range is set. Map response to the shape expected by `api-analytics-response-status-ranges` (e.g. `data: { label: string; value: number }[]` from GROUP_BY values). Pass to existing component `api-analytics-response-status-ranges` (or equivalent pie chart) so the dashboard shows HTTP status pie.
  - **Reference:** `ApiAnalyticsResponseStatusRangesComponent` input `responseStatusRanges: ApiAnalyticsResponseStatusRanges` (isLoading, data array); `shared/components/api-analytics-response-status-ranges` and `GioChartPieModule`.
- **Tests:** In `api-analytics-proxy.component.spec.ts` – mock GROUP_BY response (e.g. { "200": 100, "404": 10 }); assert pie chart component receives correct data or that section is visible with expected data.
- **Documentation:** None.

### Subtask 8.2 – Layout (two-column)

- **Create/Modify:** `api-analytics-proxy.component.html` and `.scss` – ensure layout has HTTP status pie and “response status over time” line chart in a two-column row (e.g. grid or flex). **Reference:** existing `gridContent` and `full-bleed` in `api-analytics-proxy.component.scss`.
- **Tests:** Optional: harness or DOM assertion for layout.
- **Documentation:** None.

---

## Story 9: Frontend – Existing line charts unchanged

**Dependencies:** Story 6 (and optionally Story 4 if re-wiring).

### Subtask 9.1 – Verify line charts still work

- **Create/Modify:** No mandatory code change. Optionally ensure `api-analytics-response-status-overtime` and `api-analytics-response-time-over-time` still receive `timeRangeFilter()` and call existing endpoints `getResponseStatusOvertime(apiId)` and `getResponseTimeOverTime(apiId)` (or, if product decision is to use unified DATE_HISTO, wire them to `getV4Analytics` with type=DATE_HISTO and appropriate field/interval).
  - **Reference:** `api-analytics-response-status-overtime.component.ts`, `api-analytics-response-time-over-time.component.ts`; `ApiAnalyticsV2Service.getResponseStatusOvertime`, `getResponseTimeOverTime`.
- **Tests:** In `api-analytics-proxy.component.spec.ts` – after setting time range, expect requests to `response-status-overtime` and `response-time-over-time` (or to unified analytics with DATE_HISTO); assert charts render (e.g. component present, no errors). **Reference:** existing tests that flush HTTP for these endpoints.
- **Documentation:** None.

---

## Story 10: Frontend – Empty and error states

**Dependencies:** Stories 7, 8, 9.

### Subtask 10.1 – Empty state

- **Create/Modify:** `api-analytics-proxy.component.ts` and `.html` – when unified (and/or existing) analytics return no data (e.g. count 0, empty GROUP_BY), show an empty state (e.g. `gio-card-empty-state` with a message like “No analytics data for this period”). **Reference:** existing “Enable Analytics” empty state in same template; `GioCardEmptyStateModule`.
- **Tests:** In `api-analytics-proxy.component.spec.ts` – mock responses with empty/zero data; assert empty state is displayed (e.g. by harness or text content).
- **Documentation:** None.

### Subtask 10.2 – Error state

- **Create/Modify:** `api-analytics-proxy.component.ts` and `.html` – when any analytics request fails (e.g. 500 or network error), set an error flag and show an error state (e.g. `gio-card-empty-state` with icon and message “Analytics unavailable” or similar). Keep timeframe filter visible so user can retry or change range.
  - **Reference:** Same empty-state component; `catchError` in observable chain.
- **Tests:** Mock HTTP error (e.g. 500) for one of the analytics calls; assert error state is shown and (if applicable) filter bar still visible.
- **Documentation:** None.

---

## Story 11: Tests – Backend analytics query types

**Dependencies:** Stories 1–5.

### Subtask 11.1 – Resource tests: unified endpoint

- **Create/Modify:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResourceTest.java` – add nested class e.g. `UnifiedV4Analytics` with:
  - Test permission: 403 when `API_ANALYTICS:READ` is false (reuse pattern from `RequestsCountAnalytics.should_return_403_if_incorrect_permissions`; target path `""` or `?type=COUNT&from=...&to=...`).
  - Test COUNT: mock/fake `searchV4AnalyticsCount` to return count; GET with type=COUNT&from=&to=; assert 200 and JSON `type=COUNT`, `count` value.
  - Test STATS: mock `searchV4AnalyticsStats`; GET with type=STATS&field=gateway-response-time-ms; assert 200 and min/max/avg/sum/count.
  - Test GROUP_BY: mock `searchV4AnalyticsGroupBy`; GET with type=GROUP_BY&field=status; assert 200 and values/metadata.
  - Test DATE_HISTO: mock `searchV4AnalyticsDateHisto`; GET with type=DATE_HISTO&field=status&interval=3600000; assert 200 and timestamp/values.
  - **Reference:** Existing nested classes in same file; `FakeAnalyticsQueryService` in `gravitee-apim-rest-api-service/src/test/java/fakes/` – extend fake to hold V4 analytics returns and implement `searchV4AnalyticsCount`, `searchV4AnalyticsStats`, `searchV4AnalyticsGroupBy`, `searchV4AnalyticsDateHisto` for resource test.
- **Fake:** **Modify** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/fakes/FakeAnalyticsQueryService.java` – add fields and implement the four `searchV4*` methods so resource test can inject and control responses.
- **Documentation:** None.

### Subtask 11.2 – Resource tests: validation

- **Create/Modify:** Same `ApiAnalyticsResourceTest` – add tests: type missing → 400; type invalid → 400; from/to missing → 400; from >= to → 400; STATS without field → 400; GROUP_BY without field → 400; DATE_HISTO without field or interval → 400; interval out of range → 400. Use `rootTarget().path("")` or query params on analytics path; assert response status and error body.
  - **Reference:** Validation tests in other resources; `BadRequestException` handling.
- **Documentation:** None.

### Subtask 11.3 – Service layer tests

- **Create/Modify:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImplTest.java` – add nested classes or methods for `searchV4AnalyticsCount`, `searchV4AnalyticsStats`, `searchV4AnalyticsGroupBy`, `searchV4AnalyticsDateHisto`: mock `AnalyticsRepository` (or v4 methods), call service, assert returned DTO and repository invocation (ArgumentCaptor for query). **Reference:** existing tests in same file for `searchRequestsCount`, `searchResponseStatusRanges`.
- **Documentation:** None.

### Subtask 11.4 – Adapter / repository tests (optional but recommended)

- **Create/Modify:** **Create** tests under `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/` for `V4ApiAnalyticsStatsAdapter`, `V4ApiAnalyticsGroupByAdapter`, `V4ApiAnalyticsDateHistoAdapter` – assert query JSON structure (api-id term, range, aggs) and response mapping from stub aggregation. **Reference:** `SearchRequestsCountQueryAdapterTest`, `SearchResponseStatusOverTimeAdapterTest`, `AggregateValueCountByFieldAdapterTest`.
- **Documentation:** None.

---

## Story 12: Tests – Angular dashboard and widgets

**Dependencies:** Stories 6–10.

### Subtask 12.1 – ApiAnalyticsV2Service tests

- **Create/Modify:** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.spec.ts` – ensure `getV4Analytics` is fully covered: URL and params for each type; error propagation (e.g. 500 → observable error). **Reference:** existing tests in same file.
- **Documentation:** None.

### Subtask 12.2 – ApiAnalyticsProxyComponent: stats and pie

- **Create/Modify:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.spec.ts` – tests: with valid time range and mocked `getV4Analytics` (COUNT + STATS x3 + GROUP_BY), assert four stats cards show correct labels and values and HTTP status pie receives GROUP_BY data; use `HttpTestingController` to flush multiple analytics requests. **Reference:** existing spec that uses `fakeAnalyticsRequestsCount`, `fakeAnalyticsResponseStatusRanges`; `ApiAnalyticsProxyHarness` in same folder.
- **Documentation:** None.

### Subtask 12.3 – ApiAnalyticsProxyComponent: line charts

- **Create/Modify:** Same spec – ensure “response status over time” and “response time over time” still receive data when endpoints are flushed (existing or unified); assert no regression (e.g. components present, no runtime errors). **Reference:** existing tests for `response-status-overtime` and `response-time-over-time` requests.
- **Documentation:** None.

### Subtask 12.4 – Empty and error states

- **Create/Modify:** Same spec – test empty state: mock all analytics with empty/zero data, assert empty state message is shown. Test error state: mock one or all analytics requests to return 500 (or error), assert error state is shown; optionally assert filter bar or retry path. **Reference:** existing “analytics disabled” empty state test; `catchError` behavior.
- **Documentation:** None.

### Subtask 12.5 – Harness (if needed)

- **Create/Modify:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.harness.ts` – add loader/empty/error/stats/pie selectors if not already present so specs can assert robustly. **Reference:** existing harness in same file; `GioLoaderHarness`, `GioCardEmptyStateHarness` from ui-particles if used.
- **Documentation:** None.

---

## File and package reference

| Layer | Package / path pattern |
|-------|-------------------------|
| Repository API | `io.gravitee.repository.log.v4.api`, `io.gravitee.repository.log.v4.model.analytics` |
| Repository ES | `io.gravitee.repository.elasticsearch.v4.analytics`, `...adapter` |
| Repository NoOp | `io.gravitee.repository.noop.log.v4.NoOpAnalyticsRepository` |
| Rest API model | `io.gravitee.rest.api.model.v4.analytics` (under `gravitee-apim-rest-api-model`) |
| Service core | `io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService` |
| Service impl | `io.gravitee.apim.infra.query_service.analytics.AnalyticsQueryServiceImpl` |
| REST resource | `io.gravitee.rest.api.management.v2.rest.resource.api.analytics.ApiAnalyticsResource` |
| OpenAPI | `gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/openapi-apis.yaml` |
| Console service | `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts` |
| Console proxy | `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/` |
| Console components | `.../api-traffic-v4/analytics/components/`, `.../shared/components/api-analytics-response-status-ranges/` |
| REST tests | `...rest/resource/api/analytics/ApiAnalyticsResourceTest.java`; fakes in `...service/.../fakes/FakeAnalyticsQueryService.java` |
| Service tests | `...service/.../analytics/AnalyticsQueryServiceImplTest.java` |
| Adapter tests | `...repository-elasticsearch/.../v4/analytics/adapter/*AdapterTest.java` |
| Angular specs | `*.component.spec.ts`, `*.service.spec.ts` next to component/service |

---

## Documentation to update

- **Optional:** Add or update a short section in `docs/v4-api-analytics-dashboard-user-stories.md` (or a separate “Implementation” doc under `docs/workshop/`) that points to this STORIES.md and summarizes the main packages and entry points (unified endpoint URL, service method name, component names). No mandatory doc changes are specified in the stories above.
