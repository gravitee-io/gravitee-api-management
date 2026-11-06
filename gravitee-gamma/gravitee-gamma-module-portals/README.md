# Gravitee Gamma — Developer Portals module

Developer Portals module for [Gravitee Gamma](../gravitee-gamma-plugin/gravitee-gamma-plugin-module/README.md): backend (Jersey) and React micro-frontend (Module Federation remote).

## Local development

Commands run from the **repository root** (Nx workspace), using the Node version in `.nvmrc` and the pinned Yarn version.

```bash
yarn install --immutable
yarn nx serve gravitee-gamma-module-portals
```

Open `http://localhost:3004`. The standalone shell shows Dashboard and Portals navigation. The current screens are placeholders and do not call the backend, so the Management API is not required to explore the routes.

## Routes

Routes are relative to the module mount `/environments/:envHrid/portals` in Gamma, or `/` in standalone development:

- The module root is Dashboard.
- `list` is the Portals page.
- `:portalId` redirects to `:portalId/overview`.
- `:portalId/overview` displays the portal ID and highlights Portals in the sidebar.

There is no repeated `portals` segment: a hosted overview URL is `/environments/:envHrid/portals/:portalId/overview`. Opening an overview URL directly preserves the portal context. Future portal-scoped pages belong under `:portalId`.

## Data and tests

`usePortals` and `usePortalsStats` consume the Management API v2 contract from PORTAL-333 through the shared APIM client. They use the environment ID from the Gamma SDK, wait for that context before fetching, and keep caches separate by environment. Only the list uses search and pagination parameters; statistics remain environment-wide. HTTP failures are exposed as query errors.

```bash
yarn nx test gravitee-gamma-module-portals --runInBand
yarn nx lint gravitee-gamma-module-portals
yarn nx build gravitee-gamma-module-portals
```

Tests use MSW for the HTTP boundary, including shared-client configuration/bootstrap requests. Unhandled requests fail tests. Portal factories and a fresh QueryClient wrapper are available under `src/main/ui/testing`; tests and lint do not need a running backend or network access once dependencies are installed.

## Gamma host integration

The host still redirects Developer Portals to the classic console. PORTAL-337 owns the cutover; a module manifest override alone does not enable this module in the host. The federation entry remains available for that integration.

For full-stack setup, see the **Gamma Console** section in [`CONTRIBUTING.adoc`](../../CONTRIBUTING.adoc#dev-guide-gamma-console). For host architecture and module discovery, see [docs/gamma-module-loading.md](../gravitee-gamma-control-plane-webui/docs/gamma-module-loading.md).
