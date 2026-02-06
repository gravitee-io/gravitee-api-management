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

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.event.ApiProductChangedEvent;
import io.gravitee.gateway.handlers.api.event.ApiProductEventType;
import io.gravitee.gateway.handlers.api.manager.ApiProductManager;
import io.gravitee.gateway.handlers.api.registry.ProductPlanDefinitionCache;
import io.gravitee.gateway.services.sync.process.common.mapper.RepositoryPlanToDefinitionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
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
    private final PlanRepository planRepository;
    private final PlanService planService;
    private final DistributedSyncService distributedSyncService;
    private final ProductPlanDefinitionCache productPlanDefinitionCache;
    private final EventManager eventManager;

    @Override
    public Completable deploy(ApiProductReactorDeployable deployable) {
        return Completable.fromRunnable(() -> {
            ReactableApiProduct reactableApiProduct = deployable.reactableApiProduct();
            String apiProductId = deployable.apiProductId();

            try {
                log.debug("Deploying API Product [{}]", apiProductId);
                apiProductManager.register(reactableApiProduct);

                // Register API Product plans in PlanService for subscription validation
                registerApiProductPlans(apiProductId);

                // Emit event to trigger security chain refresh for affected APIs
                emitProductChangedEvent(reactableApiProduct, ApiProductChangedEvent.ChangeType.DEPLOY);

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

    private void registerApiProductPlans(String apiProductId) {
        try {
            Set<Plan> plans = planRepository.findByReferenceIdAndReferenceType(apiProductId, Plan.PlanReferenceType.API_PRODUCT);
            Set<String> planIds = plans.stream().map(Plan::getId).collect(Collectors.toSet());
            planService.registerForApiProduct(apiProductId, planIds);

            List<io.gravitee.definition.model.v4.plan.Plan> definitionPlans = plans
                .stream()
                .filter(p -> p.getStatus() == Plan.Status.PUBLISHED || p.getStatus() == Plan.Status.DEPRECATED)
                .map(RepositoryPlanToDefinitionMapper::toDefinition)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
            if (productPlanDefinitionCache != null) {
                productPlanDefinitionCache.register(apiProductId, definitionPlans);
            }

            log.debug("Registered {} plans for API Product [{}]", planIds.size(), apiProductId);
        } catch (Exception e) {
            log.warn("Failed to load plans for API Product [{}]: {}", apiProductId, e.getMessage());
            planService.registerForApiProduct(apiProductId, Set.of());
            if (productPlanDefinitionCache != null) {
                productPlanDefinitionCache.register(apiProductId, List.of());
            }
        }
    }

    /**
     * Emit ApiProductChangedEvent to trigger security chain refresh for affected APIs.
     *
     * @param reactableApiProduct the API Product that changed
     * @param changeType the type of change (DEPLOY, UPDATE, UNDEPLOY)
     */
    private void emitProductChangedEvent(ReactableApiProduct reactableApiProduct, ApiProductChangedEvent.ChangeType changeType) {
        if (eventManager != null && reactableApiProduct != null) {
            Set<String> apiIds = reactableApiProduct.getApiIds();
            String environmentId = reactableApiProduct.getEnvironmentId();
            if (apiIds != null && !apiIds.isEmpty() && environmentId != null) {
                ApiProductChangedEvent event = new ApiProductChangedEvent(reactableApiProduct.getId(), environmentId, apiIds, changeType);
                try {
                    eventManager.publishEvent(ApiProductEventType.CHANGED, event);
                    log.debug(
                        "Emitted ApiProductChangedEvent for product [{}] affecting {} APIs",
                        reactableApiProduct.getId(),
                        apiIds.size()
                    );
                } catch (Exception e) {
                    log.warn("Failed to emit ApiProductChangedEvent for product [{}]", reactableApiProduct.getId(), e);
                    // Don't fail deployment if event emission fails
                }
            }
        }
    }

    /**
     * Emit ApiProductChangedEvent when product is not available (e.g., during undeploy).
     *
     * @param apiProductId the API Product ID
     * @param environmentId the environment ID
     * @param apiIds the set of API IDs affected
     * @param changeType the type of change
     */
    private void emitProductChangedEvent(
        String apiProductId,
        String environmentId,
        Set<String> apiIds,
        ApiProductChangedEvent.ChangeType changeType
    ) {
        if (eventManager != null && apiProductId != null && environmentId != null && apiIds != null && !apiIds.isEmpty()) {
            ApiProductChangedEvent event = new ApiProductChangedEvent(apiProductId, environmentId, apiIds, changeType);
            try {
                eventManager.publishEvent(ApiProductEventType.CHANGED, event);
                log.debug("Emitted ApiProductChangedEvent for product [{}] affecting {} APIs", apiProductId, apiIds.size());
            } catch (Exception e) {
                log.warn("Failed to emit ApiProductChangedEvent for product [{}]", apiProductId, e);
                // Don't fail undeployment if event emission fails
            }
        }
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

                // Get API IDs before unregistering (needed for event)
                ReactableApiProduct product = apiProductManager.get(apiProductId);
                Set<String> apiIds = product != null && product.getApiIds() != null ? product.getApiIds() : Set.of();
                String environmentId = product != null ? product.getEnvironmentId() : null;

                apiProductManager.unregister(apiProductId);
                planService.unregisterForApiProduct(apiProductId);
                if (productPlanDefinitionCache != null) {
                    productPlanDefinitionCache.unregister(apiProductId);
                }

                // Emit event to trigger security chain refresh for affected APIs
                if (environmentId != null && !apiIds.isEmpty()) {
                    emitProductChangedEvent(apiProductId, environmentId, apiIds, ApiProductChangedEvent.ChangeType.UNDEPLOY);
                }

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
