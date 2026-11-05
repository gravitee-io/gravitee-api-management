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

import io.gravitee.gateway.genericevent.GenericEventManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.genericevent.GenericEventDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class GenericEventDeployer implements Deployer<GenericEventDeployable> {

    private final GenericEventManager genericEventManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final GenericEventDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                genericEventManager.upsert(deployable.genericEvent());
                log.debug("Generic event [{}] of kind [{}] deployed", deployable.genericEvent().id(), deployable.genericEvent().kind());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to deploy generic event [%s].", deployable.id()), e);
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final GenericEventDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(final GenericEventDeployable deployable) {
        return Completable.fromRunnable(() -> {
            var genericEvent = deployable.genericEvent();
            try {
                genericEventManager.remove(genericEvent.environmentId(), genericEvent.kind(), genericEvent.id());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to undeploy generic event [%s].", deployable.id()), e);
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final GenericEventDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
