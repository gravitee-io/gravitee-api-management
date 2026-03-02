# Work Request: Step 003 — LLM Engine Service

## Context

You are implementing step 003 of the **Zee Mode** feature for Gravitee APIM. This is the **most critical backend step** — wiring the `json-schema-llm` engine directly to Azure OpenAI for structured output generation.

**Completed steps:**

- Step 001: Maven deps vendored (`json-schema-llm-java`, `json-schema-llm-engine`, V4 SDK in `lib/`)
- Step 002: Domain models in `io.gravitee.apim.core.zee.model` (`ZeeResourceType`, `ZeeRequest`, `ZeeResult`, `ZeeMetadata`, `FileContent`)

You are working on the `feat/zee-mode` branch in `~/workspace/Gravitee/gravitee-api-management`.

## Your Task

Read and execute the implementation described in:

- **Task file**: `tmp/feat-roadmap/003-llm-engine-service.md`
- **SDK integration guide**: `tmp/feat-roadmap/json-schema-llm-integration.md`
- **Architecture overview**: `tmp/feat-roadmap/architecture-overview.md`
- **Agent backend design**: `tmp/feat-roadmap/agent-backend.md`

In summary: create the `LlmEngineService` domain interface (core layer) and its `LlmEngineServiceImpl` infrastructure implementation that calls Azure OpenAI using the `json-schema-llm` `HttpTransport` pipeline.

## Critical: Updated API Signature

During step 001, the GAD spec SDK was fixed to match the current `json-schema-llm-engine` API. The `generate()` method on each SDK component now takes these params:

```java
// Each of the 64 components has this pattern:
Flow.generateWithPreconverted(String prompt, ProviderFormatter formatter, ProviderConfig config, LlmTransport transport)
```

**NOT** the simpler `Flow.generate(prompt, engine)` shown in the original roadmap docs. You'll need to discover the exact API surface yourself by inspecting the vendored V4 SDK JAR or the source at `~/workspace/Gravitee/gravitee-api-definition-spec/sdks/java/v4/`.

## What to Create

### 1. Domain Interface (Core Layer)

```
io.gravitee.apim.core.zee.domain_service/
└── LlmEngineService.java
```

```java
public interface LlmEngineService {
    LlmGenerationResult generate(String prompt, String componentName);

    record LlmGenerationResult(
        JsonNode data,         // Rehydrated Gravitee shape
        boolean valid,
        int tokensUsed,
        List<String> warnings
    ) {}
}
```

This interface lives in the **core/domain** layer — no infrastructure deps.

### 2. Infrastructure Implementation (Infra Layer)

```
io.gravitee.apim.infra.zee/
└── LlmEngineServiceImpl.java
```

This is where you wire the `json-schema-llm` Java engine:

1. **Read the `ZeeConfiguration`** created in step 001 to get the Azure URL and API key
2. **Create a `ProviderConfig`** with the URL and `api-key` header
3. **Create a component registry** — a `Map<String, ...>` that maps component names (e.g., `"Flow"`) to their `generateWithPreconverted` methods
4. **On `generate()` call**: look up the component by name, call `generateWithPreconverted(prompt, formatter, config, transport)`, return the result

### Key Points

- **ProviderConfig**: The Azure URL (`OPENAI_API_URL`) is already fully qualified with deployment + api-version. Just set it as the URL with an `api-key` header. Explore the `ProviderConfig` class in the vendored JAR to see its builder API.
- **ProviderFormatter**: Likely `OpenAIFormatter` or similar — check what's available in the `json-schema-llm-engine` JAR.
- **LlmTransport**: Likely `HttpTransport` — check the engine JAR for available implementations.
- **Component invocation**: Use a static registry map populated in the constructor. For each supported component, store a reference to its `generateWithPreconverted` static method. Start with at least `Flow` — we can expand in step 011.

### Discovering the API Surface

The vendored JARs are in `lib/`. To inspect them:

```bash
# List classes in the engine JAR
jar tf lib/com/jsonschema/llm/json-schema-llm-engine/0.1.0-ALPHA/json-schema-llm-engine-0.1.0-ALPHA.jar | grep -v '$' | head -30

# List classes in the V4 SDK JAR
jar tf lib/io/gravitee/apim/definition/llm/v4/v4/1.0.0-SNAPSHOT/v4-1.0.0-SNAPSHOT.jar | grep -v '$' | head -30

# Or check the source
ls ~/workspace/Gravitee/gravitee-api-definition-spec/sdks/java/v4/src/main/java/io/gravitee/apim/definition/llm/v4/
```

You can also `javap` a class to see its exact method signatures:

```bash
javap -cp lib/com/jsonschema/llm/json-schema-llm-engine/0.1.0-ALPHA/json-schema-llm-engine-0.1.0-ALPHA.jar com.jsonschema.llm.engine.LlmRoundtripEngine
```

## Hard Constraints

- **No LangChain4J** — use `json-schema-llm`'s own transport directly
- **Domain interface in core layer** (`io.gravitee.apim.core.zee.domain_service`) — no infra deps
- **Implementation in infra layer** (`io.gravitee.apim.infra.zee`) — can import engine classes
- **Use `ZeeConfiguration`** from step 001 for Azure credentials
- **Branch**: Work on `feat/zee-mode` — verify before committing

## Acceptance Criteria

- [ ] `LlmEngineService` interface in core layer (zero infra dependencies)
- [ ] `LlmEngineServiceImpl` in infra layer with `HttpTransport`-based Azure OpenAI integration
- [ ] At least `Flow` component registered and callable
- [ ] Unit test with a mock transport or builder pattern (verify the pipeline wiring)
- [ ] Integration note: if possible, verify with a live Azure call that generates a valid Flow (requires `OPENAI_API_URL` and `OPENAI_API_KEY` env vars)

## After Implementation

1. **Commit** with message: `feat(zee): implement LLM engine service with HttpTransport`
2. **Request review** from one external review agent — apply any feedback
3. **Output the following work summary**:

```markdown
## Work Summary: Step 003 — LLM Engine Service

### What Was Done

- [files created/modified with full paths]

### Decisions Made

- [any choices not specified in the step file]
- [exact API shape discovered from the JARs — what methods/classes were used]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [blockers, surprises, or things affecting future steps]
- [any API mismatches in the vendored JARs]

### Test Results

- [unit test results]
- [live Azure test if attempted — include the generated output]

### Review Feedback Applied

- [what the reviewer flagged, how it was addressed]

### Ready for Next Step?

- [yes/no + any prerequisites for step 004]
```
