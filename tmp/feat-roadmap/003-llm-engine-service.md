# 003: LLM Engine Service

> **Common docs**: [json-schema-llm-integration.md](./json-schema-llm-integration.md) · [agent-backend.md](./agent-backend.md)

## Objective

Create the `LlmEngineService` domain interface and its infrastructure implementation using `json-schema-llm`'s `HttpTransport` directly to Azure OpenAI. No LangChain4J.

## Files

```
io.gravitee.apim.core.zee.domain_service/
└── LlmEngineService.java               ← Interface (domain layer)

io.gravitee.apim.infra.zee/
└── LlmEngineServiceImpl.java           ← Implementation (infra layer)
```

## Domain Interface

```java
package io.gravitee.apim.core.zee.domain_service;

import com.fasterxml.jackson.databind.JsonNode;

public interface LlmEngineService {
    /**
     * Generate a structured resource using LLM structured output.
     * @param prompt The enriched prompt (user + RAG + files)
     * @param componentName The SDK component name (e.g., "Flow", "Plan")
     * @return The generation result with rehydrated data
     */
    LlmGenerationResult generate(String prompt, String componentName);

    record LlmGenerationResult(JsonNode data, boolean valid, int tokensUsed, List<String> warnings) {}
}
```

## Infrastructure Implementation

This is the critical piece — wiring `json-schema-llm`'s Java engine to Azure OpenAI.

### Key Points

1. **ProviderConfig**: The Azure OpenAI URL already includes deployment + api-version. Just set the URL and `api-key` header.
2. **ComponentInvoker**: Need a way to call `Component.generate(prompt, engine)` dynamically based on `componentName`. Options:
    - **Reflection**: `Class.forName("io.gravitee.apim.definition.llm.v4." + componentName)` → invoke static `.generate()` method
    - **Registry map**: A static `Map<String, BiFunction<String, Engine, RoundtripResult>>` populated at startup
    - **Recommendation**: Registry map for type safety, populated in the constructor

```java
@Component
public class LlmEngineServiceImpl implements LlmEngineService {
    private final LlmRoundtripEngine engine;
    private final Map<String, BiFunction<String, LlmRoundtripEngine, RoundtripResult>> generators;

    public LlmEngineServiceImpl(ZeeConfiguration config) {
        var providerConfig = ProviderConfig.builder()
            .url(config.getAzureUrl())
            .header("api-key", config.getAzureApiKey())
            .build();
        this.engine = LlmRoundtripEngine.create(
            new OpenAIFormatter(), providerConfig, new HttpTransport());

        // Register all supported components
        this.generators = Map.of(
            "Flow", (prompt, eng) -> Flow.generate(prompt, eng),
            "Plan", (prompt, eng) -> Plan.generate(prompt, eng),
            "Api", (prompt, eng) -> Api.generate(prompt, eng),
            // ... add more as needed
        );
    }

    @Override
    public LlmGenerationResult generate(String prompt, String componentName) {
        var generator = generators.get(componentName);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported component: " + componentName);
        }
        var result = generator.apply(prompt, engine);
        return new LlmGenerationResult(
            result.data(), result.isValid(), result.tokensUsed(), result.warnings());
    }
}
```

### System Prompt

The system prompt is injected by the `json-schema-llm` engine as part of the structured output request. You provide just the user prompt — the engine handles:

1. Setting up `response_format` with the component's schema
2. Adding the `strict: true` flag
3. Making the HTTP call
4. Rehydrating the response using the codec

## Testing

Unit test with a mock `HttpTransport` that returns a pre-built JSON response matching the Flow schema. Verify rehydration produces the expected output.

## Acceptance Criteria

- [ ] `LlmEngineService` interface in core layer (no infra deps)
- [ ] `LlmEngineServiceImpl` successfully calls Azure OpenAI with structured output
- [ ] At least `Flow` component registered in the generator map
- [ ] Unit test with mock transport verifies the pipeline
- [ ] Integration test with real Azure call (requires env vars) generates a valid Flow

## Commit Message

```
feat(zee): implement LLM engine service with HttpTransport

Add LlmEngineService domain interface and HttpTransport-based
implementation that calls Azure OpenAI with json-schema-llm
structured output pipeline. No LangChain4J dependency.
```
