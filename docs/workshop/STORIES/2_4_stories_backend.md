### Story 2.4: COUNT Query Implementation

**Title:** Implement COUNT aggregation for total request count

**Description:**
As an API publisher, I want to see the total number of requests to my API in a given time range so that I can understand overall traffic volume.

**Acceptance Criteria:**
- [ ] `GET /v2/apis/{apiId}/analytics?type=COUNT&from=X&to=Y` returns `{"type":"COUNT","count":12345}`
- [ ] Query filters by API ID and time range (@timestamp)
- [ ] Query uses `-v4-metrics-*` index pattern
- [ ] Count aggregates across all entrypoints
- [ ] Returns 0 count when no data exists
- [ ] Unit tests verify ES query structure
- [ ] Integration test with Testcontainers verifies actual ES query execution

**Layer:** Backend
**Complexity:** S (3 days)
**Dependencies:** Story 2.3

#### Subtask 2.4.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **SearchCountAnalyticsQueryAdapterTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchCountAnalyticsQueryAdapterTest.java`
   - **Test Cases:**
     - `shouldBuildValidElasticsearchQuery()`
     - `shouldIncludeApiIdFilter()`
     - `shouldIncludeTimestampRangeFilter()`
     - `shouldSetSizeToZero()`
     - `shouldUseV4MetricsIndex()`

2. **SearchCountAnalyticsResponseAdapterTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchCountAnalyticsResponseAdapterTest.java`
   - **Test Cases:**
     - `shouldParseElasticsearchResponse()`
     - `shouldReturnZeroWhenNoHits()`
     - `shouldHandleMissingTotalObject()`

3. **ApiAnalyticsResourceTest.java** (extend existing)
   - **Add Test Cases:**
     - `shouldReturnCountResponse()`
     - `shouldReturnZeroCountWhenNoData()`
     - `shouldReturnCountWithinTimeRange()`

**Reference Patterns:**
- Use JUnit 5 `@Test` annotation
- Use AssertJ for fluent assertions
- Parse generated JSON to verify structure

#### Subtask 2.4.1: Create Elasticsearch Query Adapter

**Files to Create:**

1. **SearchCountAnalyticsQueryAdapter.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchCountAnalyticsQueryAdapter.java`
   - **Content:**
   ```java
   package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

   import io.gravitee.apim.core.analytics.model.CountQuery;
   import io.vertx.core.json.JsonObject;
   import lombok.AccessLevel;
   import lombok.NoArgsConstructor;
   import java.util.HashMap;
   import java.util.List;
   import java.util.Map;

   @NoArgsConstructor(access = AccessLevel.PRIVATE)
   public class SearchCountAnalyticsQueryAdapter {

       public static String adapt(CountQuery query) {
           var jsonContent = new HashMap<String, Object>();
           jsonContent.put("size", 0);
           jsonContent.put("query", buildBoolQuery(query));

           return new JsonObject(jsonContent).encode();
       }

       private static Map<String, Object> buildBoolQuery(CountQuery query) {
           var must = List.of(
               Map.of("term", Map.of("api-id", query.apiId())),
               Map.of("range", Map.of(
                   "@timestamp", Map.of(
                       "gte", query.from().toEpochMilli(),
                       "lte", query.to().toEpochMilli()
                   )
               ))
           );

           return Map.of("bool", Map.of("must", must));
       }
   }
   ```

**Reference Patterns:**
- Existing: `SearchRequestsCountQueryAdapter.java`
- Use `JsonObject` from Vert.x

#### Subtask 2.4.2: Create Elasticsearch Response Adapter

**Files to Create:**

1. **SearchCountAnalyticsResponseAdapter.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchCountAnalyticsResponseAdapter.java`
   - **Content:**
   ```java
   package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

   import io.gravitee.rest.api.model.v4.analytics.CountResponse;
   import io.vertx.core.json.JsonObject;
   import lombok.AccessLevel;
   import lombok.NoArgsConstructor;

   @NoArgsConstructor(access = AccessLevel.PRIVATE)
   public class SearchCountAnalyticsResponseAdapter {

       public static CountResponse adapt(String esResponse) {
           var json = new JsonObject(esResponse);
           var hits = json.getJsonObject("hits");

           if (hits == null) {
               return CountResponse.builder()
                   .type("COUNT")
                   .count(0L)
                   .build();
           }

           var total = hits.getJsonObject("total");
           var count = total != null ? total.getLong("value", 0L) : 0L;

           return CountResponse.builder()
               .type("COUNT")
               .count(count)
               .build();
       }
   }
   ```

**Reference Patterns:**
- Parse ES JSON response using Vert.x `JsonObject`
- Handle missing values with defaults

#### Subtask 2.4.3: Add Repository Method

**Files to Modify:**

1. **AnalyticsRepository.java** (interface)
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/analytics/api/AnalyticsRepository.java`
   - **Action:** Add method signature
   ```java
   Optional<CountResponse> searchCount(CountQuery query);
   ```

2. **AnalyticsElasticsearchRepository.java** (implementation)
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsElasticsearchRepository.java`
   - **Action:** Implement method
   ```java
   @Override
   public Optional<CountResponse> searchCount(CountQuery query) {
       try {
           String esQuery = SearchCountAnalyticsQueryAdapter.adapt(query);
           String response = client.search(Type.V4_METRICS, esQuery);
           return Optional.of(SearchCountAnalyticsResponseAdapter.adapt(response));
       } catch (Exception e) {
           log.error("Error executing COUNT query", e);
           return Optional.empty();
       }
   }
   ```

**Reference Patterns:**
- Existing methods in `AnalyticsElasticsearchRepository.java`
- Use index constant: `Type.V4_METRICS`

#### Subtask 2.4.4: Add Query Service Method

**Files to Create:**

1. **AnalyticsQueryService.java** (interface)
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.query_service;

   import io.gravitee.apim.core.analytics.model.CountQuery;
   import io.gravitee.rest.api.model.v4.analytics.CountResponse;
   import java.util.Optional;

   public interface AnalyticsQueryService {
       Optional<CountResponse> searchCount(CountQuery query);
   }
   ```

2. **AnalyticsQueryServiceImpl.java** (implementation)
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImpl.java`
   - **Content:**
   ```java
   package io.gravitee.apim.infra.query_service.analytics;

   import io.gravitee.apim.core.analytics.model.CountQuery;
   import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
   import io.gravitee.repository.analytics.api.AnalyticsRepository;
   import io.gravitee.rest.api.model.v4.analytics.CountResponse;
   import java.util.Optional;

   public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

       private final AnalyticsRepository analyticsRepository;

       public AnalyticsQueryServiceImpl(AnalyticsRepository analyticsRepository) {
           this.analyticsRepository = analyticsRepository;
       }

       @Override
       public Optional<CountResponse> searchCount(CountQuery query) {
           return analyticsRepository.searchCount(query);
       }
   }
   ```

**Reference Patterns:**
- Simple delegation pattern from service to repository

#### Subtask 2.4.5: Wire Use Case to Handle COUNT Type

**Files to Modify:**

1. **SearchUnifiedAnalyticsUseCase.java**
   - **Action:** Add COUNT query handling
   ```java
   private final AnalyticsQueryService analyticsQueryService;

   public SearchUnifiedAnalyticsUseCase(
       ApiQueryService apiQueryService,
       AnalyticsQueryService analyticsQueryService
   ) {
       this.apiQueryService = apiQueryService;
       this.analyticsQueryService = analyticsQueryService;
   }

   public Output execute(ExecutionContext executionContext, Input input) {
       // Fetch and validate API
       var api = apiQueryService.findById(input.apiId())
           .orElseThrow(() -> new IllegalArgumentException("API not found: " + input.apiId()));

       ApiAnalyticsValidator.validateApiRequirements(api, input.environmentId());

       // Handle query types
       return switch (input.query()) {
           case CountQuery countQuery -> {
               var result = analyticsQueryService.searchCount(countQuery);
               yield new Output(result.map(r -> (AnalyticsResponse) r));
           }
           default -> new Output(Optional.empty());
       };
   }
   ```

#### Subtask 2.4.6: Integration Test with Testcontainers

**Files to Create:**

1. **SearchCountAnalyticsIntegrationTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/SearchCountAnalyticsIntegrationTest.java`
   - **Content:**
   ```java
   package io.gravitee.repository.elasticsearch.v4.analytics;

   import io.gravitee.apim.core.analytics.model.CountQuery;
   import org.junit.jupiter.api.BeforeAll;
   import org.junit.jupiter.api.Test;
   import org.testcontainers.elasticsearch.ElasticsearchContainer;
   import org.testcontainers.junit.jupiter.Container;
   import org.testcontainers.junit.jupiter.Testcontainers;
   import java.time.Instant;
   import static org.assertj.core.api.Assertions.assertThat;

   @Testcontainers
   class SearchCountAnalyticsIntegrationTest {

       @Container
       static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
           "docker.elastic.co/elasticsearch/elasticsearch:8.11.0"
       );

       @BeforeAll
       static void setup() {
           // Create index and insert test data
           // - Create v4-metrics index with proper mapping
           // - Insert 100 test documents with varied timestamps and API IDs
       }

       @Test
       void shouldExecuteActualCountQuery() {
           // Create query
           var query = new CountQuery(
               "api-123",
               Instant.now().minusSeconds(3600),
               Instant.now()
           );

           // Execute against real Elasticsearch
           var result = repository.searchCount(query);

           // Verify
           assertThat(result).isPresent();
           assertThat(result.get().getCount()).isEqualTo(50); // Expected count from test data
       }

       @Test
       void shouldReturnZeroForEmptyIndex() {
           var query = new CountQuery(
               "nonexistent-api",
               Instant.now().minusSeconds(3600),
               Instant.now()
           );

           var result = repository.searchCount(query);

           assertThat(result).isPresent();
           assertThat(result.get().getCount()).isZero();
       }

       @Test
       void shouldFilterByTimeRange() {
           var query = new CountQuery(
               "api-123",
               Instant.now().minusSeconds(1800), // 30 minutes ago
               Instant.now()
           );

           var result = repository.searchCount(query);

           assertThat(result).isPresent();
           assertThat(result.get().getCount()).isEqualTo(25); // Only requests in last 30min
       }
   }
   ```

**Maven Dependency:**
```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>elasticsearch</artifactId>
  <version>1.19.0</version>
  <scope>test</scope>
</dependency>
```

**Reference Patterns:**
- Use Testcontainers for real Elasticsearch
- Populate with realistic test data

#### Subtask 2.4.7: Run Tests (GREEN)

- Verify all unit tests pass
- Verify integration test with Testcontainers passes
- Verify REST endpoint returns actual count

---

