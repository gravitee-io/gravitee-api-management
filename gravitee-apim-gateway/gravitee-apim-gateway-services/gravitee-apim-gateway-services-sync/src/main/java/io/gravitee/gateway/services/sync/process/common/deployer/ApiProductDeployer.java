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
import io.gravitee.gateway.handlers.api.registry.ProductPlanDefinitionCache;
import io.gravitee.gateway.services.sync.process.common.mapper.RepositoryPlanToDefinitionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.PlanReferenceType;
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
            Set<Plan> plans = planRepository.findByReferenceIdAndReferenceType(apiProductId, PlanReferenceType.API_PRODUCT);
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
                planService.unregisterForApiProduct(apiProductId);
                if (productPlanDefinitionCache != null) {
                    productPlanDefinitionCache.unregister(apiProductId);
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
