---
name: Automation API signpost (restapi)
layer: local
description: Signpost only - the triggers and checklist live in the root rule and the sync guide
dirs: [gravitee-apim-rest-api]
---

# Automation API Impact

When you change a Management API v2 OpenAPI schema or a core CRD model, the mirror may need the same change. Check whether the **Automation API** must mirror the change: the triggers and the checklist live in the root `AGENTS.md` section **Automation API Sync** and `.ai/guides/automation-api-sync.md`.
