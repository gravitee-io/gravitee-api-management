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
import io.gravitee.gateway.handlers.api.registry.ApiProductPlanDefinitionCache;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final PlanService planService;
    private final DistributedSyncService distributedSyncService;
    private final ApiProductPlanDefinitionCache apiProductPlanDefinitionCache;
    private final ApiProductSubscriptionRefresher subscriptionRefresher;

    @Override
    public Completable deploy(ApiProductReactorDeployable deployable) {
        ReactableApiProduct reactableApiProduct = deployable.reactableApiProduct();
        String apiProductId = deployable.apiProductId();

        log.debug("Deploying API Product [{}]", apiProductId);
        // Capture old product before register() replaces it (needed to compute removed APIs on update)
        ReactableApiProduct oldProduct = apiProductManager.get(apiProductId);
        Set<String> newApiIds = reactableApiProduct.getApiIds() != null ? reactableApiProduct.getApiIds() : Set.of();

        Completable doBeforeEmit = Completable.complete();
        if (oldProduct != null && oldProduct.getApiIds() != null && !oldProduct.getApiIds().isEmpty()) {
            Set<String> removedApiIds = oldProduct
                .getApiIds()
                .stream()
                .filter(id -> !newApiIds.contains(id))
                .collect(Collectors.toSet());
            if (!removedApiIds.isEmpty()) {
                doBeforeEmit = doBeforeEmit.andThen(
                    subscriptionRefresher.unregisterRemovedApis(
                        removedApiIds,
                        deployable.subscribablePlans(),
                        Set.of(reactableApiProduct.getEnvironmentId())
                    )
                );
            }
        }
        doBeforeEmit = doBeforeEmit.andThen(Completable.fromRunnable(() -> registerApiProductPlans(deployable)));
        doBeforeEmit = doBeforeEmit.andThen(
            subscriptionRefresher.refresh(deployable.subscribablePlans(), Set.of(reactableApiProduct.getEnvironmentId()))
        );

        return apiProductManager
            .register(reactableApiProduct, doBeforeEmit)
            .doOnComplete(() -> log.debug("API Product [{}] deployed successfully", apiProductId))
            .onErrorResumeNext(e ->
                Completable.error(
                    new SyncException(
                        String.format(
                            "An error occurred when trying to deploy API Product %s [%s].",
                            reactableApiProduct.getName(),
                            apiProductId
                        ),
                        e
                    )
                )
            );
    }

    private void registerApiProductPlans(ApiProductReactorDeployable deployable) {
        String apiProductId = deployable.apiProductId();
        planService.register(deployable);
        if (apiProductPlanDefinitionCache != null) {
            apiProductPlanDefinitionCache.register(apiProductId, deployable.definitionPlans());
        }
        log.debug("Registered {} plans for API Product [{}]", deployable.subscribablePlans().size(), apiProductId);
    }

    private void unregisterApiProductPlans(ApiProductReactorDeployable deployable) {
        planService.unregister(deployable);
        if (apiProductPlanDefinitionCache != null) {
            apiProductPlanDefinitionCache.unregister(deployable.apiProductId());
        }
    }

    @Override
    public Completable doAfterDeployment(ApiProductReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }

    @Override
    public Completable undeploy(ApiProductReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            ReactableApiProduct reactableApiProduct = deployable.reactableApiProduct();
            String apiProductId = deployable.apiProductId();

            try {
                log.debug("Undeploying API Product [{}]", apiProductId);

                // Capture product and plans BEFORE unregister removes them (needed for subscription/API key cleanup)
                ReactableApiProduct currentProduct = apiProductManager.get(apiProductId);
                Set<String> subscribablePlans = planService.getSubscribablePlansForApiProduct(apiProductId);

                // Unregister subscriptions and API keys for all APIs in the product (must run before manager unregister)
                if (currentProduct != null && currentProduct.getApiIds() != null && !currentProduct.getApiIds().isEmpty()) {
                    subscriptionRefresher
                        .unregisterRemovedApis(currentProduct.getApiIds(), subscribablePlans, Set.of(currentProduct.getEnvironmentId()))
                        .blockingAwait();
                }

                apiProductManager.unregister(apiProductId);

                unregisterApiProductPlans(deployable);

                log.debug("API Product [{}] undeployed successfully", apiProductId);
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to undeploy API Product %s [%s].",
                        reactableApiProduct != null ? reactableApiProduct.getName() : "unknown",
                        apiProductId
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterUndeployment(ApiProductReactorDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
