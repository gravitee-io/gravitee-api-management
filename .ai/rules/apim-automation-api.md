---
name: APIM Automation API Sync
layer: local
description: When a change to API models or Management v2 also needs the Automation API
---

> Root-scoped: a cross-module concern spanning definition, repository, and rest-api; the
> root file is the vehicle that reaches all of them.

# APIM Automation API Sync

The **Automation API** (`gravitee-apim-rest-api/gravitee-apim-rest-api-automation/`) is a separate API surface that partially mirrors the Management API v2 for GitOps/automation use cases. It has its **own OpenAPI spec** and **generated models**, mapped to/from Management v2 models via MapStruct.

**When any of these changes happen, check whether the Automation API needs the same update:**

- Adding/modifying fields on v4 API definition models (`gravitee-apim-definition`)
- Adding/modifying database entity fields surfaced through Management API v2 (`gravitee-apim-repository`)
- Adding/changing enum values or schema properties in Management API v2 OpenAPI specs
- Adding/changing fields in core CRD models (`gravitee-apim-rest-api-service/.../api/model/crd/`)

**What to update** — see the *Automation API sync checklist* section in `gravitee-apim-rest-api/AGENTS.md` (path from the repo root) for the exact files and steps.
