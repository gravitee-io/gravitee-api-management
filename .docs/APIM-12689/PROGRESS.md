# Analytics Dashboard — Progress Tracker

```
Developer A (Backend)                    Developer B (Frontend)
────────────────────                     ──────────────────────

1.1 Settings toggle BE ──────────────▶  1.2 Settings toggle Console FE
        │                                       │
        ▼                                       ▼
4.1 PortalContextLoader                  5.1 Routes, sidenav, menu items
4.2 Spring wiring                               │
        │                                       ▼
        ▼                                9.1 Conditional visibility guard
2.1 Dashboard OpenAPI spec                      │
2.2 Dashboard JAX-RS ────────────────▶  6.1 Dashboard list page + cards
        │                                       │
        ▼                                       │
3.1 Computation OpenAPI spec                    │
3.2 Computation JAX-RS ──────────────▶  7.1 Dashboard detail page
                                                │
                                                ▼
                                        8.1 Filters (uses existing ApiService
                                            + ApplicationService, no BE needed)
```

Legend: `──▶` = frontend task needs backend task completed (sync point)

---

## Developer A (Backend)

- [ ] **1.1** Add `PORTAL_NEXT_ANALYTICS_ENABLED` key + `PortalNext.analytics` field
- [ ] **4.1** Create `PortalContextLoader` implementing `AnalyticsQueryContextLoader`
- [ ] **4.2** Spring wiring for `PortalContextLoader` in portal context
- [ ] **2.1** OpenAPI spec: define dashboard endpoints in `portal-openapi.yaml`
- [ ] **2.2** JAX-RS resources + mapper for dashboards
- [ ] **3.1** OpenAPI spec: define measures/facets/time-series endpoints
- [ ] **3.2** JAX-RS resource for computation

## Developer B (Frontend)

- [ ] **1.2** Console frontend: add analytics toggle to portal settings form
      ⏳ _needs 1.1_
- [ ] **5.1** Routes, sidenav, menu items + configuration
- [ ] **9.1** Guard + conditional rendering based on analytics toggle
- [ ] **6.1** Analytics service + dashboard list component + card component
      ⏳ _needs 2.2_
- [ ] **7.1** Dashboard detail component with `GraviteeDashboardComponent`
      ⏳ _needs 3.2_
- [ ] **8.1** Filters integration (API via `ApiService`, Application via `ApplicationService`, static status code group)
