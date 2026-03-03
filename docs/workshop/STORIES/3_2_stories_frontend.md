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

