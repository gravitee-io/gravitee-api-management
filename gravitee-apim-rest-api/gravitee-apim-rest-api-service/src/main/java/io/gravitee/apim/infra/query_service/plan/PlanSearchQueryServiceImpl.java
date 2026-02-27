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

import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@CustomLog
public class PlanSearchQueryServiceImpl implements PlanSearchQueryService {

    private final PlanQueryService planQueryService;
    private final PlanCrudService planCrudService;

    public PlanSearchQueryServiceImpl(PlanQueryService planQueryService, PlanCrudService planCrudService) {
        this.planQueryService = planQueryService;
        this.planCrudService = planCrudService;
    }

    @Override
    public List<Plan> searchPlans(String referenceId, String referenceType, PlanQuery query, String authenticatedUser, boolean isAdmin) {
        // authenticatedUser and isAdmin not used here; in PlanSearchService.search they are used only for group visibility (isAdmin bypasses excluded-groups check)
        return planQueryService
            .findAllByReferenceIdAndReferenceType(referenceId, referenceType)
            .stream()
            .filter(p -> applyQueryFilters(p, query))
            .collect(Collectors.toList());
    }

    @Override
    public Plan findByPlanIdAndReferenceIdAndReferenceType(String planId, String referenceId, String referenceType) {
        return planCrudService
            .findByPlanIdAndReferenceIdAndReferenceType(planId, referenceId, referenceType)
            .orElseThrow(() -> new PlanNotFoundException(planId));
    }

    private static boolean applyQueryFilters(Plan p, PlanQuery query) {
        if (query == null) {
            return true;
        }
        if (query.getName() != null && !query.getName().equals(p.getName())) {
            return false;
        }
        if (!CollectionUtils.isEmpty(query.getSecurityType())) {
            if (p.getPlanSecurity() == null || p.getPlanSecurity().getType() == null) {
                return false;
            }
            PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(p.getPlanSecurity().getType());
            if (!query.getSecurityType().contains(planSecurityType)) {
                return false;
            }
        }
        if (!CollectionUtils.isEmpty(query.getStatus())) {
            PlanStatus planStatus = PlanStatus.valueOfLabel(p.getPlanStatus().getLabel());
            if (!query.getStatus().contains(planStatus)) {
                return false;
            }
        }
        if (query.getMode() != null && !query.getMode().equals(p.getPlanMode())) {
            return false;
        }
        return true;
    }
}
