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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.handler.SyncHandler;
import io.gravitee.node.api.Node;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
        when(node.metadata()).thenReturn(Map.of(Node.META_ENVIRONMENTS, Set.of("env")));
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
        inOrder.verify(synchronizer1).synchronize(eq(-1L), any(), anyList());
        inOrder.verify(synchronizer2).synchronize(eq(-1L), any(), anyList());
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
            inOrder.verify(synchronizer1).synchronize(eq(-1L), any(), anyList());
            inOrder.verify(synchronizer2).synchronize(eq(-1L), any(), anyList());

            // Trigger a diff sync.
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // Advance time by another 5 seconds to complete the sync process and exceed the delay between 2 sync at the same time.
            testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
            testScheduler.triggerActions();

            // Synchronizer has been called twice, one on full sync (start), one on diff sync
            assertThat(counter.get()).isEqualTo(2);

            // Diff sync should be called with a from date != -1.
            inOrder.verify(synchronizer1).synchronize(argThat(from -> from != -1L), any(), anyList());
            inOrder.verify(synchronizer2).synchronize(argThat(from -> from != -1L), any(), anyList());
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
}
