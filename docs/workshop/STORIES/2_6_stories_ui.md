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

