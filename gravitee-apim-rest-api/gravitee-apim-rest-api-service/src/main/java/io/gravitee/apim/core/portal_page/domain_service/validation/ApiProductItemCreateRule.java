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
import io.gravitee.apim.core.portal_page.exception.ApiProductNavigationItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiProductItemCreateRule implements CreatePortalNavigationItemValidationRule {

    private final ApiProductQueryService apiProductQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getType() == PortalNavigationItemType.API_PRODUCT;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        var apiProductId = item.getApiProductId();
        if (apiProductId == null || apiProductId.isBlank()) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("apiProductId");
        }
        if (item.getParentId() == null) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("parentId");
        }
        if (item.getArea() != PortalArea.TOP_NAVBAR) {
            throw InvalidPortalNavigationItemDataException.apiProductMustBeInTopNavbar();
        }

        apiProductQueryService
            .findById(apiProductId)
            .filter(apiProduct -> environmentId.equals(apiProduct.getEnvironmentId()))
            .orElseThrow(() -> new ApiProductNotFoundException(apiProductId));

        boolean alreadyExists = ctx
            .navigationItems()
            .stream()
            .filter(PortalNavigationApiProduct.class::isInstance)
            .map(PortalNavigationApiProduct.class::cast)
            .anyMatch(apiProductItem -> apiProductId.equals(apiProductItem.getApiProductId()));
        if (alreadyExists) {
            throw new ApiProductNavigationItemAlreadyExistsException(apiProductId);
        }

        ApiAncestorValidation.ensureNoApiInAncestors(item.getParentId(), ctx);
        ApiProductAncestorValidation.ensureNoApiProductInAncestors(item.getParentId(), ctx);
    }
}
