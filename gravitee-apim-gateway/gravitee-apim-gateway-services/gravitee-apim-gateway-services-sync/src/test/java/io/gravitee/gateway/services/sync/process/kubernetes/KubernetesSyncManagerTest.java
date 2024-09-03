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
package io.gravitee.gateway.services.sync.process.kubernetes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.node.api.Node;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KubernetesSyncManagerTest {

    private static final Set<String> ENVIRONMENTS = Set.of("test-env");

    @Mock
    private Node node;

    @Mock
    private KubernetesSynchronizer synchronizer;

    @Mock
    private DistributedSyncService distributedSyncService;

    private KubernetesSyncManager cut;

    @BeforeEach
    void setUp() {
        when(distributedSyncService.isEnabled()).thenReturn(false);
        cut = new KubernetesSyncManager(node, List.of(synchronizer), distributedSyncService);
    }

    @AfterEach
    void tearDown() {
        cut.doStop();
    }

    @Test
    void should_not_synchronize_if_distributed_sync_service_is_enabled() {
        when(distributedSyncService.isEnabled()).thenReturn(true);
        cut.doStart();
        verifyNoInteractions(synchronizer);
    }

    @Test
    void should_be_done_once_synchronize_succeeds() {
        try {
            var scheduler = new TestScheduler();
            var inOrder = inOrder(synchronizer);

            RxJavaPlugins.setComputationSchedulerHandler(s -> scheduler);

            when(node.metadata()).thenReturn(Map.of(Node.META_ENVIRONMENTS, ENVIRONMENTS));

            when(synchronizer.synchronize(ENVIRONMENTS))
                .thenReturn(Completable.error(new RuntimeException("This an expected error")))
                .thenReturn(Completable.complete().delay(1, TimeUnit.SECONDS));

            when(synchronizer.watch(ENVIRONMENTS)).thenReturn(Completable.complete());

            cut.doStart();

            inOrder.verify(synchronizer).synchronize(ENVIRONMENTS);
            assertThat(cut.syncDone()).isFalse();
            verify(synchronizer, never()).watch(ENVIRONMENTS);

            scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

            inOrder.verify(synchronizer).synchronize(ENVIRONMENTS);
            assertThat(cut.syncDone()).isTrue();
            verify(synchronizer).watch(ENVIRONMENTS);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
