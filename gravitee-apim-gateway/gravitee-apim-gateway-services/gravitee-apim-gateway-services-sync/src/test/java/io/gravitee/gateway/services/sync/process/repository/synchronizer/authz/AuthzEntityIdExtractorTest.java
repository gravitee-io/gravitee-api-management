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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzEntityIdExtractorTest {

    private final AuthzEntityIdExtractor extractor = new AuthzEntityIdExtractor();

    @Test
    void null_api_returns_empty_set() {
        assertThat(extractor.extract(null)).isEmpty();
    }

    @Test
    void v4_proxy_api_emits_only_api_alias() {
        ReactableApi<?> api = httpProxyApi("api-1");

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactly("api.api-1");
    }

    @Test
    void v4_mcp_proxy_with_no_flows_emits_only_api_alias() {
        ReactableApi<?> api = mcpProxyApi("bookings", List.of());

        assertThat(extractor.extract(api)).containsExactly("api.bookings");
    }

    @Test
    void v4_mcp_proxy_emits_one_alias_per_tools_call_flow() {
        Flow getBooking = mcpToolFlow("get-booking", Set.of("tools/call"));
        Flow listBookings = mcpToolFlow("list-bookings", Set.of("tools/call"));
        ReactableApi<?> api = mcpProxyApi("bookings", List.of(getBooking, listBookings));

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactlyInAnyOrder("api.bookings", "mcp.bookings.get-booking", "mcp.bookings.list-bookings");
    }

    @Test
    void mcp_flow_with_methods_not_including_tools_call_is_skipped() {
        Flow listFlow = mcpToolFlow("list-flow", Set.of("tools/list"));
        ReactableApi<?> api = mcpProxyApi("bookings", List.of(listFlow));

        assertThat(extractor.extract(api)).containsExactly("api.bookings");
    }

    @Test
    void mcp_flow_with_empty_methods_treats_all_as_matching_and_emits_a_tool_alias() {
        Flow universalFlow = mcpToolFlow("any-tool", Set.of());
        ReactableApi<?> api = mcpProxyApi("bookings", List.of(universalFlow));

        assertThat(extractor.extract(api)).containsExactlyInAnyOrder("api.bookings", "mcp.bookings.any-tool");
    }

    @Test
    void mcp_flow_with_blank_name_is_skipped() {
        Flow nameless = mcpToolFlow("", Set.of("tools/call"));
        ReactableApi<?> api = mcpProxyApi("bookings", List.of(nameless));

        assertThat(extractor.extract(api)).containsExactly("api.bookings");
    }

    @Test
    void mcp_proxy_with_null_flows_emits_only_api_alias() {
        ReactableApi<?> api = mcpProxyApi("bookings", null);

        assertThat(extractor.extract(api)).containsExactly("api.bookings");
    }

    @Test
    void mcp_flow_without_an_mcp_selector_is_skipped() {
        Flow flow = new Flow();
        flow.setName("not-an-mcp-flow");
        ReactableApi<?> api = mcpProxyApi("bookings", List.of(flow));

        assertThat(extractor.extract(api)).containsExactly("api.bookings");
    }

    @Test
    void documents_known_gap_extracts_id_only_no_crossId_fallback() {
        ReactableApi<?> api = httpProxyApi("api-1");

        Set<String> ids = extractor.extract(api);

        assertThat(ids).containsExactly("api.api-1");
    }

    @Test
    void api_alias_appears_first_to_keep_iteration_order_stable() {
        Flow first = mcpToolFlow("first", Set.of("tools/call"));
        Flow second = mcpToolFlow("second", Set.of("tools/call"));
        ReactableApi<?> api = mcpProxyApi("bookings", List.of(first, second));

        assertThat(extractor.extract(api)).containsExactly("api.bookings", "mcp.bookings.first", "mcp.bookings.second");
    }

    private static ReactableApi<?> httpProxyApi(String id) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("test");
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.PROXY);
        definition.setFlows(List.of());
        return new io.gravitee.gateway.reactive.handlers.api.v4.Api(definition);
    }

    private static ReactableApi<?> mcpProxyApi(String id, List<Flow> flows) {
        io.gravitee.definition.model.v4.Api definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(id);
        definition.setName("test");
        definition.setApiVersion("1");
        definition.setDefinitionVersion(DefinitionVersion.V4);
        definition.setType(ApiType.MCP_PROXY);
        definition.setFlows(flows);
        return new io.gravitee.gateway.reactive.handlers.api.v4.Api(definition);
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
