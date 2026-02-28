# 006: JAX-RS Endpoint + DTOs

> **Common docs**: [agent-backend.md](./agent-backend.md) · [architecture-overview.md](./architecture-overview.md)

## Objective

Create the `ZeeResource` JAX-RS endpoint at `/v2/ai/generate` with request/response DTOs, auth integration, and rate limiting.

## Files

```
io.gravitee.rest.api.management.v2.rest.resource.ai/
├── ZeeResource.java                     ← JAX-RS resource
├── ZeeRequestDto.java                   ← Request DTO
└── ZeeResultDto.java                    ← Response DTO
```

## Implementation Detail

### Endpoint Registration

The v2 management API resources are registered in a JAX-RS Application class. Find the existing pattern:

```bash
grep -r "v2.*resource" gravitee-apim-rest-api/gravitee-apim-rest-api-management-v2/src/main/java/ --include="*.java" -l | head -10
```

Look for how existing resources are registered (e.g., `ApisResource`, `ApiResource`) and follow the same pattern for `ZeeResource`.

### Request DTO

```java
public class ZeeRequestDto {
    private ZeeResourceType resourceType;
    private String prompt;
    private Map<String, Object> contextData;

    public ZeeRequest toDomain(List<FormDataBodyPart> files) {
        var fileContents = files == null ? List.of() : files.stream()
            .map(f -> new FileContent(
                f.getContentDisposition().getFileName(),
                f.getValueAs(String.class),
                f.getMediaType().toString()))
            .toList();
        return new ZeeRequest(resourceType, prompt, fileContents, contextData);
    }
}
```

### Response DTO

```java
public class ZeeResultDto {
    private String resourceType;
    private Object generated;
    private ZeeMetadataDto metadata;

    public static ZeeResultDto from(ZeeResult result) {
        return new ZeeResultDto(
            result.resourceType().name(),
            result.generated(),
            ZeeMetadataDto.from(result.metadata()));
    }
}
```

### Auth & Rate Limiting

- **Auth**: Inherit from `AbstractResource` — existing Console JWT auth filters apply automatically
- **Rate Limiting**: For v0.1, a simple in-memory counter per org/env scoped by `GraviteeContext`. Can use a `@RateLimited` annotation if one exists, or a simple `AtomicInteger` + time window.

### Multipart Handling

The JAX-RS multipart handling uses Jersey's `@FormDataParam`:

```java
@POST
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public Response generate(
    @FormDataParam("request") ZeeRequestDto requestDto,
    @FormDataParam("files") List<FormDataBodyPart> files
) { ... }
```

Check how existing file upload endpoints work in the codebase:

```bash
grep -r "FormDataParam\|MULTIPART" gravitee-apim-rest-api/ --include="*.java" -l | head -5
```

## Acceptance Criteria

- [ ] Endpoint is accessible at `POST /v2/ai/generate`
- [ ] Auth via existing Console JWT works
- [ ] Multipart request with JSON request + file parts works
- [ ] Response returns generated resource + metadata
- [ ] Basic rate limiting is in place
- [ ] `curl` test succeeds end-to-end

## Commit Message

```
feat(zee): add /v2/ai/generate REST endpoint

Add JAX-RS ZeeResource at /v2/ai/generate with multipart request
handling, Console JWT auth, and basic rate limiting. Includes
ZeeRequestDto and ZeeResultDto for API contract.
```
