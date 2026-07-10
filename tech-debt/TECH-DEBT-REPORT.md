# APIM Structural Debt Report

Generated from snapshot **2026-07-10** (commit `ac3ba5be10`, version `4.13.0`).
First snapshot — no previous comparison.

## What this is

A living register of **structural / coexistence debt** in Gravitee APIM,
shown **next to imported SonarCloud summaries** so you can ideate and pick
work in one place. This does **not** replace SonarCloud analysis or remediate debt.

See [README.md](./README.md) for how to refresh and how to contribute.

> **Visual dashboard:** open [`site/index.html`](./site/index.html) in a browser
> (snapshot dropdown, Sonar filter, charts). This Markdown file is the git-friendly twin.

## Structural debt — at a glance

| KPI | id | Current | Δ vs previous | Trend |
|-----|----|---------|---------------|-------|
| LegacyWrapper files | `legacy_wrappers` | 30 | — | — |
| AngularJS *.ajs.ts files | `angularjs_files` | 40 | — | — |
| e2e specs on mapi-v1 | `e2e_mapi_v1` | 95 | — | — |
| e2e specs on mapi-v2 | `e2e_mapi_v2` | 33 | — | — |
| Dual portal UIs present | `dual_portal` | yes | — | — |

## SonarCloud summary (imported)

**Unavailable** — SONAR_TOKEN not set

Set `SONAR_TOKEN` and run `node tech-debt/fetch-sonar-summary.mjs` (or wait for the weekly workflow).
Deep dives remain on [SonarCloud / gravitee-io](https://sonarcloud.io/organizations/gravitee-io/projects).

## Debt register

### `hexagonal-legacy-wrappers` — Hexagonal migration (LegacyWrappers)

- **KPI:** `legacy_wrappers` = **30**
- **Status:** improving
- **Why it hurts:** LegacyWrapper classes bridge clean-architecture ports to pre-migration services. Each remaining wrapper means dual paths and slower change.
- **Pick hint:** Pick one bounded context in rest-api-service, replace its LegacyWrapper with a real adapter, and delete the wrapper when callers are migrated.

### `console-angularjs-hybrid` — Console AngularJS hybrid pages

- **KPI:** `angularjs_files` = **40**
- **Status:** stalled
- **Why it hurts:** Console still ships *.ajs.ts AngularJS pages alongside Angular. Hybrid UI raises maintenance cost and blocks full Angular modernization.
- **Pick hint:** Good console-team epic: convert one .ajs.ts page to Angular and remove the hybrid file in the same PR.

### `management-api-v1-e2e` — Management API v1 e2e coverage

- **KPI:** `e2e_mapi_v1` = **95**
- **Status:** improving
- **Why it hurts:** E2E still leans on mapi-v1 while product direction is Management API v2. Dual API test surface slows retirement of v1.
- **Pick hint:** Port one mapi-v1 e2e suite to mapi-v2 and delete or skip the v1 twin when parity is proven.

### `management-api-v2-e2e` — Management API v2 e2e coverage

- **KPI:** `e2e_mapi_v2` = **33**
- **Status:** improving
- **Why it hurts:** Higher mapi-v2 e2e count is the counterpart to retiring v1 tests. Track this rising while e2e_mapi_v1 falls.
- **Pick hint:** Add mapi-v2 coverage for gaps still only tested on v1 (see e2e_mapi_v1).

### `dual-portal-uis` — Dual Developer Portal UIs

- **KPI:** `dual_portal` = **yes**
- **Status:** stalled
- **Why it hurts:** portal-webui and portal-webui-next both ship. Dual UIs mean duplicate features, themes, and bugfixes until classic portal is retired.
- **Pick hint:** Needs portal product owner: define parity checklist for portal-next, then schedule classic portal removal once checklist is green.

## Trend (last snapshots)

| Date | LegacyWrappers | AngularJS | mapi-v1 | mapi-v2 | Dual portal |
|------|----------------|-----------|---------|---------|-------------|
| 2026-07-10 | 30 | 40 | 95 | 33 | yes |

## How to contribute

1. Pick a register `id` whose pick hint matches your team.
2. Open an issue or PR referencing that id (example: `hexagonal-legacy-wrappers`).
3. After merge, the next weekly refresh (or a local run) will move the KPI.
