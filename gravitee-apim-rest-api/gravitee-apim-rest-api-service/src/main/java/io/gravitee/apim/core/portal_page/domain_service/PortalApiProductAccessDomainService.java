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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalApiProductAccessDomainService {

    private final ApiProductQueryService apiProductQueryService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationApiProductVisibilityDomainService apiProductVisibilityDomainService;
    private final PortalNavigationApiVisibilityDomainService apiVisibilityDomainService;

    public AccessibleApiProduct findAccessible(String environmentId, String apiProductId, PortalNavigationItemViewerContext viewerContext) {
        var apiProduct = apiProductQueryService
            .findById(apiProductId)
            .filter(product -> environmentId.equals(product.getEnvironmentId()))
            .orElseThrow(() -> new ApiProductNotFoundException(apiProductId));

        Set<String> accessibleApiProductIds = apiProductVisibilityDomainService.resolveAccessibleApiProductIds(
            environmentId,
            viewerContext
        );

        var navigationItem = portalNavigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .published(true)
                    .type(PortalNavigationItemType.API_PRODUCT)
                    .apiProductIds(Set.of(apiProductId))
                    .build()
            )
            .stream()
            .filter(PortalNavigationApiProduct.class::isInstance)
            .map(PortalNavigationApiProduct.class::cast)
            .filter(item -> !viewerContext.shouldNotShow(item))
            .filter(item -> !apiProductVisibilityDomainService.isApiProductItemHidden(item, viewerContext, accessibleApiProductIds))
            .filter(item ->
                !apiProductVisibilityDomainService.hasHiddenApiProductAncestor(environmentId, item, viewerContext, accessibleApiProductIds)
            )
            .filter(item -> !apiVisibilityDomainService.hasHiddenApiAncestor(environmentId, item, viewerContext))
            .findFirst()
            .orElseThrow(() -> new ApiProductNotFoundException(apiProductId));

        return new AccessibleApiProduct(apiProduct, navigationItem);
    }

    public record AccessibleApiProduct(ApiProduct apiProduct, PortalNavigationApiProduct navigationItem) {}
}
