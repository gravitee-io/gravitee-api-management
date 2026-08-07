# Coding Guidelines for gravitee-apim-definition

## Automation API impact

When adding or modifying v4 API definition models, check whether the **Automation API** needs the same change. Fields that are surfaced through Management API v2 are likely also needed in the Automation API. See the *Automation API sync checklist* in [`gravitee-apim-rest-api/AGENTS.md`](../gravitee-apim-rest-api/AGENTS.md).
