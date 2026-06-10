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
package io.gravitee.apim.core.api_product.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Handles tag lifecycle operations for API Products and their plans.
 */
@DomainService
@RequiredArgsConstructor
@CustomLog
public class ApiProductTagDomainService {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductCrudService apiProductCrudService;
    private final PlanQueryService planQueryService;
    private final PlanCrudService planCrudService;

    /**
     * Remove a deleted tag key from all API Products and their plans in the given environment.
     * Called when an organization-level tag is deleted.
     */
    public void deleteTagFromApiProducts(String environmentId, String tagKey) {
        Set<ApiProduct> products = apiProductQueryService.findByEnvironmentId(environmentId);

        for (ApiProduct product : products) {
            boolean productHasTag = product.getTags() != null && product.getTags().contains(tagKey);
            if (productHasTag) {
                removeTagFromProduct(product, tagKey);
                apiProductCrudService.update(product);
                removeTagFromProductPlans(product.getId(), tagKey);
            }
        }
    }

    private void removeTagFromProduct(ApiProduct product, String tagKey) {
        Set<String> updatedTags = new HashSet<>(product.getTags());
        updatedTags.remove(tagKey);
        log.info("Removing deleted tag [{}] from API Product [{}]", tagKey, product.getId());
        product.setTags(updatedTags.isEmpty() ? null : updatedTags);
    }

    private void removeTagFromProductPlans(String productId, String tagKey) {
        var plans = planQueryService.findAllByReferenceIdAndReferenceType(productId, GenericPlanEntity.ReferenceType.API_PRODUCT);

        for (var plan : plans) {
            var planDef = plan.getPlanDefinitionHttpV4();
            if (planDef == null || planDef.getTags() == null || !planDef.getTags().contains(tagKey)) {
                continue;
            }
            Set<String> updatedTags = planDef
                .getTags()
                .stream()
                .filter(t -> !t.equals(tagKey))
                .collect(Collectors.toSet());
            log.info("Removing deleted tag [{}] from plan [{}] of API Product [{}]", tagKey, plan.getId(), productId);
            planDef.setTags(updatedTags.isEmpty() ? null : updatedTags);
            planCrudService.update(plan);
        }
    }
}
