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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.cluster;

import static io.gravitee.repository.management.model.Event.EventProperties.CLUSTER_ID;
import static io.gravitee.repository.management.model.EventType.DEPLOY_CLUSTER;
import static io.gravitee.repository.management.model.EventType.UNDEPLOY_CLUSTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.ClusterDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.ClusterMapper;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClusterSynchronizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LatestEventFetcher latestEventFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ClusterDeployer clusterDeployer;

    private ClusterSynchronizer cut;

    @BeforeEach
    void setUp() {
        cut = new ClusterSynchronizer(
            latestEventFetcher,
            new ClusterMapper(objectMapper),
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );

        lenient().when(latestEventFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createClusterDeployer()).thenReturn(clusterDeployer);
        lenient().when(clusterDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(clusterDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(clusterDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(clusterDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_not_synchronize_when_no_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verifyNoInteractions(clusterDeployer);
        }

        @Test
        void should_fetch_init_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of("env")).test().await().assertComplete();
            verify(latestEventFetcher).fetchLatest(eq(-1L), any(), eq(CLUSTER_ID), eq(Set.of("env")), eq(Set.of(DEPLOY_CLUSTER)));
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.empty());
            cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), Set.of("env")).test().await().assertComplete();
            verify(latestEventFetcher).fetchLatest(
                any(),
                any(),
                eq(CLUSTER_ID),
                eq(Set.of("env")),
                eq(Set.of(DEPLOY_CLUSTER, UNDEPLOY_CLUSTER))
            );
        }
    }

    @Nested
    class EventTest {

        @Test
        void should_deploy_cluster_when_fetching_deploy_events() throws InterruptedException {
            Event event = new Event();
            event.setId("event-id");
            event.setType(DEPLOY_CLUSTER);
            event.setPayload(
                """
                {
                    "id": "cluster-id",
                    "crossId": "my-cluster",
                    "name": "My Cluster",
                    "type": "KAFKA_CLUSTER",
                    "environmentId": "env-1",
                    "organizationId": "org-1",
                    "configuration": { "connections": [] }
                }
                """
            );

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(clusterDeployer).deploy(any());
            verify(clusterDeployer).doAfterDeployment(any());
        }

        @Test
        void should_undeploy_cluster_when_fetching_undeploy_events() throws InterruptedException {
            Event event = new Event();
            event.setId("event-id");
            event.setType(UNDEPLOY_CLUSTER);
            event.setProperties(Map.of(CLUSTER_ID.getValue(), "my-cluster"));

            when(latestEventFetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.just(List.of(event)));
            cut.synchronize(-1L, Instant.now().toEpochMilli(), Set.of()).test().await().assertComplete();

            verify(clusterDeployer).undeploy(any());
        }
    }
}
