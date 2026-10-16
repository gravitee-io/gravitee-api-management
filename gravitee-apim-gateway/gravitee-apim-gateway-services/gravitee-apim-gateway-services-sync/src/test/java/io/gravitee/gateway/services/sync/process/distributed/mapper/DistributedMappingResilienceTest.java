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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncMetrics;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DistributedMappingResilienceTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private final DistributedSyncMetrics metrics = new DistributedSyncMetrics();

    @Nested
    class UndeployPayload {

        @Test
        void should_carry_the_subscription_on_an_undeploy_event() {
            Subscription subscription = new Subscription();
            subscription.setId("sub");
            subscription.setApi("api");

            DistributedEvent event = new SubscriptionMapper(objectMapper, metrics)
                .to(SingleSubscriptionDeployable.builder().subscription(subscription).syncAction(SyncAction.UNDEPLOY).build())
                .blockingFirst();

            assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
            assertThat(event.getPayload()).contains("\"sub\"");
        }

        @Test
        void should_carry_the_api_key_on_an_undeploy_event() {
            ApiKey apiKey = new ApiKey();
            apiKey.setId("key-id");
            apiKey.setKey("secret");

            DistributedEvent event = new ApiKeyMapper(objectMapper, metrics)
                .to(SingleApiKeyDeployable.builder().apiKey(apiKey).syncAction(SyncAction.UNDEPLOY).build())
                .blockingFirst();

            assertThat(event.getPayload()).contains("key-id");
        }

        @Test
        void should_carry_the_dictionary_on_an_undeploy_event() {
            Dictionary dictionary = new Dictionary();
            dictionary.setId("dict");
            dictionary.setEnvironmentId("env");

            DistributedEvent event = new DictionaryMapper(objectMapper, metrics)
                .to(DictionaryDeployable.builder().id("dict").dictionary(dictionary).syncAction(SyncAction.UNDEPLOY).build())
                .blockingGet();

            assertThat(event.getPayload()).contains("\"env\"");
        }

        @Test
        void should_read_a_subscription_undeploy_event_without_payload() {
            DistributedEvent event = DistributedEvent.builder()
                .id("sub")
                .type(DistributedEventType.SUBSCRIPTION)
                .syncAction(DistributedSyncAction.UNDEPLOY)
                .build();

            SingleSubscriptionDeployable deployable = new SubscriptionMapper(objectMapper, metrics).to(event).blockingGet();

            assertThat(deployable.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
            assertThat(deployable.subscription().getId()).isEqualTo("sub");
            assertThat(metrics.decodeFailures()).isZero();
        }
    }

    @Nested
    class Failures {

        @Test
        void should_dead_letter_one_subscription_and_still_distribute_its_siblings() throws JsonProcessingException {
            ObjectMapper failingMapper = mock(ObjectMapper.class);
            when(failingMapper.writeValueAsString(any())).thenReturn("{}");
            when(failingMapper.writeValueAsString(argThat(o -> o instanceof Subscription s && "broken".equals(s.getId())))).thenThrow(
                new JsonProcessingException("boom") {}
            );
            Subscription ok = new Subscription();
            ok.setId("ok");
            Subscription broken = new Subscription();
            broken.setId("broken");

            List<DistributedEvent> events = new SubscriptionMapper(failingMapper, metrics)
                .to(ApiReactorDeployable.builder().apiId("api").syncAction(SyncAction.DEPLOY).subscriptions(List.of(broken, ok)).build())
                .toList()
                .blockingGet();

            assertThat(events).extracting(DistributedEvent::getId).containsExactly("ok");
            assertThat(metrics.deadLetters()).isEqualTo(1);
            assertThat(metrics.deadLettersByType()).containsEntry(DistributedEventType.SUBSCRIPTION, 1L);
        }

        @Test
        void should_dead_letter_an_api_that_cannot_be_serialized_instead_of_failing() throws JsonProcessingException {
            ObjectMapper failingMapper = mock(ObjectMapper.class);
            when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});
            ApiMapper apiMapper = new ApiMapper(
                failingMapper,
                new SubscriptionMapper(failingMapper, metrics),
                new ApiKeyMapper(failingMapper, metrics),
                metrics
            );

            apiMapper
                .to(
                    ApiReactorDeployable.builder()
                        .apiId("api")
                        .syncAction(SyncAction.DEPLOY)
                        .reactableApi(mock(io.gravitee.gateway.reactor.ReactableApi.class))
                        .build()
                )
                .test()
                .assertComplete()
                .assertNoValues();
            assertThat(metrics.deadLettersByType()).containsEntry(DistributedEventType.API, 1L);
        }

        @Test
        void should_count_a_decode_failure_once_per_event_version_for_logging() {
            DistributedEvent event = DistributedEvent.builder()
                .id("sub")
                .type(DistributedEventType.SUBSCRIPTION)
                .syncAction(DistributedSyncAction.DEPLOY)
                .payload("not json")
                .updatedAt(new Date(1000))
                .build();
            SubscriptionMapper mapper = new SubscriptionMapper(objectMapper, metrics);

            mapper.to(event).test().assertComplete().assertNoValues();
            mapper.to(event).test().assertComplete().assertNoValues();

            assertThat(metrics.decodeFailures()).isEqualTo(2);
            assertThat(metrics.recordDecodeFailure(DistributedEventType.SUBSCRIPTION, "sub", new Date(1000))).isFalse();
            assertThat(metrics.recordDecodeFailure(DistributedEventType.SUBSCRIPTION, "sub", new Date(2000))).isTrue();
        }
    }
}
