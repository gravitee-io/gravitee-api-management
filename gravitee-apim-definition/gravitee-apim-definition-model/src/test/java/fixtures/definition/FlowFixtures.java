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
package fixtures.definition;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.flow.FlowStage;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class FlowFixtures {

    private FlowFixtures() {}

    private static final Supplier<Flow.FlowBuilder<?, ?>> BASE_V4 = () -> Flow.builder().name("my-flow");
    private static final Supplier<io.gravitee.definition.model.flow.Flow.FlowBuilder> BASE_V2 = () ->
        io.gravitee.definition.model.flow.Flow
            .builder()
            .id("flow-id")
            .name("my-flow")
            .pathOperator(PathOperator.builder().path("/").operator(Operator.STARTS_WITH).build())
            .methods(Set.of(HttpMethod.GET))
            .consumers(List.of(Consumer.builder().consumerType(ConsumerType.TAG).consumerId("consumer-id").build()))
            .condition("my-condition")
            .stage(FlowStage.API)
            .pre(
                List.of(
                    io.gravitee.definition.model.flow.Step
                        .builder()
                        .name("my-step-name-1")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .post(
                List.of(
                    io.gravitee.definition.model.flow.Step
                        .builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            );

    public static Flow aSimpleFlowV4() {
        return Flow.builder().name("simple-flow").build();
    }

    public static Flow aProxyFlowV4() {
        return BASE_V4
            .get()
            .selectors(List.of(HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).build()))
            .request(
                List.of(
                    Step
                        .builder()
                        .name("my-step-name-1")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .response(
                List.of(
                    Step
                        .builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();
    }

    public static Flow aMessageFlowV4() {
        return BASE_V4
            .get()
            .selectors(List.of(ChannelSelector.builder().channel("/").channelOperator(Operator.STARTS_WITH).build()))
            .subscribe(
                List.of(
                    Step
                        .builder()
                        .name("my-step-name-1")
                        .policy("my-policy")
                        .description("my-step-description")
                        .messageCondition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    Step
                        .builder()
                        .name("my-step-name-2")
                        .policy("my-policy")
                        .description("my-step-description")
                        .messageCondition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();
    }

    public static io.gravitee.definition.model.flow.Flow aFlowV2() {
        return BASE_V2.get().build();
    }
}
