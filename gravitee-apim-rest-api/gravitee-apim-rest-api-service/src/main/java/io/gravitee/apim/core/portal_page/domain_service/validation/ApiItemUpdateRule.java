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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * For existing API items: validates parentId required, area TOP_NAVBAR, no API in ancestors, contextual uniqueness, and product membership.
 */
@RequiredArgsConstructor
public class ApiItemUpdateRule implements UpdatePortalNavigationItemValidationRule {

    private final ApiProductQueryService apiProductQueryService;

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return existingItem instanceof PortalNavigationApi;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        var payloadParentId = toUpdate.getParentId();

        if (payloadParentId == null) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("parentId");
        }
        if (existingItem.getArea() != PortalArea.TOP_NAVBAR) {
            throw InvalidPortalNavigationItemDataException.apiMustBeInTopNavbar();
        }
        ApiAncestorValidation.ensureNoApiInAncestors(payloadParentId, ctx.itemsById());

        var targetApiProductId = ApiProductAncestorValidation.findApiProductId(payloadParentId, ctx);
        validateUniqueness(existingItem, targetApiProductId.orElse(null), ctx);
        targetApiProductId.ifPresent(apiProductId ->
            validateApiProductMembership(((PortalNavigationApi) existingItem).getApiId(), apiProductId, existingItem.getEnvironmentId())
        );
    }

    private static void validateUniqueness(PortalNavigationItem existingItem, String targetApiProductId, UpdateValidationContext ctx) {
        var apiId = ((PortalNavigationApi) existingItem).getApiId();
        boolean alreadyExists = ctx
            .navigationItems()
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .filter(apiItem -> !apiItem.getId().equals(existingItem.getId()))
            .filter(apiItem -> apiId.equals(apiItem.getApiId()))
            .anyMatch(apiItem ->
                Objects.equals(
                    targetApiProductId,
                    ApiProductAncestorValidation.findApiProductId(apiItem.getParentId(), ctx.itemsById()).orElse(null)
                )
            );
        if (alreadyExists) {
            throw InvalidPortalNavigationItemDataException.apiIdAlreadyExists(apiId);
        }
    }

    private void validateApiProductMembership(String apiId, String apiProductId, String environmentId) {
        var apiProduct = apiProductQueryService
            .findById(apiProductId)
            .filter(product -> environmentId.equals(product.getEnvironmentId()))
            .orElseThrow(() -> new ApiProductNotFoundException(apiProductId));
        if (apiProduct.getApiIds() == null || !apiProduct.getApiIds().contains(apiId)) {
            throw InvalidPortalNavigationItemDataException.apiDoesNotBelongToApiProduct(apiId, apiProductId);
        }
    }
}
