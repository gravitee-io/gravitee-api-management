# Work Request: Step 005 — GenerateResourceUseCase

## Context

Zee Mode steps 001-004 are complete on `feat/zee-mode`. Current state:

- `io.gravitee.apim.core.zee.model` — ZeeRequest, ZeeResult, ZeeResourceType, ZeeMetadata, FileContent
- `io.gravitee.apim.core.zee.domain_service` — LlmEngineService, RagContextStrategy, NoOpRagStrategy
- `io.gravitee.apim.infra.zee` — LlmEngineServiceImpl, ZeeConfiguration
- `io.gravitee.apim.infra.zee.adapter` — FlowRagContextAdapter
- **47/47 unit tests passing**

## ⚠️ Critical: Build Command

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn compile -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service \
  -DskipTests -Dskip.validation -am -T 2C
```

Tests:

```bash
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.8-tem \
PATH=$HOME/.sdkman/candidates/java/21.0.8-tem/bin:$PATH \
mvn test -pl gravitee-apim-rest-api/gravitee-apim-rest-api-service \
  -DfailIfNoTests=false -Dskip.validation -am
```

## Your Task

Read:

- **Task file**: `tmp/feat-roadmap/005-generate-resource-usecase.md`
- **Agent backend design**: `tmp/feat-roadmap/agent-backend.md`
- **Architecture overview**: `tmp/feat-roadmap/architecture-overview.md`

## What to Create

### Single File: GenerateResourceUseCase (Core Layer)

```
io.gravitee.apim.core.zee.usecase/
└── GenerateResourceUseCase.java
```

**This is a core layer class — it contains ZERO infrastructure or Spring imports.** The `@UseCase` annotation may or may not exist in the APIM codebase — check first:

```bash
grep -r "@UseCase" gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/ --include="*.java" | head -5
```

If `@UseCase` exists, use it. If not, leave the class unannotated — it will be wired by Spring in the infra layer.

### Constructor Injection (not field injection)

```java
public class GenerateResourceUseCase {
    private final LlmEngineService llmEngineService;
    private final Map<ZeeResourceType, RagContextStrategy> ragStrategies;

    // Wire strategies from a List — Spring injects all RagContextStrategy beans
    public GenerateResourceUseCase(
        LlmEngineService llmEngineService,
        List<RagContextStrategy> strategies
    ) {
        this.llmEngineService = llmEngineService;
        this.ragStrategies = strategies.stream()
            .filter(s -> s.resourceType() != null)  // exclude NoOpRagStrategy
            .collect(Collectors.toMap(RagContextStrategy::resourceType, Function.identity()));
    }
}
```

### execute() Method

```java
public ZeeResult execute(ZeeRequest request, String envId, String orgId) {
    // 1. Component name from enum
    var componentName = request.resourceType().componentName();

    // 2. RAG context (graceful fallback)
    String ragContext = "";
    try {
        var strategy = ragStrategies.getOrDefault(request.resourceType(), NoOpRagStrategy.INSTANCE);
        ragContext = strategy.retrieveContext(envId, orgId, request.contextData());
    } catch (Exception e) {
        // log warning, continue without RAG context
    }

    // 3. Build enriched prompt
    var prompt = buildPrompt(request.prompt(), ragContext, request.files());

    // 4. Call LLM
    var llmResult = llmEngineService.generate(prompt, componentName);

    // 5. Return result
    return new ZeeResult(
        request.resourceType(),
        llmResult.data(),
        new ZeeMetadata("gpt-5-nano", llmResult.tokensUsed(), !ragContext.isBlank())
    );
}
```

### Prompt Builder

```java
private String buildPrompt(String userPrompt, String ragContext, List<FileContent> files) {
    var sb = new StringBuilder();
    sb.append("You are Zee, an expert Gravitee APIM assistant. ");
    sb.append("Generate Gravitee API definition resources based on the user's request. ");
    sb.append("Be precise and follow Gravitee conventions.\n\n");

    if (!ragContext.isBlank()) {
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
```

## Unit Tests

Test the use case with mocked collaborators:

```java
class GenerateResourceUseCaseTest {
    LlmEngineService mockLlm = mock(LlmEngineService.class);
    FlowRagContextAdapter mockRag = mock(FlowRagContextAdapter.class);
    GenerateResourceUseCase useCase;

    @BeforeEach
    void setUp() {
        when(mockRag.resourceType()).thenReturn(ZeeResourceType.FLOW);
        useCase = new GenerateResourceUseCase(mockLlm, List.of(mockRag));
    }

    @Test void should_use_rag_context_when_available() { ... }
    @Test void should_fall_back_to_noop_when_no_strategy() { ... }
    @Test void should_continue_when_rag_throws() { ... }
    @Test void should_include_file_contents_in_prompt() { ... }
    @Test void should_map_component_name_from_resource_type() { ... }
}
```

## Hard Constraints

- **Core layer only** — zero Spring/infra imports in `GenerateResourceUseCase`
- **`@UseCase` only if it exists** in the codebase (check first)
- **No direct instantiation of `LlmEngineServiceImpl`** — always use the interface
- **Branch**: `feat/zee-mode`, Java 21 build command above

## Acceptance Criteria

- [ ] `GenerateResourceUseCase` compiles in core layer — zero infra deps
- [ ] All collaborators injected via constructor, not field injection
- [ ] RAG failure is caught and logged — generation continues without context
- [ ] Prompt includes RAG context, file contents, and user prompt in correct order
- [ ] Unit tests cover: happy path, no matching RAG strategy, RAG throws, files included
- [ ] **All 47 prior tests + new tests** still pass via Maven

## After Implementation

1. **Commit**: `feat(zee): add GenerateResourceUseCase orchestrator`
2. **Request external review** — apply feedback
3. **Output work summary** with this updated format:

```markdown
## Work Summary: Step 005 — GenerateResourceUseCase

### What Was Done

- [files created with full paths]

### Architecture Compliance

- [confirm: GenerateResourceUseCase is in core layer with zero infra deps]
- [confirm: @UseCase annotation used/omitted and why]
- [any Onion Architecture concessions made and rationale]

### Decisions Made

- [choices not specified in the step file]

### Deviations from Plan

- [anything different — and why]

### Issues Discovered

- [surprises or things affecting future steps]

### Test Results

- [total count — must be ≥ 47 + new tests, all passing]
- [build command output: BUILD SUCCESS]

### Review Feedback Applied

- [findings and resolutions]

### Ready for Next Step?

- [yes/no]
```
