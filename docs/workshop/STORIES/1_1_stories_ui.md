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

