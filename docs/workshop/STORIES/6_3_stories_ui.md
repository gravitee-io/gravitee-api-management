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

