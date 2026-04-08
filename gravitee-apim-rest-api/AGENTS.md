# Coding Guidelines for gravitee-apim-rest-api

## API-first: OpenAPI before JAX-RS

Use this when you add or change **HTTP endpoints** (paths, operations, request/response bodies) in any of these Maven modules (paths in the table are relative to `gravitee-apim-rest-api/`):

| Area | Maven module (path suffix) | OpenAPI spec(s) |
| --- | --- | --- |
| **Management API v2** | `gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest` | `src/main/resources/openapi/openapi-*.yaml` — several specs by area (e.g. `openapi-apis.yaml`, `openapi-api-products.yaml`, `openapi-environments.yaml`) |
| **Kafka Explorer** | `gravitee-apim-rest-api-kafka-explorer` | `src/main/resources/openapi/openapi-kafka-explorer.yaml` |
| **Portal API** | `gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest` | `src/main/resources/portal-openapi.yaml` |

**What happens**

1. **Edit OpenAPI** — paths, operations, `components.schemas`, etc. For **v2**, change the YAML that already holds similar routes/schemas (e.g. product APIs → `openapi-api-products.yaml`). **Portal** and **Kafka Explorer** each have a single spec file (see table).
2. **Compile the module** — e.g. `mvn -pl gravitee-apim-rest-api/<module-path> compile`. The `openapi-generator-maven-plugin` runs with `generateModels=true` and `generateApis=false`, so **Java model/DTO classes** are generated into `target/` (not hand-written in `src/main/java`).
3. **Implement JAX-RS** — resource classes and mappers import those generated types (e.g. `io.gravitee.rest.api.management.v2.rest.model.*`, `io.gravitee.rest.api.kafkaexplorer.rest.model.*`, `io.gravitee.rest.api.portal.rest.model.*`; v2 also uses subpackages such as `...model.analytics.engine` / `...model.logs.engine` where the POM overrides `modelPackage`).

**Rule for new endpoints:** always **API-first** — update the right OpenAPI file, run Maven on that module so generation runs, then add or change resources. Starting from the resource skips step 2, so the model classes do not exist yet and tooling breaks.