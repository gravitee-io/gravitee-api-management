# V4 API Analytics Dashboard - User Stories & Implementation Guide (Refined)

This document provides a detailed breakdown of each user story into actionable subtasks, including exact file paths, test requirements, reference patterns, and dependencies.

**Document Version**: 2.0 (Refined after critical review)

---

## Table of Contents

- [Backend Stories](#backend-stories)
  - [Story 1A: Analytics Domain Models](#story-1a-analytics-domain-models)
  - [Story 1B: REST Endpoint Skeleton with Authorization](#story-1b-rest-endpoint-skeleton-with-authorization)
  - [Story 1C: Use Case Foundation](#story-1c-use-case-foundation)
  - [Story 2: COUNT Query Type Implementation](#story-2-count-query-type-implementation)
  - [Story 2.5: Elasticsearch Error Handling](#story-25-elasticsearch-error-handling)
  - [Story 3: STATS Query Type Implementation](#story-3-stats-query-type-implementation)
  - [Story 4: GROUP_BY Query Type Implementation](#story-4-group_by-query-type-implementation)
  - [Story 5: DATE_HISTO Query Type Implementation](#story-5-date_histo-query-type-implementation)
  - [Story 6.5: Performance Validation & Timeouts](#story-65-performance-validation--timeouts)
- [Frontend Stories](#frontend-stories)
  - [Story 7A: Analytics Service - COUNT Support](#story-7a-analytics-service---count-support)
  - [Story 7B: Analytics Service - STATS Support](#story-7b-analytics-service---stats-support)
  - [Story 7C: Analytics Service - GROUP_BY Support](#story-7c-analytics-service---group_by-support)
  - [Story 7D: Analytics Service - DATE_HISTO Support](#story-7d-analytics-service---date_histo-support)
  - [Story 7.5: Dashboard Route & Navigation](#story-75-dashboard-route--navigation)
  - [Story 8: Stats Cards Widget with State Handling](#story-8-stats-cards-widget-with-state-handling)
  - [Story 9: HTTP Status Pie Chart Widget](#story-9-http-status-pie-chart-widget)
  - [Story 10: Verify Existing Line Charts](#story-10-verify-existing-line-charts)
  - [Story 12: Integration & Verification](#story-12-integration--verification)
  - [Story 15: E2E Dashboard Tests](#story-15-e2e-dashboard-tests)
- [Implementation Order](#implementation-order)
- [Refinement Notes](#refinement-notes)

---

## Backend Stories

### Story 1A: Analytics Domain Models

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

#### Subtask 1A.0: Write Acceptance Tests (RED)

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

#### Subtask 1A.1: Create Core Query Models

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

3. **StatsQuery.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/StatsQuery.java`

4. **GroupByQuery.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/GroupByQuery.java`

5. **DateHistoQuery.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/DateHistoQuery.java`

**Reference Patterns:**
- Use Java 21 `sealed interface` and `record` types
- Similar to existing query models in `io.gravitee.apim.core.analytics.model`

#### Subtask 1A.2: Create Response Models

**Files to Create:**

1. **AnalyticsResponse.java** (sealed interface)
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/AnalyticsResponse.java`

2. **CountResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/CountResponse.java`

3. **StatsResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/StatsResponse.java`

4. **GroupByResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/GroupByResponse.java`

5. **DateHistoResponse.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/v4/analytics/DateHistoResponse.java`

**Reference Patterns:**
- Existing: `RequestsCount.java`, `ResponseStatusRanges.java`
- Use Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@SuperBuilder`

#### Subtask 1A.3: Run Tests (GREEN)

- Verify all tests pass
- Verify serialization/deserialization works
- Verify Lombok builders work correctly

---

### Story 1B: REST Endpoint Skeleton with Authorization

**Title:** Create REST endpoint with parameter validation and authorization

**Description:**
As a platform developer, I want a REST endpoint skeleton with authorization and validation so that I have a secure foundation for implementing query types.

**Acceptance Criteria:**
- [ ] Endpoint exists at `GET /v2/apis/{apiId}/analytics`
- [ ] Returns 400 for invalid `type` parameter
- [ ] Returns 400 when `from` is after `to`
- [ ] Returns 400 when time range exceeds 90 days
- [ ] Returns 403 when user lacks `API_ANALYTICS:READ` permission
- [ ] Returns 501 Not Implemented for all query types (temporary)
- [ ] `@Permissions` annotation enforces authorization
- [ ] OpenAPI specification exists and validates
- [ ] Clear error messages for all validation failures

**Layer:** Backend
**Complexity:** S (2 days)
**Dependencies:** Story 1A

#### Subtask 1B.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **ApiAnalyticsResourceTest.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/test/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResourceTest.java`
   - **Test Cases:**
     - `shouldReturn400WhenTypeIsInvalid()`
     - `shouldReturn400WhenTypeIsMissing()`
     - `shouldReturn400WhenFromIsMissing()`
     - `shouldReturn400WhenToIsMissing()`
     - `shouldReturn400WhenFromIsAfterTo()`
     - `shouldReturn400WhenRangeExceeds90Days()`
     - `shouldAccept90DayRange()` (boundary test)
     - `shouldReturn403WhenUserLacksPermission()`
     - `shouldReturn501ForCountType()` (temporary)
     - `shouldIncludeContentTypeJsonHeader()`

**Reference Patterns:**
- Existing: `ApiAnalyticsResourceTest.java` (same directory)
- Extend `ApiResourceTest` base class
- Use JAX-RS test client

#### Subtask 1B.1: Create REST Resource Method

**Files to Modify:**

1. **ApiAnalyticsResource.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`
   - **Action:** Add new method
   ```java
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
   public Response getAnalytics(
       @PathParam("apiId") @NotNull String apiId,
       @QueryParam("type") @NotNull String type,
       @QueryParam("from") @NotNull Long from,
       @QueryParam("to") @NotNull Long to,
       @QueryParam("field") String field,
       @QueryParam("interval") Long interval,
       @QueryParam("size") Integer size,
       @QueryParam("order") String order
   ) {
       // Validate type enum
       if (!Arrays.asList("COUNT", "STATS", "GROUP_BY", "DATE_HISTO").contains(type)) {
           return Response.status(400)
               .entity(Map.of("error", "Invalid type parameter. Must be one of: COUNT, STATS, GROUP_BY, DATE_HISTO"))
               .build();
       }

       // Validate time range
       if (from >= to) {
           return Response.status(400)
               .entity(Map.of("error", "Start time must be before end time"))
               .build();
       }

       long rangeMs = to - from;
       long maxRangeMs = 90L * 24 * 60 * 60 * 1000; // 90 days
       if (rangeMs > maxRangeMs) {
           return Response.status(400)
               .entity(Map.of("error", "Time range cannot exceed 90 days. Please select a shorter period."))
               .build();
       }

       // Temporary: Return 501 Not Implemented
       return Response.status(501)
           .entity(Map.of("message", "Query type " + type + " not yet implemented"))
           .build();
   }
   ```

**Reference Patterns:**
- Existing methods in same file
- Use `@Permissions` annotation for authorization
- Use JAX-RS annotations: `@GET`, `@Produces`, `@PathParam`, `@QueryParam`

#### Subtask 1B.2: Create OpenAPI Specification

**Files to Create:**

1. **analytics-api.yaml**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/resources/openapi/analytics-api.yaml`
   - **Content:**
   ```yaml
   openapi: 3.0.3
   info:
     title: Analytics API
     version: 2.0.0
     description: Unified analytics endpoint for V4 API metrics

   paths:
     /v2/apis/{apiId}/analytics:
       get:
         summary: Get analytics data for an API
         operationId: getAnalytics
         tags:
           - Analytics
         security:
           - bearerAuth: []
         parameters:
           - name: apiId
             in: path
             required: true
             schema:
               type: string
           - name: type
             in: query
             required: true
             schema:
               type: string
               enum: [COUNT, STATS, GROUP_BY, DATE_HISTO]
           - name: from
             in: query
             required: true
             schema:
               type: integer
               format: int64
               description: Start timestamp in milliseconds
           - name: to
             in: query
             required: true
             schema:
               type: integer
               format: int64
               description: End timestamp in milliseconds
           - name: field
             in: query
             required: false
             schema:
               type: string
               description: Field name (required for STATS, GROUP_BY, DATE_HISTO)
           - name: interval
             in: query
             required: false
             schema:
               type: integer
               format: int64
               description: Interval in milliseconds (required for DATE_HISTO)
           - name: size
             in: query
             required: false
             schema:
               type: integer
               minimum: 1
               maximum: 100
               default: 10
           - name: order
             in: query
             required: false
             schema:
               type: string
               enum: [asc, desc]
               default: desc

         responses:
           '200':
             description: Successful response
             content:
               application/json:
                 schema:
                   oneOf:
                     - $ref: '#/components/schemas/CountResponse'
                     - $ref: '#/components/schemas/StatsResponse'
                     - $ref: '#/components/schemas/GroupByResponse'
                     - $ref: '#/components/schemas/DateHistoResponse'
           '400':
             description: Bad request (invalid parameters)
           '403':
             description: Forbidden (missing API_ANALYTICS:READ permission)
           '404':
             description: API not found
           '501':
             description: Not implemented (query type not yet supported)

   components:
     schemas:
       CountResponse:
         type: object
         required: [type, count]
         properties:
           type:
             type: string
             enum: [COUNT]
           count:
             type: integer
             format: int64

       StatsResponse:
         type: object
         required: [type, count]
         properties:
           type:
             type: string
             enum: [STATS]
           count:
             type: integer
             format: int64
           min:
             type: number
             format: double
             nullable: true
           max:
             type: number
             format: double
             nullable: true
           avg:
             type: number
             format: double
             nullable: true
           sum:
             type: number
             format: double
             nullable: true

       GroupByResponse:
         type: object
         required: [type, values, metadata]
         properties:
           type:
             type: string
             enum: [GROUP_BY]
           values:
             type: object
             additionalProperties:
               type: integer
               format: int64
           metadata:
             type: object
             additionalProperties:
               type: object
               properties:
                 name:
                   type: string

       DateHistoResponse:
         type: object
         required: [type, timestamp, values]
         properties:
           type:
             type: string
             enum: [DATE_HISTO]
           timestamp:
             type: array
             items:
               type: integer
               format: int64
           values:
             type: array
             items:
               type: object
               properties:
                 field:
                   type: string
                 buckets:
                   type: array
                   items:
                     type: integer
                     format: int64
                 metadata:
                   type: object
                   properties:
                     name:
                       type: string
   ```

**Reference Patterns:**
- Look for existing OpenAPI specs in `gravitee-apim-rest-api-management-v2-rest`
- Use `oneOf` for discriminated union responses
- OpenAPI 3.0 specification: https://swagger.io/specification/

#### Subtask 1B.3: Run Tests (GREEN)

- Verify all validation tests pass
- Verify 403 response for missing permission
- Verify 501 response for all query types
- Verify OpenAPI spec validates with swagger-cli

---

### Story 1C: Use Case Foundation

**Title:** Create use case with API validation

**Description:**
As a platform developer, I want a use case that validates API requirements so that all query implementations inherit consistent validation.

**Acceptance Criteria:**
- [ ] Use case validates API exists (returns 404 if not)
- [ ] Use case validates API is V4 (returns 400 if V2)
- [ ] Use case validates API is not TCP proxy (returns 400 if TCP)
- [ ] Use case validates API belongs to user's environment (multi-tenancy)
- [ ] Use case returns 501 for all query types (temporary)
- [ ] Mapper interface exists for future response mapping

**Layer:** Backend
**Complexity:** S (2 days)
**Dependencies:** Story 1B

#### Subtask 1C.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **SearchUnifiedAnalyticsUseCaseTest.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/analytics/use_case/SearchUnifiedAnalyticsUseCaseTest.java`
   - **Test Cases:**
     - `shouldThrowWhenAPINotFound()`
     - `shouldThrowWhenAPIIsV2()`
     - `shouldThrowWhenAPIIsTCPProxy()`
     - `shouldThrowWhenEnvironmentMismatch()`
     - `shouldPassValidationForValidV4API()`
     - `shouldReturn501ForCountQuery()` (temporary)

2. **ApiAnalyticsValidatorTest.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/test/java/io/gravitee/apim/core/analytics/validator/ApiAnalyticsValidatorTest.java`
   - **Test Cases:**
     - `shouldPassForValidV4API()`
     - `shouldThrowWhenAPIIsNull()`
     - `shouldThrowWhenEnvironmentMismatch()`
     - `shouldThrowWhenAPIIsV2()`
     - `shouldThrowWhenAPIIsTCPProxy()`

**Reference Patterns:**
- Existing: `SearchRequestsCountAnalyticsUseCaseTest.java`
- Mock `ApiQueryService` with Mockito

#### Subtask 1C.1: Create Validator

**Files to Create:**

1. **ApiAnalyticsValidator.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/validator/ApiAnalyticsValidator.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.validator;

   import io.gravitee.apim.core.api.model.Api;
   import io.gravitee.definition.model.v4.ApiType;
   import io.gravitee.definition.model.DefinitionVersion;

   public class ApiAnalyticsValidator {

       public static void validateApiRequirements(Api api, String environmentId) {
           if (api == null) {
               throw new IllegalArgumentException("API not found");
           }

           if (!environmentId.equals(api.getEnvironmentId())) {
               throw new SecurityException("API does not belong to the user's environment");
           }

           if (api.getDefinitionVersion() != DefinitionVersion.V4) {
               throw new IllegalArgumentException("Analytics are only available for V4 APIs");
           }

           if (api.getType() == ApiType.PROXY_TCP) {
               throw new IllegalArgumentException("Analytics are not available for TCP Proxy APIs");
           }
       }
   }
   ```

**Reference Patterns:**
- Similar validation in existing use cases

#### Subtask 1C.2: Create Use Case

**Files to Create:**

1. **SearchUnifiedAnalyticsUseCase.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/use_case/SearchUnifiedAnalyticsUseCase.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.use_case;

   import io.gravitee.apim.core.UseCase;
   import io.gravitee.apim.core.analytics.model.AnalyticsQuery;
   import io.gravitee.apim.core.analytics.validator.ApiAnalyticsValidator;
   import io.gravitee.apim.core.api.query_service.ApiQueryService;
   import io.gravitee.rest.api.model.v4.analytics.AnalyticsResponse;
   import io.gravitee.rest.api.service.common.ExecutionContext;
   import java.util.Optional;

   @UseCase
   public class SearchUnifiedAnalyticsUseCase {

       private final ApiQueryService apiQueryService;

       public SearchUnifiedAnalyticsUseCase(ApiQueryService apiQueryService) {
           this.apiQueryService = apiQueryService;
       }

       public record Input(String apiId, String environmentId, AnalyticsQuery query) {}
       public record Output(Optional<AnalyticsResponse> response) {}

       public Output execute(ExecutionContext executionContext, Input input) {
           // Fetch and validate API
           var api = apiQueryService.findById(input.apiId())
               .orElseThrow(() -> new IllegalArgumentException("API not found: " + input.apiId()));

           ApiAnalyticsValidator.validateApiRequirements(api, input.environmentId());

           // Temporary: Return empty (501 handled at REST layer)
           return new Output(Optional.empty());
       }
   }
   ```

**Reference Patterns:**
- Existing: `SearchRequestsCountAnalyticsUseCase.java`

#### Subtask 1C.3: Create Mapper Interface

**Files to Modify:**

1. **ApiAnalyticsMapper.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/mapper/ApiAnalyticsMapper.java`
   - **Action:** Add placeholder mapping methods
   ```java
   @Mapper
   public interface ApiAnalyticsMapper {
       ApiAnalyticsMapper INSTANCE = Mappers.getMapper(ApiAnalyticsMapper.class);

       // Placeholder - will be implemented in Stories 2-5
       // AnalyticsResponse map(CountResponse countResponse);
       // AnalyticsResponse map(StatsResponse statsResponse);
       // etc.
   }
   ```

**Reference Patterns:**
- Existing mapper in same file
- Use MapStruct `@Mapper` annotation

#### Subtask 1C.4: Wire Use Case to REST Resource

**Files to Modify:**

1. **ApiAnalyticsResource.java**
   - **Action:** Replace 501 logic with use case call
   ```java
   // Inject use case
   @Inject
   private SearchUnifiedAnalyticsUseCase searchUnifiedAnalyticsUseCase;

   // In getAnalytics() method:
   try {
       var input = new SearchUnifiedAnalyticsUseCase.Input(
           apiId,
           getExecutionContext().getEnvironmentId(),
           buildQuery(type, from, to, field, interval, size, order)
       );

       var output = searchUnifiedAnalyticsUseCase.execute(getExecutionContext(), input);

       if (output.response().isEmpty()) {
           return Response.status(501)
               .entity(Map.of("message", "Query type not yet implemented"))
               .build();
       }

       return Response.ok(output.response().get()).build();

   } catch (IllegalArgumentException e) {
       return Response.status(400).entity(Map.of("error", e.getMessage())).build();
   } catch (SecurityException e) {
       return Response.status(403).entity(Map.of("error", e.getMessage())).build();
   }
   ```

#### Subtask 1C.5: Run Tests (GREEN)

- Verify all validation tests pass
- Verify 404, 400, 403 responses work correctly
- Verify use case is called from REST resource

---

### Story 2: COUNT Query Type Implementation

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
**Dependencies:** Story 1A, 1B, 1C

#### Subtask 2.0: Write Acceptance Tests (RED)

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

#### Subtask 2.1: Create Elasticsearch Query Adapter

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

#### Subtask 2.2: Create Elasticsearch Response Adapter

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

#### Subtask 2.3: Add Repository Method

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

#### Subtask 2.4: Add Query Service Method

**Files to Modify:**

1. **AnalyticsQueryService.java** (interface)
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/query_service/AnalyticsQueryService.java`
   - **Action:** Add method signature

2. **AnalyticsQueryServiceImpl.java** (implementation)
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/analytics/AnalyticsQueryServiceImpl.java`
   - **Action:** Implement method

**Reference Patterns:**
- Simple delegation pattern from service to repository

#### Subtask 2.5: Wire Use Case to Handle COUNT Type

**Files to Modify:**

1. **SearchUnifiedAnalyticsUseCase.java**
   - **Action:** Add COUNT query handling
   ```java
   public Output execute(ExecutionContext executionContext, Input input) {
       validateApiRequirements(input);

       return switch (input.query()) {
           case CountQuery countQuery -> {
               var result = analyticsQueryService.searchCount(countQuery);
               yield new Output(result.map(r -> (AnalyticsResponse) r));
           }
           default -> new Output(Optional.empty());
       };
   }
   ```

#### Subtask 2.6: Integration Test with Testcontainers

**Files to Create:**

1. **SearchCountAnalyticsIntegrationTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/SearchCountAnalyticsIntegrationTest.java`
   - **Test Cases:**
     - `shouldExecuteActualCountQuery()`
     - `shouldReturnCorrectCountWithTestData()`
     - `shouldReturnZeroForEmptyIndex()`
     - `shouldFilterByApiId()`
     - `shouldFilterByTimeRange()`

**Setup:**
```java
@Testcontainers
class SearchCountAnalyticsIntegrationTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
        "docker.elastic.co/elasticsearch/elasticsearch:8.11.0"
    );

    @BeforeAll
    static void setup() {
        // Create index
        // Insert test data (100 requests with varied timestamps and API IDs)
    }

    @Test
    void shouldExecuteActualCountQuery() {
        // Create real query
        // Execute against real Elasticsearch
        // Verify actual count matches test data
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

#### Subtask 2.7: Run Tests (GREEN)

- Verify all unit tests pass
- Verify integration test with Testcontainers passes
- Verify REST endpoint returns actual count

---

### Story 2.5: Elasticsearch Error Handling

**Title:** Handle Elasticsearch failures gracefully

**Description:**
As a platform operator, I want analytics queries to handle Elasticsearch failures gracefully so that users receive appropriate error messages instead of generic 500 errors.

**Acceptance Criteria:**
- [ ] Returns 503 Service Unavailable when ES is unreachable
- [ ] Returns 500 with error ID when ES returns query errors
- [ ] Logs ES errors with full request context (apiId, query, timestamp)
- [ ] Returns empty results (not error) when index doesn't exist
- [ ] Returns 504 Gateway Timeout if ES query exceeds 10 seconds
- [ ] Error messages are actionable for users

**Layer:** Backend
**Complexity:** S (2 days)
**Dependencies:** Story 2

#### Subtask 2.5.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **ElasticsearchErrorHandlingTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/ElasticsearchErrorHandlingTest.java`
   - **Test Cases:**
     - `shouldReturn503WhenESUnreachable()`
     - `shouldReturn500WhenESQueryFails()`
     - `shouldReturnEmptyWhenIndexMissing()`
     - `shouldReturn504WhenESTimeout()`
     - `shouldLogErrorsWithContext()`
     - `shouldIncludeErrorIdInResponse()`

**Reference Patterns:**
- Mock ES client to throw specific exceptions
- Verify error responses and logging

#### Subtask 2.5.1: Add Error Detection in Repository

**Files to Modify:**

1. **AnalyticsElasticsearchRepository.java**
   - **Action:** Add error handling and detection
   ```java
   @Override
   public Optional<CountResponse> searchCount(CountQuery query) {
       try {
           String esQuery = SearchCountAnalyticsQueryAdapter.adapt(query);
           String response = client.search(Type.V4_METRICS, esQuery);
           return Optional.of(SearchCountAnalyticsResponseAdapter.adapt(response));

       } catch (IndexNotFoundException e) {
           log.warn("Analytics index not found for API {}, returning empty result", query.apiId());
           return Optional.of(CountResponse.builder().type("COUNT").count(0L).build());

       } catch (ElasticsearchTimeoutException e) {
           log.error("Elasticsearch query timeout for API {}", query.apiId(), e);
           throw new AnalyticsTimeoutException("Query execution exceeded 10 seconds", e);

       } catch (ElasticsearchConnectionException e) {
           log.error("Elasticsearch connection failed for API {}", query.apiId(), e);
           throw new AnalyticsUnavailableException("Elasticsearch is unreachable", e);

       } catch (Exception e) {
           String errorId = UUID.randomUUID().toString();
           log.error("Elasticsearch query failed for API {} (errorId: {})", query.apiId(), errorId, e);
           throw new AnalyticsQueryException("Query execution failed. Error ID: " + errorId, e);
       }
   }
   ```

#### Subtask 2.5.2: Create Custom Exceptions

**Files to Create:**

1. **AnalyticsTimeoutException.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/exception/AnalyticsTimeoutException.java`

2. **AnalyticsUnavailableException.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/exception/AnalyticsUnavailableException.java`

3. **AnalyticsQueryException.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/exception/AnalyticsQueryException.java`

#### Subtask 2.5.3: Map Exceptions to HTTP Status Codes

**Files to Modify:**

1. **ApiAnalyticsResource.java**
   - **Action:** Add exception handling
   ```java
   try {
       var output = searchUnifiedAnalyticsUseCase.execute(getExecutionContext(), input);
       return Response.ok(output.response().get()).build();

   } catch (AnalyticsTimeoutException e) {
       return Response.status(504)
           .entity(Map.of("error", e.getMessage()))
           .build();

   } catch (AnalyticsUnavailableException e) {
       return Response.status(503)
           .entity(Map.of("error", "Analytics service is temporarily unavailable. Please try again later."))
           .build();

   } catch (AnalyticsQueryException e) {
       return Response.status(500)
           .entity(Map.of("error", e.getMessage()))
           .build();

   } catch (IllegalArgumentException e) {
       return Response.status(400).entity(Map.of("error", e.getMessage())).build();
   } catch (SecurityException e) {
       return Response.status(403).entity(Map.of("error", e.getMessage())).build();
   }
   ```

#### Subtask 2.5.4: Add ES Query Timeout Configuration

**Files to Modify:**

1. **application.yml** (or equivalent)
   - **Action:** Add configuration
   ```yaml
   elasticsearch:
     analytics:
       timeout: 10000  # 10 seconds in milliseconds
   ```

2. **ElasticsearchClient.java** (or equivalent)
   - **Action:** Configure timeout
   ```java
   RequestOptions options = RequestOptions.DEFAULT.toBuilder()
       .setRequestConfig(RequestConfig.custom()
           .setSocketTimeout(analyticsTimeout)
           .setConnectTimeout(5000)
           .build())
       .build();
   ```

#### Subtask 2.5.5: Run Tests (GREEN)

- Verify all error scenarios return correct status codes
- Verify errors are logged with context
- Verify timeout configuration works
- Verify empty result when index missing

---

### Story 3: STATS Query Type Implementation

**Title:** Implement STATS aggregation for min/max/avg/sum calculations

**Description:**
As an API publisher, I want to see statistical metrics (min, max, avg, sum) for numeric fields like response time so that I can identify performance trends and outliers.

**Acceptance Criteria:**
- [ ] Returns stats with min, max, avg, sum, count
- [ ] Supports fields: `gateway-latency-ms`, `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`
- [ ] Returns 400 for unsupported field
- [ ] Returns null values when no data exists
- [ ] Query uses ES stats aggregation
- [ ] Unit tests cover all supported fields
- [ ] Integration test verifies calculations match ES results

**Layer:** Backend
**Complexity:** M (4 days)
**Dependencies:** Story 1A, 1B, 1C, Story 2

#### Subtask 3.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **AnalyticsFieldValidatorTest.java**
2. **SearchStatsAnalyticsQueryAdapterTest.java**
3. **SearchStatsAnalyticsResponseAdapterTest.java**
4. **SearchStatsAnalyticsIntegrationTest.java**
5. **ApiAnalyticsResourceTest.java** (extend)

**Test Cases:**
- Field validation tests
- Stats aggregation building tests
- Response parsing tests
- Integration test with real ES

**Reference Patterns:**
- Similar to Story 2 test structure

#### Subtask 3.1: Create Field Validator

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
           "request-content-length"
       );

       private static final Set<String> GROUP_BY_FIELDS = Set.of(
           "status",
           "mapped-status",
           "application",
           "plan",
           "host",
           "uri"
       );

       public static boolean isValidStatsField(String field) {
           return STATS_FIELDS.contains(field);
       }

       public static boolean isValidGroupByField(String field) {
           return GROUP_BY_FIELDS.contains(field);
       }

       public static void validateStatsField(String field) {
           if (!isValidStatsField(field)) {
               throw new IllegalArgumentException(
                   "Unsupported field for STATS: " + field +
                   ". Supported fields: " + String.join(", ", STATS_FIELDS)
               );
           }
       }

       public static void validateGroupByField(String field) {
           if (!isValidGroupByField(field)) {
               throw new IllegalArgumentException(
                   "Unsupported field for GROUP_BY: " + field +
                   ". Supported fields: " + String.join(", ", GROUP_BY_FIELDS)
               );
           }
       }
   }
   ```

#### Subtask 3.2: Create Query and Response Adapters

**Files to Create:**

1. **SearchStatsAnalyticsQueryAdapter.java**
2. **SearchStatsAnalyticsResponseAdapter.java**

**Reference Patterns:**
- Similar structure to COUNT adapters
- Use ES `stats` aggregation

#### Subtask 3.3: Add Repository, Service, and Use Case Methods

**Files to Modify:**

1. **AnalyticsRepository.java** - Add `searchStats()`
2. **AnalyticsElasticsearchRepository.java** - Implement with error handling
3. **AnalyticsQueryService.java** - Add `searchStats()`
4. **AnalyticsQueryServiceImpl.java** - Implement
5. **SearchUnifiedAnalyticsUseCase.java** - Add STATS case
6. **ApiAnalyticsResource.java** - Add field validation for STATS

#### Subtask 3.4: Integration Test with Testcontainers

**Files to Create:**

1. **SearchStatsAnalyticsIntegrationTest.java**

**Test Cases:**
- Verify actual stats calculations (min, max, avg, sum)
- Verify all supported fields work
- Verify null handling when no data

#### Subtask 3.5: Run Tests (GREEN)

- Verify all tests pass
- Verify field validation works
- Verify stats calculations are correct

---

### Story 4: GROUP_BY Query Type Implementation

**Title:** Implement GROUP_BY aggregation for top-N value distribution

**Description:**
As an API publisher, I want to see the distribution of requests grouped by a specific field (e.g., status codes, applications) so that I can identify top consumers and error patterns.

**Acceptance Criteria:**
- [ ] Returns grouped values with counts and metadata
- [ ] Supports fields: `status`, `mapped-status`, `application`, `plan`, `host`, `uri`
- [ ] Defaults to top 10 results, respects `size` parameter (max 100)
- [ ] Returns empty values map when no data exists
- [ ] Supports `order=desc` (default) and `order=asc`
- [ ] Query uses ES terms aggregation
- [ ] Handles `.keyword` field suffix dynamically

**Layer:** Backend
**Complexity:** L (5 days)
**Dependencies:** Story 1A, 1B, 1C, Story 2

#### Subtask 4.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **SearchGroupByAnalyticsQueryAdapterTest.java**
   - Test cases for terms aggregation building
   - Test size defaults and limits
   - Test ordering
   - Test .keyword suffix handling

2. **SearchGroupByAnalyticsResponseAdapterTest.java**
   - Test bucket parsing
   - Test metadata building
   - Test empty results

3. **SearchGroupByAnalyticsIntegrationTest.java**
   - Test with real ES
   - Test all supported fields
   - Test top-N limiting

**Reference Patterns:**
- Similar to Story 2 and 3 test structure

#### Subtask 4.1: Create Query Adapter with Field Type Detection

**Files to Create:**

1. **SearchGroupByAnalyticsQueryAdapter.java**
   - Handle `.keyword` suffix based on field type
   - Implement size limiting (default 10, max 100)
   - Implement ordering (asc/desc)

**Reference Patterns:**
- Check `AnalyticsElasticsearchRepository` for field type checking logic

#### Subtask 4.2: Create Response Adapter

**Files to Create:**

1. **SearchGroupByAnalyticsResponseAdapter.java**
   - Parse ES bucket aggregations
   - Build values map and metadata map

#### Subtask 4.3: Add Repository Method with Field Type Checking

**Files to Modify:**

1. **AnalyticsElasticsearchRepository.java**
   - Check if field needs `.keyword` suffix via `client.getFieldTypes()`
   - Pass flag to adapter

**Reference Patterns:**
- Existing field type checking in `searchResponseStatusRanges()`

#### Subtask 4.4: Add Service, Use Case, and REST Validation

**Files to Modify:**

1. **AnalyticsQueryService.java** - Add `searchGroupBy()`
2. **AnalyticsQueryServiceImpl.java** - Implement
3. **SearchUnifiedAnalyticsUseCase.java** - Add GROUP_BY case
4. **ApiAnalyticsResource.java** - Add field validation for GROUP_BY

#### Subtask 4.5: Integration Test with Testcontainers

**Files to Create:**

1. **SearchGroupByAnalyticsIntegrationTest.java**

**Test Cases:**
- Verify top-N limiting
- Verify ordering
- Verify .keyword handling for text fields

#### Subtask 4.6: Run Tests (GREEN)

- Verify all tests pass
- Verify field type detection works
- Verify top-N limiting and ordering work

---

### Story 5: DATE_HISTO Query Type Implementation

**Title:** Implement DATE_HISTO aggregation for time-series data

**Description:**
As an API publisher, I want to see metrics over time in fixed intervals so that I can identify peak usage periods and performance degradation.

**Acceptance Criteria:**
- [ ] Response includes `timestamp` array and `values` array with nested buckets
- [ ] `interval` parameter is required (in milliseconds)
- [ ] Supports all fields from GROUP_BY (keyword) and STATS (numeric)
- [ ] Uses `min_doc_count: 0` to fill gaps
- [ ] Uses `extended_bounds` to ensure full time range coverage
- [ ] Returns empty buckets for periods with no data
- [ ] Handles both numeric fields (stats agg) and keyword fields (terms agg)

**Layer:** Backend
**Complexity:** L (6 days)
**Dependencies:** Story 3 (STATS), Story 4 (GROUP_BY)

#### Subtask 5.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **AnalyticsFieldTypeTest.java**
   - Test field type detection (NUMERIC vs KEYWORD)

2. **SearchDateHistoAnalyticsQueryAdapterTest.java**
   - Test date histogram with stats aggregation (numeric fields)
   - Test date histogram with terms aggregation (keyword fields)
   - Test fixed_interval usage
   - Test extended_bounds usage
   - Test min_doc_count: 0

3. **SearchDateHistoAnalyticsResponseAdapterTest.java**
   - Test parsing numeric field responses (stats in buckets)
   - Test parsing keyword field responses (terms in buckets)
   - Test empty bucket handling
   - Test timestamp ordering

4. **SearchDateHistoAnalyticsIntegrationTest.java**
   - Test with real ES for both field types
   - Test gap filling
   - Test bucket alignment

**Reference Patterns:**
- Double test coverage for dual paths (numeric + keyword)

#### Subtask 5.1: Create Field Type Enum

**Files to Create:**

1. **AnalyticsFieldType.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/AnalyticsFieldType.java`
   - **Content:**
   ```java
   package io.gravitee.apim.core.analytics.model;

   public enum AnalyticsFieldType {
       NUMERIC,    // Stats aggregation (avg, min, max, sum)
       KEYWORD;    // Terms aggregation

       public static AnalyticsFieldType fromField(String field) {
           return switch (field) {
               case "gateway-latency-ms",
                    "gateway-response-time-ms",
                    "endpoint-response-time-ms",
                    "request-content-length" -> NUMERIC;
               case "status",
                    "mapped-status",
                    "application",
                    "plan",
                    "host",
                    "uri" -> KEYWORD;
               default -> throw new IllegalArgumentException("Unknown field: " + field);
           };
       }
   }
   ```

#### Subtask 5.2: Create Query Adapter (Dual Path)

**Files to Create:**

1. **SearchDateHistoAnalyticsQueryAdapter.java**
   - Implement date histogram aggregation
   - Branch on field type:
     - NUMERIC → nested stats aggregation
     - KEYWORD → nested terms aggregation

**Structure:**
```java
public static String adapt(DateHistoQuery query, boolean isFieldKeyword) {
    AnalyticsFieldType fieldType = AnalyticsFieldType.fromField(query.field());

    Map<String, Object> nestedAgg;
    if (fieldType == AnalyticsFieldType.NUMERIC) {
        nestedAgg = buildStatsAggregation(query.field());
    } else {
        String field = isFieldKeyword ? query.field() + ".keyword" : query.field();
        nestedAgg = buildTermsAggregation(field);
    }

    // Build date histogram with nested agg
    return buildDateHistogramQuery(query, nestedAgg);
}
```

#### Subtask 5.3: Create Response Adapter (Dual Path)

**Files to Create:**

1. **SearchDateHistoAnalyticsResponseAdapter.java**
   - Parse date histogram buckets
   - Branch on field type to parse nested aggregations
   - Build timestamp array and values array

#### Subtask 5.4: Add Repository, Service, and Use Case Methods

**Files to Modify:**

1. **AnalyticsRepository.java** - Add `searchDateHisto()`
2. **AnalyticsElasticsearchRepository.java** - Implement with field type detection
3. **AnalyticsQueryService.java** - Add `searchDateHisto()`
4. **AnalyticsQueryServiceImpl.java** - Implement
5. **SearchUnifiedAnalyticsUseCase.java** - Add DATE_HISTO case
6. **ApiAnalyticsResource.java** - Add interval validation

#### Subtask 5.5: Integration Test with Testcontainers (Both Paths)

**Files to Create:**

1. **SearchDateHistoAnalyticsIntegrationTest.java**

**Test Cases:**
- Test with numeric field (gateway-response-time-ms)
- Test with keyword field (status)
- Verify gap filling works
- Verify extended_bounds works
- Verify both aggregation paths

#### Subtask 5.6: Run Tests (GREEN)

- Verify all tests pass for both field types
- Verify gap filling works
- Verify time alignment is correct

---

### Story 6.5: Performance Validation & Timeouts

**Title:** Validate query performance and add timeout protection

**Description:**
As a platform operator, I want analytics queries to have performance guarantees and timeout protection so that the system remains stable under load.

**Acceptance Criteria:**
- [ ] All queries complete in < 5 seconds for datasets up to 1M requests
- [ ] ES query timeout is configurable (default 10 seconds)
- [ ] Returns 504 Gateway Timeout if timeout exceeded
- [ ] Performance test runs in CI/CD
- [ ] Slow queries are logged with execution time

**Layer:** Backend
**Complexity:** M (5 days)
**Dependencies:** Story 2, 3, 4, 5

#### Subtask 6.5.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **AnalyticsPerformanceTest.java**
   - **Path:** `gravitee-apim-repository/gravitee-apim-repository-elasticsearch/src/test/java/io/gravitee/repository/elasticsearch/v4/analytics/AnalyticsPerformanceTest.java`
   - **Test Cases:**
     - `shouldCompleteCOUNTQueryIn5Seconds()`
     - `shouldCompleteSTATSQueryIn5Seconds()`
     - `shouldCompleteGROUPBYQueryIn5Seconds()`
     - `shouldCompleteDATEHISTOQueryIn5Seconds()`
     - `shouldTimeout10SecondsOnSlowQuery()`
     - `shouldLogSlowQueries()`

**Setup:**
- Use Testcontainers with 1M record dataset
- Measure query execution time
- Use @Timeout annotation

#### Subtask 6.5.1: Add Query Execution Time Logging

**Files to Modify:**

1. **AnalyticsElasticsearchRepository.java**
   - Add execution time measurement
   - Log slow queries (> 2 seconds)
   ```java
   long startTime = System.currentTimeMillis();
   try {
       String response = client.search(Type.V4_METRICS, esQuery);
       long duration = System.currentTimeMillis() - startTime;

       if (duration > 2000) {
           log.warn("Slow analytics query detected: {}ms for API {}", duration, query.apiId());
       }

       return Optional.of(adapter.adapt(response));
   } catch (Exception e) {
       long duration = System.currentTimeMillis() - startTime;
       log.error("Query failed after {}ms for API {}", duration, query.apiId(), e);
       throw e;
   }
   ```

#### Subtask 6.5.2: Add Timeout Configuration

**Files to Modify:**

1. **application.yml**
   ```yaml
   elasticsearch:
     analytics:
       timeout: 10000  # 10 seconds
       slowQueryThreshold: 2000  # Log queries > 2 seconds
   ```

2. **ElasticsearchClient.java** (or equivalent)
   - Configure request timeout

#### Subtask 6.5.3: Create Performance Test Dataset

**Files to Create:**

1. **test-data/analytics-1m-records.json**
   - Script to generate 1M test records
   - Varied timestamps, API IDs, status codes
   - Realistic response times

#### Subtask 6.5.4: Add CI/CD Performance Test

**Files to Modify:**

1. **pom.xml** (or build script)
   - Add performance test profile
   - Configure to run on specific branches or schedules

**Example:**
```xml
<profile>
  <id>performance-tests</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <includes>
            <include>**/*PerformanceTest.java</include>
          </includes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

#### Subtask 6.5.5: Run Tests (GREEN)

- Verify all queries meet < 5 second SLA
- Verify timeout protection works
- Verify slow query logging works

**Note:** Defer caching, circuit breakers, and heavy load testing (100 concurrent users) to post-MVP.

---

## Frontend Stories

### Story 7A: Analytics Service - COUNT Support

**Title:** Create TypeScript service with COUNT query support

**Description:**
As a frontend developer, I want a typed service method to fetch COUNT analytics so that I can display total request metrics.

**Acceptance Criteria:**
- [ ] TypeScript models for CountRequest and CountResponse exist
- [ ] Service method `getAnalytics()` supports COUNT requests
- [ ] Returns typed Observable<CountResponse>
- [ ] Includes HttpClient error handling
- [ ] Unit test mocks HttpClient and verifies request

**Layer:** Frontend
**Complexity:** S (2 days)
**Dependencies:** Story 2 (Backend COUNT endpoint)

#### Subtask 7A.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **api-analytics-v2.service.spec.ts** (extend)
   - **Path:** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.spec.ts`
   - **Test Cases:**
     - `shouldCallCorrectEndpointForCountRequest()`
     - `shouldIncludeAllQueryParameters()`
     - `shouldReturnTypedCountResponse()`
     - `shouldHandleHttpErrors()`
     - `shouldThrowForUnsupportedTypes()`

**Reference Patterns:**
- Use `HttpTestingController` from `@angular/common/http/testing`
- Use fixture data for mock responses

#### Subtask 7A.1: Create TypeScript Models for COUNT

**Files to Create:**

1. **analyticsRequest.ts**
   - **Path:** `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/analyticsRequest.ts`
   - **Content:**
   ```typescript
   export enum AnalyticsQueryType {
     COUNT = 'COUNT',
     STATS = 'STATS',
     GROUP_BY = 'GROUP_BY',
     DATE_HISTO = 'DATE_HISTO',
   }

   export interface BaseAnalyticsRequest {
     from: number;
     to: number;
   }

   export interface CountRequest extends BaseAnalyticsRequest {
     type: AnalyticsQueryType.COUNT;
   }

   // Placeholder for future types
   export type AnalyticsRequest = CountRequest; // Will expand in 7B, 7C, 7D
   ```

2. **analyticsResponse.ts**
   - **Path:** `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/analyticsResponse.ts`
   - **Content:**
   ```typescript
   export interface CountResponse {
     type: 'COUNT';
     count: number;
   }

   // Placeholder for future types
   export type AnalyticsResponse = CountResponse; // Will expand in 7B, 7C, 7D
   ```

3. **analyticsRequest.fixture.ts**
   - **Path:** `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/analyticsRequest.fixture.ts`

4. **analyticsResponse.fixture.ts**
   - **Path:** `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/analyticsResponse.fixture.ts`

**Reference Patterns:**
- Existing fixtures: `analyticsRequestsCount.fixture.ts`

#### Subtask 7A.2: Add getAnalytics() Method (COUNT Only)

**Files to Modify:**

1. **api-analytics-v2.service.ts**
   - **Path:** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts`
   - **Action:** Add new method
   ```typescript
   import { AnalyticsRequest, AnalyticsResponse, AnalyticsQueryType } from '../entities/management-api-v2/analytics';

   getAnalytics(apiId: string, request: AnalyticsRequest): Observable<AnalyticsResponse> {
     // Only COUNT is supported in this story
     if (request.type !== AnalyticsQueryType.COUNT) {
       return throwError(() => new Error(`Query type ${request.type} not yet supported`));
     }

     return this.timeRangeFilter().pipe(
       switchMap(() => {
         const params = new HttpParams()
           .set('type', request.type)
           .set('from', request.from.toString())
           .set('to', request.to.toString());

         return this.http.get<AnalyticsResponse>(
           `${this.v2BaseURL}/apis/${apiId}/analytics`,
           { params }
         );
       })
     );
   }
   ```

**Reference Patterns:**
- Existing methods in same service
- Use `switchMap` to coordinate with time range filter

#### Subtask 7A.3: Run Tests (GREEN)

- Verify COUNT requests work
- Verify unsupported types throw error
- Verify HTTP parameters are correct

**Note:** Defer convenience methods (getCount(), etc.) - components will call `getAnalytics()` directly.

---

### Story 7B: Analytics Service - STATS Support

**Title:** Extend service to support STATS queries

**Description:**
As a frontend developer, I want the analytics service to support STATS queries so that I can display statistical metrics.

**Acceptance Criteria:**
- [ ] StatsRequest and StatsResponse types exist
- [ ] `getAnalytics()` handles STATS type
- [ ] Field parameter included in HTTP request
- [ ] Unit test verifies STATS requests

**Layer:** Frontend
**Complexity:** S (2 days)
**Dependencies:** Story 3 (Backend STATS endpoint), Story 7A

#### Subtask 7B.0: Write Acceptance Tests (RED)

**Files to Modify:**

1. **api-analytics-v2.service.spec.ts**
   - **Add Test Cases:**
     - `shouldCallCorrectEndpointForStatsRequest()`
     - `shouldIncludeFieldParameter()`
     - `shouldReturnTypedStatsResponse()`

#### Subtask 7B.1: Add STATS Models

**Files to Modify:**

1. **analyticsRequest.ts**
   - Add `StatsRequest` interface
   - Update `AnalyticsRequest` type union

2. **analyticsResponse.ts**
   - Add `StatsResponse` interface
   - Update `AnalyticsResponse` type union

3. **analyticsResponse.fixture.ts**
   - Add `fakeStatsResponse()` function

#### Subtask 7B.2: Extend getAnalytics() Method

**Files to Modify:**

1. **api-analytics-v2.service.ts**
   - Remove `throwError` for STATS type
   - Add field parameter handling
   ```typescript
   let params = new HttpParams()
     .set('type', request.type)
     .set('from', request.from.toString())
     .set('to', request.to.toString());

   if (request.type === AnalyticsQueryType.STATS) {
     params = params.set('field', (request as StatsRequest).field);
   }
   ```

#### Subtask 7B.3: Run Tests (GREEN)

- Verify STATS requests work
- Verify field parameter is included

---

### Story 7C: Analytics Service - GROUP_BY Support

**Title:** Extend service to support GROUP_BY queries

**Description:**
As a frontend developer, I want the analytics service to support GROUP_BY queries so that I can display value distributions.

**Acceptance Criteria:**
- [ ] GroupByRequest and GroupByResponse types exist
- [ ] `getAnalytics()` handles GROUP_BY type
- [ ] Field, size, and order parameters included in HTTP request
- [ ] Unit test verifies GROUP_BY requests

**Layer:** Frontend
**Complexity:** S (2 days)
**Dependencies:** Story 4 (Backend GROUP_BY endpoint), Story 7B

#### Subtask 7C.0: Write Acceptance Tests (RED)

#### Subtask 7C.1: Add GROUP_BY Models

#### Subtask 7C.2: Extend getAnalytics() Method

```typescript
if (request.type === AnalyticsQueryType.GROUP_BY) {
  params = params.set('field', (request as GroupByRequest).field);
  if (request.size) {
    params = params.set('size', request.size.toString());
  }
  if (request.order) {
    params = params.set('order', request.order);
  }
}
```

#### Subtask 7C.3: Run Tests (GREEN)

---

### Story 7D: Analytics Service - DATE_HISTO Support

**Title:** Extend service to support DATE_HISTO queries

**Description:**
As a frontend developer, I want the analytics service to support DATE_HISTO queries so that I can display time-series charts.

**Acceptance Criteria:**
- [ ] DateHistoRequest and DateHistoResponse types exist
- [ ] `getAnalytics()` handles DATE_HISTO type
- [ ] Field and interval parameters included in HTTP request
- [ ] Unit test verifies DATE_HISTO requests

**Layer:** Frontend
**Complexity:** S (2 days)
**Dependencies:** Story 5 (Backend DATE_HISTO endpoint), Story 7C

#### Subtask 7D.0: Write Acceptance Tests (RED)

#### Subtask 7D.1: Add DATE_HISTO Models

#### Subtask 7D.2: Extend getAnalytics() Method

```typescript
if (request.type === AnalyticsQueryType.DATE_HISTO) {
  params = params.set('field', (request as DateHistoRequest).field);
  params = params.set('interval', (request as DateHistoRequest).interval.toString());
}
```

#### Subtask 7D.3: Run Tests (GREEN)

---

### Story 7.5: Dashboard Route & Navigation

**Title:** Add dashboard route and navigation menu item

**Description:**
As an API publisher, I want to access the analytics dashboard from the API menu so that I can view metrics without typing URLs manually.

**Acceptance Criteria:**
- [ ] Dashboard accessible at `/apis/:apiId/analytics/dashboard`
- [ ] Menu item "Analytics" appears in API sidebar (under "Traffic")
- [ ] Breadcrumb shows: APIs > {API Name} > Analytics > Dashboard
- [ ] Route guard checks API_ANALYTICS:READ permission
- [ ] Default timeframe is "Last 24 Hours"
- [ ] Menu item only visible when user has permission

**Layer:** Frontend
**Complexity:** S (2 days)
**Dependencies:** Story 7A (Service exists)

#### Subtask 7.5.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **api-analytics-routing.spec.ts**
   - **Test Cases:**
     - `shouldNavigateToDashboard()`
     - `shouldRedirectToLoginWhenUnauthorized()`
     - `shouldRedirectTo403WhenNoPermission()`

2. **api-sidebar.component.spec.ts** (extend)
   - **Test Cases:**
     - `shouldShowAnalyticsMenuItemWhenHasPermission()`
     - `shouldHideAnalyticsMenuItemWhenNoPermission()`

#### Subtask 7.5.1: Add Route

**Files to Modify:**

1. **api-routing.module.ts** (or equivalent)
   - **Path:** `gravitee-apim-console-webui/src/management/api/`
   - **Action:** Add route
   ```typescript
   {
     path: 'analytics/dashboard',
     component: ApiAnalyticsProxyComponent,
     canActivate: [PermissionGuard],
     data: {
       permissions: {
         anyOf: ['api-analytics-r']
       }
     }
   }
   ```

**Reference Patterns:**
- Existing routes in same file
- Use `PermissionGuard` for authorization

#### Subtask 7.5.2: Add Menu Item

**Files to Modify:**

1. **api-sidebar.component.ts**
   - **Action:** Add analytics menu item
   ```typescript
   {
     label: 'Analytics',
     icon: 'analytics',
     routerLink: './analytics/dashboard',
     permissions: ['api-analytics-r']
   }
   ```

2. **api-sidebar.component.html**
   - **Action:** Add menu item template
   ```html
   <a routerLink="./analytics/dashboard"
      *ngIf="hasAnalyticsPermission"
      matListItem>
     <mat-icon>analytics</mat-icon>
     <span>Analytics</span>
   </a>
   ```

**Reference Patterns:**
- Existing menu items in sidebar
- Use `*ngIf` for permission checks

#### Subtask 7.5.3: Add Breadcrumb

**Files to Modify:**

1. **api-analytics-proxy.component.ts**
   - **Action:** Set breadcrumb metadata
   ```typescript
   @Component({
     // ...
     providers: [
       {
         provide: 'breadcrumb',
         useValue: 'Dashboard'
       }
     ]
   })
   ```

#### Subtask 7.5.4: Run Tests (GREEN)

- Verify navigation works
- Verify permission checks work
- Verify menu item appears/disappears correctly

---

### Story 8: Stats Cards Widget with State Handling

**Title:** Display four stats cards with loading and error states

**Description:**
As an API publisher, I want to see key performance metrics at a glance so that I quickly understand my API's health.

**Acceptance Criteria:**
- [ ] Dashboard displays 4 stat cards: Total Requests, Avg Gateway Time, Avg Upstream Time, Avg Content Length
- [ ] Cards show loading skeleton while fetching
- [ ] Cards show "-" when data is null/undefined
- [ ] Cards show error snackbar on HTTP failure
- [ ] Cards show empty state when all stats are null
- [ ] Cards update when timeframe changes

**Layer:** Frontend
**Complexity:** M (3 days)
**Dependencies:** Story 7A, 7B (Service with COUNT and STATS)

**Note:** This story includes empty/error/loading state handling (formerly Story 11).

#### Subtask 8.0: Write Acceptance Tests (RED)

**Files to Modify:**

1. **api-analytics-proxy.component.spec.ts**
   - **Test Cases:**
     - `shouldDisplay4StatsCards()`
     - `shouldShowLoadingStateInitially()`
     - `shouldDisplayDashWhenDataIsNull()`
     - `shouldFormatNumbersCorrectly()`
     - `shouldConvertBytesToKB()`
     - `shouldShowEmptyStateWhenAllStatsNull()`
     - `shouldShowErrorSnackbarOnHttpFailure()`
     - `shouldDegradeGracefullyOnPartialFailure()`

**Reference Patterns:**
- Use `HttpTestingController` to mock API responses
- Use fixture data from `analyticsResponse.fixture.ts`

#### Subtask 8.1: Fetch 4 Stats in Parallel

**Files to Modify:**

1. **api-analytics-proxy.component.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
   - **Action:** Fetch stats
   ```typescript
   import { inject, signal } from '@angular/core';
   import { toSignal } from '@angular/core/rxjs-interop';
   import { combineLatest, of } from 'rxjs';
   import { map, startWith, catchError } from 'rxjs/operators';

   private readonly apiAnalyticsV2Service = inject(ApiAnalyticsV2Service);
   private readonly snackBarService = inject(SnackBarService);
   private readonly apiId = signal<string>(''); // Set from route params

   private readonly statsData$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
     switchMap((timeRange) => combineLatest([
       // COUNT query
       this.apiAnalyticsV2Service.getAnalytics(this.apiId(), {
         type: AnalyticsQueryType.COUNT,
         from: timeRange.from,
         to: timeRange.to
       }),
       // STATS queries (3)
       this.apiAnalyticsV2Service.getAnalytics(this.apiId(), {
         type: AnalyticsQueryType.STATS,
         field: 'gateway-response-time-ms',
         from: timeRange.from,
         to: timeRange.to
       }),
       this.apiAnalyticsV2Service.getAnalytics(this.apiId(), {
         type: AnalyticsQueryType.STATS,
         field: 'endpoint-response-time-ms',
         from: timeRange.from,
         to: timeRange.to
       }),
       this.apiAnalyticsV2Service.getAnalytics(this.apiId(), {
         type: AnalyticsQueryType.STATS,
         field: 'request-content-length',
         from: timeRange.from,
         to: timeRange.to
       })
     ])),
     map(([count, gatewayTime, endpointTime, contentLength]) =>
       this.mapToStatsCards(count as CountResponse, gatewayTime as StatsResponse,
                           endpointTime as StatsResponse, contentLength as StatsResponse)
     ),
     startWith({ isLoading: true, stats: [] }),
     catchError((error) => {
       this.snackBarService.error(`Failed to load analytics: ${error.message}`);
       return of({ isLoading: false, stats: [] });
     })
   );

   readonly statsCards = toSignal(this.statsData$, {
     initialValue: { isLoading: true, stats: [] }
   });

   private mapToStatsCards(
     count: CountResponse,
     gatewayTime: StatsResponse,
     endpointTime: StatsResponse,
     contentLength: StatsResponse
   ): { isLoading: boolean; stats: AnalyticsRequestStats[] } {
     return {
       isLoading: false,
       stats: [
         {
           label: 'Total Requests',
           value: count.count?.toString() ?? '-',
           unitLabel: '',
           isLoading: false,
         },
         {
           label: 'Avg Gateway Response Time',
           value: gatewayTime.avg?.toFixed(2) ?? '-',
           unitLabel: 'ms',
           isLoading: false,
         },
         {
           label: 'Avg Upstream Response Time',
           value: endpointTime.avg?.toFixed(2) ?? '-',
           unitLabel: 'ms',
           isLoading: false,
         },
         {
           label: 'Avg Content Length',
           value: contentLength.avg ? (contentLength.avg / 1024).toFixed(2) : '-',
           unitLabel: 'KB',
           isLoading: false,
         },
       ],
     };
   }
   ```

**Reference Patterns:**
- Use `toSignal()` to convert observables to signals
- Use `combineLatest` for parallel requests

#### Subtask 8.2: Update Template with State Handling

**Files to Modify:**

1. **api-analytics-proxy.component.html**
   - **Action:** Add stats cards with loading/empty/error states
   ```html
   <div class="analytics-dashboard">
     @if (statsCards().isLoading) {
       <gio-loader></gio-loader>
     } @else if (statsCards().stats.length === 0) {
       <gio-card-empty-state>
         <mat-icon>analytics</mat-icon>
         <p>No analytics data available for this time range</p>
         <p class="subtitle">Try selecting a different time period</p>
       </gio-card-empty-state>
     } @else {
       <api-analytics-request-stats
         [requestStats]="statsCards().stats"
         [isLoading]="statsCards().isLoading"
       />
     }
   </div>
   ```

**Reference Patterns:**
- Use `@if` control flow (Angular 17+)
- Use `GioLoaderModule` and `GioCardEmptyStateModule`

#### Subtask 8.3: Update Styles

**Files to Modify:**

1. **api-analytics-proxy.component.scss**
   - **Action:** Add responsive layout
   ```scss
   .analytics-dashboard {
     padding: 16px;
   }

   .stats-row {
     display: grid;
     grid-template-columns: repeat(4, 1fr);
     gap: 16px;
     margin-bottom: 16px;
   }

   @media (max-width: 1024px) {
     .stats-row {
       grid-template-columns: repeat(2, 1fr);
     }
   }

   @media (max-width: 768px) {
     .stats-row {
       grid-template-columns: 1fr;
     }
   }
   ```

#### Subtask 8.4: Run Tests (GREEN)

- Verify all 4 stats cards render
- Verify loading state works
- Verify empty state shows when all null
- Verify error snackbar shows on failure
- Verify numbers are formatted correctly

---

### Story 9: HTTP Status Pie Chart Widget

**Title:** Add pie chart showing HTTP status code distribution

**Description:**
As an API publisher, I want to see the breakdown of HTTP status codes in a pie chart so that I can quickly spot error patterns.

**Acceptance Criteria:**
- [ ] Dashboard displays HTTP status pie chart
- [ ] Chart fetches data via GROUP_BY query with `field=status`
- [ ] Chart displays top 10 status codes
- [ ] Colors follow convention (2xx=green, 3xx=blue, 4xx=orange, 5xx=red)
- [ ] Chart shows "No data" empty state when values map is empty
- [ ] Chart shows loading state while fetching
- [ ] Chart shows error snackbar on failure
- [ ] Chart updates when timeframe changes

**Layer:** Frontend
**Complexity:** M (4 days)
**Dependencies:** Story 7C (Service with GROUP_BY)

**Note:** This story includes empty/error/loading state handling (formerly Story 11).

#### Subtask 9.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **api-analytics-response-status-distribution.component.spec.ts**
   - **Test Cases:**
     - `shouldDisplayPieChart()`
     - `shouldShowLoadingState()`
     - `shouldShowEmptyStateWhenNoData()`
     - `shouldUseCorrectColorsFor2xxCodes()`
     - `shouldUseCorrectColorsFor4xxCodes()`
     - `shouldUseCorrectColorsFor5xxCodes()`
     - `shouldDisplayTop10StatusCodes()`
     - `shouldShowErrorSnackbarOnHttpFailure()`
     - `shouldUpdateWhenTimeframeChanges()`

#### Subtask 9.1: Create Status Distribution Component

**Files to Create:**

1. **api-analytics-response-status-distribution.component.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/components/api-analytics-response-status-distribution/api-analytics-response-status-distribution.component.ts`
   - **Content:**
   ```typescript
   import { Component, inject, input } from '@angular/core';
   import { toSignal } from '@angular/core/rxjs-interop';
   import { CommonModule } from '@angular/common';
   import { MatCardModule } from '@angular/material/card';
   import { GioChartPieModule, GioChartPieInput } from '@gravitee/ui-components/gio-chart-pie';
   import { GioLoaderModule, GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';
   import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
   import { AnalyticsQueryType, GroupByResponse } from '../../../../../entities/management-api-v2/analytics';
   import { map, startWith, catchError, switchMap } from 'rxjs/operators';
   import { of } from 'rxjs';

   @Component({
     selector: 'api-analytics-response-status-distribution',
     standalone: true,
     imports: [CommonModule, MatCardModule, GioChartPieModule, GioLoaderModule, GioCardEmptyStateModule],
     templateUrl: './api-analytics-response-status-distribution.component.html',
     styleUrl: './api-analytics-response-status-distribution.component.scss',
   })
   export class ApiAnalyticsResponseStatusDistributionComponent {
     private readonly apiAnalyticsV2Service = inject(ApiAnalyticsV2Service);
     private readonly snackBarService = inject(SnackBarService);

     apiId = input.required<string>();

     private readonly statusData$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
       switchMap((timeRange) =>
         this.apiAnalyticsV2Service.getAnalytics(this.apiId(), {
           type: AnalyticsQueryType.GROUP_BY,
           field: 'status',
           size: 10,
           order: 'desc',
           from: timeRange.from,
           to: timeRange.to
         })
       ),
       map((response: GroupByResponse) => this.mapToPieChartData(response)),
       startWith({ isLoading: true, data: [] }),
       catchError((error) => {
         this.snackBarService.error(`Failed to load status distribution: ${error.message}`);
         return of({ isLoading: false, data: [] });
       })
     );

     readonly chartData = toSignal(this.statusData$, {
       initialValue: { isLoading: true, data: [] }
     });

     private mapToPieChartData(response: GroupByResponse): { isLoading: boolean; data: GioChartPieInput[] } {
       if (!response.values || Object.keys(response.values).length === 0) {
         return { isLoading: false, data: [] };
       }

       const data = Object.entries(response.values).map(([status, count]) => ({
         label: status,
         value: count,
         color: this.getStatusColor(status),
       }));

       return { isLoading: false, data };
     }

     private getStatusColor(status: string): string {
       if (status.startsWith('2')) return '#30ab61'; // Green
       if (status.startsWith('3')) return '#365bd3'; // Blue
       if (status.startsWith('4')) return '#ff9f40'; // Orange
       if (status.startsWith('5')) return '#cf3942'; // Red
       return '#999999'; // Gray for 1xx or unknown
     }
   }
   ```

2. **api-analytics-response-status-distribution.component.html**
   ```html
   <mat-card>
     <mat-card-header>
       <mat-card-title>HTTP Status Distribution</mat-card-title>
     </mat-card-header>
     <mat-card-content>
       @if (chartData().isLoading) {
         <gio-loader></gio-loader>
       } @else if (chartData().data.length === 0) {
         <gio-card-empty-state>
           <mat-icon>pie_chart</mat-icon>
           <p>No status data available for this time range</p>
           <p class="subtitle">Try selecting a different time period</p>
         </gio-card-empty-state>
       } @else {
         <gio-chart-pie [data]="chartData().data"></gio-chart-pie>
       }
     </mat-card-content>
   </mat-card>
   ```

3. **api-analytics-response-status-distribution.component.scss**
   ```scss
   :host {
     display: block;
   }

   mat-card-content {
     min-height: 300px;
     display: flex;
     align-items: center;
     justify-content: center;
   }
   ```

#### Subtask 9.2: Add Component to Parent Layout

**Files to Modify:**

1. **api-analytics-proxy.component.ts**
   - Import new component

2. **api-analytics-proxy.component.html**
   - Add to layout
   ```html
   <!-- Row 3: Two-column layout -->
   <div class="analytics-row-3">
     <div class="left-column">
       <api-analytics-response-status-distribution [apiId]="apiId()" />
     </div>
     <div class="right-column">
       <api-analytics-response-status-overtime [apiId]="apiId()" />
     </div>
   </div>
   ```

3. **api-analytics-proxy.component.scss**
   - Add grid layout
   ```scss
   .analytics-row-3 {
     display: grid;
     grid-template-columns: 1fr 1fr;
     gap: 16px;
     margin-top: 16px;
   }

   @media (max-width: 768px) {
     .analytics-row-3 {
       grid-template-columns: 1fr;
     }
   }
   ```

#### Subtask 9.3: Create Component Harness

**Files to Create:**

1. **api-analytics-response-status-distribution.component.harness.ts**

#### Subtask 9.4: Run Tests (GREEN)

- Verify pie chart renders
- Verify colors are correct
- Verify empty state works
- Verify error handling works

---

### Story 10: Verify Existing Line Charts

**Title:** Verify existing line charts continue to work unchanged

**Description:**
As an API publisher, I want my existing line charts for response status over time and response time over time to continue working so that I don't lose historical trend visibility.

**Acceptance Criteria:**
- [ ] "Response Status Over Time" line chart renders with mock data
- [ ] "Response Time Over Time" line chart renders with mock data
- [ ] Both charts update when timeframe filter changes
- [ ] No console errors or warnings
- [ ] Charts appear in correct position in layout
- [ ] Tooltips and legends work correctly
- [ ] Visual snapshot tests pass

**Layer:** Frontend
**Complexity:** XS (1 day)
**Dependencies:** Story 7A (Service exists)

**Decision:** Keep existing endpoints (DO NOT MIGRATE to unified endpoint).

#### Subtask 10.0: Write Acceptance Tests (RED)

**Files to Modify:**

1. **api-analytics-proxy.component.spec.ts**
   - **Add Test Cases:**
     - `shouldRenderResponseStatusOvertimeChart()`
     - `shouldRenderResponseTimeOverTimeChart()`
     - `shouldUpdateChartsWhenTimeframeChanges()`
     - `shouldDisplayLegendAndTooltips()`
     - `shouldIncludeBothChartsInDashboardLayout()`

2. **Visual Regression Tests** (if framework exists)
   - `snapshot: response-status-overtime-chart.png`
   - `snapshot: response-time-over-time-chart.png`

#### Subtask 10.1: Verify Components Are Imported

**Files to Verify:**

1. **api-analytics-proxy.component.ts**
   - Verify imports:
     - `ApiAnalyticsResponseStatusOvertimeComponent`
     - `ApiAnalyticsResponseTimeOverTimeComponent`

**Existing Components (No Modifications):**
- `api-analytics-response-status-overtime.component.ts`
  - Data Source: `ApiAnalyticsV2Service.getResponseStatusOvertime()`
  - Chart Type: Multi-line chart (one line per status range: 2xx, 3xx, 4xx, 5xx)
- `api-analytics-response-time-over-time.component.ts`
  - Data Source: `ApiAnalyticsV2Service.getResponseTimeOverTime()`
  - Chart Type: Line chart with multiple series (gateway, endpoint)

#### Subtask 10.2: Verify Layout Includes Charts

**Files to Verify:**

1. **api-analytics-proxy.component.html**
   - Verify charts are in layout:
   ```html
   <!-- Row 3: Two-column layout -->
   <div class="analytics-row-3">
     <div class="left-column">
       <api-analytics-response-status-distribution [apiId]="apiId()" />
     </div>
     <div class="right-column">
       <api-analytics-response-status-overtime [apiId]="apiId()" />
     </div>
   </div>

   <!-- Row 4: Full-width -->
   <div class="analytics-row-4">
     <api-analytics-response-time-over-time [apiId]="apiId()" />
   </div>
   ```

#### Subtask 10.3: Run Tests (GREEN)

- Verify both charts render with mock data
- Verify charts update on timeframe change
- Verify no console errors
- Verify visual regression tests pass

---

### Story 12: Integration & Verification

**Title:** Full dashboard integration with coordinated widget refresh

**Description:**
As an API publisher, I want all analytics widgets to refresh together when I change the timeframe so that I have a consistent view of my API's performance.

**Acceptance Criteria:**
- [ ] All widgets (stats cards, pie chart, line charts) visible on page
- [ ] Changing timeframe filter triggers refresh of all widgets simultaneously
- [ ] Dashboard loads in < 2 seconds for APIs with < 100k requests
- [ ] No console errors or warnings
- [ ] Responsive layout works on desktop (1920x1080) and tablet (1024x768)
- [ ] All widgets share same time range state

**Layer:** Frontend
**Complexity:** M (3 days)
**Dependencies:** Story 8, 9, 10

#### Subtask 12.0: Write Acceptance Tests (RED)

**Files to Modify:**

1. **api-analytics-proxy.component.spec.ts**
   - **Add Test Cases:**
     - `shouldDisplayAllWidgets()`
     - `shouldRefreshAllWidgetsWhenTimeframeChanges()`
     - `shouldShareTimeRangeAcrossWidgets()`
     - `shouldNotHaveConsoleErrors()`
     - `shouldBeResponsive()`
     - `shouldLoadIn2SecondsOrLess()`

**Reference Patterns:**
- Mock `HttpTestingController` for all endpoints
- Verify multiple HTTP requests when timeframe changes

#### Subtask 12.1: Wire Timeframe Filter to All Widgets

**Files to Verify:**

All data streams should start with `timeRangeFilter().pipe(switchMap(...))`:

1. **api-analytics-proxy.component.ts**
   - Stats data stream uses `timeRangeFilter()`

2. **api-analytics-response-status-distribution.component.ts**
   - Pie chart data stream uses `timeRangeFilter()`

3. **Existing chart components**
   - Verify they use `timeRangeFilter()`

**Pattern:**
```typescript
private readonly data$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
  switchMap((timeRange) => /* fetch data with timeRange */),
  // ...
);
```

#### Subtask 12.2: Add Performance Optimization

**Files to Modify:**

1. **api-analytics-proxy.component.ts**
   - Add `shareReplay` to avoid duplicate requests
   ```typescript
   import { shareReplay } from 'rxjs/operators';

   private readonly statsData$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
     switchMap(...),
     map(...),
     startWith(...),
     catchError(...),
     shareReplay(1) // Cache latest emission
   );
   ```

**Reference Patterns:**
- Use `shareReplay(1)` for multi-cast observables

#### Subtask 12.3: Verify Responsive Layout

**Files to Verify:**

1. **api-analytics-proxy.component.scss**
   - Verify responsive breakpoints exist
   - Test at 1920x1080 (desktop)
   - Test at 1024x768 (tablet)
   - Test at 768px and below (mobile)

#### Subtask 12.4: Run Tests (GREEN)

- Verify all widgets visible
- Verify timeframe changes trigger all refreshes
- Verify no duplicate HTTP requests (shareReplay working)
- Verify responsive layout at different breakpoints

---

### Story 15: E2E Dashboard Tests

**Title:** End-to-end tests for complete user journey

**Description:**
As a platform developer, I want E2E tests that validate the complete user journey so that I can ensure the dashboard works correctly in a real browser.

**Acceptance Criteria:**
- [ ] User can login and navigate to analytics dashboard
- [ ] User can change timeframe and see all widgets refresh
- [ ] Dashboard displays correctly in Chrome
- [ ] Dashboard loads in < 3 seconds (measured in E2E test)
- [ ] No console errors or warnings in browser
- [ ] Basic accessibility audit passes (axe-core)
- [ ] Responsive layout works on mobile viewport (375px)

**Layer:** Frontend (E2E Test)
**Complexity:** M (5 days)
**Dependencies:** Story 12

**Note:** Defer cross-browser testing (Firefox, Safari) and visual regression (Percy/Chromatic) to post-MVP.

#### Subtask 15.0: Write E2E Test Suite

**Files to Create:**

1. **e2e/analytics-dashboard.spec.ts**
   - **Test Cases:**
     - `test('should navigate to analytics dashboard')`
     - `test('should display all widgets with data')`
     - `test('should refresh widgets when timeframe changes')`
     - `test('should show empty state when no data')`
     - `test('should load in under 3 seconds')`
     - `test('should be accessible (axe audit)')`
     - `test('should work on mobile viewport')`

**Example:**
```typescript
import { test, expect } from '@playwright/test'; // or Cypress

test.describe('Analytics Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    // Login and navigate to test API
    await page.goto('/apis/test-api-123/analytics/dashboard');
  });

  test('should navigate to analytics dashboard', async ({ page }) => {
    await expect(page.locator('api-analytics-proxy')).toBeVisible();
    await expect(page.locator('h1')).toContainText('Analytics');
  });

  test('should display all widgets with data', async ({ page }) => {
    // Wait for stats cards
    await expect(page.locator('api-analytics-request-stats')).toBeVisible();
    // Wait for pie chart
    await expect(page.locator('api-analytics-response-status-distribution')).toBeVisible();
    // Wait for line charts
    await expect(page.locator('api-analytics-response-status-overtime')).toBeVisible();
    await expect(page.locator('api-analytics-response-time-over-time')).toBeVisible();
  });

  test('should refresh widgets when timeframe changes', async ({ page }) => {
    // Change timeframe
    await page.click('[data-testid="timeframe-selector"]');
    await page.click('[data-testid="timeframe-7d"]');

    // Wait for refresh
    await expect(page.locator('gio-loader')).toBeVisible();
    await expect(page.locator('gio-loader')).not.toBeVisible();

    // Verify data updated (check if different from initial)
    const statsValue = await page.locator('.stat-value').first().textContent();
    expect(statsValue).not.toBe('-');
  });

  test('should load in under 3 seconds', async ({ page }) => {
    const startTime = Date.now();
    await page.goto('/apis/test-api-123/analytics/dashboard');
    await expect(page.locator('api-analytics-request-stats')).toBeVisible();
    const loadTime = Date.now() - startTime;
    expect(loadTime).toBeLessThan(3000);
  });

  test('should be accessible (axe audit)', async ({ page }) => {
    const { injectAxe, checkA11y } = require('axe-playwright');
    await injectAxe(page);
    await checkA11y(page, null, {
      detailedReport: true,
      detailedReportOptions: { html: true }
    });
  });

  test('should work on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await expect(page.locator('api-analytics-proxy')).toBeVisible();
    // Verify cards stack vertically
    const statsRow = page.locator('.stats-row');
    const gridColumns = await statsRow.evaluate((el) =>
      window.getComputedStyle(el).gridTemplateColumns
    );
    expect(gridColumns).toBe('1fr');
  });
});
```

#### Subtask 15.1: Setup Test Environment

**Files to Create:**

1. **e2e/support/analytics-helpers.ts**
   - Helper functions for common actions (login, navigate, change timeframe)

2. **e2e/fixtures/test-api-data.json**
   - Test API configuration
   - Known analytics data for assertions

3. **playwright.config.ts** (or cypress.config.ts)
   ```typescript
   export default defineConfig({
     testDir: './e2e',
     timeout: 30000,
     use: {
       baseURL: 'http://localhost:4200',
       trace: 'on-first-retry',
       video: 'retain-on-failure',
       screenshot: 'only-on-failure'
     },
     projects: [
       {
         name: 'Chrome',
         use: { ...devices['Desktop Chrome'] }
       }
     ]
   });
   ```

**Dependencies:**
```json
{
  "devDependencies": {
    "@playwright/test": "^1.40.0",
    "axe-playwright": "^1.2.3"
  }
}
```

#### Subtask 15.2: Add CI/CD Integration

**Files to Modify:**

1. **.github/workflows/e2e-tests.yml** (or similar)
   ```yaml
   name: E2E Tests
   on: [pull_request]

   jobs:
     e2e:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - uses: actions/setup-node@v3
         - run: npm ci
         - run: npm run build
         - run: npm run e2e
         - uses: actions/upload-artifact@v3
           if: failure()
           with:
             name: test-results
             path: test-results/
   ```

#### Subtask 15.3: Run Tests (GREEN)

- Verify all E2E tests pass
- Verify tests run in CI/CD
- Verify accessibility audit passes
- Verify mobile viewport works

---

## Implementation Order

### Sprint 1: Foundation + COUNT (2 weeks)

**Goal:** Establish foundation with complete TDD cycle for simplest query type

**Week 1:**
- ✅ Story 1A: Domain Models (M, 3 days) - Tests first
- ✅ Story 1B: REST Endpoint + Auth (S, 2 days) - Tests first

**Week 2:**
- ✅ Story 1C: Use Case Foundation (S, 2 days) - Tests first
- ✅ Story 2: COUNT Implementation (S, 3 days) - Tests first
- ✅ Story 2.5: ES Error Handling (S, 2 days) - Tests first

**Deliverable:** Working COUNT endpoint with auth and error handling

---

### Sprint 2: STATS + GROUP_BY + Frontend Start (2 weeks)

**Goal:** Add aggregations and unblock frontend

**Week 3:**
- ✅ Story 3: STATS Implementation (M, 4 days) - Tests first
- ✅ Story 7A: Frontend Service - COUNT (S, 2 days) - Tests first
- ✅ Story 8: Stats Cards (M, 3 days) starts - Tests first

**Week 4:**
- ✅ Story 4: GROUP_BY Implementation (L, 5 days) - Tests first
- ✅ Story 8: Stats Cards (continues)

**Deliverable:** COUNT and STATS working end-to-end, frontend displaying stats cards

---

### Sprint 3: DATE_HISTO + Dashboard UI (2 weeks)

**Goal:** Complete backend, build dashboard UI

**Week 5:**
- ✅ Story 5: DATE_HISTO Implementation (L, 6 days) - Tests first
- ✅ Story 7B: Frontend Service - STATS (S, 2 days) - Tests first

**Week 6:**
- ✅ Story 7C: Frontend Service - GROUP_BY (S, 2 days) - Tests first
- ✅ Story 7D: Frontend Service - DATE_HISTO (S, 2 days) - Tests first
- ✅ Story 7.5: Dashboard Navigation (S, 2 days) - Tests first
- ✅ Story 9: Pie Chart Widget (M, 4 days) starts - Tests first

**Deliverable:** All query types working, dashboard accessible with navigation

---

### Sprint 4: Integration + Performance + E2E (2 weeks)

**Goal:** Validate complete system, performance, and E2E

**Week 7:**
- ✅ Story 9: Pie Chart Widget (continues)
- ✅ Story 10: Verify Existing Charts (XS, 1 day) - Tests first
- ✅ Story 12: Full Integration (M, 3 days) - Tests first

**Week 8:**
- ✅ Story 6.5: Performance Validation (M, 5 days) - Tests first
- ✅ Story 15: E2E Tests (M, 5 days) - Tests first

**Deliverable:** Fully integrated dashboard with performance validation and E2E tests

---

## Refinement Notes

### Version 2.0 Changes (Post-Review)

This document was significantly refined based on a critical review focusing on TDD principles, story sizing, missing functionality, and dependency optimization.

#### Critical Changes Implemented

1. **TDD Compliance** ✅
   - **Change:** Eliminated Stories 13 and 14 (separate testing stories)
   - **Reason:** Tests must be written FIRST in each story (RED → GREEN → REFACTOR), not as separate stories at the end
   - **Impact:** Every story now has Subtask X.0 for writing failing tests before implementation
   - **Justification:** Aligns with TDD constraint-based discipline from user's rules (tdd.md). Tests as specs, not validation.

2. **Story 1 Decomposition** ✅
   - **Change:** Split Story 1 into 1A (Domain Models), 1B (REST Endpoint + Auth), 1C (Use Case)
   - **Reason:** Original Story 1 was too large (10 models + endpoint + use case + mapper = 3-4 stories of work)
   - **Impact:** Better parallelization, smaller PRs, clearer review scope
   - **New Complexities:** 1A (M), 1B (S), 1C (S) - more accurate sizing

3. **Authorization Moved to Foundation** ✅
   - **Change:** Eliminated Story 6, moved authorization to Story 1B
   - **Reason:** Security should never be retrofitted - must be foundational
   - **Impact:** All subsequent stories (2-5) inherit auth automatically, no rework needed
   - **Justification:** Follows "secure by default" principle, prevents security vulnerabilities

4. **Elasticsearch Error Handling Added** ✅
   - **Change:** Added Story 2.5 for ES error handling
   - **Reason:** Critical production gap - need to handle ES failures gracefully
   - **Impact:** Returns 503/500/504 based on failure type, logs with context
   - **Justification:** Distinguishes between "no data" (200) and "system error" (503) for better UX

5. **Time Range Validation Added** ✅
   - **Change:** Added Subtask 1B.4 for time range validation (max 90 days)
   - **Reason:** DoS protection - prevent users from querying 10 years of data and killing ES
   - **Impact:** Enforces max 90 days, clear error messages
   - **Justification:** Production-critical for system stability

6. **Story 7 Split for Parallelization** ✅
   - **Change:** Split Story 7 into 7A (COUNT), 7B (STATS), 7C (GROUP_BY), 7D (DATE_HISTO)
   - **Reason:** Original dependency blocked frontend for 3-4 weeks until ALL backend types done
   - **Impact:** Frontend can start Story 8 (Stats Cards) in Week 3 using COUNT (7A)
   - **Justification:** Enables parallel development, earlier integration, faster feedback

7. **Story 10 Rewritten with Specifics** ✅
   - **Change:** Rewrote Story 10 from vague "Preserve Existing Charts" to detailed verification
   - **Reason:** Original had no clear acceptance criteria, couldn't be tested
   - **Impact:** Now specifies exact components, data sources, visual regression tests
   - **Decision:** Keep existing endpoints (DO NOT MIGRATE) to reduce risk

8. **Story 11 Merged into Widget Stories** ✅
   - **Change:** Eliminated Story 11, merged empty/error/loading states into Stories 8 and 9
   - **Reason:** Empty states are quality attributes, not features - should be part of widget definition of done
   - **Impact:** Less overhead, more cohesive widget stories, still maintains quality
   - **Justification:** Standard Angular patterns, no new business logic

9. **Dashboard Navigation Added** ✅
   - **Change:** Added Story 7.5 for navigation and routing
   - **Reason:** Critical gap - dashboard without navigation is unusable
   - **Impact:** Menu item, route, breadcrumb, permission checks
   - **Justification:** Obvious requirement that was missing

10. **Performance Story Added** ✅
    - **Change:** Added Story 6.5 for performance validation and timeouts
    - **Reason:** No validation of "< 2 second" SLA mentioned in Story 12
    - **Impact:** ES query timeout (10s), performance tests, slow query logging
    - **Note:** Kept lighter than original proposal (deferred caching, circuit breakers to post-MVP)

11. **Integration Tests Added** ✅
    - **Change:** Added Subtask X.5 to each backend story (2-5) for Testcontainers integration tests
    - **Reason:** Critical gap - unit tests mock ES, need to test against real Elasticsearch
    - **Impact:** Validates ES query syntax, field mappings, actual behavior
    - **Justification:** Aligns with Java guidelines (java.md) emphasizing Testcontainers

12. **E2E Testing Story Added** ✅
    - **Change:** Added Story 15 for E2E tests with real browser
    - **Reason:** All testing was unit/component level, no validation of complete user journey
    - **Impact:** Full user journey in Chrome, basic accessibility, mobile viewport
    - **Note:** Kept lighter than original proposal (deferred cross-browser, visual regression to post-MVP)

13. **OpenAPI Made Mandatory** ✅
    - **Change:** Made OpenAPI spec mandatory in Story 1B (was optional)
    - **Reason:** API contracts should be explicit and versioned in modern API-first development
    - **Impact:** Swagger UI documentation, potential for TypeScript type generation
    - **Note:** Kept lighter than original proposal (basic docs, defer full codegen)

14. **Story 5 Complexity Updated** ✅
    - **Change:** Updated Story 5 complexity from M to L
    - **Reason:** DATE_HISTO implements TWO different aggregation strategies (numeric stats + keyword terms)
    - **Impact:** Added explicit subtasks for each path (5.2A/B, 5.3A/B), doubled test coverage
    - **Decision:** Kept as one story (not split) since they share date histogram logic

#### Changes Rejected

1. **Sealed Interfaces Retained** ❌
   - **Reviewer Suggested:** Use simple interfaces instead of sealed interfaces (over-engineering)
   - **Decision:** REJECTED - Keep sealed interfaces
   - **Justification:**
     - Java 21 is already the target version
     - Sealed interfaces provide valuable compile-time exhaustiveness checking for query type dispatching
     - Prevents accidental addition of unsupported query types
     - Not prevented from extension, just requires intentional change
     - Type safety > theoretical future extensibility for MVP

2. **Convenience Methods Deferred** ~
   - **Reviewer Suggested:** Remove convenience methods (getCount(), getStats(), etc.)
   - **Decision:** PARTIALLY ACCEPTED - Defer to usage patterns
   - **Justification:**
     - Direct calls to `getAnalytics()` provide same type safety
     - Wait for real usage patterns before adding abstractions (YAGNI)
     - Can add later if code reviews show boilerplate

#### Story Count Summary

**Original:** 14 stories (6 backend + 6 frontend + 2 testing)

**Refined:** 18 stories (9 backend + 8 frontend + 1 E2E)

**Changes:**
- Story 1 → 1A, 1B, 1C (+2)
- Story 6 eliminated (-1)
- Story 2.5 added (+1)
- Story 7 → 7A, 7B, 7C, 7D (+3)
- Story 7.5 added (+1)
- Story 6.5 added (+1)
- Story 11 eliminated (-1)
- Story 13, 14 eliminated (-2)
- Story 15 added (+1)

**Net:** +4 stories, but better granularity, parallelization, and TDD alignment

#### Key Benefits of Refinement

1. **TDD Compliance:** Tests first (RED) in every story, not as afterthought
2. **Security First:** Authorization in foundation (Story 1B), not retrofitted
3. **Parallel Development:** Frontend unblocked 4 weeks earlier (Story 7 split)
4. **Production Readiness:** ES error handling (2.5), timeouts (6.5), performance validation
5. **Quality Assurance:** Integration tests (Testcontainers) + E2E tests (real browser)
6. **Better Sizing:** More accurate complexity estimates (Story 1 split, Story 5 updated to L)
7. **Complete UX:** Navigation (7.5), empty/error states (merged into widgets)

#### Implementation Timeline Impact

**Original Timeline:** ~10 weeks
**Refined Timeline:** ~8 weeks (improved parallelization despite more stories)

**Improvements:**
- Frontend starts Week 3 (was Week 6)
- Backend and frontend work in parallel (was serialized)
- Earlier integration and feedback
- Better risk distribution

---

**Document Status:** Ready for implementation ✅
**Review Version:** 2.0 (Post-Critical Review)
**Last Updated:** 2026-03-03
