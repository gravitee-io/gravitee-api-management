# Playwright E2E Tests

Playwright suite for APIM Console UI testing, replacing the Cypress suite at
[`../ui-test/`](../ui-test/). Tracked under [APIM-14926](https://gravitee.atlassian.net/browse/APIM-14926).

Folder shape and conventions are aligned with the Playwright suites in
[gravitee-access-management](https://github.com/gravitee-io/gravitee-access-management/tree/master/gravitee-am-test/playwright)
and gravitee-cockpit, for consistency across Gravitee.io repos.

**Migration status**: this suite is being built up incrementally alongside the existing Cypress
suite. Cypress remains the CI-gating suite until the migration completes and a soak period has
passed — see APIM-14926 for the PR-by-PR rollout.

## One source of truth for the API

This suite does **not** define its own API clients, models, or credentials. It reuses exactly what
the Jest `api-test/` suite already uses, so the two can never drift apart:

| Concern                          | Where it comes from                                                                                                                                    | Do not                                         |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------- |
| API operations + models          | `@gravitee/management-webclient-sdk`, `@gravitee/management-v2-webclient-sdk`, `@gravitee/portal-webclient-sdk` — **generated from the OpenAPI specs** | hand-write request URLs or response interfaces |
| Auth config, base URLs, personas | `@gravitee/utils/configuration` (`ADMIN_USER`, `forManagementAsAdminUser()`, …), sourced from [`../.env`](../.env)                                     | redeclare credentials or base URLs             |
| Setup / teardown helpers         | `@gravitee/utils/management` (`teardownApisAndApplications`, …)                                                                                        | reimplement teardown                           |
| Gateway calls with retries       | `@gravitee/utils/apim-http` (`fetchGatewaySuccess`, …)                                                                                                 | write your own retry loop                      |
| Test data builders               | `@gravitee/fixtures/management/*Faker`                                                                                                                 | hand-roll payloads                             |

Only genuinely Playwright-specific things live here: browser auth/`storageState`, page objects,
fixtures, and the Console UI base URL.

## Getting started

**1. Generate the SDK clients** (once, and again whenever the OpenAPI specs change). The v1 spec is
read from the REST API module's build output, so that module must be built first:

```bash
yarn update:sdk
```

**2. Start a local APIM stack** — see `CONTRIBUTING.adoc` ("AI Agent Context (Docker Compose Full
Stack)"), or from this package:

```bash
yarn apim:serve
```

**3. Install browsers and run:**

```bash
npx playwright install chromium
yarn pw
```

| Script           | Purpose                                                               |
| ---------------- | --------------------------------------------------------------------- |
| `yarn pw`        | Run all specs headless against the local stack                        |
| `yarn pw:headed` | Same, with a visible browser                                          |
| `yarn pw:ui`     | Playwright's interactive UI mode                                      |
| `yarn pw:debug`  | Debug mode (step through)                                             |
| `yarn pw:report` | Open the last HTML report                                             |
| `yarn pw:ci`     | What the container runs; base URLs come from the environment          |
| `yarn test:pw`   | Full containerized run — brings up the stack and runs the suite in it |

> **The default local stack runs released images.** `.env` pins `APIM_TAG`, so `yarn apim:serve`
> starts whatever release that names — not your working tree. UI specs assert against markup from
> the Console under test, so run them either against a tag close to your branch
> (`APIM_TAG=4.12 yarn apim:serve`) or against a locally served Console. CI has no such gap: it
> builds images from the branch and passes that tag through.

### Configuration

All service URLs and credentials come from [`../.env`](../.env), shared with the Jest suite —
`CONSOLE_BASE_URL`, `MANAGEMENT_BASE_URL`, `GATEWAY_BASE_URL`, `ADMIN_USERNAME`/`ADMIN_PASSWORD`,
`API_USERNAME`/`API_PASSWORD`, and so on. Anything set in the environment wins over `.env`, which is
how the container retargets the suite at the nginx-fronted stack without editing any file.

## Folder structure

```
playwright/
  playwright.config.ts   projects (setup / chromium), reporters, baseURL wiring
  fixtures/               test.extend fixtures — auth (global.setup.ts), page objects, data setup/teardown
  pages/                  Page Object Model — locators (as getters) + actions only, zero assertions
  utils/                  Playwright-specific helpers only (see "one source of truth" above)
  tests/                  spec files, organized by feature area
```

## Conventions

- **Import `test`/`expect` from the local fixture file** (`fixtures/base.fixture.ts`), never directly
  from `@playwright/test`.
- **Page objects expose only locators and actions — zero assertions.** Assertions live in specs.
- **No `page.waitForTimeout()`, no `networkidle`.** Wait on the actual mutating request's response
  (`page.waitForResponse(...)`) or a specific UI-state assertion instead.
- **API-first setup, UI-first assertions**: create test data with the generated SDK, assert
  application behavior through the UI.
- **Jira traceability**: prefix ticket-driven test names `APIM-XXXX: <description>` and call
  `linkJira(test.info(), 'APIM-XXXX')` from `utils/jira.ts` as the first line of the test body,
  defaulting to the migration epic `APIM-14926` when a test isn't tied to a more specific ticket.
- **Clean up what you create** — every fixture that creates data also tears it down, via
  `@gravitee/utils/management`.

## Known gotchas

- **A network failure surfaces as `TypeError: Cannot read properties of undefined (reading 'status')`.**
  The generated SDK swallows fetch errors when no `onError` middleware is registered and returns
  `undefined` instead of rethrowing. If you see that TypeError, the stack is almost certainly
  unreachable — check `MANAGEMENT_BASE_URL` and that the stack is up, rather than debugging the call.
- **Don't authenticate API calls by sharing browser cookies.** The management API enforces CSRF on
  cookie-based sessions, so an `APIRequestContext` that inherits cookies from a logged-in browser
  context can get an unexpected `403`. The generated SDK authenticates statelessly per request and is
  not affected — which is another reason data setup goes through the SDK, not `page.request`.
- **Never start a `goto()` path with `/`.** Playwright resolves it as `new URL(path, baseURL)`, and a
  leading slash discards the base URL's path — so `goto('/')` against `http://nginx/console/` lands on
  the Portal at the origin root instead of the Console, and the specs fail with a blank page. Pass
  relative paths (`''`, `'#!/DEFAULT/gateways'`); `BasePage.gotoHashRoute()` already does.
