### Story 2.3: Use Case Foundation

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
**Dependencies:** Story 2.2

#### Subtask 2.3.0: Write Acceptance Tests (RED)

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

#### Subtask 2.3.1: Create Validator

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

#### Subtask 2.3.2: Create Use Case

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

#### Subtask 2.3.3: Create Mapper Interface

**Files to Create:**

1. **ApiAnalyticsMapper.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/mapper/ApiAnalyticsMapper.java`
   - **Content:**
   ```java
   package io.gravitee.rest.api.management.v2.rest.mapper;

   import org.mapstruct.Mapper;
   import org.mapstruct.factory.Mappers;

   @Mapper
   public interface ApiAnalyticsMapper {
       ApiAnalyticsMapper INSTANCE = Mappers.getMapper(ApiAnalyticsMapper.class);

       // Placeholder - will be implemented in upcoming stories
       // AnalyticsResponse map(CountResponse countResponse);
       // AnalyticsResponse map(StatsResponse statsResponse);
       // etc.
   }
   ```

**Reference Patterns:**
- Existing mapper in same package
- Use MapStruct `@Mapper` annotation

#### Subtask 2.3.4: Wire Use Case to REST Resource

**Files to Modify:**

1. **ApiAnalyticsResource.java**
   - **Action:** Replace 501 logic with use case call
   ```java
   @Inject
   private SearchUnifiedAnalyticsUseCase searchUnifiedAnalyticsUseCase;

   // In getAnalytics() method:
   try {
       var query = buildQuery(type, from, to, field, interval, size, order);
       var input = new SearchUnifiedAnalyticsUseCase.Input(
           apiId,
           getExecutionContext().getEnvironmentId(),
           query
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

2. **Add buildQuery() helper method:**
   ```java
   private AnalyticsQuery buildQuery(String type, Long from, Long to,
                                      String field, Long interval, Integer size, String order) {
       Instant fromInstant = Instant.ofEpochMilli(from);
       Instant toInstant = Instant.ofEpochMilli(to);
       String apiId = // extract from path

       return switch (type) {
           case "COUNT" -> new CountQuery(apiId, fromInstant, toInstant);
           case "STATS" -> new StatsQuery(apiId, fromInstant, toInstant, field);
           case "GROUP_BY" -> new GroupByQuery(apiId, fromInstant, toInstant, field, size, order);
           case "DATE_HISTO" -> new DateHistoQuery(apiId, fromInstant, toInstant, field, interval);
           default -> throw new IllegalArgumentException("Invalid query type: " + type);
       };
   }
   ```

#### Subtask 2.3.5: Run Tests (GREEN)

- Verify all validation tests pass
- Verify 404, 400, 403 responses work correctly
- Verify use case is called from REST resource

---

