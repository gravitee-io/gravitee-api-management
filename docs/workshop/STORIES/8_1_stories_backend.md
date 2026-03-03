### Story 8.1: Performance Validation & Timeouts

**Title:** Add performance constraints and timeout protection

**Description:**
As a platform operator, I want analytics queries to have performance constraints so that they don't overload Elasticsearch.

**Acceptance Criteria:**
- [ ] 90-day max time range enforced
- [ ] 10-second ES query timeout configured
- [ ] < 5 second end-to-end SLA monitored
- [ ] Performance tests verify constraints

**Layer:** Backend
**Complexity:** S (3 days)
**Dependencies:** Flow 7

#### Subtask 8.1.0: Write Tests (RED)

#### Subtask 8.1.1: Add Timeout Configuration

#### Subtask 8.1.2: Add Performance Tests

#### Subtask 8.1.3: Run Tests (GREEN)

---

