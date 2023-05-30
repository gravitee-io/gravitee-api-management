/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fixtures;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.management.v2.rest.model.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("ALL")
public class FlowFixtures {

    private static final ChannelSelector.ChannelSelectorBuilder BASE_MODEL_CHANNEL_SELECTOR_V4 = ChannelSelector
        .builder()
        .channel("channel")
        .entrypoints(Set.of("entrypoint1", "entrypoint2"))
        .operations(Set.of(ChannelSelector.Operation.SUBSCRIBE, ChannelSelector.Operation.PUBLISH))
        .channelOperator(io.gravitee.definition.model.flow.Operator.EQUALS);

    private static final io.gravitee.rest.api.management.v2.rest.model.ChannelSelector.ChannelSelectorBuilder BASE_CHANNEL_SELECTOR_V4 =
        io.gravitee.rest.api.management.v2.rest.model.ChannelSelector
            .builder()
            .channel("channel")
            .type(BaseSelector.TypeEnum.CHANNEL)
            .entrypoints(Set.of("entrypoint1", "entrypoint2"))
            .operations(
                Set.of(
                    io.gravitee.rest.api.management.v2.rest.model.ChannelSelector.OperationsEnum.SUBSCRIBE,
                    io.gravitee.rest.api.management.v2.rest.model.ChannelSelector.OperationsEnum.PUBLISH
                )
            )
            .channelOperator(io.gravitee.rest.api.management.v2.rest.model.Operator.EQUALS);

    private static final Step.StepBuilder BASE_MODEL_STEP_V4 = Step
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .messageCondition("{#context.attribute['messageCondition'] == true}")
        .configuration("{\n  \"nice\" : \"config\"\n}");

    private static final StepV4.StepV4Builder BASE_STEP_V4 = StepV4
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .messageCondition("{#context.attribute['messageCondition'] == true}")
        .configuration(new LinkedHashMap<>(Map.of("nice", "config")));

    private static final Flow.FlowBuilder BASE_MODEL_FLOW_V4 = Flow
        .builder()
        .name("Flow")
        .enabled(true)
        .selectors(List.of(BASE_MODEL_CHANNEL_SELECTOR_V4.build()))
        .request(List.of(BASE_MODEL_STEP_V4.name("step_request").build()))
        .publish(List.of(BASE_MODEL_STEP_V4.name("step_publish").build()))
        .response(List.of(BASE_MODEL_STEP_V4.name("step_response").build()))
        .subscribe(List.of(BASE_MODEL_STEP_V4.name("step_subscribe").build()));

    private static final FlowV4.FlowV4Builder BASE_FLOW_V4 = FlowV4
        .builder()
        .name("Flow")
        .enabled(true)
        .selectors(List.of(new Selector(BASE_CHANNEL_SELECTOR_V4.build())))
        .request(List.of(BASE_STEP_V4.name("step_request").build()))
        .publish(List.of(BASE_STEP_V4.name("step_publish").build()))
        .response(List.of(BASE_STEP_V4.name("step_response").build()))
        .subscribe(List.of(BASE_STEP_V4.name("step_subscribe").build()))
        .tags(Set.of("tag1", "tag2"));

    private static final io.gravitee.definition.model.flow.Step.StepBuilder BASE_MODEL_STEP_V2 = io.gravitee.definition.model.flow.Step
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .configuration("{\n  \"nice\" : \"config\"\n}");

    private static final StepV2.StepV2Builder BASE_STEP_V2 = StepV2
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .configuration(new LinkedHashMap<>(Map.of("nice", "config")));

    private static final io.gravitee.definition.model.flow.Flow.FlowBuilder BASE_MODEL_FLOW_V2 = io.gravitee.definition.model.flow.Flow
        .builder()
        .name("Flow")
        .enabled(true)
        .pathOperator(
            io.gravitee.definition.model.flow.PathOperator
                .builder()
                .operator(io.gravitee.definition.model.flow.Operator.EQUALS)
                .path("/path")
                .build()
        )
        .condition("{#context.attribute['condition'] == true}")
        .pre(List.of(BASE_MODEL_STEP_V2.name("step_pre").build()))
        .post(List.of(BASE_MODEL_STEP_V2.name("step_pot").build()));

    private static final FlowV2.FlowV2Builder BASE_FLOW_V2 = FlowV2
        .builder()
        .name("Flow")
        .enabled(true)
        .pathOperator(PathOperator.builder().operator(Operator.EQUALS).path("/path").build())
        .condition("{#context.attribute['condition'] == true}")
        .pre(List.of(BASE_STEP_V2.name("step_pre").build()))
        .post(List.of(BASE_STEP_V2.name("step_pot").build()));

    public static Flow aModelFlowV4() {
        return BASE_MODEL_FLOW_V4.build();
    }

    public static FlowV4 aFlowV4() {
        return BASE_FLOW_V4.build();
    }

    public static io.gravitee.definition.model.flow.Flow aModelFlowV2() {
        return BASE_MODEL_FLOW_V2.build();
    }

    public static FlowV2 aFlowV2() {
        return BASE_FLOW_V2.build();
    }
}
