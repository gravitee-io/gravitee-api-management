### Story 3.1: STATS Query Implementation

**Title:** Implement STATS aggregation for numeric field statistics

**Description:**
As an API publisher, I want to see statistical aggregations (min, max, avg, sum) for numeric fields so that I can analyze performance metrics.

**Acceptance Criteria:**
- [ ] `GET /v2/apis/{apiId}/analytics?type=STATS&field=gateway-latency-ms&from=X&to=Y` returns stats
- [ ] Response includes: count, min, max, avg, sum
- [ ] Null values handled correctly (when no data exists)
- [ ] Field validation ensures only allowed fields are queried
- [ ] Integration test with Testcontainers verifies actual ES aggregation

**Layer:** Backend
**Complexity:** M (4 days)
**Dependencies:** Story 2.4

#### Subtask 3.1.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **AnalyticsFieldValidatorTest.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/analytics/validator/AnalyticsFieldValidatorTest.java`
   - **Test Cases:**
     - `shouldAcceptValidStatsField()`
     - `shouldRejectInvalidStatsField()`
     - `shouldAcceptValidGroupByField()`

2. **SearchStatsAnalyticsQueryAdapterTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchStatsAnalyticsQueryAdapterTest.java`
   - **Test Cases:**
     - `shouldBuildStatsAggregation()`
     - `shouldIncludeAllStatsFunctions()`
     - `shouldHandleKeywordFieldSuffix()`

3. **SearchStatsAnalyticsResponseAdapterTest.java**
   - **Path:** Same directory
   - **Test Cases:**
     - `shouldParseStatsResponse()`
     - `shouldHandleNullValues()`
     - `shouldReturnZeroCountWhenNoData()`

#### Subtask 3.1.1: Create Field Validator

**Files to Create:**

1. **AnalyticsFieldValidator.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/validator/AnalyticsFieldValidator.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.validator;

   import java.util.Set;

   public class AnalyticsFieldValidator {

       private static final Set<String> STATS_FIELDS = Set.of(
           "gateway-latency-ms",
           "gateway-response-time-ms",
           "endpoint-response-time-ms",
           "request-content-length",
           "response-content-length"
       );

       private static final Set<String> GROUP_BY_FIELDS = Set.of(
           "status",
           "http-method",
           "host",
           "endpoint",
           "application",
           "plan"
       );

       public static void validateStatsField(String field) {
           if (!STATS_FIELDS.contains(field)) {
               throw new IllegalArgumentException(
                   "Invalid field for STATS query. Allowed: " + String.join(", ", STATS_FIELDS)
               );
           }
       }

       public static void validateGroupByField(String field) {
           if (!GROUP_BY_FIELDS.contains(field)) {
               throw new IllegalArgumentException(
                   "Invalid field for GROUP_BY query. Allowed: " + String.join(", ", GROUP_BY_FIELDS)
               );
           }
       }

       public static boolean isKeywordField(String field) {
           return GROUP_BY_FIELDS.contains(field);
       }
   }
   ```

#### Subtask 3.1.2: Create Query Adapter

**Files to Create:**

1. **SearchStatsAnalyticsQueryAdapter.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/main/java/io/gravitee/repository/elasticsearch/v4/analytics/adapter/SearchStatsAnalyticsQueryAdapter.java`
   - **Content:**
   ```java
   package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

   import io.gravitee.apim.core.analytics.model.StatsQuery;
   import io.vertx.core.json.JsonObject;
   import lombok.AccessLevel;
   import lombok.NoArgsConstructor;
   import java.util.HashMap;
   import java.util.List;
   import java.util.Map;

   @NoArgsConstructor(access = AccessLevel.PRIVATE)
   public class SearchStatsAnalyticsQueryAdapter {

       public static String adapt(StatsQuery query) {
           var jsonContent = new HashMap<String, Object>();
           jsonContent.put("size", 0);
           jsonContent.put("query", buildBoolQuery(query));
           jsonContent.put("aggs", buildStatsAggregation(query.field()));

           return new JsonObject(jsonContent).encode();
       }

       private static Map<String, Object> buildBoolQuery(StatsQuery query) {
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

       private static Map<String, Object> buildStatsAggregation(String field) {
           return Map.of(
               "field_stats", Map.of(
                   "stats", Map.of("field", field)
               )
           );
       }
   }
   ```

#### Subtask 3.1.3: Create Response Adapter

**Files to Create:**

1. **SearchStatsAnalyticsResponseAdapter.java**
   - **Path:** Same directory
   - **Content:**
   ```java
   package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

   import io.gravitee.rest.api.model.v4.analytics.StatsResponse;
   import io.vertx.core.json.JsonObject;
   import lombok.AccessLevel;
   import lombok.NoArgsConstructor;

   @NoArgsConstructor(access = AccessLevel.PRIVATE)
   public class SearchStatsAnalyticsResponseAdapter {

       public static StatsResponse adapt(String esResponse) {
           var json = new JsonObject(esResponse);
           var aggregations = json.getJsonObject("aggregations");

           if (aggregations == null) {
               return StatsResponse.builder()
                   .type("STATS")
                   .count(0L)
                   .build();
           }

           var fieldStats = aggregations.getJsonObject("field_stats");
           if (fieldStats == null) {
               return StatsResponse.builder()
                   .type("STATS")
                   .count(0L)
                   .build();
           }

           return StatsResponse.builder()
               .type("STATS")
               .count(fieldStats.getLong("count", 0L))
               .min(fieldStats.getDouble("min"))
               .max(fieldStats.getDouble("max"))
               .avg(fieldStats.getDouble("avg"))
               .sum(fieldStats.getDouble("sum"))
               .build();
       }
   }
   ```

#### Subtask 3.1.4: Add Repository and Service Methods

**Files to Modify:**

1. **AnalyticsRepository.java**
   - **Action:** Add `searchStats` method signature

2. **AnalyticsElasticsearchRepository.java**
   - **Action:** Implement `searchStats` method

3. **AnalyticsQueryService.java**
   - **Action:** Add `searchStats` method signature

4. **AnalyticsQueryServiceImpl.java**
   - **Action:** Implement `searchStats` method

#### Subtask 3.1.5: Update Use Case

**Files to Modify:**

1. **SearchUnifiedAnalyticsUseCase.java**
   - **Action:** Add STATS query handling
   ```java
   case StatsQuery statsQuery -> {
       // Validate field
       AnalyticsFieldValidator.validateStatsField(statsQuery.field());

       var result = analyticsQueryService.searchStats(statsQuery);
       yield new Output(result.map(r -> (AnalyticsResponse) r));
   }
   ```

#### Subtask 3.1.6: Integration Test

**Files to Create:**

1. **SearchStatsAnalyticsIntegrationTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/SearchStatsAnalyticsIntegrationTest.java`
   - **Test Cases:**
     - `shouldCalculateActualStats()`
     - `shouldHandleNoData()`
     - `shouldFilterByApiAndTimeRange()`

#### Subtask 3.1.7: Run Tests (GREEN)

---

