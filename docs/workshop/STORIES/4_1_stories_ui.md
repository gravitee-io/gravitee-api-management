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

