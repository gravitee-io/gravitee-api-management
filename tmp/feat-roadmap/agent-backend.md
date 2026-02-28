# Zee Agent — Backend Design

## Overview

The Zee backend is a single REST endpoint (`POST /v2/ai/generate`) inside the APIM REST API that orchestrates a one-shot LLM structured output call. No memory, no multi-turn conversation, no agent framework — just a direct `prompt → schema-constrained generation → rehydration → response` pipeline.

## Onion Architecture Breakdown

### Core Layer (Domain)

```java
// io.gravitee.apim.core.zee.model.ZeeResourceType
public enum ZeeResourceType {
    FLOW, PLAN, API, ENDPOINT, ENTRYPOINT, STEP,
    ENDPOINT_GROUP, HTTP_LISTENER, SUBSCRIPTION_LISTENER;

    // Map to SDK component class name
    public String componentClassName() { ... }
}

// io.gravitee.apim.core.zee.model.ZeeRequest
public record ZeeRequest(
    ZeeResourceType resourceType,
    String prompt,
    List<FileContent> files,
    Map<String, Object> contextData  // apiId, envId, orgId, etc.
) {}

// io.gravitee.apim.core.zee.model.ZeeResult
public record ZeeResult(
    ZeeResourceType resourceType,
    JsonNode generated,         // Rehydrated Gravitee shape
    ZeeMetadata metadata
) {}

public record ZeeMetadata(
    String model,
    int tokensUsed,
    boolean ragContextUsed
) {}

public record FileContent(String filename, String content, String mediaType) {}
```

### Core Layer (Domain Services — Interfaces Only)

```java
// io.gravitee.apim.core.zee.domain_service.LlmEngineService
public interface LlmEngineService {
    RoundtripResult generate(String prompt, String componentName);
}

// io.gravitee.apim.core.zee.domain_service.RagContextStrategy
public interface RagContextStrategy {
    ZeeResourceType resourceType();
    String retrieveContext(String envId, String orgId, Map<String, Object> contextData);
}

// io.gravitee.apim.core.zee.domain_service.ComponentRegistry
public interface ComponentRegistry {
    /** Resolve SDK component name from ZeeResourceType */
    String resolveComponentName(ZeeResourceType type);
}
```

### Core Layer (Use Case)

```java
// io.gravitee.apim.core.zee.usecase.GenerateResourceUseCase
@UseCase
public class GenerateResourceUseCase {
    private final LlmEngineService llmEngineService;
    private final Map<ZeeResourceType, RagContextStrategy> ragStrategies;
    private final ComponentRegistry componentRegistry;

    public ZeeResult execute(ZeeRequest request, String envId, String orgId) {
        // 1. Resolve component
        var componentName = componentRegistry.resolveComponentName(request.resourceType());

        // 2. Gather RAG context (tenant-scoped)
        var ragStrategy = ragStrategies.getOrDefault(
            request.resourceType(), new NoOpRagStrategy());
        var ragContext = ragStrategy.retrieveContext(envId, orgId, request.contextData());

        // 3. Build enriched prompt
        var enrichedPrompt = buildPrompt(request.prompt(), ragContext, request.files());

        // 4. Generate via LLM
        var result = llmEngineService.generate(enrichedPrompt, componentName);

        // 5. Build response
        return new ZeeResult(
            request.resourceType(),
            result.data(),
            new ZeeMetadata("gpt-5-nano", result.tokensUsed(), !ragContext.isEmpty())
        );
    }

    private String buildPrompt(String userPrompt, String ragContext, List<FileContent> files) {
        var sb = new StringBuilder();
        sb.append("You are Zee, an expert Gravitee APIM assistant.\n\n");

        if (!ragContext.isEmpty()) {
            sb.append("## Existing Configuration Context\n").append(ragContext).append("\n\n");
        }

        if (files != null && !files.isEmpty()) {
            sb.append("## Uploaded Files\n");
            for (var file : files) {
                sb.append("### ").append(file.filename()).append("\n");
                sb.append(file.content()).append("\n\n");
            }
        }

        sb.append("## User Request\n").append(userPrompt);
        return sb.toString();
    }
}
```

### Infrastructure Layer

```java
// io.gravitee.apim.infra.zee.LlmEngineServiceImpl
@Component
public class LlmEngineServiceImpl implements LlmEngineService {
    private final LlmRoundtripEngine engine;

    public LlmEngineServiceImpl(ZeeConfiguration config) {
        var providerConfig = ProviderConfig.builder()
            .url(config.getAzureUrl())
            .header("api-key", config.getAzureApiKey())
            .build();
        this.engine = LlmRoundtripEngine.create(
            new OpenAIFormatter(), providerConfig, new HttpTransport());
    }

    @Override
    public RoundtripResult generate(String prompt, String componentName) {
        // Use reflection or a switch to resolve the component
        return ComponentInvoker.generate(componentName, prompt, engine);
    }
}
```

### API Layer

```java
// io.gravitee.rest.api.management.v2.rest.resource.ai.ZeeResource
@Path("/v2/ai/generate")
@Tag(name = "AI - Zee")
public class ZeeResource extends AbstractResource {

    @Inject
    private GenerateResourceUseCase generateResourceUseCase;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(
        @FormDataParam("request") ZeeRequestDto requestDto,
        @FormDataParam("files") List<FormDataBodyPart> files
    ) {
        // Auth is handled by existing JAX-RS filters (Console JWT)
        var envId = GraviteeContext.getEnvironmentId();
        var orgId = GraviteeContext.getOrganizationId();

        var request = requestDto.toDomain(files);
        var result = generateResourceUseCase.execute(request, envId, orgId);

        return Response.ok(ZeeResultDto.from(result)).build();
    }
}
```

## Prompt Strategy

The system prompt is structured in sections:

1. **System role**: "You are Zee, an expert Gravitee APIM assistant..."
2. **RAG context** (if available): existing flows, policies, API structure
3. **File contents** (if uploaded): parsed text from user's files
4. **User prompt**: the natural language request

The schema constraint (structured output) is handled separately via the `response_format` parameter — it's NOT in the prompt. This is the key advantage: the prompt can be purely semantic, while the schema guarantees structural validity.

## Configuration

```yaml
ai:
    zee:
        enabled: true
        azure:
            url: ${OPENAI_API_URL}
            apiKey: ${OPENAI_API_KEY}
        rateLimiting:
            maxRequestsPerMinute: 10
```
