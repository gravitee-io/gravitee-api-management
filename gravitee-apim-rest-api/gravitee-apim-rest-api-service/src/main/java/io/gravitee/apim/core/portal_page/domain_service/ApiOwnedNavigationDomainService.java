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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * The documentation an API owns: items stored against the API rather than inside a portal's tree, and
 * the navigation entry that lists the API in a portal.
 */
@DomainService
@RequiredArgsConstructor
public class ApiOwnedNavigationDomainService {

    private final PortalNavigationItemsQueryService queryService;

    public List<PortalNavigationItem> findOwnedItems(String environmentId, String apiId) {
        var roots = queryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
            environmentId,
            PortalArea.TOP_NAVBAR,
            new NavigationItemReference.ApiReference(apiId)
        );
        var ownedItems = new ArrayList<PortalNavigationItem>();
        roots.forEach(root -> collectWithDescendants(environmentId, root, ownedItems));
        return ownedItems;
    }

    /**
     * An entry under an API product lists the API as a member of that product and is not the API's own
     * listing.
     */
    public Optional<PortalNavigationApi> findStandaloneListing(String environmentId, String apiId) {
        var criteria = PortalNavigationItemQueryCriteria.builder()
            .environmentId(environmentId)
            .type(PortalNavigationItemType.API)
            .apiIds(Set.of(apiId))
            .build();
        return queryService
            .search(criteria)
            .stream()
            .map(PortalNavigationApi.class::cast)
            .filter(listing -> !hasApiProductAncestor(environmentId, listing))
            .findFirst();
    }

    private void collectWithDescendants(String environmentId, PortalNavigationItem item, List<PortalNavigationItem> collected) {
        collected.add(item);
        queryService
            .findByParentIdAndEnvironmentId(environmentId, item.getId())
            .forEach(child -> collectWithDescendants(environmentId, child, collected));
    }

    private boolean hasApiProductAncestor(String environmentId, PortalNavigationItem item) {
        var current = item;
        while (current != null && current.getParentId() != null) {
            current = queryService.findByIdAndEnvironmentId(environmentId, current.getParentId());
            if (current instanceof PortalNavigationApiProduct) {
                return true;
            }
        }
        return false;
    }
}
