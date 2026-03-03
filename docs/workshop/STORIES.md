# V4 API Analytics Dashboard - User Stories & Implementation Guide (End-to-End Flows)

This document provides a detailed breakdown organized into **end-to-end feature flows** that deliver user value incrementally. Each flow follows the pattern: **Backend → Frontend Service → UI → E2E/Performance Tests**.

**Document Version**: 3.0 (Reorganized for incremental delivery)

**Guiding Principles:**
- **Start with UI** to establish user experience first
- **Build complete features** end-to-end before moving to the next
- **Deliver value ASAP** - each flow produces working functionality
- **Small iterations** - prefer more flows over larger ones
- **TDD throughout** - RED-GREEN-REFACTOR for every story

---

## Table of Contents

- [Flow 1: Empty Dashboard Foundation](#flow-1-empty-dashboard-foundation)
- [Flow 2: Request Count (First Analytics Feature)](#flow-2-request-count-first-analytics-feature)
- [Flow 3: Gateway Latency Statistics](#flow-3-gateway-latency-statistics)
- [Flow 4: Additional Performance Statistics](#flow-4-additional-performance-statistics)
- [Flow 5: Status Code Distribution (Pie Chart)](#flow-5-status-code-distribution-pie-chart)
- [Flow 6: Response Time Over Time (Line Chart)](#flow-6-response-time-over-time-line-chart)
- [Flow 7: Error Handling & Resilience](#flow-7-error-handling--resilience)
- [Flow 8: Performance Validation & Final Polish](#flow-8-performance-validation--final-polish)
- [Implementation Timeline](#implementation-timeline)
- [Version History](#version-history)

---

## Flow 1: Empty Dashboard Foundation

**Goal:** Establish the UI foundation with routing, navigation, and empty state. No backend integration yet.

**User Value:** Users can navigate to the new Analytics dashboard and see a timeframe selector with an empty state.

**Duration:** 2 days

---

### Story 1.1: Dashboard Route & Navigation

**Title:** Create analytics dashboard route with navigation

**Description:**
As an API publisher, I want to navigate to the Analytics dashboard from the API menu so that I can access analytics data.

**Acceptance Criteria:**
- [ ] Route exists at `/apis/:apiId/analytics-v4`
- [ ] Menu item visible in API navigation sidebar
- [ ] Dashboard component renders with empty state
- [ ] Timeframe selector is visible and functional
- [ ] Page title shows "Analytics Dashboard"
- [ ] No backend calls are made yet
- [ ] Component harness tests verify navigation

**Layer:** Frontend
**Complexity:** XS (2 days)
**Dependencies:** None

#### Subtask 1.1.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **api-analytics-dashboard.component.harness.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-dashboard.component.harness.ts`
   - **Test Cases:**
     - `shouldRenderEmptyState()`
     - `shouldDisplayTimeframeSelector()`
     - `shouldNotMakeBackendCalls()`

2. **api-analytics-dashboard.component.spec.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-dashboard.component.spec.ts`
   - **Test Cases:**
     - `shouldCreate()`
     - `shouldDisplayEmptyState()`
     - `shouldRenderTimeframeSelector()`
     - `shouldHaveCorrectPageTitle()`

**Reference Patterns:**
- Existing harness: `api-analytics-proxy.component.harness.ts`
- Use Angular Component Harnesses pattern
- Use `HttpTestingController` to verify no HTTP calls

#### Subtask 1.1.1: Create Dashboard Component

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`

**Files to Create:**

1. **api-analytics-dashboard.component.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-dashboard.component.ts`
   - **Content:**
   ```typescript
   import { Component, computed, inject, signal } from '@angular/core';
   import { CommonModule } from '@angular/common';
   import { ActivatedRoute } from '@angular/router';
   import { MatCardModule } from '@angular/material/card';
   import { GioTimeframeModule } from '@gravitee/ui-particles-angular';

   @Component({
     selector: 'api-analytics-dashboard',
     standalone: true,
     imports: [CommonModule, MatCardModule, GioTimeframeModule],
     template: `
       <div class="analytics-dashboard">
         <div class="dashboard-header">
           <h1>Analytics Dashboard</h1>
           <gio-timeframe-selector
             [from]="timeRange().from"
             [to]="timeRange().to"
             (timeRangeChange)="onTimeRangeChange($event)"
           />
         </div>

         <div class="empty-state">
           <mat-card>
             <mat-card-content>
               <p>Select a time range to view analytics data.</p>
             </mat-card-content>
           </mat-card>
         </div>
       </div>
     `,
     styleUrls: ['./api-analytics-dashboard.component.scss']
   })
   export class ApiAnalyticsDashboardComponent {
     private readonly activatedRoute = inject(ActivatedRoute);

     readonly apiId = computed(() => this.activatedRoute.snapshot.params['apiId']);
     readonly timeRange = signal({
       from: Date.now() - 24 * 60 * 60 * 1000, // 24 hours ago
       to: Date.now()
     });

     onTimeRangeChange(range: { from: number; to: number }): void {
       this.timeRange.set(range);
     }
   }
   ```

2. **api-analytics-dashboard.component.scss**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-dashboard.component.scss`
   - **Content:**
   ```scss
   .analytics-dashboard {
     padding: 24px;

     .dashboard-header {
       display: flex;
       justify-content: space-between;
       align-items: center;
       margin-bottom: 24px;

       h1 {
         margin: 0;
       }
     }

     .empty-state {
       display: flex;
       justify-content: center;
       align-items: center;
       min-height: 400px;

       mat-card {
         text-align: center;
         padding: 48px;
       }
     }
   }
   ```

**Reference Patterns:**
- Study existing: `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
- Use Standalone Components
- Use Signals: `signal()`, `computed()`
- Use `inject()` for DI (NO constructor injection)
- Use `@if` control flow (NOT `*ngIf`)

#### Subtask 1.1.2: Add Route Configuration

**Files to Modify:**

1. **api-traffic-v4-routing.module.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/api-traffic-v4-routing.module.ts`
   - **Action:** Add route
   ```typescript
   {
     path: 'analytics-v4',
     component: ApiAnalyticsDashboardComponent,
     data: {
       breadcrumb: 'Analytics Dashboard'
     }
   }
   ```

#### Subtask 1.1.3: Add Navigation Menu Item

**Files to Modify:**

1. **api-navigation.component.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/components/api-navigation/api-navigation.component.ts`
   - **Action:** Add menu item
   ```typescript
   {
     label: 'Analytics Dashboard',
     routerLink: './analytics-v4',
     icon: 'chart-bar',
     permission: 'api-analytics-r'
   }
   ```

#### Subtask 1.1.4: Run Tests (GREEN)

- Verify all component tests pass
- Verify harness tests pass
- Verify navigation works in browser
- Verify no HTTP calls are made

---

### Story 1.2: E2E Test for Empty Dashboard

**Title:** E2E test verifying empty dashboard navigation

**Description:**
As a developer, I want E2E tests for the empty dashboard so that I can verify the basic navigation flow works.

**Acceptance Criteria:**
- [ ] E2E test navigates to dashboard
- [ ] E2E test verifies empty state is displayed
- [ ] E2E test verifies timeframe selector is present
- [ ] Test runs in CI/CD pipeline

**Layer:** Frontend E2E
**Complexity:** XS (1 day)
**Dependencies:** Story 1.1

#### Subtask 1.2.0: Write E2E Test

**Files to Create:**

1. **api-analytics-dashboard.e2e-spec.ts**
   - **Path:** `gravitee-apim-console-webui/e2e/api/analytics/api-analytics-dashboard.e2e-spec.ts`
   - **Test Cases:**
     - `should navigate to analytics dashboard`
     - `should display empty state`
     - `should display timeframe selector`

**Reference Patterns:**
- Existing E2E tests in `e2e/` directory
- Use Playwright or Protractor patterns

#### Subtask 1.2.1: Run Tests (GREEN)

- Verify E2E test passes locally
- Verify E2E test passes in CI

---

## Flow 2: Request Count (First Analytics Feature)

**Goal:** Deliver the first complete end-to-end analytics feature: total request count.

**User Value:** Users can see the total number of requests to their API in the selected time range.

**Duration:** 1.5 weeks

---

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

### Story 2.5: Frontend Service - COUNT Support

**Title:** Implement frontend service with COUNT request support

**Description:**
As a frontend developer, I want an Angular service that fetches COUNT analytics so that I can display the total request count in the UI.

**Acceptance Criteria:**
- [ ] Service method `getAnalytics(apiId, request)` exists
- [ ] TypeScript types for COUNT request/response exist
- [ ] Service coordinates with timeframe selector via BehaviorSubject
- [ ] HTTP parameters are correctly serialized
- [ ] Service tests verify HTTP calls

**Layer:** Frontend Service
**Complexity:** S (2 days)
**Dependencies:** Story 2.4

#### Subtask 2.5.0: Write Acceptance Tests (RED)

**Files to Create:**

1. **api-analytics-v2.service.spec.ts**
   - **Path:** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.spec.ts`
   - **Test Cases:**
     - `shouldFetchCountAnalytics()`
     - `shouldIncludeTimeRange()`
     - `shouldBuildCorrectQueryParams()`
     - `shouldHandleHttpError()`

**Reference Patterns:**
- Use `HttpTestingController` for testing
- Verify HTTP request parameters

#### Subtask 2.5.1: Create TypeScript Types

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

   export type BaseAnalyticsRequest = {
     from: number;
     to: number;
   };

   export type CountRequest = BaseAnalyticsRequest & {
     type: AnalyticsQueryType.COUNT;
   };

   export type StatsRequest = BaseAnalyticsRequest & {
     type: AnalyticsQueryType.STATS;
     field: string;
   };

   export type GroupByRequest = BaseAnalyticsRequest & {
     type: AnalyticsQueryType.GROUP_BY;
     field: string;
     size?: number;
     order?: 'asc' | 'desc';
   };

   export type DateHistoRequest = BaseAnalyticsRequest & {
     type: AnalyticsQueryType.DATE_HISTO;
     field: string;
     interval: number;
   };

   export type AnalyticsRequest = CountRequest | StatsRequest | GroupByRequest | DateHistoRequest;
   ```

2. **analyticsResponse.ts**
   - **Path:** `gravitee-apim-console-webui/src/entities/management-api-v2/analytics/analyticsResponse.ts`
   - **Content:**
   ```typescript
   export type CountResponse = {
     type: 'COUNT';
     count: number;
   };

   export type StatsResponse = {
     type: 'STATS';
     count: number;
     min?: number;
     max?: number;
     avg?: number;
     sum?: number;
   };

   export type GroupByResponse = {
     type: 'GROUP_BY';
     values: Record<string, number>;
     metadata: Record<string, { name: string }>;
   };

   export type DateHistoResponse = {
     type: 'DATE_HISTO';
     timestamp: number[];
     values: Array<{
       field: string;
       buckets: number[];
       metadata: { name: string };
     }>;
   };

   export type AnalyticsResponse = CountResponse | StatsResponse | GroupByResponse | DateHistoResponse;
   ```

**Reference Patterns:**
- Use TypeScript discriminated unions
- Use `type` field as discriminator

#### Subtask 2.5.2: Create Service

**Files to Create:**

1. **api-analytics-v2.service.ts**
   - **Path:** `gravitee-apim-console-webui/src/services-ngx/api-analytics-v2.service.ts`
   - **Content:**
   ```typescript
   import { Injectable, inject } from '@angular/core';
   import { HttpClient, HttpParams } from '@angular/common/http';
   import { BehaviorSubject, Observable } from 'rxjs';
   import { switchMap } from 'rxjs/operators';
   import { AnalyticsRequest, AnalyticsResponse } from '../entities/management-api-v2/analytics';

   @Injectable({
     providedIn: 'root',
   })
   export class ApiAnalyticsV2Service {
     private readonly http = inject(HttpClient);
     private readonly v2BaseURL = '/management/v2';
     private readonly timeRangeFilter$ = new BehaviorSubject<{ from: number; to: number } | null>(null);

     timeRangeFilter(): Observable<{ from: number; to: number } | null> {
       return this.timeRangeFilter$.asObservable();
     }

     setTimeRangeFilter(from: number, to: number): void {
       this.timeRangeFilter$.next({ from, to });
     }

     getAnalytics(apiId: string, request: AnalyticsRequest): Observable<AnalyticsResponse> {
       return this.timeRangeFilter().pipe(
         switchMap(() => {
           const params = this.buildQueryParams(request);
           return this.http.get<AnalyticsResponse>(`${this.v2BaseURL}/apis/${apiId}/analytics`, { params });
         })
       );
     }

     private buildQueryParams(request: AnalyticsRequest): HttpParams {
       let params = new HttpParams()
         .set('type', request.type)
         .set('from', request.from.toString())
         .set('to', request.to.toString());

       if ('field' in request && request.field) {
         params = params.set('field', request.field);
       }

       if ('interval' in request && request.interval) {
         params = params.set('interval', request.interval.toString());
       }

       if ('size' in request && request.size) {
         params = params.set('size', request.size.toString());
       }

       if ('order' in request && request.order) {
         params = params.set('order', request.order);
       }

       return params;
     }
   }
   ```

**Reference Patterns:**
- Study existing: `src/services-ngx/api-v2.service.ts`
- Use `inject()` for DI
- Use `BehaviorSubject` for timeframe coordination

#### Subtask 2.5.3: Run Tests (GREEN)

- Verify all service tests pass
- Verify HTTP calls are correct
- Verify parameters are serialized correctly

---

### Story 2.6: Count Card Widget

**Title:** Display total request count in a stats card

**Description:**
As an API publisher, I want to see the total request count displayed in a card so that I can quickly understand my API's traffic volume.

**Acceptance Criteria:**
- [ ] Card displays "Total Requests" label
- [ ] Card displays count value (formatted with commas)
- [ ] Card shows loading state while fetching
- [ ] Card shows error state if request fails
- [ ] Card updates when timeframe changes
- [ ] Component harness tests verify display

**Layer:** Frontend UI
**Complexity:** S (2 days)
**Dependencies:** Story 2.5

#### Subtask 2.6.0: Write Acceptance Tests (RED)

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`

**Files to Create:**

1. **api-analytics-count-card.component.harness.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-count-card/api-analytics-count-card.component.harness.ts`
   - **Test Cases:**
     - `shouldDisplayCountValue()`
     - `shouldDisplayLoadingState()`
     - `shouldDisplayErrorState()`
     - `shouldFormatNumberWithCommas()`

2. **api-analytics-count-card.component.spec.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-count-card/api-analytics-count-card.component.spec.ts`
   - **Test Cases:**
     - `shouldCreate()`
     - `shouldFetchCountOnInit()`
     - `shouldUpdateOnTimeRangeChange()`
     - `shouldHandleError()`

#### Subtask 2.6.1: Create Component

**Files to Create:**

1. **api-analytics-count-card.component.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-count-card/api-analytics-count-card.component.ts`
   - **Content:**
   ```typescript
   import { Component, computed, inject, input } from '@angular/core';
   import { CommonModule } from '@angular/common';
   import { MatCardModule } from '@angular/material/card';
   import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
   import { toSignal } from '@angular/core/rxjs-interop';
   import { switchMap, catchError, startWith } from 'rxjs/operators';
   import { of } from 'rxjs';

   import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
   import { AnalyticsQueryType, CountResponse } from '../../../../../entities/management-api-v2/analytics';

   @Component({
     selector: 'api-analytics-count-card',
     standalone: true,
     imports: [CommonModule, MatCardModule, MatProgressSpinnerModule],
     template: `
       <mat-card class="count-card">
         <mat-card-header>
           <mat-card-title>Total Requests</mat-card-title>
         </mat-card-header>
         <mat-card-content>
           @if (countData(); as data) {
             @if (data.isLoading) {
               <mat-spinner diameter="40" />
             } @else if (data.error) {
               <div class="error">Failed to load count</div>
             } @else if (data.count !== null) {
               <div class="count-value">{{ data.count | number }}</div>
             }
           }
         </mat-card-content>
       </mat-card>
     `,
     styleUrls: ['./api-analytics-count-card.component.scss']
   })
   export class ApiAnalyticsCountCardComponent {
     private readonly apiAnalyticsV2Service = inject(ApiAnalyticsV2Service);

     readonly apiId = input.required<string>();
     readonly timeRange = input.required<{ from: number; to: number }>();

     private readonly countData$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
       switchMap(() => {
         const range = this.timeRange();
         return this.apiAnalyticsV2Service.getAnalytics(this.apiId(), {
           type: AnalyticsQueryType.COUNT,
           from: range.from,
           to: range.to,
         }).pipe(
           catchError((error) => {
             console.error('Error fetching count analytics:', error);
             return of({ type: 'COUNT' as const, count: null, error: true });
           }),
           startWith({ isLoading: true, count: null })
         );
       })
     );

     readonly countData = toSignal(this.countData$, { initialValue: { isLoading: true, count: null } });
   }
   ```

2. **api-analytics-count-card.component.scss**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-count-card/api-analytics-count-card.component.scss`
   - **Content:**
   ```scss
   .count-card {
     min-height: 150px;

     mat-card-content {
       display: flex;
       justify-content: center;
       align-items: center;
       min-height: 80px;

       .count-value {
         font-size: 36px;
         font-weight: 600;
         color: var(--gio-app-primary-main-color);
       }

       .error {
         color: var(--gio-app-error-color);
       }
     }
   }
   ```

**Reference Patterns:**
- Study existing: `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts`
- Use `toSignal()` for Observable-to-Signal conversion
- Use `@if` control flow (NOT `*ngIf`)

#### Subtask 2.6.2: Update Dashboard Component

**Files to Modify:**

1. **api-analytics-dashboard.component.ts**
   - **Action:** Replace empty state with count card
   ```typescript
   imports: [CommonModule, MatCardModule, GioTimeframeModule, ApiAnalyticsCountCardComponent],
   template: `
     <div class="analytics-dashboard">
       <div class="dashboard-header">
         <h1>Analytics Dashboard</h1>
         <gio-timeframe-selector
           [from]="timeRange().from"
           [to]="timeRange().to"
           (timeRangeChange)="onTimeRangeChange($event)"
         />
       </div>

       <div class="dashboard-content">
         <api-analytics-count-card
           [apiId]="apiId()"
           [timeRange]="timeRange()"
         />
       </div>
     </div>
   `,
   ```

#### Subtask 2.6.3: Run Tests (GREEN)

- Verify all component tests pass
- Verify harness tests pass
- Verify count displays correctly in browser
- Verify loading and error states work

---

### Story 2.7: E2E Test for Count Feature

**Title:** E2E test verifying complete count flow

**Description:**
As a developer, I want E2E tests for the count feature so that I can verify the complete end-to-end flow works.

**Acceptance Criteria:**
- [ ] E2E test navigates to dashboard
- [ ] E2E test verifies count card is displayed
- [ ] E2E test verifies count value is shown
- [ ] E2E test changes timeframe and verifies count updates
- [ ] Test uses real backend (or mock server)

**Layer:** Frontend E2E
**Complexity:** XS (1 day)
**Dependencies:** Story 2.6

#### Subtask 2.7.0: Write E2E Test

**Files to Modify:**

1. **api-analytics-dashboard.e2e-spec.ts**
   - **Action:** Add count tests
   - **Test Cases:**
     - `should display count card with value`
     - `should update count when timeframe changes`
     - `should handle loading state`

#### Subtask 2.7.1: Run Tests (GREEN)

- Verify E2E tests pass locally
- Verify E2E tests pass in CI

---

## Flow 3: Gateway Latency Statistics

**Goal:** Add the first statistics card showing gateway latency (avg, min, max).

**User Value:** Users can see gateway latency statistics to understand API performance.

**Duration:** 1 week

---

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

### Story 3.2: Frontend Service - STATS Support

**Title:** Add STATS request support to frontend service

**Description:**
As a frontend developer, I want the service to support STATS requests so that I can fetch statistics data.

**Acceptance Criteria:**
- [ ] Service handles STATS request type
- [ ] TypeScript types for STATS request/response exist
- [ ] Service tests verify STATS HTTP calls

**Layer:** Frontend Service
**Complexity:** XS (1 day)
**Dependencies:** Story 3.1

#### Subtask 3.2.0: Write Tests (RED)

**Files to Modify:**

1. **api-analytics-v2.service.spec.ts**
   - **Action:** Add STATS tests
   - **Test Cases:**
     - `shouldFetchStatsAnalytics()`
     - `shouldIncludeFieldParameter()`

#### Subtask 3.2.1: Verify Types Exist

- Types for STATS were already created in Story 2.5.1
- No changes needed to service (already supports discriminated unions)

#### Subtask 3.2.2: Run Tests (GREEN)

---

### Story 3.3: Gateway Latency Stats Card

**Title:** Display gateway latency statistics in a card

**Description:**
As an API publisher, I want to see gateway latency stats (avg, min, max) so that I can understand API performance.

**Acceptance Criteria:**
- [ ] Card displays "Gateway Latency" label
- [ ] Card displays avg, min, max values (formatted in ms)
- [ ] Card shows loading and error states
- [ ] Card updates when timeframe changes
- [ ] Component harness tests verify display

**Layer:** Frontend UI
**Complexity:** S (2 days)
**Dependencies:** Story 3.2

#### Subtask 3.3.0: Write Tests (RED)

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`

**Files to Create:**

1. **api-analytics-stats-card.component.harness.ts**
2. **api-analytics-stats-card.component.spec.ts**

#### Subtask 3.3.1: Create Component

**Files to Create:**

1. **api-analytics-stats-card.component.ts**
   - **Path:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics-dashboard/api-analytics-stats-card/api-analytics-stats-card.component.ts`
   - **Content:**
   ```typescript
   import { Component, computed, inject, input } from '@angular/core';
   import { CommonModule } from '@angular/common';
   import { MatCardModule } from '@angular/material/card';
   import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
   import { toSignal } from '@angular/core/rxjs-interop';
   import { switchMap, catchError, startWith } from 'rxjs/operators';
   import { of } from 'rxjs';

   import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
   import { AnalyticsQueryType } from '../../../../../entities/management-api-v2/analytics';

   @Component({
     selector: 'api-analytics-stats-card',
     standalone: true,
     imports: [CommonModule, MatCardModule, MatProgressSpinnerModule],
     template: `
       <mat-card class="stats-card">
         <mat-card-header>
           <mat-card-title>{{ title() }}</mat-card-title>
         </mat-card-header>
         <mat-card-content>
           @if (statsData(); as data) {
             @if (data.isLoading) {
               <mat-spinner diameter="40" />
             } @else if (data.error) {
               <div class="error">Failed to load statistics</div>
             } @else if (data.stats) {
               <div class="stats-grid">
                 <div class="stat-item">
                   <div class="stat-label">Average</div>
                   <div class="stat-value">{{ data.stats.avg | number:'1.2-2' }} ms</div>
                 </div>
                 <div class="stat-item">
                   <div class="stat-label">Min</div>
                   <div class="stat-value">{{ data.stats.min | number:'1.2-2' }} ms</div>
                 </div>
                 <div class="stat-item">
                   <div class="stat-label">Max</div>
                   <div class="stat-value">{{ data.stats.max | number:'1.2-2' }} ms</div>
                 </div>
               </div>
             }
           }
         </mat-card-content>
       </mat-card>
     `,
     styleUrls: ['./api-analytics-stats-card.component.scss']
   })
   export class ApiAnalyticsStatsCardComponent {
     private readonly apiAnalyticsV2Service = inject(ApiAnalyticsV2Service);

     readonly apiId = input.required<string>();
     readonly timeRange = input.required<{ from: number; to: number }>();
     readonly field = input.required<string>();
     readonly title = input.required<string>();

     private readonly statsData$ = this.apiAnalyticsV2Service.timeRangeFilter().pipe(
       switchMap(() => {
         const range = this.timeRange();
         return this.apiAnalyticsV2Service.getAnalytics(this.apiId(), {
           type: AnalyticsQueryType.STATS,
           field: this.field(),
           from: range.from,
           to: range.to,
         }).pipe(
           catchError((error) => {
             console.error('Error fetching stats analytics:', error);
             return of({ error: true });
           }),
           startWith({ isLoading: true })
         );
       })
     );

     readonly statsData = toSignal(this.statsData$, { initialValue: { isLoading: true } });
   }
   ```

2. **api-analytics-stats-card.component.scss**
   - **Content:**
   ```scss
   .stats-card {
     min-height: 180px;

     mat-card-content {
       display: flex;
       justify-content: center;
       align-items: center;
       min-height: 120px;

       .stats-grid {
         display: grid;
         grid-template-columns: repeat(3, 1fr);
         gap: 24px;
         width: 100%;

         .stat-item {
           text-align: center;

           .stat-label {
             font-size: 12px;
             color: var(--gio-app-text-muted-color);
             margin-bottom: 8px;
           }

           .stat-value {
             font-size: 24px;
             font-weight: 600;
             color: var(--gio-app-primary-main-color);
           }
         }
       }

       .error {
         color: var(--gio-app-error-color);
       }
     }
   }
   ```

#### Subtask 3.3.2: Update Dashboard Component

**Files to Modify:**

1. **api-analytics-dashboard.component.ts**
   - **Action:** Add gateway latency card
   ```typescript
   <div class="dashboard-content">
     <api-analytics-count-card
       [apiId]="apiId()"
       [timeRange]="timeRange()"
     />
     <api-analytics-stats-card
       [apiId]="apiId()"
       [timeRange]="timeRange()"
       [field]="'gateway-latency-ms'"
       [title]="'Gateway Latency'"
     />
   </div>
   ```

#### Subtask 3.3.3: Run Tests (GREEN)

---

### Story 3.4: E2E Test for Stats Feature

**Title:** E2E test verifying statistics display

**Description:**
As a developer, I want E2E tests for the stats feature so that I can verify statistics are displayed correctly.

**Acceptance Criteria:**
- [ ] E2E test verifies stats card is displayed
- [ ] E2E test verifies avg, min, max values are shown
- [ ] E2E test changes timeframe and verifies stats update

**Layer:** Frontend E2E
**Complexity:** XS (1 day)
**Dependencies:** Story 3.3

#### Subtask 3.4.0: Write E2E Test

**Files to Modify:**

1. **api-analytics-dashboard.e2e-spec.ts**
   - **Action:** Add stats tests

#### Subtask 3.4.1: Run Tests (GREEN)

---

## Flow 4: Additional Performance Statistics

**Goal:** Add more statistics cards for response time and endpoint latency.

**User Value:** Users get a complete view of performance metrics.

**Duration:** 3 days

---

### Story 4.1: Response Time and Endpoint Stats Cards

**Title:** Add response time and endpoint latency stats cards

**Description:**
As an API publisher, I want to see response time and endpoint latency stats so that I have a complete performance overview.

**Acceptance Criteria:**
- [ ] Card for "Gateway Response Time" displays stats
- [ ] Card for "Endpoint Response Time" displays stats
- [ ] Both cards reuse existing STATS backend
- [ ] Dashboard shows all three stats cards in a grid

**Layer:** Frontend UI
**Complexity:** XS (3 days)
**Dependencies:** Story 3.3

#### Subtask 4.1.0: Write Tests (RED)

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`

#### Subtask 4.1.1: Update Dashboard Component

**Files to Modify:**

1. **api-analytics-dashboard.component.ts**
   - **Action:** Add more stats cards
   ```typescript
   <div class="dashboard-content">
     <div class="cards-grid">
       <api-analytics-count-card
         [apiId]="apiId()"
         [timeRange]="timeRange()"
       />
     </div>

     <div class="stats-grid">
       <api-analytics-stats-card
         [apiId]="apiId()"
         [timeRange]="timeRange()"
         [field]="'gateway-latency-ms'"
         [title]="'Gateway Latency'"
       />
       <api-analytics-stats-card
         [apiId]="apiId()"
         [timeRange]="timeRange()"
         [field]="'gateway-response-time-ms'"
         [title]="'Gateway Response Time'"
       />
       <api-analytics-stats-card
         [apiId]="apiId()"
         [timeRange]="timeRange()"
         [field]="'endpoint-response-time-ms'"
         [title]="'Endpoint Response Time'"
       />
     </div>
   </div>
   ```

2. **api-analytics-dashboard.component.scss**
   - **Action:** Add grid layout
   ```scss
   .dashboard-content {
     .cards-grid {
       display: grid;
       grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
       gap: 16px;
       margin-bottom: 24px;
     }

     .stats-grid {
       display: grid;
       grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
       gap: 16px;
     }
   }
   ```

#### Subtask 4.1.2: Run Tests (GREEN)

---

## Flow 5: Status Code Distribution (Pie Chart)

**Goal:** Add a pie chart showing the distribution of HTTP status codes.

**User Value:** Users can visualize the success/error rate of their API.

**Duration:** 1 week

---

### Story 5.1: GROUP_BY Query Implementation

**Title:** Implement GROUP_BY aggregation for categorical field grouping

**Description:**
As an API publisher, I want to group metrics by categorical fields so that I can see distributions (e.g., status code breakdown).

**Acceptance Criteria:**
- [ ] `GET /v2/apis/{apiId}/analytics?type=GROUP_BY&field=status&size=10&order=desc` returns grouped data
- [ ] Response includes value counts and metadata
- [ ] Field validation ensures only allowed fields
- [ ] Size parameter limits results (default 10, max 100)
- [ ] Order parameter sorts results (asc/desc)
- [ ] Automatic `.keyword` suffix for text fields
- [ ] Integration test verifies actual ES terms aggregation

**Layer:** Backend
**Complexity:** M (4 days)
**Dependencies:** Story 3.1

#### Subtask 5.1.0: Write Acceptance Tests (RED)

Similar structure to Story 3.1, with tests for:
- Query adapter
- Response adapter
- Integration test with Testcontainers

#### Subtask 5.1.1: Create Query Adapter

**Files to Create:**

1. **SearchGroupByAnalyticsQueryAdapter.java**
   - Build ES terms aggregation
   - Handle `.keyword` suffix automatically
   - Support size and order parameters

#### Subtask 5.1.2: Create Response Adapter

**Files to Create:**

1. **SearchGroupByAnalyticsResponseAdapter.java**
   - Parse ES buckets into values map
   - Extract metadata (e.g., status code names)

#### Subtask 5.1.3: Add Repository and Service Methods

#### Subtask 5.1.4: Update Use Case

**Files to Modify:**

1. **SearchUnifiedAnalyticsUseCase.java**
   - Add GROUP_BY query handling

#### Subtask 5.1.5: Integration Test

#### Subtask 5.1.6: Run Tests (GREEN)

---

### Story 5.2: Frontend Service - GROUP_BY Support

**Title:** Add GROUP_BY request support to frontend service

**Acceptance Criteria:**
- [ ] Service handles GROUP_BY request type
- [ ] TypeScript types exist
- [ ] Service tests verify HTTP calls

**Layer:** Frontend Service
**Complexity:** XS (1 day)
**Dependencies:** Story 5.1

---

### Story 5.3: HTTP Status Pie Chart Widget

**Title:** Display HTTP status code distribution in a pie chart

**Description:**
As an API publisher, I want to see the distribution of HTTP status codes in a pie chart so that I can visualize success vs error rates.

**Acceptance Criteria:**
- [ ] Pie chart displays status code segments
- [ ] Chart uses Highcharts (existing in codebase)
- [ ] Chart shows loading and error states
- [ ] Chart updates when timeframe changes
- [ ] Legend shows status code labels (200 OK, 404 Not Found, etc.)
- [ ] Component harness tests verify chart rendering

**Layer:** Frontend UI
**Complexity:** M (3 days)
**Dependencies:** Story 5.2

#### Subtask 5.3.0: Write Tests (RED)

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`
- Specifically study how Highcharts is integrated in existing analytics components

#### Subtask 5.3.1: Create Component

**Files to Create:**

1. **api-analytics-pie-chart.component.ts**
   - Use Highcharts for pie chart
   - Map GROUP_BY response to Highcharts data format
   - Include status code names in labels

#### Subtask 5.3.2: Update Dashboard Component

**Files to Modify:**

1. **api-analytics-dashboard.component.ts**
   - Add pie chart below stats cards

#### Subtask 5.3.3: Run Tests (GREEN)

---

### Story 5.4: E2E Test for Pie Chart

**Layer:** Frontend E2E
**Complexity:** XS (1 day)
**Dependencies:** Story 5.3

---

## Flow 6: Response Time Over Time (Line Chart)

**Goal:** Add a line chart showing response time trends over time.

**User Value:** Users can see performance trends and identify spikes.

**Duration:** 1 week

---

### Story 6.1: DATE_HISTO Query Implementation

**Title:** Implement DATE_HISTO aggregation for time series data

**Description:**
As an API publisher, I want to see metrics over time so that I can identify trends and anomalies.

**Acceptance Criteria:**
- [ ] `GET /v2/apis/{apiId}/analytics?type=DATE_HISTO&field=gateway-response-time-ms&interval=3600000` returns time series
- [ ] Response includes timestamps and bucket values
- [ ] Interval parameter controls bucket size (in milliseconds)
- [ ] Time series aligned to interval boundaries
- [ ] Integration test verifies actual ES date_histogram aggregation

**Layer:** Backend
**Complexity:** M (4 days)
**Dependencies:** Story 5.1

#### Subtask 6.1.0: Write Acceptance Tests (RED)

#### Subtask 6.1.1: Create Query Adapter

**Files to Create:**

1. **SearchDateHistoAnalyticsQueryAdapter.java**
   - Build ES date_histogram aggregation
   - Support interval parameter

#### Subtask 6.1.2: Create Response Adapter

**Files to Create:**

1. **SearchDateHistoAnalyticsResponseAdapter.java**
   - Parse ES buckets into timestamp and values arrays

#### Subtask 6.1.3: Add Repository and Service Methods

#### Subtask 6.1.4: Update Use Case

#### Subtask 6.1.5: Integration Test

#### Subtask 6.1.6: Run Tests (GREEN)

---

### Story 6.2: Frontend Service - DATE_HISTO Support

**Layer:** Frontend Service
**Complexity:** XS (1 day)
**Dependencies:** Story 6.1

---

### Story 6.3: Response Time Line Chart

**Title:** Display response time over time in a line chart

**Description:**
As an API publisher, I want to see response time trends over time so that I can identify performance degradation.

**Acceptance Criteria:**
- [ ] Line chart displays time series data
- [ ] Chart uses Highcharts (existing pattern)
- [ ] X-axis shows time
- [ ] Y-axis shows response time (ms)
- [ ] Chart updates when timeframe changes
- [ ] Component harness tests verify chart rendering

**Layer:** Frontend UI
**Complexity:** M (3 days)
**Dependencies:** Story 6.2

#### Subtask 6.3.0: Write Tests (RED)

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`

#### Subtask 6.3.1: Create or Verify Component

- Check if existing line chart component can be reused
- If yes, integrate it
- If no, create new component following existing patterns

#### Subtask 6.3.2: Update Dashboard Component

#### Subtask 6.3.3: Run Tests (GREEN)

---

### Story 6.4: E2E Test for Line Chart

**Layer:** Frontend E2E
**Complexity:** XS (1 day)
**Dependencies:** Story 6.3

---

## Flow 7: Error Handling & Resilience

**Goal:** Add robust error handling for Elasticsearch failures and UI error states.

**User Value:** Users see helpful error messages instead of crashes.

**Duration:** 1 week

---

### Story 7.1: Elasticsearch Error Handling

**Title:** Handle Elasticsearch failures gracefully

**Description:**
As a platform operator, I want analytics queries to handle ES failures gracefully so that users receive appropriate error messages.

**Acceptance Criteria:**
- [ ] Returns 503 when ES is unreachable
- [ ] Returns 500 with error ID when ES query fails
- [ ] Returns 504 when ES query exceeds 10 seconds
- [ ] Logs errors with full context
- [ ] Returns empty results (not error) when index doesn't exist

**Layer:** Backend
**Complexity:** S (2 days)
**Dependencies:** Story 2.4, 3.1, 5.1, 6.1

#### Subtask 7.1.0: Write Tests (RED)

#### Subtask 7.1.1: Add Error Handling to Repository

**Files to Modify:**

1. **AnalyticsElasticsearchRepository.java**
   - Add try-catch blocks
   - Detect different error types
   - Log with error IDs

#### Subtask 7.1.2: Add Custom Exceptions

**Files to Create:**

1. **AnalyticsTimeoutException.java**
2. **AnalyticsUnavailableException.java**
3. **AnalyticsQueryException.java**

#### Subtask 7.1.3: Update REST Resource

**Files to Modify:**

1. **ApiAnalyticsResource.java**
   - Map exceptions to HTTP status codes
   - Return error details

#### Subtask 7.1.4: Run Tests (GREEN)

---

### Story 7.2: UI Error States

**Title:** Display error states in all widgets

**Description:**
As an API publisher, I want to see clear error messages when analytics fail so that I know what went wrong.

**Acceptance Criteria:**
- [ ] All widgets show error state UI
- [ ] Error messages are user-friendly
- [ ] Retry button allows manual refresh
- [ ] Error details logged to console for debugging

**Layer:** Frontend UI
**Complexity:** S (2 days)
**Dependencies:** Story 7.1

#### Subtask 7.2.0: Write Tests (RED)

**IMPORTANT:** The Gravitee Console uses the Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material. Before implementing any UI component, **study the EXISTING analytics code** at:
- `src/management/api/api-traffic-v4/analytics/`

#### Subtask 7.2.1: Update Components

- Enhance error handling in all widget components
- Add retry functionality
- Improve error messages

#### Subtask 7.2.2: Run Tests (GREEN)

---

### Story 7.3: E2E Error Scenario Tests

**Title:** E2E tests for error scenarios

**Description:**
As a developer, I want E2E tests for error scenarios so that I can verify error handling works correctly.

**Acceptance Criteria:**
- [ ] Test for ES unavailable scenario
- [ ] Test for ES timeout scenario
- [ ] Test for invalid API ID
- [ ] Test for missing permissions

**Layer:** Frontend E2E
**Complexity:** S (2 days)
**Dependencies:** Story 7.2

---

## Flow 8: Performance Validation & Final Polish

**Goal:** Add performance constraints, validation, and comprehensive E2E tests.

**User Value:** Dashboard performs well even with large data volumes.

**Duration:** 1 week

---

### Story 8.1: Performance Validation & Timeouts

**Title:** Add performance constraints and timeout protection

**Description:**
As a platform operator, I want analytics queries to have performance constraints so that they don't overload Elasticsearch.

**Acceptance Criteria:**
- [ ] 90-day max time range enforced
- [ ] 10-second ES query timeout configured
- [ ] < 5 second end-to-end SLA monitored
- [ ] Performance tests verify constraints

**Layer:** Backend
**Complexity:** S (3 days)
**Dependencies:** Flow 7

#### Subtask 8.1.0: Write Tests (RED)

#### Subtask 8.1.1: Add Timeout Configuration

#### Subtask 8.1.2: Add Performance Tests

#### Subtask 8.1.3: Run Tests (GREEN)

---

### Story 8.2: Comprehensive E2E Tests

**Title:** End-to-end dashboard tests covering all features

**Description:**
As a developer, I want comprehensive E2E tests so that I can verify the complete dashboard works correctly.

**Acceptance Criteria:**
- [ ] Test complete user journey
- [ ] Test all widgets together
- [ ] Test timeframe changes
- [ ] Test performance under load
- [ ] Test cross-browser compatibility

**Layer:** Frontend E2E
**Complexity:** M (3 days)
**Dependencies:** All previous stories

#### Subtask 8.2.0: Write Comprehensive E2E Tests

**Files to Create:**

1. **api-analytics-dashboard-complete.e2e-spec.ts**
   - Test complete user flow
   - Test all interactions
   - Test performance

#### Subtask 8.2.1: Run Tests (GREEN)

---

### Story 8.3: Final Integration & Verification

**Title:** Integration testing and final verification

**Description:**
As a product owner, I want to verify the complete dashboard works correctly so that it's ready for production.

**Acceptance Criteria:**
- [ ] All widgets display correctly together
- [ ] Dashboard is responsive
- [ ] Performance meets SLA
- [ ] Documentation is complete
- [ ] Code review completed

**Layer:** Frontend
**Complexity:** S (2 days)
**Dependencies:** Story 8.2

---

## Implementation Timeline

### Overview

Total Duration: **8 weeks** (organized into 8 end-to-end flows)

### Flow-by-Flow Schedule

| Flow | Duration | Description | User Value Delivered |
|------|----------|-------------|---------------------|
| Flow 1 | 2 days | Empty Dashboard Foundation | Navigation & UI shell |
| Flow 2 | 1.5 weeks | Request Count (First Analytics) | Total request count |
| Flow 3 | 1 week | Gateway Latency Statistics | First performance metric |
| Flow 4 | 3 days | Additional Performance Stats | Complete performance overview |
| Flow 5 | 1 week | Status Code Distribution | Success/error visualization |
| Flow 6 | 1 week | Response Time Over Time | Performance trends |
| Flow 7 | 1 week | Error Handling & Resilience | Robust error handling |
| Flow 8 | 1 week | Performance & Final Polish | Production-ready dashboard |

### Parallel Work Opportunities

- **Week 1-2 (Flow 1-2):** Single team, establish foundation
- **Week 3-6 (Flow 3-6):** Can parallelize:
  - Team A: Backend query types (STATS, GROUP_BY, DATE_HISTO)
  - Team B: Frontend widgets (Stats cards, Pie chart, Line chart)
- **Week 7-8 (Flow 7-8):** Single team, polish and integration

### Delivery Milestones

**Week 2:** First user value - empty dashboard with navigation
**Week 3.5:** First analytics feature - request count visible
**Week 4.5:** Performance metrics visible
**Week 5:** Complete performance overview
**Week 6:** Status distribution visualization
**Week 7:** Time series trends
**Week 8:** Production-ready dashboard

---

## Version History

### Version 3.0 (Current) - End-to-End Flows Organization

**Changes:**
- **Reorganized structure**: Changed from layer-based (backend/frontend) to end-to-end feature flows
- **Incremental delivery**: Each flow delivers complete user value
- **UI-first approach**: Flow 1 starts with empty dashboard (no backend)
- **Small iterations**: 8 flows instead of 4 sprints, shorter feedback cycles
- **Particles design system**: Added explicit requirement to every UI story to use Gravitee Particles design system
- **Study existing code**: Added requirement to study existing analytics code before implementing UI

**Structure:**
- 8 end-to-end flows (vs. 18 sequential stories in v2.0)
- Each flow: Backend → Frontend Service → UI → E2E/Perf tests
- Flows deliver user value: Count → Stats → Pie → Line → Errors → Performance

**Key Principles:**
- Start with UI to establish experience
- Build backend only when needed
- Test end-to-end after each flow
- Deliver user value ASAP
- Keep flows small for fast iteration

### Version 2.0 - TDD Compliance & Critical Review

**Changes from v1.0:**
- TDD compliance: Tests first (Subtask X.0) in every story
- Story 1 split into 1A/1B/1C for accurate sizing
- Authorization moved to Story 1B (foundation, not afterthought)
- Added Story 2.5 (ES error handling)
- Added Story 6.5 (performance validation)
- Added Story 7.5 (navigation)
- Story 7 split into 7A/7B/7C/7D (unblock frontend earlier)
- Story 11 eliminated (merged into 8 & 9)
- Testcontainers added to all backend stories
- Sealed interfaces kept (type safety valuable)

### Version 1.0 - Initial Decomposition

**Original structure:**
- 14 sequential stories
- Backend stories: 1-6
- Frontend stories: 7-12
- Testing stories: 13-14
- Tests at end (violated TDD)

---

## Notes on Design System Usage

**IMPORTANT:** Every UI component in this workshop MUST use the Gravitee Particles design system (`@gravitee/ui-particles-angular`) on top of Angular Material.

**Before implementing any UI story:**
1. Study existing analytics code at: `src/management/api/api-traffic-v4/analytics/`
2. Review Particles components documentation
3. Reuse existing patterns and components
4. Consult with UX team if new components are needed

**Common Particles Components:**
- `gio-timeframe-selector` for date range selection
- `gio-card` for card containers
- `gio-loader` for loading states
- `gio-banner` for error messages
- Chart components (verify existing integrations)

This ensures visual consistency across the Gravitee Console.
