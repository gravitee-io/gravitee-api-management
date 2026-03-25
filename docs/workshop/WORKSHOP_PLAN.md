# V4 API Analytics Dashboard — Workshop Plan

## Stories Summary (Revised)

| # | Story | Layer | Size | Dependencies |
|---|-------|-------|------|-------------|
| S1 | Unified Endpoint Scaffolding + COUNT | Backend | M | None |
| S2 | STATS Endpoint + ES Adapter | Backend | L | S1 |
| S3 | GROUP_BY Endpoint + ES Adapter | Backend | L | S1 |
| S4 | DATE_HISTO Endpoint + ES Adapter | Backend | L | S1 |
| S5 | Angular Unified Analytics Service | Frontend | S | Contract from S1 |
| S6 | Enhanced Stats Cards + Dashboard Layout | Frontend | M | S5 |
| S7 | HTTP Status Pie Chart Widget | Frontend | S | S5 |

**7 stories total** (reduced from 8 — merged original S6+S8).

## Implementation Order

```
S1 (scaffolding + COUNT)
├── S2 (STATS + ES)      ─┐
├── S3 (GROUP_BY + ES)   ─┼── S5 (FE service) ──┬── S6 (stats + layout)
└── S4 (DATE_HISTO + ES) ─┘                      └── S7 (pie chart)
```

## Time Tracking

| Phase | Planned | Actual |
|-------|---------|--------|
| Phase 1 — Decomposition | 30–45 min | ___ |
| Phase 2 — Backend Stories (S1–S4) | 60–75 min | ___ |
| Phase 2 — Frontend Stories (S5–S7) | 60–75 min | ___ |
| Phase 3 — Documentation | 15–20 min | ___ |
| Phase 4 — Deploy & Test | 20–30 min | ___ |
| Phase 5 — PR | 15 min | ___ |
| **Total** | **~3.5–4 hours** | ___ |

## Key Changes from Phase 1.3 Review

1. **GioChartPieComponent already exists** — Story 7 now reuses `GioChartPieModule` instead of creating from scratch (S → resized)
2. **ES repository layer explicitly scoped** — Stories 2–4 now include full file paths for `AnalyticsRepository` API, ES impl, and NoOp stub (M → L)
3. **One use case per operation** — Following codebase convention (4 separate use cases, not 1 dispatcher)
4. **Stories 6+8 merged** — Both modified same files, merged into "Stats Cards + Dashboard Layout"
5. **Dependency graph loosened** — S2/S3/S4 are parallelizable; S5 can start before backend is complete

## Full Story Details

See [STORIES.md](./STORIES.md) for complete subtask breakdowns, file lists, and test plans.
