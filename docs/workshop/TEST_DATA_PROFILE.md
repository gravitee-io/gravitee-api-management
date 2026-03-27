# Test Data Profile

## Generated Traffic Summary

| Metric | Value |
|--------|-------|
| Total Requests | ~120 |
| HTTP Methods | GET, POST, PUT, DELETE |
| Status Codes | 200, 201, 400, 404, 500, 502, 503 |
| Payload Sizes | 0B (GET), ~30B (small JSON), ~5KB (large JSON) |
| Time Pattern | Steady stream + burst spike |

## Request Distribution

| Request Type | Method | Path | Expected Status | Count |
|-------------|--------|------|----------------|-------|
| Basic GET | GET | /get | 200 | 30 |
| Basic POST | POST | /post | 200 | 20 |
| Update PUT | PUT | /put | 200 | 10 |
| Delete | DELETE | /delete | 200 | 5 |
| Created | POST | /status/201 | 201 | 10 |
| Bad Request | GET | /status/400 | 400 | 8 |
| Not Found | GET | /status/404 | 404 | 7 |
| Server Error | POST | /status/500 | 500 | 5 |
| Bad Gateway | GET | /status/502 | 502 | 3 |
| Unavailable | GET | /status/503 | 503 | 2 |
| Large Payload | POST | /post | 200 | 5 |
| Burst | GET | /get | 200 | 15 |

## Expected Widget Values

### Stats Cards

| Widget | Expected |
|--------|----------|
| Total Requests | ~120 |
| Avg Gateway Response Time | 50–300ms (varies by network) |
| Avg Upstream Response Time | 50–300ms (httpbin.org latency) |
| Avg Content Length | Mix of 0B GETs and ~5KB POSTs |

### HTTP Status Pie Chart (GROUP_BY)

| Status | Count | % (approx) | Color |
|--------|-------|------------|-------|
| 200 | ~80 | ~67% | Green |
| 201 | ~10 | ~8% | Green |
| 400 | ~8 | ~7% | Orange |
| 404 | ~7 | ~6% | Orange |
| 500 | ~5 | ~4% | Red |
| 502 | ~3 | ~2.5% | Red |
| 503 | ~2 | ~1.5% | Red |

### Response Status Ranges (existing widget)

| Range | Count | % (approx) |
|-------|-------|------------|
| 2xx | ~90 | ~75% |
| 4xx | ~15 | ~12.5% |
| 5xx | ~10 | ~8% |

### Time-Series Charts

- **Response Status Over Time**: Should show steady 200s with occasional 4xx/5xx, plus a burst spike
- **Response Time Over Time**: Should show consistent latency with slight variation from large payloads

## Edge Cases Exercised

1. **Multiple status codes** → pie chart shows multiple slices with distinct colors
2. **Large payloads** → content-length stats show meaningful average
3. **Burst traffic** → time-series shows spike in histogram
4. **Error codes** → 4xx orange + 5xx red segments visible in pie chart
5. **Multiple HTTP methods** → exercises POST/PUT/DELETE paths
