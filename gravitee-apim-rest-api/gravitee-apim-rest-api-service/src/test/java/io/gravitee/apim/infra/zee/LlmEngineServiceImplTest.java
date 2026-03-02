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
package io.gravitee.apim.infra.zee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jsonschema.llm.engine.LlmRoundtripEngine;
import com.jsonschema.llm.engine.LlmTransportException;
import com.jsonschema.llm.engine.RoundtripResult;
import io.gravitee.apim.core.zee.domain_service.LlmEngineService.LlmGenerationResult;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LlmEngineServiceImpl} after the SchemaGenerator facade
 * simplification.
 *
 * <p>
 * Since the implementation now delegates to
 * {@code SchemaGenerator.generate(Component, prompt, engine)}, these tests
 * verify the dispatch logic, error wrapping, and the Api description-patch
 * special case — without mocking the static SDK methods. Instead, we mock
 * the {@link LlmRoundtripEngine} that the SDK calls through.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LlmEngineServiceImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    class Generate {

        @Test
        void throws_on_unknown_component() {
            var engine = mock(LlmRoundtripEngine.class);
            var service = new LlmEngineServiceImpl(engine);

            assertThatThrownBy(() -> service.generate("prompt", "UnknownWidget"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported component: UnknownWidget");
        }

        @Test
        void throws_on_null_component_name() {
            var engine = mock(LlmRoundtripEngine.class);
            var service = new LlmEngineServiceImpl(engine);

            assertThatThrownBy(() -> service.generate("prompt", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported component: null");
        }

        @Test
        void resolves_flow_component_via_schema_generator() throws Exception {
            // Arrange — mock the engine so the SDK's SchemaGenerator.generate can call
            // through
            ObjectNode fakeData = MAPPER.createObjectNode().put("name", "rate-limit-flow").put("enabled", true);
            var fakeResult = new RoundtripResult(fakeData, null, List.of(), List.of());

            var engine = mock(LlmRoundtripEngine.class);
            when(engine.generateWithPreconverted(anyString(), anyString(), any(), eq("my prompt")))
                    .thenReturn(fakeResult);

            var service = new LlmEngineServiceImpl(engine);

            // Act
            LlmGenerationResult result = service.generate("my prompt", "Flow");

            // Assert
            assertThat(result.data()).isEqualTo(fakeData);
            assertThat(result.valid()).isTrue();
            assertThat(result.tokensUsed()).isEqualTo(-1);
            assertThat(result.warnings()).isEmpty();
            assertThat(result.validationErrors()).isEmpty();
        }

        @Test
        void api_component_uses_patch_variant() throws Exception {
            // Arrange — Api triggers the generateWithPatch path
            ObjectNode fakeData = MAPPER.createObjectNode().put("name", "my-api");
            var fakeResult = new RoundtripResult(fakeData, null, List.of(), List.of());

            var engine = mock(LlmRoundtripEngine.class);
            // The patch variant goes through generateWithPatch, not
            // generateWithPreconverted
            when(engine.generateWithPatch(anyString(), eq("create an API"), anyString()))
                    .thenReturn(fakeResult);

            var service = new LlmEngineServiceImpl(engine);

            // Act
            LlmGenerationResult result = service.generate("create an API", "Api");

            // Assert
            assertThat(result.data()).isEqualTo(fakeData);
            verify(engine).generateWithPatch(anyString(), eq("create an API"), anyString());
        }

        @Test
        void endpoint_group_component_uses_patch_variant() throws Exception {
            ObjectNode fakeData = MAPPER.createObjectNode().put("name", "my-group");
            var fakeResult = new RoundtripResult(fakeData, null, List.of(), List.of());

            var engine = mock(LlmRoundtripEngine.class);
            when(engine.generateWithPatch(anyString(), eq("create a group"), anyString()))
                    .thenReturn(fakeResult);

            var service = new LlmEngineServiceImpl(engine);

            LlmGenerationResult result = service.generate("create a group", "EndpointGroup");

            assertThat(result.data()).isEqualTo(fakeData);
            verify(engine).generateWithPatch(anyString(), eq("create a group"), anyString());
        }

        @Test
        void endpoint_component_uses_patch_variant() throws Exception {
            ObjectNode fakeData = MAPPER.createObjectNode().put("name", "my-endpoint");
            var fakeResult = new RoundtripResult(fakeData, null, List.of(), List.of());

            var engine = mock(LlmRoundtripEngine.class);
            when(engine.generateWithPatch(anyString(), eq("create an endpoint"), anyString()))
                    .thenReturn(fakeResult);

            var service = new LlmEngineServiceImpl(engine);

            LlmGenerationResult result = service.generate("create an endpoint", "Endpoint");

            assertThat(result.data()).isEqualTo(fakeData);
            verify(engine).generateWithPatch(anyString(), eq("create an endpoint"), anyString());
        }

        @Test
        void maps_validation_errors_from_roundtrip_result() throws Exception {
            ObjectNode fakeData = MAPPER.createObjectNode();
            var errors = List.of("$.name: required property missing");
            var warnings = List.of("field 'x' was dropped during rehydration");
            var fakeResult = new RoundtripResult(fakeData, null, warnings, errors);

            var engine = mock(LlmRoundtripEngine.class);
            when(engine.generateWithPreconverted(anyString(), anyString(), any(), anyString()))
                    .thenReturn(fakeResult);

            var service = new LlmEngineServiceImpl(engine);

            LlmGenerationResult result = service.generate("Create a plan", "Plan");

            assertThat(result.valid()).isFalse();
            assertThat(result.validationErrors()).containsExactly("$.name: required property missing");
            assertThat(result.warnings()).containsExactly("field 'x' was dropped during rehydration");
        }

        @Test
        void wraps_transport_exception_as_runtime_exception() throws Exception {
            var engine = mock(LlmRoundtripEngine.class);
            when(engine.generateWithPreconverted(anyString(), anyString(), any(), anyString()))
                    .thenThrow(new LlmTransportException("connection refused", 0));

            var service = new LlmEngineServiceImpl(engine);

            assertThatThrownBy(() -> service.generate("prompt", "Flow"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("LLM transport failed");
        }

        @Test
        void component_name_lookup_is_case_insensitive() throws Exception {
            // SchemaGenerator.Component.from() does case-insensitive lookup
            ObjectNode fakeData = MAPPER.createObjectNode();
            var fakeResult = new RoundtripResult(fakeData, null, List.of(), List.of());

            var engine = mock(LlmRoundtripEngine.class);
            when(engine.generateWithPreconverted(anyString(), anyString(), any(), anyString()))
                    .thenReturn(fakeResult);

            var service = new LlmEngineServiceImpl(engine);

            // "flow" (lowercase) should resolve to FLOW component
            LlmGenerationResult result = service.generate("prompt", "flow");
            assertThat(result).isNotNull();
        }
    }

    @Nested
    class DisabledMode {

        @Test
        void throws_when_disabled() {
            var config = new ZeeConfiguration();
            config.setEnabled(false);
            var service = new LlmEngineServiceImpl(config);

            assertThatThrownBy(() -> service.generate("prompt", "Flow"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Zee Mode is disabled");
        }
    }
}
