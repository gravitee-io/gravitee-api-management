### Story 6.1: DATE_HISTO Query Implementation

**Title:** Implement DATE_HISTO aggregation for time series data

**Description:**
As an API publisher, I want to see metrics over time so that I can identify trends and anomalies.

**Acceptance Criteria:**
- [ ] `GET /v2/apis/{apiId}/analytics?type=DATE_HISTO&field=gateway-response-time-ms&interval=3600000` returns time series
- [ ] Response includes timestamps and bucket values
- [ ] Interval parameter controls bucket size (in milliseconds)
- [ ] Time series aligned to interval boundaries
- [ ] Integration test verifies actual ES date_histogram aggregation

**Layer:** Backend
**Complexity:** M (4 days)
**Dependencies:** Story 5.1

#### Subtask 6.1.0: Write Acceptance Tests (RED)

#### Subtask 6.1.1: Create Query Adapter

**Files to Create:**

1. **SearchDateHistoAnalyticsQueryAdapter.java**
   - Build ES date_histogram aggregation
   - Support interval parameter

#### Subtask 6.1.2: Create Response Adapter

**Files to Create:**

1. **SearchDateHistoAnalyticsResponseAdapter.java**
   - Parse ES buckets into timestamp and values arrays

#### Subtask 6.1.3: Add Repository and Service Methods

#### Subtask 6.1.4: Update Use Case

#### Subtask 6.1.5: Integration Test

#### Subtask 6.1.6: Run Tests (GREEN)

---

