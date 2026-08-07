# Automation API sync checklist

Depth behind the "Automation API Sync" rule. Follow this when one of the rule's triggers fires — a Management API v2 change (or a lower-layer change in the v4 definition or repository) that the Automation API must mirror.

**Step 1 (mandatory): assess impact** — determine whether the change affects the Automation API.

**Step 2 (if impacted): execute the checklist.**

1. **Update the Automation API OpenAPI spec** — add or modify the schema (`gravitee-apim-rest-api/gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest/src/main/resources/open-api.yaml`).
2. **Regenerate models** — compile the module so the openapi-generator produces the updated Java DTOs:

   ```bash
   mvn -pl gravitee-apim-rest-api/gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest compile
   ```

3. **Update MapStruct mappers** — if the new field needs explicit mapping (not auto-mapped by name), update the relevant mapper in `gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest/src/main/java/io/gravitee/apim/rest/api/automation/mapper/`.
4. **Update core CRD models if needed** — the CRD models in `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/api/model/crd/` act as a bridge between Management v2 and the Automation API. If the field is new to the CRD layer, add it there first.
5. **Make sure HRID fields are copied during update** — update operations in the service layer must copy any `hrid` fields from the existing entity to the updated entity.

**Typical data flow:** Automation OpenAPI spec → generated Automation DTOs ↔ MapStruct mappers ↔ core CRD models ↔ Management v2 generated DTOs ← Management v2 OpenAPI specs.
