### Story 2.2: REST Endpoint Skeleton with Authorization

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
**Dependencies:** Story 2.1

#### Subtask 2.2.0: Write Acceptance Tests (RED)

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

#### Subtask 2.2.1: Create REST Resource Method

**Files to Create:**

1. **ApiAnalyticsResource.java**
   - **Path:** `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/api/analytics/ApiAnalyticsResource.java`
   - **Content:**
   ```java
   package io.gravitee.rest.api.management.v2.rest.resource.api.analytics;

   import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
   import io.gravitee.rest.api.model.permissions.RolePermission;
   import io.gravitee.rest.api.model.permissions.RolePermissionAction;
   import io.gravitee.rest.api.rest.annotation.Permission;
   import io.gravitee.rest.api.rest.annotation.Permissions;
   import jakarta.ws.rs.*;
   import jakarta.ws.rs.core.MediaType;
   import jakarta.ws.rs.core.Response;
   import java.util.Arrays;
   import java.util.Map;

   @Path("/v2/apis/{apiId}/analytics")
   public class ApiAnalyticsResource extends AbstractResource {

       @GET
       @Produces(MediaType.APPLICATION_JSON)
       @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
       public Response getAnalytics(
           @PathParam("apiId") String apiId,
           @QueryParam("type") String type,
           @QueryParam("from") Long from,
           @QueryParam("to") Long to,
           @QueryParam("field") String field,
           @QueryParam("interval") Long interval,
           @QueryParam("size") Integer size,
           @QueryParam("order") String order
       ) {
           // Validate required parameters
           if (type == null || type.isEmpty()) {
               return Response.status(400)
                   .entity(Map.of("error", "Parameter 'type' is required"))
                   .build();
           }

           if (from == null || to == null) {
               return Response.status(400)
                   .entity(Map.of("error", "Parameters 'from' and 'to' are required"))
                   .build();
           }

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
   }
   ```

**Reference Patterns:**
- Existing resources in same package
- Use `@Permissions` annotation for authorization
- Use JAX-RS annotations: `@GET`, `@Produces`, `@PathParam`, `@QueryParam`

#### Subtask 2.2.2: Create OpenAPI Specification

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

#### Subtask 2.2.3: Run Tests (GREEN)

- Verify all validation tests pass
- Verify 403 response for missing permission
- Verify 501 response for all query types
- Verify OpenAPI spec validates with swagger-cli

---

