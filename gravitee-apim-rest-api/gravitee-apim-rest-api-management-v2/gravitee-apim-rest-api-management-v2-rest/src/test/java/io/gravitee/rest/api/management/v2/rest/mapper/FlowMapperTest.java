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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import fixtures.FlowFixtures;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.step.StepV4;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.rest.api.management.v2.rest.model.FlowV2;
import io.gravitee.rest.api.management.v2.rest.model.StepV2;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class FlowMapperTest {

    private final FlowMapper flowMapper = Mappers.getMapper(FlowMapper.class);

    /**
     * Http Flow
     */

    @Test
    void should_map_from_HttpFlowEntityV4() throws JsonProcessingException {
        var flowEntityV4 = FlowFixtures.aModelFlowHttpV4();
        var flowV4 = flowMapper.mapFromHttpV4(flowEntityV4);
        assertThat(flowV4).isNotNull();
        assertHttpFlowV4Equals(flowEntityV4, flowV4);
    }

    @Test
    void should_map_to_HttpFlowEntityV4() throws JsonProcessingException {
        var flowV4 = FlowFixtures.aFlowHttpV4();
        var flowEntityV4 = flowMapper.mapToHttpV4(flowV4);
        assertThat(flowV4).isNotNull();
        assertHttpFlowV4Equals(flowEntityV4, flowV4);
    }

    @Test
    void should_map_FlowV4_to_list_of_HttpFlow() throws JsonProcessingException {
        var flow1 = FlowFixtures.aFlowHttpV4();
        var flow2 = FlowFixtures.aFlowHttpV4();
        var flows = flowMapper.mapToHttpV4(List.of(flow1, flow2));
        assertThat(flows).hasSize(2);
        assertHttpFlowV4Equals(flows.get(0), flow1);
        assertHttpFlowV4Equals(flows.get(1), flow2);
    }

    /**
     * Native Flow
     */

    @Test
    void should_map_from_NativeFlowEntityV4() throws JsonProcessingException {
        var flowEntityV4 = FlowFixtures.aModelFlowNativeV4();
        var flowV4 = flowMapper.mapFromNativeV4(flowEntityV4);
        assertThat(flowV4).isNotNull();
        assertNativeFlowV4Equals(flowEntityV4, flowV4);
    }

    @Test
    void should_map_to_NativeFlowEntityV4() throws JsonProcessingException {
        var flowV4 = FlowFixtures.aFlowNativeV4();
        var flowEntityV4 = flowMapper.mapToNativeV4(flowV4);
        assertThat(flowV4).isNotNull();
        assertNativeFlowV4Equals(flowEntityV4, flowV4);
    }

    @Test
    void should_map_FlowV4_to_list_of_NativeFlow() throws JsonProcessingException {
        var flow1 = FlowFixtures.aFlowNativeV4();
        var flow2 = FlowFixtures.aFlowNativeV4();
        var flows = flowMapper.mapToNativeV4(List.of(flow1, flow2));
        assertThat(flows).hasSize(2);
        assertNativeFlowV4Equals(flows.get(0), flow1);
        assertNativeFlowV4Equals(flows.get(1), flow2);
    }

    private void assertHttpFlowV4Equals(FlowV4Impl flowEntityV4, io.gravitee.rest.api.management.v2.rest.model.FlowV4 flowV4)
        throws JsonProcessingException {
        assertEquals(flowEntityV4.getName(), flowV4.getName());

        final var flowSelectors = flowEntityV4.getSelectors();
        final var flowV4Selectors = flowV4.getSelectors();

        assertThat(flowSelectors).hasSameSizeAs(flowV4Selectors);

        for (int j = 0; j < flowSelectors.size(); j++) {
            final ChannelSelector selector = (ChannelSelector) flowSelectors.getFirst();
            final io.gravitee.rest.api.management.v2.rest.model.ChannelSelector selectorV4 = flowV4Selectors.get(0).getChannelSelector();

            assertEquals(selector.getChannel(), selectorV4.getChannel());
            assertEquals(selector.getChannelOperator().name(), selectorV4.getChannelOperator().name());
            assertEquals(selector.getEntrypoints(), selectorV4.getEntrypoints());
            assertEquals(selector.getType().name(), selectorV4.getType().name());

            assertEquals(
                selector.getOperations().stream().map(Enum::name).collect(Collectors.toSet()),
                selectorV4.getOperations().stream().map(Enum::name).collect(Collectors.toSet())
            );
        }

        assertThat(flowV4.getConnect()).isEmpty();
        assertThat(flowV4.getInteract()).isEmpty();

        assertStepsV4Equals(flowEntityV4.getRequest(), flowV4.getRequest());
        assertStepsV4Equals(flowEntityV4.getPublish(), flowV4.getPublish());
        assertStepsV4Equals(flowEntityV4.getResponse(), flowV4.getResponse());
        assertStepsV4Equals(flowEntityV4.getSubscribe(), flowV4.getSubscribe());
    }

    private void assertNativeFlowV4Equals(NativeFlow flowEntityV4, io.gravitee.rest.api.management.v2.rest.model.FlowV4 flowV4)
        throws JsonProcessingException {
        assertEquals(flowEntityV4.getName(), flowV4.getName());

        final var flowV4Selectors = flowV4.getSelectors();
        assertThat(flowV4Selectors).isEmpty();
        assertThat(flowV4.getRequest()).isEmpty();
        assertThat(flowV4.getResponse()).isEmpty();

        assertStepsV4Equals(flowEntityV4.getPublish(), flowV4.getPublish());
        assertStepsV4Equals(flowEntityV4.getSubscribe(), flowV4.getSubscribe());
        assertStepsV4Equals(flowEntityV4.getConnect(), flowV4.getConnect());
        assertStepsV4Equals(flowEntityV4.getInteract(), flowV4.getInteract());
    }

    private void assertStepsV4Equals(List<StepV4> steps, List<io.gravitee.rest.api.management.v2.rest.model.StepV4> stepsV4)
        throws JsonProcessingException {
        assertThat(steps).hasSameSizeAs(stepsV4);

        for (int i = 0; i < steps.size(); i++) {
            final var step = steps.get(i);
            final var stepV4 = stepsV4.get(i);
            assertEquals(step.getName(), stepV4.getName());
            assertEquals(step.getDescription(), stepV4.getDescription());
            assertEquals(step.getPolicy(), stepV4.getPolicy());
            assertEquals(step.getCondition(), stepV4.getCondition());
            assertEquals(step.getMessageCondition(), stepV4.getMessageCondition());
            assertEquals(step.getConfiguration(), new GraviteeMapper().writeValueAsString(stepV4.getConfiguration()));
        }
    }

    @Test
    void shouldMapFromFlowEntityV2() throws JsonProcessingException {
        var flowEntityV2 = FlowFixtures.aModelFlowV2();
        var flowV2 = flowMapper.map(flowEntityV2);
        assertThat(flowV2).isNotNull();
        assertFlowV2Equals(flowEntityV2, flowV2);
    }

    @Test
    void shouldMapToFlowEntityV2() throws JsonProcessingException {
        var flowV2 = FlowFixtures.aFlowV2();
        var flowEntityV2 = flowMapper.map(flowV2);
        assertThat(flowV2).isNotNull();
        assertFlowV2Equals(flowEntityV2, flowV2);
    }

    private void assertFlowV2Equals(FlowV2Impl flowEntityV2, FlowV2 flowV2) throws JsonProcessingException {
        assertEquals(flowEntityV2.getName(), flowV2.getName());
        assertEquals(flowEntityV2.getPath(), flowV2.getPathOperator().getPath());
        assertEquals(flowEntityV2.getOperator().name(), flowV2.getPathOperator().getOperator().name());
        assertEquals(flowEntityV2.getCondition(), flowV2.getCondition());
        assertStepsV2Equals(flowEntityV2.getPre(), flowV2.getPre());
        assertStepsV2Equals(flowEntityV2.getPost(), flowV2.getPost());
    }

    private void assertStepsV2Equals(List<io.gravitee.definition.model.flow.StepV2> steps, List<StepV2> stepsV2)
        throws JsonProcessingException {
        assertEquals(steps.size(), stepsV2.size());

        for (int i = 0; i < steps.size(); i++) {
            final var step = steps.get(i);
            final var stepV2 = stepsV2.get(i);
            assertEquals(step.getName(), stepV2.getName());
            assertEquals(step.getDescription(), stepV2.getDescription());
            assertEquals(step.getPolicy(), stepV2.getPolicy());
            assertEquals(step.getCondition(), stepV2.getCondition());
            assertEquals(step.getConfiguration(), new GraviteeMapper().writeValueAsString(stepV2.getConfiguration()));
        }
    }
}
