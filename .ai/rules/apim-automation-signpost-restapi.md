---
name: Automation API signpost (restapi)
layer: local
description: Signpost only - the owned surface and the hand-off live in the root ownership rule
dirs: [gravitee-apim-rest-api]
---

# Automation API Impact

The `gravitee-apim-rest-api-automation` module, every class whose name contains `CRD` and every `model/crd/` package in this tree are owned by `@gravitee-io/gko` and `@gravitee-io/tech-lead`. Do not edit them, and do not mirror a Management API v2 schema change into them: follow the root `AGENTS.md` section **Automation API, CRD and Terraform Ownership**.
