# 007: Backend Integration Test

> **Common docs**: [agent-backend.md](./agent-backend.md) · [json-schema-llm-integration.md](./json-schema-llm-integration.md)

## Objective

Verify the full backend pipeline works end-to-end: REST endpoint → use case → RAG → LLM → rehydrated response. Both unit tests and a live curl test.

## Unit Tests

### GenerateResourceUseCaseTest

```java
@Test
void should_generate_flow_with_rag_context() {
    // Given
    var mockLlm = mock(LlmEngineService.class);
    var mockRag = mock(RagContextStrategy.class);
    when(mockRag.resourceType()).thenReturn(ZeeResourceType.FLOW);
    when(mockRag.retrieveContext(any(), any(), any())).thenReturn("## Existing flows...");
    when(mockLlm.generate(anyString(), eq("Flow"))).thenReturn(
        new LlmGenerationResult(validFlowJson(), true, 1000, List.of()));

    var useCase = new GenerateResourceUseCase(mockLlm, List.of(mockRag));

    // When
    var result = useCase.execute(
        new ZeeRequest(ZeeResourceType.FLOW, "Create a rate limit flow", List.of(), Map.of("apiId", "123")),
        "env-1", "org-1");

    // Then
    assertThat(result.resourceType()).isEqualTo(ZeeResourceType.FLOW);
    assertThat(result.generated()).isNotNull();
    assertThat(result.metadata().ragContextUsed()).isTrue();
}
```

### LlmEngineServiceImplTest

Test with a mock HTTP transport that returns pre-built JSON.

### ZeeResourceTest

Test the JAX-RS endpoint with a mocked use case — verify request parsing, response format, error handling.

## Live curl Test

Requires APIM REST API running locally with env vars set:

```bash
# Start REST API
cd gravitee-apim-rest-api
OPENAI_API_URL=... OPENAI_API_KEY=... \
mvn clean compile exec:java -Pdev \
  -pl gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-container

# Test (in another terminal)
curl -X POST http://localhost:8083/management/v2/ai/generate \
  -H "Authorization: Bearer <console-jwt-token>" \
  -F 'request={"resourceType":"FLOW","prompt":"Create a rate-limiting flow for /api/v1/* that limits to 100 requests per minute","contextData":{"apiId":"my-api-id"}};type=application/json' \
  | jq .
```

## Acceptance Criteria

- [ ] Unit tests pass for UseCase, LlmEngineService, ZeeResource
- [ ] Live curl test returns a valid structured Flow
- [ ] Error cases handled: invalid resourceType, blank prompt, LLM failure

## Commit Message

```
test(zee): add backend unit and integration tests

Add unit tests for GenerateResourceUseCase, LlmEngineServiceImpl,
and ZeeResource. Includes curl-based integration test procedure.
```
