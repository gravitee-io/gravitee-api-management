---
name: Automation API signpost (definition)
layer: local
description: Signpost only - the owned surface and the hand-off live in the root ownership rule
dirs: [gravitee-apim-definition]
---

# Automation API Impact

A v4 API definition model change may reach the Automation API and the CRD classes through Management API v2. Do not mirror it there: flag it as the root `AGENTS.md` section **Automation API, CRD and Terraform Ownership** says.
