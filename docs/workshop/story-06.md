# Story 06: Stats Cards Row

**Layer:** Frontend | **Complexity:** S | **Status:** ⬜ Pending
**Dependencies:** Story 05

---

## User Story

**As an** API Publisher,
**I want** four stats cards showing Total Requests, Avg GW Response Time, Avg Upstream Response Time, and Avg Content Length,
**so that** key performance indicators are visible at a glance, sourced from the unified endpoint.

---

## Acceptance Criteria

- [ ] Dashboard shows 4 stats cards (replacing the current 2)
- [ ] **Total Requests** sources from `getAnalyticsCount(apiId)` → `.count`
- [ ] **Avg GW Response Time (ms)** sources from `getAnalyticsStats(apiId, 'gateway-response-time-ms')` → `.avg`
- [ ] **Avg Upstream Response Time (ms)** sources from `getAnalyticsStats(apiId, 'endpoint-response-time-ms')` → `.avg`
- [ ] **Avg Content Length (B)** sources from `getAnalyticsStats(apiId, 'request-content-length')` → `.avg`
- [ ] Each card shows spinner while loading; shows `—` when data is unavailable
- [ ] Cards refresh when timeframe filter changes
- [ ] Legacy `requests-count` and `average-connection-duration` endpoint calls are removed
- [ ] Proxy component spec updated: old HTTP expectations replaced, 4 card values asserted

---

## Files

| Action | File |
|--------|------|
| **Modify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts` |
| **Modify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.spec.ts` |

---

## requestStats Array (4 entries)

```typescript
requestStats: [
  { label: 'Total Requests',                value: count.count,   isLoading },
  { label: 'Avg GW Response Time',  unitLabel: 'ms', value: gwStats.avg,  isLoading },
  { label: 'Avg Upstream Response Time', unitLabel: 'ms', value: upStats.avg, isLoading },
  { label: 'Avg Content Length',    unitLabel: 'B',  value: clStats.avg,  isLoading },
]
```

---

## Key Change in Component

Replace 2 observables:
```typescript
// BEFORE
private getRequestsCount$          = apiAnalyticsV2Service.getRequestsCount(apiId)
private getAverageConnectionDuration$ = apiAnalyticsV2Service.getAverageConnectionDuration(apiId)

// AFTER
private count$    = apiAnalyticsV2Service.getAnalyticsCount(apiId)
private gwStats$  = apiAnalyticsV2Service.getAnalyticsStats(apiId, 'gateway-response-time-ms')
private upStats$  = apiAnalyticsV2Service.getAnalyticsStats(apiId, 'endpoint-response-time-ms')
private clStats$  = apiAnalyticsV2Service.getAnalyticsStats(apiId, 'request-content-length')
```

`combineLatest([count$, gwStats$, upStats$, clStats$, ...])` — all wrapped with `catchError`.

---

## Tests to Update in Proxy Spec

- `should display HTTP Proxy Entrypoint - Request Stats`
  - Change HTTP expectations from `/requests-count` + `/average-connection-duration` to `?type=COUNT` + `?type=STATS&field=...` (3x)
  - Assert 4 card values instead of 2
- `should refresh`
  - Assert 4 unified calls re-issued after timeframe change
