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
package fixtures;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.List;
import java.util.Set;

public class FlowModelFixtures {

    private FlowModelFixtures() {}

    private static final ChannelSelector.ChannelSelectorBuilder BASE_MODEL_CHANNEL_SELECTOR_V4 = ChannelSelector
        .builder()
        .channel("channel")
        .entrypoints(Set.of("entrypoint1", "entrypoint2"))
        .operations(Set.of(ChannelSelector.Operation.SUBSCRIBE, ChannelSelector.Operation.PUBLISH))
        .channelOperator(io.gravitee.definition.model.flow.Operator.EQUALS);

    private static final Step.StepBuilder BASE_MODEL_STEP_V4 = Step
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .messageCondition("{#context.attribute['messageCondition'] == true}")
        .configuration("{\n  \"nice\" : \"config\"\n}");

    private static final Flow.FlowBuilder BASE_MODEL_FLOW_HTTP_V4 = Flow
        .builder()
        .name("Flow")
        .enabled(true)
        .selectors(List.of(BASE_MODEL_CHANNEL_SELECTOR_V4.build()))
        .request(List.of(BASE_MODEL_STEP_V4.name("step_request").build()))
        .publish(List.of(BASE_MODEL_STEP_V4.name("step_publish").build()))
        .response(List.of(BASE_MODEL_STEP_V4.name("step_response").build()))
        .subscribe(List.of(BASE_MODEL_STEP_V4.name("step_subscribe").build()));

    private static final NativeFlow.NativeFlowBuilder BASE_MODEL_FLOW_NATIVE_V4 = NativeFlow
        .builder()
        .name("Flow")
        .enabled(true)
        .connect(List.of(BASE_MODEL_STEP_V4.name("step_connect").build()))
        .publish(List.of(BASE_MODEL_STEP_V4.name("step_publish").build()))
        .interact(List.of(BASE_MODEL_STEP_V4.name("step_interact").build()))
        .subscribe(List.of(BASE_MODEL_STEP_V4.name("step_subscribe").build()));

    private static final io.gravitee.definition.model.flow.Step.StepBuilder BASE_MODEL_STEP_V2 = io.gravitee.definition.model.flow.Step
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .configuration("{\n  \"nice\" : \"config\"\n}");

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

    public static Flow aModelFlowHttpV4() {
        return BASE_MODEL_FLOW_HTTP_V4.build();
    }

    public static NativeFlow aModelFlowNativeV4() {
        return BASE_MODEL_FLOW_NATIVE_V4.build();
    }

    public static io.gravitee.definition.model.flow.Flow aModelFlowV2() {
        return BASE_MODEL_FLOW_V2.build();
    }
}
