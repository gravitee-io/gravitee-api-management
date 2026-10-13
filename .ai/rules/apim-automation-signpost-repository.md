---
name: Automation API signpost (repository)
layer: local
description: Signpost only - the owned surface and the hand-off live in the root ownership rule
dirs: [gravitee-apim-repository]
---

# Automation API Impact

A database entity field surfaced through Management API v2 may reach the Automation API and the CRD classes. Do not mirror it there: flag it as the root `AGENTS.md` section **Automation API, CRD and Terraform Ownership** says.
