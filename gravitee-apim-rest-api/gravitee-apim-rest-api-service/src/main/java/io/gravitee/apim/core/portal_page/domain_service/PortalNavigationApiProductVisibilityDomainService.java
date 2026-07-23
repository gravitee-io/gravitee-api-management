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
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationApiProductVisibilityDomainService {

    private final PortalNavigationItemsQueryService queryService;
    private final ApiProductAccessibleIdsDomainService apiProductAccessibleIdsDomainService;

    public boolean isApiProductItemHidden(
        String environmentId,
        PortalNavigationApiProduct item,
        PortalNavigationItemViewerContext viewerContext
    ) {
        if (!viewerContext.isPortalMode() || PortalVisibility.PUBLIC.equals(item.getVisibility())) {
            return false;
        }
        if (!viewerContext.isAuthenticated()) {
            return true;
        }
        return viewerContext
            .userId()
            .map(userId ->
                !apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(environmentId, userId).contains(item.getApiProductId())
            )
            .orElse(true);
    }

    public Set<String> resolveAccessibleApiProductIds(String environmentId, PortalNavigationItemViewerContext viewerContext) {
        if (!viewerContext.isPortalMode()) {
            return Set.of();
        }
        return viewerContext
            .userId()
            .map(userId -> apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(environmentId, userId))
            .orElse(Set.of());
    }

    public boolean isApiProductItemHidden(
        PortalNavigationApiProduct item,
        PortalNavigationItemViewerContext viewerContext,
        Set<String> accessibleApiProductIds
    ) {
        if (!viewerContext.isPortalMode() || PortalVisibility.PUBLIC.equals(item.getVisibility())) {
            return false;
        }
        return !viewerContext.isAuthenticated() || !accessibleApiProductIds.contains(item.getApiProductId());
    }

    public boolean hasHiddenApiProductAncestor(
        String environmentId,
        PortalNavigationItem item,
        PortalNavigationItemViewerContext viewerContext
    ) {
        return hasHiddenApiProductAncestor(environmentId, item, viewerContext, apiProductAncestor ->
            isApiProductItemHidden(environmentId, apiProductAncestor, viewerContext)
        );
    }

    public boolean hasHiddenApiProductAncestor(
        String environmentId,
        PortalNavigationItem item,
        PortalNavigationItemViewerContext viewerContext,
        Set<String> accessibleApiProductIds
    ) {
        return hasHiddenApiProductAncestor(environmentId, item, viewerContext, apiProductAncestor ->
            isApiProductItemHidden(apiProductAncestor, viewerContext, accessibleApiProductIds)
        );
    }

    private boolean hasHiddenApiProductAncestor(
        String environmentId,
        PortalNavigationItem item,
        PortalNavigationItemViewerContext viewerContext,
        Predicate<PortalNavigationApiProduct> isHidden
    ) {
        if (!viewerContext.isPortalMode()) {
            return false;
        }

        Set<PortalNavigationItemId> visited = new HashSet<>();
        PortalNavigationItem current = item;
        while (current != null && current.getParentId() != null && visited.add(current.getId())) {
            current = queryService.findByIdAndEnvironmentId(environmentId, current.getParentId());
            if (current instanceof PortalNavigationApiProduct apiProductAncestor && isHidden.test(apiProductAncestor)) {
                return true;
            }
        }
        return false;
    }
}
