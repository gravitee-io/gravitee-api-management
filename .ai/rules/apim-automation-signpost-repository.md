---
name: Automation API signpost (repository)
layer: local
description: Signpost only - the triggers and checklist live in the root rule and the sync guide
dirs: [gravitee-apim-repository]
---

# Automation API Impact

When you add or change a database entity field surfaced through Management API v2, the mirror may need it too. Check whether the **Automation API** must mirror the change: the triggers and the checklist live in the root `AGENTS.md` section **Automation API Sync** and `.ai/guides/automation-api-sync.md`.
