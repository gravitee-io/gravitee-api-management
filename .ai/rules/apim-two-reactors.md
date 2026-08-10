---
name: APIM Two Maven Reactors
layer: local
description: The repository holds two independent Maven reactors - what that changes about every mvn command you run
---

# APIM Two Maven Reactors

This repository holds **two independent reactors**:

- the **product** reactor, at the root — the engine and the libraries;
- the **distribution** reactor, under `gravitee-apim-distribution/` — the assembly that produces the Docker images, the bundle and the RPM. It is *not* a module of the root pom.

Consequences for any Maven command:

- A root `mvn clean install` builds the engine and stops there. It does **not** refresh `target/distribution`, the folder run configurations use as `GRAVITEE_HOME`.
- `mvn -pl gravitee-apim-distribution/…` from the root fails with *Could not find the selected project in the reactor*. Use `-f gravitee-apim-distribution/pom.xml`; inside that reactor, `-pl` paths are relative to it.
- Building the distribution takes two phases: install the engine first, then assemble against it. `task build-quick` does both.

The distribution assembles a **pinned released** engine unless `-Pengine-snapshot` is passed. Leaving the profile out does not fail — it produces a distribution without the change under test. `task which-engine` prints which engine actually got bundled.
