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

import io.gravitee.gateway.handlers.api.manager.ApprovalStateManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate.ApprovalStateDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/** Deploys the approval states of the organizations that receive them. */
@RequiredArgsConstructor
@CustomLog
public class ApprovalStateDeployer implements Deployer<ApprovalStateDeployable> {

    private final ApprovalStateManager approvalStateManager;
    private final DistributedSyncService distributedSyncService;

    /**
     * Stages regardless of licensing: this runs once, when the sync receives the state, with no later event to
     * re-stage it if the license changes afterwards, so checking the license here instead could leave a state
     * permanently unanswerable behind a stale or transient check. The license is checked fresh on every read
     * instead ({@link ApprovalStateManager#receivesStates(String)}).
     */
    @Override
    public Completable deploy(final ApprovalStateDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                approvalStateManager.deploy(deployable.state());
                log.debug("Approval state [{}] deployed", deployable.id());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to deploy approval state [%s].", deployable.id()), e);
            }
        });
    }

    /** Not distributed when its organization does not receive approval states: every other gateway skipped it too. */
    @Override
    public Completable doAfterDeployment(final ApprovalStateDeployable deployable) {
        return distributeIfReceived(deployable);
    }

    @Override
    public Completable undeploy(final ApprovalStateDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                approvalStateManager.undeploy(deployable.approvalRequestId());
            } catch (Exception e) {
                throw new SyncException(
                    String.format("An error occurred when trying to undeploy approval state [%s].", deployable.id()),
                    e
                );
            }
        });
    }

    /** Not distributed when its organization does not receive approval states: every other gateway skipped it too. */
    @Override
    public Completable doAfterUndeployment(final ApprovalStateDeployable deployable) {
        return distributeIfReceived(deployable);
    }

    private Completable distributeIfReceived(final ApprovalStateDeployable deployable) {
        if (!approvalStateManager.receivesStates(deployable.organizationId())) {
            return Completable.complete();
        }
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
