---
name: APIM Web UI Shared Code
layer: local
dirs:
  - gravitee-apim-console-webui
  - gravitee-apim-portal-webui-next
  - gravitee-apim-portal-webui
description: Where frontend code needed by more than one APIM web app lives - one copy in a library, never one per app
---

# Shared Frontend Code

Observed agent mistake: a helper needed by the Console, the next-gen Portal and the legacy Portal was written three times, once per app.

- Code needed by both the Console and the next-gen Portal (a pure helper, a pipe, a component) lives once, in a library under `gravitee-apim-webui-libs/`, imported through its `@gravitee/<library>` path alias. Use an existing library when the code belongs to its domain (`gravitee-markdown` for documentation rendering, `gravitee-dashboard` for dashboards); otherwise propose a new library in the PR description rather than copying.
- The legacy Portal (`gravitee-apim-portal-webui`) does not import `gravitee-apim-webui-libs`. When it needs the same logic, it keeps one copy, whose header comment names the library file it mirrors, and whose tests mirror that library's tests.
- Before adding a file under an app's `util/` or `helpers/`, search the other two apps and `gravitee-apim-webui-libs/` for the same logic. Checkable: the PR adds no exported function whose body already exists in another app.
