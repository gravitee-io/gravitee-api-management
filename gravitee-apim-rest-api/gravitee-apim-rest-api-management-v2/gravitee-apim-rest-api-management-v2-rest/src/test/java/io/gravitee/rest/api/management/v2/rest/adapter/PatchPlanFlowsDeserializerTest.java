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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PatchPlanFlowsDeserializerTest {

    private final GraviteeMapper mapper = new GraviteeMapper(false);
    private final PatchPlanFlowsDeserializer deserializer = new PatchPlanFlowsDeserializer(mapper);

    private static final String FLOW_ID = "flow-id-1";

    private static Flow aFlowWithHttpSelector() {
        return Flow.builder()
            .id(FLOW_ID)
            .name("my-flow")
            .enabled(true)
            .selectors(List.of(HttpSelector.builder().path("/api").build()))
            .build();
    }

    @Test
    void fromPatchedFlowsNode_accepts_uppercase_HTTP_selector_and_returns_domain_flow_with_HttpSelector() throws IOException {
        var node = mapper.readTree(
            "[{\"id\":\"" +
                FLOW_ID +
                "\",\"name\":\"my-flow\",\"enabled\":true,\"selectors\":[{\"type\":\"HTTP\",\"path\":\"/api\",\"pathOperator\":\"STARTS_WITH\"}]}]"
        );

        var result = deserializer.fromPatchedFlowsNode(node);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSelectors()).hasSize(1);
        assertThat(result.get(0).getSelectors().get(0)).isInstanceOf(HttpSelector.class);
    }

    @Test
    void fromPatchedFlowsNode_rejects_lowercase_http_selector_discriminator_with_ValidationDomainException() {
        var node = mapper.valueToTree(
            List.of(
                mapper
                    .createObjectNode()
                    .put("name", "my-flow")
                    .put("enabled", true)
                    .set(
                        "selectors",
                        mapper
                            .createArrayNode()
                            .add(mapper.createObjectNode().put("type", "http").put("path", "/api").put("pathOperator", "STARTS_WITH"))
                    )
            )
        );

        assertThatThrownBy(() -> deserializer.fromPatchedFlowsNode(node))
            .isInstanceOf(ValidationDomainException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void fromPatchedFlowsNode_rejects_BOGUS_selector_discriminator_with_ValidationDomainException() {
        var node = mapper.valueToTree(
            List.of(
                mapper
                    .createObjectNode()
                    .put("name", "my-flow")
                    .put("enabled", true)
                    .set(
                        "selectors",
                        mapper
                            .createArrayNode()
                            .add(mapper.createObjectNode().put("type", "BOGUS").put("path", "/api").put("pathOperator", "STARTS_WITH"))
                    )
            )
        );

        assertThatThrownBy(() -> deserializer.fromPatchedFlowsNode(node))
            .isInstanceOf(ValidationDomainException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void toCurrentFlowsNode_emits_uppercase_HTTP_for_HttpSelector() {
        var flows = List.of(aFlowWithHttpSelector());

        var result = deserializer.toCurrentFlowsNode(flows);

        assertThat(result.isArray()).isTrue();
        assertThat(result.get(0).path("selectors").get(0).path("type").asText()).isEqualTo("HTTP");
    }

    @Test
    void round_trip_preserves_flow_id() throws IOException {
        var flows = List.of(aFlowWithHttpSelector());
        var node = deserializer.toCurrentFlowsNode(flows);

        var result = deserializer.fromPatchedFlowsNode(node);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(FLOW_ID);
    }

    @Test
    void round_trip_preserves_populated_tags() throws IOException {
        var flow = Flow.builder()
            .id(FLOW_ID)
            .name("my-flow")
            .enabled(true)
            .selectors(List.of(HttpSelector.builder().path("/api").build()))
            .tags(Set.of("tag1", "tag2"))
            .build();

        var result = deserializer.fromPatchedFlowsNode(deserializer.toCurrentFlowsNode(List.of(flow)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTags()).containsExactlyInAnyOrder("tag1", "tag2");
    }

    @Test
    void round_trip_normalizes_null_tags_to_empty() throws IOException {
        var flow = Flow.builder()
            .id(FLOW_ID)
            .name("my-flow")
            .enabled(true)
            .selectors(List.of(HttpSelector.builder().path("/api").build()))
            .build();

        var result = deserializer.fromPatchedFlowsNode(deserializer.toCurrentFlowsNode(List.of(flow)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTags()).isEmpty();
        assertThat(result.get(0).getSubscribe()).isEmpty();
        assertThat(result.get(0).getPublish()).isEmpty();
    }

    @Test
    void round_trip_preserves_populated_methods() throws IOException {
        var flow = Flow.builder()
            .id(FLOW_ID)
            .name("my-flow")
            .enabled(true)
            .selectors(List.of(HttpSelector.builder().path("/api").methods(Set.of(HttpMethod.GET, HttpMethod.POST)).build()))
            .build();

        var result = deserializer.fromPatchedFlowsNode(deserializer.toCurrentFlowsNode(List.of(flow)));

        assertThat(result).hasSize(1);
        var selector = (HttpSelector) result.get(0).getSelectors().get(0);
        assertThat(selector.getMethods()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
    }

    @Test
    void round_trip_normalizes_null_methods_to_empty() throws IOException {
        var flow = Flow.builder()
            .id(FLOW_ID)
            .name("my-flow")
            .enabled(true)
            .selectors(List.of(HttpSelector.builder().path("/api").build()))
            .build();

        var result = deserializer.fromPatchedFlowsNode(deserializer.toCurrentFlowsNode(List.of(flow)));

        assertThat(result).hasSize(1);
        var selector = (HttpSelector) result.get(0).getSelectors().get(0);
        assertThat(selector.getMethods()).isEmpty();
    }

    @Test
    void round_trip_preserves_request_and_response_steps() throws IOException {
        var requestStep = Step.builder().name("req-step").policy("transform-headers").build();
        var responseStep = Step.builder().name("resp-step").policy("cache").build();
        var flow = Flow.builder()
            .id(FLOW_ID)
            .name("my-flow")
            .enabled(true)
            .selectors(List.of(HttpSelector.builder().path("/api").build()))
            .request(List.of(requestStep))
            .response(List.of(responseStep))
            .build();

        var result = deserializer.fromPatchedFlowsNode(deserializer.toCurrentFlowsNode(List.of(flow)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRequest()).hasSize(1);
        assertThat(result.get(0).getRequest().get(0).getName()).isEqualTo("req-step");
        assertThat(result.get(0).getRequest().get(0).getPolicy()).isEqualTo("transform-headers");
        assertThat(result.get(0).getResponse()).hasSize(1);
        assertThat(result.get(0).getResponse().get(0).getName()).isEqualTo("resp-step");
        assertThat(result.get(0).getResponse().get(0).getPolicy()).isEqualTo("cache");
    }

    @Test
    void round_trip_preserves_condition_selector() throws IOException {
        var flow = Flow.builder()
            .id(FLOW_ID)
            .name("my-flow")
            .enabled(true)
            .selectors(List.of(ConditionSelector.builder().condition("{#request.content.length() > 10}").build()))
            .build();

        var result = deserializer.fromPatchedFlowsNode(deserializer.toCurrentFlowsNode(List.of(flow)));

        assertThat(result).hasSize(1);
        var selector = (ConditionSelector) result.get(0).getSelectors().get(0);
        assertThat(selector.getCondition()).isEqualTo("{#request.content.length() > 10}");
    }
}
