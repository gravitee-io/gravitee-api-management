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
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationItemDomainService {

    private final PortalNavigationItemCrudService crudService;
    private final PortalNavigationItemsQueryService queryService;
    private final PortalPageContentCrudService pageContentCrudService;

    public PortalNavigationItem create(String organizationId, String environmentId, CreatePortalNavigationItem createPortalNavigationItem) {
        final var order = createPortalNavigationItem.getOrder();
        // Order is zero based, so new max order == size()
        final var newMaxOrder = this.retrieveSiblingItems(
            createPortalNavigationItem.getParentId(),
            environmentId,
            createPortalNavigationItem.getArea()
        ).size();
        // Limit the new item's order to at most new max order
        createPortalNavigationItem.setOrder(order == null ? newMaxOrder : Math.min(order, newMaxOrder));

        if (
            createPortalNavigationItem.getType() == PortalNavigationItemType.PAGE &&
            createPortalNavigationItem.getPortalPageContentId() == null
        ) {
            final var defaultPageContent = pageContentCrudService.createDefault(organizationId, environmentId);
            createPortalNavigationItem.setPortalPageContentId(defaultPageContent.getId());
        }

        final var portalNavigationItem = this.crudService.create(
            PortalNavigationItem.from(createPortalNavigationItem, organizationId, environmentId)
        );

        // Update orders of all following sibling items
        this.retrieveSiblingItems(
                portalNavigationItem.getParentId(),
                portalNavigationItem.getEnvironmentId(),
                createPortalNavigationItem.getArea()
            )
            .stream()
            .filter(item -> !Objects.equals(item.getId(), portalNavigationItem.getId()))
            .filter(sibling -> sibling.getOrder() >= portalNavigationItem.getOrder())
            .forEach(followingSibling -> {
                followingSibling.setOrder(followingSibling.getOrder() + 1);
                this.crudService.update(followingSibling);
            });

        return portalNavigationItem;
    }

    private List<PortalNavigationItem> retrieveSiblingItems(PortalNavigationItemId parentId, String environmentId, PortalArea area) {
        return parentId != null
            ? queryService.findByParentIdAndEnvironmentId(environmentId, parentId)
            : queryService.findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, area);
    }

    public void delete(PortalNavigationItem item) {
        // Determine if item is a page and collect content id
        final var contentId = item instanceof io.gravitee.apim.core.portal_page.model.PortalNavigationPage
            ? ((io.gravitee.apim.core.portal_page.model.PortalNavigationPage) item).getPortalPageContentId()
            : null;

        // Reorder siblings at the deleted item's parent level: decrement order for siblings with order > deleted order
        var parentId = item.getParentId();
        var deletedOrder = item.getOrder();
        var siblings = retrieveSiblingItems(parentId, item.getEnvironmentId(), item.getArea());
        var siblingsToUpdate = siblings
            .stream()
            .filter(sibling -> !sibling.getId().equals(item.getId()))
            .filter(sibling -> sibling.getOrder() > deletedOrder)
            .toList();
        siblingsToUpdate.forEach(sibling -> sibling.setOrder(sibling.getOrder() - 1));

        // Perform deletions/updates for the single item
        if (contentId != null) {
            pageContentCrudService.delete(contentId);
        }
        crudService.delete(item.getId());
        siblingsToUpdate.forEach(crudService::update);
    }
}
