# PORTAL-138 — Implementation plan

Epic: [PORTAL-138 · Gamma Portal - Default portal (foundation)](https://gravitee.atlassian.net/browse/PORTAL-138) · Target 4.15.0
Written Oct 30, 2026, from the epic description as of that date and the code on `master`.

**Goal (from the epic):** admins see and open their existing developer portal from the Gamma Developer Portals module. Under the hood, the portal becomes a real record in each environment: navigation (homepage included), API listings and documentation belong to it, and the next-gen portal and Gamma read them per portal. Customers still have exactly one portal.

**Ordering constraint:** the Gamma frontend is built before the backend. The plan makes that safe by (1) agreeing the Management API v2 contract first, as an OpenAPI change with no implementation, (2) building and testing the screens against that contract with mocked HTTP, and (3) keeping the module unreachable in the product until the backend serves the contract: the Gamma host keeps redirecting "Developer Portals" to the classic console until story 13.

This plan supersedes, for this epic's scope, the story numbering in `PORTAL-138 Epic breakdown.md` (Oct 23), which covered the wider three-step split before PORTAL-137 was carved out.

---

## 1. Where the code stands today

| Area | Today | Consequence for this epic |
| --- | --- | --- |
| Gamma portals module | [`AppRoutes.tsx`](gravitee-gamma/gravitee-gamma-module-portals/src/main/ui/app/AppRoutes.tsx) renders a single `<h1>`; no data layer, no tests, no MSW. [`PortalsModule.restResource()`](gravitee-gamma/gravitee-gamma-module-portals/src/main/java/io/gravitee/gamma/module/portals/PortalsModule.java) returns `null`. | The whole frontend is new. Copy the structure of the APIM module (`config/`, `features/<x>/{types,services,hooks,utils,components,pages}`, `testing/`). |
| Gamma host | The "Developer Portals" app is never mounted: [`AppRoutes.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/app/AppRoutes.tsx) swaps it for [`PortalNextEditorRedirect`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/components/PortalNextEditorRedirect.tsx); [`ShellLayout.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/shared/components/ShellLayout.tsx) and [`HomePage.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/pages/home/HomePage.tsx) link to the classic console editor via [`buildPortalNextEditorUrl`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/portal-next.ts). | Frontend PRs can merge without users seeing them. Story 13 flips the host once the backend is live. |
| Core portal model | [`Portal`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/model/Portal.java) (id, env, org, name, navigation structure, activeThemeId) exists, created only by the Automation API (`CreateOrUpdatePortalUseCase`). [`PortalId.ZERO`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/model/PortalId.java) is a placeholder. | No default marker, no URL, no portal in a console-only environment. |
| Navigation items | Repository [`PortalNavigationItem.referenceId`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/model/PortalNavigationItem.java) defaults to the zero UUID; [`NavigationItemReference.defaultReference()`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal_page/model/NavigationItemReference.java) returns `PortalReference(PortalId.ZERO)`. Portal API [`PortalNavigationItemsResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/PortalNavigationItemsResource.java) lists items **per environment, not per portal**. | Items authored through the Automation API reference a real portal id; console-authored items reference the zero UUID. Today both show on the one next-gen portal. The upgrader must keep that (see decision D4). |
| Portal listings | [`PortalListing`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/model/PortalListing.java) (`portalId`, `apis`) is written only by the Automation API and CRD import, then materialized into navigation items. | Console-only environments have no listings; API counts cannot come from listings alone (decision D3). |
| Portal URL | [`InstallationAccessQueryServiceImpl.getPortalUrl`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/infra/query_service/installation/InstallationAccessQueryServiceImpl.java:322) — Cloud access point when multi-tenant, else the configured/environment `portal.url`. The next-gen portal is that URL + `/next` ([`ConsoleAuthenticationResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/auth/ConsoleAuthenticationResource.java:92)). | The default portal's URL is derived, not stored. |
| Portal bootstrap | [`PortalUIBootstrapResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/bootstrap/PortalUIBootstrapResource.java) returns `organizationId`, `environmentId`, `baseURL`. Next-gen [`ConfigService`](gravitee-apim-portal-webui-next/src/services/config.service.ts) reads `assets/config.json` then `/ui/bootstrap`. | Add `portalId` (the default portal's id) to the bootstrap; add optional `portalId` to `config.json`. |
| Management API v2 | No `/portals` endpoint. Navigation items live under `/portal-navigation-items` in [`openapi-environments.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-model/src/main/resources/openapi/openapi-environments.yaml:1104). License tier is exposed by [`GraviteeLicenseResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/installation/GraviteeLicenseResource.java:51). | New portals endpoints go in `openapi-environments.yaml`. |

---

## 2. Decisions this plan takes (challenge them in review)

- **D1 — Where the screens live.** "Gamma dashboard" is the landing page of the Developer Portals module; "Portals page" is the second item of the module's sidebar ("Portal menu"). Routes, relative to the module mount `/environments/:envHrid/portals`: the index is the dashboard, `portals` is the Portals page, `portals/:portalId` redirects to `portals/:portalId/overview`. Every portal-scoped route starts with `portals/:portalId`, so PORTAL-35/36/140 add pages under it without route changes. *Confirm the URL segments with design: the full path reads `/portals/portals/{portalId}`.*
- **D2 — Contract first, frontend second, cutover last.** Story 1 only edits the OpenAPI spec (generated models, no resource). Stories 2–4 build against it with MSW. Story 13 mounts the module in the host after story 9 has shipped.
- **D3 — "APIs published" on a portal** = number of distinct APIs referenced by `API` navigation items of that portal that are published to the portal (the set the next-gen portal catalog shows anonymous-or-not, ignoring per-user visibility). Listings are materialized into navigation items, so this works for console-only and automation-managed environments alike. *Confirm with Product; the alternative, "APIs in listings", reads 0 for every console-only customer.*
- **D4 — Upgrade must not change what the next-gen portal shows.** Today it shows every navigation item of the environment, whatever its `referenceId`. Rule for the upgrader, per environment:
  - no portal exists → create the default portal and re-point zero-UUID items and the docs attached to them onto it;
  - exactly one portal exists (created through the Automation API) → mark it default and re-point zero-UUID items onto it;
  - in both cases, re-attach to the default portal every listing whose `portalId` points to no existing portal (the zero UUID included).

  No customer has more than one portal per environment today, Automation API included. If the upgrader meets several, it fails for that environment with an error naming it and the portals, and changes nothing there, rather than guessing which one is the default.
- **D5 — Portal URL.** The default portal's URL is never stored: it is `getPortalUrl(env)` + `/next`, read at request time. Story 5 adds a nullable `url` column for non-default portals (PORTAL-137) so the schema migrates once; nothing writes it in this epic. If Product drops `/next` for Gamma users (open question Q1), only the resolver in story 8 changes.
- **D6 — Quota.** `limit` comes from the license tier: `universe` → 10, anything else (trial, `planet`, `galaxy`, `oss`, unknown, no license) → 1. Defined once in a core domain service so PORTAL-137's create check reuses it. `used` = number of portals in the environment.
- **D7 — Logo.** Until PORTAL-319, every portal's logo is the logo of the environment's enabled next-gen theme. The list does not embed it: the Management API v2 serves it at `GET /portals/{portalId}/logo`, and each portal's `logoUrl` points there (null when the theme has no logo). Gamma is the admin app and never calls the Portal API; this follows the API picture (`_links.pictureUrl` → `/apis/{apiId}/picture`). The frontend falls back to the portal's initials when `logoUrl` is null or the image fails to load.
- **D8 — Fuzzy search** runs in the browser over at most 10 portals with a small in-module subsequence matcher (no new dependency). The list API neither pages nor searches.
- **D9 — Permissions.** `GET /portals`, `GET /portals/{portalId}` and `GET /portals/{portalId}/logo` require `ENVIRONMENT_DOCUMENTATION[READ]`, the permission of the existing navigation endpoints. The frontend hides "Configure" without `ENVIRONMENT_DOCUMENTATION[UPDATE]`. *Security-sensitive: confirm in story 1 review.*

## 3. Open questions (owner)

| # | Question | Blocks | Default if unanswered |
| --- | --- | --- | --- |
| Q1 | Gamma users' next-gen portal URL without `/next`: how is it derived? (Product, Cloud team) | Story 8 | Keep `/next`. |
| Q2 | What does "% Completion" on portal tiles measure? (Product) | Story 3 | Not shown. |
| Q3 | How is a **trial** license recognized in APIM? `License.getTier()` has no trial value. (Licensing) | Story 9 | Trial falls under "anything else → 1", which is already the trial limit. |
| Q4 | Is the default portal marked on tiles and rows, and how? (Design) | Stories 3, 4 | "Default" badge next to the name. |

---

## 4. Sequencing

| # | Story | Side | Depends on | Est. (dev-days) |
| --- | --- | --- | --- | --- |
| 1 | Portals contract in OpenAPI | Contract | — | 0.5 |
| 2 | Portals module foundation: routes, data layer, tests | Gamma FE | 1 | 2 *(proposed)* |
| 3 | Dashboard: quota, tiles/table, search | Gamma FE | 2 | 3.5 *(proposed)* |
| 4 | Portals page | Gamma FE | 2 (3 for shared components) | 2 *(proposed)* |
| 5 | Portal model: default marker, URL, creation date | BE | — | 2 |
| 6 | Default portal for every new environment; every write path names its portal | BE | 5 | 3 |
| 7 | Upgrader: default portal for existing environments, placeholder removed, upgrade tests | BE | 5, 6 (merge right after 6) | 5.5 |
| 8 | Default portal URL and `portalId` in Portal API bootstrap | BE | 6 | 1 |
| 9 | Management API v2: list and get portals with quota, logo | BE | 1, 7, 8 | 3.5 |
| 10 | Management API v2: per-portal navigation and page content endpoints | BE | 7 | 3 |
| 11 | Portal API: `/portal/{portalId}/...` navigation, search, API lists | BE | 7 | 4 |
| 12 | Next-gen portal: optional `portalId`, calls through `/portal/{portalId}/...` | Portal FE | 8, 11 | 1.5 |
| 13 | Gamma host opens the Developer Portals module | Gamma FE | 2–4, 9 released | 1 *(proposed)* |

Backend total 23.5 dev-days, about a day over the epic's 4.5 dev-weeks, review not counted. Frontend 8.5 dev-days proposed.

Stories 10, 11 and 12 can run in parallel with 13 once 7 has merged. Story 8 needs `DefaultPortalDomainService` from story 6.

---

## 5. Stories

Each story lists the test cases to write **first** (RED), then the files. Paths are from the repository root. `MODULE` = `gravitee-gamma/gravitee-gamma-module-portals/src/main/ui`; `CORE` = `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim`.

### Story 1 · Portals contract in OpenAPI

**Why.** The frontend goes first. Without an agreed response shape, the screens would be built on guesses and reworked when the backend lands. The repository is API-first: the spec changes before any JAX-RS code. Generation is models-only, so merging the spec changes no runtime behaviour.

**Files**
- Change [`openapi-environments.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-model/src/main/resources/openapi/openapi-environments.yaml):
  - `GET /portals` → `PortalsResponse { data: Portal[], quota: PortalQuota }`; no `page`/`perPage`/`q`.
  - `GET /portals/{portalId}` → `Portal`; `404` when the portal is not in the environment.
  - `GET /portals/{portalId}/logo` → the logo image (D7); `404` when the theme has no logo or the portal is not in the environment.
  - `Portal { id, name, url, default: boolean, logoUrl?: string, publishedApisCount: integer, createdAt: date-time }`; `url` and `logoUrl` are nullable.
  - `PortalQuota { used: integer, limit: integer }`.
  - Descriptions state the permission (D9) and that the list is unpaged because an environment holds at most 10 portals.
- Run `mvn -pl gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-model -am compile` and check the generated `Portal`, `PortalsResponse`, `PortalQuota` models.

**Acceptance criteria**
- The spec validates and the model module compiles.
- No resource class is added; calling `/portals` still returns 404.
- Frontend and backend owners have approved the shape on the PR (it is the contract for stories 2–4 and 9).

### Story 2 · Portals module foundation: routes, data layer, tests

**Why.** Every screen in this epic and in PORTAL-35/36/140 needs the module's routing (with `portalId` from the start), its sidebar, React Query, the Management API client and a test harness. Doing it once, separately from the screens, keeps stories 3 and 4 about UI behaviour.

**Tests first**
- `config/routes.spec.ts`: `getActiveNavKey` returns `dashboard` for the module root, `portals` for `/portals`, and `portals` for `/portals/:portalId/overview` (portal pages sit under the Portals menu item).
- `features/portals/hooks/usePortals.spec.tsx` (MSW): returns `data` and `quota` from `GET …/environments/:envId/portals`; exposes an error state on 500; refetches when the environment changes.
- `features/portals/utils/matchPortals.spec.ts`: empty query returns all portals in input order; case-insensitive; matches a subsequence (`"dvprt"` → "Dev Portal"); ranks contiguous matches before scattered ones; ignores surrounding spaces.
- `app/AppRoutes.spec.tsx`: the index renders the dashboard route; `/portals/:portalId` redirects to `/portals/:portalId/overview`; the overview placeholder has the portal id from the URL.

**Files**
- Change [`MODULE/app/AppRoutes.tsx`](gravitee-gamma/gravitee-gamma-module-portals/src/main/ui/app/AppRoutes.tsx): `QueryClientProvider`, a `ModuleLayout` that sets the sidebar and breadcrumbs through `useLayoutConfig` (as in the APIM module's `AppRoutes.tsx`), routes from D1.
- Add `MODULE/config/routes.ts`, `MODULE/config/navigation.ts` (two items: Dashboard, Portals), modelled on `gravitee-gamma-module-apim/src/main/ui/config/`.
- Add `MODULE/app/useModuleNavigation.ts`, `MODULE/app/useModuleBreadcrumbs.ts` (copy-adapt from the APIM module; do not share code across modules).
- Add `MODULE/shared/api/apimClient.ts` re-exporting `gamma-ui-shared/src/api` as the APIM module does.
- Add `MODULE/features/portals/types/portal.ts` — `Portal`, `PortalQuota`, `PortalsResponse`, mirroring story 1's schemas.
- Add `MODULE/features/portals/services/portals.ts` — `listPortals(envId)` via `apimFetchJsonV2`. `getPortal` and its hook come with PORTAL-140, the first page that needs one portal.
- Add `MODULE/features/portals/utils/queryKeys.ts`, `MODULE/features/portals/utils/matchPortals.ts`.
- Add `MODULE/features/portals/hooks/usePortals.ts`, `usePortalIdParam.ts`.
- Add `MODULE/features/portals/pages/PortalOverviewPlaceholderPage.tsx` (shows the portal id only; PORTAL-140 replaces it).
- Add `MODULE/testing/server.ts`, `MODULE/testing/handlers/{index,portals.handlers}.ts`, `MODULE/testing/factories.ts` (`aPortal()`, `aPortalsResponse()`), `MODULE/test-setup.ts`.
- Change `gravitee-gamma/gravitee-gamma-module-portals/jest.config.ts` (`setupFilesAfterEnv` → `test-setup.ts`, `jest-fixed-jsdom` environment as in the APIM module) and `package.json` only if MSW / Testing Library are not already hoisted from the root.

**Acceptance criteria**
- `yarn nx serve gravitee-gamma-module-portals` shows the sidebar with Dashboard and Portals; the module root lands on Dashboard.
- Every portal-scoped URL carries `portalId`; refreshing `/portals/:portalId/overview` keeps the same portal id.
- The data layer calls only `GET /portals` as specified in story 1.
- `yarn nx test gravitee-gamma-module-portals` and `yarn nx lint gravitee-gamma-module-portals` pass with no network access.

### Story 3 · Dashboard: quota, tiles/table, search

**Why.** This is the screen the epic's "Done when" names: admins see their portal, its quota, and open it. It also sets the components the Portals page reuses (portal row, "View Portal", quota, search box).

**Tests first** (`features/portals/pages/PortalsDashboardPage.spec.tsx`, MSW)
- Shows the quota above the list as "1 / 10" from `quota.used` / `quota.limit`; the client never computes the limit.
- Shows the total number of APIs published, the sum of `publishedApisCount` across portals, next to the quota.
- Shows each portal's logo from `logoUrl` (initials when `logoUrl` is null or the image fails to load), name, "Default" marker on the default portal (Q4), and `publishedApisCount`.
- Tiles by default; the toggle switches to a table and back without refetching. The choice is local to the page; remembering it is out of scope.
- With 4 portals, tiles show 3 and a "View all Portals" link to `portals`; with 6 portals, the table shows 5 and the same link; with 1 portal, no link.
- Typing in search filters with `matchPortals`; the 3/5 limit applies to the filtered result; no match shows an empty state.
- "View Portal" is a link to `portal.url` with `target="_blank"` and `rel="noopener noreferrer"`; when `url` is null it is disabled, with a tooltip saying the portal URL is not configured.
- "Configure" navigates to `portals/:portalId/overview`; it is hidden without `ENVIRONMENT_DOCUMENTATION[UPDATE]` (mock `useHasPermission`).
- "New Portal" is rendered and disabled, with a tooltip saying creating portals is coming soon.
- Loading shows a skeleton; an API error shows an error state with "Retry"; a 403 shows a no-access message instead of the generic error.

**Files**
- Add `MODULE/features/portals/pages/PortalsDashboardPage.tsx`.
- Add `MODULE/features/portals/components/`: `PortalQuota.tsx`, `PublishedApisSummary.tsx`, `PortalTile.tsx`, `PortalTiles.tsx`, `PortalTable.tsx` (graphene `DataTable`), `PortalLogo.tsx`, `ViewPortalLink.tsx`, `PortalSearchInput.tsx`, `NewPortalButton.tsx`, `PortalsViewToggle.tsx`, `index.ts`.
- Change `MODULE/app/AppRoutes.tsx` to route the index to the page.

**Acceptance criteria**
- Matches the epic's frontend item 1 and the PRD layout; design sign-off on a screenshot in the PR.
- No API call other than `GET /portals` and the logo images at each `logoUrl`; search and limits run in the browser.
- Keyboard: the toggle, search, links and buttons are reachable and labelled (axe check in the spec passes).

### Story 4 · Portals page

**Why.** The Portal menu's full list: the dashboard only shows a preview, and admins need one place to find any portal and open its configuration.

**Tests first** (`features/portals/pages/PortalsListPage.spec.tsx`)
- Shows the quota above a table of **all** portals in the API's order (no 5-row limit, no paging), with search.
- Columns: logo and name (with the default marker), URL, APIs published, created (via `formatRelativeDateTime` from `gamma-ui-shared/src/time`), and a "View Portal" action.
- Clicking a row navigates to `portals/:portalId/overview`; "View Portal" in a row opens the URL in a new tab **without** navigating the row.
- Keyboard: Enter on a focused row opens it; "View Portal" is reachable on its own.
- Disabled "New Portal" as on the dashboard.
- Empty search result, error and 403 states as on the dashboard.

**Files**
- Add `MODULE/features/portals/pages/PortalsListPage.tsx`.
- Change `MODULE/features/portals/components/PortalTable.tsx`: optional `limit`, optional `onRowClick`, the URL and created columns.
- Change `MODULE/app/AppRoutes.tsx` to route `portals`.

**Acceptance criteria**
- Matches the epic's frontend item 2.
- The breadcrumb reads "Developer Portals / Portals"; on an overview page, "Developer Portals / Portals / <portal id>" until PORTAL-140 shows the name.

### Story 5 · Portal model: default marker, URL, creation date

**Why.** A portal must be a real record that can be told apart as the default, so requests without a `portalId` have a portal to act on. The URL column is added now so the schema migrates once (D5); the creation date serves the list (story 9); story 7 backfills it for existing portals with the upgrade time. The zero UUID placeholder stays until story 7 has moved the data off it.

**Tests first**
- `PortalTest`: `Portal.createDefault(env, org, name)` is default; `Portal.create(...)` is not; `createdAt` is set.
- Repository contract tests (`gravitee-apim-repository-test`): `PortalRepository` round-trips `defaultPortal`, `url`, `createdAt`; existing rows read `defaultPortal = false`, `url = null`, `createdAt = null`; new `findDefaultByEnvironmentId(envId)` returns it, or empty, and never a portal of another environment.
- JDBC and Mongo test suites run the same contract.
- `CreateOrUpdatePortalUseCaseTest`: an update of an existing portal keeps its stored `defaultPortal`, `url` and `createdAt`.

**Files**
- Change [`CORE/core/portal/model/Portal.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/model/Portal.java): `defaultPortal`, `url`, `createdAt`; factory `createDefault`; `isDefault()`.
- Change `CORE/core/portal/crud_service/PortalCrudService.java` (+ infra impl and the in-memory test double): `findDefaultByEnvironmentId`.
- Change [`CORE/core/portal/use_case/CreateOrUpdatePortalUseCase.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/use_case/CreateOrUpdatePortalUseCase.java): an update keeps the stored `defaultPortal`, `url` and `createdAt`. The Automation API's `PUT` replaces the whole definition and does not carry these fields (§6).
- Change [`PortalRepository`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/api/PortalRepository.java), [`model/Portal.java`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/model/Portal.java), `JdbcPortalRepository`, `MongoPortalRepository`, `PortalMongo`, `PortalAdapter` (infra).
- Add Liquibase changelog `gravitee-apim-repository-jdbc/src/main/resources/liquibase/changelogs/v4_14_0/02_add_default_url_created_at_to_portals.yml` (the next free number; use a `v4_15_0` folder instead if the 4.14 release branch is cut by then) (+ master changelog entry); Mongo needs no migration, add an index on `(environmentId, defaultPortal)`.
- Keep the zero-UUID defaults on `PortalNavigationItem.referenceId` / `PortalNavigationItemMongo` until story 7 has re-pointed the data; remove them in story 7.

**Acceptance criteria**
- Builds and all existing portal, navigation and automation tests pass unchanged in behaviour.
- JDBC (all supported databases in CI) and Mongo store and read the new fields.
- An Automation API upsert of an existing portal keeps its default marker, URL and creation date.
- Automation API output is unchanged (the new fields are not mapped there — see §6).

### Story 6 · Default portal for every new environment; every write path names its portal

**Why.** A new environment must start with the portal that requests without a `portalId` act on; otherwise the first navigation item created in the console has nowhere to go. Code that creates navigation items today relies on the zero placeholder (`NavigationItemReference.defaultReference()` → `PortalId.ZERO`); each such path must resolve the default portal instead, so story 7 can remove the placeholder. The default portal must also not be deletable.

**Tests first**
- `DefaultPortalDomainServiceTest`, `ensureDefault(org, env)`: no portal → creates one, default, named "Developer Portal" (*confirm copy*), with `createdAt`; one non-default portal (created through the Automation API) → marks it default; a default already present → returns it unchanged; several portals → fails loudly naming the environment (D4). `getDefault(env)` throws `DefaultPortalNotFoundException` when there is none.
- `CreateDefaultPortalNavigationItemsUseCaseTest`: `should_attach_seeded_items_to_default_portal` — the seeded homepage, top navbar and APIs folder reference the default portal, not the zero id.
- `EnvironmentCommandHandlerTest`: `should_create_default_portal_with_environment`; the same for `EnvironmentServiceImpl.createOrUpdate` on a new environment. Running environment creation twice creates no second portal.
- `DeletePortalUseCaseTest`: `should_refuse_to_delete_default_portal`; nothing is deleted.
- One test per changed caller below: it writes the default portal's id, never the zero id.

**Files**
- Add `CORE/core/portal/domain_service/DefaultPortalDomainService.java` (`ensureDefault`, `getDefault`). Story 7's upgrader reuses `ensureDefault`, so the D4 rule lives in one place.
- Add `CORE/core/portal/exception/DefaultPortalNotFoundException.java`, `DefaultPortalDeletionException.java`.
- Change [`CORE/core/portal/use_case/DeletePortalUseCase.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/use_case/DeletePortalUseCase.java): refuse to delete the default portal (reachable through the Automation API).
- Change [`CORE/core/portal_page/use_case/CreateDefaultPortalNavigationItemsUseCase.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal_page/use_case/CreateDefaultPortalNavigationItemsUseCase.java) (seeds a new environment's navigation): call `ensureDefault` first and attach the seeded items to it. Cockpit ([`EnvironmentCommandHandler`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/cockpit/command/handler/EnvironmentCommandHandler.java)) and the seeding upgraders (`EnvironmentsDefaultPortalNavigationItemsUpgrader`, `EnvironmentsDefaultApisFolderUpgrader`) already go through it.
- Change [`EnvironmentServiceImpl.createOrUpdate`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/EnvironmentServiceImpl.java:163): a new environment ends with a default portal.
- Change every caller of `NavigationItemReference.defaultReference()` to pass the default portal:
  - `CORE/core/portal_page/model/CreatePortalNavigationItem.java`, `CORE/core/portal_page/model/PortalNavigationItem.java`;
  - `CORE/core/portal_page/domain_service/PortalNavigationItemDomainService.java`, `CORE/core/portal_page/domain_service/reconciliation/HomepageReconciler.java`;
  - `CORE/core/portal_page/use_case/ImportPortalNavigationUseCase.java`;
  - `CORE/core/portal/domain_service/PortalNavigationSyncDomainService.java`, `CORE/core/portal_listing/domain_service/NavigationItemEntryMaterializer.java`.
- The tests, `fixtures/…` and `inmemory/…` of each changed class.

**Acceptance criteria**
- Every new environment (self-hosted start-up, Cockpit, Management API) has exactly one default portal.
- The seeded navigation, and any navigation item created afterwards, stores the default portal's id as `referenceId`.
- No code path outside story 7's upgrader writes `PortalId.ZERO`.
- Deleting the default portal through any entry point, the Automation API included, returns a validation error and deletes nothing.
- Merge story 7 right after this one: in between, an environment created before this story has no default portal, so navigation writes fail loudly there. Both ship in the same release.

### Story 7 · Upgrader: default portal for existing environments, placeholder removed, upgrade tests

**Why.** Existing environments carry data on the zero UUID. The epic's first "Done when" is that an upgraded install looks exactly as before: same content, theme and URL. Once every environment is moved onto its default portal, the placeholder can go. This is the story that can break customers, so it carries the upgrade tests on both databases (epic items 3 and 9).

**Tests first**
- `DefaultPortalUpgraderTest` (in-memory), no portal → default created; one automation portal → marked default; default already present → no-op; several portals → that environment fails with an error naming it, its data unchanged, and the other environments still upgrade; a re-run after a partial failure finishes the job; existing portals get `createdAt`.
- Re-pointing: navigation items with `referenceType=PORTAL, referenceId=zero` → default id; a `PortalListing` whose `portalId` matches no portal of the environment (the zero UUID included) → default id, while a listing on an existing portal is untouched; documentation pages attached through those items keep their links; items with `referenceType=API` are untouched; the navigation `rootId` sentinel is untouched.
- Page contents, item order, visibility, published state, categories and automation metadata are unchanged, item for item.
- JDBC and Mongo integration tests seeded with a 4.14-shaped dataset (navigation in all areas including the homepage, listings, pages): after the upgrade the Portal API returns the same navigation tree, homepage and API list as before, item for item.
- `PortalIdTest`: `PortalId.of("00000000-…")` returns a plain id (no special instance).

**Files**
- Add `gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/upgrade/upgrader/DefaultPortalUpgrader.java`: per environment, `DefaultPortalDomainService.ensureDefault` (story 6), then the re-pointing above. Register its order constant in `UpgraderOrder` after `PORTAL_NAVIGATION_ITEM_API_OWNED_CONSOLE_MIGRATION_UPGRADER` (726), so it runs after every existing navigation upgrader, [`PortalNavigationItemRootIdUpgrader`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/upgrade/upgrader/PortalNavigationItemRootIdUpgrader.java) included.
- Add repository methods for the bulk re-point: `PortalNavigationItemRepository.updateReferenceId(envId, PORTAL, from, to)`, `PortalListingRepository.updatePortalId(envId, from, to)` (+ JDBC, Mongo, contract tests).
- Remove the placeholder:
  - [`CORE/core/portal/model/PortalId.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/model/PortalId.java): drop `ZERO`;
  - [`CORE/core/portal_page/model/NavigationItemReference.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal_page/model/NavigationItemReference.java): drop `PortalReference.DEFAULT` and `defaultReference()` (story 6 removed their callers);
  - [`PortalNavigationItem`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/model/PortalNavigationItem.java) and `PortalNavigationItemMongo`: drop the zero-UUID builder default and its follow-up comment;
  - a new Liquibase changeset in the story 5 folder dropping the column default on `portal_navigation_items.reference_id` (set in `v4_13_0/14_add_reference_columns_to_portal_navigation_items.yml`).
- Keep [`PortalNavigationItemDefaultReferenceMongoUpgrader`](gravitee-apim-repository/gravitee-apim-repository-mongodb/src/main/java/io/gravitee/repository/mongodb/management/upgrade/upgrader/portalnavigationitem/PortalNavigationItemDefaultReferenceMongoUpgrader.java): older data still needs it before `DefaultPortalUpgrader` runs. Point its "follow-up" comment at the new upgrader.
- Add upgrade test fixtures under `gravitee-apim-repository-test/src/test/resources/data/` (`portalnavigationitem-tests`, `portallisting-tests`, `portal-tests`), or the rest-api integration test resources, whichever the existing upgrader tests use.

**Acceptance criteria**
- After upgrade, every environment has exactly one default portal, no `PORTAL`-type navigation item references the zero UUID, and every listing references an existing portal of its environment.
- A next-gen portal deployment without `portalId` shows the same navigation, homepage and APIs as before the upgrade (JDBC and Mongo tests).
- `PortalId.ZERO` and `PortalReference.DEFAULT` no longer exist.
- The upgrader is idempotent and logs one line per environment it changes, with the environment id.
- Rollback note in the PR: rollback is a backup restore. 4.14 displays navigation per environment, but it finds and checks homepages by the zero reference ([`HomepageReconciler`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal_page/domain_service/reconciliation/HomepageReconciler.java:58), [`HomepageUniquenessRule`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal_page/domain_service/validation/HomepageUniquenessRule.java:41)), so a downgraded node would miss the re-pointed homepage on writes.

### Story 8 · Default portal URL and `portalId` in the Portal API bootstrap

**Why.** Gamma needs "View Portal" to open the right URL, and the next-gen portal needs to know which portal to load when its config names none. Both are read at request time so Cloud access points and environment settings stay the single source of truth (D5).

**Tests first**
- `PortalUrlDomainServiceTest`: default portal → `getPortalUrl(env)` + `/next` (multi-tenant access point and self-hosted setting); trailing slash handled; no portal URL configured → null, never a made-up value; non-default portal → its stored `url`, else null.
- `PortalUIBootstrapResourceTest`: response contains `portalId`, the default portal of the resolved environment, with and without `?environmentId=`.

**Files**
- Add `CORE/core/portal/domain_service/PortalUrlDomainService.java` (uses [`InstallationAccessQueryService`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/installation/query_service/InstallationAccessQueryService.java)).
- Change [`PortalUIBootstrapResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/bootstrap/PortalUIBootstrapResource.java) and `PortalUIBootstrapEntity`: add `portalId`, from `DefaultPortalDomainService.getDefault` (story 6).
- Change [`portal-openapi.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml) if the bootstrap is documented there.

**Acceptance criteria**
- The resolved URL equals the one the console's "open next-gen portal" redirect uses today.
- `/ui/bootstrap` keeps its existing fields; `portalId` is added.

### Story 9 · Management API v2: list and get portals with quota

**Why.** Implements story 1's contract so the Gamma screens show real data and the host can mount the module (story 13).

**Tests first**
- `ListPortalsUseCaseTest`: returns the environment's portals, default portal first then by creation date, never another environment's, with URL (story 8, null when not configured), default marker, `publishedApisCount` (D3), `createdAt`; `quota.used` = count; `quota.limit` per D6 for `universe`, `galaxy`, `planet`, `oss`, null license.
- `PortalQuotaDomainServiceTest` for D6 alone.
- `GetPortalUseCaseTest`: 404 for a portal of another environment.
- `PortalsResourceTest` (Jersey): 200 shapes match the spec; `logoUrl` points to `…/portals/{portalId}/logo` when the enabled theme has a logo and is null otherwise; 403 without `ENVIRONMENT_DOCUMENTATION[READ]`; 404 on unknown id.
- `PortalResourceTest` for the logo: returns the enabled next-gen theme's logo with its image content type; 404 when the theme has no logo; 404 for a portal of another environment; 403 without the permission.

**Files**
- Add `CORE/core/portal/use_case/ListPortalsUseCase.java`; extend or add a get use case for the Management API (the existing [`GetPortalUseCase`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/use_case/GetPortalUseCase.java) serves the Automation API — do not change its output).
- Add `CORE/core/portal/domain_service/PortalQuotaDomainService.java`, `PortalApiCountDomainService.java`.
- Add `gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/environment/PortalsResource.java`, `PortalResource.java` (with a `logo` sub-path, modelled on the API picture endpoint `/apis/{apiId}/picture`); register in `EnvironmentResource`.
- Add `gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/mapper/PortalMapper.java`.

**Acceptance criteria**
- `GET /management/v2/environments/{envId}/portals` returns the default portal with quota on an upgraded and on a new environment.
- The logo is served by the Management API; Gamma makes no Portal API call.
- Responses validate against the story 1 schemas (the Gamma MSW handlers' fixtures can be replaced by a recorded response without test changes).

### Story 10 · Management API v2: per-portal navigation endpoints

**Why.** The navigation and homepage editors (PORTAL-35, PORTAL-36) edit one portal's navigation. Existing endpoints must keep working for the classic console, so they act on the default portal. No screen in this epic.

**Tests first**
- For each operation of `/portal-navigation-items` (list, create, bulk, import, default pages, get, update, delete, fetch) and of `/portal-page-contents` (read and update a page's content): a test under `/portals/{portalId}/…` that only sees and changes that portal's items, and a test that the existing path still acts on the default portal.
- 404 when `portalId` is not in the environment; 404 for an item id that belongs to another portal or environment, never its data.
- Items created under a portal carry its id as the reference.

**Files**
- Change `openapi-environments.yaml`: `/portals/{portalId}/navigation-items…` mirroring `/portal-navigation-items…`, and `/portals/{portalId}/page-contents…` mirroring `/portal-page-contents…` (shared schemas, `portalId` path parameter).
- Change [`PortalNavigationItemsResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/environment/PortalNavigationItemsResource.java) / [`PortalNavigationItemResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/environment/PortalNavigationItemResource.java) / [`PortalPageContentsResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/environment/PortalPageContentsResource.java): take the portal from a path parameter or the default portal (`DefaultPortalDomainService.getDefault`); mount as sub-resources of `PortalResource` too.
- Change the `core/portal_page` use cases' `Input` records to carry a `PortalId`, and filter items by `referenceType = PORTAL`, `referenceId = portalId`: `ListPortalNavigationItemsUseCase`, `CreatePortalNavigationItemUseCase`, `BulkCreatePortalNavigationItemUseCase`, `ImportPortalNavigationUseCase`, `UpdatePortalNavigationItemUseCase`, `DeletePortalNavigationItemUseCase`, `GetPortalNavigationItemUseCase`, `FetchPortalNavigationItemUseCase`, `SeedDefaultPagesForPortalNavigationItemsUseCase`, `GetPortalPageContentByNavigationIdUseCase`, `UpdatePortalPageContentUseCase`.
- Change `PortalNavigationItemCriteria` (+ JDBC and Mongo repositories) to filter by reference, if story 9's API count has not added it already.

**Acceptance criteria**
- Navigation items and page contents of one portal are never returned or changed through another portal's path.
- Permissions match the existing endpoints (`ENVIRONMENT_DOCUMENTATION`, same actions).
- Existing console navigation editor works unchanged.

### Story 11 · Portal API: `/portal/{portalId}/...` navigation, search, API lists

**Why.** The next-gen portal must show one portal's navigation and APIs. Existing endpoints keep serving the default portal so current deployments and the classic portal are unaffected.

**Tests first**
- `GET /portal/{portalId}/portal-navigation-items` (all areas, homepage included), `/_search`, item by id, item content (`/{id}/content`): only that portal's items.
- `POST /portal/{portalId}/apis/_search` and the API list endpoints used by the catalog: only APIs reachable from that portal's navigation (D3), with today's visibility rules applied on top.
- Existing paths return exactly what they return today on an upgraded environment.
- 404 on unknown or other-environment `portalId`, for anonymous and authenticated users alike (no portal-existence leak through a different status).
- Security config test: each new path has the same anonymous and authenticated access as its unscoped equivalent.

**Files**
- Change [`portal-openapi.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml).
- Add `gravitee-apim-rest-api-portal-rest/.../resource/PortalScopedResource.java` (`@Path("portal/{portalId}")`) with sub-resources reusing [`PortalNavigationItemsResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/PortalNavigationItemsResource.java) and [`ApisResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApisResource.java) with a portal id.
- Change `ListPortalNavigationItemsUseCase`, `GetPortalNavigationItemUseCase`, `GetPortalPageContentByNavigationIdUseCase`, `GetVisiblePortalNavigationApisUseCase`, `GetVisiblePortalCatalogItemsUseCase` inputs to carry a `PortalId`.
- Change the Portal API's [`BasicSecurityConfigurerAdapter`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-security/src/main/java/io/gravitee/rest/api/portal/security/config/BasicSecurityConfigurerAdapter.java): give the new paths the same public and authenticated rules as their unscoped equivalents, so anonymous visitors are not locked out.

**Acceptance criteria**
- Epic backend item 7.
- Visibility, publication, user access and category rules are unchanged on the scoped paths.
- Theme endpoint is not portal-scoped (PORTAL-319).

### Story 12 · Next-gen portal: optional `portalId`, calls through `/portal/{portalId}/...`

**Why.** One deployment per portal (epic note "How a portal is found"). With no `portalId`, the deployment keeps working as today on the default portal.

**Tests first** (`config.service.spec.ts`, service specs)
- `portalId` from `assets/config.json` wins; otherwise `portalId` from `/ui/bootstrap`; neither → startup error state.
- Navigation (list, get, content, `_search`) and API list / `_search` services call `…/portal/{portalId}/…`; API details and the theme service keep their current paths.
- [`app-initializer.spec.ts`](gravitee-apim-portal-webui-next/src/app/app-initializer.spec.ts): a 404 on the scoped navigation (unknown `portalId`) navigates to `/503`, not to a blank page.

**Files**
- Change [`config.service.ts`](gravitee-apim-portal-webui-next/src/services/config.service.ts) (`Config.portalId?`, filled from the bootstrap when `config.json` has none; `portalBaseURL` getter), [`assets/config.json`](gravitee-apim-portal-webui-next/src/assets/config.json) and [`assets/config.prod.json`](gravitee-apim-portal-webui-next/src/assets/config.prod.json) (documented, absent by default).
- Change [`portal-navigation-items.service.ts`](gravitee-apim-portal-webui-next/src/services/portal-navigation-items.service.ts), [`api.service.ts`](gravitee-apim-portal-webui-next/src/services/api.service.ts) (list and `_search` only), and any `portal-pages` / `portal-categories` calls that story 11 scopes.
- Change [`catalog.component.ts`](gravitee-apim-portal-webui-next/src/app/catalog/catalog.component.ts), [`documentation.resolver.ts`](gravitee-apim-portal-webui-next/src/app/documentation/resolvers/documentation.resolver.ts), [`homepage-content.resolver.ts`](gravitee-apim-portal-webui-next/src/resolvers/homepage-content.resolver.ts) only where they build these URLs themselves.
- Change [`app-initializer.ts`](gravitee-apim-portal-webui-next/src/app/app-initializer.ts) if the 404 case is not already routed to `/503`.
- Document `portalId` in the portal's deployment docs / Helm values (check `gravitee-apim-distribution` and the Helm chart repository for a `config.json` template).

**Acceptance criteria**
- Epic "Done when": a next-gen portal deployment works with or without `portalId`; without it, it shows exactly what it showed before the epic.
- With `portalId`, every navigation, search and API-list request goes to `/portal/{portalId}/…`; the theme request does not.
- Login, logout, subscriptions and applications are unaffected.

### Story 13 · Gamma host opens the Developer Portals module

**Why.** Until now the host sends "Developer Portals" to the classic console, which kept stories 2–4 invisible. Once story 9 is released, the host mounts the module so admins land on the dashboard.

**Tests first**
- Host `AppRoutes.spec.tsx`: `/environments/:envHrid/portals/*` renders the remote module, not the redirect.
- `ShellLayout.spec.tsx` / `HomePage.spec.tsx`: the Developer Portals app and card link to the module path, in the same tab.

**Files**
- Change [`AppRoutes.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/app/AppRoutes.tsx), [`ShellLayout.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/shared/components/ShellLayout.tsx), [`HomePage.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/pages/home/HomePage.tsx): drop the portals special case.
- Delete [`PortalNextEditorRedirect.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/components/PortalNextEditorRedirect.tsx) and its spec; delete [`portal-next.ts`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/portal-next.ts) if nothing else uses it.
- Change [`modules.catalog.ts`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/modules.catalog.ts): drop the "classic console editor" comment.

**Acceptance criteria**
- Epic "Done when": the Gamma dashboard and Portals page show the default portal with its quota, and "View Portal" opens it in a new tab — checked on a local full stack and on a Cloud dev environment.
- Gamma no longer links to the classic console's portal editor; its features are rebuilt in Gamma by PORTAL-35, PORTAL-36, PORTAL-140 and the other portal epics.

### Epic verification (after stories 12 and 13)

Run on a JDBC and a MongoDB distribution built with `task build-distribution`, so the changed core is bundled (check with `task which-engine`):

1. Seed a 4.14 install with a homepage, a top navbar with folders, pages and links, published APIs, an API listing and a custom theme. Note the next-gen portal URL.
2. Upgrade. The next-gen portal shows the same content, theme and URL (epic "Done when" item 1).
3. Gamma dashboard and Portals page: one default portal, quota "1 / <tier limit>", the right number of APIs published, the theme's logo. "View Portal" opens the portal in a new tab (item 2).
4. `GET /management/v2/environments/{env}/portals` and the per-portal navigation endpoints answer for the default portal (item 3).
5. The next-gen portal shows the same portal with and without `portalId` in `config.json` (item 4).

---

## 6. Automation API / CRD impact

No change to the Automation API module in this epic (owned by `@gravitee-io/gko`, `@gravitee-io/tech-lead`). Story 5 adds `defaultPortal`, `url` and `createdAt` to the core `Portal` model, which the Automation API's portal mapper reads; they are **not** mapped there. GKO-3426 mirrors them. Story 5 must check that the Automation API's portal `PUT` still round-trips (a `PUT` replaces the whole definition and must not reset `defaultPortal`): `CreateOrUpdatePortalUseCase` keeps the stored `defaultPortal`, `url` and `createdAt` when the input lacks them. That use case sits in `core/portal`, outside the owned surface, but raise it with the GKO team on the story 5 PR.

## 7. Security-sensitive changes

To flag in the PR descriptions:
- Stories 1, 9, 10, 11: new endpoints and their permission checks (D9; Portal API visibility rules on portal-scoped API lists). Stories 10 and 11: isolation between portals and between environments.
- Story 11: change to the Portal API security configuration for the new public paths.
- Story 8: a new field on the public `/ui/bootstrap` response (`portalId`, not secret).
- Stories 5, 7: persistence schema change and data migration.
- Story 12: a new deployment configuration key (`portalId`).

## 8. Out of scope (from the epic)

Navigation editor (PORTAL-36) and homepage editor (PORTAL-35); theme per portal and theme screen (PORTAL-319); Overview, settings, rename, delete (PORTAL-140); creating portals, enforcing the license limit, publishing APIs to specific portals (PORTAL-137); per-portal identity providers (PORTAL-142), categories (PORTAL-141), general settings, subscription forms; multiple variants of API documentation per portal.
