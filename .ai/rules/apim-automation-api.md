---
name: APIM Automation API Sync
layer: local
description: When a Management API v2 change must be mirrored into the Automation API - the triggers and where the checklist lives
---

# Automation API Sync

The **Automation API** (`gravitee-apim-rest-api/gravitee-apim-rest-api-automation/`) is a separate API surface that partially mirrors Management API v2 for GitOps and automation, with its own OpenAPI spec and generated models. Check whether it needs the same update when any of these change:

- v4 API definition models (`gravitee-apim-definition`)
- database entity fields surfaced through Management API v2 (`gravitee-apim-repository`)
- enum values or schema properties in Management API v2 OpenAPI specs
- core CRD models (`gravitee-apim-rest-api-service/.../api/model/crd/`)

When a trigger fires, follow the checklist in `.ai/guides/automation-api-sync.md`; the API-first section in this file names the spec paths and the compile step.
