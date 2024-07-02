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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnvironmentFlowMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    private EnvironmentFlowMapper cut;

    @BeforeEach
    void setUp() {
        cut = new EnvironmentFlowMapper(objectMapper);
    }

    @SneakyThrows
    @Test
    void should_return_distributed_environment_flow_event() {
        ReactableEnvironmentFlow reactableEnvironmentFlow = new ReactableEnvironmentFlow();
        reactableEnvironmentFlow.setId("env-flow-id");
        reactableEnvironmentFlow.setName("env-flow-name");
        final DistributedEvent distributedEvent = DistributedEvent
            .builder()
            .id("env-flow-id")
            .payload(objectMapper.writeValueAsString(reactableEnvironmentFlow))
            .updatedAt(new Date())
            .type(DistributedEventType.ENVIRONMENT_FLOW)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(result -> {
                assertThat(result.environmentFlowId()).isEqualTo("env-flow-id");
                assertThat(result.reactableEnvironmentFlow())
                    .satisfies(reactable -> {
                        assertThat(reactable.getId()).isEqualTo("env-flow-id");
                        assertThat(reactable.getName()).isEqualTo("env-flow-name");
                    });
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertNoValues().assertComplete();
    }
}
