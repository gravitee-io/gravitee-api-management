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

import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.flow.StepV2;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.step.StepV4;
import io.gravitee.definition.model.v4.nativeapi.NativeFlowImpl;
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

    private static final StepV4.StepV4Builder BASE_MODEL_STEP_V4 = StepV4
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .messageCondition("{#context.attribute['messageCondition'] == true}")
        .configuration("{\n  \"nice\" : \"config\"\n}");

    private static final FlowV4Impl.FlowV4ImplBuilder BASE_MODEL_FLOW_HTTP_V4 = FlowV4Impl
        .builder()
        .name("Flow")
        .enabled(true)
        .selectors(List.of(BASE_MODEL_CHANNEL_SELECTOR_V4.build()))
        .request(List.of(BASE_MODEL_STEP_V4.name("step_request").build()))
        .publish(List.of(BASE_MODEL_STEP_V4.name("step_publish").build()))
        .response(List.of(BASE_MODEL_STEP_V4.name("step_response").build()))
        .subscribe(List.of(BASE_MODEL_STEP_V4.name("step_subscribe").build()));

    private static final NativeFlowImpl.NativeFlowBuilder BASE_MODEL_FLOW_NATIVE_V4 = NativeFlowImpl
        .builder()
        .name("Flow")
        .enabled(true)
        .connect(List.of(BASE_MODEL_STEP_V4.name("step_connect").build()))
        .publish(List.of(BASE_MODEL_STEP_V4.name("step_publish").build()))
        .interact(List.of(BASE_MODEL_STEP_V4.name("step_interact").build()))
        .subscribe(List.of(BASE_MODEL_STEP_V4.name("step_subscribe").build()));

    private static final StepV2.StepV2Builder BASE_MODEL_STEP_V2 = StepV2
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .configuration("{\n  \"nice\" : \"config\"\n}");

    private static final FlowV2Impl.FlowV2ImplBuilder BASE_MODEL_FLOW_V2 = FlowV2Impl
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

    public static FlowV4Impl aModelFlowHttpV4() {
        return BASE_MODEL_FLOW_HTTP_V4.build();
    }

    public static NativeFlowImpl aModelFlowNativeV4() {
        return BASE_MODEL_FLOW_NATIVE_V4.build();
    }

    public static FlowV2Impl aModelFlowV2() {
        return BASE_MODEL_FLOW_V2.build();
    }
}
