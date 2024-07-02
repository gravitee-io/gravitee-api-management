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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.environmentflow.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.EnvironmentFlowDeployer;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.EnvironmentFlowMapper;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.Date;
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
class DistributedEnvironmentFlowSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    private DistributedEnvironmentFlowSynchronizer cut;

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private EnvironmentFlowDeployer environmentFlowDeployer;

    @BeforeEach
    void setUp() {
        cut =
            new DistributedEnvironmentFlowSynchronizer(
                eventsFetcher,
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                deployerFactory,
                new EnvironmentFlowMapper(objectMapper)
            );

        when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createEnvironmentFlowDeployer()).thenReturn(environmentFlowDeployer);
        lenient().when(environmentFlowDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(environmentFlowDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(environmentFlowDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(environmentFlowDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEvent {

        @Test
        void should_not_synchronize_environment_flows_when_no_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), eq(DistributedEventType.ENVIRONMENT_FLOW), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verifyNoInteractions(environmentFlowDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher)
                .fetchLatest(eq(-1L), any(), eq(DistributedEventType.ENVIRONMENT_FLOW), eq(Set.of(DistributedSyncAction.DEPLOY)));
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher)
                .fetchLatest(
                    any(),
                    any(),
                    eq(DistributedEventType.ENVIRONMENT_FLOW),
                    eq(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
                );
        }
    }

    @Nested
    class DistributedEnvironmentFlowSynchronization {

        private ReactableEnvironmentFlow environmentFlow;

        @BeforeEach
        public void init() {
            environmentFlow = new ReactableEnvironmentFlow();
            environmentFlow.setId("id");
        }

        @Test
        void should_deploy_environment_flow_when_fetching_deployed_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("environment_flow")
                .payload(objectMapper.writeValueAsString(environmentFlow))
                .type(DistributedEventType.ENVIRONMENT_FLOW)
                .syncAction(DistributedSyncAction.DEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(environmentFlowDeployer).deploy(any());
            verify(environmentFlowDeployer).doAfterDeployment(any());
        }

        @Test
        void should_undeploy_environment_flow_when_fetching_undeployed_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("environment_flow")
                .payload(objectMapper.writeValueAsString(environmentFlow))
                .type(DistributedEventType.DICTIONARY)
                .syncAction(DistributedSyncAction.UNDEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(environmentFlowDeployer).undeploy(any());
            verify(environmentFlowDeployer).doAfterUndeployment(any());
        }
    }
}
