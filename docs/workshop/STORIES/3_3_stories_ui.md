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

