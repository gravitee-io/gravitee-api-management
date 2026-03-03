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

