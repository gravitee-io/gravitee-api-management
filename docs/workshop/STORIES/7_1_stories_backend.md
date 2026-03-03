### Story 7.1: Elasticsearch Error Handling

**Title:** Handle Elasticsearch failures gracefully

**Description:**
As a platform operator, I want analytics queries to handle ES failures gracefully so that users receive appropriate error messages.

**Acceptance Criteria:**
- [ ] Returns 503 when ES is unreachable
- [ ] Returns 500 with error ID when ES query fails
- [ ] Returns 504 when ES query exceeds 10 seconds
- [ ] Logs errors with full context
- [ ] Returns empty results (not error) when index doesn't exist

**Layer:** Backend
**Complexity:** S (2 days)
**Dependencies:** Story 2.4, 3.1, 5.1, 6.1

#### Subtask 7.1.0: Write Tests (RED)

#### Subtask 7.1.1: Add Error Handling to Repository

**Files to Modify:**

1. **AnalyticsElasticsearchRepository.java**
   - Add try-catch blocks
   - Detect different error types
   - Log with error IDs

#### Subtask 7.1.2: Add Custom Exceptions

**Files to Create:**

1. **AnalyticsTimeoutException.java**
2. **AnalyticsUnavailableException.java**
3. **AnalyticsQueryException.java**

#### Subtask 7.1.3: Update REST Resource

**Files to Modify:**

1. **ApiAnalyticsResource.java**
   - Map exceptions to HTTP status codes
   - Return error details

#### Subtask 7.1.4: Run Tests (GREEN)

---

