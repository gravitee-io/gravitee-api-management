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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.domain_service.OAuth2PlanSecurityConfigurationReader;
import io.gravitee.apim.core.plan.model.OAuth2PlanSecurityConfiguration;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ValidateApiProductPlanOAuth2ResourcesDomainService {

    private final OAuth2PlanSecurityConfigurationReader oauth2PlanSecurityConfigurationReader;

    public void validate(Plan plan, ApiProduct apiProduct) {
        if (!isOAuth2ApiProductPlan(plan)) {
            return;
        }

        OAuth2PlanSecurityConfiguration configuration = oauth2PlanSecurityConfigurationReader.read(plan.getPlanSecurity());
        validateOAuthResource(plan, apiProduct, configuration.getOauthResource());
        if (StringUtils.isNotEmpty(configuration.getOauthCacheResource())) {
            validateCacheResource(plan, apiProduct, configuration.getOauthCacheResource());
        }
    }

    private static boolean isOAuth2ApiProductPlan(Plan plan) {
        if (plan.getReferenceType() != GenericPlanEntity.ReferenceType.API_PRODUCT) {
            return false;
        }
        if (plan.getPlanSecurity() == null || plan.getPlanSecurity().getType() == null) {
            return false;
        }
        return PlanSecurityType.OAUTH2 == PlanSecurityType.valueOfLabel(plan.getPlanSecurity().getType());
    }

    private void validateOAuthResource(Plan plan, ApiProduct apiProduct, String resourceName) {
        String planName = plan.getName();
        if (resourceName == null || resourceName.isBlank()) {
            throw new ValidationDomainException("OAuth2 plan '" + planName + "' requires an OAuth2 resource");
        }

        Resource resource = findResource(apiProduct, resourceName);
        if (resource == null) {
            throw new ValidationDomainException(
                "OAuth2 plan '" + planName + "' references unknown resource '" + resourceName + "' on the API Product"
            );
        }
        if (resource.getType() == null || !resource.getType().startsWith("oauth2")) {
            throw new ValidationDomainException("OAuth2 plan '" + planName + "' resource '" + resourceName + "' is not an OAuth2 resource");
        }
        if (!resource.isEnabled()) {
            throw new ValidationDomainException("OAuth2 plan '" + planName + "' resource '" + resourceName + "' is disabled");
        }
    }

    private void validateCacheResource(Plan plan, ApiProduct apiProduct, String resourceName) {
        String planName = plan.getName();
        Resource resource = findResource(apiProduct, resourceName);
        if (resource == null) {
            throw new ValidationDomainException(
                "OAuth2 plan '" + planName + "' references unknown cache resource '" + resourceName + "' on the API Product"
            );
        }
        if (
            resource.getType() == null ||
            (!Objects.equals("cache", resource.getType()) && !Objects.equals("cache-redis", resource.getType()))
        ) {
            throw new ValidationDomainException(
                "OAuth2 plan '" + planName + "' cache resource '" + resourceName + "' is not a cache resource"
            );
        }
        if (!resource.isEnabled()) {
            throw new ValidationDomainException("OAuth2 plan '" + planName + "' cache resource '" + resourceName + "' is disabled");
        }
    }

    private static Resource findResource(ApiProduct apiProduct, String resourceName) {
        List<Resource> resources = apiProduct.getResources();
        if (resources == null) {
            return null;
        }
        return resources
            .stream()
            .filter(resource -> resourceName.equals(resource.getName()))
            .findFirst()
            .orElse(null);
    }
}
