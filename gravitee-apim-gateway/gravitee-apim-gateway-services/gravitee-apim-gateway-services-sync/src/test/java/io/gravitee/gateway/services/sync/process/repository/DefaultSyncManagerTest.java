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
package io.gravitee.gateway.services.sync.process.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.services.sync.process.distributed.DistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.handler.SyncHandler;
import io.gravitee.node.api.Node;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.gravitee.repository.distributedsync.model.DistributedSyncStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DefaultSyncManagerTest {

    @Mock
    Router router;

    @Mock
    Route route;

    @Mock
    Node node;

    private final List<RepositorySynchronizer> synchronizers = new ArrayList<>();

    private DefaultSyncManager cut;

    @BeforeEach
    public void beforeEach() {
        lenient().when(node.metadata()).thenReturn(Map.of(Node.META_ENVIRONMENTS, Set.of("env")));
        when(router.get(any())).thenReturn(route);
        when(route.produces(any())).thenReturn(route);
        cut = new DefaultSyncManager(router, node, synchronizers, null, new NoopDistributedSyncService(), 5, TimeUnit.SECONDS, 1);
    }

    @Test
    void should_synchronize_at_startup() throws Exception {
        RepositorySynchronizer synchronizer2 = spy(new FakeSynchronizer(Completable.complete(), 2));
        RepositorySynchronizer synchronizer1 = spy(new FakeSynchronizer(Completable.complete(), 1));
        synchronizers.add(synchronizer2);
        synchronizers.add(synchronizer1);
        cut.start();
        InOrder inOrder = inOrder(route, synchronizer1, synchronizer2);
        inOrder.verify(route).handler(argThat(argument -> argument instanceof SyncHandler));
        inOrder.verify(synchronizer1).synchronize(eq(-1L), any(), anySet());
        inOrder.verify(synchronizer2).synchronize(eq(-1L), any(), anySet());
        assertThat(cut.syncDone()).isTrue();
    }

    @Test
    void should_perform_full_initial_sync_on_primary_cold_start_even_with_existing_distributed_state() throws Exception {
        DistributedSyncService distributedSyncService = mock(DistributedSyncService.class);
        when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.CONNECTED));
        when(distributedSyncService.isEnabled()).thenReturn(true);
        when(distributedSyncService.isPrimaryNode()).thenReturn(true);
        when(distributedSyncService.ready()).thenReturn(Completable.complete());
        lenient()
            .when(distributedSyncService.state())
            .thenReturn(Maybe.just(DistributedSyncState.builder().from(1_000L).to(2_000L).build()));
        when(distributedSyncService.storeState(anyLong(), anyLong())).thenReturn(Completable.complete());

        RepositorySynchronizer synchronizer = spy(new FakeSynchronizer(Completable.complete(), 1));
        synchronizers.add(synchronizer);

        cut = new DefaultSyncManager(router, node, synchronizers, null, distributedSyncService, 5, TimeUnit.SECONDS, 1);
        cut.start();

        // A cold-started primary has nothing deployed in memory: the distributed sync state stored
        // by a previous run must not shrink the first sync window, otherwise no API gets deployed.
        verify(synchronizer).synchronize(eq(-1L), any(), anySet());
        verify(distributedSyncService, never()).state();
        assertThat(cut.syncDone()).isTrue();
    }

    @Test
    void should_resume_from_distributed_state_when_secondary_is_promoted_to_primary() throws Exception {
        try {
            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
            RxJavaPlugins.setIoSchedulerHandler(s -> testScheduler);

            DistributedSyncService distributedSyncService = mock(DistributedSyncService.class);
            when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.CONNECTED));
            when(distributedSyncService.isEnabled()).thenReturn(true);
            // Node starts as secondary (doStart + first cycle), then gets promoted to primary.
            when(distributedSyncService.isPrimaryNode()).thenReturn(false, false, false, true);
            when(distributedSyncService.ready()).thenReturn(Completable.complete());
            when(distributedSyncService.state()).thenReturn(Maybe.just(DistributedSyncState.builder().from(1_000L).to(2_000L).build()));
            when(distributedSyncService.storeState(anyLong(), anyLong())).thenReturn(Completable.complete());

            RepositorySynchronizer synchronizer = spy(new FakeSynchronizer(Completable.complete(), 1));
            synchronizers.add(synchronizer);
            DistributedSynchronizer distributedSynchronizer = mock(DistributedSynchronizer.class);
            when(distributedSynchronizer.synchronize(any(), any())).thenReturn(Completable.complete());

            cut = new DefaultSyncManager(
                router,
                node,
                synchronizers,
                new ArrayList<>(List.of(distributedSynchronizer)),
                distributedSyncService,
                5,
                TimeUnit.SECONDS,
                1
            );
            cut.start();

            // As a secondary, the initial sync reads distributed events from scratch.
            verify(distributedSynchronizer).synchronize(eq(-1L), any());

            // Promotion happens on the next cycle: the node is already warm, so it resumes the
            // repository sync from the window stored by the previous primary.
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            verify(distributedSyncService).state();
            verify(synchronizer).synchronize(eq(1_000L), any(), anySet());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    void should_perform_full_initial_sync_when_secondary_is_promoted_before_completing_any_sync() throws Exception {
        DistributedSyncService distributedSyncService = mock(DistributedSyncService.class);
        when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.CONNECTED));
        when(distributedSyncService.isEnabled()).thenReturn(true);
        // Node starts as secondary but the primary dies before the first sync cycle runs.
        when(distributedSyncService.isPrimaryNode()).thenReturn(false, true);
        when(distributedSyncService.ready()).thenReturn(Completable.complete());
        lenient()
            .when(distributedSyncService.state())
            .thenReturn(Maybe.just(DistributedSyncState.builder().from(1_000L).to(2_000L).build()));
        when(distributedSyncService.storeState(anyLong(), anyLong())).thenReturn(Completable.complete());

        RepositorySynchronizer synchronizer = spy(new FakeSynchronizer(Completable.complete(), 1));
        synchronizers.add(synchronizer);

        cut = new DefaultSyncManager(router, node, synchronizers, null, distributedSyncService, 5, TimeUnit.SECONDS, 1);
        cut.start();

        // Nothing is deployed yet, so the promotion must not resume from the stored state.
        verify(synchronizer).synchronize(eq(-1L), any(), anySet());
        verify(distributedSyncService, never()).state();
        assertThat(cut.syncDone()).isTrue();
    }

    @Test
    void should_report_not_ready_until_initial_sync_succeeds() throws Exception {
        try {
            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
            RxJavaPlugins.setIoSchedulerHandler(s -> testScheduler);

            AtomicInteger counter = new AtomicInteger(0);
            // Fail the initial sync (and its inner retry), then succeed on the outer retry.
            RepositorySynchronizer synchronizer = spy(
                new FakeSynchronizer(
                    Completable.defer(() ->
                        counter.getAndIncrement() < 2 ? Completable.error(new RuntimeException("bridge 502")) : Completable.complete()
                    ),
                    1
                )
            );
            synchronizers.add(synchronizer);

            cut.start();

            // Initial sync failed (bridge 502): the node must not be marked ready.
            assertThat(cut.syncDone()).isFalse();

            // The retries (bridge recovered) eventually succeed: the node becomes ready.
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            testScheduler.triggerActions();
            assertThat(cut.syncDone()).isTrue();
            assertThat(counter.get()).isGreaterThanOrEqualTo(3);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    void should_synchronize_sequentially_after_initial_synchronization() throws Exception {
        try {
            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
            RxJavaPlugins.setIoSchedulerHandler(s -> testScheduler);

            AtomicInteger counter = new AtomicInteger(0);

            RepositorySynchronizer synchronizer1 = spy(new FakeSynchronizer(Completable.complete(), 1));
            RepositorySynchronizer synchronizer2 = spy(
                new FakeSynchronizer(
                    Completable.defer(() -> {
                        if (counter.getAndIncrement() == 0) {
                            return Completable.complete();
                        }

                        return Completable.complete().delay(8, TimeUnit.SECONDS);
                    }),
                    2
                )
            );
            synchronizers.add(synchronizer1);
            synchronizers.add(synchronizer2);

            cut.start();

            InOrder inOrder = inOrder(route, synchronizer1, synchronizer2);
            inOrder.verify(route).handler(argThat(argument -> argument instanceof SyncHandler));
            inOrder.verify(synchronizer1).synchronize(eq(-1L), any(), anySet());
            inOrder.verify(synchronizer2).synchronize(eq(-1L), any(), anySet());

            // Trigger a diff sync.
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // Advance time by another 5 seconds to complete the sync process and exceed the delay between 2 sync at the same time.
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // Synchronizer has been called twice, one on full sync (start), one on diff sync
            assertThat(counter.get()).isEqualTo(2);

            // Diff sync should be called with a from date != -1.
            inOrder.verify(synchronizer1).synchronize(argThat(from -> from != -1L), any(), anySet());
            inOrder.verify(synchronizer2).synchronize(argThat(from -> from != -1L), any(), anySet());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    void should_synchronize_after_last_attempt_synchronizer_failed() throws Exception {
        try {
            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
            RxJavaPlugins.setIoSchedulerHandler(s -> testScheduler);

            AtomicInteger counter = new AtomicInteger(0);
            AtomicInteger counterSyncComplete = new AtomicInteger(0);

            RepositorySynchronizer synchronizer1 = spy(new FakeSynchronizer(Completable.complete(), 1));
            RepositorySynchronizer synchronizer2 = spy(
                new FakeSynchronizer(
                    Completable.defer(() -> {
                        var currentCounter = counter.getAndIncrement();
                        if (currentCounter > 0 && currentCounter < 3) {
                            return Completable.error(new RuntimeException("Sync exception"));
                        }
                        counterSyncComplete.incrementAndGet();
                        return Completable.complete();
                    }),
                    2
                )
            );
            synchronizers.add(synchronizer1);
            synchronizers.add(synchronizer2);

            cut.start();

            InOrder inOrder = inOrder(route, synchronizer1, synchronizer2);
            inOrder.verify(route).handler(argThat(argument -> argument instanceof SyncHandler));
            inOrder.verify(synchronizer1).synchronize(eq(-1L), any(), anySet());
            inOrder.verify(synchronizer2).synchronize(eq(-1L), any(), anySet());

            // Trigger a diff sync (failure)
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // First retry (failure)
            testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // Wait 3 more second for retry delay
            testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // Sync process retry (success)
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // Synchronizer has been called 4 times with 2 success (one on full sync (start) and one on diff sync after 2 failure)
            assertThat(counter.get()).isEqualTo(4);
            assertThat(counterSyncComplete.get()).isEqualTo(2);

            // Diff sync should be called with a from date != -1.
            inOrder.verify(synchronizer1).synchronize(argThat(from -> from != -1L), any(), anySet());
            inOrder.verify(synchronizer2).synchronize(argThat(from -> from != -1L), any(), anySet());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Nested
    class Pause {

        private final TestScheduler testScheduler = new TestScheduler();
        private DistributedSyncService distributedSyncService;
        private DistributedSynchronizer distributedSynchronizer;
        private RepositorySynchronizer synchronizer;

        @BeforeEach
        void setUp() {
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);
            RxJavaPlugins.setIoSchedulerHandler(s -> testScheduler);
            distributedSyncService = mock(DistributedSyncService.class);
            lenient().when(distributedSyncService.isEnabled()).thenReturn(true);
            lenient().when(distributedSyncService.isPrimaryNode()).thenReturn(true);
            lenient().when(distributedSyncService.ready()).thenReturn(Completable.complete());
            lenient().when(distributedSyncService.storeState(anyLong(), anyLong())).thenReturn(Completable.complete());
            lenient().when(distributedSyncService.updateStatus(any())).thenReturn(Completable.complete());
            synchronizer = spy(new FakeSynchronizer(Completable.complete(), 1));
            synchronizers.add(synchronizer);
            distributedSynchronizer = mock(DistributedSynchronizer.class);
            lenient().when(distributedSynchronizer.synchronize(any(), any())).thenReturn(Completable.complete());
            cut = new DefaultSyncManager(
                router,
                node,
                synchronizers,
                new ArrayList<>(List.of(distributedSynchronizer)),
                distributedSyncService,
                5,
                TimeUnit.SECONDS,
                1
            );
        }

        @AfterEach
        void tearDown() {
            RxJavaPlugins.reset();
        }

        @Test
        void should_read_distributed_events_on_a_paused_primary_and_keep_the_stored_state() throws Exception {
            when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.PAUSED));

            cut.start();

            verify(distributedSynchronizer).synchronize(eq(-1L), any());
            verify(synchronizer, never()).synchronize(any(), any(), anySet());
            verify(distributedSyncService, never()).storeState(anyLong(), anyLong());
            assertThat(cut.syncDone()).isTrue();
        }

        @Test
        void should_confirm_the_pause_on_the_primary() throws Exception {
            when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.PAUSING));

            cut.start();

            verify(distributedSyncService).updateStatus(DistributedSyncStatus.PAUSED);
            verify(synchronizer, never()).synchronize(any(), any(), anySet());
        }

        @Test
        void should_not_confirm_the_pause_on_a_secondary() throws Exception {
            when(distributedSyncService.isPrimaryNode()).thenReturn(false);
            when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.PAUSING));

            cut.start();

            verify(distributedSyncService, never()).updateStatus(any());
            verify(distributedSynchronizer).synchronize(eq(-1L), any());
        }

        @Test
        void should_cancel_a_running_cycle_when_a_pause_is_requested() throws Exception {
            CompletableSubject stuckCycle = CompletableSubject.create();
            RepositorySynchronizer stuck = spy(new FakeSynchronizer(stuckCycle, 2));
            synchronizers.add(stuck);
            when(distributedSyncService.status()).thenReturn(
                Single.just(DistributedSyncStatus.CONNECTED),
                Single.just(DistributedSyncStatus.PAUSING)
            );
            AtomicBoolean stateStored = new AtomicBoolean();
            when(distributedSyncService.storeState(anyLong(), anyLong())).thenReturn(Completable.fromAction(() -> stateStored.set(true)));

            cut.start();
            assertThat(stuckCycle.hasObservers()).isTrue();

            testScheduler.advanceTimeBy(DefaultSyncManager.PAUSE_POLL_DELAY_MS, TimeUnit.MILLISECONDS);

            assertThat(stuckCycle.hasObservers()).isFalse();
            assertThat(stateStored).isFalse();
        }

        @Test
        void should_replay_from_the_stored_window_with_a_margin_then_reconnect_when_resuming() throws Exception {
            when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.RESUMING));
            long storedFrom = 10_000_000L;
            when(distributedSyncService.state()).thenReturn(Maybe.just(DistributedSyncState.builder().from(storedFrom).to(2L).build()));

            AtomicBoolean reconnected = new AtomicBoolean();
            when(distributedSyncService.updateStatus(DistributedSyncStatus.CONNECTED)).thenReturn(
                Completable.fromAction(() -> reconnected.set(true))
            );

            cut.start();

            verify(synchronizer).synchronize(eq(storedFrom - DefaultSyncManager.RESUME_MARGIN_MS), any(), anySet());
            assertThat(reconnected).isTrue();
        }

        @Test
        void should_stay_resuming_when_the_replay_fails() throws Exception {
            synchronizers.clear();
            synchronizers.add(new FakeSynchronizer(Completable.error(new RuntimeException("control plane unreachable")), 1));
            when(distributedSyncService.status()).thenReturn(Single.just(DistributedSyncStatus.RESUMING));
            when(distributedSyncService.state()).thenReturn(Maybe.just(DistributedSyncState.builder().from(10_000_000L).to(2L).build()));
            AtomicBoolean reconnected = new AtomicBoolean();
            when(distributedSyncService.updateStatus(DistributedSyncStatus.CONNECTED)).thenReturn(
                Completable.fromAction(() -> reconnected.set(true))
            );

            cut.start();
            testScheduler.advanceTimeBy(DefaultSyncManager.RETRY_DELAY_MS * 2L, TimeUnit.MILLISECONDS);

            assertThat(reconnected).isFalse();
        }
    }
}
