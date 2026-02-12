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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct;

import io.gravitee.gateway.services.sync.process.common.mapper.PlanMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Fetches plans for API Products in bulk during sync, avoiding N+1 repository calls at gateway startup.
 * Plans are set on the deployable before deployment so ApiProductDeployer does not need to fetch them.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class ApiProductPlanAppender {

    private final PlanRepository planRepository;

    /**
     * Bulk-fetch plans for deploy deployables and populate subscribablePlans and definitionPlans on each.
     *
     * @param deployables the deployables to append plans to (DEPLOY action only are processed)
     * @param environments the environments to filter plans by
     * @return the same deployables with plans populated
     */
    public List<ApiProductReactorDeployable> appends(final List<ApiProductReactorDeployable> deployables, final Set<String> environments) {
        if (deployables == null || deployables.isEmpty()) {
            return deployables;
        }
        List<String> apiProductIds = deployables
            .stream()
            .filter(d -> d.syncAction() == SyncAction.DEPLOY)
            .map(ApiProductReactorDeployable::apiProductId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (apiProductIds.isEmpty()) {
            return deployables;
        }
        if (environments == null || environments.isEmpty()) {
            log.warn("No environments provided for API Product plan fetch; plans will be empty");
            clearPlans(deployables);
            return deployables;
        }
        try {
            var plansByApiProduct = planRepository
                .findByReferenceIdsAndReferenceTypeAndEnvironment(apiProductIds, Plan.PlanReferenceType.API_PRODUCT, environments)
                .stream()
                .collect(Collectors.groupingBy(Plan::getReferenceId));
            for (ApiProductReactorDeployable deployable : deployables) {
                if (deployable.syncAction() != SyncAction.DEPLOY) {
                    continue;
                }
                List<Plan> plans = plansByApiProduct.getOrDefault(deployable.apiProductId(), Collections.emptyList());
                deployable.subscribablePlans(plans.stream().map(Plan::getId).collect(Collectors.toSet()));
                deployable.definitionPlans(
                    plans
                        .stream()
                        .filter(p -> p.getStatus() == Plan.Status.PUBLISHED || p.getStatus() == Plan.Status.DEPRECATED)
                        .map(PlanMapper::toDefinition)
                        .filter(Objects::nonNull)
                        .toList()
                );
            }
        } catch (TechnicalException e) {
            log.error("Failed to bulk-fetch plans for API Products [{}]", apiProductIds, e);
            clearPlans(deployables);
        }
        return deployables;
    }

    private void clearPlans(List<ApiProductReactorDeployable> deployables) {
        for (ApiProductReactorDeployable d : deployables) {
            if (d.syncAction() == SyncAction.DEPLOY) {
                d.subscribablePlans(Set.of());
                d.definitionPlans(List.of());
            }
        }
    }
}
