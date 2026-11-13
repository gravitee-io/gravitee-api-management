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
package io.gravitee.gamma.rest.resources.gravitee_plugin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPolicyPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PolicyPluginDtoTest {

    @Test
    void should_serialize_the_flows_in_the_order_of_the_flow_enum() {
        var flows = new EnumMap<PolicyFlowType, List<String>>(PolicyFlowType.class);
        for (var flow : PolicyFlowType.values()) {
            flows.put(flow, List.of("REQUEST"));
        }
        var policy = new PlatformPolicyPlugin(PlatformPlugin.builder().id("policy").build(), new DeclaredFlows(flows));

        JsonNode flow = new ObjectMapper().valueToTree(PolicyPluginDto.from(policy)).get("flow");

        Iterable<String> keys = flow::fieldNames;
        assertThat(StreamSupport.stream(keys.spliterator(), false).toList())
            .as("flow keys")
            .containsExactly("http-proxy", "http-message", "native-kafka", "mcp-proxy", "llm-proxy", "a2a-proxy", "guardian");
    }
}
