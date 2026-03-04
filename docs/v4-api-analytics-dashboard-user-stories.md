# V4 API Analytics Dashboard – User Stories

This document breaks down the V4 API Analytics Dashboard feature (PRD M1) into user stories. It is based on the existing code in:

- **Backend:** `gravitee-apim-rest-api/.../api/analytics/ApiAnalyticsResource.java` (unified `getApiV4Analytics` + separate endpoints), use cases, and `AnalyticsQueryService` / repository ES adapters.
- **Frontend:** `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/` (proxy dashboard, filters bar, request stats, response status ranges/overtime, response time over time) and `ApiAnalyticsV2Service`.

---

## 1. Backend: Unified analytics endpoint and COUNT

**Title:** Unified GET /v2/apis/{apiId}/analytics with COUNT type

**Description:**  
As an API publisher, I want to call a single analytics endpoint with `type=COUNT` so that I can get total request count for a v4 API over a time range without using a dedicated “requests-count” path.

**Acceptance criteria:**

- [Backend] GET `/v2/apis/{apiId}/analytics?type=COUNT&from=<ms>&to=<ms>` returns JSON: `{ "type": "COUNT", "count": <number> }`.
- [Backend] Query uses only the `*-v4-metrics-*` Elasticsearch index (v4 API metrics).
- [Backend] Missing or invalid `type` / `from` / `to` returns 400 with a clear message.
- [Backend] Endpoint enforces `API_ANALYTICS:READ` permission (AC #5).

**Layer:** Backend  

**Complexity:** M  

**Dependencies:** None  

**Notes:** Can delegate to existing `searchRequestsCount`-style logic or a dedicated COUNT path over v4-metrics; response shape must match the unified contract.

---

## 2. Backend: STATS analytics type

**Title:** Unified analytics endpoint returns STATS for a field

**Description:**  
As an API publisher, I want to request stats (min, max, avg, sum, count) for a given metric field so that I can show “Avg Gateway Response Time”, “Avg Upstream Response Time”, “Avg Content Length”, etc.

**Acceptance criteria:**

- [Backend] GET `/v2/apis/{apiId}/analytics?type=STATS&from=&to=&field=<field>` returns:  
  `{ "type": "STATS", "count": number, "min": number, "max": number, "avg": number, "sum": number }`.
- [Backend] Supported fields include at least: `status`, `mapped-status`, `application`, `plan`, `host`, `uri`, `gateway-latency-ms`, `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length` (mapped to ES field names as per PRD).
- [Backend] `field` is required for `type=STATS`; otherwise 400.
- [Backend] Data source is `*-v4-metrics-*` only.

**Layer:** Backend  

**Complexity:** M  

**Dependencies:** Story 1 (same unified endpoint and permission model).

---

## 3. Backend: GROUP_BY analytics type

**Title:** Unified analytics endpoint returns GROUP_BY (top-N by field)

**Description:**  
As an API publisher, I want to group metrics by a field (e.g. HTTP status) with top-N and optional sort so that I can power a “HTTP status” pie chart or other breakdowns.

**Acceptance criteria:**

- [Backend] GET `/v2/apis/{apiId}/analytics?type=GROUP_BY&from=&to=&field=<field>&size=10&order=...` returns:  
  `{ "type": "GROUP_BY", "values": { "<key>": count, ... }, "metadata": { "<key>": { "name": "..." }, ... } }`.
- [Backend] `field` is required for `type=GROUP_BY`; `size` (default 10) and `order` are optional.
- [Backend] Same supported fields as STATS where applicable; data from `*-v4-metrics-*` only.

**Layer:** Backend  

**Complexity:** M  

**Dependencies:** Story 1.

---

## 4. Backend: DATE_HISTO analytics type

**Title:** Unified analytics endpoint returns DATE_HISTO (time-bucketed histogram)

**Description:**  
As an API publisher, I want to get time-bucketed histograms (e.g. per status or per response time) so that I can render “response status over time” and “response time over time” from a single endpoint when desired.

**Acceptance criteria:**

- [Backend] GET `/v2/apis/{apiId}/analytics?type=DATE_HISTO&from=&to=&field=<field>&interval=<ms>` returns:  
  `{ "type": "DATE_HISTO", "timestamp": [ ... ], "values": [ { "field": "...", "buckets": [ ... ], "metadata": { ... } }, ... ] }`.
- [Backend] `field` and `interval` (ms) are required for `type=DATE_HISTO`; interval within a sensible range (e.g. 1000–1e9 ms).
- [Backend] Data source is `*-v4-metrics-*` only.

**Layer:** Backend  

**Complexity:** M  

**Dependencies:** Story 1.

---

## 5. Backend: Permission and validation

**Title:** Unified analytics endpoint enforces API_ANALYTICS:READ and validates parameters

**Description:**  
As a platform integrator, I want the unified analytics endpoint to enforce permissions and validate query parameters so that only authorized users get data and invalid requests are rejected clearly.

**Acceptance criteria:**

- [Backend] Endpoint enforces `API_ANALYTICS:READ` permission (AC #5).
- [Backend] Validation: `type` required; `from`/`to` required and `from` < `to`; `field` required for STATS and GROUP_BY; `interval` required for DATE_HISTO; appropriate 400 responses with message.

**Layer:** Backend  

**Complexity:** S  

**Dependencies:** Stories 1–4 (applies to the same endpoint).

---

## 6. Frontend: Angular service for unified endpoint

**Title:** ApiAnalyticsV2Service calls unified analytics endpoint for all four query types

**Description:**  
As a frontend developer, I want a single service method that calls the unified analytics API with type and optional field/interval/size/order so that all dashboard widgets can use one endpoint and stay consistent.

**Acceptance criteria:**

- [Frontend] Angular service (e.g. `ApiAnalyticsV2Service`) exposes a method (e.g. `getV4Analytics(apiId, params)`) that calls GET `/v2/apis/{apiId}/analytics` with query params: `type`, `from`, `to`, and when needed `field`, `interval`, `size`, `order` (AC #6).
- [Frontend] Response is typed for the four shapes: COUNT, STATS, GROUP_BY, DATE_HISTO (union or discriminated type).
- [Frontend] Errors (e.g. 4xx/5xx) are propagated so the UI can show an error/empty state.

**Layer:** Frontend  

**Complexity:** S  

**Dependencies:** Stories 1–4 (backend endpoint available).

---

## 7. Frontend: Stats cards (four metrics)

**Title:** Dashboard shows four stats cards from unified endpoint

**Description:**  
As an API publisher, I want to see total requests, avg gateway response time, avg upstream response time, and avg content length so that I can quickly assess traffic and performance.

**Acceptance criteria:**

- [Frontend] Stats cards show: Total Requests (COUNT), Avg Gateway Response Time (STATS on `gateway-response-time-ms`), Avg Upstream Response Time (STATS on `endpoint-response-time-ms`), Avg Content Length (STATS on `request-content-length`) (AC #7).
- [Frontend] Data is loaded from the unified endpoint (same time range as the rest of the dashboard).
- [Frontend] Cards show loading state and handle missing/empty data (e.g. “-” or 0).

**Layer:** Frontend  

**Complexity:** M  

**Dependencies:** Story 6.

---

## 8. Frontend: HTTP status pie chart

**Title:** Dashboard shows HTTP status pie chart from GROUP_BY

**Description:**  
As an API publisher, I want to see a pie chart of HTTP status codes (e.g. 200, 404, 500) so that I can quickly see the distribution of success vs errors.

**Acceptance criteria:**

- [Frontend] Dashboard includes an HTTP status pie chart (new or existing widget) (AC #8).
- [Frontend] Data comes from the unified endpoint with `type=GROUP_BY&field=status` (and appropriate `size`).
- [Frontend] Chart uses existing chart component patterns (e.g. `GioChartPie` / `api-analytics-response-status-ranges`) and fits the two-column layout (pie + “response status over time” line chart).

**Layer:** Frontend  

**Complexity:** M  

**Dependencies:** Story 6.

---

## 9. Frontend: Existing line charts unchanged

**Title:** Response status over time and response time over time charts still work

**Description:**  
As an API publisher, I want the existing “response status over time” and “response time over time” line charts to keep working so that my workflow is not broken when the dashboard is enhanced.

**Acceptance criteria:**

- [Frontend] “Response status over time” and “Response time over time” line charts still render and refresh with the selected time range (AC #9).
- [Frontend] They may keep using existing endpoints (e.g. `response-status-overtime`, `response-time-over-time`) or be wired to the unified endpoint DATE_HISTO; in either case behaviour and layout remain correct.
- [Frontend] No regressions in loading, empty data, or timeframe changes.

**Layer:** Frontend  

**Complexity:** S  

**Dependencies:** Story 6 (and optionally Story 4 if re-wiring to DATE_HISTO).

---

## 10. Frontend: Empty and error states

**Title:** Dashboard shows empty state when no data and error state when analytics unavailable

**Description:**  
As an API publisher, I want to see a clear empty state when there is no analytics data and an error state when the analytics service is unavailable so that I am not left with a blank or misleading screen.

**Acceptance criteria:**

- [Frontend] Empty state is displayed when there is no analytics data (e.g. no hits in range) (AC #10).
- [Frontend] Error state is displayed when the analytics service is unavailable (e.g. request failure); message is user-friendly (e.g. “Analytics unavailable”).
- [Frontend] Timeframe filter and other controls remain usable where appropriate (e.g. retry or change range).

**Layer:** Frontend  

**Complexity:** S  

**Dependencies:** Stories 7, 8, 9 (widgets that can be empty or fail).

---

## 11. Tests: Backend analytics query types

**Title:** Unit tests for all backend analytics query types (COUNT, STATS, GROUP_BY, DATE_HISTO)

**Description:**  
As a developer, I want unit tests for each analytics query type so that regressions are caught and the contract (response shape, validation, permission) is documented.

**Acceptance criteria:**

- [Tests] Unit tests for the unified endpoint (or equivalent logic) cover: COUNT, STATS, GROUP_BY, DATE_HISTO (AC #11).
- [Tests] Tests verify response shape and key values (e.g. count, min/max/avg for STATS; keys in GROUP_BY values).
- [Tests] Tests verify validation (missing/invalid params → 400) and permission (e.g. 403 when lacking API_ANALYTICS:READ).
- [Tests] Repository/query layer tests cover ES query building or adapters where feasible (e.g. v4-metrics index, field mapping).

**Layer:** Backend (tests)  

**Complexity:** M  

**Dependencies:** Stories 1–4, 5.

---

## 12. Tests: Angular dashboard and widgets

**Title:** Angular component tests for analytics dashboard and widgets

**Description:**  
As a developer, I want component tests for the analytics dashboard and its widgets so that we avoid regressions when changing the unified endpoint or the layout.

**Acceptance criteria:**

- [Tests] Angular component tests for the dashboard and widgets (AC #12).
- [Tests] Tests cover: stats cards (values from unified endpoint), HTTP status pie chart (GROUP_BY data), and that line charts receive data (or mocks) and render.
- [Tests] Tests cover empty state and error state when the service returns no data or fails.
- [Tests] Tests use existing patterns (e.g. harnesses, `ApiAnalyticsV2Service` mock, time range filter).

**Layer:** Frontend (tests)  

**Complexity:** M  

**Dependencies:** Stories 6–10.

---

## Summary matrix

| #  | Story summary                          | Layer   | Complexity | Deps   |
|----|----------------------------------------|---------|------------|--------|
| 1  | Unified endpoint + COUNT               | Backend | M          | –      |
| 2  | STATS type                             | Backend | M          | 1      |
| 3  | GROUP_BY type                          | Backend | M          | 1      |
| 4  | DATE_HISTO type                        | Backend | M          | 1      |
| 5  | Permission & validation                | Backend | S          | 1–4    |
| 6  | Angular service for unified API        | Frontend| S          | 1–4    |
| 7  | Four stats cards                       | Frontend| M          | 6      |
| 8  | HTTP status pie chart                  | Frontend| M          | 6      |
| 9  | Existing line charts unchanged         | Frontend| S          | 6      |
| 10 | Empty & error states                   | Frontend| S          | 7–9    |
| 11 | Backend unit tests                     | Backend | M          | 1–5    |
| 12 | Angular component tests                | Frontend| M          | 6–10   |

---

## Suggested implementation order

1. **Backend:** 1 → 2 → 3 → 4 → 5 (unified endpoint and all types, then permission/validation).
2. **Frontend:** 6 → 7, 8, 9 in parallel if desired → 10 (service, then widgets, then empty/error).
3. **Tests:** 11 after 1–5; 12 after 6–10.

This aligns with the PRD (unified GET with flexible field selection) and the given acceptance criteria (AC #1–12).
