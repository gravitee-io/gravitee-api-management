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

