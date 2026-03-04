# V4 API Analytics Dashboard — Story Breakdown

## Conventions

| Abbreviation | Meaning |
|---|---|
| **BE** | Backend (Java, REST API, service, repository) |
| **FE** | Frontend (Angular, TypeScript) |
| **S / M / L** | T-shirt size complexity |

### Codebase root aliases

| Alias | Absolute path |
|---|---|
| `REST_V2` | `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest` |
| `SERVICE` | `gravitee-apim-rest-api/gravitee-apim-rest-api-service` |
| `REPO_API` | `gravitee-apim-repository/gravitee-apim-repository-api` |
| `REPO_ES` | `gravitee-apim-repository/gravitee-apim-repository-elasticsearch` |
| `CONSOLE` | `gravitee-apim-console-webui` |

---

## BE-1: Query Parameter Model & Validation

**Layer:** Backend | **Size:** S | **Dependencies:** None

> As an API publisher, I want the analytics endpoint to validate my query
> parameters so that I get clear 400 errors for invalid requests.

### Subtasks

#### BE-1.1 Create `AnalyticsType` enum (V2)

- **Create** `REST_V2/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/param/AnalyticsType.java`
  - Enum: `COUNT`, `STATS`, `GROUP_BY`, `DATE_HISTO`
- **Pattern:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/src/main/java/io/gravitee/rest/api/management/rest/resource/param/AnalyticsType.java`

#### BE-1.2 Create `AnalyticsParam` bean param (V2)

- **Create** `REST_V2/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/param/AnalyticsParam.java`
  - Fields: `type` (required, `AnalyticsType`), `from` (required, `long`), `to` (required, `long`), `field` (`String`), `interval` (`long`), `size` (`int`, default 10), `order` (`String`)
  - `validate()` method: `type` required; `from`/`to` required and `from < to`; `interval` required and in `[1000, 1_000_000_000]` for `DATE_HISTO`; `field` required for `STATS` and `GROUP_BY`
- **Pattern:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/src/main/java/io/gravitee/rest/api/management/rest/resource/param/AnalyticsParam.java` (lines 31-216)

#### BE-1.3 Create `AnalyticsFieldParam` enum (supported fields)

- **Create** `REST_V2/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/param/AnalyticsFieldParam.java`
  - Values: `status`, `mapped-status`, `application`, `plan`, `host`, `uri`, `gateway-latency-ms`, `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`
  - Add field validation in `AnalyticsParam.validate()`

#### BE-1.4 Tests for validation

- **Tested as part of BE-6** (integration tests in `ApiAnalyticsResourceTest.java`)
  - 400 for missing `type`
  - 400 for `from >= to`
  - 400 for `GROUP_BY` without `field`
  - 400 for `DATE_HISTO` without `interval`
  - 400 for unsupported `field` value

---

## BE-2: COUNT Query Type + Endpoint Scaffolding

**Layer:** Backend | **Size:** M | **Dependencies:** BE-1

> As an API publisher, I want to query `GET /v2/apis/{apiId}/analytics?type=COUNT`
> so that I get the total number of requests for a time range.

### Subtasks

#### BE-2.1 Define OpenAPI schema for unified endpoint

- **Modify** `REST_V2/src/main/resources/openapi/openapi-apis.yaml`
  - Add path `/environments/{envId}/apis/{apiId}/analytics` with GET operation
  - Add query parameters: `type`, `from`, `to`, `field`, `interval`, `size`, `order`
  - Add response schemas: `ApiAnalyticsCountResponse`, `ApiAnalyticsStatsResponse`, `ApiAnalyticsGroupByResponse`, `ApiAnalyticsDateHistoResponse`
  - Use `oneOf` discriminated by `type` field, or define 4 separate response schemas referenced from separate response definitions
- **Pattern:** Existing analytics response schemas at line ~8232 of `openapi-apis.yaml`
- **Doc:** OpenAPI spec is the API contract doc

#### BE-2.2 Create `SearchApiAnalyticsUseCase`

- **Create** `SERVICE/src/main/java/io/gravitee/apim/core/analytics/use_case/SearchApiAnalyticsUseCase.java`
  - `@UseCase` + `@RequiredArgsConstructor`
  - `Input` record: `apiId`, `environmentId`, `type` (enum), `from`, `to`, `field`, `interval`, `size`, `order`
  - `Output` record: wraps a generic analytics result (polymorphic — count, stats, group-by, or date-histo)
  - `execute()`: validates API (V4, not TCP, belongs to env — reuse pattern from `SearchRequestsCountAnalyticsUseCase`), then dispatches by `type`
  - For `COUNT`: call `analyticsQueryService.searchCount(ctx, apiId, from, to)`
- **Pattern:** `SERVICE/src/main/java/io/gravitee/apim/core/analytics/use_case/SearchRequestsCountAnalyticsUseCase.java`
- **Injection:** Automatic via `@UseCase` + `UsecaseSpringConfiguration` component scan

#### BE-2.3 Add `searchCount` to `AnalyticsQueryService`

- **Modify** `SERVICE/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java`
  - Add: `Optional<Long> searchCount(ExecutionContext ctx, String apiId, Instant from, Instant to)`

#### BE-2.4 Implement `searchCount` in `AnalyticsQueryServiceImpl`

- **Modify** `SERVICE/src/main/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImpl.java`
  - Build `CountQuery` (new repo model) from params
  - Call `analyticsRepository.searchCount(queryContext, countQuery)`
  - Map `CountResult` → `Long`
- **Pattern:** Existing `searchRequestsCount` method in the same file

#### BE-2.5 Add `searchCount` to V4 `AnalyticsRepository`

- **Modify** `REPO_API/src/main/java/io/gravitee/repository/log/v4/api/AnalyticsRepository.java`
  - Add: `Optional<Long> searchCount(QueryContext ctx, CountQuery query)`

#### BE-2.6 Create `CountQuery` repository model

- **Create** `REPO_API/src/main/java/io/gravitee/repository/log/v4/model/analytics/CountQuery.java`
  - Record with: `String apiId`, `Instant from`, `Instant to`
  - Use `@Builder(toBuilder = true)`
- **Pattern:** `REPO_API/.../log/v4/model/analytics/RequestsCountQuery.java`

#### BE-2.7 Create ES count query adapter

- **Create** `REPO_ES/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchCountQueryAdapter.java`
  - Static `adapt(CountQuery query) -> String` (ES JSON)
  - Build `query.bool.must` with `term("api-id", apiId)` + `range("@timestamp", from, to)`
  - `size: 0` (count only)
- **Pattern:** `REPO_ES/.../v4/analytics/adapter/SearchRequestsCountQueryAdapter.java`

#### BE-2.8 Create ES count response adapter

- **Create** `REPO_ES/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchCountResponseAdapter.java`
  - Static `adapt(SearchResponse response) -> Optional<Long>`
  - Read `total.value` from ES response
- **Pattern:** `REPO_ES/.../v4/analytics/adapter/SearchRequestsCountResponseAdapter.java`

#### BE-2.9 Implement `searchCount` in `AnalyticsElasticsearchRepository`

- **Modify** `REPO_ES/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepository.java`
  - Use `V4_METRICS` index, call `client.search(index, null, SearchCountQueryAdapter.adapt(query))`
  - Map via `SearchCountResponseAdapter.adapt()`
- **Pattern:** Existing `searchRequestsCount` method in same file

#### BE-2.10 Add unified `getAnalytics` endpoint in REST resource

- **Modify** `REST_V2/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`
  - Add method: `getAnalytics(@BeanParam AnalyticsParam param)` at `@Path("")` (root path)
  - `@GET`, `@Produces(APPLICATION_JSON)`, `@Permissions(API_ANALYTICS:READ)`
  - Call `param.validate()`, build use case `Input`, dispatch, map result via `ApiAnalyticsMapper`
- **Pattern:** V1 `ApiAnalyticsResource.getApiAnalyticsHits()` (line 73-93) for the switch/dispatch pattern

#### BE-2.11 Update `ApiAnalyticsMapper` for COUNT

- **Modify** `REST_V2/src/main/java/io/gravitee/rest/api/management/v2/rest/mapper/ApiAnalyticsMapper.java`
  - Add mapping for count result → `ApiAnalyticsCountResponse` (generated from OpenAPI)

#### BE-2.12 Add `searchCount` to `FakeAnalyticsQueryService`

- **Modify** `SERVICE/src/test/java/fakes/FakeAnalyticsQueryService.java`
  - Add `public Long countResult` field
  - Implement `searchCount()` returning `Optional.ofNullable(countResult)`

### Tests

- **Adapter unit tests:** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchCountQueryAdapterTest.java` (new)
- **Adapter unit tests:** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchCountResponseAdapterTest.java` (new)
- **Integration tests:** covered in BE-6

---

## BE-3: STATS Query Type

**Layer:** Backend | **Size:** M | **Dependencies:** BE-1, BE-2

> As an API publisher, I want to query `?type=STATS&field=gateway-response-time-ms`
> so that I get min/max/avg/sum statistics for a metric.

### Subtasks

#### BE-3.1 Create core domain model for stats result

- **Create** `SERVICE/src/main/java/io/gravitee/apim/core/analytics/model/StatsResult.java`
  - Record or `@Data @Builder` class with: `long count`, `double min`, `double max`, `double avg`, `double sum`

#### BE-3.2 Add `searchStats` to `AnalyticsQueryService`

- **Modify** `SERVICE/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java`
  - Add: `Optional<StatsResult> searchStats(ExecutionContext ctx, String apiId, Instant from, Instant to, String field)`

#### BE-3.3 Implement `searchStats` in `AnalyticsQueryServiceImpl`

- **Modify** `SERVICE/src/main/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImpl.java`
  - Build `StatsQuery` (new repo model), call repository, map `StatsAggregate` → `StatsResult`

#### BE-3.4 Create `StatsQuery` and `StatsAggregate` repository models

- **Create** `REPO_API/src/main/java/io/gravitee/repository/log/v4/model/analytics/StatsQuery.java`
  - Record: `String apiId`, `Instant from`, `Instant to`, `String field`
- **Create** `REPO_API/src/main/java/io/gravitee/repository/log/v4/model/analytics/StatsAggregate.java`
  - Record: `long count`, `double min`, `double max`, `double avg`, `double sum`

#### BE-3.5 Add `searchStats` to V4 `AnalyticsRepository`

- **Modify** `REPO_API/src/main/java/io/gravitee/repository/log/v4/api/AnalyticsRepository.java`
  - Add: `Optional<StatsAggregate> searchStats(QueryContext ctx, StatsQuery query)`

#### BE-3.6 Create ES stats query/response adapters

- **Create** `REPO_ES/.../v4/analytics/adapter/SearchStatsQueryAdapter.java`
  - Build ES JSON: `size: 0`, filter by `api-id` + `@timestamp`, `aggs.stats_field.stats` on `query.field()`
- **Create** `REPO_ES/.../v4/analytics/adapter/SearchStatsResponseAdapter.java`
  - Parse ES stats aggregation response → `StatsAggregate`
- **Pattern:** V1 Freemarker `stats.ftl` for the ES query structure

#### BE-3.7 Implement `searchStats` in `AnalyticsElasticsearchRepository`

- **Modify** `REPO_ES/.../v4/analytics/AnalyticsElasticsearchRepository.java`
  - `V4_METRICS` index, call adapters

#### BE-3.8 Wire STATS into use case + mapper

- **Modify** `SearchApiAnalyticsUseCase` — add STATS branch calling `analyticsQueryService.searchStats()`
- **Modify** `ApiAnalyticsMapper` — map `StatsResult` → `ApiAnalyticsStatsResponse`
- **Modify** `FakeAnalyticsQueryService` — add `StatsResult statsResult` field

### Tests

- **Create** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchStatsQueryAdapterTest.java`
- **Create** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchStatsResponseAdapterTest.java`

---

## BE-4: GROUP_BY Query Type

**Layer:** Backend | **Size:** L | **Dependencies:** BE-1, BE-2

> As an API publisher, I want to query `?type=GROUP_BY&field=status`
> so that I see request counts broken down by individual HTTP status code.

### Subtasks

#### BE-4.1 Create core domain model for group-by result

- **Create** `SERVICE/src/main/java/io/gravitee/apim/core/analytics/model/GroupByResult.java`
  - `Map<String, Long> values` + `Map<String, Map<String, String>> metadata`

#### BE-4.2 Add `searchGroupBy` to `AnalyticsQueryService`

- **Modify** `SERVICE/.../analytics/query_service/AnalyticsQueryService.java`
  - Add: `Optional<GroupByResult> searchGroupBy(ExecutionContext ctx, String apiId, Instant from, Instant to, String field, int size)`

#### BE-4.3 Implement `searchGroupBy` in `AnalyticsQueryServiceImpl`

- **Modify** `SERVICE/.../infra/query_service/analytics/AnalyticsQueryServiceImpl.java`

#### BE-4.4 Create `GroupByQuery` and `GroupByAggregate` repository models

- **Create** `REPO_API/.../log/v4/model/analytics/GroupByQuery.java`
  - Record: `String apiId`, `Instant from`, `Instant to`, `String field`, `int size`
- **Create** `REPO_API/.../log/v4/model/analytics/GroupByAggregate.java`
  - Record: `Map<String, Long> values`, `Map<String, Map<String, String>> metadata`

#### BE-4.5 Add `searchGroupBy` to V4 `AnalyticsRepository`

- **Modify** `REPO_API/.../log/v4/api/AnalyticsRepository.java`

#### BE-4.6 Create ES group-by query/response adapters

- **Create** `REPO_ES/.../v4/analytics/adapter/SearchGroupByQueryAdapter.java`
  - Build ES JSON: filter by `api-id` + `@timestamp`, `aggs.group_by_field.terms` on field with `size`
- **Create** `REPO_ES/.../v4/analytics/adapter/SearchGroupByResponseAdapter.java`
  - Parse ES terms aggregation buckets → `GroupByAggregate`
- **Pattern:** V1 Freemarker `groupBy.ftl`; existing `AggregateValueCountByFieldAdapter.java`

#### BE-4.7 Implement `searchGroupBy` in `AnalyticsElasticsearchRepository`

- **Modify** `REPO_ES/.../v4/analytics/AnalyticsElasticsearchRepository.java`

#### BE-4.8 Wire GROUP_BY into use case + mapper

- **Modify** `SearchApiAnalyticsUseCase` — add GROUP_BY branch
- **Modify** `ApiAnalyticsMapper` — map `GroupByResult` → `ApiAnalyticsGroupByResponse`
- **Modify** `FakeAnalyticsQueryService` — add `GroupByResult groupByResult` field

### Tests

- **Create** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchGroupByQueryAdapterTest.java`
- **Create** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchGroupByResponseAdapterTest.java`

---

## BE-5: DATE_HISTO Query Type

**Layer:** Backend | **Size:** L | **Dependencies:** BE-1, BE-2

> As an API publisher, I want to query
> `?type=DATE_HISTO&field=status&interval=3600000`
> so that I see time-bucketed histogram data.

### Subtasks

#### BE-5.1 Create core domain model for date-histo result

- **Create** `SERVICE/src/main/java/io/gravitee/apim/core/analytics/model/DateHistoResult.java`
  - `List<Long> timestamps`
  - `List<DateHistoBucket> values` where `DateHistoBucket` = `{ String field, List<Long> buckets, Map<String, String> metadata }`

#### BE-5.2 Add `searchDateHistogram` to `AnalyticsQueryService`

- **Modify** `SERVICE/.../analytics/query_service/AnalyticsQueryService.java`
  - Add: `Optional<DateHistoResult> searchDateHistogram(ExecutionContext ctx, String apiId, Instant from, Instant to, String field, Duration interval)`

#### BE-5.3 Implement `searchDateHistogram` in `AnalyticsQueryServiceImpl`

- **Modify** `SERVICE/.../infra/query_service/analytics/AnalyticsQueryServiceImpl.java`

#### BE-5.4 Create `DateHistogramQuery` and `DateHistoAggregate` repository models

- **Create** `REPO_API/.../log/v4/model/analytics/DateHistogramQuery.java`
  - Record: `String apiId`, `Instant from`, `Instant to`, `String field`, `Duration interval`
- **Create** `REPO_API/.../log/v4/model/analytics/DateHistoAggregate.java`
  - Record mirroring the result structure

#### BE-5.5 Add `searchDateHistogram` to V4 `AnalyticsRepository`

- **Modify** `REPO_API/.../log/v4/api/AnalyticsRepository.java`

#### BE-5.6 Create ES date-histo query/response adapters

- **Create** `REPO_ES/.../v4/analytics/adapter/SearchDateHistogramQueryAdapter.java`
  - Build ES JSON: filter by `api-id` + `@timestamp`, `aggs.by_date.date_histogram` with `fixed_interval`, sub-aggregation `terms` on `field`
  - Handle `extended_bounds` from `from`/`to`
  - Use `info` (ES version) for interval compatibility (as done in `ResponseTimeRangeQueryAdapter`)
- **Create** `REPO_ES/.../v4/analytics/adapter/SearchDateHistogramResponseAdapter.java`
  - Parse ES date_histogram buckets with nested terms buckets → `DateHistoAggregate`
- **Pattern:** V1 Freemarker `dateHistogram.ftl`; existing `SearchResponseStatusOverTimeAdapter.java`

#### BE-5.7 Implement `searchDateHistogram` in `AnalyticsElasticsearchRepository`

- **Modify** `REPO_ES/.../v4/analytics/AnalyticsElasticsearchRepository.java`
  - Inject `info` (ES version) for interval compatibility

#### BE-5.8 Wire DATE_HISTO into use case + mapper

- **Modify** `SearchApiAnalyticsUseCase` — add DATE_HISTO branch
- **Modify** `ApiAnalyticsMapper` — map `DateHistoResult` → `ApiAnalyticsDateHistoResponse`
- **Modify** `FakeAnalyticsQueryService` — add `DateHistoResult dateHistoResult` field

### Tests

- **Create** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchDateHistogramQueryAdapterTest.java`
- **Create** `REPO_ES/src/test/java/.../v4/analytics/adapter/SearchDateHistogramResponseAdapterTest.java`

---

## BE-6: Backend Integration Tests

**Layer:** Backend | **Size:** M | **Dependencies:** BE-2, BE-3, BE-4, BE-5

> As a developer, I want comprehensive REST integration tests
> so that all 4 query types are validated end-to-end.

### Subtasks

#### BE-6.1 Add `UnifiedAnalytics` nested test class

- **Modify** `REST_V2/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResourceTest.java`
  - Add `@Nested class UnifiedAnalytics` with `WebTarget analyticsTarget = rootTarget()`
- **Pattern:** Existing nested classes (`RequestsCountAnalytics`, `ResponseStatusRangesAnalytics`, etc.) in the same file

#### BE-6.2 COUNT tests

- `should_return_403_if_incorrect_permissions` (permission mock returns false)
- `should_return_400_if_type_missing`
- `should_return_400_if_from_missing`
- `should_return_count` (set `fakeAnalyticsQueryService.countResult = 12345L`, assert response `type=COUNT, count=12345`)
- `should_return_zero_count_when_no_data`

#### BE-6.3 STATS tests

- `should_return_400_if_field_missing_for_stats`
- `should_return_stats` (set `fakeAnalyticsQueryService.statsResult`, assert min/max/avg/sum)

#### BE-6.4 GROUP_BY tests

- `should_return_400_if_field_missing_for_group_by`
- `should_return_group_by` (set `fakeAnalyticsQueryService.groupByResult`, assert values map)

#### BE-6.5 DATE_HISTO tests

- `should_return_400_if_interval_missing_for_date_histo`
- `should_return_date_histo` (set `fakeAnalyticsQueryService.dateHistoResult`, assert timestamps + values)

---

## FE-1: Angular Service & Models for Unified Endpoint

**Layer:** Frontend | **Size:** M | **Dependencies:** BE-2 (API contract)

> As a frontend developer, I want a TypeScript service method and models
> so that components can call the unified endpoint.

### Subtasks

#### FE-1.1 Create unified response type interfaces

- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsCount.ts`
  ```typescript
  export interface AnalyticsCountResponse {
    type: 'COUNT';
    count: number;
  }
  ```
- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsStats.ts`
  ```typescript
  export interface AnalyticsStatsResponse {
    type: 'STATS';
    count: number;
    min: number;
    max: number;
    avg: number;
    sum: number;
  }
  ```
- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsGroupBy.ts`
  ```typescript
  export interface AnalyticsGroupByResponse {
    type: 'GROUP_BY';
    values: Record<string, number>;
    metadata: Record<string, { name: string }>;
  }
  ```
- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsDateHisto.ts`
  ```typescript
  export interface AnalyticsDateHistoResponse {
    type: 'DATE_HISTO';
    timestamp: number[];
    values: { field: string; buckets: number[]; metadata: { name: string } }[];
  }
  ```
- **Pattern:** `CONSOLE/src/entities/management-api-v2/analytics/analyticsRequestsCount.ts`

#### FE-1.2 Create fixture files

- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsCount.fixture.ts`
- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsStats.fixture.ts`
- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsGroupBy.fixture.ts`
- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsDateHisto.fixture.ts`
- **Pattern:** `CONSOLE/src/entities/management-api-v2/analytics/analyticsRequestsCount.fixture.ts`
  - Export `fakeAnalytics<Type>(modifier?: Partial<T>): T`

#### FE-1.3 Create unified query params type

- **Create** `CONSOLE/src/entities/management-api-v2/analytics/analyticsQueryParams.ts`
  ```typescript
  export type AnalyticsQueryType = 'COUNT' | 'STATS' | 'GROUP_BY' | 'DATE_HISTO';

  export interface AnalyticsQueryParams {
    type: AnalyticsQueryType;
    from: number;
    to: number;
    field?: string;
    interval?: number;
    size?: number;
    order?: string;
  }
  ```

#### FE-1.4 Add `getAnalytics` method to `ApiAnalyticsV2Service`

- **Modify** `CONSOLE/src/services-ngx/api-analytics-v2.service.ts`
  - Add imports for new types
  - Add method:
    ```typescript
    getAnalytics<T>(apiId: string, params: AnalyticsQueryParams): Observable<T> {
      return this.timeRangeFilter().pipe(
        filter(data => !!data),
        switchMap(({ from, to }) => {
          const queryParams = new HttpParams()
            .set('type', params.type)
            .set('from', params.from ?? from)
            .set('to', params.to ?? to);
          // conditionally append field, interval, size, order
          const url = `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics`;
          return this.http.get<T>(url, { params: queryParams });
        }),
      );
    }
    ```
- **Pattern:** Existing `getRequestsCount()` method in same file (lines 50-57)

#### FE-1.5 Service unit test

- **Modify** `CONSOLE/src/services-ngx/api-analytics-v2.service.spec.ts`
  - Add tests for `getAnalytics()` with each type
  - Verify correct query string construction
  - Use `HttpTestingController` pattern

---

## FE-2: Enhanced Stats Cards

**Layer:** Frontend | **Size:** M | **Dependencies:** FE-1

> As an API publisher, I want to see 4 stat cards (Total Requests, Avg Gateway
> Response Time, Avg Upstream Response Time, Avg Content Length) so that I get
> a quick overview of API performance.

### Subtasks

#### FE-2.1 Update proxy component to fetch 4 stats

- **Modify** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
  - Replace the 2-observable pattern (`getRequestsCount$` + `getAverageConnectionDuration$`) with 4 observables using the new `getAnalytics()`:
    1. `getAnalytics<AnalyticsCountResponse>(apiId, { type: 'COUNT' })` → Total Requests
    2. `getAnalytics<AnalyticsStatsResponse>(apiId, { type: 'STATS', field: 'gateway-response-time-ms' })` → Avg Gateway Response Time
    3. `getAnalytics<AnalyticsStatsResponse>(apiId, { type: 'STATS', field: 'endpoint-response-time-ms' })` → Avg Upstream Response Time
    4. `getAnalytics<AnalyticsStatsResponse>(apiId, { type: 'STATS', field: 'request-content-length' })` → Avg Content Length
  - Update `analyticsData$` `combineLatest` to include all 4
  - Update `requestStats` array to emit 4 `AnalyticsRequestStats` items
- **Pattern:** Existing `getRequestsCount$` and `getAverageConnectionDuration$` observables (lines 67-78)
- **Reference component:** `ApiAnalyticsRequestStatsComponent` (already supports N cards)

#### FE-2.2 No template change needed for stat cards

- `ApiAnalyticsRequestStatsComponent` already renders an array of `AnalyticsRequestStats` — it will automatically show 4 cards instead of 2

#### FE-2.3 Update proxy component spec

- **Modify** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.spec.ts`
  - Update HTTP expectations: replace `requests-count` and `average-connection-duration` calls with unified `/analytics?type=COUNT` and `/analytics?type=STATS&field=...` calls
  - Assert 4 stat cards are rendered
- **Pattern:** Existing spec file setup (lines 1-end)

---

## FE-3: HTTP Status Pie Chart

**Layer:** Frontend | **Size:** M | **Dependencies:** FE-1

> As an API publisher, I want a pie chart showing request counts by individual
> HTTP status code so that I can see the distribution of responses.

### Subtasks

#### FE-3.1 Create `ApiAnalyticsHttpStatusPieChartComponent`

- **Create** `CONSOLE/src/management/api/api-traffic-v4/analytics/components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.ts`
  - Standalone component
  - `selector: 'api-analytics-http-status-pie-chart'`
  - `@Input() apiId: string`
  - Injects `ApiAnalyticsV2Service`
  - Calls `getAnalytics<AnalyticsGroupByResponse>(apiId, { type: 'GROUP_BY', field: 'status', size: 20 })`
  - Maps `values` → `GioChartPieInput[]` using color logic from `ApiAnalyticsResponseStatusRangesComponent` (2xx green, 3xx blue, 4xx orange, 5xx red)
  - Imports: `MatCard`, `GioChartPieModule`, `GioLoaderModule`
- **Pattern:** `CONSOLE/src/shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component.ts` for color/label mapping + pie chart wiring

#### FE-3.2 Create component template

- **Create** `CONSOLE/src/management/api/api-traffic-v4/analytics/components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.html`
  - `<mat-card>` with title "HTTP Status Codes"
  - Loader while fetching
  - `<gio-chart-pie>` with mapped input
  - Empty state when no data

#### FE-3.3 Create component styles

- **Create** `CONSOLE/src/management/api/api-traffic-v4/analytics/components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.scss`
- **Pattern:** `CONSOLE/src/shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component.scss`

#### FE-3.4 Create component harness (for testing)

- **Create** `CONSOLE/src/management/api/api-traffic-v4/analytics/components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.harness.ts`
- **Pattern:** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.harness.ts`

#### FE-3.5 Create component spec

- **Create** `CONSOLE/src/management/api/api-traffic-v4/analytics/components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.spec.ts`
  - Test data mapping (GROUP_BY values → pie slices)
  - Test color assignment by status code prefix
  - Test loading state
  - Test empty state
- **Pattern:** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.spec.ts`

---

## FE-4: Dashboard Layout & Integration

**Layer:** Frontend | **Size:** S | **Dependencies:** FE-2, FE-3

> As an API publisher, I want the dashboard to show all widgets in a clean layout
> and refresh together when I change the timeframe.

### Subtasks

#### FE-4.1 Update proxy component imports

- **Modify** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
  - Add `ApiAnalyticsHttpStatusPieChartComponent` to `imports` array

#### FE-4.2 Update proxy template layout

- **Modify** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.html`
  - New layout:
    ```
    Row 1: <api-analytics-filters-bar>
    Row 2: <app-api-analytics-request-stats> (full width, now 4 cards)
    Row 3: <api-analytics-http-status-pie-chart> (left) + <api-analytics-response-status-overtime> (right)
    Row 4: <api-analytics-response-time-over-time> (full width)
    ```
  - The request stats card gets `class="full-bleed"` to span full width
  - Pie chart and overtime chart are in the 2-column grid

#### FE-4.3 Update proxy styles if needed

- **Modify** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.scss`
  - Grid should already work (2-column, `.full-bleed` spans both)
  - May need minor adjustments for the new 4-row layout

#### FE-4.4 Ensure existing charts still work

- Existing `ApiAnalyticsResponseStatusOvertimeComponent` and `ApiAnalyticsResponseTimeOverTimeComponent` call their own endpoints independently (`getResponseStatusOvertime()`, `getResponseTimeOverTime()`)
- They react to `timeRangeFilter()` changes automatically — no changes needed
- **Verify** in spec that these components still render

---

## FE-5: Frontend Component Tests

**Layer:** Frontend | **Size:** M | **Dependencies:** FE-2, FE-3, FE-4

> As a developer, I want Angular component tests so that the dashboard
> and new widgets are validated.

### Subtasks

#### FE-5.1 Update proxy component spec for new layout

- **Modify** `CONSOLE/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.spec.ts`
  - Assert 4 stat cards rendered
  - Assert pie chart component present
  - Assert both line chart components present
  - Test timeframe change triggers refetch of all unified endpoint calls
  - Test empty state (analytics disabled)
  - Test error handling (service unavailable → graceful degradation)
- **Pattern:** Existing spec with `HttpTestingController` expectations

#### FE-5.2 HTTP status pie chart spec (already created in FE-3.5)

- Covered in FE-3.5

#### FE-5.3 Service spec updates (already created in FE-1.5)

- Covered in FE-1.5

---

## Dependency Graph

```
BE-1 ──┬──> BE-2 ──┬──> BE-6
       │           │
       ├──> BE-3 ──┤
       │           │
       ├──> BE-4 ──┤
       │           │
       └──> BE-5 ──┘
                   │
                   v
                 FE-1 ──┬──> FE-2 ──┬──> FE-4 ──> FE-5
                        │           │
                        └──> FE-3 ──┘
```

## Implementation Order (recommended)

| Phase | Stories | Parallelism |
|---|---|---|
| 1 | BE-1 | Sequential |
| 2 | BE-2, BE-3, BE-4, BE-5 | Parallel (share scaffolding from BE-2) |
| 3 | BE-6 | Sequential |
| 4 | FE-1 | Sequential |
| 5 | FE-2, FE-3 | Parallel |
| 6 | FE-4, FE-5 | Sequential |

## File Change Summary

### New files

| File | Story |
|---|---|
| `REST_V2/.../resource/param/AnalyticsType.java` | BE-1.1 |
| `REST_V2/.../resource/param/AnalyticsParam.java` | BE-1.2 |
| `REST_V2/.../resource/param/AnalyticsFieldParam.java` | BE-1.3 |
| `SERVICE/.../core/analytics/use_case/SearchApiAnalyticsUseCase.java` | BE-2.2 |
| `SERVICE/.../core/analytics/model/StatsResult.java` | BE-3.1 |
| `SERVICE/.../core/analytics/model/GroupByResult.java` | BE-4.1 |
| `SERVICE/.../core/analytics/model/DateHistoResult.java` | BE-5.1 |
| `REPO_API/.../log/v4/model/analytics/CountQuery.java` | BE-2.6 |
| `REPO_API/.../log/v4/model/analytics/StatsQuery.java` | BE-3.4 |
| `REPO_API/.../log/v4/model/analytics/StatsAggregate.java` | BE-3.4 |
| `REPO_API/.../log/v4/model/analytics/GroupByQuery.java` | BE-4.4 |
| `REPO_API/.../log/v4/model/analytics/GroupByAggregate.java` | BE-4.4 |
| `REPO_API/.../log/v4/model/analytics/DateHistogramQuery.java` | BE-5.4 |
| `REPO_API/.../log/v4/model/analytics/DateHistoAggregate.java` | BE-5.4 |
| `REPO_ES/.../v4/analytics/adapter/SearchCountQueryAdapter.java` | BE-2.7 |
| `REPO_ES/.../v4/analytics/adapter/SearchCountResponseAdapter.java` | BE-2.8 |
| `REPO_ES/.../v4/analytics/adapter/SearchStatsQueryAdapter.java` | BE-3.6 |
| `REPO_ES/.../v4/analytics/adapter/SearchStatsResponseAdapter.java` | BE-3.6 |
| `REPO_ES/.../v4/analytics/adapter/SearchGroupByQueryAdapter.java` | BE-4.6 |
| `REPO_ES/.../v4/analytics/adapter/SearchGroupByResponseAdapter.java` | BE-4.6 |
| `REPO_ES/.../v4/analytics/adapter/SearchDateHistogramQueryAdapter.java` | BE-5.6 |
| `REPO_ES/.../v4/analytics/adapter/SearchDateHistogramResponseAdapter.java` | BE-5.6 |
| `REPO_ES/src/test/java/.../adapter/SearchCountQueryAdapterTest.java` | BE-2 |
| `REPO_ES/src/test/java/.../adapter/SearchCountResponseAdapterTest.java` | BE-2 |
| `REPO_ES/src/test/java/.../adapter/SearchStatsQueryAdapterTest.java` | BE-3 |
| `REPO_ES/src/test/java/.../adapter/SearchStatsResponseAdapterTest.java` | BE-3 |
| `REPO_ES/src/test/java/.../adapter/SearchGroupByQueryAdapterTest.java` | BE-4 |
| `REPO_ES/src/test/java/.../adapter/SearchGroupByResponseAdapterTest.java` | BE-4 |
| `REPO_ES/src/test/java/.../adapter/SearchDateHistogramQueryAdapterTest.java` | BE-5 |
| `REPO_ES/src/test/java/.../adapter/SearchDateHistogramResponseAdapterTest.java` | BE-5 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsCount.ts` | FE-1.1 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsStats.ts` | FE-1.1 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsGroupBy.ts` | FE-1.1 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsDateHisto.ts` | FE-1.1 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsQueryParams.ts` | FE-1.3 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsCount.fixture.ts` | FE-1.2 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsStats.fixture.ts` | FE-1.2 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsGroupBy.fixture.ts` | FE-1.2 |
| `CONSOLE/src/entities/management-api-v2/analytics/analyticsDateHisto.fixture.ts` | FE-1.2 |
| `CONSOLE/.../components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.ts` | FE-3.1 |
| `CONSOLE/.../components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.html` | FE-3.2 |
| `CONSOLE/.../components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.scss` | FE-3.3 |
| `CONSOLE/.../components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.harness.ts` | FE-3.4 |
| `CONSOLE/.../components/api-analytics-http-status-pie-chart/api-analytics-http-status-pie-chart.component.spec.ts` | FE-3.5 |

### Modified files

| File | Story |
|---|---|
| `REST_V2/.../openapi/openapi-apis.yaml` | BE-2.1 |
| `REST_V2/.../resource/api/analytics/ApiAnalyticsResource.java` | BE-2.10 |
| `REST_V2/.../mapper/ApiAnalyticsMapper.java` | BE-2.11, BE-3.8, BE-4.8, BE-5.8 |
| `SERVICE/.../analytics/query_service/AnalyticsQueryService.java` | BE-2.3, BE-3.2, BE-4.2, BE-5.2 |
| `SERVICE/.../infra/query_service/analytics/AnalyticsQueryServiceImpl.java` | BE-2.4, BE-3.3, BE-4.3, BE-5.3 |
| `SERVICE/src/test/java/fakes/FakeAnalyticsQueryService.java` | BE-2.12, BE-3.8, BE-4.8, BE-5.8 |
| `REPO_API/.../log/v4/api/AnalyticsRepository.java` | BE-2.5, BE-3.5, BE-4.5, BE-5.5 |
| `REPO_ES/.../v4/analytics/AnalyticsElasticsearchRepository.java` | BE-2.9, BE-3.7, BE-4.7, BE-5.7 |
| `REST_V2/src/test/java/.../ApiAnalyticsResourceTest.java` | BE-6 |
| `CONSOLE/src/services-ngx/api-analytics-v2.service.ts` | FE-1.4 |
| `CONSOLE/src/services-ngx/api-analytics-v2.service.spec.ts` | FE-1.5 |
| `CONSOLE/.../api-analytics-proxy/api-analytics-proxy.component.ts` | FE-2.1, FE-4.1 |
| `CONSOLE/.../api-analytics-proxy/api-analytics-proxy.component.html` | FE-4.2 |
| `CONSOLE/.../api-analytics-proxy/api-analytics-proxy.component.scss` | FE-4.3 |
| `CONSOLE/.../api-analytics-proxy/api-analytics-proxy.component.spec.ts` | FE-2.3, FE-5.1 |
