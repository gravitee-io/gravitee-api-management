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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.genericevent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.genericevent.GenericEventManager;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.deployer.GenericEventDeployer;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.GenericEventMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent.GenericEventDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedGenericEventSynchronizerTest {

    private final GenericEventMapper genericEventMapper = new GenericEventMapper(new ObjectMapper());

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private GenericEventDeployer deployer;

    @Mock
    private GenericEventManager genericEventManager;

    private DistributedGenericEventSynchronizer cut;

    @BeforeEach
    void beforeEach() {
        cut = new DistributedGenericEventSynchronizer(
            eventsFetcher,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            deployerFactory,
            genericEventMapper,
            genericEventManager
        );
        lenient().when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createGenericEventDeployer()).thenReturn(deployer);
        lenient().when(deployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(deployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(deployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Test
    void should_use_the_generic_event_order() {
        assertThat(cut.order()).isEqualTo(Order.GENERIC_EVENT.index());
    }

    @Test
    void should_fetch_only_deploy_events_on_initial_sync() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(eventsFetcher).fetchLatest(eq(-1L), any(), eq(DistributedEventType.GENERIC_EVENT), eq(Set.of(DistributedSyncAction.DEPLOY)));
    }

    @Test
    void should_fetch_deploy_and_undeploy_events_on_incremental_sync() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(eventsFetcher).fetchLatest(
            any(),
            any(),
            eq(DistributedEventType.GENERIC_EVENT),
            eq(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
        );
    }

    @Test
    void should_deploy_a_distributed_event_then_commit() throws InterruptedException {
        var deployable = deployable(SyncAction.DEPLOY, "{\"hubId\":\"hub-1\"}");
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(
            Flowable.just(genericEventMapper.to(deployable).blockingGet())
        );

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        var order = inOrder(deployer, genericEventManager);
        order.verify(deployer).deploy(any());
        order.verify(deployer).doAfterDeployment(any());
        order.verify(genericEventManager).commit();
    }

    @Test
    void should_undeploy_a_distributed_event_then_commit() throws InterruptedException {
        var deployable = deployable(SyncAction.UNDEPLOY, null);
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(
            Flowable.just(genericEventMapper.to(deployable).blockingGet())
        );

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();

        var order = inOrder(deployer, genericEventManager);
        order.verify(deployer).undeploy(any());
        order.verify(deployer).doAfterUndeployment(any());
        order.verify(genericEventManager).commit();
    }

    @Test
    void should_commit_even_without_events_so_expired_events_are_dropped() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(genericEventManager).commit();
    }

    @Test
    void should_not_commit_when_the_sync_fails() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.error(new RuntimeException("store down")));

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertError(RuntimeException.class);

        verify(genericEventManager, never()).commit();
    }

    private static GenericEventDeployable deployable(SyncAction syncAction, String data) {
        return new GenericEventDeployable(
            "event-1",
            syncAction,
            new GenericEvent("aihub.hub", "hub-1", "env-1", "org-1", null, null, data)
        );
    }
}
