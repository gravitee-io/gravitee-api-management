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
import io.gravitee.rest.api.management.v2.rest.model.ChannelSelector;
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
import java.util.function.Supplier;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowFixtures {

    private FlowFixtures() {}

    private static final Supplier<ChannelSelector> BASE_CHANNEL_SELECTOR_V4 = () ->
        (io.gravitee.rest.api.management.v2.rest.model.ChannelSelector) new io.gravitee.rest.api.management.v2.rest.model.ChannelSelector()
            .entrypoints(Set.of("entrypoint1", "entrypoint2"))
            .operations(
                Set.of(
                    io.gravitee.rest.api.management.v2.rest.model.ChannelSelector.OperationsEnum.SUBSCRIBE,
                    io.gravitee.rest.api.management.v2.rest.model.ChannelSelector.OperationsEnum.PUBLISH
                )
            )
            .channelOperator(io.gravitee.rest.api.management.v2.rest.model.Operator.EQUALS)
            .channel("channel")
            .type(BaseSelector.TypeEnum.CHANNEL);

    private static final Supplier<StepV4> BASE_STEP_V4 = () ->
        new StepV4()
            .name("step")
            .description("description")
            .enabled(true)
            .policy("policy")
            .condition("{#context.attribute['condition'] == true}")
            .messageCondition("{#context.attribute['messageCondition'] == true}")
            .configuration(new LinkedHashMap<>(Map.of("nice", "config")));

    private static final Supplier<FlowV4> BASE_FLOW_HTTP_V4 = () ->
        new FlowV4()
            .name("Flow")
            .enabled(true)
            .selectors(List.of(new Selector(BASE_CHANNEL_SELECTOR_V4.get())))
            .request(List.of(BASE_STEP_V4.get().name("step_request")))
            .publish(List.of(BASE_STEP_V4.get().name("step_publish")))
            .response(List.of(BASE_STEP_V4.get().name("step_response")))
            .subscribe(List.of(BASE_STEP_V4.get().name("step_subscribe")))
            .tags(Set.of("tag1", "tag2"));

    private static final Supplier<FlowV4> BASE_FLOW_NATIVE_V4 = () ->
        new FlowV4()
            .name("Flow")
            .enabled(true)
            .entrypointConnect(List.of(BASE_STEP_V4.get().name("step_entrypoint_connect")))
            .publish(List.of(BASE_STEP_V4.get().name("step_publish")))
            .interact(List.of(BASE_STEP_V4.get().name("step_interact")))
            .subscribe(List.of(BASE_STEP_V4.get().name("step_subscribe")))
            .tags(Set.of("tag1", "tag2"));

    private static final Supplier<StepV2> BASE_STEP_V2 = () ->
        new StepV2()
            .name("step")
            .description("description")
            .enabled(true)
            .policy("policy")
            .condition("{#context.attribute['condition'] == true}")
            .configuration(new LinkedHashMap<>(Map.of("nice", "config")));

    private static final Supplier<FlowV2> BASE_FLOW_V2 = () ->
        new FlowV2()
            .name("Flow")
            .enabled(true)
            .pathOperator(new PathOperator().operator(Operator.EQUALS).path("/path"))
            .condition("{#context.attribute['condition'] == true}")
            .pre(List.of(BASE_STEP_V2.get().name("step_pre")))
            .post(List.of(BASE_STEP_V2.get().name("step_pot")));

    public static FlowV4 aFlowHttpV4() {
        return BASE_FLOW_HTTP_V4.get();
    }

    public static FlowV4 aFlowNativeV4() {
        return BASE_FLOW_NATIVE_V4.get();
    }

    public static FlowV2 aFlowV2() {
        return BASE_FLOW_V2.get();
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
