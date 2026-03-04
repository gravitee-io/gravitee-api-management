# Workshop Progress Log

## TODO

- ✅ BE-1 — Query Parameter Model & Validation
- ✅ BE-2 — COUNT Query Type + Endpoint Scaffolding
- ✅ BE-3 — STATS Query Type
- ✅ BE-4 — GROUP_BY Query Type
- ✅ BE-5 — DATE_HISTO Query Type
- ✅ BE-6 — Backend Integration Tests
- FE-1 — Angular Service & Models for Unified Endpoint
- FE-2 — Enhanced Stats Cards
- FE-3 — HTTP Status Pie Chart
- FE-4 — Dashboard Layout, Integration & Tests

---

## Stories Completed

### BE-1: Query Parameter Model & Validation ✅

Created the V2 query parameter model for the unified analytics endpoint. Three new source files + one test file:

| File | Purpose |
|---|---|
| `REST_V2/.../resource/param/AnalyticsType.java` | Enum: `COUNT`, `STATS`, `GROUP_BY`, `DATE_HISTO` |
| `REST_V2/.../resource/param/AnalyticsFieldParam.java` | Enum of 10 supported field names with `isSupported(String)` lookup |
| `REST_V2/.../resource/param/AnalyticsParam.java` | JAX-RS bean param with `validate()` enforcing all rules |
| `REST_V2/.../resource/param/AnalyticsParamTest.java` | 18 unit tests covering every validation branch |

Validation rules implemented:
- `type`, `from`, `to` required
- `from < to`
- `field` validated against `AnalyticsFieldParam` allowlist
- `field` required for `STATS` / `GROUP_BY`
- `interval` required for `DATE_HISTO`, range `[1000, 1_000_000_000]`
- `size` defaults to 10 via `@DefaultValue("10")`

### BE-2: COUNT Query Type + Endpoint Scaffolding ✅

Full vertical slice from ES to REST endpoint. 5 new files + 2 test files, 6 modified files:

| File | Type | Purpose |
|---|---|---|
| `REPO_API/.../model/analytics/CountQuery.java` | New | Record: `apiId`, `from`, `to` with `@Builder` |
| `REPO_ES/.../adapter/SearchCountQueryAdapter.java` | New | Builds ES JSON: `size:0` + `bool.must` filter on `api-id` + `@timestamp` range |
| `REPO_ES/.../adapter/SearchCountResponseAdapter.java` | New | Reads `hits.total.value` from ES response |
| `SERVICE/.../use_case/SearchApiAnalyticsUseCase.java` | New | Unified use case with sealed `AnalyticsResult` interface, dispatches by type |
| `REPO_ES/.../adapter/SearchCountQueryAdapterTest.java` | New | 5 tests: null, api-only, api+time, time-only, all-null |
| `REPO_ES/.../adapter/SearchCountResponseAdapterTest.java` | New | 5 tests: null hits, null total, zero count, normal count, large count |
| `REPO_API/.../api/AnalyticsRepository.java` | Modified | Added `searchCount(QueryContext, CountQuery)` |
| `REPO_ES/.../AnalyticsElasticsearchRepository.java` | Modified | Implemented `searchCount` using V4_METRICS index |
| `SERVICE/.../query_service/AnalyticsQueryService.java` | Modified | Added `searchCount` method |
| `SERVICE/.../AnalyticsQueryServiceImpl.java` | Modified | Delegates to `analyticsRepository.searchCount()` |
| `SERVICE/test/.../FakeAnalyticsQueryService.java` | Modified | Added `countResult` field |
| `REST_V2/.../ApiAnalyticsResource.java` | Modified | Added `GET /analytics` unified endpoint with `@BeanParam` |

Key design: `SearchApiAnalyticsUseCase` uses a `sealed interface AnalyticsResult` with `CountResult` (expandable for STATS, GROUP_BY, DATE_HISTO in later stories). The REST endpoint returns `jakarta.ws.rs.core.Response` with a Map body to avoid OpenAPI code-gen dependency for now.

### BE-3: STATS Query Type ✅

Min/max/avg/sum statistics for a metric field. 3 new source files + 2 repo models + 2 test files, 8 modified files:

| File | Type | Purpose |
|---|---|---|
| `SERVICE/.../model/StatsResult.java` | New | Record: `count`, `min`, `max`, `avg`, `sum` with `@Builder` |
| `REPO_API/.../model/analytics/StatsQuery.java` | New | Record: `apiId`, `from`, `to`, `field` |
| `REPO_API/.../model/analytics/StatsAggregate.java` | New | Record: `count`, `min`, `max`, `avg`, `sum` |
| `REPO_ES/.../adapter/SearchStatsQueryAdapter.java` | New | ES JSON: `size:0`, filter api-id + @timestamp, `aggs.stats_field.stats` on field |
| `REPO_ES/.../adapter/SearchStatsResponseAdapter.java` | New | Parse stats agg → `StatsAggregate`; coerces null/NaN to 0.0 |
| `REPO_ES/.../adapter/SearchStatsQueryAdapterTest.java` | New | 3 tests: full query, field variants, minimal query |
| `REPO_ES/.../adapter/SearchStatsResponseAdapterTest.java` | New | 5 tests: null aggs, empty aggs, normal stats, empty-range (count=0), missing stats agg |
| `AnalyticsQueryService` / `AnalyticsQueryServiceImpl` | Modified | Added `searchStats(ExecutionContext, apiId, from, to, field)` |
| `AnalyticsRepository` / `AnalyticsElasticsearchRepository` | Modified | Added `searchStats(QueryContext, StatsQuery)` |
| `SearchApiAnalyticsUseCase` | Modified | Added `STATS` branch, `StatsResultResult` sealed variant |
| `ApiAnalyticsResource` | Modified | `mapResult()` handles STATS → Map with type, count, min, max, avg, sum |
| `FakeAnalyticsQueryService` | Modified | Added `statsResult` field, `searchStats()` impl |
| `NoOpAnalyticsRepository` | Modified | Added `searchStats()` returning `Optional.empty()` |
| `SearchApiAnalyticsUseCaseTest` | Modified | `StatsQuery` nested class: should_return_stats, should_return_stats_with_zero_values_for_empty_range |

Endpoint: `GET /environments/{envId}/apis/{apiId}/analytics?type=STATS&field=gateway-response-time-ms&from=...&to=...`

Gotcha: gravitee `Aggregation` uses `Float` for count/min/max/avg/sum; adapter uses `longValue()` for count and `safeDouble()` for numeric fields.

### BE-4: GROUP_BY Query Type ✅

Request counts broken down by field (e.g. HTTP status code). 3 new source files + 2 repo models + 2 test files, 8 modified files:

| File | Type | Purpose |
|---|---|---|
| `SERVICE/.../model/GroupByResult.java` | New | Record: `values` (Map<String,Long>), `metadata` (Map<String,Map<String,String>>) |
| `REPO_API/.../model/analytics/GroupByQuery.java` | New | Record: `apiId`, `from`, `to`, `field`, `size` |
| `REPO_API/.../model/analytics/GroupByAggregate.java` | New | Record: `values`, `metadata` |
| `REPO_ES/.../adapter/SearchGroupByQueryAdapter.java` | New | ES JSON: filter api-id + @timestamp, `aggs.group_by_field.terms` on field with size |
| `REPO_ES/.../adapter/SearchGroupByResponseAdapter.java` | New | Parse terms buckets → `GroupByAggregate`; empty result → `{values:{}, metadata:{}}` |
| `REPO_ES/.../adapter/SearchGroupByQueryAdapterTest.java` | New | 3 tests: full query, field variants, minimal query |
| `REPO_ES/.../adapter/SearchGroupByResponseAdapterTest.java` | New | 5 tests: null aggs, empty aggs, buckets parsing, empty docs, missing agg |
| `AnalyticsQueryService` / `AnalyticsQueryServiceImpl` | Modified | Added `searchGroupBy(ExecutionContext, apiId, from, to, field, size)` |
| `AnalyticsRepository` / `AnalyticsElasticsearchRepository` | Modified | Added `searchGroupBy(QueryContext, GroupByQuery)` |
| `SearchApiAnalyticsUseCase` | Modified | Added `GROUP_BY` branch, `GroupByResultResult` sealed variant |
| `ApiAnalyticsResource` | Modified | `mapResult()` handles GROUP_BY → Map with type, values, metadata |
| `FakeAnalyticsQueryService` | Modified | Added `groupByResult` field, `searchGroupBy()` impl |
| `NoOpAnalyticsRepository` | Modified | Added `searchGroupBy()` returning `Optional.empty()` |
| `SearchApiAnalyticsUseCaseTest` | Modified | `GroupByQuery` nested class: should_return_group_by, should_return_empty_group_by_when_no_data |

Endpoint: `GET /environments/{envId}/apis/{apiId}/analytics?type=GROUP_BY&field=status&from=...&to=...&size=20`

Gotcha: In adapter tests, use `JsonNode` (not `ObjectNode`) as return type for bucket creation—`List<ObjectNode>` is not assignable to `List<JsonNode>` due to generic invariance.

### BE-5: DATE_HISTO Query Type ✅

Time-bucketed histogram data with breakdown by field (e.g. status over time). 3 new source files + 2 repo models + 2 test files, 9 modified files:

| File | Type | Purpose |
|---|---|---|
| `SERVICE/.../model/DateHistoResult.java` | New | Record: `timestamps` (List<Long>), `values` (List<DateHistoBucket>) with field, buckets, metadata |
| `REPO_API/.../model/analytics/DateHistogramQuery.java` | New | Record: `apiId`, `from`, `to`, `field`, `interval` (Duration), `size` |
| `REPO_API/.../model/analytics/DateHistoAggregate.java` | New | Record: `timestamps`, `values` (List<DateHistoBucketAggregate>) |
| `REPO_ES/.../adapter/SearchDateHistogramQueryAdapter.java` | New | ES JSON: filter api-id + @timestamp, `aggs.by_date.date_histogram` with fixed_interval/interval (ES version), sub-agg `by_field.terms` |
| `REPO_ES/.../adapter/SearchDateHistogramResponseAdapter.java` | New | Parse date_histogram + nested terms buckets → `DateHistoAggregate`; empty → `{timestamps:[], values:[]}` |
| `REPO_ES/.../adapter/SearchDateHistogramQueryAdapterTest.java` | New | 4 tests: full query, fixed vs legacy interval, minimal query |
| `REPO_ES/.../adapter/SearchDateHistogramResponseAdapterTest.java` | New | 5 tests: null aggs, empty aggs, date+terms parsing, empty buckets, missing agg |
| `AnalyticsQueryService` / `AnalyticsQueryServiceImpl` | Modified | Added `searchDateHistogram(ExecutionContext, apiId, from, to, field, Duration, size)` |
| `AnalyticsRepository` / `AnalyticsElasticsearchRepository` | Modified | Added `searchDateHistogram(QueryContext, DateHistogramQuery)`; passes `info` for ES version |
| `SearchApiAnalyticsUseCase` | Modified | Added `DATE_HISTO` branch, `DateHistoResultResult` sealed variant |
| `ApiAnalyticsResource` | Modified | `mapResult()` handles DATE_HISTO → Map with type, timestamps, values |
| `AnalyticsParam` | Modified | `field` required for DATE_HISTO |
| `FakeAnalyticsQueryService` | Modified | Added `dateHistoResult` field, `searchDateHistogram()` impl |
| `NoOpAnalyticsRepository` | Modified | Added `searchDateHistogram()` returning `Optional.empty()` |
| `SearchApiAnalyticsUseCaseTest` | Modified | `DateHistoQuery` nested class: should_return_date_histo, should_return_empty_date_histo_when_no_data |

Endpoint: `GET /environments/{envId}/apis/{apiId}/analytics?type=DATE_HISTO&field=status&interval=3600000&from=...&to=...&size=20`

Gotcha: Switch expression block must use `yield` not `return` — added `.cursor/rules/java-conventions.mdc` to document this.

### BE-6: Backend Integration Tests ✅

REST integration tests for the unified analytics endpoint. 1 modified file:

| File | Changes |
|---|---|
| `REST_V2/.../ApiAnalyticsResourceTest.java` | Added `UnifiedAnalytics` nested class with validation, COUNT, STATS, GROUP_BY, DATE_HISTO tests |

**Validation tests:** 403 (permissions), 400 (type/from/to/field/interval missing, from≥to, unsupported field), 4xx (TCP API).

**Query tests:** Each of COUNT, STATS, GROUP_BY, DATE_HISTO has happy-path + empty-data tests.

---

## Key Decisions

| Decision | Rationale |
|---|---|
| `from`/`to` are `Long` (boxed) not `long` (primitive) | V1 used primitive `long` which defaults to 0, making "missing" indistinguishable from "epoch 0". Boxed `Long` lets us distinguish null (missing) from 0. |
| `interval` is `Long` (boxed) | Same reason — null means "not provided", which is valid for non-DATE_HISTO types. V1 validated interval for all types; we only validate it for DATE_HISTO. |
| Field validation happens before type-specific checks | An unsupported field value is rejected early regardless of type, so you don't get a confusing "field required" error when you actually provided a bad field name. |
| Unit tests for `AnalyticsParam` rather than waiting for BE-6 integration tests | The stories say "tested as part of BE-6", but validate() is pure logic with no dependencies — unit-testable now, gives faster feedback. BE-6 will still add integration coverage. |
| `AnalyticsFieldParam` uses a `Map<String, Enum>` lookup | O(1) lookup vs iterating `values()`. The field names use kebab-case (matching ES field names), so the enum constants use SCREAMING_SNAKE but expose the kebab-case via `getValue()`. |
| `sealed interface AnalyticsResult` for polymorphic returns | Java 17 sealed types give exhaustive switch. Each query type adds a record variant (`CountResult`, later `StatsResult`, etc.). Compiler enforces all branches handled. |
| `CountQuery` uses non-optional fields unlike `RequestsCountQuery` | `RequestsCountQuery` wraps everything in `Optional` for flexibility. Our unified endpoint always has `apiId`/`from`/`to` validated by `AnalyticsParam`, so `CountQuery` uses plain types. Simpler adapter code. |
| Return `Response` with `Map` body instead of generated model | The response models (`ApiAnalyticsCountResponse`, etc.) come from OpenAPI code generation. Skipping the OpenAPI YAML edit (BE-2.1) for now — will add when we have all 4 response types to define at once. The Map approach is functionally identical. |
| Count uses `hits.total.value` not aggregations | Unlike `SearchRequestsCountQueryAdapter` which uses a terms aggregation on `entrypoint-id`, the unified COUNT just needs the total document count. Simpler query, simpler response parsing. |

---

## Gotchas & Surprises

1. **V1 `AnalyticsParam` validates `interval` for ALL types** — even COUNT/GROUP_BY where interval is irrelevant. V1 also checks `interval == -1` which only happens if the query string contains `interval=-1`. Our V2 version is stricter: interval is only validated when `type == DATE_HISTO`.

2. **V1 uses primitive `long` for `from`/`to`** — which means `from=0` and "from not provided" are the same. The V1 code works around this with `-1` sentinel values. V2 uses boxed `Long` for cleaner null handling.

3. **`@DefaultValue("10")` on `size`** — JAX-RS defaults `int` to 0, not 10. Without this annotation, a GROUP_BY query without explicit `size` would silently request 0 buckets from ES.

4. **No existing param tests in the V2 module** — the V2 REST module had zero test files under `resource/param/`. All existing analytics validation was integration-tested through the resource test. We created the first param-level unit test.

5. **gravitee `Aggregation` uses `Float`** — `getCount()` returns `Float`, not `Long`. Use `longValue()` when mapping to `long`. Setters (`setCount`, `setMin`, etc.) expect `Float`; use `100f`, `5.0f` in tests.

---

## Current Blockers / Open Questions

- **None blocking.** BE-1 through BE-5 are complete. The unified endpoint supports all 4 query types: COUNT, STATS, GROUP_BY, DATE_HISTO.
- **`endpoint-response-time-ms` field** — flagged in STORIES.md Known Risks. Not relevant until FE-2 (stat cards). Must verify against a live `*-v4-metrics-*` index before using it.
- **OpenAPI spec update** (BE-2.1) — deferred. Response is currently a `Map` body. Will add proper schema when all 4 response types are defined.

---

## Effective Prompts

### Initial prompt (Story 1 kickoff)

```
Read @docs/workshop/STORIES.md for the full list of user stories.

Before starting, study the existing code that we're evolving:

Backend (Java):
- ApiAnalyticsResource.java in gravitee-apim-rest-api/.../api/analytics/
  — has existing endpoints: /requests-count, /response-status-ranges, etc.
- The existing use cases (SearchRequestsCountAnalyticsUseCase, etc.)
- How Elasticsearch queries are built and executed
- ApiAnalyticsResourceTest.java for test patterns

Frontend (Angular):
- ApiAnalyticsProxyComponent in api-traffic-v4/analytics/api-analytics-proxy/
- ApiAnalyticsV2Service in services-ngx/api-analytics-v2.service.ts
- Existing widget components in api-traffic-v4/analytics/components/
- Chart libraries already in use

Now implement Story 1.
Include tests that follow the existing test patterns.
Keep the existing separate endpoints working — don't break them.
```

**Why it worked:** Pointed the agent at concrete file paths across all layers (REST resource, use cases, ES adapters, tests, frontend). The "study before starting" instruction forced deep pattern-reading before any code was written. "Keep existing endpoints working" set a clear non-regression constraint.

### Story 2 prompt

```
Implement Story 2 from @docs/workshop/STORIES.md
Include tests following the existing patterns in the codebase.
```

**Why it worked:** Short and effective because the agent already had full codebase context from Story 1. The `@docs/workshop/STORIES.md` reference anchored the exact requirements. "Following existing patterns" leveraged the deep pattern study already done.

### Story 3 prompt

Read @docs/workshop/STORIES.md  and @docs/workshop/PROGRESS.md for the full list of user stories.

Before starting, study the existing code that we're evolving:

Backend (Java):
- ApiAnalyticsResource.java in gravitee-apim-rest-api/.../api/analytics/
  — has existing endpoints: /requests-count, /response-status-ranges, etc.
- The existing use cases (SearchRequestsCountAnalyticsUseCase, etc.)
- How Elasticsearch queries are built and executed
- ApiAnalyticsResourceTest.java for test patterns

Frontend (Angular):
- ApiAnalyticsProxyComponent in api-traffic-v4/analytics/api-analytics-proxy/
- ApiAnalyticsV2Service in services-ngx/api-analytics-v2.service.ts
- Existing widget components in api-traffic-v4/analytics/components/
- Chart libraries already in use

Now implement Story 3.
Include tests that follow the existing test patterns.
Keep the existing separate endpoints working — don't break them.


### Story 4 prompt

Read @docs/workshop/STORIES.md  and @docs/workshop/PROGRESS.md for the full list of user stories.

Before starting, study the existing code that we're evolving:

Backend (Java):
- ApiAnalyticsResource.java in gravitee-apim-rest-api/.../api/analytics/
  — has existing endpoints: /requests-count, /response-status-ranges, etc.
- The existing use cases (SearchRequestsCountAnalyticsUseCase, etc.)
- How Elasticsearch queries are built and executed
- ApiAnalyticsResourceTest.java for test patterns

Frontend (Angular):
- ApiAnalyticsProxyComponent in api-traffic-v4/analytics/api-analytics-proxy/
- ApiAnalyticsV2Service in services-ngx/api-analytics-v2.service.ts
- Existing widget components in api-traffic-v4/analytics/components/
- Chart libraries already in use

Now implement Story 4.
Include tests that follow the existing test patterns.
Keep the existing separate endpoints working — don't break them.

### Story 5 prompt

```
Read @docs/workshop/STORIES.md and @docs/workshop/PROGRESS.md for the full list of user stories.

Before starting, study the existing code that we're evolving:

Backend (Java):
- ApiAnalyticsResource.java in gravitee-apim-rest-api/.../api/analytics/
  — has existing endpoints: /requests-count, /response-status-ranges, etc.
- The existing use cases (SearchRequestsCountAnalyticsUseCase, etc.)
- How Elasticsearch queries are built and executed
- ApiAnalyticsResourceTest.java for test patterns

Frontend (Angular):
- ApiAnalyticsProxyComponent in api-traffic-v4/analytics/api-analytics-proxy/
- ApiAnalyticsV2Service in services-ngx/api-analytics-v2.service.ts
- Existing widget components in api-traffic-v4/analytics/components/
- Chart libraries already in use

Now implement BE Story 5.
Include tests that follow the existing test patterns.
Keep the existing separate endpoints working — don't break them.
```


### Story 6 prompt

Ran in by the same agent session as Story 5

Now implement BE story 6