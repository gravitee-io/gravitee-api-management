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
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * For API type: parentId required, area TOP_NAVBAR, apiId required, apiId not already used, no API in ancestors.
 */
@RequiredArgsConstructor
public class ApiItemCreateRule implements CreatePortalNavigationItemValidationRule {

    private final ApiProductQueryService apiProductQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getType() == PortalNavigationItemType.API;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        PortalNavigationItemId parentId = item.getParentId();
        PortalArea itemArea = item.getArea();
        String apiId = item.getApiId();
        List<PortalNavigationItem> navigationItems = ctx.navigationItems();
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById = ctx.itemsById();

        if (parentId == null) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("parentId");
        }
        if (itemArea != PortalArea.TOP_NAVBAR) {
            throw InvalidPortalNavigationItemDataException.apiMustBeInTopNavbar();
        }
        if (apiId == null || apiId.isBlank()) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("apiId");
        }
        var targetApiProductId = ApiProductAncestorValidation.findApiProductId(parentId, ctx);
        if (isApiIdAlreadyUsed(apiId, targetApiProductId.orElse(null), navigationItems, itemsById)) {
            throw InvalidPortalNavigationItemDataException.apiIdAlreadyExists(apiId);
        }
        ApiAncestorValidation.ensureNoApiInAncestors(parentId, ctx);
        targetApiProductId.ifPresent(apiProductId -> validateApiProductMembership(apiId, apiProductId, environmentId));
    }

    private static boolean isApiIdAlreadyUsed(
        @NonNull String apiId,
        String targetApiProductId,
        List<PortalNavigationItem> navigationItems,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById
    ) {
        return navigationItems
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .filter(apiItem -> apiId.equals(apiItem.getApiId()))
            .anyMatch(apiItem ->
                Objects.equals(
                    targetApiProductId,
                    ApiProductAncestorValidation.findApiProductId(apiItem.getParentId(), itemsById).orElse(null)
                )
            );
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
