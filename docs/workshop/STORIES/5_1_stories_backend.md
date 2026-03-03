### Story 5.1: GROUP_BY Query Implementation

**Title:** Implement GROUP_BY aggregation for categorical field grouping

**Description:**
As an API publisher, I want to group metrics by categorical fields so that I can see distributions (e.g., status code breakdown).

**Acceptance Criteria:**
- [ ] `GET /v2/apis/{apiId}/analytics?type=GROUP_BY&field=status&size=10&order=desc` returns grouped data
- [ ] Response includes value counts and metadata
- [ ] Field validation ensures only allowed fields
- [ ] Size parameter limits results (default 10, max 100)
- [ ] Order parameter sorts results (asc/desc)
- [ ] Automatic `.keyword` suffix for text fields
- [ ] Integration test verifies actual ES terms aggregation

**Layer:** Backend
**Complexity:** M (4 days)
**Dependencies:** Story 3.1

#### Subtask 5.1.0: Write Acceptance Tests (RED)

Similar structure to Story 3.1, with tests for:
- Query adapter
- Response adapter
- Integration test with Testcontainers

#### Subtask 5.1.1: Create Query Adapter

**Files to Create:**

1. **SearchGroupByAnalyticsQueryAdapter.java**
   - Build ES terms aggregation
   - Handle `.keyword` suffix automatically
   - Support size and order parameters

#### Subtask 5.1.2: Create Response Adapter

**Files to Create:**

1. **SearchGroupByAnalyticsResponseAdapter.java**
   - Parse ES buckets into values map
   - Extract metadata (e.g., status code names)

#### Subtask 5.1.3: Add Repository and Service Methods

#### Subtask 5.1.4: Update Use Case

**Files to Modify:**

1. **SearchUnifiedAnalyticsUseCase.java**
   - Add GROUP_BY query handling

#### Subtask 5.1.5: Integration Test

#### Subtask 5.1.6: Run Tests (GREEN)

---

