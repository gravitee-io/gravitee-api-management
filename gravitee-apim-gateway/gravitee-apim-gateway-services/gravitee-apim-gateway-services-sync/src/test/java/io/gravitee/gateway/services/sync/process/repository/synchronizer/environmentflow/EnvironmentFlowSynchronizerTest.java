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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.environmentflow;

import static io.gravitee.repository.management.model.Event.EventProperties.ENVIRONMENT_FLOW_ID;
import static io.gravitee.repository.management.model.EventType.DEPLOY_ENVIRONMENT_FLOW;
import static io.gravitee.repository.management.model.EventType.UNDEPLOY_ENVIRONMENT_FLOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.EnvironmentFlowDeployer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.EnvironmentFlowMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnvironmentFlowSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private LatestEventFetcher latestEventFetcher;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private EnvironmentFlowDeployer environmentFlowDeployer;

    private EnvironmentFlowSynchronizer cut;

    @BeforeEach
    void setUp() {
        cut =
            new EnvironmentFlowSynchronizer(
                latestEventFetcher,
                new EnvironmentFlowMapper(objectMapper, environmentService),
                deployerFactory,
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
            );

        lenient().when(latestEventFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createEnvironmentFlowDeployer()).thenReturn(environmentFlowDeployer);
        lenient().when(environmentFlowDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(environmentFlowDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(environmentFlowDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(environmentFlowDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_environment_flow_when_no_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(environmentFlowDeployer);
        }

        @Test
        void should_not_synchronize_environment_flow_when_events_with_unknown_type() throws InterruptedException {
            Event event = new Event();
            event.setId("event");
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(environmentFlowDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("event")).test().await().assertComplete();
            verify(latestEventFetcher)
                .fetchLatest(eq(-1L), any(), eq(ENVIRONMENT_FLOW_ID), eq(Set.of("event")), eq(Set.of(DEPLOY_ENVIRONMENT_FLOW)));
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of("event")).test().await().assertComplete();
            verify(latestEventFetcher)
                .fetchLatest(
                    any(),
                    any(),
                    eq(ENVIRONMENT_FLOW_ID),
                    eq(Set.of("event")),
                    eq(Set.of(DEPLOY_ENVIRONMENT_FLOW, EventType.UNDEPLOY_ENVIRONMENT_FLOW))
                );
        }
    }

    @Nested
    class EventTest {

        @Test
        void should_register_api_when_fetching_publish_events() throws InterruptedException, JsonProcessingException {
            Event event = new Event();
            event.setId("id");
            final SharedPolicyGroup environmentFlow = SharedPolicyGroup
                .builder()
                .id("id")
                .definition(
                    objectMapper.writeValueAsString(
                        io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow
                            .builder()
                            .phase(EnumSet.of(io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow.Phase.REQUEST))
                            .policies(List.of())
                            .id("env_flow_id")
                            .name("name")
                            .build()
                    )
                )
                .build();
            event.setPayload(objectMapper.writeValueAsString(environmentFlow));
            event.setType(DEPLOY_ENVIRONMENT_FLOW);

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(environmentFlowDeployer).deploy(any());
            verify(environmentFlowDeployer).doAfterDeployment(any());
        }

        @Test
        void should_unregister_api_when_fetching_close_events() throws InterruptedException {
            Event event = new Event();
            event.setId("id");
            event.setProperties(Map.of(ENVIRONMENT_FLOW_ID.getValue(), "id"));
            event.setType(UNDEPLOY_ENVIRONMENT_FLOW);

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(environmentFlowDeployer).undeploy(any());
        }
    }
}
