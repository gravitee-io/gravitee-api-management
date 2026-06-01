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

import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEnginePort;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.authz.AuthzEntityReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@CustomLog
@RequiredArgsConstructor
public class AuthzEntityDeployer implements Deployer<AuthzEntityReactorDeployable> {

    private final AuthzEnginePort enginePort;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(AuthzEntityReactorDeployable deployable) {
        return enginePort
            .addOrUpdateEntity(deployable.engineUid(), deployable.attributes(), deployable.parents())
            .doOnComplete(() -> log.debug("Authz entity '{}' staged for next commit", deployable.entityId()))
            .doOnError(e -> log.warn("Failed to stage authz entity '{}': {}", deployable.entityId(), e.getMessage()));
    }

    @Override
    public Completable doAfterDeployment(AuthzEntityReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(AuthzEntityReactorDeployable deployable) {
        return enginePort
            .removeEntity(deployable.engineUid())
            .doOnComplete(() -> log.debug("Authz entity '{}' staged for removal on next commit", deployable.entityId()))
            .doOnError(e -> log.warn("Failed to stage authz entity '{}' removal: {}", deployable.entityId(), e.getMessage()));
    }

    @Override
    public Completable doAfterUndeployment(AuthzEntityReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
