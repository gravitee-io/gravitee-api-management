# APIM Structural Debt Visibility

Living register of **structural / coexistence debt** in Gravitee APIM, shown
**alongside imported SonarCloud summaries** in one place so people can ideate,
prioritize, and pick work without switching tools.

## What this is

- Measurable migration KPIs Sonar does not track (LegacyWrappers, AngularJS
  hybrid files, mapi-v1 vs mapi-v2 e2e skew, dual portal UIs).
- A curated [debt-register.yaml](./debt-register.yaml) with “why it hurts” and
  pick hints.
- Weekly snapshots under [metrics/](./metrics/) and a generated
  [TECH-DEBT-REPORT.md](./TECH-DEBT-REPORT.md).
- A visual dashboard at [site/index.html](./site/index.html) (charts, snapshot
  dropdown, Without Sonar / Sonar only / With Sonar filter).

## What this is not

- Not a second static analyzer (we **import** Sonar; we do not re-run it).
- Not a remediation program — this surfaces data; teams pick the work.
- Not a replacement for [SonarCloud](https://sonarcloud.io/organizations/gravitee-io/projects)
  deep-dives (bugs, hotspots, PR decoration stay there).

## Refresh locally

Requires Node 20+ and [ripgrep](https://github.com/BurntSushi/ripgrep) (`rg`)
on `PATH` (optional fallback walker is used if `rg` is missing).

```bash
# From repository root
node tech-debt/collect-metrics.mjs
# Optional — needs SONAR_TOKEN with SonarCloud browse permission
SONAR_TOKEN=*** node tech-debt/fetch-sonar-summary.mjs
node tech-debt/generate-report.mjs
node tech-debt/generate-site.mjs
```

Or all at once:

```bash
node tech-debt/collect-metrics.mjs \
  && node tech-debt/fetch-sonar-summary.mjs \
  && node tech-debt/generate-report.mjs \
  && node tech-debt/generate-site.mjs
```

Then open `tech-debt/site/index.html` in a browser.

`fetch-sonar-summary.mjs` is best-effort: if the token is missing or the API
fails, structural KPIs are still published and the report marks Sonar as
unavailable.

## CI

[`.github/workflows/structural-debt-metrics.yml`](../.github/workflows/structural-debt-metrics.yml)
runs Mondays (and on `workflow_dispatch`), refreshes metrics + report, and
commits when something changed. Configure repository secret `SONAR_TOKEN` for
Sonar import.

## How to pick work

1. Open [TECH-DEBT-REPORT.md](./TECH-DEBT-REPORT.md).
2. Choose a register item whose pick hint matches your team.
3. Open an issue/PR and reference the register `id` (e.g. `hexagonal-legacy-wrappers`).
