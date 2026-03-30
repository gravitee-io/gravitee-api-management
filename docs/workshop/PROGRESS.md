# Workshop Progress Snapshot

Date: 2026-03-30  
Scope: V4 API Analytics Dashboard (M1)

---

## Stories completed so far

### Story 1 (B1a) — Repository contracts: unified analytics query/response models

**Summary:** Added the repository-layer contract for the unified analytics endpoint (field mapping enum, query criteria records, aggregate result models, and new repository method signatures). Added a small JUnit test for field lookup and ensured module formatting/tests pass.

**Implemented (high-signal files):**

- **New field mapping**
  - `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/model/analytics/ApiAnalyticsField.java`
  - Includes `fromPrdName(...)` lookup used by higher layers for validation

- **New query criteria**
  - `.../ApiAnalyticsCountQuery.java`
  - `.../ApiAnalyticsStatsQuery.java`
  - `.../ApiAnalyticsGroupByQuery.java`
  - `.../ApiAnalyticsDateHistoQuery.java`
  - `.../ApiAnalyticsGroupByOrder.java`

- **New aggregate result models**
  - `.../ApiAnalyticsCountAggregate.java`
  - `.../ApiAnalyticsStatsAggregate.java`
  - `.../ApiAnalyticsGroupByAggregate.java`
  - `.../ApiAnalyticsDateHistoAggregate.java`
  - `.../ApiAnalyticsMetadata.java`

- **Repository interface updated**
  - `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/log/v4/api/AnalyticsRepository.java`
  - Added new methods:
    - `searchApiAnalyticsCount(...)`
    - `searchApiAnalyticsStats(...)`
    - `searchApiAnalyticsGroupBy(...)`
    - `searchApiAnalyticsDateHisto(...)`

- **Tests added**
  - `gravitee-apim-repository/gravitee-apim-repository-api/src/test/java/io/gravitee/repository/log/v4/model/analytics/ApiAnalyticsFieldTest.java`

**Verification:**

- `mvn test -pl gravitee-apim-repository/gravitee-apim-repository-api -q` ✅
- `mvn clean compile -pl gravitee-apim-repository/gravitee-apim-repository-elasticsearch,gravitee-apim-repository/gravitee-apim-repository-noop -am -q` ✅
- `mvn test -pl gravitee-apim-rest-api -q` ✅ (full REST API module tests after wiring)

**Repository implementations (stubs until B1b):**

- `AnalyticsElasticsearchRepository` and `NoOpAnalyticsRepository` implement the new methods by returning `Optional.empty()` so clean builds succeed. Real ES queries land in Story 2 (B1b).

---

## Key decisions made (and why)

- **Field mapping is centralized in `ApiAnalyticsField`**: keeps supported field validation and ES-field mapping deterministic and reusable across adapter/use-case/resource layers.
- **Repository query criteria use `Optional` fields** (pattern match with `RequestsCountQuery`): aligns with existing repository API conventions.
- **Metadata modeled as `ApiAnalyticsMetadata(name)`**: minimal and sufficient for M1 widgets (status-code metadata); can be expanded later for application/plan resolution without changing the top-level response shape.
- **No defaults enforced in query constructors** (yet): the PRD default (`size=10`) is intended to be owned by the REST layer (B3) so the API contract is explicit; adapters can still apply a safety default in B1b.

---

## Gotchas / surprises encountered

- **Prettier formatting gate** in `gravitee-apim-repository-api`: the module fails builds on formatting check.\n
  - Fixed by running `mvn -pl gravitee-apim-repository/gravitee-apim-repository-api -q prettier:write`.
- **Interface-only change breaks `clean compile`** until all `AnalyticsRepository` implementations add the new methods.\n
  - Fixed with stub overrides in `AnalyticsElasticsearchRepository` and `NoOpAnalyticsRepository`.

---

### Story 2 (B1b) — Elasticsearch adapters & repository implementation for unified analytics

**Summary:** Implemented Elasticsearch query adapters + response adapters for unified analytics COUNT/STATS/GROUP_BY/DATE_HISTO and wired `AnalyticsElasticsearchRepository` to execute them against `Type.V4_METRICS`.

**Implemented (high-signal files):**

- **ES adapters (new)**
  - `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchApiAnalyticsCountQueryAdapter.java`
  - `.../SearchApiAnalyticsCountResponseAdapter.java` (uses `response.getSearchHits().getTotal().getValue()`)
  - `.../SearchApiAnalyticsStatsQueryAdapter.java` (min/max/avg/sum/value_count aggs)
  - `.../SearchApiAnalyticsStatsResponseAdapter.java` (reads `Aggregation.getValue()` per agg)
  - `.../SearchApiAnalyticsGroupByQueryAdapter.java` (terms + size/order)
  - `.../SearchApiAnalyticsGroupByResponseAdapter.java`
  - `.../SearchApiAnalyticsDateHistoQueryAdapter.java` (**min_doc_count=0** + **extended_bounds**)
  - `.../SearchApiAnalyticsDateHistoResponseAdapter.java` (builds aligned series buckets)

- **Repository wiring**
  - `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepository.java`
    - Removed stubs; now calls `client.search(...).map(...Adapter::adapt).blockingGet()` for each unified method.

- **Tests added (adapter-level)**
  - `.../src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchApiAnalyticsStatsQueryAdapterTest.java`
  - `.../SearchApiAnalyticsGroupByQueryAdapterTest.java`
  - `.../SearchApiAnalyticsDateHistoQueryAdapterTest.java`

**Verification:**

- `mvn -pl gravitee-apim-repository/gravitee-apim-repository-elasticsearch -am -q test -Dtest="SearchApiAnalytics*AdapterTest" -Dsurefire.failIfNoSpecifiedTests=false` ✅
- `mvn test -pl gravitee-apim-rest-api -q` ✅ (legacy REST API tests still pass)

**Note:** Full `gravitee-apim-repository-elasticsearch` test suite is not runnable in this environment due to pre-existing Spring/Mockito/Testcontainers-style integration tests failing to initialize. We keep our verification scoped to adapter unit tests + REST API module tests (per workshop workflow).

---

### Story 3 (B2) — Core: `AnalyticsQueryService` + per-type use cases

**Summary:** Extended `AnalyticsQueryService` with four methods that delegate to `AnalyticsRepository` (same pattern as `searchRequestsCount`). Added one use case per query type with the same V4 validation as `SearchRequestsCountAnalyticsUseCase`, plus **empty results when `analytics.enabled` is false** (no repository call). Shared validation lives in package-private `ApiAnalyticsV4ApiValidation`. Test fixture `ApiFixtures.aMessageApiV4WithAnalyticsEnabled()` enables analytics for happy-path tests (fixtures default to disabled).

**Verification:**

- `mvn -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service -am -q test -Dtest="SearchApiAnalytics*UseCaseTest,AnalyticsQueryServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false` ✅

---

### Story 4 (B3) — REST: `GET .../apis/{apiId}/analytics` + OpenAPI

**Summary:** Added `GET /environments/{envId}/apis/{apiId}/analytics` with query params `type`, `from`, `to`, optional `field`, `interval`, `size`, `order`. OpenAPI defines `ApiUnifiedAnalyticsResponse` and related schemas; implementation is `ApiAnalyticsResource.getApiUnifiedAnalytics` plus `ApiUnifiedAnalyticsSupport` for validation (400 via `BadRequestException`) and mapping from repository aggregates. `GROUP_BY` defaults `size` to 10; `DATE_HISTO` caps histogram buckets at 500. Legacy sub-paths (`/requests-count`, etc.) unchanged.

**Verification:**

- `mvn -pl gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest -am -q test -Dtest=ApiAnalyticsResourceTest -Dsurefire.failIfNoSpecifiedTests=false` ✅

## Current blockers / open questions

- **ES field names & types**: `ApiAnalyticsField` currently maps PRD names directly to same-named ES fields with a numeric/keyword hint. When implementing B1b, we must confirm actual v4 metrics index mappings (and any `.keyword` requirements).\n
- **DATE_HISTO response shape**: repository aggregate models define `timestamps` and series `values`; B1b needs to confirm the ES aggregation layout used to produce that shape consistently.\n
- **GROUP_BY order semantics**: currently encoded as `COUNT_DESC/COUNT_ASC`. B3 must map REST `order` query param(s) to this enum.

---

## Prompts that were particularly effective (exact text)

- “We have 8 stories total (4 backend, 4 frontend). Now implement Story 1. Include tests that follow the existing test patterns. Keep the existing separate endpoints working — don't break them.”
- “Before we start implementing, verify that the development environment is set up correctly. Check each of the following and report pass/fail… Save the results to ./docs/workshop/ENVIRONMENT_CHECK.md”

