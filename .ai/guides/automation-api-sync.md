# Automation API sync checklist

Depth behind the "Automation API Sync" rule. Follow this when one of the rule's triggers fires — a Management API v2 change (or a lower-layer change in the v4 definition or repository) that the Automation API must mirror.

**Step 1 (mandatory): assess impact** — determine whether the change affects the Automation API.

**Step 2 (if impacted): execute the checklist.**

1. **Update the Automation API OpenAPI spec and regenerate its models** — the spec path and the compile step (generation runs on `compile`) are in the **Management API: API-first** table in the root `AGENTS.md`.
2. **Update MapStruct mappers** — if the new field needs explicit mapping (not auto-mapped by name), update the relevant mapper in `gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest/src/main/java/io/gravitee/apim/rest/api/automation/mapper/`.
3. **Update core CRD models if needed** — the CRD models in `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/api/model/crd/` act as a bridge between Management v2 and the Automation API. If the field is new to the CRD layer, add it there first.
4. **Update the hand-written Management v2 CRD classes** — `ApiCRDSpec`, `PlanCRD`, `PageCRD` and `ApplicationCRDSettings` in `gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/io/gravitee/rest/api/management/v2/rest/model/` are not generated. MapStruct ignores an unmapped *source* property without a warning, so a field present in the Automation spec but missing here is silently dropped on write.
5. **Make sure HRID fields are copied during update** — update operations in the service layer must copy any `hrid` fields from the existing entity to the updated entity.
6. **Prove the round trip** — for every new field, assert it on the write path (capture the use-case input in the resource test for `PUT`) and on the read path (the resource test for `GET`). An Automation `PUT` replaces the whole definition, so a field lost on either path is reset on every apply.

**Typical data flow:** Automation OpenAPI spec → generated Automation DTOs ↔ Automation MapStruct mappers ↔ Management v2 CRD classes (nesting Management v2 generated DTOs, from the Management v2 OpenAPI specs) ↔ Management v2 mappers ↔ core CRD models.
