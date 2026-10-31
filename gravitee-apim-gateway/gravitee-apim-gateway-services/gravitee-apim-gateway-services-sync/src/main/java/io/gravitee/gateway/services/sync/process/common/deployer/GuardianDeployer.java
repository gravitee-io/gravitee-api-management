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

import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.manager.GuardianManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.guardian.GuardianReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Hands synchronized Guardians to the {@link GuardianManager}. Guardians are not distributed across the cluster yet:
 * every gateway reads them from the repository.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@CustomLog
public class GuardianDeployer implements Deployer<GuardianReactorDeployable> {

    private final GuardianManager guardianManager;

    @Override
    public Completable deploy(final GuardianReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            ReactableGuardian reactableGuardian = deployable.reactableGuardian();
            try {
                guardianManager.register(reactableGuardian);
                log.debug("Guardian [{}] deployed", deployable.guardianId());
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to deploy guardian %s [%s][%s].",
                        reactableGuardian.getName(),
                        reactableGuardian.getId(),
                        reactableGuardian.getDefinition().getVersion()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable undeploy(final GuardianReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            String environmentId = deployable.allEnvironments() ? "all" : deployable.environmentId();
            try {
                if (deployable.allEnvironments()) {
                    guardianManager.unregisterAll(deployable.guardianId());
                } else {
                    guardianManager.unregister(deployable.guardianId(), environmentId);
                }
                log.debug("Guardian [{}] of environment [{}] undeployed", deployable.guardianId(), environmentId);
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to undeploy guardian [%s] of environment [%s].",
                        deployable.guardianId(),
                        environmentId
                    ),
                    e
                );
            }
        });
    }
}
