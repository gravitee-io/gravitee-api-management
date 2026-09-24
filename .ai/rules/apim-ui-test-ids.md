---
name: APIM UI Test Ids
layer: local
dirs:
  - gravitee-apim-console-webui
  - gravitee-apim-portal-webui
  - gravitee-apim-portal-webui-next
  - gravitee-apim-webui-libs/gravitee-dashboard
  - gravitee-apim-webui-libs/gravitee-kafka-explorer
  - gravitee-apim-webui-libs/gravitee-markdown
description: data-testid is a contract with the platform E2E suite - provision it on data fields, never change an existing one
---

# UI Test Ids

The platform E2E framework, outside this repository, selects elements by `data-testid`, so a changed id breaks it without failing any test here.

- Every element your change adds or modifies that reads or writes data carries a `data-testid`: form controls, toggles, selects, displayed values, and the buttons that create, save, delete, or change state.
- Never rename or remove an existing `data-testid` value, even when its name is poor or no longer fits its context. Cleaning up the code around it does not extend to the id.
- When extracting markup into a shared component, the original screen keeps rendering its original ids: take the id, or its prefix, as an input and have the original caller pass the old value. New callers may use new ids.
- Remove an id only when its element leaves the product, and call the removal out in the PR description as a compatibility-sensitive change.
