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

- **`-Dapim.core.version=<root triplet>`** — without it you assemble the pinned *released* core instead of the working tree. The build succeeds and the change under test is simply absent. `task build-distribution` computes the value for you.
- **`-nsu`** — without it Maven may replace the engine you just installed with a timestamped snapshot from the remote. Same symptom, different route.
- **`-Dbundle=dev`** — activates the profile adding the Cloud initializer and MCP libraries to `lib/`. That profile belongs to the gateway container, an external dependency here, so `-P` does not reach it; only the property activation does.

## What this reactor does not inherit

Its parent is `io.gravitee:gravitee-parent`, the organisation pom — not `gravitee-apim-parent`. That parent carries build conventions only: no `dependencyManagement`, no `repositories`, no dependencies. Anything the product parent supplied must be declared here explicitly.

- **Jacoco is not in the chain.** Never add `@{argLine}` to a surefire argument line: nothing defines the property, surefire passes the token through literally, and the fork dies on `could not open '{argLine}'` before a single test runs.
- The integration suites need their `--add-opens` flags in `surefireArgLine`; they reflect into `java.base`.
- Dependency versions come from one BOM import, `gravitee-apim-bom:${apim.core.version}`.

## Versions

Bundled plugin versions live in `gravitee-apim-distribution/pom.xml` — bumping one is a one-line change that does not touch the engine's build.

The two reactors carry their own `<revision>`/`<sha1>`/`<changelist>` and no longer have to agree. Which core the distribution assembles is decided by `apim.core.version` — its pin — or by the `-Dapim.core.version` a build passes, never by its own triplet.

The assembled distribution carries a `core.version` file at its root, filtered at assembly time, so what shipped can be read back from the bundle. The gateway and rest-api images publish the same value as the OCI label `io.gravitee.core.version`.
