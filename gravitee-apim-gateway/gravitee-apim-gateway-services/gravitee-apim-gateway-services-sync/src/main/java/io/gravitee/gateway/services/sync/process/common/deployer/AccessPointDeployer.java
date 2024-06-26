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

import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class AccessPointDeployer implements Deployer<AccessPointDeployable> {

    private final AccessPointManager accessPointManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(AccessPointDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                accessPointManager.register(deployable.reactableAccessPoint());
                log.debug("Access point for environment [{}] deployed ", deployable.reactableAccessPoint().getEnvironmentId());
            } catch (Exception e) {
                log.warn(
                    "Access point cannot be registered for environment [{}].",
                    deployable.reactableAccessPoint().getEnvironmentId(),
                    e
                );
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to deploy access point for environment %s.",
                        deployable.reactableAccessPoint().getEnvironmentId()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable undeploy(AccessPointDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                accessPointManager.unregister(deployable.reactableAccessPoint());
            } catch (Exception e) {
                log.warn(
                    "Access point cannot be unregistered for environment [{}].",
                    deployable.reactableAccessPoint().getEnvironmentId(),
                    e
                );
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to undeploy access point for environment %s.",
                        deployable.reactableAccessPoint().getEnvironmentId()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterDeployment(AccessPointDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable doAfterUndeployment(AccessPointDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
