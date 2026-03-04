# V4 API Analytics Dashboard – UI patterns reference

The Gravitee Console uses **Particles** (`@gravitee/ui-particles-angular`) on top of Angular Material. When implementing or changing analytics UI, follow the **existing** patterns in `gravitee-apim-console-webui/src/management/api/api-traffic-v4/analytics/` and the shared components below.

---

## 1. Components and libraries to use

| Purpose | Use this | Do NOT use |
|--------|----------|------------|
| **Charts (line)** | `GioChartLineModule` from `shared/components/gio-chart-line/` | Raw Highcharts, ngx-charts, Chart.js |
| **Charts (pie)** | `GioChartPieModule` from `shared/components/gio-chart-pie/` | New pie/donut libraries; raw Highcharts for pie |
| **Empty states** | `<gio-card-empty-state>` from `@gravitee/ui-particles-angular` | Custom empty-state markup |
| **Loading** | `GioLoaderModule` / `<gio-loader>` from `@gravitee/ui-particles-angular` | Custom spinners, raw Material progress |
| **Icons** | `GioIconsModule` from `@gravitee/ui-particles-angular` | Raw Material icons only |
| **Cards** | `MatCardModule` (Angular Material) – used directly in analytics | Custom card wrappers |
| **Tables** | `gio-table-wrapper` pattern (see `src/management/` examples) | Raw `mat-table` without wrapper |

---

## 2. Existing analytics structure (api-traffic-v4/analytics)

- **Proxy (dashboard):** `api-analytics-proxy/`  
  - Uses: `GioCardEmptyStateModule`, `GioLoaderModule`, `MatCardModule`, `ApiAnalyticsFiltersBarComponent`, `ApiAnalyticsRequestStatsComponent`, `ApiAnalyticsResponseStatusRangesComponent`, `ApiAnalyticsResponseStatusOvertimeComponent`, `ApiAnalyticsResponseTimeOverTimeComponent`.
- **Filters:** `components/api-analytics-filters-bar/` – `MatCardModule`, `GioIconsModule`.
- **Request stats (cards):** `components/api-analytics-requests-stats/` – `GioLoaderModule`, `MatCardModule`; shows list of stats with `<gio-loader>` per row when loading.
- **HTTP status pie:** `shared/components/api-analytics-response-status-ranges/` – `MatCard`, `GioChartPieModule`, `GioLoaderModule`; input shape `{ label, value }[]` → mapped to `GioChartPieInput[]` (label, value, color).
- **Line charts:**  
  - `api-analytics-response-status-overtime` and `api-analytics-response-time-over-time` use **GioChartLineModule** with `[data]` and `[options]` (`GioChartLineData[]`, `GioChartLineOptions`).

---

## 3. Pie chart (HTTP status) – reuse, do not recreate

The **HTTP status pie** is already implemented:

- **Component:** `api-analytics-response-status-ranges`  
  - Path: `src/shared/components/api-analytics-response-status-ranges/`
  - Uses **GioChartPieModule** and accepts `responseStatusRanges: { isLoading: boolean; data?: { label: string; value: number }[] }`.
- **Usage in v4 proxy:**  
  `api-analytics-proxy` already wires GROUP_BY status from the unified endpoint to this component; no second pie widget is needed.

For any **new** pie/donut in the app, use the same **GioChartPieModule** and the same data shape (`GioChartPieInput[]`: label, value, color). Do not add another chart library.

---

## 4. Styling and tokens

- **Do NOT** use hardcoded colors, spacing, or font sizes. Use Particles/theme:
  - Prefer `@use '@gravitee/ui-particles-angular' as gio;` and `mat.m2-get-color-from-palette(gio.$mat-success-palette, 'default')` (and similar for warning, error, neutral) instead of hex.
  - Use `gio.$mat-theme`, `gio.$mat-dove-palette`, etc. for typography and borders (see `api-analytics-response-status-ranges.component.scss`).
- **Layout:** Use existing patterns such as `gio-layout.gio-responsive-margin-container` and the proxy’s `gridContent` / `full-bleed` grid (see `api-analytics-proxy.component.scss`).

**Note:** The current `api-analytics-response-status-ranges` component uses hardcoded hex colors in `getColor()` (2xx/3xx/4xx/5xx). For new code or when touching that file, prefer theme palette colors (e.g. success for 2xx, warning for 4xx, error for 5xx) for consistency with Particles.

---

## 5. Test harnesses

- **Pie:** `GioChartPieHarness` from `shared/components/gio-chart-pie/gio-chart-pie.harness.ts` (e.g. `hasNoData()`, `displaysChart()`).
- **Status ranges card:** `ApiAnalyticsResponseStatusRangesHarness` from `shared/components/api-analytics-response-status-ranges/`; uses `GioChartPieHarness` internally.
- Use **DivHarness** (or the same harness patterns) from `@gravitee/ui-particles-angular/testing` where applicable; do not introduce ad-hoc selectors that bypass Particles harnesses.

---

## 6. Self-lint before committing

- [ ] No new chart libraries (only GioChartLine, GioChartPie as above).
- [ ] No raw Angular Material where a Gravitee/Particles wrapper exists (e.g. use `gio-loader`, `gio-card-empty-state`, not raw progress or custom empty divs).
- [ ] No hardcoded colors/fonts/spacing; use Particles/theme tokens or palettes.
- [ ] Empty state: `<gio-card-empty-state>` with icon/title/subtitle.
- [ ] Loading: `<gio-loader>` (e.g. in card __loader div).
- [ ] Tables: if added, use `gio-table-wrapper` pattern from `src/management/`.
- [ ] Tests: use existing harnesses (e.g. `GioChartPieHarness`, `ApiAnalyticsResponseStatusRangesHarness`) where they exist.

---

## 7. File reference

| Area | Path |
|------|------|
| V4 analytics proxy | `src/management/api/api-traffic-v4/analytics/api-analytics-proxy/` |
| Filters bar | `src/management/api/api-traffic-v4/analytics/components/api-analytics-filters-bar/` |
| Request stats | `src/management/api/api-traffic-v4/analytics/components/api-analytics-requests-stats/` |
| Response status overtime (line) | `src/management/api/api-traffic-v4/analytics/components/api-analytics-response-status-overtime/` |
| Response time over time (line) | `src/management/api/api-traffic-v4/analytics/components/api-analytics-response-time-over-time/` |
| HTTP status pie (shared) | `src/shared/components/api-analytics-response-status-ranges/` |
| GioChartLine | `src/shared/components/gio-chart-line/` |
| GioChartPie | `src/shared/components/gio-chart-pie/` |
| gio-table-wrapper examples | `grep -r "gio-table-wrapper" --include="*.html" src/management/` |
