# Gravitee Gamma — Developer Portals module

Developer Portals module for [Gravitee Gamma](../gravitee-gamma-plugin/gravitee-gamma-plugin-module/README.md): backend (Jersey) and React micro-frontend (Module Federation remote).

## Local development

Commands run from the **repository root** (Nx workspace), using the Node version in `.nvmrc` and the pinned Yarn version.

```bash
yarn install --immutable
yarn nx serve gravitee-gamma-module-portals
```

Open `http://localhost:3004`. The standalone shell shows Dashboard and Portals navigation. Dashboard and Portals consume the Management API portals endpoints through the shared client. The overview remains a placeholder. Browser verification requires an operator-coordinated backend or an explicitly identified mock preview.

## Routes

Routes are relative to the module mount `/environments/:envHrid/portals` in Gamma, or `/` in standalone development:

- The module root is Dashboard.
- `list` is the Portals page.
- `:portalId` redirects to `:portalId/overview`.
- `:portalId/overview` displays the portal ID and highlights Portals in the sidebar.

There is no repeated `portals` segment: a hosted overview URL is `/environments/:envHrid/portals/:portalId/overview`. Opening an overview URL directly preserves the portal context. Future portal-scoped pages belong under `:portalId`.

## Portals list

The `list` route shows the complete server page in API order, with portal logo/name/default marker, URL, published API count and View Portal. Search is debounced by 300 ms and applies `q` together with `page=1`; subsequent page changes retain that query. Page size is 10. Quota comes independently from `_stats` and does not change with filtering or paging.

Page controls use the API's `pageCount`, or derive it from a known filtered `totalCount`. When the total is missing or `-1`, explicit pagination links indicate whether another page exists; no total is fabricated. Requests always use the existing environment-scoped service, rather than following response link URLs directly.

Clicking a row or pressing Enter on its focused row opens the overview only with `environment-documentation-u`, matching the dashboard's Configure action. Without that permission, rows are passive while their data and View Portal remain available. View Portal is a separate tab stop and opens a configured HTTP(S) URL in a new tab without opening the row. Missing URLs leave the action unavailable with an explanation; missing or failed logos use initials. New Portal remains unavailable. Loading, empty collection, no matches, forbidden access and retryable errors have distinct states. Statistics failures retain a usable list; a 403 on either endpoint hides partial data.

The full list opts into retaining the previous table and pagination while another page or search loads in the same environment. Both page buttons are disabled during fetching, and the pager announces loading until the new results arrive. A previous empty response shows loading instead of a definitive empty-result message for the new query. Changing environments hides previous data; errors and forbidden responses replace retained rows. The dashboard keeps its existing loading behavior.

The dashboard retains its three-tile/five-row preview, permission-controlled Configure action and view switching without refetching. List row navigation is opt-in and does not make dashboard rows interactive. The existing Graphene DataTable renders the dashboard; the interactive list uses Graphene table primitives with the same cell renderers.

## Data and tests

`usePortals` and `usePortalsStats` consume the Management API v2 contract from PORTAL-333 through the shared APIM client. They use the environment ID from the Gamma SDK, wait for that context before fetching, and keep caches separate by environment. Only the list uses search and pagination parameters; statistics remain environment-wide. HTTP failures are exposed as query errors.

```bash
yarn nx test gravitee-gamma-module-portals --runInBand
yarn nx lint gravitee-gamma-module-portals
yarn nx build gravitee-gamma-module-portals
```

Regression tests cover search/page interaction, unknown totals, independent statistics, late responses, environment isolation, row versus external-link actions, keyboard operation, routing/breadcrumbs and dashboard behavior. Tests use MSW for the HTTP boundary, including shared-client configuration/bootstrap requests. Unhandled requests fail tests. Portal factories and a fresh QueryClient wrapper are available under `src/main/ui/testing`; tests and lint do not need a running backend or network access once dependencies are installed.

## Gamma host integration

The host still redirects Developer Portals to the classic console. PORTAL-337 owns the cutover; a module manifest override alone does not enable this module in the host. The federation entry remains available for that integration.

For full-stack setup, see the **Gamma Console** section in [`CONTRIBUTING.adoc`](../../CONTRIBUTING.adoc#dev-guide-gamma-console). For host architecture and module discovery, see [docs/gamma-module-loading.md](../gravitee-gamma-control-plane-webui/docs/gamma-module-loading.md).
