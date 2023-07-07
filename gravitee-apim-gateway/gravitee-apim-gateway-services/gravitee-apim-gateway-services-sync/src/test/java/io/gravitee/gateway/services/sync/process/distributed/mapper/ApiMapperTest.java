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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private ApiMapper cut;
    private DistributedEvent distributedEvent;
    private ApiReactorDeployable apiReactorDeployable;
    private Api api;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new ApiMapper(objectMapper, new SubscriptionMapper(objectMapper), new ApiKeyMapper(objectMapper));

        io.gravitee.definition.model.v4.Api apiDef = new io.gravitee.definition.model.v4.Api();
        apiDef.setId("apiId");
        api = new io.gravitee.gateway.reactive.handlers.api.v4.Api(apiDef);

        distributedEvent =
            DistributedEvent
                .builder()
                .id("apiId")
                .payload(objectMapper.writeValueAsString(api))
                .updatedAt(new Date())
                .type(DistributedEventType.API)
                .syncAction(DistributedSyncAction.DEPLOY)
                .build();

        apiReactorDeployable = ApiReactorDeployable.builder().apiId("apiId").reactableApi(api).syncAction(SyncAction.DEPLOY).build();
    }

    @Test
    void should_return_distributed_event() {
        cut
            .to(distributedEvent)
            .test()
            .assertValue(apiReactorDeployable -> {
                assertThat(apiReactorDeployable).isEqualTo(this.apiReactorDeployable);
                return true;
            });
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertComplete();
    }

    @Test
    void should_map_api_reactor_deployable() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId("apiKey");
        Subscription subscription = new Subscription();
        subscription.setId("subscription");
        apiReactorDeployable =
            ApiReactorDeployable
                .builder()
                .apiId("apiId")
                .reactableApi(api)
                .syncAction(SyncAction.DEPLOY)
                .apiKeys(List.of(apiKey))
                .subscriptions(List.of(subscription))
                .build();
        cut
            .to(apiReactorDeployable)
            .test()
            .assertValueCount(3)
            .assertValueAt(
                0,
                distributedEvent -> {
                    assertThat(distributedEvent.getId()).isEqualTo("apiId");
                    assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API);
                    assertThat(distributedEvent.getPayload()).isEqualTo(objectMapper.writeValueAsString(api));
                    assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                    return true;
                }
            )
            .assertValueAt(
                1,
                distributedEvent -> {
                    assertThat(distributedEvent.getId()).isEqualTo("subscription");
                    assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.SUBSCRIPTION);
                    assertThat(distributedEvent.getPayload()).isEqualTo(objectMapper.writeValueAsString(subscription));
                    assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                    return true;
                }
            )
            .assertValueAt(
                2,
                distributedEvent -> {
                    assertThat(distributedEvent.getId()).isEqualTo("apiKey");
                    assertThat(distributedEvent.getType()).isEqualTo(DistributedEventType.API_KEY);
                    assertThat(distributedEvent.getPayload()).isEqualTo(objectMapper.writeValueAsString(apiKey));
                    assertThat(distributedEvent.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
                    return true;
                }
            );
    }
}
