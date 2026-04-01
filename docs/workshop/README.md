# V4 API Analytics Dashboard — Workshop Stories

## Overview

This workshop implements the V4 API Analytics Dashboard for the Gravitee API Management Console. It replaces separate per-metric analytics endpoints with a unified `GET /v2/apis/{apiId}/analytics` endpoint and enhances the Console dashboard with new stats cards and a GROUP_BY-powered status pie chart.

## Dependency Graph

```
✅ Story 01 (COUNT) ─────┐
✅ Story 02 (STATS) ─────┤
✅ Story 03 (GROUP_BY) ──┤──→ ⬜ Story 05 (FE Service) ─┬─→ ⬜ Story 06 (Stats Cards) ─┐
✅ Story 04 (DATE_HISTO) ┘                               └─→ ⬜ Story 07 (Pie Chart)   ─┤
                                                                                          └─→ ⬜ Story 08 (Assembly)
```

## Suggested Implementation Order

1. **Stories 01 → 02 → 03 → 04** — Backend (sequential, each builds on shared infrastructure) ✅ Complete
2. **Story 05** — Frontend service (needs backend done)
3. **Story 06** — Stats cards row (needs Story 05)
4. **Story 07** — HTTP status pie chart (needs Story 05)
5. **Story 08** — Dashboard assembly (needs Stories 06 + 07)

## Stories

| # | Title | Layer | Complexity | Status | File |
|---|-------|-------|------------|--------|------|
| 01 | Unified Analytics Endpoint — COUNT | Backend | M | ✅ Complete | [story-01.md](./story-01.md) |
| 02 | Unified Analytics Endpoint — STATS | Backend | M | ✅ Complete | [story-02.md](./story-02.md) |
| 03 | Unified Analytics Endpoint — GROUP_BY | Backend | L | ✅ Complete | [story-03.md](./story-03.md) |
| 04 | Unified Analytics Endpoint — DATE_HISTO | Backend | L | ✅ Complete | [story-04.md](./story-04.md) |
| 05 | Frontend Service for Unified Endpoint | Frontend | S | ⬜ Pending | [story-05.md](./story-05.md) |
| 06 | Stats Cards Row | Frontend | S | ⬜ Pending | [story-06.md](./story-06.md) |
| 07 | HTTP Status Pie Chart | Frontend | S | ⬜ Pending | [story-07.md](./story-07.md) |
| 08 | Dashboard Assembly & States | Frontend | M | ⬜ Pending | [story-08.md](./story-08.md) |
