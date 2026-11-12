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
package io.gravitee.gamma.rest.core.guardian.use_case;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicyPluginProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.guardian.model.Guardrail;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListGuardrailsUseCaseTest {

    private final Map<String, PolicyCapabilities> policies = new LinkedHashMap<>();
    private final ListGuardrailsUseCase useCase = new ListGuardrailsUseCase(aPolicyPluginProvider(() -> policies));

    @Test
    void should_list_only_the_guardrails_running_in_the_phase_for_the_api_type_sorted_by_name() {
        policies.put(
            "pii-filter",
            new PolicyCapabilities("PII filter", true, Map.of(ApiType.LLM_PROXY, Set.of(Phase.REQUEST, Phase.RESPONSE)))
        );
        policies.put("prompt-guard", new PolicyCapabilities("Prompt guard", true, Map.of(ApiType.LLM_PROXY, Set.of(Phase.REQUEST))));
        policies.put("answer-check", new PolicyCapabilities("Answer check", true, Map.of(ApiType.LLM_PROXY, Set.of(Phase.RESPONSE))));
        policies.put("tool-guard", new PolicyCapabilities("Tool guard", true, Map.of(ApiType.MCP_PROXY, Set.of(Phase.REQUEST))));
        policies.put(
            "transform-headers",
            new PolicyCapabilities("Transform headers", false, Map.of(ApiType.LLM_PROXY, Set.of(Phase.REQUEST)))
        );
        policies.put("no-metadata", new PolicyCapabilities("No metadata", true, Map.of()));

        var output = useCase.execute(new ListGuardrailsUseCase.Input("llm-proxy", "REQUEST"));

        assertThat(output.guardrails())
            .as("a conventional policy, another phase, another API type and missing metadata are all left out")
            .containsExactly(new Guardrail("pii-filter", "PII filter"), new Guardrail("prompt-guard", "Prompt guard"));
    }

    @Test
    void should_list_nothing_when_no_guardrail_is_installed() {
        assertThat(useCase.execute(new ListGuardrailsUseCase.Input("llm-proxy", "RESPONSE")).guardrails()).isEmpty();
    }

    @Test
    void should_list_nothing_for_an_api_type_no_guardian_can_be_created_for() {
        policies.put("tool-guard", new PolicyCapabilities("Tool guard", true, Map.of(ApiType.MCP_PROXY, Set.of(Phase.REQUEST))));

        assertThat(useCase.execute(new ListGuardrailsUseCase.Input("mcp-proxy", "REQUEST")).guardrails())
            .as("a guardrail declaring an unsupported API type can never run in a Guardian")
            .isEmpty();
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = "null",
        value = {
            "null, REQUEST",
            "'', REQUEST",
            "LLM_PROXY, REQUEST",
            "unknown, REQUEST",
            "llm-proxy, null",
            "llm-proxy, request",
            "llm-proxy, INTERACT",
        }
    )
    void should_refuse_a_missing_or_unknown_api_type_or_phase(String apiType, String phase) {
        assertThatThrownBy(() -> useCase.execute(new ListGuardrailsUseCase.Input(apiType, phase))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    @Test
    void should_name_the_expected_values_when_refusing() {
        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> useCase.execute(new ListGuardrailsUseCase.Input("unknown", "REQUEST")))
                .hasMessageContaining("unknown")
                .hasMessageContaining("llm-proxy");
            softly
                .assertThatThrownBy(() -> useCase.execute(new ListGuardrailsUseCase.Input("llm-proxy", "INTERACT")))
                .hasMessageContaining("INTERACT")
                .hasMessageContaining("REQUEST")
                .hasMessageContaining("RESPONSE");
        });
    }
}
