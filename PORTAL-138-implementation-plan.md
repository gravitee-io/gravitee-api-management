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
| Portal bootstrap | [`PortalUIBootstrapResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/bootstrap/PortalUIBootstrapResource.java) returns `organizationId`, `environmentId`, `baseURL`. Next-gen [`ConfigService`](gravitee-apim-portal-webui-next/src/services/config.service.ts) reads `assets/config.json` then `/ui/bootstrap`. | Add `defaultPortalId`; add optional `portalId` to `config.json`. |
| Management API v2 | No `/portals` endpoint. Navigation items live under `/portal-navigation-items` in [`openapi-environments.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-model/src/main/resources/openapi/openapi-environments.yaml:1104). License tier is exposed by [`GraviteeLicenseResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/installation/GraviteeLicenseResource.java:51). | New portals endpoints go in `openapi-environments.yaml`. |

---

## 2. Decisions this plan takes (challenge them in review)

- **D1 — Where the screens live.** "Gamma dashboard" is the landing page of the Developer Portals module; "Portals page" is the second item of the module's sidebar ("Portal menu"). Routes, relative to the module mount `/environments/:envHrid/portals`: `dashboard` (index redirects here), `list`, `:portalId/overview`. Every portal-scoped route starts with `:portalId`, so PORTAL-35/36/140 add pages under it without route changes. *Confirm the URL segments with design.*
- **D2 — Contract first, frontend second, cutover last.** Story 1 only edits the OpenAPI spec (generated models, no resource). Stories 2–4 build against it with MSW. Story 13 mounts the module in the host after story 9 has shipped.
- **D3 — "APIs published" on a portal** = number of distinct APIs referenced by `API` navigation items of that portal that are published to the portal (the set the next-gen portal catalog shows anonymous-or-not, ignoring per-user visibility). Listings are materialized into navigation items, so this works for console-only and automation-managed environments alike. *Confirm with Product; the alternative, "APIs in listings", reads 0 for every console-only customer.*
- **D4 — Upgrade must not change what the next-gen portal shows.** Today it shows every navigation item of the environment, whatever its `referenceId`. Rule for the upgrader, per environment:
  - no portal exists → create the default portal, re-point zero-UUID items, listings and docs onto it;
  - exactly one portal exists (created through the Automation API) → mark it default and re-point zero-UUID items onto it.

  No customer has more than one portal per environment today, Automation API included. If the upgrader meets several, it fails for that environment with an error naming it and the portals, and changes nothing there, rather than guessing which one is the default.
- **D5 — Portal URL.** The default portal's URL is never stored: it is `getPortalUrl(env)` + `/next`, read at request time. Story 5 adds a nullable `url` column for non-default portals (PORTAL-137) so the schema migrates once; nothing writes it in this epic. If Product drops `/next` for Gamma users (open question Q1), only the resolver in story 8 changes.
- **D6 — Quota.** `limit` comes from the license tier: `universe` → 10, anything else (trial, `planet`, `galaxy`, `oss`, unknown, no license) → 1. Defined once in a core domain service so PORTAL-137's create check reuses it. `used` = number of portals in the environment.
- **D7 — Logo.** Until PORTAL-319, every portal's logo is the logo of the environment's enabled next-gen theme, returned as the stored string (data URI). The frontend falls back to the portal's initials when it is empty.
- **D8 — Fuzzy search** runs in the browser over at most 10 portals with a small in-module subsequence matcher (no new dependency). The list API neither pages nor searches.
- **D9 — Permissions.** `GET /portals` and `GET /portals/{portalId}` require `ENVIRONMENT_DOCUMENTATION[READ]`, the permission of the existing navigation endpoints. The frontend hides "Configure" without `ENVIRONMENT_DOCUMENTATION[UPDATE]`. *Security-sensitive: confirm in story 1 review.*

## 3. Open questions (owner)

| # | Question | Blocks | Default if unanswered |
| --- | --- | --- | --- |
| Q1 | Gamma users' next-gen portal URL without `/next`: how is it derived? (Product, Cloud team) | Story 8 | Keep `/next`. |
| Q2 | What does "% Completion" on portal tiles measure? (Product) | Story 3 | Not shown. |
| Q3 | How is a **trial** license recognized in APIM? `License.getTier()` has no trial value. (Licensing) | Story 9 | Trial falls under "anything else → 1", which is already the trial limit. |
| Q4 | Is the default portal marked on tiles and rows, and how? (Design) | Stories 3, 4 | "Default" badge next to the name. |
| Q5 | After the host cutover, where do admins open the classic portal editor until PORTAL-36/140 land? (Product) | Story 13 | "Open portal editor" link on the Overview placeholder. |

---

## 4. Sequencing

| # | Story | Side | Depends on | Est. (dev-days) |
| --- | --- | --- | --- | --- |
| 1 | Portals contract in OpenAPI | Contract | — | 0.5 |
| 2 | Portals module foundation: routes, data layer, tests | Gamma FE | 1 | 2 *(proposed)* |
| 3 | Dashboard: quota, tiles/table, search | Gamma FE | 2 | 3 *(proposed)* |
| 4 | Portals page | Gamma FE | 2 (3 for shared components) | 1.5 *(proposed)* |
| 5 | Portal model: default marker, URL, creation date; drop `PortalId.ZERO` | BE | — | 2 |
| 6 | Default portal for every new environment | BE | 5 | 2 |
| 7 | Upgrader: default portal for existing environments, upgrade tests | BE | 5, 6 | 5 |
| 8 | Default portal URL and `defaultPortalId` in Portal API bootstrap | BE | 5 | 1 |
| 9 | Management API v2: list and get portals with quota | BE | 1, 7, 8 | 3 |
| 10 | Management API v2: per-portal navigation endpoints | BE | 7 | 2.5 |
| 11 | Portal API: `/portal/{portalId}/...` navigation, search, API lists | BE | 7 | 3.5 |
| 12 | Next-gen portal: optional `portalId`, calls through `/portal/{portalId}/...` | Portal FE | 8, 11 | 1.5 |
| 13 | Gamma host opens the Developer Portals module | Gamma FE | 2–4, 9 released | 1 *(proposed)* |

Backend total 20.5 dev-days (≈ the epic's 4.5 dev-weeks once review is counted). Frontend 7.5 dev-days proposed.

Stories 10, 11 and 12 can run in parallel with 13 once 7 has merged.

---

## 5. Stories

Each story lists the test cases to write **first** (RED), then the files. Paths are from the repository root. `MODULE` = `gravitee-gamma/gravitee-gamma-module-portals/src/main/ui`; `CORE` = `gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim`.

### Story 1 · Portals contract in OpenAPI

**Why.** The frontend goes first. Without an agreed response shape, the screens would be built on guesses and reworked when the backend lands. The repository is API-first: the spec changes before any JAX-RS code. Generation is models-only, so merging the spec changes no runtime behaviour.

**Files**
- Change [`openapi-environments.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-model/src/main/resources/openapi/openapi-environments.yaml):
  - `GET /portals` → `PortalsResponse { data: Portal[], quota: PortalQuota }`; no `page`/`perPage`/`q`.
  - `GET /portals/{portalId}` → `Portal`; `404` when the portal is not in the environment.
  - `Portal { id, name, url, default: boolean, logo?: string, apisPublished: integer, createdAt: date-time }`.
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
- `config/routes.spec.ts`: `getActiveNavKey` returns `dashboard` for the module root and `/dashboard`, `list` for `/list`, and `list` for `/:portalId/overview` (portal pages sit under the Portals menu item).
- `features/portals/hooks/usePortals.spec.tsx` (MSW): returns `data` and `quota` from `GET …/environments/:envId/portals`; exposes an error state on 500; refetches when the environment changes.
- `features/portals/hooks/usePortal.spec.tsx`: returns a portal; exposes not-found on 404.
- `features/portals/utils/matchPortals.spec.ts`: empty query returns all portals in input order; case-insensitive; matches a subsequence (`"dvprt"` → "Dev Portal"); ranks contiguous matches before scattered ones; ignores surrounding spaces.
- `app/AppRoutes.spec.tsx`: the index redirects to `dashboard`; `/:portalId` redirects to `/:portalId/overview`; the overview placeholder shows the portal name and, for an unknown id, a "Portal not found" state with a link back to the list.

**Files**
- Change [`MODULE/app/AppRoutes.tsx`](gravitee-gamma/gravitee-gamma-module-portals/src/main/ui/app/AppRoutes.tsx): `QueryClientProvider`, a `ModuleLayout` that sets the sidebar and breadcrumbs through `useLayoutConfig` (as in the APIM module's `AppRoutes.tsx`), routes from D1.
- Add `MODULE/config/routes.ts`, `MODULE/config/navigation.ts` (two items: Dashboard, Portals), modelled on `gravitee-gamma-module-apim/src/main/ui/config/`.
- Add `MODULE/app/useModuleNavigation.ts`, `MODULE/app/useModuleBreadcrumbs.ts` (copy-adapt from the APIM module; do not share code across modules).
- Add `MODULE/shared/api/apimClient.ts` re-exporting `gamma-ui-shared/src/api` as the APIM module does.
- Add `MODULE/features/portals/types/portal.ts` — `Portal`, `PortalQuota`, `PortalsResponse`, mirroring story 1's schemas.
- Add `MODULE/features/portals/services/portals.ts` — `listPortals(envId)`, `getPortal(envId, portalId)` via `apimFetchJsonV2`.
- Add `MODULE/features/portals/utils/queryKeys.ts`, `MODULE/features/portals/utils/matchPortals.ts`.
- Add `MODULE/features/portals/hooks/usePortals.ts`, `usePortal.ts`, `usePortalIdParam.ts`.
- Add `MODULE/features/portals/pages/PortalOverviewPlaceholderPage.tsx` (name, "View Portal", back link; PORTAL-140 replaces it).
- Add `MODULE/testing/server.ts`, `MODULE/testing/handlers/{index,portals.handlers}.ts`, `MODULE/testing/factories.ts` (`aPortal()`, `aPortalsResponse()`), `MODULE/test-setup.ts`.
- Change `gravitee-gamma/gravitee-gamma-module-portals/jest.config.ts` (`setupFilesAfterEnv` → `test-setup.ts`, `jest-fixed-jsdom` environment as in the APIM module) and `package.json` only if MSW / Testing Library are not already hoisted from the root.

**Acceptance criteria**
- `yarn nx serve gravitee-gamma-module-portals` shows the sidebar with Dashboard and Portals; the module root lands on Dashboard.
- Every portal-scoped URL carries `portalId`; refreshing `/:portalId/overview` reloads the same portal.
- The data layer calls only `GET /portals` and `GET /portals/{portalId}` as specified in story 1.
- `yarn nx test gravitee-gamma-module-portals` and `yarn nx lint gravitee-gamma-module-portals` pass with no network access.

### Story 3 · Dashboard: quota, tiles/table, search

**Why.** This is the screen the epic's "Done when" names: admins see their portal, its quota, and open it. It also sets the components the Portals page reuses (portal row, "View Portal", quota, search box).

**Tests first** (`features/portals/pages/PortalsDashboardPage.spec.tsx`, MSW)
- Shows the quota above the list as "1 / 10" from `quota.used` / `quota.limit`.
- Shows each portal's logo (or initials when `logo` is empty), name, "Default" marker on the default portal (Q4), and number of APIs published.
- Tiles by default; the toggle switches to a table and back; the choice survives a re-render of the page (kept in the URL as `?view=table`).
- With 4 portals, tiles show 3 and a "View all Portals" link to `list`; with 6 portals, the table shows 5 and the same link; with 1 portal, no link.
- Typing in search filters with `matchPortals`; the 3/5 limit applies to the filtered result; no match shows an empty state.
- "View Portal" is a link to `portal.url` with `target="_blank"` and `rel="noopener noreferrer"`; it is hidden, not broken, when `url` is empty.
- "Configure" navigates to `:portalId/overview`; it is hidden without `ENVIRONMENT_DOCUMENTATION[UPDATE]` (mock `useHasPermission`).
- "New Portal" is rendered and disabled, with a tooltip saying creating portals is coming soon.
- Loading shows a skeleton; an API error shows an error state with "Retry".

**Files**
- Add `MODULE/features/portals/pages/PortalsDashboardPage.tsx`.
- Add `MODULE/features/portals/components/`: `PortalQuota.tsx`, `PortalTile.tsx`, `PortalTiles.tsx`, `PortalTable.tsx` (graphene `DataTable`), `PortalLogo.tsx`, `ViewPortalLink.tsx`, `PortalSearchInput.tsx`, `NewPortalButton.tsx`, `PortalsViewToggle.tsx`, `index.ts`.
- Change `MODULE/app/AppRoutes.tsx` to route `dashboard` to the page.

**Acceptance criteria**
- Matches the epic's frontend item 1 and the PRD layout; design sign-off on a screenshot in the PR.
- No API call other than `GET /portals`; search and limits run in the browser.
- Keyboard: the toggle, search, links and buttons are reachable and labelled (axe check in the spec passes).

### Story 4 · Portals page

**Why.** The Portal menu's full list: the dashboard only shows a preview, and admins need one place to find any portal and open its configuration.

**Tests first** (`features/portals/pages/PortalsListPage.spec.tsx`)
- Shows the quota above a table of **all** portals (no 5-row limit), with search.
- Clicking a row navigates to `:portalId/overview`; "View Portal" in a row opens the URL in a new tab **without** navigating the row.
- Disabled "New Portal" as on the dashboard.
- Empty search result and error states as on the dashboard.

**Files**
- Add `MODULE/features/portals/pages/PortalsListPage.tsx`.
- Change `MODULE/features/portals/components/PortalTable.tsx`: optional `limit`, optional `onRowClick`.
- Change `MODULE/app/AppRoutes.tsx` to route `list`.

**Acceptance criteria**
- Matches the epic's frontend item 2.
- The breadcrumb reads "Developer Portals / Portals"; on an overview page, "Developer Portals / Portals / <portal name>".

### Story 5 · Portal model: default marker, URL, creation date; drop `PortalId.ZERO`

**Why.** A portal must be a real record that can be told apart as the default, so requests without a `portalId` have a portal to act on. The zero UUID placeholder encodes "no portal" and must go before any code relies on real ids. The URL column is added now so the schema migrates once (D5); the creation date serves the list (story 9); story 7 backfills it for existing portals with the upgrade time.

**Tests first**
- `PortalTest`: `Portal.createDefault(env, org, name)` is default; `Portal.create(...)` is not; `createdAt` is set.
- `PortalIdTest`: `PortalId.of("00000000-…")` returns a plain id (no special instance).
- Repository contract tests (`gravitee-apim-repository-test`): `PortalRepository` round-trips `defaultPortal`, `url`, `createdAt`; new `findDefaultByEnvironmentId(envId)` returns it, or empty.
- JDBC and Mongo test suites run the same contract.

**Files**
- Change [`CORE/core/portal/model/Portal.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/model/Portal.java): `defaultPortal`, `url`, `createdAt`; factory `createDefault`; `isDefault()`.
- Change [`CORE/core/portal/model/PortalId.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/model/PortalId.java): remove `ZERO`.
- Change [`CORE/core/portal_page/model/NavigationItemReference.java`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal_page/model/NavigationItemReference.java): remove `PortalReference.DEFAULT` and `defaultReference()`; callers resolve the default portal through a new `DefaultPortalDomainService` (`CORE/core/portal/domain_service/DefaultPortalDomainService.java`, `findDefault(envId)` failing loudly when absent). List the callers in the PR; they include `PortalNavigationItemDomainService` and the infra `PortalNavigationItemAdapter`.
- Change `CORE/core/portal/crud_service/PortalCrudService.java` (+ infra impl and the in-memory test double): `findDefaultByEnvironmentId`.
- Change [`PortalRepository`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/api/PortalRepository.java), [`model/Portal.java`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/model/Portal.java), `JdbcPortalRepository`, `MongoPortalRepository`, `PortalMongo`, `PortalAdapter` (infra).
- Add Liquibase changelog `gravitee-apim-repository-jdbc/src/main/resources/liquibase/changelogs/v4_15_0/NN_add_default_url_created_at_to_portals.yml` (+ master changelog entry); Mongo needs no migration, add an index on `(environmentId, defaultPortal)`.
- Keep the zero-UUID defaults on `PortalNavigationItem.referenceId` / `PortalNavigationItemMongo` until story 7 has re-pointed the data; remove them in story 7.

**Acceptance criteria**
- Builds and all existing portal, navigation and automation tests pass unchanged in behaviour.
- `PortalId.ZERO` and `PortalReference.DEFAULT` no longer exist.
- JDBC (all supported databases in CI) and Mongo store and read the new fields.
- Automation API output is unchanged (the new fields are not mapped there — see §6).

### Story 6 · Default portal for every new environment

**Why.** A new environment must start with the portal that requests without a `portalId` act on; otherwise the first navigation item created in the console has nowhere to go.

**Tests first**
- `CreateDefaultPortalUseCaseTest`: creates a default portal named after the environment ("Developer Portal" if blank, *confirm copy*); is idempotent — a second run with a default already present changes nothing, so an environment never has two defaults (existing environments are story 7's job).
- `DefaultPortalInitializerTest`: runs for each environment at start-up and for an environment created through Cockpit.

**Files**
- Add `CORE/core/portal/use_case/CreateDefaultPortalUseCase.java`.
- Add `gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/upgrade/initializer/DefaultPortalInitializer.java` extending [`EnvironmentInitializer`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/upgrade/initializer/EnvironmentInitializer.java) (same pattern as `DefaultThemeInitializer`).
- Change the Cockpit environment creation path ([`EnvironmentCommandHandler`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/cockpit/command/handler/EnvironmentCommandHandler.java)) if it does not already run environment initializers — check first.
- Change `PortalNavigationItemDomainService` and friends: a console-created item gets the default portal's id.

**Acceptance criteria**
- Every new environment (self-hosted start-up, Cockpit, Management API) has exactly one default portal.
- Creating a navigation item in a new environment stores the default portal's id as `referenceId`.

### Story 7 · Upgrader: default portal for existing environments, upgrade tests

**Why.** Existing environments carry data on the zero UUID. The epic's first "Done when" is that an upgraded install looks exactly as before: same content, theme and URL. This is the story that can break customers, so it carries the upgrade tests on both databases (epic items 3 and 9).

**Tests first**
- `DefaultPortalUpgraderTest` (in-memory), no portal → default created; one automation portal → marked default; default already present → no-op; several portals → that environment fails with an error naming it, its data unchanged, and the other environments still upgrade; a re-run after a partial failure finishes the job; existing portals get `createdAt`.
- Re-pointing: navigation items with `referenceType=PORTAL, referenceId=zero` → default id; `PortalListing.portalId=zero` (if any) → default id; documentation pages attached through those items keep their links; items with `referenceType=API` are untouched; the navigation `rootId` sentinel is untouched.
- JDBC and Mongo integration tests seeded with a 4.14-shaped dataset (navigation in all areas including the homepage, listings, pages): after the upgrade the Portal API returns the same navigation tree, homepage and API list as before, item for item.

**Files**
- Add `gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/upgrade/upgrader/DefaultPortalUpgrader.java` (order after [`PortalNavigationItemRootIdUpgrader`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/rest/api/service/impl/upgrade/upgrader/PortalNavigationItemRootIdUpgrader.java); register its order constant in `UpgraderOrder`).
- Add repository methods for the bulk re-point: `PortalNavigationItemRepository.updateReferenceId(envId, PORTAL, from, to)`, `PortalListingRepository.updatePortalId(envId, from, to)` (+ JDBC, Mongo, contract tests).
- Remove the zero-UUID defaults from [`PortalNavigationItem`](gravitee-apim-repository/gravitee-apim-repository-api/src/main/java/io/gravitee/repository/management/model/PortalNavigationItem.java) and `PortalNavigationItemMongo`; leave the existing Liquibase default (dropping a column default is a separate, later migration) and the Mongo `PortalNavigationItemDefaultReferenceMongoUpgrader` constant untouched.
- Add upgrade test fixtures under `gravitee-apim-repository-test/src/test/resources/data/` (or the rest-api integration test resources, whichever the existing upgrader tests use).

**Acceptance criteria**
- After upgrade, every environment has exactly one default portal, and no `PORTAL`-type navigation item or listing references the zero UUID.
- A next-gen portal deployment without `portalId` shows the same navigation, homepage and APIs as before the upgrade (JDBC and Mongo tests).
- The upgrader is idempotent and logs one line per environment it changes, with the environment id.
- Rollback note in the PR: the upgrade is data-only and forward-compatible; reverting the code leaves items on real ids, which 4.14 already reads as before (it does not filter by portal).

### Story 8 · Default portal URL and `defaultPortalId` in the Portal API bootstrap

**Why.** Gamma needs "View Portal" to open the right URL, and the next-gen portal needs to know which portal to load when its config names none. Both are read at request time so Cloud access points and environment settings stay the single source of truth (D5).

**Tests first**
- `PortalUrlDomainServiceTest`: default portal → `getPortalUrl(env)` + `/next` (multi-tenant access point and self-hosted setting); trailing slash handled; non-default portal → its stored `url`, else null.
- `PortalUIBootstrapResourceTest`: response contains `defaultPortalId` for the resolved environment, with and without `?environmentId=`.

**Files**
- Add `CORE/core/portal/domain_service/PortalUrlDomainService.java` (uses [`InstallationAccessQueryService`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/installation/query_service/InstallationAccessQueryService.java)).
- Change [`PortalUIBootstrapResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/bootstrap/PortalUIBootstrapResource.java) and `PortalUIBootstrapEntity`: add `defaultPortalId`.
- Change [`portal-openapi.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml) if the bootstrap is documented there.

**Acceptance criteria**
- The resolved URL equals the one the console's "open next-gen portal" redirect uses today.
- `/ui/bootstrap` keeps its existing fields; `defaultPortalId` is added.

### Story 9 · Management API v2: list and get portals with quota

**Why.** Implements story 1's contract so the Gamma screens show real data and the host can mount the module (story 13).

**Tests first**
- `ListPortalsUseCaseTest`: returns the environment's portals with URL (story 8), default marker, logo (enabled theme's, D7), `apisPublished` (D3), `createdAt`; `quota.used` = count; `quota.limit` per D6 for `universe`, `galaxy`, `planet`, `oss`, null license.
- `PortalQuotaDomainServiceTest` for D6 alone.
- `GetPortalUseCaseTest`: 404 for a portal of another environment.
- `PortalsResourceTest` (Jersey): 200 shapes match the spec; 403 without `ENVIRONMENT_DOCUMENTATION[READ]`; 404 on unknown id.

**Files**
- Add `CORE/core/portal/use_case/ListPortalsUseCase.java`; extend or add a get use case for the Management API (the existing [`GetPortalUseCase`](gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/portal/use_case/GetPortalUseCase.java) serves the Automation API — do not change its output).
- Add `CORE/core/portal/domain_service/PortalQuotaDomainService.java`, `PortalApiCountDomainService.java`.
- Add `gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/environment/PortalsResource.java`, `PortalResource.java`; register in `EnvironmentResource`.
- Add `gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/mapper/PortalMapper.java`.

**Acceptance criteria**
- `GET /management/v2/environments/{envId}/portals` returns the default portal with quota on an upgraded and on a new environment.
- Responses validate against the story 1 schemas (the Gamma MSW handlers' fixtures can be replaced by a recorded response without test changes).

### Story 10 · Management API v2: per-portal navigation endpoints

**Why.** The navigation and homepage editors (PORTAL-35, PORTAL-36) edit one portal's navigation. Existing endpoints must keep working for the classic console, so they act on the default portal. No screen in this epic.

**Tests first**
- For each operation of `/portal-navigation-items` (list, create, bulk, import, default pages, get, update, delete, fetch): a test under `/portals/{portalId}/navigation-items` that only sees and changes that portal's items, and a test that the existing path still acts on the default portal.
- 404 when `portalId` is not in the environment.

**Files**
- Change `openapi-environments.yaml`: `/portals/{portalId}/navigation-items…` mirroring `/portal-navigation-items…` (shared schemas, `portalId` path parameter).
- Change [`PortalNavigationItemsResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/environment/PortalNavigationItemsResource.java) / [`PortalNavigationItemResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/resource/environment/PortalNavigationItemResource.java): take the portal from a path parameter or the default portal; mount as a sub-resource of `PortalResource`.
- Change the `core/portal_page` use cases' `Input` records to carry a `PortalId` instead of reading the default.

**Acceptance criteria**
- Navigation items of one portal are never returned or changed through another portal's path.
- Existing console navigation editor works unchanged.

### Story 11 · Portal API: `/portal/{portalId}/...` navigation, search, API lists

**Why.** The next-gen portal must show one portal's navigation and APIs. Existing endpoints keep serving the default portal so current deployments and the classic portal are unaffected.

**Tests first**
- `GET /portal/{portalId}/portal-navigation-items` (all areas, homepage included), `/_search`, item by id: only that portal's items.
- `POST /portal/{portalId}/apis/_search` and the API list endpoints used by the catalog: only APIs reachable from that portal's navigation (D3), with today's visibility rules applied on top.
- Existing paths return exactly what they return today on an upgraded environment.
- 404 on unknown or other-environment `portalId`, for anonymous and authenticated users alike (no portal-existence leak through a different status).

**Files**
- Change [`portal-openapi.yaml`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml).
- Add `gravitee-apim-rest-api-portal-rest/.../resource/PortalScopedResource.java` (`@Path("portal/{portalId}")`) with sub-resources reusing [`PortalNavigationItemsResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/PortalNavigationItemsResource.java) and [`ApisResource`](gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/java/io/gravitee/rest/api/portal/rest/resource/ApisResource.java) with a portal id.
- Change `ListPortalNavigationItemsUseCase`, `GetVisiblePortalNavigationApisUseCase`, `GetVisiblePortalCatalogItemsUseCase` inputs to carry a `PortalId`.

**Acceptance criteria**
- Epic backend item 7.
- Theme endpoint is not portal-scoped (PORTAL-319).

### Story 12 · Next-gen portal: optional `portalId`, calls through `/portal/{portalId}/...`

**Why.** One deployment per portal (epic note "How a portal is found"). With no `portalId`, the deployment keeps working as today on the default portal.

**Tests first** (`config.service.spec.ts`, service specs)
- `portalId` from `assets/config.json` wins; otherwise `defaultPortalId` from `/ui/bootstrap`; neither → startup error state.
- Navigation, search and API list services call `…/portal/{portalId}/…`; the theme service still calls `/theme`.

**Files**
- Change [`config.service.ts`](gravitee-apim-portal-webui-next/src/services/config.service.ts) (`Config.portalId?`, `Config.defaultPortalId`, `portalBaseURL` getter), [`assets/config.json`](gravitee-apim-portal-webui-next/src/assets/config.json) (documented, absent by default).
- Change [`portal-navigation-items.service.ts`](gravitee-apim-portal-webui-next/src/services/portal-navigation-items.service.ts), the API search / list services, and any `portal-pages` / `portal-categories` calls that story 11 scopes.
- Document `portalId` in the portal's deployment docs / Helm values (check `helm/` for a `config.json` template).

**Acceptance criteria**
- Epic "Done when": a next-gen portal deployment works with or without `portalId`.

### Story 13 · Gamma host opens the Developer Portals module

**Why.** Until now the host sends "Developer Portals" to the classic console, which kept stories 2–4 invisible. Once story 9 is released, the host mounts the module so admins land on the dashboard.

**Tests first**
- Host `AppRoutes.spec.tsx`: `/environments/:envHrid/portals/*` renders the remote module, not the redirect.
- `ShellLayout.spec.tsx` / `HomePage.spec.tsx`: the Developer Portals app and card link to the module path, in the same tab.
- Module: the overview placeholder shows "Open portal editor" linking to the classic console editor (Q5), using `consoleUrl` from the Gamma bootstrap.

**Files**
- Change [`AppRoutes.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/app/AppRoutes.tsx), [`ShellLayout.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/shared/components/ShellLayout.tsx), [`HomePage.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/pages/home/HomePage.tsx): drop the portals special case.
- Delete [`PortalNextEditorRedirect.tsx`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/components/PortalNextEditorRedirect.tsx) and its spec; delete [`portal-next.ts`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/portal-next.ts) if nothing else uses it.
- Change [`gamma-ui-shared/src/api/apimClient.ts`](gravitee-gamma/gamma-ui-shared/src/api/apimClient.ts): keep `consoleUrl` from the Gamma bootstrap in `ApimBootstrap` (the [Gamma bootstrap](gravitee-gamma/gravitee-gamma-rest-api/src/main/java/io/gravitee/gamma/rest/resources/GammaUIResource.java:92) already returns it).
- Change `MODULE/features/portals/pages/PortalOverviewPlaceholderPage.tsx`: "Open portal editor" link.
- Change [`modules.catalog.ts`](gravitee-gamma/gravitee-gamma-control-plane-webui/src/features/modules/modules.catalog.ts): drop the "classic console editor" comment.

**Acceptance criteria**
- Epic "Done when": the Gamma dashboard and Portals page show the default portal with its quota, and "View Portal" opens it in a new tab — checked on a local full stack and on a Cloud dev environment.
- Admins can still reach the classic portal editor in at most two clicks.

---

## 6. Automation API / CRD impact

No change to the Automation API module in this epic (owned by `@gravitee-io/gko`, `@gravitee-io/tech-lead`). Story 5 adds `defaultPortal`, `url` and `createdAt` to the core `Portal` model, which the Automation API's portal mapper reads; they are **not** mapped there. GKO-3426 mirrors them. Story 5 must check that the Automation API's portal `PUT` still round-trips (a `PUT` replaces the whole definition and must not reset `defaultPortal`): `CreateOrUpdatePortalUseCase` keeps the stored `defaultPortal`, `url` and `createdAt` when the input lacks them. That use case sits in `core/portal`, outside the owned surface, but raise it with the GKO team on the story 5 PR.

## 7. Security-sensitive changes

To flag in the PR descriptions:
- Stories 1, 9, 10, 11: new endpoints and their permission checks (D9; Portal API visibility rules on portal-scoped API lists).
- Stories 5, 7: persistence schema change and data migration.
- Story 12: a new deployment configuration key (`portalId`).

## 8. Out of scope (from the epic)

Navigation editor (PORTAL-36) and homepage editor (PORTAL-35); theme per portal and theme screen (PORTAL-319); Overview, settings, rename, delete (PORTAL-140); creating portals, enforcing the license limit, publishing APIs to specific portals (PORTAL-137); per-portal identity providers (PORTAL-142), categories (PORTAL-141), general settings, subscription forms; multiple variants of API documentation per portal.
