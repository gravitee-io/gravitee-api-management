---
name: APIM Distribution Build
layer: local
dirs:
  - gravitee-apim-distribution
  - gravitee-apim-distribution/gravitee-apim-distribution-integration-tests
description: What the distribution reactor no longer inherits, and the three flags whose absence fails silently
---

# APIM Distribution Build

Run Maven here with `-f gravitee-apim-distribution/pom.xml` — see the two-reactor rule at the repository root.

Three flags, each of which fails quietly rather than loudly:

- **`-Pengine-snapshot`** — without it you assemble the pinned *released* engine instead of the working tree. The build succeeds and the change under test is simply absent.
- **`-nsu`** — without it Maven may replace the engine you just installed with a timestamped snapshot from the remote. Same symptom, different route.
- **`-Dbundle=dev`** — activates the profile adding the Cloud initializer and MCP libraries to `lib/`. That profile belongs to the gateway container, an external dependency here, so `-P` does not reach it; only the property activation does.

## What this reactor does not inherit

Its parent is `io.gravitee:gravitee-parent`, the organisation pom — not `gravitee-apim-parent`. That parent carries build conventions only: no `dependencyManagement`, no `repositories`, no dependencies. Anything the product parent supplied must be declared here explicitly.

- **Jacoco is not in the chain.** Never add `@{argLine}` to a surefire argument line: nothing defines the property, surefire passes the token through literally, and the fork dies on `could not open '{argLine}'` before a single test runs.
- The integration suites need their `--add-opens` flags in `surefireArgLine`; they reflect into `java.base`.
- Dependency versions come from one BOM import, `gravitee-apim-bom:${apim.server.version}`.

## Versions

Bundled plugin versions live in `gravitee-apim-distribution/pom.xml` — bumping one is a one-line change that does not touch the engine's build.

Both reactors must keep the same `<revision>`/`<sha1>`/`<changelist>`. A CI step fails the build when they diverge, because a stale triplet resolves the previous version's snapshot from Nexus instead of failing.
