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
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.rest.api.management.v2.rest.model.BaseSelector;
import io.gravitee.rest.api.management.v2.rest.model.FlowV2;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import io.gravitee.rest.api.management.v2.rest.model.Operator;
import io.gravitee.rest.api.management.v2.rest.model.PathOperator;
import io.gravitee.rest.api.management.v2.rest.model.Selector;
import io.gravitee.rest.api.management.v2.rest.model.StepV2;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
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

    private FlowFixtures() {}

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

    private static final StepV4.StepV4Builder BASE_STEP_V4 = StepV4
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .messageCondition("{#context.attribute['messageCondition'] == true}")
        .configuration(new LinkedHashMap<>(Map.of("nice", "config")));

    private static final FlowV4.FlowV4Builder BASE_FLOW_HTTP_V4 = FlowV4
        .builder()
        .name("Flow")
        .enabled(true)
        .selectors(List.of(new Selector(BASE_CHANNEL_SELECTOR_V4.build())))
        .request(List.of(BASE_STEP_V4.name("step_request").build()))
        .publish(List.of(BASE_STEP_V4.name("step_publish").build()))
        .response(List.of(BASE_STEP_V4.name("step_response").build()))
        .subscribe(List.of(BASE_STEP_V4.name("step_subscribe").build()))
        .tags(Set.of("tag1", "tag2"));

    private static final FlowV4.FlowV4Builder BASE_FLOW_NATIVE_V4 = FlowV4
        .builder()
        .name("Flow")
        .enabled(true)
        .connect(List.of(BASE_STEP_V4.name("step_connect").build()))
        .publish(List.of(BASE_STEP_V4.name("step_publish").build()))
        .interact(List.of(BASE_STEP_V4.name("step_interact").build()))
        .subscribe(List.of(BASE_STEP_V4.name("step_subscribe").build()))
        .tags(Set.of("tag1", "tag2"));

    private static final StepV2.StepV2Builder BASE_STEP_V2 = StepV2
        .builder()
        .name("step")
        .description("description")
        .enabled(true)
        .policy("policy")
        .condition("{#context.attribute['condition'] == true}")
        .configuration(new LinkedHashMap<>(Map.of("nice", "config")));

    private static final FlowV2.FlowV2Builder BASE_FLOW_V2 = FlowV2
        .builder()
        .name("Flow")
        .enabled(true)
        .pathOperator(PathOperator.builder().operator(Operator.EQUALS).path("/path").build())
        .condition("{#context.attribute['condition'] == true}")
        .pre(List.of(BASE_STEP_V2.name("step_pre").build()))
        .post(List.of(BASE_STEP_V2.name("step_pot").build()));

    public static FlowV4 aFlowHttpV4() {
        return BASE_FLOW_HTTP_V4.build();
    }

    public static FlowV4 aFlowNativeV4() {
        return BASE_FLOW_NATIVE_V4.build();
    }

    public static FlowV2 aFlowV2() {
        return BASE_FLOW_V2.build();
    }

    public static Flow aModelFlowHttpV4() {
        return FlowModelFixtures.aModelFlowHttpV4();
    }

    public static NativeFlow aModelFlowNativeV4() {
        return FlowModelFixtures.aModelFlowNativeV4();
    }

    public static io.gravitee.definition.model.flow.Flow aModelFlowV2() {
        return FlowModelFixtures.aModelFlowV2();
    }
}
