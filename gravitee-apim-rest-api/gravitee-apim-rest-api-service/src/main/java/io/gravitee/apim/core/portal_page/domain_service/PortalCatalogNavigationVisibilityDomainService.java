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
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalCatalogNavigationVisibilityDomainService {

    private final PortalNavigationApiProductVisibilityDomainService apiProductVisibilityDomainService;

    public <T extends PortalNavigationItem> List<T> filterVisibleItems(
        List<T> items,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
        PortalNavigationItemViewerContext viewerContext,
        Set<PortalNavigationItemId> accessibleApiNavigationItemIds,
        Set<String> accessibleApiProductIds
    ) {
        return items
            .stream()
            .filter(
                item ->
                    !isHidden(item, viewerContext, accessibleApiNavigationItemIds, accessibleApiProductIds) &&
                    !hasHiddenAncestor(item, itemsById, viewerContext, accessibleApiNavigationItemIds, accessibleApiProductIds)
            )
            .toList();
    }

    public List<PortalNavigationApi> filterStandaloneApis(
        List<PortalNavigationApi> items,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById
    ) {
        return items
            .stream()
            .filter(item -> !hasApiProductAncestor(item, itemsById))
            .toList();
    }

    private boolean hasApiProductAncestor(PortalNavigationItem item, Map<PortalNavigationItemId, PortalNavigationItem> itemsById) {
        Set<PortalNavigationItemId> visited = new HashSet<>();
        PortalNavigationItem current = item;
        while (current != null && current.getParentId() != null && visited.add(current.getId())) {
            current = itemsById.get(current.getParentId());
            if (current instanceof PortalNavigationApiProduct) {
                return true;
            }
        }
        return false;
    }

    private boolean hasHiddenAncestor(
        PortalNavigationItem item,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
        PortalNavigationItemViewerContext viewerContext,
        Set<PortalNavigationItemId> accessibleApiNavigationItemIds,
        Set<String> accessibleApiProductIds
    ) {
        Set<PortalNavigationItemId> visited = new HashSet<>();
        PortalNavigationItem current = item;
        while (current != null && current.getParentId() != null && visited.add(current.getId())) {
            current = itemsById.get(current.getParentId());
            if (current != null && isHidden(current, viewerContext, accessibleApiNavigationItemIds, accessibleApiProductIds)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHidden(
        PortalNavigationItem item,
        PortalNavigationItemViewerContext viewerContext,
        Set<PortalNavigationItemId> accessibleApiNavigationItemIds,
        Set<String> accessibleApiProductIds
    ) {
        if (viewerContext.shouldNotShow(item)) {
            return true;
        }
        if (item instanceof PortalNavigationApi) {
            return !accessibleApiNavigationItemIds.contains(item.getId());
        }
        if (item instanceof PortalNavigationApiProduct apiProduct) {
            return apiProductVisibilityDomainService.isApiProductItemHidden(apiProduct, viewerContext, accessibleApiProductIds);
        }
        return false;
    }
}
