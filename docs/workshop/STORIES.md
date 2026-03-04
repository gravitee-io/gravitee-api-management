# V4 API Analytics Dashboard (M1) - Story Breakdown (Refined)

This breakdown follows monorepo conventions already used in Gravitee APIM:
- Backend REST resources: `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/...`
- Backend use-cases/query services: `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/...`
- Repository API models: `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics`
- Elasticsearch adapters: `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter`
- Backend tests mirror package names under each module’s `src/test/java`
- Frontend analytics entities: `gravitee-apim-console-webui/src/entities/management-api-v2/analytics` using `analytics*.ts` and `analytics*.fixture.ts`
- Frontend V4 analytics UI: `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics`

## 1. Story BE-1a - Unified Endpoint Contract and Validation
Layer: Backend  
Complexity: M  
Dependencies: None

### 1.1 Add unified `GET /v2/apis/{apiId}/analytics` endpoint signature
Files: modify `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`; create `.../resource/api/analytics/param/SearchApiAnalyticsParam.java`.
Tests: extend `.../src/test/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResourceTest.java` with happy-path request parsing for each type.
Pattern to reference: `EnvironmentAnalyticsResource.java`, `SearchLogsParam.java`, `TimeRangeParam.java`.
Documentation: update path/params in `.../src/main/resources/openapi/openapi-apis.yaml`.

### 1.2 Enforce parameter compatibility matrix and invalid-range rules
Files: modify `SearchApiAnalyticsParam.java`; optionally create `.../param/validation/SearchApiAnalyticsParamValidator.java` + annotation class in same package subtree.
Tests: in `ApiAnalyticsResourceTest.java` add `400` cases for missing required params by type, `from > to`, missing bounds, and too-large range policy (if defined).
Pattern to reference: existing validation patterns under `.../resource/environment/param/validation`.
Documentation: add explicit request validation rules in `openapi-apis.yaml` endpoint description.

### 1.3 Define empty-data response semantics by query type
Files: modify unified response mapping in `ApiAnalyticsResource.java` and mapper class (`ApiAnalyticsMapper.java` or new mapper dedicated to unified responses).
Tests: in `ApiAnalyticsResourceTest.java` add empty-data assertions for COUNT/STATS/GROUP_BY/DATE_HISTO contracts.
Pattern to reference: current default-empty behavior in `SearchRequestsCountAnalyticsUseCase` and `getResponseStatusOvertime()`.
Documentation: add per-type empty payload examples to `openapi-apis.yaml`.

## 2. Story BE-1b - Authorization Semantics
Layer: Backend  
Complexity: S  
Dependencies: 1

### 2.1 Enforce `API_ANALYTICS:READ` on unified endpoint
Files: modify `ApiAnalyticsResource.java` annotations for new endpoint.
Tests: in `ApiAnalyticsResourceTest.java`, add forbidden-access scenario for `GET /analytics?type=COUNT`.
Pattern to reference: existing `@Permissions` usage in split analytics endpoints.
Documentation: endpoint auth requirement in `openapi-apis.yaml`.

### 2.2 Clarify and lock `401` vs `403` behavior
Files: modify only if necessary in resource/security integration points; primary touchpoint remains `ApiAnalyticsResourceTest.java`.
Tests: add explicit tests covering unauthenticated vs insufficient-permission semantics where test framework supports both paths.
Pattern to reference: existing security assertions in management-v2 resource tests.
Documentation: update auth error response section in `openapi-apis.yaml` if behavior is explicitly documented.

## 3. Story BE-1c - Consistent V4 API Scoping Across All Query Types
Layer: Backend  
Complexity: M  
Dependencies: 1

### 3.1 Centralize V4/proxy/environment eligibility checks for unified analytics
Files: create `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/use_case/SearchApiAnalyticsUseCase.java` (or equivalent orchestrator); reuse validation logic patterns from existing analytics use-cases.
Tests: create `.../src/test/java/io/gravitee/apim/core/analytics/use_case/SearchApiAnalyticsUseCaseTest.java` for invalid definition version, cross-environment access, and TCP proxy rejection.
Pattern to reference: `SearchRequestsCountAnalyticsUseCase.java`, `SearchResponseStatusOverTimeUseCase.java`, `SearchResponseTimeUseCase.java`.
Documentation: endpoint description in `openapi-apis.yaml` should state V4 scope and TCP limitation consistently.

### 3.2 Ensure all query-type branches use the same scope guard
Files: modify `SearchApiAnalyticsUseCase.java` execute branches (COUNT/STATS/GROUP_BY/DATE_HISTO).
Tests: add one test per query type asserting same scope validation path and errors.
Pattern to reference: common validation helpers currently duplicated in multiple use-cases.
Documentation: none beyond OpenAPI endpoint notes.

## 4. Story BE-2 - COUNT Query Type
Layer: Backend  
Complexity: S  
Dependencies: 1, 3

### 4.1 Implement COUNT in unified use-case + query service path
Files: modify `SearchApiAnalyticsUseCase.java`; if needed, extend `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java` (reuse existing `searchRequestsCount` preferred).
Tests: add COUNT success and no-data tests in `SearchApiAnalyticsUseCaseTest.java`.
Pattern to reference: `SearchRequestsCountAnalyticsUseCase.java`.
Documentation: add COUNT response schema and example in `openapi-apis.yaml`.

### 4.2 Map COUNT domain result to unified response model
Files: modify mapper (`ApiAnalyticsMapper.java` or dedicated unified mapper), and `ApiAnalyticsResource.java` return mapping.
Tests: `ApiAnalyticsResourceTest.java` response body assertions for `{ type, count }`.
Pattern to reference: existing `ApiAnalyticsMapper.map(RequestsCount)`.
Documentation: OpenAPI response component entries for COUNT.

## 5. Story BE-3 - STATS Query Type
Layer: Backend  
Complexity: M  
Dependencies: 1, 3

### 5.1 Add STATS repository contracts
Files: create `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/StatsQueryCriteria.java`; create `.../StatsAggregate.java`; modify `.../api/AnalyticsRepository.java`.
Tests: compile + integration coverage via elastic repository tests.
Pattern to reference: `RequestResponseTimeQueryCriteria.java`, `AverageAggregate.java`.
Documentation: OpenAPI STATS schema fields (`count,min,max,avg,sum`).

### 5.2 Implement Elasticsearch STATS adapter and repository method
Files: create `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchStatsAdapter.java`; modify `.../AnalyticsElasticsearchRepository.java`.
Tests: create `.../src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchStatsAdapterTest.java`; extend `.../AnalyticsElasticsearchRepositoryTest.java`.
Pattern to reference: `SearchRequestResponseTimeAdapter.java` and `ResponseTimeRangeQueryAdapter.java` script-aggregation style.
Documentation: document supported STATS `field` values in `openapi-apis.yaml`.

### 5.3 Wire STATS through query service and unified use-case
Files: modify `AnalyticsQueryService.java`; modify `AnalyticsQueryServiceImpl.java`; modify `SearchApiAnalyticsUseCase.java`.
Tests: extend `SearchApiAnalyticsUseCaseTest.java` with STATS valid/invalid-field cases.
Pattern to reference: `AnalyticsQueryServiceImpl.searchRequestResponseTime(...)`.
Documentation: STATS request examples in `openapi-apis.yaml`.

## 6. Story BE-4 - GROUP_BY Query Type
Layer: Backend  
Complexity: M  
Dependencies: 1, 3

### 6.1 Add GROUP_BY repository contracts
Files: create `.../repository-api/.../model/analytics/GroupByQueryCriteria.java`; create `.../GroupByAggregate.java`; modify `AnalyticsRepository.java`.
Tests: integration coverage in elastic repository tests and use-case tests.
Pattern to reference: `TopHitsQueryCriteria.java`, `TopHitsAggregate.java`.
Documentation: OpenAPI GROUP_BY schema (`values`, `metadata`, `size`, `order`).

### 6.2 Implement Elasticsearch GROUP_BY adapter with deterministic ordering
Files: create `.../repository-elasticsearch/.../adapter/SearchGroupByAdapter.java`; modify `AnalyticsElasticsearchRepository.java`.
Tests: create `SearchGroupByAdapterTest.java` for top-N and tie-breaking stability; extend `AnalyticsElasticsearchRepositoryTest.java` for ordering and empty results.
Pattern to reference: `AggregateValueCountByFieldAdapter.java`.
Documentation: document deterministic tie-breaker in `openapi-apis.yaml` (for stable UI rendering).

### 6.3 Wire GROUP_BY into unified flow
Files: modify `AnalyticsQueryService.java`; modify `AnalyticsQueryServiceImpl.java`; modify `SearchApiAnalyticsUseCase.java`; modify `ApiAnalyticsResource.java` mapping.
Tests: add GROUP_BY resource/use-case tests.
Pattern to reference: top hits mapping in `SearchEnvironmentTopAppsByRequestCountUseCase.java`.
Documentation: include status pie example payload in `openapi-apis.yaml`.

## 7. Story BE-5 - DATE_HISTO Query Type
Layer: Backend  
Complexity: L  
Dependencies: 1, 3

### 7.1 Add DATE_HISTO repository contracts
Files: create `.../repository-api/.../model/analytics/DateHistogramQuery.java`; create `.../DateHistogramAggregate.java`; modify `AnalyticsRepository.java`.
Tests: use-case/resource tests for `interval` requirement and invalid combinations.
Pattern to reference: `ResponseStatusOverTimeQuery.java`, `ResponseStatusOverTimeAggregate.java`.
Documentation: OpenAPI DATE_HISTO schema (`timestamp`, `values[].buckets`, metadata).

### 7.2 Implement ES DATE_HISTO adapter with timezone/gap bucket policy
Files: create `.../repository-elasticsearch/.../adapter/SearchDateHistogramAdapter.java`; modify `AnalyticsElasticsearchRepository.java`.
Tests: create `SearchDateHistogramAdapterTest.java` covering zero-filled empty buckets, DST/timezone behavior, and interval handling; extend `AnalyticsElasticsearchRepositoryTest.java`.
Pattern to reference: `SearchResponseStatusOverTimeAdapter.java`, `ResponseTimeRangeQueryAdapter.java`.
Documentation: explicitly document timezone and zero-fill policy in `openapi-apis.yaml`.

### 7.3 Wire DATE_HISTO to unified endpoint
Files: modify `AnalyticsQueryService.java`; modify `AnalyticsQueryServiceImpl.java`; modify `SearchApiAnalyticsUseCase.java`; modify `ApiAnalyticsResource.java` mapping.
Tests: DATE_HISTO resource/use-case tests for standard and empty responses.
Pattern to reference: existing response-status-over-time and response-time-over-time endpoint mappings.
Documentation: DATE_HISTO examples in `openapi-apis.yaml`.

## 8. Story BE-T1 - Query-Type Backend Tests (Feature-Aligned)
Layer: Backend  
Complexity: M  
Dependencies: 4, 5, 6, 7

### 8.1 Add use-case unit tests for all four query types
Files: modify/create `.../SearchApiAnalyticsUseCaseTest.java`; modify `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/fakes/FakeAnalyticsQueryService.java` for new methods.
Tests: COUNT/STATS/GROUP_BY/DATE_HISTO success and empty-data paths.
Pattern to reference: `SearchResponseStatusOverTimeUseCaseTest.java`, `SearchRequestsCountAnalyticsUseCaseTest.java`.
Documentation: none.

### 8.2 Add repository adapter tests by query type
Files: create `SearchStatsAdapterTest.java`, `SearchGroupByAdapterTest.java`, `SearchDateHistogramAdapterTest.java`; extend `AnalyticsElasticsearchRepositoryTest.java`.
Tests: query JSON and response adaptation tests, including edge cases.
Pattern to reference: `AggregateValueCountByFieldAdapterTest.java`, `SearchResponseStatusOverTimeAdapterTest.java`.
Documentation: none.

## 9. Story BE-T2 - Permission, Validation, and Contract Hardening Tests
Layer: Backend  
Complexity: M  
Dependencies: 1, 2, 3, 4, 5, 6, 7

### 9.1 Add resource-level auth and validation matrix tests
Files: modify `ApiAnalyticsResourceTest.java`.
Tests: 401/403 semantics, required params by type, invalid ranges, unsupported field/type combinations.
Pattern to reference: existing nested style in `ApiAnalyticsResourceTest.java`.
Documentation: update OpenAPI error sections/examples if needed.

### 9.2 Add empty-payload contract regression tests
Files: modify `ApiAnalyticsResourceTest.java` and, where needed, use-case tests.
Tests: assert exact empty contract per type to protect FE empty-state logic.
Pattern to reference: existing no-data assertions in analytics tests.
Documentation: keep payload examples in OpenAPI synchronized.

## 10. Story FE-1 - Unified Angular Analytics Service (Contract-First)
Layer: Frontend  
Complexity: M  
Dependencies: 1

### 10.1 Add TypeScript models for unified analytics responses
Files: create `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/analyticsQueryType.ts`; create `analyticsCount.ts`, `analyticsStats.ts`, `analyticsGroupBy.ts`, `analyticsDateHisto.ts`; create matching `*.fixture.ts` files.
Tests: fixtures used in service/component specs.
Pattern to reference: existing analytics entity + fixture naming in same folder.
Documentation: inline comments in model files where field semantics are not obvious.

### 10.2 Implement unified endpoint calls in `ApiAnalyticsV2Service`
Files: modify `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts`.
Tests: modify `.../api-analytics-v2.service.spec.ts` to verify query params for all four types.
Pattern to reference: existing `getRequestsCount` and filter stream usage.
Documentation: keep temporary coexistence with old split methods documented in code comments only; no deprecation work in this epic.

### 10.3 Make service resilient to rapid timeframe changes
Files: modify `api-analytics-v2.service.ts` and/or consuming orchestration to guarantee latest-request-wins behavior.
Tests: service/component test showing stale responses are ignored/cancelled.
Pattern to reference: RxJS `switchMap` behavior currently used in analytics components.
Documentation: none.

## 11. Story FE-2 - Dashboard Data-State Orchestration (Refresh/Loading/Error/Empty)
Layer: Frontend  
Complexity: M  
Dependencies: 10

### 11.1 Build a single VM orchestration flow for the proxy dashboard
Files: modify `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`.
Tests: update `api-analytics-proxy.component.spec.ts` to verify one refresh triggers all relevant widget fetches.
Pattern to reference: current VM composition with `combineLatest` in proxy/message components.
Documentation: none.

### 11.2 Implement partial-failure behavior per widget
Files: modify `api-analytics-proxy.component.ts`; modify any widget components receiving VM state.
Tests: component tests for one failed query while other widgets still render data.
Pattern to reference: per-widget `catchError` already present in current proxy component.
Documentation: add internal note in story/PR that dashboard degrades per-widget, not all-or-nothing.

### 11.3 Implement explicit empty and error dashboard states
Files: modify `api-analytics-proxy.component.html` and optional styling in `api-analytics-proxy.component.scss`.
Tests: component tests covering no-data empty state and backend-failure error state.
Pattern to reference: `gio-card-empty-state` usage in current proxy/message components.
Documentation: none.

### 11.4 Scope timeframe presets for this dashboard to PRD ranges
Files: modify `.../components/api-analytics-filters-bar/api-analytics-filters-bar.component.ts`; modify `.html`; modify `api-analytics-filters-bar.configuration.ts`; modify harness if selectors/controls change.
Tests: add/create filters bar spec for `5m`, `1h`, `24h`, `7d`, `30d` and refresh behavior.
Pattern to reference: existing filters bar and `dashboard-filters-bar` conventions.
Documentation: note this page-specific preset scope in component comments.

## 12. Story FE-3 - Enhanced Stats Cards
Layer: Frontend  
Complexity: M  
Dependencies: 10, 11, 4, 5

### 12.1 Bind KPI cards to COUNT + STATS responses
Files: modify `api-analytics-proxy.component.ts`.
Tests: update `api-analytics-proxy.component.spec.ts` expected card values for total requests, avg gateway response time, avg upstream response time, avg content length.
Pattern to reference: `ApiAnalyticsRequestStatsComponent` contract.
Documentation: none.

### 12.2 Update proxy layout for the new card row
Files: modify `api-analytics-proxy.component.html` and optionally `.scss`.
Tests: harness/spec assertions for card order and labels.
Pattern to reference: current request stats card layout.
Documentation: none.

## 13. Story FE-4 - HTTP Status Pie Chart Widget
Layer: Frontend  
Complexity: M  
Dependencies: 10, 11, 6

### 13.1 Create dedicated status pie component for exact status codes
Files: create `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-status-pie/api-analytics-status-pie.component.ts`; create matching `.html`, `.scss`, `.harness.ts`, `.spec.ts`.
Tests: new widget unit tests for loading, empty, and data rendering.
Pattern to reference: `src/shared/components/api-analytics-response-status-ranges` and `gio-chart-pie` component usage.
Documentation: none.

### 13.2 Integrate status pie into proxy dashboard
Files: modify `api-analytics-proxy.component.ts`; modify `api-analytics-proxy.component.html`; modify `api-analytics-proxy.component.harness.ts`.
Tests: proxy component tests for GROUP_BY(status) integration.
Pattern to reference: existing response-status widget integration style.
Documentation: none.

## 14. Story FE-5a - Response Status Over Time Rewire
Layer: Frontend  
Complexity: M  
Dependencies: 10, 11, 7

### 14.1 Rewire status-over-time line chart to DATE_HISTO
Files: modify `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-response-status-overtime/api-analytics-response-status-overtime.component.ts`.
Tests: create `api-analytics-response-status-overtime.component.spec.ts` for DATE_HISTO mapping to `gio-chart-line` series.
Pattern to reference: current component mapping + `GioChartLineData`.
Documentation: none.

### 14.2 Keep chart options and bucket alignment stable
Files: modify same component to preserve `pointStart`/`pointInterval` semantics.
Tests: assert options in widget test to prevent bucket drift regressions.
Pattern to reference: existing `chartOptions` handling in component.
Documentation: none.

## 15. Story FE-5b - Existing Line Chart Regression Compatibility
Layer: Frontend  
Complexity: S  
Dependencies: 14

### 15.1 Define line chart inventory and expected behavior baseline
Files: modify `api-analytics-proxy.component.spec.ts` to explicitly cover both existing line charts:
`api-analytics-response-status-overtime` and `api-analytics-response-time-over-time`.
Tests: baseline assertions for both widgets under normal/refresh flows.
Pattern to reference: current proxy spec helper methods for both endpoints.
Documentation: add short “line chart compatibility checklist” section in this file and PR description.

### 15.2 Add smoke regression tests for response-time-over-time chart
Files: create/modify `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-response-time-over-time/api-analytics-response-time-over-time.component.spec.ts`.
Tests: ensure chart still renders and handles refresh/error after service updates.
Pattern to reference: status-overtime widget test style.
Documentation: none.

## 16. Story FE-T2 - Frontend Test Hardening
Layer: Frontend  
Complexity: M  
Dependencies: 12, 13, 14, 15

### 16.1 Complete integration-style dashboard tests
Files: modify `api-analytics-proxy.component.spec.ts`; update harnesses (`api-analytics-proxy.component.harness.ts`, filters bar harness, status pie harness).
Tests: end-to-end component matrix for refresh, partial failure, empty state, and stale request handling.
Pattern to reference: current harness-based tests for proxy/message analytics.
Documentation: none.

### 16.2 Service contract tests for all query types
Files: modify `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.spec.ts`.
Tests: verify all unified query shapes and query param encoding.
Pattern to reference: existing service spec style.
Documentation: none.

---

## Dependency Summary
1. `BE-1a` starts first.
2. `BE-1b` and `BE-1c` depend on `BE-1a`.
3. Query implementations `BE-2`, `BE-3`, `BE-4`, `BE-5` depend on `BE-1a` + `BE-1c`.
4. `BE-T1` depends on query implementation stories (`BE-2`..`BE-5`).
5. `BE-T2` depends on `BE-1a`, `BE-1b`, `BE-1c`, and all query stories.
6. `FE-1` depends only on `BE-1a` (contract-first, backend query internals can be mocked).
7. `FE-2` depends on `FE-1`.
8. `FE-3` depends on `FE-1`, `FE-2`, `BE-2`, `BE-3`.
9. `FE-4` depends on `FE-1`, `FE-2`, `BE-4`.
10. `FE-5a` depends on `FE-1`, `FE-2`, `BE-5`.
11. `FE-5b` depends on `FE-5a`.
12. `FE-T2` depends on `FE-3`, `FE-4`, `FE-5a`, `FE-5b`.

---

## Refinement Notes

### Decision Log (Review Suggestions)
1. AC9 only partially covered: **Accepted**. Added `Story FE-5b` with explicit line chart inventory and regression scope.
2. V4 scope inconsistent across query types: **Accepted**. Added `Story BE-1c` to enforce shared V4 eligibility for COUNT/STATS/GROUP_BY/DATE_HISTO.
3. Empty-data contract not explicit by query type: **Accepted**. Added `Story BE-1a` subtask `1.3` and hardening tests in `BE-T2`.
4. BE-1 too broad: **Accepted**. Split into `BE-1a` (contract/validation) and `BE-1b` (authorization), plus `BE-1c` for cross-type scoping.
5. Split recommendation BE-1a contract/validation: **Accepted**.
6. Split recommendation BE-1b authorization: **Accepted**.
7. BE-6 too broad: **Accepted**. Replaced with `BE-T1` and `BE-T2`.
8. Split recommendation BE-T1 query type tests: **Accepted**.
9. Split recommendation BE-T2 permission/validation/empty tests: **Accepted**.
10. FE-6 too broad: **Accepted**. Split behavior and test hardening responsibilities.
11. Split recommendation FE-6a behavior: **Accepted as merge**. Merged into `FE-2` (data-state orchestration).
12. Split recommendation FE-6b Angular tests: **Accepted** as `FE-T2`.
13. FE-2 and FE-6a overlap: **Accepted**. Merged into `FE-2` to avoid duplicate orchestration logic.
14. BE-2 may be too granular (optional COUNT+STATS merge): **Rejected**. Kept separate stories for clearer AC traceability (`AC1` vs `AC2`) and lower review risk.
15. Frontend dependencies over-blocked: **Accepted**. `FE-1` now depends only on `BE-1a` contract.
16. Widget dependencies too broad: **Accepted**. Dependencies are now per query type (`FE-3` -> COUNT/STATS, `FE-4` -> GROUP_BY, `FE-5a` -> DATE_HISTO).
17. Tests too end-loaded: **Accepted**. Added test subtasks directly to each story and kept `BE-T1/BE-T2/FE-T2` for final hardening.
18. GROUP_BY field scope too broad for current dashboard: **Rejected**. Kept broader scope because PRD backend contract lists multiple supported fields beyond status.
19. FE-1 includes deprecating old split calls unnecessarily: **Accepted**. Removed deprecation scope from this epic; keep coexistence only.
20. OpenAPI docs non-critical suggestion: **Rejected**. In this repo OpenAPI is a generated-model contract source; keeping it updated is required for implementation consistency.
21. Invalid time-range edge cases: **Accepted**. Added explicit validation/test subtasks in `BE-1a` and `BE-T2`.
22. DATE_HISTO DST/timezone/gap behavior: **Accepted**. Added explicit policy and tests in `BE-5`.
23. GROUP_BY deterministic ordering ties: **Accepted**. Added deterministic tie-breaker subtask/test in `BE-4`.
24. Partial widget failure behavior: **Accepted**. Added per-widget degradation handling in `FE-2`.
25. Rapid timeframe changes / stale responses: **Accepted**. Added latest-request-wins subtask in `FE-1` and test coverage in `FE-T2`.
26. Permission response semantics 401 vs 403: **Accepted**. Added explicit auth semantics subtask/testing in `BE-1b` and `BE-T2`.

### Structural Changes Applied
- Replaced original 12 stories with refined sequence: `BE-1a`, `BE-1b`, `BE-1c`, `BE-2`, `BE-3`, `BE-4`, `BE-5`, `BE-T1`, `BE-T2`, `FE-1`, `FE-2`, `FE-3`, `FE-4`, `FE-5a`, `FE-5b`, `FE-T2`.
- Tightened dependencies to allow frontend contract-first work.
- Added explicit edge-case and compatibility requirements that were previously implicit.
