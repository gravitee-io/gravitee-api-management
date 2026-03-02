# Work Request: Step 006 — JAX-RS Endpoint & DTOs

## Context

Zee Mode steps 001-005 are complete on `feat/zee-mode`. We have the full backend pipeline (domain models, RAG, LLM engine, and `GenerateResourceUseCase`).
Currently **56/56 unit tests** are passing.

Now it's time to build the API layer: the `POST /v2/ai/generate` REST endpoint that the Console UI will call.

## ⚠️ Critical: Build Command

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn compile -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service,gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest \
  -DskipTests -Dskip.validation -am -T 2C
```

_Note: We are adding the `management-v2-rest` module to the build command now since that's where this step lives._

## Your Task

Read:

- **Task file**: `tmp/feat-roadmap/006-jaxrs-endpoint-dtos.md`
- **Agent backend design**: `tmp/feat-roadmap/agent-backend.md`

## What to Create

All files go in the `gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest` module.

### 1. DTOs

Create these in `io.gravitee.rest.api.management.v2.rest.model.ai` (or similar package if existing):

**`ZeeRequestDto.java`**
Must handle: `resourceType` (enum or string), `prompt` (string), `contextData` (Map).
Add a method specifically to convert this into the domain `ZeeRequest` model, combining it with the multipart file uploads:

```java
public ZeeRequest toDomain(List<FormDataBodyPart> files) { ... }
```

**`ZeeResultDto.java`**
Must map the domain `ZeeResult` back to the API shape. Include `resourceType`, `generated` (the JSON node object), and `metadata`. Use a static `from(ZeeResult)` factory method.

**`ZeeMetadataDto.java`**
Maps the domain `ZeeMetadata`.

_Note: APIM V2 API heavily uses MapStruct for DTO mapping. If there's an existing MapStruct mapper interface for V2 models, you can create a `ZeeMapper`, but manual mapping is also fine here since the models are simple._

### 2. JAX-RS Resource

Create `ZeeResource.java` in `io.gravitee.rest.api.management.v2.rest.resource.ai`.

```java
@Path("/environments/{envId}/ai/generate")
@Tag(name = "AI - Zee")
public class ZeeResource extends AbstractResource {

    @Inject
    private GenerateResourceUseCase generateResourceUseCase;

    // TODO: Add endpoint
}
```

_Wait, the roadmap said `/v2/ai/generate` but V2 APIM endpoints are typically scoped by environment. Check existing V2 endpoints before deciding the `@Path`:_

```bash
grep -r "@Path" gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/gravitee-apim-rest-api-management-v2-rest/src/main/java/ --include="*Resource.java" | head -10
```

Use whatever path pattern matches the existing V2 management API (likely `/environments/{envId}/ai/generate`).

**Endpoint method:**

```java
@POST
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public Response generate(
    @PathParam("envId") String envId,
    @FormDataParam("request") ZeeRequestDto requestDto,
    @FormDataParam("files") List<FormDataBodyPart> files
) {
    var orgId = GraviteeContext.getOrganizationId();
    var request = requestDto.toDomain(files);

    // Apply basic rate limiting here (e.g. 10 req/min per env using an AtomicInteger or Guava Cache)

    var result = generateResourceUseCase.execute(request, envId, orgId);
    return Response.ok(ZeeResultDto.from(result)).build();
}
```

### 3. Resource Registration

V2 resources are registered in an Application or configuration class. Find it and add `ZeeResource`:

```bash
grep -r "register(.*Resource.class)" gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/ --include="*.java"
```

## Acceptance Criteria

- [ ] JAX-RS endpoint, request DTO, and result DTO implemented
- [ ] Multipart request handling implemented (`FormDataBodyPart`)
- [ ] Basic rate limiting implemented (in-memory is fine for v0.1)
- [ ] Registered in the V2 JAX-RS configuration
- [ ] Existing JWT auth protects the endpoint automatically (via `AbstractResource`)
- [ ] **Tests**: Create `ZeeResourceTest` mirroring existing resource tests (mock the UseCase, verify status codes and JSON mapping)
- [ ] Build passes with modified command — **BUILD SUCCESS**

## Hard Constraints

- **API Layer ONLY**: Do not modify files in the core or infra layers created in steps 001-005.
- **V2 API Conventions**: Follow the existing V2 REST API patterns (e.g., path variables, response building, DTO naming).
- **Branch**: `feat/zee-mode`

## After Implementation

1. **Commit**: `feat(zee): add /v2/ai/generate REST endpoint`
2. **Request external review** — apply feedback
3. **Output work summary**:

```markdown
## Work Summary: Step 006 — JAX-RS Endpoint & DTOs

### What Was Done

- [files created with full paths]
- [how the resource was registered in JAX-RS]

### Architecture Compliance

- [confirm: ZeeResource is in the API layer, calling only the UseCase (no infra/DB direct calls)]
- [confirm: DTOs do not leak into the core layer]

### Decisions Made

- [exact @Path chosen and why]
- [how rate limiting was implemented]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [surprises or things affecting future steps]

### Test Results

- [total count — must be ≥ 56 + new tests, all passing]
- [build command output: BUILD SUCCESS]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
