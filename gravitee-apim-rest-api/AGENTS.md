# Coding Guidelines for gravitee-apim-rest-api

## API-first: OpenAPI before JAX-RS

Use this when you add or change **HTTP endpoints** (paths, operations, request/response bodies) in any of these Maven modules (paths in the table are relative to `gravitee-apim-rest-api/`):

| Area | Maven module (path suffix) | OpenAPI spec(s) |
| --- | --- | --- |
| **Management API v2** | `gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest` | `src/main/resources/openapi/openapi-*.yaml` — several specs by area (e.g. `openapi-apis.yaml`, `openapi-api-products.yaml`, `openapi-environments.yaml`) |
| **Kafka Explorer** | `gravitee-apim-rest-api-kafka-explorer` | `src/main/resources/openapi/openapi-kafka-explorer.yaml` |
| **Portal API** | `gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest` | `src/main/resources/portal-openapi.yaml` |
| **Automation API** | `gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest` | `src/main/resources/open-api.yaml`|

**What happens**

1. **Edit OpenAPI** — paths, operations, `components.schemas`, etc. For **v2**, change the YAML that already holds similar routes/schemas (e.g. product APIs → `openapi-api-products.yaml`). **Portal** and **Kafka Explorer** each have a single spec file (see table).
2. **Compile the module** — e.g. `mvn -pl gravitee-apim-rest-api/<module-path> compile`. The `openapi-generator-maven-plugin` runs with `generateModels=true` and `generateApis=false`, so **Java model/DTO classes** are generated into `target/` (not hand-written in `src/main/java`).
3. **Implement JAX-RS** — resource classes and mappers import those generated types (e.g. `io.gravitee.rest.api.management.v2.rest.model.*`, `io.gravitee.rest.api.kafkaexplorer.rest.model.*`, `io.gravitee.rest.api.portal.rest.model.*`; v2 also uses subpackages such as `...model.analytics.engine` / `...model.logs.engine` where the POM overrides `modelPackage`).

**Rule for new endpoints:** always **API-first** — update the right OpenAPI file, run Maven on that module so generation runs, then add or change resources. Starting from the resource skips step 2, so the model classes do not exist yet and tooling breaks.

## Automation API sync

The **Automation API** partially mirrors Management API v2 for GitOps/automation use cases. When a field, enum value, or schema is added or changed in the Management API v2 (or in lower layers like the v4 definition or repository), the Automation API must reflect that change.

**Step1 (mandatory) Assess impact: determine if the change affects the Automation API**
**Step2 (if Automation API is impacted) execute the implementation checklist below: 
1. **Update the Automation API OpenAPI spec** — add/modify the schema.
2. **Regenerate models** — compile the module so the openapi-generator produces the updated Java DTOs:
   ```bash
   mvn -pl gravitee-apim-rest-api/gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest compile
   ```
3. **Update MapStruct mappers** — if the new field needs explicit mapping (not auto-mapped by name), update the relevant mapper in:
   `gravitee-apim-rest-api-automation/gravitee-apim-rest-api-automation-rest/src/main/java/io/gravitee/apim/rest/api/automation/mapper/`
4. **Update core CRD models if needed** — the CRD models in `gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/api/model/crd/` act as a bridge between Management v2 and Automation API. If the field is new to the CRD layer, add it there first.
5. **Make sure that HRID fields or copied during update** - Update operations in the service layer must copy any hrid fields from the existing entity to updated entity.

**Typical data flow:** Automation OpenAPI spec → generated Automation DTOs ↔ MapStruct mappers ↔ core CRD models ↔ Management v2 generated DTOs ← Management v2 OpenAPI specs.