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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.A2A_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.GUARDIAN;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.HTTP_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.LLM_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.MCP_PROXY;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PolicyCapabilitiesTest {

    private static PolicyCapabilities declaring(Map<PolicyFlowType, List<String>> flows) {
        return new PolicyCapabilities(true, new DeclaredFlows(flows));
    }

    @Test
    void should_project_the_guardian_flow_onto_guardian_phases() {
        var policy = declaring(Map.of(GUARDIAN, List.of("RESPONSE", "INTERACT"), LLM_PROXY, List.of("REQUEST")));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(policy.guardianPhases()).as("guardian phases, INTERACT dropped").containsExactly(Phase.RESPONSE);
            softly.assertThat(policy.runsInGuardian(Phase.RESPONSE)).as("RESPONSE Guardian").isTrue();
            softly.assertThat(policy.runsInGuardian(Phase.REQUEST)).as("REQUEST Guardian, declared for llm-proxy only").isFalse();
        });
    }

    @Test
    void should_not_run_in_any_guardian_without_guardian_flow() {
        var policy = declaring(Map.of(LLM_PROXY, List.of("REQUEST", "RESPONSE")));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(policy.guardianPhases()).as("guardian phases").isEmpty();
            softly.assertThat(policy.runsInGuardian(Phase.REQUEST)).as("REQUEST Guardian").isFalse();
        });
    }

    @Test
    void should_project_the_guardrail_flows_onto_api_types() {
        var policy = declaring(
            Map.of(
                LLM_PROXY,
                List.of("REQUEST", "RESPONSE"),
                MCP_PROXY,
                List.of("INTERACT", "REQUEST"),
                A2A_PROXY,
                List.of("MESSAGE_REQUEST"),
                HTTP_PROXY,
                List.of("REQUEST")
            )
        );

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(policy.supports(Set.of(ApiType.LLM_PROXY, ApiType.MCP_PROXY)))
                .as("supports llm-proxy and mcp-proxy")
                .isTrue();
            softly
                .assertThat(policy.supports(Set.of(ApiType.A2A_PROXY)))
                .as("supports a2a-proxy, declared with no Guardian phase")
                .isTrue();
            softly.assertThat(policy.supports(Set.of(ApiType.PROXY))).as("the HTTP flow is no guardrail API type").isFalse();
        });
    }
}
