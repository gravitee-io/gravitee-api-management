---
name: Automation API, CRD and Terraform ownership (review)
layer: local
description: What a reviewer reports when a change touches the Automation API, CRD or Terraform surface without its owning team
review-instructions: true
dirs: [gravitee-apim-rest-api, gravitee-apim-definition, gravitee-apim-repository, gravitee-apim-console-webui/src/management/api/general-info]
---

# Automation API, CRD and Terraform Ownership

The Automation API, the CRD import/export path and the Terraform export are owned by `@gravitee-io/gko` and `@gravitee-io/tech-lead`. Owned files:

- everything under `gravitee-apim-rest-api/gravitee-apim-rest-api-automation/`;
- under `gravitee-apim-rest-api/`, every class whose name contains `CRD` and every `model/crd/` package;
- the `_import/crd`, `_export/crd`, `crd` and `import-crd` endpoint methods;
- `gravitee-apim-console-webui/src/management/api/general-info/api-general-info-export-v4-dialog/`.

Report:

- **Blocker** — the diff changes an owned file and the PR description has no **Automation API / CRD impact** section stating that the owning team requested the change.
- **Improvement** — the diff changes a v4 API definition model, a Management API v2 schema property or enum value, or a repository entity field surfaced through Management API v2, and the PR description has no **Automation API / CRD impact** section saying whether the owned surface must follow.
