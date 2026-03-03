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

