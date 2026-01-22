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

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class ApiProductDeployer implements Deployer<ApiProductReactorDeployable> {

    private final ApiProductManager apiProductManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(ApiProductReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            ReactableApiProduct reactableApiProduct = deployable.reactableApiProduct();
            String apiProductId = deployable.apiProductId();

            try {
                log.debug("Deploying API Product [{}]", apiProductId);
                apiProductManager.register(reactableApiProduct);
                log.debug("API Product [{}] deployed successfully", apiProductId);
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to deploy API Product %s [%s].",
                        reactableApiProduct.getName(),
                        apiProductId
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterDeployment(ApiProductReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(ApiProductReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            String apiProductId = deployable.apiProductId();

            try {
                log.debug("Undeploying API Product [{}]", apiProductId);
                apiProductManager.unregister(apiProductId);
                log.debug("API Product [{}] undeployed successfully", apiProductId);
            } catch (Exception e) {
                throw new SyncException(String.format("An error occurred when trying to undeploy API Product [%s].", apiProductId), e);
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(ApiProductReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
