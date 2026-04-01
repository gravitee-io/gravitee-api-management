# Story 05: Frontend Service for Unified Endpoint

**Layer:** Frontend | **Complexity:** S | **Status:** ⬜ Pending
**Dependencies:** Stories 01–04 (backend endpoint complete)

---

## User Story

**As a** frontend developer,
**I want** `ApiAnalyticsV2Service` to expose typed methods for the unified analytics endpoint,
**so that** dashboard components have a clean, reactive API instead of constructing raw HTTP calls.

---

## Acceptance Criteria

- [ ] `getAnalyticsCount(apiId)` calls `GET .../analytics?type=COUNT&from=...&to=...`
- [ ] `getAnalyticsStats(apiId, field)` calls `GET .../analytics?type=STATS&field=...&from=...&to=...`
- [ ] `getAnalyticsGroupBy(apiId, field, size?)` calls `GET .../analytics?type=GROUP_BY&field=...&size=...&from=...&to=...`; `size` defaults to 10
- [ ] All methods use the existing `timeRangeFilter$` pattern: `filter(Boolean)` + `switchMap({ from, to })`
- [ ] TypeScript interfaces `AnalyticsCount`, `AnalyticsStats`, `AnalyticsGroupBy` defined in `analyticsUnified.ts`
- [ ] Service spec tests use `HttpTestingController` (no mocks) covering all three methods
- [ ] `should not fetch when timeRangeFilter is null` test passes

---

## Files

| Action | File |
|--------|------|
| **Create** | `src/entities/management-api-v2/analytics/analyticsUnified.ts` |
| **Modify** | `src/services-ngx/api-analytics-v2.service.ts` |
| **Modify** | `src/services-ngx/api-analytics-v2.service.spec.ts` |

---

## TypeScript Interfaces

```typescript
// analyticsUnified.ts
export interface AnalyticsCount  { count: number; }
export interface AnalyticsStats  { count: number; min: number; max: number; avg: number; sum: number; }
export interface AnalyticsGroupBy {
  values: Record<string, number>;
  metadata: Record<string, Record<string, string>>;
}
```

---

## Service Implementation Pattern

```typescript
// Follow existing method pattern exactly:
getAnalyticsCount(apiId: string): Observable<AnalyticsCount> {
  return this.timeRangeFilter$.pipe(
    filter((data) => !!data),
    switchMap(({ from, to }) =>
      this.http.get<AnalyticsCount>(
        `${this.constants.env.v2BaseURL}/apis/${apiId}/analytics`,
        { params: { type: 'COUNT', from, to } },
      ),
    ),
  );
}
```

---

## Tests to Write (HttpTestingController)

- `should call unified endpoint with type=COUNT`
- `should call unified endpoint with type=STATS and field param`
- `should call unified endpoint with type=GROUP_BY with field, size, from, to`
- `should not fetch when timeRangeFilter is null`
- `should re-fetch on timeRangeFilter change`
