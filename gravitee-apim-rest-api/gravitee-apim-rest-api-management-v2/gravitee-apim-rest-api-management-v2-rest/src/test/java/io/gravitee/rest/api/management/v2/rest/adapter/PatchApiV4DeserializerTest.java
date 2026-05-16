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
package io.gravitee.rest.api.management.v2.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fixtures.core.model.ApiFixtures;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.io.IOException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PatchApiV4DeserializerTest {

    private final GraviteeMapper mapper = new GraviteeMapper(false);
    private final PatchApiV4Deserializer deserializer = new PatchApiV4Deserializer(mapper);

    @Test
    void should_not_throw_when_converting_proxy_api_to_current_state_node() {
        assertThatNoException().isThrownBy(() -> deserializer.toCurrentStateNode(ApiFixtures.aProxyApiV4()));
    }

    @Test
    void should_return_object_node_with_listener_type_from_proxy_api() {
        var result = deserializer.toCurrentStateNode(ApiFixtures.aProxyApiV4());

        assertThat(result).isInstanceOf(ObjectNode.class);
        assertThat(result.isObject()).isTrue();
        assertThat(result.path("listeners").get(0).path("type").asText()).isEqualTo("HTTP");
    }

    @Test
    void should_map_listeners_when_present() throws IOException {
        var node = deserializer.toCurrentStateNode(ApiFixtures.aProxyApiV4());

        var result = deserializer.fromPatchedNode(node);

        assertThat(result.listeners()).hasSize(1);
        assertThat(result.listeners().getFirst()).isInstanceOf(HttpListener.class);
    }

    @Test
    void should_return_empty_fields_when_all_polymorphic_fields_absent() throws IOException {
        var node = mapper.createObjectNode();

        var result = deserializer.fromPatchedNode(node);

        assertThat(result.listeners()).isEmpty();
        assertThat(result.endpointGroups()).isEmpty();
        assertThat(result.flows()).isEmpty();
        assertThat(result.resources()).isEmpty();
    }

    @Test
    void should_include_endpoint_groups_in_current_state_node_for_proxy_api() {
        var result = deserializer.toCurrentStateNode(ApiFixtures.aProxyApiV4());

        assertThat(result.path("endpointGroups").isArray()).isTrue();
        assertThat(result.path("endpointGroups").get(0).path("type").asText()).isEqualTo("http-proxy");
    }

    @Test
    void should_map_endpoint_groups_from_proxy_api_round_trip() throws IOException {
        var node = deserializer.toCurrentStateNode(ApiFixtures.aProxyApiV4());

        var result = deserializer.fromPatchedNode(node);

        assertThat(result.endpointGroups()).hasSize(1);
        assertThat(result.endpointGroups().getFirst().getType()).isEqualTo("http-proxy");
    }

    @Test
    void should_map_resources_when_present() throws IOException {
        var node = mapper.readTree("{\"resources\":[{\"name\":\"my-cache\",\"type\":\"cache\",\"enabled\":true,\"configuration\":{}}]}");

        var result = deserializer.fromPatchedNode(node);

        assertThat(result.resources()).hasSize(1);
        assertThat(result.resources().getFirst().getName()).isEqualTo("my-cache");
        assertThat(result.resources().getFirst().getType()).isEqualTo("cache");
        assertThat(result.resources().getFirst().getConfiguration()).isEqualTo("{ }");
    }

    @Test
    void should_map_flows_when_present() throws IOException {
        var node = mapper.readTree("{\"flows\":[{\"name\":\"my-flow\",\"enabled\":true,\"request\":[],\"response\":[]}]}");

        var result = deserializer.fromPatchedNode(node);

        assertThat(result.flows()).hasSize(1);
        assertThat(result.flows().getFirst().getName()).isEqualTo("my-flow");
        assertThat(result.flows().getFirst().isEnabled()).isTrue();
    }

    @Test
    void should_accept_uppercase_HTTP_listener_type_in_patch() throws IOException {
        var node = mapper.readTree(
            "{\"listeners\":[{\"type\":\"HTTP\",\"paths\":[{\"path\":\"/x\"}],\"entrypoints\":[{\"type\":\"http-proxy\",\"configuration\":\"{}\"}]}]}"
        );

        var result = deserializer.fromPatchedNode(node);

        assertThat(result.listeners()).hasSize(1);
        assertThat(result.listeners().getFirst()).isInstanceOf(HttpListener.class);
    }
}
