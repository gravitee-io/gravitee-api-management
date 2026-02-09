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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationItemDomainService {

    private final PortalNavigationItemCrudService crudService;
    private final PortalNavigationItemsQueryService queryService;
    private final PortalPageContentCrudService pageContentCrudService;
    private final ApiCrudService apiCrudService;

    public PortalNavigationItem create(String organizationId, String environmentId, CreatePortalNavigationItem createPortalNavigationItem) {
        int sanitizedOrder = this.sanitizeOrderForInsertion(
            createPortalNavigationItem.getParentId(),
            environmentId,
            createPortalNavigationItem.getArea(),
            createPortalNavigationItem.getOrder()
        );
        createPortalNavigationItem.setOrder(sanitizedOrder);

        if (
            createPortalNavigationItem.getType() == PortalNavigationItemType.PAGE &&
            createPortalNavigationItem.getPortalPageContentId() == null
        ) {
            final var defaultPageContent = pageContentCrudService.createDefault(organizationId, environmentId);
            createPortalNavigationItem.setPortalPageContentId(defaultPageContent.getId());
        }

        if (createPortalNavigationItem.getType() == PortalNavigationItemType.API) {
            Api api = apiCrudService.get(createPortalNavigationItem.getApiId());
            createPortalNavigationItem.setTitle(api.getName());
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

    public PortalNavigationItem update(UpdatePortalNavigationItem toUpdate, PortalNavigationItem originalItem) {
        final Integer originalOrder = originalItem.getOrder();
        final PortalNavigationItemId originalParentId = originalItem.getParentId();

        boolean isMoveToNewParent = !Objects.equals(originalParentId, toUpdate.getParentId());

        int sanitizedOrder = isMoveToNewParent
            ? this.sanitizeOrderForInsertion(
                toUpdate.getParentId(),
                originalItem.getEnvironmentId(),
                originalItem.getArea(),
                toUpdate.getOrder()
            )
            : this.sanitizeOrderForReordering(
                toUpdate.getParentId(),
                originalItem.getEnvironmentId(),
                originalItem.getArea(),
                toUpdate.getOrder()
            );

        toUpdate.setOrder(sanitizedOrder);

        originalItem.update(toUpdate);

        var updatedItem = crudService.update(originalItem);

        List<PortalNavigationItem> siblingsToUpdate = new ArrayList<>();
        if (!Objects.equals(originalParentId, updatedItem.getParentId())) {
            siblingsToUpdate.addAll(this.reorderDestinationSiblingsOnInsertion(updatedItem));
            siblingsToUpdate.addAll(this.reorderOriginSiblings(originalOrder, updatedItem, originalParentId));
        } else if (!Objects.equals(originalOrder, updatedItem.getOrder())) {
            siblingsToUpdate.addAll(this.reorderDestinationSiblings(originalOrder, updatedItem));
        }
        siblingsToUpdate.forEach(crudService::update);

        return updatedItem;
    }

    private int sanitizeOrderForReordering(PortalNavigationItemId parentId, String environmentId, PortalArea area, Integer order) {
        final var siblingItems = this.retrieveSiblingItems(parentId, environmentId, area);
        final var maxOrder = Math.max(0, siblingItems.size() - 1);
        if (Objects.isNull(order)) return maxOrder;
        return Math.min(order, maxOrder);
    }

    private int sanitizeOrderForInsertion(PortalNavigationItemId parentId, String environmentId, PortalArea area, Integer order) {
        final var newMaxOrder = this.retrieveSiblingItems(parentId, environmentId, area).size();
        if (Objects.isNull(order)) return newMaxOrder;

        return Math.min(order, newMaxOrder);
    }

    private List<PortalNavigationItem> reorderOriginSiblings(
        Integer originalOrder,
        PortalNavigationItem updatedItem,
        PortalNavigationItemId originalParentId
    ) {
        return this.retrieveSiblingItems(originalParentId, updatedItem.getEnvironmentId(), updatedItem.getArea())
            .stream()
            .filter(sibling -> sibling.getOrder() > originalOrder)
            .peek(sibling -> {
                sibling.setOrder(sibling.getOrder() - 1);
            })
            .toList();
    }

    private List<PortalNavigationItem> reorderDestinationSiblingsOnInsertion(PortalNavigationItem updatedItem) {
        return this.retrieveSiblingItems(updatedItem.getParentId(), updatedItem.getEnvironmentId(), updatedItem.getArea())
            .stream()
            .filter(sibling -> !Objects.equals(sibling.getId(), updatedItem.getId()) && sibling.getOrder() >= updatedItem.getOrder())
            .peek(sibling -> {
                sibling.setOrder(sibling.getOrder() + 1);
            })
            .toList();
    }

    private List<PortalNavigationItem> reorderDestinationSiblings(Integer originalOrder, PortalNavigationItem updatedItem) {
        Predicate<PortalNavigationItem> shouldBeDecremented = sibling ->
            sibling.getOrder() > originalOrder && sibling.getOrder() <= updatedItem.getOrder();

        Predicate<PortalNavigationItem> shouldBeIncremented = sibling ->
            sibling.getOrder() >= updatedItem.getOrder() && sibling.getOrder() < originalOrder;

        return this.retrieveSiblingItems(updatedItem.getParentId(), updatedItem.getEnvironmentId(), updatedItem.getArea())
            .stream()
            .filter(
                sibling ->
                    !Objects.equals(sibling.getId(), updatedItem.getId()) &&
                    (shouldBeDecremented.test(sibling) || shouldBeIncremented.test(sibling))
            )
            .peek(sibling -> {
                if (shouldBeDecremented.test(sibling)) {
                    sibling.setOrder(sibling.getOrder() - 1);
                } else if (shouldBeIncremented.test(sibling)) {
                    sibling.setOrder(sibling.getOrder() + 1);
                }
            })
            .toList();
    }
}
