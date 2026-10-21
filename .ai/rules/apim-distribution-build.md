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
- **`-Dbundle=dev`** — picks the bundle-dev flavor (see below), and activates the profile adding the Cloud initializer and MCP libraries to `lib/`. That profile belongs to the gateway container, an external dependency here, so `-P` does not reach it; only the property activation does.

## Flavors: what a distribution ships

Each component is built in two steps, both in the `package` phase of `gravitee-apim-distribution-standalone-{gateway,rest-api}`:

1. the assembly builds the **slim** distribution in `target/slim`, from the dependencies declared outside any profile;
2. the `flavor` goal of `gravitee-apim-release-manifest-maven-plugin` copies it to `target/distribution` and adds the plugins its **flavor file** names.

| Build | Flavor file | Ships |
|---|---|---|
| no `-Dbundle` | `flavors/community.txt` | open-source plugins only |
| `-Dbundle` | `flavors/bundle-default.txt` | the released zip and images |
| `-Dbundle=dev` | `flavors/bundle-dev.txt` | our dev and test images |

- **The flavors nest:** community ⊂ bundle-default ⊂ bundle-dev. A plugin listed in a flavor file is also listed in every bigger one.
- **Adding a plugin takes three edits:**
  1. its version, in a property of `gravitee-apim-distribution/pom.xml` named after its artifactId (`<gravitee-policy-foo.version>`);
  2. its declaration, with `<type>zip</type>` and `<scope>runtime</scope>`, in `gravitee-apim-distribution-standalone/pom.xml`: outside any profile when every distribution ships it, otherwise in the `catalog` profile;
  3. its artifactId in the flavor file of the smallest flavor that ships it, and in every bigger one.

  Declared in `catalog` but listed nowhere, it ships in no image: the release manifest still lists it, for custom images.
- **Both components get every plugin their assembly accepts.** The `plugins` dependencySet of each `plugin-assembly-distribution.xml` is an exclude list: a plugin meant for one component only is excluded from the other's assembly. Policies stay in both: the Management API loads them too.
- **Check where it landed.** A name no component accepts, a typo included, is only logged as skipped (`not part of the catalog of <component>`) and ships nowhere. After a build, look for the zip in `target/distribution/plugins` and in `target/release-manifest.json` of each component.
- **The `catalog` profile is never activated.** `-Pcatalog` would put every plugin into the slim distribution, and so into every flavor.
- **One file per flavor, for both components.** Each component takes the names its assembly accepts and logs the others as skipped: a typo shows up there, not as a failure.
- `target/distribution` keeps its path and its content: the Dockerfiles, `task docker-backend` and the CI read it as before.

## Running the inference plugin on a Mac

The `gravitee-inference-service` zip bundled by default only ships the Linux ONNX Runtime natives, which is what the images need. To run the plugin natively on macOS, assemble with `task build-distribution-macos`, or pass `-Dgravitee-inference-service.classifier=macos` to the distribution build: it swaps in the `macos` zip, never both.

- **Never for an image.** A distribution assembled with the macOS zip packaged into a Linux image starts, then fails when the plugin loads. `task docker` rebuilds through `build-distribution`, so it keeps the Linux zip; for a hand-made `buildx` image, assemble the distribution without the property.
- **Never in `~/.m2/settings.xml` or the environment.** The property is read by every distribution build, images included. Pass it per build.

## What this reactor does not inherit

Its parent is `io.gravitee:gravitee-parent`, the organisation pom — not `gravitee-apim-parent`. That parent carries build conventions only: no `dependencyManagement`, no `repositories`, no dependencies. Anything the product parent supplied must be declared here explicitly.

- **Jacoco is not in the chain.** Never add `@{argLine}` to a surefire argument line: nothing defines the property, surefire passes the token through literally, and the fork dies on `could not open '{argLine}'` before a single test runs.
- The integration suites need their `--add-opens` flags in `surefireArgLine`; they reflect into `java.base`.
- Dependency versions come from one BOM import, `gravitee-apim-bom:${apim.core.version}`.

## Versions

Bundled plugin versions live in `gravitee-apim-distribution/pom.xml` — bumping one is a one-line change that does not touch the engine's build.

The two reactors carry their own `<revision>`/`<sha1>`/`<changelist>` and no longer have to agree. Which core the distribution assembles is decided by `apim.core.version` — its pin — or by the `-Dapim.core.version` a build passes, never by its own triplet.

The assembled distribution carries a `core.version` file at its root, filtered at assembly time, so what shipped can be read back from the bundle. The gateway and rest-api images publish the same value as the OCI label `io.gravitee.core.version`.
