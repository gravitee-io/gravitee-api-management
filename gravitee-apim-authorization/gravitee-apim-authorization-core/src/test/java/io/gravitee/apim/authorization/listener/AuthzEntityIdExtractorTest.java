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
package io.gravitee.apim.authorization.listener;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzEntityIdExtractorTest {

    private final AuthzEntityIdExtractor extractor = new AuthzEntityIdExtractor();

    @Test
    void extracts_api_prefix_using_id_when_crossId_is_null() {
        Api api = httpApi("api-1", null, "env-1");

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactly("api.api-1");
    }

    @Test
    void extracts_api_prefix_using_crossId_when_present() {
        Api api = httpApi("api-1", "cross-1", "env-1");

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactly("api.cross-1");
    }

    @Test
    void extracts_api_prefix_for_a_non_mcp_proxy_with_no_tool_aliases() {
        Api api = httpApi("api-1", null, "env-1");

        assertThat(extractor.extract(api)).containsExactly("api.api-1");
    }

    @Test
    void extracts_mcp_tool_aliases_for_each_tools_call_flow() {
        Flow getBooking = mcpToolFlow("get-booking", Set.of("tools/call"));
        Flow listBookings = mcpToolFlow("list-bookings", Set.of("tools/call"));
        Api api = mcpProxyApi("api-1", "bookings", "env-1", List.of(getBooking, listBookings));

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactlyInAnyOrder("api.bookings", "mcp.bookings.get-booking", "mcp.bookings.list-bookings");
    }

    @Test
    void mcp_flow_with_methods_not_including_tools_call_is_skipped() {
        Flow listFlow = mcpToolFlow("list-flow", Set.of("tools/list"));
        Api api = mcpProxyApi("api-1", "bookings", "env-1", List.of(listFlow));

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactly("api.bookings");
    }

    @Test
    void mcp_flow_with_empty_methods_treats_all_as_matching_and_emits_a_tool_alias() {
        Flow universalFlow = mcpToolFlow("any-tool", Set.of());
        Api api = mcpProxyApi("api-1", "bookings", "env-1", List.of(universalFlow));

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactlyInAnyOrder("api.bookings", "mcp.bookings.any-tool");
    }

    @Test
    void mcp_flow_with_blank_name_is_skipped() {
        Flow nameless = mcpToolFlow("", Set.of("tools/call"));
        Api api = mcpProxyApi("api-1", "bookings", "env-1", List.of(nameless));

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactly("api.bookings");
    }

    @Test
    void mcp_proxy_with_no_flows_emits_only_the_api_alias() {
        Api api = mcpProxyApi("api-1", "bookings", "env-1", List.of());

        assertThat(extractor.extract(api)).containsExactly("api.bookings");
    }

    @Test
    void null_api_returns_empty_set() {
        assertThat(extractor.extract(null)).isEmpty();
    }

    private static Api httpApi(String id, String crossId, String envId) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("test");
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.PROXY);
        definition.setFlows(List.of());

        Api api = Api.builder().id(id).crossId(crossId).environmentId(envId).name("test").build();
        api.setApiDefinitionValue(definition);
        return api;
    }

    private static Api mcpProxyApi(String id, String crossId, String envId, List<Flow> flows) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("test");
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.MCP_PROXY);
        definition.setFlows(flows);

        Api api = Api.builder().id(id).crossId(crossId).environmentId(envId).name("test").build();
        api.setApiDefinitionValue(definition);
        return api;
    }

    private static Flow mcpToolFlow(String name, Set<String> methods) {
        McpSelector selector = new McpSelector();
        selector.setMethods(methods);

        Flow flow = new Flow();
        flow.setName(name);
        flow.setSelectors(List.<Selector>of(selector));
        return flow;
    }
}
