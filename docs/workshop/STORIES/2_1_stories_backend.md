### Story 2.1: Analytics Domain Models

**Title:** Create analytics domain models and type hierarchy

**Description:**
As a platform developer, I want well-typed domain models for analytics queries and responses so that I have type-safe communication between layers.

**Acceptance Criteria:**
- [ ] All 10 model files compile successfully
- [ ] Lombok annotations work correctly (`@Data`, `@Builder`, `@SuperBuilder`)
- [ ] Sealed interfaces provide compile-time exhaustiveness checking
- [ ] Java records are used for immutable DTOs
- [ ] Serialization/deserialization tests pass
- [ ] Models support JSON serialization for REST responses

**Layer:** Backend
**Complexity:** M (3 days)
**Dependencies:** None

#### Subtask 2.1.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **AnalyticsQueryTest.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/analytics/model/AnalyticsQueryTest.java`
   - **Test Cases:**
     - `shouldCreateCountQuery()`
     - `shouldCreateStatsQuery()`
     - `shouldCreateGroupByQuery()`
     - `shouldCreateDateHistoQuery()`
     - `shouldEnforceTypeConstraintsAtCompileTime()` (compile test)
     - `shouldProvideCorrectTypeForEachQuery()`

2. **AnalyticsResponseTest.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/test/java/io/gravitee/rest/api/model/v4/analytics/AnalyticsResponseTest.java`
   - **Test Cases:**
     - `shouldSerializeCountResponseToJson()`
     - `shouldSerializeStatsResponseToJson()`
     - `shouldSerializeGroupByResponseToJson()`
     - `shouldSerializeDateHistoResponseToJson()`
     - `shouldDeserializeJsonToCountResponse()`
     - `shouldDeserializeJsonToStatsResponse()`
     - `shouldHandleNullValuesInStatsResponse()`
     - `shouldBuildWithLombokBuilders()`

**Reference Patterns:**
- Existing test: `RequestsCountTest.java`
- Use JUnit 5 `@Test` annotation
- Use AssertJ for assertions
- Use Jackson ObjectMapper for serialization tests

#### Subtask 2.1.1: Create Core Query Models

**Files to Create:**

1. **AnalyticsQuery.java** (sealed interface)
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/AnalyticsQuery.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.model;

   import java.time.Instant;

   public sealed interface AnalyticsQuery
       permits CountQuery, StatsQuery, GroupByQuery, DateHistoQuery {
       String apiId();
       Instant from();
       Instant to();
       QueryType type();

       enum QueryType {
           COUNT, STATS, GROUP_BY, DATE_HISTO
       }
   }
   ```

2. **CountQuery.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/CountQuery.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.model;

   import java.time.Instant;

   public record CountQuery(
       String apiId,
       Instant from,
       Instant to
   ) implements AnalyticsQuery {
       @Override
       public QueryType type() {
           return QueryType.COUNT;
       }
   }
   ```

3. **StatsQuery.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/StatsQuery.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.model;

   import java.time.Instant;

   public record StatsQuery(
       String apiId,
       Instant from,
       Instant to,
       String field
   ) implements AnalyticsQuery {
       @Override
       public QueryType type() {
           return QueryType.STATS;
       }
   }
   ```

4. **GroupByQuery.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/GroupByQuery.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.model;

   import java.time.Instant;

   public record GroupByQuery(
       String apiId,
       Instant from,
       Instant to,
       String field,
       Integer size,
       String order
   ) implements AnalyticsQuery {
       @Override
       public QueryType type() {
           return QueryType.GROUP_BY;
       }
   }
   ```

5. **DateHistoQuery.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/DateHistoQuery.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.model;

   import java.time.Instant;

   public record DateHistoQuery(
       String apiId,
       Instant from,
       Instant to,
       String field,
       Long interval
   ) implements AnalyticsQuery {
       @Override
       public QueryType type() {
           return QueryType.DATE_HISTO;
       }
   }
   ```

**Reference Patterns:**
- Use Java 21 `sealed interface` and `record` types
- Similar to existing query models in `io.gravitee.apim.core.analytics.model`

#### Subtask 2.1.2: Create Response Models

**Files to Create:**

1. **AnalyticsResponse.java** (sealed interface)
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/AnalyticsResponse.java`
   - **Content:**
   ```java
   package io.gravitee.rest.api.model.v4.analytics;

   public sealed interface AnalyticsResponse
       permits CountResponse, StatsResponse, GroupByResponse, DateHistoResponse {
       String type();
   }
   ```

2. **CountResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/CountResponse.java`
   - **Content:**
   ```java
   package io.gravitee.rest.api.model.v4.analytics;

   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   @Builder
   public class CountResponse implements AnalyticsResponse {
       private String type = "COUNT";
       private Long count;
   }
   ```

3. **StatsResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/StatsResponse.java`
   - **Content:**
   ```java
   package io.gravitee.rest.api.model.v4.analytics;

   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   @Builder
   public class StatsResponse implements AnalyticsResponse {
       private String type = "STATS";
       private Long count;
       private Double min;
       private Double max;
       private Double avg;
       private Double sum;
   }
   ```

4. **GroupByResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/GroupByResponse.java`
   - **Content:**
   ```java
   package io.gravitee.rest.api.model.v4.analytics;

   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import java.util.Map;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   @Builder
   public class GroupByResponse implements AnalyticsResponse {
       private String type = "GROUP_BY";
       private Map<String, Long> values;
       private Map<String, Metadata> metadata;

       @Data
       @NoArgsConstructor
       @AllArgsConstructor
       @Builder
       public static class Metadata {
           private String name;
       }
   }
   ```

5. **DateHistoResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/DateHistoResponse.java`
   - **Content:**
   ```java
   package io.gravitee.rest.api.model.v4.analytics;

   import lombok.AllArgsConstructor;
   import lombok.Builder;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import java.util.List;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   @Builder
   public class DateHistoResponse implements AnalyticsResponse {
       private String type = "DATE_HISTO";
       private List<Long> timestamp;
       private List<FieldBucket> values;

       @Data
       @NoArgsConstructor
       @AllArgsConstructor
       @Builder
       public static class FieldBucket {
           private String field;
           private List<Long> buckets;
           private Metadata metadata;
       }

       @Data
       @NoArgsConstructor
       @AllArgsConstructor
       @Builder
       public static class Metadata {
           private String name;
       }
   }
   ```

**Reference Patterns:**
- Existing: `RequestsCount.java`, `ResponseStatusRanges.java`
- Use Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

#### Subtask 2.1.3: Run Tests (GREEN)

- Verify all tests pass
- Verify serialization/deserialization works
- Verify Lombok builders work correctly

---

