# Story 07: HTTP Status Pie Chart

**Layer:** Frontend | **Complexity:** S | **Status:** ⬜ Pending
**Dependencies:** Story 05

---

## User Story

**As an** API Publisher,
**I want** the HTTP status pie chart sourced from the unified GROUP_BY endpoint,
**so that** status distribution data is accurate and consistent with the new analytics backend.

---

## Acceptance Criteria

- [ ] Pie chart data sourced from `getAnalyticsGroupBy(apiId, 'status')` (replaces `getResponseStatusRanges`)
- [ ] Individual status codes (200, 201, 404, 500…) are bucketed into 5 ranges: 1xx, 2xx, 3xx, 4xx, 5xx
- [ ] Bucket range keys match the format expected by `ApiAnalyticsResponseStatusRangesComponent`: `"100.0-200.0"`, `"200.0-300.0"`, `"300.0-400.0"`, `"400.0-500.0"`, `"500.0-600.0"`
- [ ] Chart shows spinner while loading; shows empty state when no data
- [ ] Legacy `response-status-ranges` endpoint call is removed
- [ ] Proxy component spec updated: old expectation replaced with `?type=GROUP_BY&field=status`

---

## Files

| Action | File |
|--------|------|
| **Modify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.ts` |
| **Modify** | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/api-analytics-proxy.component.spec.ts` |

---

## Bucketing Mapper

Add as a pure function in the proxy component (not in the service or child component):

```typescript
function bucketStatusCodes(values: Record<string, number>): Record<string, number> {
  const ranges: Record<string, number> = {};
  for (const [code, count] of Object.entries(values)) {
    const n = Number(code);
    const key =
      n < 200 ? '100.0-200.0'
      : n < 300 ? '200.0-300.0'
      : n < 400 ? '300.0-400.0'
      : n < 500 ? '400.0-500.0'
               : '500.0-600.0';
    ranges[key] = (ranges[key] ?? 0) + count;
  }
  return ranges;
}
```

Then pass to `responseStatusRanges.data`:
```typescript
data: Object.entries(bucketStatusCodes(groupBy.values ?? {}))
            .map(([label, value]) => ({ label, value }))
```

---

## Tests to Update in Proxy Spec

- `should display Response Status`
  - Change HTTP expectation from `/response-status-ranges` to `?type=GROUP_BY&field=status`
  - Flush `{ values: { '200': 60, '404': 1, '500': 1 }, metadata: {} }`
  - Assert pie chart renders with data
