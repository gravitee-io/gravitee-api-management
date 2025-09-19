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

import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class ApiDeployer implements Deployer<ApiReactorDeployable> {

    private final ApiManager apiManager;
    private final PlanService planService;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final ApiReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            ReactableApi<?> reactableApi = deployable.reactableApi();
            try {
                apiManager.register(reactableApi);
                log.debug("Api [{}] deployed ", deployable.apiId());
            } catch (Exception e) {
                throw new SyncException(
                    String.format("An error occurred when trying to deploy api %s [%s].", reactableApi.getName(), reactableApi.getId()),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final ApiReactorDeployable deployable) {
        return Completable.fromRunnable(() -> planService.register(deployable)).andThen(
            distributedSyncService.distributeIfNeeded(deployable)
        );
    }

    @Override
    public Completable undeploy(final ApiReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                planService.unregister(deployable);
                apiManager.unregister(deployable.apiId());
                log.debug("Api [{}] undeployed ", deployable.apiId());
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to undeploy api [%s].", deployable.apiId()), e);
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final ApiReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
