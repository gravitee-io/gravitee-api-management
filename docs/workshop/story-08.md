# Story 08: Dashboard Assembly & States

**Layer:** Frontend | **Complexity:** M | **Status:** ⬜ Pending
**Dependencies:** Stories 06, 07

---

## User Story

**As an** API Publisher,
**I want** a fully assembled analytics dashboard with correct loading, empty, and error states,
**so that** the page is reliable and coherent regardless of data availability.

---

## Acceptance Criteria

- [ ] Dashboard layout (top to bottom): filter bar → 4 stats cards → [pie chart | status overtime] → response time over time
- [ ] **Existing charts** (`ApiAnalyticsResponseStatusOvertimeComponent`, `ApiAnalyticsResponseTimeOverTimeComponent`) continue to work unchanged — they call their own endpoints internally, not rewired
- [ ] **Empty state** (`GioCardEmptyStateModule`) displayed when `api.analytics.enabled === false`
- [ ] **Error state**: each new unified endpoint call is wrapped with `catchError(() => of({ isLoading: false }))` so one failure doesn't break the whole dashboard
- [ ] Timeframe change triggers re-fetch of all 6 unified endpoint calls (4x STATS/COUNT + 1x GROUP_BY + 1 filter sync)
- [ ] Full proxy component spec passes: loading, disabled analytics, enabled analytics (all 8 HTTP calls), refresh cycle

---

## Full HTTP Call Inventory (analytics.enabled=true)

```
GET /apis/{id}
GET /apis/{id}/analytics?type=COUNT&from=...&to=...
GET /apis/{id}/analytics?type=STATS&field=gateway-response-time-ms&from=...&to=...
GET /apis/{id}/analytics?type=STATS&field=endpoint-response-time-ms&from=...&to=...
GET /apis/{id}/analytics?type=STATS&field=request-content-length&from=...&to=...
GET /apis/{id}/analytics?type=GROUP_BY&field=status&size=10&from=...&to=...
GET /apis/{id}/analytics/response-status-overtime?from=...&to=...    ← existing, unchanged
GET /apis/{id}/analytics/response-time-over-time?from=...&to=...     ← existing, unchanged
```

---

## Files

| Action | File |
|--------|------|
| **Modify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts` |
| **Modify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.html` |
| **Modify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.spec.ts` |
| **Verify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.harness.ts` |

---

## Updated `ApiAnalyticsVM` Type

```typescript
type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  requestStats?: AnalyticsRequestStats;                           // 4 cards
  responseStatusRanges?: ApiAnalyticsResponseStatusRanges;       // pie
};
```

---

## Observable Wiring in Component

```typescript
private analyticsData$ = combineLatest([
  this.count$.pipe(catchError(() => of({ isLoading: false }))),
  this.gwStats$.pipe(catchError(() => of({ isLoading: false }))),
  this.upStats$.pipe(catchError(() => of({ isLoading: false }))),
  this.clStats$.pipe(catchError(() => of({ isLoading: false }))),
  this.groupByStatus$.pipe(catchError(() => of({ isLoading: false }))),
]).pipe(
  map(([count, gwStats, upStats, clStats, groupByStatus]) => ({
    requestStats: [
      { label: 'Total Requests',                    value: count.count,     isLoading: count.isLoading },
      { label: 'Avg GW Response Time',   unitLabel: 'ms', value: gwStats.avg,    isLoading: gwStats.isLoading },
      { label: 'Avg Upstream Response Time', unitLabel: 'ms', value: upStats.avg, isLoading: upStats.isLoading },
      { label: 'Avg Content Length',     unitLabel: 'B',  value: clStats.avg,   isLoading: clStats.isLoading },
    ],
    responseStatusRanges: {
      isLoading: groupByStatus.isLoading,
      data: Object.entries(bucketStatusCodes(groupByStatus.values ?? {}))
                  .map(([label, value]) => ({ label, value: toNumber(value) })),
    },
  })),
);
```

---

## Dashboard Layout (HTML)

```html
<!-- Row 1: Filter bar -->
<api-analytics-filters-bar />

<!-- Row 2: Stats cards -->
@if (vm.requestStats) {
  <app-api-analytics-request-stats title="Request Stats" [requestsStats]="vm.requestStats" />
}

<!-- Row 3: Two-column -->
<div class="gridContent">
  <api-analytics-response-status-ranges
    title="Response Status"
    [responseStatusRanges]="vm.responseStatusRanges" />
  <api-analytics-response-status-overtime />
</div>

<!-- Row 4: Full-width -->
<api-analytics-response-time-over-time class="full-bleed" />
```

---

## Tests to Update in Proxy Spec

Update the helper function `expectAllAnalyticsCall()` to expect the new set of HTTP calls:
```typescript
function expectAllAnalyticsCall() {
  expectApiGetRequest(fakeApiV4({ id: API_ID, analytics: { enabled: true } }));
  expectApiGetResponseStatusOvertime();
  expectApiGetResponseTimeOverTime();
  expectAnalyticsCount();
  expectAnalyticsStats('gateway-response-time-ms');
  expectAnalyticsStats('endpoint-response-time-ms');
  expectAnalyticsStats('request-content-length');
  expectAnalyticsGroupBy('status');
}
```

Add graceful degradation test:
- `should show stats cards even when one stat call fails` → flush 3 STATS, error on 1 → assert remaining 3 cards still show values
