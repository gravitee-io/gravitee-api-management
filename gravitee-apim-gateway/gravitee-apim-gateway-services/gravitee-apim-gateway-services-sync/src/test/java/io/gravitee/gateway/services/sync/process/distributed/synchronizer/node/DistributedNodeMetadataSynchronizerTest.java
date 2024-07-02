/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.gravitee.gateway.services.sync.process.distributed.synchronizer.node;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.NodeMetadataDeployer;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
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
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedNodeMetadataSynchronizerTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private NodeMetadataDeployer nodeMetadataDeployer;

    private DistributedNodeMetadataSynchronizer cut;

    @BeforeEach
    public void beforeEach() {
        cut =
            new DistributedNodeMetadataSynchronizer(
                eventsFetcher,
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
                deployerFactory,
                new NodeMetadataMapper(objectMapper)
            );
        lenient().when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createNodeMetadataDeployer()).thenReturn(nodeMetadataDeployer);
        lenient().when(nodeMetadataDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(nodeMetadataDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(nodeMetadataDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(nodeMetadataDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_node_metadata_when_no_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), eq(DistributedEventType.NODE_METADATA), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verifyNoInteractions(nodeMetadataDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher)
                .fetchLatest(eq(-1L), any(), eq(DistributedEventType.NODE_METADATA), eq(Set.of(DistributedSyncAction.DEPLOY)));
        }

        @Test
        void should_not_fetch_incremental_events() throws InterruptedException {
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();
            verify(eventsFetcher, never())
                .fetchLatest(
                    any(),
                    any(),
                    eq(DistributedEventType.NODE_METADATA),
                    eq(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
                );
        }
    }

    @Nested
    class DistributedNodeMetadataSynchronizationTest {

        private NodeMetadataMapper.DistributedNodeMetadataDeployable metadataDeployable;

        @BeforeEach
        public void init() {
            metadataDeployable = new NodeMetadataMapper.DistributedNodeMetadataDeployable(Set.of("orga1", "orga2"), "installation");
        }

        @Test
        void should_deploy_dictionary_when_fetching_deployed_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("id")
                .payload(objectMapper.writeValueAsString(metadataDeployable))
                .type(DistributedEventType.NODE_METADATA)
                .syncAction(DistributedSyncAction.DEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verify(nodeMetadataDeployer).deploy(any());
            verify(nodeMetadataDeployer).doAfterDeployment(any());
            verify(nodeMetadataDeployer, never()).undeploy(any());
            verify(nodeMetadataDeployer, never()).doAfterUndeployment(any());
        }

        @Test
        void should_ignore_undeploy_node_metadata_when_fetching_undeployed_events() throws InterruptedException, JsonProcessingException {
            DistributedEvent distributedEvent = DistributedEvent
                .builder()
                .id("id")
                .payload(objectMapper.writeValueAsString(metadataDeployable))
                .type(DistributedEventType.NODE_METADATA)
                .syncAction(DistributedSyncAction.UNDEPLOY)
                .updatedAt(new Date())
                .build();

            when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(distributedEvent));
            cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

            verifyNoInteractions(nodeMetadataDeployer);
        }
    }
}
