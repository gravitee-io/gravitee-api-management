# 005: GenerateResource Use Case

> **Common docs**: [agent-backend.md](./agent-backend.md) · [architecture-overview.md](./architecture-overview.md)

## Objective

Create the `GenerateResourceUseCase` that orchestrates the full Zee generation pipeline: resolve component → gather RAG context → build prompt → call LLM → return result.

## File

```
io.gravitee.apim.core.zee.usecase/
└── GenerateResourceUseCase.java
```

## Implementation

This is the orchestrator — it wires together the domain services created in steps 003 and 004.

```java
@UseCase
public class GenerateResourceUseCase {

    private final LlmEngineService llmEngineService;
    private final Map<ZeeResourceType, RagContextStrategy> ragStrategies;

    public GenerateResourceUseCase(
        LlmEngineService llmEngineService,
        List<RagContextStrategy> strategies
    ) {
        this.llmEngineService = llmEngineService;
        this.ragStrategies = strategies.stream()
            .collect(Collectors.toMap(RagContextStrategy::resourceType, Function.identity()));
    }

    public ZeeResult execute(ZeeRequest request, String envId, String orgId) {
        // 1. Get component name from enum
        var componentName = request.resourceType().componentName();

        // 2. Gather RAG context (tenant-scoped)
        var ragStrategy = ragStrategies.getOrDefault(
            request.resourceType(), NoOpRagStrategy.INSTANCE);
        var ragContext = ragStrategy.retrieveContext(envId, orgId, request.contextData());

        // 3. Build enriched prompt
        var prompt = buildPrompt(request, ragContext);

        // 4. Call LLM engine
        var llmResult = llmEngineService.generate(prompt, componentName);

        // 5. Build and return domain result
        return new ZeeResult(
            request.resourceType(),
            llmResult.data(),
            new ZeeMetadata("gpt-5-nano", llmResult.tokensUsed(), !ragContext.isEmpty())
        );
    }
}
```

### Prompt Building

The prompt is assembled in sections — system context, RAG, files, user request:

```
You are Zee, an expert Gravitee APIM assistant. Generate Gravitee API
definition resources based on the user's request. Be precise and follow
Gravitee conventions.

## Existing Configuration Context
[RAG output — existing flows, available policies, etc.]

## Uploaded Files
### filename.json
[file contents]

## User Request
[user's natural language prompt]
```

### Error Handling

- If RAG strategy throws → log warning, continue without RAG context
- If LLM call fails → propagate exception (JAX-RS layer handles error response)
- If rehydration produces warnings → include in metadata, don't fail

## Acceptance Criteria

- [ ] Use case compiles in core layer with no infra dependencies
- [ ] Correctly wires RAG strategies via constructor injection
- [ ] Builds enriched prompt with RAG + files + user prompt
- [ ] Falls back to NoOpRag gracefully
- [ ] Unit test with mocked services verifies the full pipeline

## Commit Message

```
feat(zee): add GenerateResourceUseCase orchestrator

Add use case that orchestrates the Zee generation pipeline:
component resolution, RAG context gathering, prompt enrichment,
and LLM structured output generation.
```
