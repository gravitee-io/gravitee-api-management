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

import io.gravitee.gateway.handlers.api.manager.ApprovalRuleManager;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule.ApprovalRuleDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class ApprovalRuleDeployer implements Deployer<ApprovalRuleDeployable> {

    private final ApprovalRuleManager approvalRuleManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final ApprovalRuleDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                approvalRuleManager.deploy(
                    new DeployedApprovalRule(
                        deployable.approvalRuleId(),
                        deployable.environmentId(),
                        deployable.organizationId(),
                        deployable.toolPattern(),
                        deployable.toolBindings(),
                        deployable.toolEntityIds(),
                        deployable.argumentConditions(),
                        deployable.priority(),
                        deployable.createdAt(),
                        deployable.updatedAt()
                    )
                );
                log.debug("Approval rule [{}] staged for deployment", deployable.id());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to deploy approval rule [%s].", deployable.id()), e);
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final ApprovalRuleDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(final ApprovalRuleDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                approvalRuleManager.undeploy(deployable.environmentId(), deployable.approvalRuleId());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to undeploy approval rule [%s].", deployable.id()), e);
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final ApprovalRuleDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    /** Makes the rules (un)deployed since the last commit visible to the policies, once for the whole sync pass. */
    public Completable commit() {
        return Completable.fromRunnable(approvalRuleManager::commit);
    }
}
