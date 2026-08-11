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
package io.gravitee.apim.core.portal_page.domain_service;

import static java.util.Comparator.comparingInt;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.plan.domain_service.PlanExcludedGroupsDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalApiProductPlanDomainService {

    private final PortalApiProductAccessDomainService portalApiProductAccessDomainService;
    private final PlanSearchQueryService planSearchQueryService;
    private final PlanExcludedGroupsDomainService planExcludedGroupsDomainService;

    public List<Plan> findAccessiblePlans(String environmentId, String apiProductId, PortalNavigationItemViewerContext viewerContext) {
        var accessibleApiProduct = portalApiProductAccessDomainService.findAccessible(environmentId, apiProductId, viewerContext);
        var userId = viewerContext.userId().orElse(null);
        var query = PlanQuery.builder()
            .referenceId(apiProductId)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .status(List.of(PlanStatus.PUBLISHED))
            .build();

        return planSearchQueryService
            .searchPlans(apiProductId, GenericPlanEntity.ReferenceType.API_PRODUCT, query, userId, false)
            .stream()
            .filter(plan ->
                planExcludedGroupsDomainService.isUserAuthorizedToAccessApiProductPlan(
                    accessibleApiProduct.apiProduct(),
                    plan.getExcludedGroups(),
                    userId
                )
            )
            .sorted(comparingInt(Plan::getOrder))
            .toList();
    }
}
