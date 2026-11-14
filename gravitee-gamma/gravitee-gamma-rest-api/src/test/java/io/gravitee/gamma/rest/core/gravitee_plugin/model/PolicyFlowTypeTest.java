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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.ApiType;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PolicyFlowTypeTest {

    @Test
    void should_read_each_flow_from_its_manifest_key() {
        assertThat(Arrays.stream(PolicyFlowType.values()).collect(Collectors.toMap(flow -> flow, PolicyFlowType::manifestKey)))
            .as("manifest keys")
            .isEqualTo(
                Map.of(
                    PolicyFlowType.HTTP_PROXY,
                    "http_proxy",
                    PolicyFlowType.HTTP_MESSAGE,
                    "http_message",
                    PolicyFlowType.NATIVE_KAFKA,
                    "native_kafka",
                    PolicyFlowType.MCP_PROXY,
                    "mcp_proxy",
                    PolicyFlowType.LLM_PROXY,
                    "llm_proxy",
                    PolicyFlowType.A2A_PROXY,
                    "a2a_proxy",
                    PolicyFlowType.GUARDIAN,
                    "guardian"
                )
            );
    }

    @Test
    void should_fall_back_on_the_deprecated_keys_of_the_http_flows_only() {
        SoftAssertions.assertSoftly(softly -> {
            for (var flow : PolicyFlowType.values()) {
                var expected = switch (flow) {
                    case HTTP_PROXY -> Optional.of("proxy");
                    case HTTP_MESSAGE -> Optional.of("message");
                    default -> Optional.<String>empty();
                };
                softly.assertThat(flow.deprecatedManifestKey()).as("deprecated key of %s", flow).isEqualTo(expected);
            }
        });
    }

    @Test
    void should_map_each_guardrail_flow_to_its_api_type_and_scope_protocol() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(PolicyFlowType.LLM_PROXY.guardrailApiType()).as("llm-proxy API type").contains(ApiType.LLM_PROXY);
            softly.assertThat(PolicyFlowType.MCP_PROXY.guardrailApiType()).as("mcp-proxy API type").contains(ApiType.MCP_PROXY);
            softly.assertThat(PolicyFlowType.A2A_PROXY.guardrailApiType()).as("a2a-proxy API type").contains(ApiType.A2A_PROXY);
            softly.assertThat(PolicyFlowType.fromScopeProtocol("LLM")).as("LLM scope").contains(PolicyFlowType.LLM_PROXY);
            softly.assertThat(PolicyFlowType.fromScopeProtocol("MCP")).as("MCP scope").contains(PolicyFlowType.MCP_PROXY);
            softly.assertThat(PolicyFlowType.fromScopeProtocol("A2A")).as("A2A scope").contains(PolicyFlowType.A2A_PROXY);
            softly.assertThat(PolicyFlowType.fromScopeProtocol("llm")).as("scope protocols are case-sensitive").isEmpty();
            softly.assertThat(PolicyFlowType.fromScopeProtocol("HTTP")).as("HTTP scope").isEmpty();
            for (var flow : new PolicyFlowType[] {
                PolicyFlowType.HTTP_PROXY,
                PolicyFlowType.HTTP_MESSAGE,
                PolicyFlowType.NATIVE_KAFKA,
                PolicyFlowType.GUARDIAN,
            }) {
                softly.assertThat(flow.guardrailApiType()).as("%s is no guardrail flow", flow).isEmpty();
            }
        });
    }
}
