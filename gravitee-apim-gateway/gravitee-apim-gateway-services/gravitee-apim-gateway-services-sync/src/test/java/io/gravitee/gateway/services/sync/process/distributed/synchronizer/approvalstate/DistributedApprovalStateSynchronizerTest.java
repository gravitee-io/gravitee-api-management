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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.approvalstate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.ExecutorLease;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.Grant;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalFeature;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateJson;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateManagerImpl;
import io.gravitee.gateway.services.sync.process.common.deployer.ApprovalStateDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.synchronizer.Order;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApprovalStateMapper;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate.ApprovalStateDeployable;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
class DistributedApprovalStateSynchronizerTest {

    private static final DeployedApprovalState STATE = new DeployedApprovalState(
        "req-1",
        List.of("alias-1"),
        "env-1",
        "api-1",
        "rule-1",
        "2026-10-01T00:00:00Z",
        "open-1",
        "APPROVED",
        2_000L,
        null,
        new Grant("TIMED", 5_000L, "app-1", null, "digest-1"),
        new ExecutorLease("node-1", "lease-1", 4_000L),
        6_000L,
        500L,
        null,
        1_000L,
        3L
    );

    private final ApprovalStateMapper approvalStateMapper = new ApprovalStateMapper(
        new ApprovalStateJson(new GraviteeMapper()),
        Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)
    );

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApprovalStateDeployer approvalStateDeployer;

    private DistributedApprovalStateSynchronizer cut;

    @BeforeEach
    void beforeEach() {
        cut = new DistributedApprovalStateSynchronizer(
            eventsFetcher,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            deployerFactory,
            approvalStateMapper
        );
        lenient().when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createApprovalStateDeployer()).thenReturn(approvalStateDeployer);
        lenient().when(approvalStateDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(approvalStateDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(approvalStateDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(approvalStateDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
    }

    @Test
    void should_forget_an_unpublished_state_from_every_lookup_of_a_secondary_gateway() throws InterruptedException {
        ApprovalStateManagerImpl manager = new ApprovalStateManagerImpl(
            new ApprovalFeature(licensedLicenseManager(), true),
            Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC)
        );
        when(deployerFactory.createApprovalStateDeployer()).thenReturn(
            new ApprovalStateDeployer(manager, new NoopDistributedSyncService())
        );
        DistributedEvent deployed = approvalStateMapper
            .to(
                ApprovalStateDeployable.builder()
                    .approvalRequestId("req-1")
                    .organizationId("org-1")
                    .state(STATE)
                    .syncAction(SyncAction.DEPLOY)
                    .build()
            )
            .blockingGet();
        DistributedEvent undeployed = approvalStateMapper
            .to(ApprovalStateDeployable.builder().approvalRequestId("req-1").syncAction(SyncAction.UNDEPLOY).build())
            .blockingGet();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any()))
            .thenReturn(Flowable.just(deployed))
            .thenReturn(Flowable.just(undeployed));

        cut.synchronize(-1L, 100L).test().await().assertComplete();

        assertThat(manager.byId("req-1").blockingGet()).isNotNull();
        assertThat(manager.byId("alias-1").blockingGet()).isNotNull();
        assertThat(manager.byOpenKey("open-1").blockingGet()).isNotNull();
        assertThat(manager.activeGrant("env-1", "api-1", "rule-1", "app-1", null, "digest-1").blockingGet()).isNotNull();

        cut.synchronize(100L, 200L).test().await().assertComplete();

        assertThat(manager.byId("req-1").blockingGet()).isNull();
        assertThat(manager.byId("alias-1").blockingGet()).isNull();
        assertThat(manager.byOpenKey("open-1").blockingGet()).isNull();
        assertThat(manager.activeGrant("env-1", "api-1", "rule-1", "app-1", null, "digest-1").blockingGet()).isNull();
    }

    @Test
    void should_run_with_the_approval_state_order() {
        assertThat(cut.order()).isEqualTo(Order.APPROVAL_STATE.index());
    }

    @Test
    void should_fetch_only_deploy_events_on_initial_sync() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(-1L, 100L).test().await().assertComplete();

        verify(eventsFetcher).fetchLatest(
            eq(-1L),
            any(),
            eq(DistributedEventType.APPROVAL_STATE),
            eq(Set.of(DistributedSyncAction.DEPLOY))
        );
    }

    @Test
    void should_deploy_a_distributed_state_as_the_primary_node_deployed_it() throws InterruptedException {
        ApprovalStateDeployable deployable = ApprovalStateDeployable.builder()
            .approvalRequestId("req-1")
            .organizationId("org-1")
            .state(STATE)
            .syncAction(SyncAction.DEPLOY)
            .build();
        DistributedEvent event = approvalStateMapper.to(deployable).blockingGet();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(event));

        cut.synchronize(-1L, 100L).test().await().assertComplete();

        verify(approvalStateDeployer).deploy(deployable);
        verify(approvalStateDeployer).doAfterDeployment(deployable);
    }

    @Test
    void should_undeploy_a_distributed_state() throws InterruptedException {
        ApprovalStateDeployable deployable = ApprovalStateDeployable.builder()
            .approvalRequestId("req-1")
            .syncAction(SyncAction.UNDEPLOY)
            .build();
        DistributedEvent event = approvalStateMapper.to(deployable).blockingGet();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(event));

        cut.synchronize(100L, 200L).test().await().assertComplete();

        verify(approvalStateDeployer).undeploy(deployable);
        verify(approvalStateDeployer).doAfterUndeployment(deployable);
    }

    @Test
    void should_skip_a_distributed_state_without_organization() {
        DistributedEvent event = approvalStateMapper
            .to(ApprovalStateDeployable.builder().approvalRequestId("req-1").state(STATE).syncAction(SyncAction.DEPLOY).build())
            .blockingGet();

        assertThat(approvalStateMapper.to(event).blockingGet()).isNull();
    }

    private static LicenseManager licensedLicenseManager() {
        LicenseManager licenseManager = mock(LicenseManager.class);
        License license = mock(License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
        when(license.isFeatureEnabled(any())).thenReturn(true);
        return licenseManager;
    }
}
