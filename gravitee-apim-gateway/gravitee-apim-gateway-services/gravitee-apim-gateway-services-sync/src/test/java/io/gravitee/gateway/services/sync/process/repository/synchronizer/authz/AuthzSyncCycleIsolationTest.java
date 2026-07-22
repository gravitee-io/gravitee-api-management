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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzEntityDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.AuthzPolicyDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.Vertx;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * An authz synchronizer failure must never fail the shared sync cycle: authz runs at order 8-10,
 * before API(12)/SUBSCRIPTION(13)/API_KEY(14), and a propagated error would both stop API sync from
 * progressing and pin the cycle window. Instead the synchronizer swallows the error and re-fetches
 * its own missed window on the next cycle.
 */
class AuthzSyncCycleIsolationTest {

    private final LatestEventFetcher fetcher = mock(LatestEventFetcher.class);
    private final AuthzEnginePort enginePort = mock(AuthzEnginePort.class);
    private final DeployerFactory deployerFactory = mock(DeployerFactory.class);

    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        NoopDistributedSyncService distributedSyncService = new NoopDistributedSyncService();
        lenient().when(fetcher.bulkItems()).thenReturn(10);
        lenient().when(enginePort.commit()).thenReturn(Completable.complete());
        lenient().when(enginePort.addOrUpdateEntity(any(), any(), any(), any(), any(), anyLong())).thenReturn(Completable.complete());
        lenient().when(enginePort.commitScope(any(), any())).thenReturn(Completable.complete());
        lenient().when(deployerFactory.createAuthzEntityDeployer()).thenReturn(new AuthzEntityDeployer(enginePort, distributedSyncService));
        lenient().when(deployerFactory.createAuthzPolicyDeployer()).thenReturn(new AuthzPolicyDeployer(enginePort, distributedSyncService));
    }

    @AfterEach
    void tearDown() {
        vertx.close().blockingAwait();
    }

    @Test
    void entity_fetch_error_does_not_fail_the_cycle() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.error(new RuntimeException("db down")));

        entitySynchronizer().synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
    }

    @Test
    void policy_fetch_error_does_not_fail_the_cycle() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.error(new RuntimeException("db down")));

        policySynchronizer().synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
    }

    @Test
    void pdp_fetch_error_does_not_fail_the_cycle() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any())).thenReturn(Flowable.error(new RuntimeException("db down")));

        pdpSynchronizer().synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
    }

    @Test
    void entity_refetches_failed_window_on_next_cycle() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("db down")))
            .thenReturn(Flowable.empty());
        AuthzEntitySynchronizer synchronizer = entitySynchronizer();

        synchronizer.synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(6_000L, 7_000L, Set.of("env-1")).test().await().assertComplete();

        assertThat(fetchedFroms()).containsExactly(1_000L, 1_000L);
    }

    @Test
    void entity_returns_to_live_window_after_recovery() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("db down")))
            .thenReturn(Flowable.empty());
        AuthzEntitySynchronizer synchronizer = entitySynchronizer();

        synchronizer.synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(6_000L, 7_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(11_000L, 12_000L, Set.of("env-1")).test().await().assertComplete();

        assertThat(fetchedFroms()).containsExactly(1_000L, 1_000L, 11_000L);
    }

    @Test
    void entity_initial_fetch_error_redoes_initial_fetch_on_next_cycle() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("db down")))
            .thenReturn(Flowable.empty());
        AuthzEntitySynchronizer synchronizer = entitySynchronizer();

        synchronizer.synchronize(-1L, 2_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(6_000L, 7_000L, Set.of("env-1")).test().await().assertComplete();

        assertThat(fetchedFroms()).containsExactly(-1L, -1L);
        assertThat(fetchedTypes().get(1)).containsExactly(EventType.PUBLISH_AUTHZ_ENTITY);
    }

    @Test
    void entity_keeps_widened_window_across_repeated_failures() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("db down")))
            .thenReturn(Flowable.error(new RuntimeException("db still down")))
            .thenReturn(Flowable.empty());
        AuthzEntitySynchronizer synchronizer = entitySynchronizer();

        synchronizer.synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(6_000L, 7_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(11_000L, 12_000L, Set.of("env-1")).test().await().assertComplete();

        assertThat(fetchedFroms()).containsExactly(1_000L, 1_000L, 1_000L);
    }

    @Test
    void pdp_refetches_failed_window_on_next_cycle() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("db down")))
            .thenReturn(Flowable.empty());
        AuthzPdpSynchronizer synchronizer = pdpSynchronizer();

        synchronizer.synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(6_000L, 7_000L, Set.of("env-1")).test().await().assertComplete();

        assertThat(fetchedFroms()).containsExactly(1_000L, 1_000L);
    }

    @Test
    void pdp_returns_to_live_window_after_recovery() throws InterruptedException {
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("db down")))
            .thenReturn(Flowable.empty());
        AuthzPdpSynchronizer synchronizer = pdpSynchronizer();

        synchronizer.synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(6_000L, 7_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(11_000L, 12_000L, Set.of("env-1")).test().await().assertComplete();

        assertThat(fetchedFroms()).containsExactly(1_000L, 1_000L, 11_000L);
    }

    @Test
    void entity_recovered_window_events_are_deployed() throws InterruptedException {
        Event missed = new Event();
        missed.setId("evt-missed");
        missed.setType(EventType.PUBLISH_AUTHZ_ENTITY);
        missed.setPayload(
            "{\"entityId\":\"custom.x\",\"kind\":\"RESOURCE\",\"attributes\":{},\"parents\":[],\"environmentId\":\"env-1\",\"targetPdpIds\":[\"default\"]}"
        );
        when(fetcher.fetchLatest(any(), any(), any(), any(), any()))
            .thenReturn(Flowable.error(new RuntimeException("db down")))
            .thenReturn(Flowable.just(List.of(missed)));
        AuthzEntitySynchronizer synchronizer = entitySynchronizer();

        synchronizer.synchronize(1_000L, 2_000L, Set.of("env-1")).test().await().assertComplete();
        synchronizer.synchronize(6_000L, 7_000L, Set.of("env-1")).test().await().assertComplete();

        verify(enginePort).addOrUpdateEntity(eq("env-1"), any(), any(), any(), any(), anyLong());
        verify(enginePort, atLeastOnce()).commit();
    }

    private List<Long> fetchedFroms() {
        ArgumentCaptor<Long> from = ArgumentCaptor.forClass(Long.class);
        verify(fetcher, atLeastOnce()).fetchLatest(from.capture(), any(), any(), any(), any());
        return from.getAllValues();
    }

    @SuppressWarnings("unchecked")
    private List<Set<EventType>> fetchedTypes() {
        ArgumentCaptor<Set<EventType>> types = ArgumentCaptor.forClass(Set.class);
        verify(fetcher, atLeastOnce()).fetchLatest(any(), any(), any(), any(), types.capture());
        return types.getAllValues();
    }

    private AuthzEntitySynchronizer entitySynchronizer() {
        return new AuthzEntitySynchronizer(
            fetcher,
            new AuthzEntityMapper(new ObjectMapper()),
            deployerFactory,
            enginePort,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
    }

    private AuthzPolicySynchronizer policySynchronizer() {
        return new AuthzPolicySynchronizer(
            fetcher,
            new AuthzPolicyMapper(new ObjectMapper()),
            deployerFactory,
            enginePort,
            new AuthzScopePlacement(),
            executor(),
            executor()
        );
    }

    private AuthzPdpSynchronizer pdpSynchronizer() {
        Node node = mock(Node.class);
        GatewayConfiguration gatewayConfiguration = mock(GatewayConfiguration.class);
        lenient().when(node.id()).thenReturn("gw-1");
        lenient().when(gatewayConfiguration.shardingTags()).thenReturn(Optional.empty());
        return new AuthzPdpSynchronizer(
            fetcher,
            new AuthzPdpMapper(new ObjectMapper()),
            policySynchronizer(),
            entitySynchronizer(),
            node,
            gatewayConfiguration,
            vertx,
            new AuthzHostedScopes(),
            executor(),
            executor(),
            new AuthzAppliedRevisions()
        );
    }

    private static ThreadPoolExecutor executor() {
        return new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }
}
