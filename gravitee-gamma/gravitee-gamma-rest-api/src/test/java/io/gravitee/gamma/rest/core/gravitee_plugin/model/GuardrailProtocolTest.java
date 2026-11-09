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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Pins the table that gravitee-reactor-guardian's GuardianProtocolValidator applies at deployment. */
class GuardrailProtocolTest {

    @Test
    void should_map_each_scope_protocol_to_the_api_type_the_gateway_expects() {
        assertThat(Arrays.stream(GuardrailProtocol.values()).collect(Collectors.toMap(Enum::name, GuardrailProtocol::apiType))).isEqualTo(
            Map.of("LLM", ApiType.LLM_PROXY, "MCP", ApiType.MCP_PROXY, "A2A", ApiType.A2A_PROXY)
        );
    }

    @Test
    void should_read_the_manifest_property_named_after_the_api_type() {
        assertThat(
            Arrays.stream(GuardrailProtocol.values()).collect(Collectors.toMap(Function.identity(), GuardrailProtocol::manifestKey))
        ).isEqualTo(Map.of(GuardrailProtocol.LLM, "llm_proxy", GuardrailProtocol.MCP, "mcp_proxy", GuardrailProtocol.A2A, "a2a_proxy"));
    }

    @Test
    void should_resolve_only_known_scope_protocols() {
        assertThat(GuardrailProtocol.fromScope("MCP")).contains(GuardrailProtocol.MCP);
        assertThat(GuardrailProtocol.fromScope("mcp")).as("scope protocols are case sensitive").isEmpty();
        assertThat(GuardrailProtocol.fromScope("HTTP")).isEmpty();
    }
}
