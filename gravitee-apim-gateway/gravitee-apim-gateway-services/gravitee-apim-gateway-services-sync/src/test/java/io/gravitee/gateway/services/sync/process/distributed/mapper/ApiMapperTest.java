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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.NativeApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private ApiMapper cut;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new ApiMapper(objectMapper, new SubscriptionMapper(objectMapper), new ApiKeyMapper(objectMapper));
    }

    @SneakyThrows
    @Test
    void should_return_distributed_event_for_v2_api() {
        io.gravitee.definition.model.Api apiDef = new io.gravitee.definition.model.Api();
        apiDef.setId("apiId");
        apiDef.setDefinitionVersion(DefinitionVersion.V2);
        apiDef.setProxy(Proxy.builder().virtualHosts(List.of(new VirtualHost("/path"))).build());
        io.gravitee.gateway.handlers.api.definition.Api api = new io.gravitee.gateway.handlers.api.definition.Api(apiDef);

        DistributedEvent distributedEvent = DistributedEvent
            .builder()
            .id("apiId")
            .payload(objectMapper.writeValueAsString(api))
            .updatedAt(new Date())
            .type(DistributedEventType.API)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable
            .builder()
            .apiId("apiId")
            .reactableApi(api)
            .syncAction(SyncAction.DEPLOY)
            .build();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(reactorDeployable -> {
                assertThat(reactorDeployable).isEqualTo(apiReactorDeployable);
                return true;
            });
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(value = ApiType.class, names = { "PROXY", "MESSAGE" })
    void should_return_distributed_event_for_v4_api(ApiType apiType) {
        io.gravitee.definition.model.v4.Api apiDef = new io.gravitee.definition.model.v4.Api();
        apiDef.setId("apiId");
        apiDef.setDefinitionVersion(DefinitionVersion.V4);
        apiDef.setType(apiType);
        Api api = new io.gravitee.gateway.reactive.handlers.api.v4.Api(apiDef);

        DistributedEvent distributedEvent = DistributedEvent
            .builder()
            .id("apiId")
            .payload(objectMapper.writeValueAsString(api))
            .updatedAt(new Date())
            .type(DistributedEventType.API)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable
            .builder()
            .apiId("apiId")
            .reactableApi(api)
            .syncAction(SyncAction.DEPLOY)
            .build();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(reactorDeployable -> {
                assertThat(reactorDeployable).isEqualTo(apiReactorDeployable);
                return true;
            });
    }

    @SneakyThrows
    @Test
    void should_return_distributed_event_for_v4_native_api() {
        io.gravitee.definition.model.v4.nativeapi.NativeApi apiDef = new io.gravitee.definition.model.v4.nativeapi.NativeApi();
        apiDef.setId("apiId");
        apiDef.setDefinitionVersion(DefinitionVersion.V4);
        apiDef.setType(ApiType.NATIVE);
        NativeApi api = new NativeApi(apiDef);

        DistributedEvent distributedEvent = DistributedEvent
            .builder()
            .id("apiId")
            .payload(objectMapper.writeValueAsString(api))
            .updatedAt(new Date())
            .type(DistributedEventType.API)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable
            .builder()
            .apiId("apiId")
            .reactableApi(api)
            .syncAction(SyncAction.DEPLOY)
            .build();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(reactorDeployable -> {
                assertThat(reactorDeployable).isEqualTo(apiReactorDeployable);
                return true;
            });
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertComplete();
    }

    @Test
    void should_map_api_reactor_deployable() {
        io.gravitee.definition.model.v4.Api apiDef = new io.gravitee.definition.model.v4.Api();
        apiDef.setId("apiId");
        Api api = new io.gravitee.gateway.reactive.handlers.api.v4.Api(apiDef);

        ApiKey apiKey = new ApiKey();
        apiKey.setId("apiKey");
        Subscription subscription = new Subscription();
        subscription.setId("subscription");
        ApiReactorDeployable apiReactorDeployable = ApiReactorDeployable
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
