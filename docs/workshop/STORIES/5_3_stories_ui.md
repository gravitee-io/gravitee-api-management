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

