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
package io.gravitee.apim.infra.query_service.plan;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.ApiProductPlanSearchQueryService;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@CustomLog
public class ApiProductPlanSearchQueryServiceImpl implements ApiProductPlanSearchQueryService {

    private final ApiProductsRepository apiProductsRepository;
    private final PlanRepository planRepository;

    public ApiProductPlanSearchQueryServiceImpl(
        @Lazy final ApiProductsRepository apiProductRepository,
        @Lazy final PlanRepository planRepository
    ) {
        this.apiProductsRepository = apiProductRepository;
        this.planRepository = planRepository;
    }

    @Override
    public List<Plan> searchForApiProductPlans(String apiProductId, PlanQuery query, String authenticatedUser, boolean isAdmin) {
        return findByApiProduct(query.getReferenceId())
            .stream()
            .filter(p -> {
                boolean filtered = true;
                if (query.getName() != null) {
                    filtered = query.getName().equals(p.getName());
                }
                if (filtered && !CollectionUtils.isEmpty(query.getSecurityType())) {
                    if (p.getPlanSecurity() == null || p.getPlanSecurity().getType() == null) {
                        return false;
                    }
                    PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(p.getPlanSecurity().getType());
                    filtered = query.getSecurityType().contains(planSecurityType);
                }
                if (filtered && !CollectionUtils.isEmpty(query.getStatus())) {
                    PlanStatus planStatus = PlanStatus.valueOfLabel(p.getPlanStatus().getLabel());
                    filtered = query.getStatus().contains(planStatus);
                }
                if (filtered && query.getMode() != null) {
                    filtered = query.getMode().equals(p.getPlanMode());
                }
                return filtered;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Plan findByPlanIdIdForApiProduct(String planId, String apiProductId) {
        try {
            log.debug("Find plan by id : {}", planId);
            return planRepository
                .findByIdAndReferenceIdAndReferenceType(
                    planId,
                    apiProductId,
                    io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT
                )
                .map(PlanAdapter.INSTANCE::fromRepository)
                .orElseThrow(() -> new PlanNotFoundException(planId));
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(String.format("An error occurs while trying to find a plan by id: %s", planId), ex);
        }
    }

    private Set<Plan> findByApiProduct(final String apiProductId) {
        try {
            log.debug("Find plan by api product : {}", apiProductId);
            Optional<ApiProduct> apiOptional = apiProductsRepository.findById(apiProductId);
            if (apiOptional.isPresent()) {
                return planRepository
                    .findByReferenceIdAndReferenceType(
                        apiProductId,
                        io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT
                    )
                    .stream()
                    .map(PlanAdapter.INSTANCE::fromRepository)
                    .collect(Collectors.toSet());
            } else {
                return Set.of();
            }
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find a plan by api product: %s", apiProductId),
                ex
            );
        }
    }
}
