---
name: Automation API, CRD and Terraform Ownership
layer: local
description: Who owns the Automation API, the CRD import/export path and the Terraform export, and the hand-off any other change must follow instead of editing them
---

# Automation API, CRD and Terraform Ownership

The Automation API, the CRD import/export path and the Terraform export are a public contract consumed by the Kubernetes operator (GKO) and the Terraform provider. An Automation `PUT` replaces the whole definition, so a field dropped on the write or the read path resets user configuration on every apply. This surface is owned by `@gravitee-io/gko` and `@gravitee-io/tech-lead`.

**Owned surface:**

| Area | Paths |
| --- | --- |
| Automation API | everything under `gravitee-apim-rest-api/gravitee-apim-rest-api-automation/` — the `open-api.yaml` spec, mappers, resources, HRID helpers, Gamma automation ports |
| CRD models and use cases | under `gravitee-apim-rest-api/`, every class whose name contains `CRD` and every `model/crd/` package — core models, `Import*`/`Validate*`/`Export*` CRD use cases and domain services, the hand-written Management API v2 `*CRD*` classes and mappers, `ApiCRDEntity` |
| CRD endpoints | `_import/crd` and `_export/crd` in Management API v2; `crd` and `import-crd` in Management API v1 |
| CRD and Terraform export UI | `gravitee-apim-console-webui/src/management/api/general-info/api-general-info-export-v4-dialog/` |

Do not create, edit, rename or delete a file on the owned surface unless the person you are working with confirms the owning team requested the change. Otherwise, before editing:

1. Stop and name the owned files the task would touch, and why.
2. Leave them unchanged; finish the rest of the task if it stands on its own.
3. Add an **Automation API / CRD impact** section to the PR description listing the owned files and the change they need, and tell the person to raise it with the owning team.

The same hand-off applies when a change outside the surface alters what the surface exposes: a v4 API definition model (`gravitee-apim-definition`), a Management API v2 schema property or enum value, or a repository entity field surfaced through Management API v2. Do not mirror it into the Automation API or the CRD classes; record it in the PR description's impact section. The owning team's mirroring procedure is `.ai/guides/automation-api-sync.md`.
