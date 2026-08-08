---
name: Module Context Files
layer: local
description: Where the per-module AGENTS.md files live, until the modules are declared in the manifest and this index is generated instead
---

# Module Context Files

This list is maintained by hand until `.ai/manifest.yaml` declares the modules. Root conventions apply everywhere; a module's own `AGENTS.md` wins for its directory and below. Directories not listed here have no module file — root conventions apply there, and rules naming a framework apply only where that framework is actually used.

**Java (Maven):** `gravitee-apim-common/`, `gravitee-apim-definition/`, `gravitee-apim-distribution/`, `gravitee-apim-distribution/gravitee-apim-distribution-integration-tests/`, `gravitee-apim-gateway/`, `gravitee-apim-plugin/`, `gravitee-apim-reporter/`, `gravitee-apim-repository/`, `gravitee-apim-rest-api/`

**Angular:** `gravitee-apim-console-webui/`, `gravitee-apim-portal-webui-next/`, `gravitee-apim-webui-libs/gravitee-dashboard/`, `gravitee-apim-webui-libs/gravitee-kafka-explorer/`, `gravitee-apim-webui-libs/gravitee-markdown/`

**Gamma (Maven + Nx):** `gravitee-gamma/gravitee-gamma-module-apim/`, `gravitee-gamma/gravitee-gamma-module-platform/`, `gravitee-gamma/gravitee-gamma-rest-api/`

Each of the 17 listed directories carries an `AGENTS.md`; read it when working there.
