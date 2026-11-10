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
import io.gravitee.gateway.handlers.api.manager.ApiProductChange;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
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
    private final ApiProductSubscriptionRefresher subscriptionRefresher;

    @Override
    public Completable deploy(ApiProductReactorDeployable deployable) {
        ReactableApiProduct reactableApiProduct = deployable.reactableApiProduct();
        String apiProductId = deployable.apiProductId();

        log.debug("Deploying API Product [{}]", apiProductId);

        return apiProductManager
            .register(reactableApiProduct, change -> prepareSubscriptions(deployable, change))
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

    /**
     * Brings the subscription caches in line with the change before the product is published. Removing an API needs no
     * eviction: a product subscription is cached under the product, and the registry stops resolving the removed API
     * to it.
     *
     * <p>Plans are loaded before others are evicted, so a failed load leaves cached the plans of the version the
     * manager puts back. Eviction matches each entry's current plan, so a subscription moved to a loaded plan stays.
     * On failure the previously registered plans are registered again, and the plans the deployed version does not
     * cache are evicted, so a partial load leaves nothing behind. The load error is the one reported.</p>
     */
    private Completable prepareSubscriptions(ApiProductReactorDeployable deployable, ApiProductChange change) {
        String apiProductId = deployable.apiProductId();
        return Completable.defer(() -> {
            Set<String> previouslyRegisteredPlans = planService.apiProductPlans(apiProductId);
            Set<String> registeredPlans = registerApiProductPlans(deployable);
            return subscriptionRefresher
                .refresh(change.planIdsToLoadSubscriptionsFor(), Set.of(deployable.reactableApiProduct().getEnvironmentId()))
                .andThen(Completable.defer(() -> subscriptionRefresher.unregisterByApiProductPlans(apiProductId, change.planIdsToEvict())))
                .onErrorResumeNext(e -> {
                    planService.restoreApiProductPlans(apiProductId, registeredPlans, previouslyRegisteredPlans);
                    return subscriptionRefresher
                        .unregisterByApiProductPlans(apiProductId, change.planIdsToEvictIfLoadFails())
                        .doOnError(evictionError ->
                            log.warn("Unable to evict the partially loaded plans of API Product [{}]", apiProductId, evictionError)
                        )
                        .onErrorComplete()
                        .andThen(Completable.error(e));
                });
        });
    }

    private Set<String> registerApiProductPlans(ApiProductReactorDeployable deployable) {
        Set<String> registeredPlans = planService.register(deployable);
        log.debug("Registered {} plans for API Product [{}]", deployable.subscribablePlans().size(), deployable.apiProductId());
        return registeredPlans;
    }

    private void unregisterApiProductPlans(ApiProductReactorDeployable deployable) {
        planService.unregister(deployable);
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

                // Subscriptions and API keys are cached under the product: one bulk eviction, no
                // repository round-trip and no fan-out over the member APIs.
                subscriptionRefresher.unregisterByApiProduct(apiProductId).blockingAwait();

                apiProductManager.unregister(apiProductId);

                unregisterApiProductPlans(deployable);

                log.debug("API Product [{}] undeployed successfully", apiProductId);
            } catch (SyncException e) {
                // Already carries the step that failed — wrapping it again would only nest the message.
                throw e;
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
