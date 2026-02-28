/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.core.zee.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.zee.domain_service.LlmEngineService;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.model.FileContent;
import io.gravitee.apim.core.zee.model.ZeeRequest;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link GenerateResourceUseCase}.
 */
class GenerateResourceUseCaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode FAKE_JSON = MAPPER.createObjectNode().put("name", "my-flow");
    private static final LlmEngineService.LlmGenerationResult FAKE_RESULT = new LlmEngineService.LlmGenerationResult(
        FAKE_JSON,
        true,
        100,
        List.of(),
        List.of()
    );

    LlmEngineService mockLlm = mock(LlmEngineService.class);
    RagContextStrategy mockRag = mock(RagContextStrategy.class);
    GenerateResourceUseCase useCase;

    @BeforeEach
    void setUp() {
        when(mockRag.resourceType()).thenReturn(ZeeResourceType.FLOW);
        when(mockRag.retrieveContext(anyString(), anyString(), any())).thenReturn("");
        when(mockLlm.generate(anyString(), anyString())).thenReturn(FAKE_RESULT);
        useCase = new GenerateResourceUseCase(mockLlm, List.of(mockRag));
    }

    @Test
    void should_return_result_with_correct_resource_type() {
        when(mockRag.retrieveContext(anyString(), anyString(), any())).thenReturn("rag context");

        var request = new ZeeRequest(ZeeResourceType.FLOW, "create a flow", List.of(), Map.of());
        var result = useCase.execute(request, "env-1", "org-1");

        assertThat(result).isNotNull();
        assertThat(result.resourceType()).isEqualTo(ZeeResourceType.FLOW);
        assertThat(result.generated()).isEqualTo(FAKE_JSON);
    }

    @Test
    void should_use_rag_context_when_available() {
        when(mockRag.retrieveContext(anyString(), anyString(), any())).thenReturn("existing flows context");

        var request = new ZeeRequest(ZeeResourceType.FLOW, "create a flow", List.of(), Map.of());
        var result = useCase.execute(request, "env-1", "org-1");

        assertThat(result.metadata().ragContextUsed()).isTrue();
        var promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLlm).generate(promptCaptor.capture(), eq("Flow"));
        assertThat(promptCaptor.getValue()).contains("## Existing Configuration Context");
        assertThat(promptCaptor.getValue()).contains("existing flows context");
    }

    @Test
    void should_fall_back_to_noop_when_no_strategy_for_resource_type() {
        // No RAG strategy registered for PLAN — useCase only has FLOW strategy → falls
        // back to NoOp (empty context)
        var request = new ZeeRequest(ZeeResourceType.PLAN, "create a plan", List.of(), Map.of());
        var result = useCase.execute(request, "env-1", "org-1");

        assertThat(result.metadata().ragContextUsed()).isFalse();
        var promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLlm).generate(promptCaptor.capture(), eq("Plan"));
        assertThat(promptCaptor.getValue()).doesNotContain("## Existing Configuration Context");
    }

    @Test
    void should_continue_when_rag_throws() {
        when(mockRag.retrieveContext(any(), any(), any())).thenThrow(new RuntimeException("RAG service unavailable"));

        var request = new ZeeRequest(ZeeResourceType.FLOW, "create a flow", List.of(), Map.of());
        // Should not throw — graceful degradation
        var result = useCase.execute(request, "env-1", "org-1");

        assertThat(result).isNotNull();
        assertThat(result.metadata().ragContextUsed()).isFalse();
        verify(mockLlm).generate(anyString(), eq("Flow"));
    }

    @Test
    void should_include_file_contents_in_prompt() {
        var file = new FileContent("spec.json", "{\"openapi\":\"3.0\"}", "application/json");
        var request = new ZeeRequest(ZeeResourceType.FLOW, "create from spec", List.of(file), Map.of());
        useCase.execute(request, "env-1", "org-1");

        var promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLlm).generate(promptCaptor.capture(), anyString());
        var prompt = promptCaptor.getValue();
        assertThat(prompt).contains("## Uploaded Files");
        assertThat(prompt).contains("### spec.json");
        assertThat(prompt).contains("{\"openapi\":\"3.0\"}");
    }

    @Test
    void should_map_component_name_from_resource_type() {
        var request = new ZeeRequest(ZeeResourceType.FLOW, "create flow", List.of(), Map.of());
        useCase.execute(request, "env-1", "org-1");

        verify(mockLlm).generate(anyString(), eq("Flow"));
    }

    @Test
    void should_include_user_prompt_in_generated_prompt() {
        var request = new ZeeRequest(ZeeResourceType.FLOW, "my specific user request", List.of(), Map.of());
        useCase.execute(request, "env-1", "org-1");

        var promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLlm).generate(promptCaptor.capture(), anyString());
        assertThat(promptCaptor.getValue()).contains("## User Request");
        assertThat(promptCaptor.getValue()).contains("my specific user request");
    }

    @Test
    void should_propagate_tokens_used_from_llm_result() {
        var request = new ZeeRequest(ZeeResourceType.FLOW, "create flow", List.of(), Map.of());
        var result = useCase.execute(request, "env-1", "org-1");

        assertThat(result.metadata().tokensUsed()).isEqualTo(100);
    }

    @Test
    void should_pass_env_and_org_to_rag_strategy() {
        var request = new ZeeRequest(ZeeResourceType.FLOW, "create flow", List.of(), Map.of("apiId", "api-123"));
        useCase.execute(request, "env-abc", "org-xyz");

        verify(mockRag).retrieveContext(eq("env-abc"), eq("org-xyz"), any());
    }
}
