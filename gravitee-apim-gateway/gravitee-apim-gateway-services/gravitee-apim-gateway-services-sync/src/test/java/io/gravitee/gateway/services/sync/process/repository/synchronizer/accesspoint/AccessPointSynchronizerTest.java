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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.gateway.services.sync.process.common.deployer.AccessPointDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.repository.fetcher.AccessPointFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.AccessPointMapper;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.List;
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
class AccessPointSynchronizerTest {

    @Mock
    private AccessPointFetcher accessPointFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private AccessPointDeployer accessPointDeployer;

    private AccessPointSynchronizer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new AccessPointSynchronizer(
            accessPointFetcher,
            new AccessPointMapper(),
            deployerFactory,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
        );

        lenient().when(accessPointFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(List.of()));
        lenient().when(accessPointFetcher.bulkItems()).thenReturn(100);
        lenient().when(deployerFactory.createAccessPointDeployer()).thenReturn(accessPointDeployer);
        lenient().when(accessPointDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(accessPointDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(accessPointDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(accessPointDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Nested
    class NoEventTest {

        @Test
        void should_only_fetch_created_status_if_not_incremental() throws InterruptedException {
            Long to = Instant.now().toEpochMilli();
            cut.synchronize(-1L, to, Set.of("env")).test().await().assertComplete();

            verify(accessPointFetcher).fetchLatest(-1L, to, Set.of("env"), AccessPointStatus.CREATED);
        }

        @Test
        void should_fetch_incremental_events() throws InterruptedException {
            Long from = Instant.now().toEpochMilli() - 300000L;
            Long to = Instant.now().toEpochMilli();
            cut.synchronize(from, to, Set.of("env")).test().await().assertComplete();

            verify(accessPointFetcher).fetchLatest(from, to, Set.of("env"), null);
        }

        @Test
        void should_not_synchronize_when_no_changes() throws InterruptedException {
            cut.synchronize(1L, 1L, Set.of("env")).test().await().assertComplete();

            verifyNoInteractions(accessPointDeployer);
        }
    }

    @Nested
    class AccessPointSynchronizationTest {

        @Test
        void should_deploy_created_access_points() throws InterruptedException {
            AccessPoint accessPoint = new AccessPoint();
            accessPoint.setStatus(AccessPointStatus.CREATED);
            lenient().when(accessPointFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(List.of(accessPoint)));

            cut.synchronize(1L, 1L, Set.of("env1")).test().await().assertComplete();

            verify(accessPointDeployer).deploy(any());
            verify(accessPointDeployer).doAfterDeployment(any());
        }

        @Test
        void should_undeploy_deleted_access_points() throws InterruptedException {
            AccessPoint accessPoint = new AccessPoint();
            accessPoint.setStatus(AccessPointStatus.DELETED);
            lenient().when(accessPointFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(List.of(accessPoint)));

            cut.synchronize(1L, 1L, Set.of("env1")).test().await().assertComplete();

            verify(accessPointDeployer).undeploy(any());
            verify(accessPointDeployer).doAfterUndeployment(any());
        }
    }
}
