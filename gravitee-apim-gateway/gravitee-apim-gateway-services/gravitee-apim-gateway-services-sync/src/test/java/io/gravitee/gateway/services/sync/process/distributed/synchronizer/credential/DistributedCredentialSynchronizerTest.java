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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.credential;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.CredentialDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.CredentialMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.credential.CredentialDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedCredentialSynchronizerTest {

    private final CredentialMapper credentialMapper = new CredentialMapper(new GraviteeMapper());

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private CredentialDeployer credentialDeployer;

    private DistributedCredentialSynchronizer cut;

    @BeforeEach
    void beforeEach() {
        cut = new DistributedCredentialSynchronizer(
            eventsFetcher,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            deployerFactory,
            credentialMapper
        );
        when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createCredentialDeployer()).thenReturn(credentialDeployer);
        lenient().when(credentialDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(credentialDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(credentialDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(credentialDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Test
    void should_not_synchronize_credentials_when_no_events() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), eq(DistributedEventType.CREDENTIAL), any())).thenReturn(Flowable.empty());

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        verifyNoInteractions(credentialDeployer);
    }

    @Test
    void should_fetch_only_deploy_events_on_initial_sync() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(eventsFetcher).fetchLatest(eq(-1L), any(), eq(DistributedEventType.CREDENTIAL), eq(Set.of(DistributedSyncAction.DEPLOY)));
    }

    @Test
    void should_fetch_deploy_and_undeploy_events_on_incremental_sync() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(eventsFetcher).fetchLatest(
            any(),
            any(),
            eq(DistributedEventType.CREDENTIAL),
            eq(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
        );
    }

    @Test
    void should_deploy_a_distributed_credential() throws InterruptedException {
        CredentialDeployable deployable = CredentialDeployable.builder()
            .credentialId("credential-1")
            .environmentId("env-1")
            .organizationId("org-1")
            .encryptedSecret("ciphertext")
            .updatedAt(1234L)
            .syncAction(SyncAction.DEPLOY)
            .build();
        DistributedEvent event = credentialMapper.to(deployable).blockingGet();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(event));

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(credentialDeployer).deploy(deployable);
        verify(credentialDeployer).doAfterDeployment(deployable);
    }

    @Test
    void should_undeploy_a_distributed_credential() throws InterruptedException {
        CredentialDeployable deployable = CredentialDeployable.builder()
            .credentialId("credential-1")
            .syncAction(SyncAction.UNDEPLOY)
            .build();
        DistributedEvent event = credentialMapper.to(deployable).blockingGet();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(event));

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(credentialDeployer).undeploy(deployable);
        verify(credentialDeployer).doAfterUndeployment(deployable);
    }
}
