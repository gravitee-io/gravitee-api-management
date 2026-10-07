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
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * The documentation an API owns: items stored against the API rather than inside a portal's tree, and
 * the navigation entry that lists the API in a portal.
 */
@DomainService
@RequiredArgsConstructor
public class ApiOwnedNavigationDomainService {

    private static final Set<PortalNavigationItemType> DOCUMENTATION_TYPES = Set.of(
        PortalNavigationItemType.PAGE,
        PortalNavigationItemType.FOLDER,
        PortalNavigationItemType.LINK
    );

    private final PortalNavigationItemsQueryService queryService;
    private final PortalNavigationItemCrudService crudService;

    public List<PortalNavigationItem> findOwnedItems(String environmentId, String apiId) {
        var owner = new NavigationItemReference.ApiReference(apiId);
        var roots = queryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(environmentId, PortalArea.TOP_NAVBAR, owner);

        var ownedItems = new ArrayList<PortalNavigationItem>();
        var visitedIds = new HashSet<PortalNavigationItemId>();
        var toVisit = new ArrayDeque<>(roots);
        while (!toVisit.isEmpty()) {
            var item = toVisit.pop();
            if (!visitedIds.add(item.getId())) {
                continue;
            }
            ownedItems.add(item);
            // Nothing stops an item of the portal, or of another API, from being stored under a folder this API
            // owns: such a child, and whatever sits below it, is not this API's documentation
            queryService
                .findByParentIdAndEnvironmentId(environmentId, item.getId())
                .stream()
                .filter(child -> owner.equals(child.getReference()))
                .forEach(toVisit::push);
        }
        return ownedItems;
    }

    /**
     * One entry per portal that lists the API. An entry under an API product lists the API as a member of
     * that product and is not the API's own listing.
     */
    public List<PortalNavigationApi> findStandaloneListings(String environmentId, String apiId) {
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
            .toList();
    }

    /**
     * The sections an API can be listed under: the published top-level folders of the portal's main
     * navigation. An unpublished section cannot hold a published listing.
     */
    public List<PortalNavigationFolder> findPublishLocations(String environmentId) {
        return queryService
            .findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, PortalArea.TOP_NAVBAR)
            .stream()
            .filter(item -> !(item.getReference() instanceof NavigationItemReference.ApiReference))
            .filter(PortalNavigationFolder.class::isInstance)
            .map(PortalNavigationFolder.class::cast)
            .filter(section -> Boolean.TRUE.equals(section.getPublished()))
            .sorted(Comparator.comparing(PortalNavigationFolder::getOrder))
            .toList();
    }

    public PortalNavigationFolder requirePublishLocation(String environmentId, PortalNavigationItemId sectionId) {
        if (queryService.findByIdAndEnvironmentId(environmentId, sectionId) == null) {
            throw new ParentNotFoundException(sectionId.json());
        }
        return findPublishLocations(environmentId)
            .stream()
            .filter(section -> section.getId().equals(sectionId))
            .findFirst()
            .orElseThrow(() -> InvalidPortalNavigationItemDataException.notAPublishLocation(sectionId.json()));
    }

    /**
     * Publication is a property of the API's documentation as a whole when the API enters or leaves the
     * portal: every item follows, whatever its own flag was.
     */
    public void setPublished(String environmentId, String apiId, boolean published) {
        findOwnedItems(environmentId, apiId)
            .stream()
            .filter(item -> !Boolean.valueOf(published).equals(item.getPublished()))
            .forEach(item -> {
                item.setPublished(published);
                crudService.update(item);
            });
    }

    /**
     * The API is taken from the request URL, where permissions are resolved, while the item id comes from
     * the caller: an item of another API, or of the portal, is reported as missing so that its existence
     * is not revealed.
     */
    public PortalNavigationItem requireOwnedItem(String environmentId, String apiId, PortalNavigationItemId itemId) {
        var item = queryService.findByIdAndEnvironmentId(environmentId, itemId);
        if (item == null || !new NavigationItemReference.ApiReference(apiId).equals(item.getReference())) {
            throw new PortalNavigationItemNotFoundException(itemId.json());
        }
        return item;
    }

    /**
     * Turns an item a caller asked to create into documentation of the API: owned by it, unpublished until
     * the caller publishes it, and only ever under a parent the API owns.
     */
    public CreatePortalNavigationItem claimForApi(String environmentId, String apiId, CreatePortalNavigationItem item) {
        if (!DOCUMENTATION_TYPES.contains(item.getType())) {
            throw InvalidPortalNavigationItemDataException.notApiDocumentationType(String.valueOf(item.getType()));
        }
        if (item.getParentId() != null) {
            requireOwnedItem(environmentId, apiId, item.getParentId());
        }
        return item
            .toBuilder()
            .reference(new NavigationItemReference.ApiReference(apiId))
            .area(PortalArea.TOP_NAVBAR)
            .published(false)
            .build();
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
