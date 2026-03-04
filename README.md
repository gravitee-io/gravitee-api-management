# Gravitee.io API Management

<p style="text-align: center;">
  <img src="./assets/gravitee-logo-dark.svg#gh-light-mode-only" width="400" alt="Gravitee Dark Logo">
  <img src="./assets/gravitee-logo-white.svg#gh-dark-mode-only" width="400" alt="Gravitee White Logo">
</p>

<h1 style="text-align: center;">API Management</h1>
<p style="text-align: center;">Flexible and blazing-fast Open Source API Gateway.</p>

<br/>

<p style="text-align: center;">
  <a href="https://circleci.com/gh/gravitee-io/gravitee-api-management">
    <img src="https://circleci.com/gh/gravitee-io/gravitee-api-management.svg?style=svg" alt="Build Status on CircleCI" />
  </a>
  <a href="https://github.com/gravitee-io/gravitee-api-management/blob/master/LICENSE.txt">
    <img src="https://img.shields.io/github/license/gravitee-io/gravitee-api-management.svg" alt="License" />
  </a>
  <a href="https://gravitee.io">
    <img src="https://img.shields.io/snyk/vulnerabilities/github/gravitee-io/gravitee-api-management" alt="Snyk Vulnerabilities for GitHub Repo" />
  </a>
<br/>
  <a href="https://community.gravitee.io">
    <img src="https://img.shields.io/badge/community-join-4BC424.svg" alt="Gravitee.io Community" />
  </a>
  <a href="https://documentation.gravitee.io/apim">
    <img src="https://img.shields.io/badge/documentation-see-4BC424.svg" alt="Gravitee.io Documentation" />
  </a>
  <a href="https://twitter.com/intent/follow?screen_name=graviteeio">
    <img src="https://img.shields.io/badge/twitter-follow-4BC424.svg" alt="Official Twitter Handle" />
  </a>
</p>

## Table of contents

-   [Overview](#-overview)
-   [Features](#-features)
-   [V4 API Analytics Dashboard](#v4-api-analytics-dashboard)
-   [Documentation](#-documentation)
-   [Community](#-community)
-   [Contributing](#-contributing)
-   [License](#-license)

## Overview

Gravitee.io API Management is a flexible, lightweight, and blazing-fast Open Source solution that helps your organization control who, when, and how users access your APIs.
Effortlessly manage the lifecycle of your APIs.
Download API Management to document, discover, and publish your APIs.

## Features

-   Register your API: Create and register APIs in a few clicks to easily expose your secured APIs to internal and external consumers.
-   Configure policies using flows: Gravitee.io API Management provides over 50 pre-built policies to effectively shape
    traffic reaching the gateway according to your business requirements.
-   Developer portal: Build the portal that your developers want with a custom theme, full-text search, and API documentation.
-   Analytics dashboard: The out-of-the-box dashboards give you a 360-degree view of your API. The **V4 API Analytics Dashboard** (see below) provides a unified analytics experience for V4 HTTP Proxy APIs.
-   Register applications: Users and administrators can register applications for consuming APIs with ease.
-   Secured plans: Create plans to define the rate limits, quotas, and security policies that apply to your APIs.

---

## V4 API Analytics Dashboard

The V4 API Analytics Dashboard gives API publishers and consumers a single view of traffic for **V4 HTTP Proxy APIs**: total requests, response times, content size, and HTTP status breakdown. Data is read from the **\*-v4-metrics-\*** Elasticsearch index only. Access requires the **API_ANALYTICS:READ** permission.

### Analytics endpoint API

**Base path:** `GET /management/v2/environments/{envId}/apis/{apiId}/analytics`

**Required query parameters (all types):**

| Parameter | Type   | Description                    |
|-----------|--------|--------------------------------|
| `type`    | string | One of: `COUNT`, `STATS`, `GROUP_BY`, `DATE_HISTO` |
| `from`    | long   | Start time (epoch milliseconds) |
| `to`      | long   | End time (epoch milliseconds), must be &gt; `from` |

**Optional parameters:** `field` (required for STATS, GROUP_BY, DATE_HISTO), `interval` (required for DATE_HISTO, ms, 1000–1000000000), `size` (GROUP_BY, default 10), `order` (GROUP_BY).

**Supported fields (for STATS / GROUP_BY / DATE_HISTO):**  
`status`, `mapped-status`, `application`, `plan`, `host`, `uri`, `gateway-latency-ms`, `gateway-response-time-ms`, `endpoint-response-time-ms`, `request-content-length`.

---

#### 1. COUNT

Total number of request hits in the time range.

**Example request:**

```http
GET /management/v2/environments/DEFAULT/apis/my-api-id/analytics?type=COUNT&from=1700000000000&to=1700086400000
```

**Example response (200):**

```json
{
  "type": "COUNT",
  "count": 12345
}
```

---

#### 2. STATS

Min, max, avg, sum and count for a numeric field over the time range.

**Example request:**

```http
GET /management/v2/environments/DEFAULT/apis/my-api-id/analytics?type=STATS&from=1700000000000&to=1700086400000&field=gateway-response-time-ms
```

**Example response (200):**

```json
{
  "type": "STATS",
  "count": 12345,
  "min": 2.0,
  "max": 1500.0,
  "avg": 125.5,
  "sum": 1549567.0
}
```

---

#### 3. GROUP_BY

Top-N buckets by a field (terms aggregation), with optional metadata.

**Example request:**

```http
GET /management/v2/environments/DEFAULT/apis/my-api-id/analytics?type=GROUP_BY&from=1700000000000&to=1700086400000&field=status&size=10
```

**Example response (200):**

```json
{
  "type": "GROUP_BY",
  "values": {
    "200": 10000,
    "404": 1500,
    "500": 845
  },
  "metadata": {
    "200": { "name": "200" },
    "404": { "name": "404" },
    "500": { "name": "500" }
  }
}
```

---

#### 4. DATE_HISTO

Time-bucketed histogram: shared `timestamp` array and per-field bucket counts in `values`.

**Example request:**

```http
GET /management/v2/environments/DEFAULT/apis/my-api-id/analytics?type=DATE_HISTO&from=1700000000000&to=1700086400000&field=status&interval=3600000
```

**Example response (200):**

```json
{
  "type": "DATE_HISTO",
  "timestamp": [1700000000000, 1700003600000, 1700007200000],
  "values": [
    {
      "field": "200",
      "buckets": [120, 130, 115],
      "metadata": { "name": "200" }
    },
    {
      "field": "500",
      "buckets": [2, 5, 1],
      "metadata": { "name": "500" }
    }
  ]
}
```

---

### Dashboard layout and widgets

Location: **Console → APIs → [V4 API] → API Traffic → Analytics**.

| Row | Content | Data source |
|-----|---------|-------------|
| 1   | **Timeframe filter bar** | Predefined ranges + optional custom range; drives all widgets. |
| 2   | **Stats cards** (horizontal) | Total Requests (COUNT), Avg Gateway Response Time (STATS on `gateway-response-time-ms`), Avg Upstream Response Time (STATS on `endpoint-response-time-ms`), Avg Content Length (STATS on `request-content-length`). |
| 3   | **Left:** HTTP Status pie chart | GROUP_BY on `status` (top 10). |
| 3   | **Right:** Response status over time (line chart) | Existing endpoint: `/analytics/response-status-overtime`. |
| 4   | **Full width:** Response time over time (line chart) | Existing endpoint: `/analytics/response-time-over-time`. |

**States:**

- **Loading:** Spinner while API or time range is resolving, or while unified analytics are loading.
- **Analytics disabled:** Empty state prompting to enable Analytics in API settings.
- **Error:** “Analytics unavailable” when any unified request fails.
- **No data:** “No analytics data for this period” when the period has zero requests.

---

### Timeframe filter

- **Predefined ranges:** Last 5 min, Last hour, Last 24 hours, Last 7 days, Last 30 days. Each maps to a fixed `from`/`to` (and, for line charts, an interval) relative to “now” (UTC).
- **Custom range:** Optional date/time pickers for “From” and “To”; “Apply” sets the same `from`/`to` for all widgets.
- **Refresh:** Button re-applies the current range and triggers a full re-fetch of all widgets.

The selected range is held in `ApiAnalyticsV2Service` (`timeRangeFilter$`). Changing it causes the dashboard to re-query the unified endpoint (and the existing line-chart endpoints) with the new `from`/`to`.

---

### Architecture (data flow)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Console (Angular)                                                           │
│  ┌──────────────────────┐  ┌─────────────────────────────────────────────┐ │
│  │ API Traffic >         │  │ ApiAnalyticsProxyComponent                    │ │
│  │ Analytics tab        │  │ - apiAnalyticsVM$ (loading / stats / error /  │ │
│  │                      │  │   no-data / grid)                             │ │
│  │                      │  │ - v4AnalyticsData$ → forkJoin(COUNT, 3×STATS, │ │
│  │                      │  │   GROUP_BY status)                            │ │
│  └──────────┬───────────┘  └─────────────────────┬───────────────────────────┘ │
│             │                                   │                             │
│             │  timeRangeFilter()                 │  getV4Analytics(apiId,      │
│             │  setTimeRangeFilter()              │    { type, from, to, ... }) │
│             ▼                                   ▼                             │
│  ┌──────────────────────┐  ┌─────────────────────────────────────────────┐   │
│  │ api-analytics-       │  │ ApiAnalyticsV2Service                        │   │
│  │ filters-bar          │  │ - getV4Analytics() → GET .../analytics?type=…│   │
│  │ (predefined + custom)│  │ - getResponseStatusOvertime() /                │   │
│  └──────────────────────┘  │   getResponseTimeOverTime() (existing)      │   │
│                             └─────────────────────┬───────────────────────┘   │
└─────────────────────────────────────────────────────┼─────────────────────────┘
                                                      │ HTTP
                                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Management REST API (JAX-RS)                                                 │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ ApiAnalyticsResource (v2)                                               │  │
│  │ GET .../apis/{apiId}/analytics?type=COUNT|STATS|GROUP_BY|DATE_HISTO     │  │
│  │   → AnalyticsQueryService.searchV4Analytics*(...)                       │  │
│  │ GET .../analytics/response-status-overtime | response-time-over-time    │  │
│  │   → (existing use cases)                                               │  │
│  └─────────────────────────────┬─────────────────────────────────────────┘  │
│                                │                                              │
│                                ▼                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ AnalyticsQueryServiceImpl                                              │  │
│  │ - searchV4AnalyticsCount / Stats / GroupBy / DateHisto                 │  │
│  │   → AnalyticsRepository (searchRequestsCount, searchStats,              │  │
│  │      searchGroupBy, searchDateHisto)                                  │  │
│  └─────────────────────────────┬─────────────────────────────────────────┘  │
└────────────────────────────────┼────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Repository layer (e.g. Elasticsearch)                                        │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ AnalyticsRepository (v4)                                              │  │
│  │ - Index: *-v4-metrics-*                                                 │  │
│  │ - COUNT: count aggregation over filtered request docs                   │  │
│  │ - STATS: stats aggregation on given field                              │  │
│  │ - GROUP_BY: terms aggregation + size/order                              │  │
│  │ - DATE_HISTO: date_histogram + terms sub-aggregation                   │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Flow summary:** Console uses the timeframe to call the unified analytics endpoint (and existing endpoints). The REST resource validates parameters and calls `AnalyticsQueryService`. The service delegates to `AnalyticsRepository`, which runs the appropriate query against the v4-metrics Elasticsearch index and returns the result to the API and then to the UI.

---

### How to extend

- **New widget type (dashboard):**  
  - Add a new component (or reuse an existing chart/stat component).  
  - In `ApiAnalyticsProxyComponent`, extend `v4AnalyticsData$` or add a separate observable that calls `ApiAnalyticsV2Service.getV4Analytics()` with the desired `type` and `field`.  
  - Add the component to the template and pass the resolved data (or bind to the same time range and let the component call the service).

- **New query type (backend):**  
  - Add a new `case` in `ApiAnalyticsResource.getApiV4Analytics()` with validation and a call to a new method on `AnalyticsQueryService`.  
  - Extend `AnalyticsQueryService` and `AnalyticsQueryServiceImpl` to delegate to `AnalyticsRepository`.  
  - In the repository layer, add the new query/aggregation (e.g. a new adapter for the ES request/response).  
  - Update the OpenAPI spec and the Angular service types and `getV4Analytics()` if the new type needs different params or response shape.

- **New filterable field:**  
  - Backend: Ensure the repository and ES mapping support the new field (e.g. in `V4ApiAnalyticsFieldMapping` or equivalent); the unified endpoint already accepts a generic `field` parameter.  
  - Frontend: Use the new field name in existing widgets (e.g. `field: 'my-new-field'`) or add a widget that calls `getV4Analytics(..., { type: 'STATS', field: 'my-new-field', ... })`.  
  - Document the field in the API description (e.g. OpenAPI and this README).

---

## Documentation

You can find Gravitee.io API Management's documentation on the [dedicated website](https://documentation.gravitee.io/apim/).

## Community

Got questions, suggestions or feedback? Why not join us on the [Gravitee.io Community Forum](https://community.gravitee.io/).

## Contributing

We welcome contributions! Please read the dedicated [CONTRIBUTING](./CONTRIBUTING.adoc) guide for more info.

## License

Gravitee.io API Management is licensed under the [Apache License, Version 2.0](./LICENSE.txt).
