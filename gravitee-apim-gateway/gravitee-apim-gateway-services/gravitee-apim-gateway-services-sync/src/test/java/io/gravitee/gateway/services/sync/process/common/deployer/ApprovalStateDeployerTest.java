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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.handlers.api.manager.ApprovalStateManager;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate.ApprovalStateDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateDeployerTest {

    private static final DeployedApprovalState STATE = new DeployedApprovalState(
        "req-1",
        List.of(),
        "env-1",
        "api-1",
        "rule-1",
        "2026-10-01T00:00:00Z",
        "open-1",
        "PENDING",
        2_000L,
        null,
        null,
        null,
        3_000L,
        500L,
        null,
        1_000L,
        3L
    );

    @Mock
    private ApprovalStateManager approvalStateManager;

    @Mock
    private DistributedSyncService distributedSyncService;

    private ApprovalStateDeployer cut;

    @BeforeEach
    void beforeEach() {
        cut = new ApprovalStateDeployer(approvalStateManager, distributedSyncService);
        lenient().when(approvalStateManager.receivesStates("org-1")).thenReturn(true);
    }

    @Test
    void should_deploy_the_state_of_an_organization_that_receives_them() {
        cut.deploy(deployable(SyncAction.DEPLOY)).test().assertComplete();

        verify(approvalStateManager).deploy(STATE);
    }

    /**
     * deploy() only runs once, when the sync receives the state, with no later event to re-stage it if the license
     * changes afterwards; checking it here instead could leave a state permanently unanswerable behind a stale or
     * transient check. The license is checked fresh on every read instead (receivesStates).
     */
    @Test
    void should_not_check_the_license_on_deploy() {
        cut.deploy(deployable(SyncAction.DEPLOY)).test().assertComplete();

        verify(approvalStateManager, never()).receivesStates(any());
    }

    @Test
    void should_report_a_failed_deployment() {
        doThrow(new RuntimeException("error")).when(approvalStateManager).deploy(any());

        cut.deploy(deployable(SyncAction.DEPLOY)).test().assertFailure(SyncException.class);
    }

    @Test
    void should_undeploy_a_state() {
        cut.undeploy(deployable(SyncAction.UNDEPLOY)).test().assertComplete();

        verify(approvalStateManager).undeploy("req-1");
    }

    @Test
    void should_distribute_after_deployment_and_undeployment() {
        ApprovalStateDeployable deployable = deployable(SyncAction.DEPLOY);
        when(distributedSyncService.distributeIfNeeded(deployable)).thenReturn(Completable.complete());

        cut.doAfterDeployment(deployable).test().assertComplete();
        cut.doAfterUndeployment(deployable).test().assertComplete();

        verify(distributedSyncService, times(2)).distributeIfNeeded(deployable);
    }

    /**
     * A state skipped on this gateway because its organization does not receive states must not be told to every other
     * gateway either: distributing it anyway wastes cache writes for an organization this node has already decided not
     * to serve, cluster-wide.
     */
    @Test
    void should_not_distribute_the_state_of_an_organization_that_does_not_receive_them() {
        when(approvalStateManager.receivesStates("org-1")).thenReturn(false);
        ApprovalStateDeployable deployable = deployable(SyncAction.DEPLOY);

        cut.doAfterDeployment(deployable).test().assertComplete();
        cut.doAfterUndeployment(deployable).test().assertComplete();

        verify(distributedSyncService, never()).distributeIfNeeded(any(ApprovalStateDeployable.class));
    }

    private static ApprovalStateDeployable deployable(SyncAction syncAction) {
        return ApprovalStateDeployable.builder()
            .approvalRequestId("req-1")
            .organizationId("org-1")
            .state(syncAction == SyncAction.DEPLOY ? STATE : null)
            .syncAction(syncAction)
            .build();
    }
}
