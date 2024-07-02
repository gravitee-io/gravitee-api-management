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

import io.gravitee.gateway.handlers.environmentflow.manager.EnvironmentFlowManager;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.environmentflow.EnvironmentFlowReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class EnvironmentFlowDeployer implements Deployer<EnvironmentFlowReactorDeployable> {

    private final EnvironmentFlowManager environmentFlowManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final EnvironmentFlowReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            ReactableEnvironmentFlow reactableEnvironmentFlow = deployable.reactableEnvironmentFlow();
            try {
                environmentFlowManager.register(reactableEnvironmentFlow);
                log.debug("Environment Flow [{}] deployed ", deployable.environmentFlowId());
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to deploy environment flow %s [%s][%s].",
                        reactableEnvironmentFlow.getName(),
                        reactableEnvironmentFlow.getId(),
                        deployable.reactableEnvironmentFlow().getDefinition().getVersion()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final EnvironmentFlowReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(final EnvironmentFlowReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                environmentFlowManager.unregister(deployable.environmentFlowId());
                log.debug("Environment flow [{}] undeployed ", deployable.environmentFlowId());
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to undeploy environment flow [%s] [%s].",
                        deployable.environmentFlowId(),
                        deployable.reactableEnvironmentFlow().getDefinition().getVersion()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(final EnvironmentFlowReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
