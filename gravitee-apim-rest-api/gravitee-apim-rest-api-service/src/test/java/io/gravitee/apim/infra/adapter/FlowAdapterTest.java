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
package io.gravitee.apim.infra.adapter;

import fixtures.definition.FlowFixtures;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.flow.Consumer;
import io.gravitee.definition.model.flow.ConsumerType;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.McpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowConsumer;
import io.gravitee.repository.management.model.flow.FlowConsumerType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowChannelSelector;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowMcpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.rest.api.service.common.UuidString;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FlowAdapterTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_convert_from_v4_flow_to_repository() {
        var model = FlowFixtures.aProxyFlowV4()
            .toBuilder()
            .tags(Set.of("tag1"))
            .selectors(
                List.of(
                    HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).methods(Set.of(HttpMethod.GET)).build(),
                    ChannelSelector.builder()
                        .channel("/")
                        .channelOperator(Operator.STARTS_WITH)
                        .entrypoints(Set.of("sse"))
                        .operations(Set.of(ChannelSelector.Operation.PUBLISH))
                        .build(),
                    ConditionSelector.builder().condition("my-condition").build(),
                    McpSelector.builder().methods(Set.of("mcp")).build()
                )
            )
            .request(
                List.of(
                    Step.builder()
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
                    Step.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    Step.builder()
                        .name("my-step-name-3")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .subscribe(
                List.of(
                    Step.builder()
                        .name("my-step-name-4")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();

        var result = FlowAdapter.INSTANCE.toRepository(model, FlowReferenceType.API, "api-id", 12);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getId()).isEqualTo("generated-id");
            soft.assertThat(result.getReferenceType()).isEqualTo(FlowReferenceType.API);
            soft.assertThat(result.getReferenceId()).isEqualTo("api-id");
            soft.assertThat(result.getName()).isEqualTo("my-flow");
            soft.assertThat(result.isEnabled()).isTrue();
            soft.assertThat(result.getCreatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(result.getUpdatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(result.getOrder()).isEqualTo(12);
            soft.assertThat(result.getTags()).containsExactly("tag1");
            soft
                .assertThat(result.getSelectors())
                .hasSize(4)
                .containsOnly(
                    FlowHttpSelector.builder().path("/").pathOperator(FlowOperator.STARTS_WITH).methods(Set.of(HttpMethod.GET)).build(),
                    FlowChannelSelector.builder()
                        .channel("/")
                        .channelOperator(FlowOperator.STARTS_WITH)
                        .entrypoints(Set.of("sse"))
                        .operations(Set.of(FlowChannelSelector.Operation.PUBLISH))
                        .build(),
                    FlowConditionSelector.builder().condition("my-condition").build(),
                    FlowMcpSelector.builder().methods(Set.of("mcp")).build()
                );
            soft
                .assertThat(result.getRequest())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-1")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getResponse())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getPublish())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-3")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getSubscribe())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-4")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
        });
    }

    @Test
    void should_update_repository_with_v4_flow_to_repository() {
        var model = FlowFixtures.aProxyFlowV4()
            .toBuilder()
            .tags(Set.of("tag1"))
            .selectors(
                List.of(
                    HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).methods(Set.of(HttpMethod.GET)).build(),
                    ChannelSelector.builder()
                        .channel("/")
                        .channelOperator(Operator.STARTS_WITH)
                        .entrypoints(Set.of("sse"))
                        .operations(Set.of(ChannelSelector.Operation.PUBLISH))
                        .build(),
                    ConditionSelector.builder().condition("my-condition").build(),
                    McpSelector.builder().methods(Set.of("mcp")).build()
                )
            )
            .request(
                List.of(
                    Step.builder()
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
                    Step.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    Step.builder()
                        .name("my-step-name-3")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .subscribe(
                List.of(
                    Step.builder()
                        .name("my-step-name-4")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();

        var createdResult = FlowAdapter.INSTANCE.toRepository(model, FlowReferenceType.API, "api-id", 12);

        var updatedModel = model
            .toBuilder()
            .name("updated-name")
            .tags(Set.of("updated-tag1"))
            .enabled(false)
            .selectors(null)
            .request(List.of())
            .response(
                List.of(
                    Step.builder()
                        .name("my-updated-step-name-2")
                        .policy("a-updated-policy")
                        .description("my-updated-step-description")
                        .condition("my-updated-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    Step.builder()
                        .name("my-updated-step-name-3")
                        .policy("a-updated-policy")
                        .description("my-updated-step-description")
                        .condition("my-updated-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .subscribe(List.of())
            .build();
        var updatedResult = FlowAdapter.INSTANCE.toRepositoryUpdate(createdResult, updatedModel, 22);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(updatedResult.getId()).isEqualTo("generated-id");
            soft.assertThat(updatedResult.getReferenceType()).isEqualTo(FlowReferenceType.API);
            soft.assertThat(updatedResult.getReferenceId()).isEqualTo("api-id");
            soft.assertThat(updatedResult.getName()).isEqualTo("updated-name");
            soft.assertThat(updatedResult.isEnabled()).isFalse();
            soft.assertThat(updatedResult.getCreatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(updatedResult.getUpdatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(updatedResult.getOrder()).isEqualTo(22);
            soft.assertThat(updatedResult.getTags()).containsExactly("updated-tag1");
            soft.assertThat(updatedResult.getSelectors()).isNull();
            soft.assertThat(updatedResult.getRequest()).isEmpty();
            soft
                .assertThat(updatedResult.getResponse())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-updated-step-name-2")
                        .policy("a-updated-policy")
                        .description("my-updated-step-description")
                        .condition("my-updated-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(updatedResult.getPublish())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-updated-step-name-3")
                        .policy("a-updated-policy")
                        .description("my-updated-step-description")
                        .condition("my-updated-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft.assertThat(updatedResult.getSubscribe()).isEmpty();
        });
    }

    @Test
    void should_convert_from_v2_flow_to_repository() {
        var model = FlowFixtures.aFlowV2();

        var result = FlowAdapter.INSTANCE.toRepository(model, FlowReferenceType.API, "api-id", 12);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getId()).isEqualTo("generated-id");
            soft.assertThat(result.getReferenceType()).isEqualTo(FlowReferenceType.API);
            soft.assertThat(result.getReferenceId()).isEqualTo("api-id");
            soft.assertThat(result.getName()).isEqualTo("my-flow");
            soft.assertThat(result.isEnabled()).isTrue();
            soft.assertThat(result.getCreatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(result.getUpdatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(result.getOrder()).isEqualTo(12);
            soft.assertThat(result.getTags()).isNull();
            soft.assertThat(result.getPath()).isEqualTo("/");
            soft.assertThat(result.getOperator()).isEqualTo(FlowOperator.STARTS_WITH);
            soft.assertThat(result.getCondition()).isEqualTo("my-condition");
            soft.assertThat(result.getMethods()).containsExactly(HttpMethod.GET);
            soft.assertThat(result.getConsumers()).containsExactly(new FlowConsumer(FlowConsumerType.TAG, "consumer-id"));
            soft
                .assertThat(result.getPre())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-1")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getPost())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
        });
    }

    @Test
    void should_convert_from_repository_to_flow_v4() {
        var repository = Flow.builder()
            .name("my-flow")
            .enabled(true)
            .tags(Set.of("tag1"))
            .selectors(
                List.of(
                    FlowHttpSelector.builder().path("/").pathOperator(FlowOperator.STARTS_WITH).methods(Set.of(HttpMethod.GET)).build(),
                    FlowChannelSelector.builder()
                        .channel("/")
                        .channelOperator(FlowOperator.STARTS_WITH)
                        .entrypoints(Set.of("sse"))
                        .operations(Set.of(FlowChannelSelector.Operation.PUBLISH))
                        .build(),
                    FlowConditionSelector.builder().condition("my-condition").build(),
                    FlowMcpSelector.builder().methods(Set.of("mcp")).build()
                )
            )
            .request(
                List.of(
                    FlowStep.builder()
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
                    FlowStep.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-3")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .subscribe(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-4")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();

        var result = FlowAdapter.INSTANCE.toFlowV4(repository);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getName()).isEqualTo("my-flow");
            soft.assertThat(result.isEnabled()).isTrue();
            soft.assertThat(result.getTags()).containsExactly("tag1");
            soft
                .assertThat(result.getSelectors())
                .hasSize(4)
                .containsOnly(
                    HttpSelector.builder().path("/").pathOperator(Operator.STARTS_WITH).methods(Set.of(HttpMethod.GET)).build(),
                    ChannelSelector.builder()
                        .channel("/")
                        .channelOperator(Operator.STARTS_WITH)
                        .entrypoints(Set.of("sse"))
                        .operations(Set.of(ChannelSelector.Operation.PUBLISH))
                        .build(),
                    ConditionSelector.builder().condition("my-condition").build(),
                    McpSelector.builder().methods(Set.of("mcp")).build()
                );
            soft
                .assertThat(result.getRequest())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-1")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getResponse())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getPublish())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-3")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getSubscribe())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-4")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
        });
    }

    @Test
    void should_convert_from_repository_to_flow_v2() {
        var repository = Flow.builder()
            .name("my-flow")
            .enabled(true)
            .tags(Set.of("tag1"))
            .condition("my-condition")
            .path("/")
            .operator(FlowOperator.STARTS_WITH)
            .consumers(List.of(new FlowConsumer(FlowConsumerType.TAG, "consumer-id")))
            .methods(Set.of(HttpMethod.GET))
            .pre(
                List.of(
                    FlowStep.builder()
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
                    FlowStep.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-3")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .subscribe(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-4")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();

        var result = FlowAdapter.INSTANCE.toFlowV2(repository);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getName()).isEqualTo("my-flow");
            soft.assertThat(result.isEnabled()).isTrue();
            soft.assertThat(result.getPathOperator()).isEqualTo(PathOperator.builder().path("/").operator(Operator.STARTS_WITH).build());
            soft.assertThat(result.getPath()).isEqualTo("/");
            soft.assertThat(result.getOperator()).isEqualTo(Operator.STARTS_WITH);
            soft.assertThat(result.getCondition()).isEqualTo("my-condition");
            soft.assertThat(result.getMethods()).containsExactly(HttpMethod.GET);
            soft.assertThat(result.getConsumers()).containsExactly(new Consumer(ConsumerType.TAG, "consumer-id"));
            soft
                .assertThat(result.getPre())
                .containsOnly(
                    io.gravitee.definition.model.flow.Step.builder()
                        .name("my-step-name-1")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getPost())
                .containsOnly(
                    io.gravitee.definition.model.flow.Step.builder()
                        .name("my-step-name-2")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
        });
    }

    @Test
    void should_convert_from_native_flow_to_repository() {
        var model = io.gravitee.definition.model.v4.nativeapi.NativeFlow.builder()
            .name("my-native-flow")
            .enabled(true)
            .tags(Set.of("tag1"))
            .entrypointConnect(
                List.of(
                    Step.builder()
                        .name("my-step-name-entrypoint-connect")
                        .policy("ip-filtering")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{\"blacklistIps\":[\"127.0.0.1\"]}")
                        .build()
                )
            )
            .interact(
                List.of(
                    Step.builder()
                        .name("my-step-name-interact")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    Step.builder()
                        .name("my-step-name-publish")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .subscribe(
                List.of(
                    Step.builder()
                        .name("my-step-name-subscribe")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();

        var result = FlowAdapter.INSTANCE.toRepository(model, FlowReferenceType.API, "api-id", 12);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getId()).isEqualTo("generated-id");
            soft.assertThat(result.getReferenceType()).isEqualTo(FlowReferenceType.API);
            soft.assertThat(result.getReferenceId()).isEqualTo("api-id");
            soft.assertThat(result.getName()).isEqualTo("my-native-flow");
            soft.assertThat(result.isEnabled()).isTrue();
            soft.assertThat(result.getCreatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(result.getUpdatedAt()).isEqualTo(Date.from(INSTANT_NOW));
            soft.assertThat(result.getOrder()).isEqualTo(12);
            soft.assertThat(result.getTags()).containsExactly("tag1");
            soft
                .assertThat(result.getEntrypointConnect())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-entrypoint-connect")
                        .policy("ip-filtering")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{\"blacklistIps\":[\"127.0.0.1\"]}")
                        .build()
                );
            soft
                .assertThat(result.getInteract())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-interact")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getPublish())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-publish")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getSubscribe())
                .containsOnly(
                    FlowStep.builder()
                        .name("my-step-name-subscribe")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
        });
    }

    @Test
    void should_convert_from_repository_to_native_flow() {
        var repository = Flow.builder()
            .name("my-native-flow")
            .enabled(true)
            .tags(Set.of("tag1"))
            .entrypointConnect(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-entrypoint-connect")
                        .policy("ip-filtering")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{\"blacklistIps\":[\"127.0.0.1\"]}")
                        .build()
                )
            )
            .interact(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-interact")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .publish(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-publish")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .subscribe(
                List.of(
                    FlowStep.builder()
                        .name("my-step-name-subscribe")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                )
            )
            .build();

        var result = FlowAdapter.INSTANCE.toNativeFlow(repository);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getName()).isEqualTo("my-native-flow");
            soft.assertThat(result.isEnabled()).isTrue();
            soft.assertThat(result.getTags()).containsExactly("tag1");
            soft
                .assertThat(result.getEntrypointConnect())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-entrypoint-connect")
                        .policy("ip-filtering")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{\"blacklistIps\":[\"127.0.0.1\"]}")
                        .build()
                );
            soft
                .assertThat(result.getInteract())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-interact")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getPublish())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-publish")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
            soft
                .assertThat(result.getSubscribe())
                .containsOnly(
                    Step.builder()
                        .name("my-step-name-subscribe")
                        .policy("a-policy")
                        .description("my-step-description")
                        .condition("my-step-condition")
                        .configuration("{}")
                        .build()
                );
        });
    }
}
