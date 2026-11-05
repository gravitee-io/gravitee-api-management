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
package io.gravitee.gateway.services.sync.process.distributed.synchronizer.approvalrule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.gateway.services.sync.process.common.deployer.ApprovalRuleDeployer;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApprovalRuleMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule.ApprovalRuleDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedApprovalRuleSynchronizerTest {

    private final ApprovalRuleMapper approvalRuleMapper = new ApprovalRuleMapper(new GraviteeMapper());

    @Mock
    private DistributedEventFetcher eventsFetcher;

    @Mock
    private DeployerFactory deployerFactory;

    @Mock
    private ApprovalRuleDeployer approvalRuleDeployer;

    private DistributedApprovalRuleSynchronizer cut;

    @BeforeEach
    void beforeEach() {
        cut = new DistributedApprovalRuleSynchronizer(
            eventsFetcher,
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            new ThreadPoolExecutor(1, 1, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>()),
            deployerFactory,
            approvalRuleMapper
        );
        when(eventsFetcher.bulkItems()).thenReturn(1);
        lenient().when(deployerFactory.createApprovalRuleDeployer()).thenReturn(approvalRuleDeployer);
        lenient().when(approvalRuleDeployer.deploy(any())).thenReturn(Completable.complete());
        lenient().when(approvalRuleDeployer.doAfterDeployment(any())).thenReturn(Completable.complete());
        lenient().when(approvalRuleDeployer.undeploy(any())).thenReturn(Completable.complete());
        lenient().when(approvalRuleDeployer.doAfterUndeployment(any())).thenReturn(Completable.complete());
        lenient().when(approvalRuleDeployer.commit()).thenReturn(Completable.complete());
    }

    @Test
    void should_not_synchronize_approval_rules_when_no_events() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), eq(DistributedEventType.APPROVAL_RULE), any())).thenReturn(Flowable.empty());

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(approvalRuleDeployer, never()).deploy(any());
        verify(approvalRuleDeployer, never()).undeploy(any());
    }

    @Test
    void should_fetch_only_deploy_events_on_initial_sync() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(eventsFetcher).fetchLatest(eq(-1L), any(), eq(DistributedEventType.APPROVAL_RULE), eq(Set.of(DistributedSyncAction.DEPLOY)));
    }

    @Test
    void should_fetch_deploy_and_undeploy_events_on_incremental_sync() throws InterruptedException {
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.empty());

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(eventsFetcher).fetchLatest(
            any(),
            any(),
            eq(DistributedEventType.APPROVAL_RULE),
            eq(Set.of(DistributedSyncAction.DEPLOY, DistributedSyncAction.UNDEPLOY))
        );
    }

    @Test
    void should_deploy_a_distributed_approval_rule() throws InterruptedException {
        ApprovalRuleDeployable deployable = ApprovalRuleDeployable.builder()
            .approvalRuleId("approval-rule-1")
            .environmentId("env-1")
            .organizationId("org-1")
            .toolPattern("payments.*")
            .toolBindings(List.of(new ToolBinding("api-1", "transfer")))
            .toolEntityIds(List.of("payments.refund"))
            .argumentConditions(List.of(new ArgumentCondition("amount", "GT", "1000")))
            .priority(10)
            .createdAt(1000L)
            .updatedAt(2000L)
            .syncAction(SyncAction.DEPLOY)
            .build();
        DistributedEvent event = approvalRuleMapper.to(deployable).blockingGet();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(event));

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(approvalRuleDeployer).deploy(deployable);
        verify(approvalRuleDeployer).doAfterDeployment(deployable);
    }

    @Test
    void should_commit_once_after_staging_every_rule_of_the_pass() throws InterruptedException {
        List<DistributedEvent> events = IntStream.range(0, 100)
            .mapToObj(i ->
                approvalRuleMapper
                    .to(
                        ApprovalRuleDeployable.builder()
                            .approvalRuleId("approval-rule-" + i)
                            .environmentId("env-1")
                            .organizationId("org-1")
                            .toolPattern("payments")
                            .argumentConditions(List.of())
                            .syncAction(SyncAction.DEPLOY)
                            .build()
                    )
                    .blockingGet()
            )
            .toList();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.fromIterable(events));

        cut.synchronize(-1L, Instant.now().toEpochMilli()).test().await().assertComplete();

        InOrder inOrder = inOrder(approvalRuleDeployer);
        inOrder.verify(approvalRuleDeployer, times(100)).deploy(any());
        inOrder.verify(approvalRuleDeployer, times(1)).commit();
    }

    @Test
    void should_undeploy_a_distributed_approval_rule() throws InterruptedException {
        ApprovalRuleDeployable deployable = ApprovalRuleDeployable.builder()
            .approvalRuleId("approval-rule-1")
            .syncAction(SyncAction.UNDEPLOY)
            .build();
        DistributedEvent event = approvalRuleMapper.to(deployable).blockingGet();
        when(eventsFetcher.fetchLatest(any(), any(), any(), any())).thenReturn(Flowable.just(event));

        cut.synchronize(Instant.now().toEpochMilli(), Instant.now().toEpochMilli()).test().await().assertComplete();

        verify(approvalRuleDeployer).undeploy(deployable);
        verify(approvalRuleDeployer).doAfterUndeployment(deployable);
    }
}
