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

import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class SharedPolicyGroupDeployer implements Deployer<SharedPolicyGroupReactorDeployable> {

    private final SharedPolicyGroupManager sharedPolicyGroupManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final SharedPolicyGroupReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            ReactableSharedPolicyGroup reactableSharedPolicyGroup = deployable.reactableSharedPolicyGroup();
            try {
                sharedPolicyGroupManager.register(reactableSharedPolicyGroup);
                log.debug("Shared Policy Group [{}] deployed ", deployable.sharedPolicyGroupId());
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to deploy shared policy group %s [%s][%s].",
                        reactableSharedPolicyGroup.getName(),
                        reactableSharedPolicyGroup.getId(),
                        deployable.reactableSharedPolicyGroup().getDefinition().getVersion()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final SharedPolicyGroupReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(final SharedPolicyGroupReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                sharedPolicyGroupManager.unregister(deployable.sharedPolicyGroupId());
                log.debug("Shared Policy Group [{}] undeployed ", deployable.sharedPolicyGroupId());
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to undeploy shared policy group [%s] [%s].",
                        deployable.sharedPolicyGroupId(),
                        deployable.reactableSharedPolicyGroup().getDefinition().getVersion()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final SharedPolicyGroupReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
