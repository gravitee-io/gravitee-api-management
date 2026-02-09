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
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationItemValidatorService {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalPageContentQueryService pageContentQueryService;

    public void validate(CreatePortalNavigationItem item, String environmentId) {
        validateItem(item, environmentId);
        validateParent(item.getParentId(), item.getArea(), environmentId);
    }

    private void validateItem(CreatePortalNavigationItem item, String environmentId) {
        final var itemId = item.getId();
        if (itemId != null) {
            final var existingItem = this.navigationItemsQueryService.findByIdAndEnvironmentId(environmentId, itemId);
            if (existingItem != null) {
                throw new ItemAlreadyExistsException(itemId.toString());
            }
        }

        if (item.getArea().equals(PortalArea.HOMEPAGE)) {
            final var existingHomepage = this.navigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(
                environmentId,
                item.getArea()
            );
            if (!existingHomepage.isEmpty()) {
                throw new HomepageAlreadyExistsException();
            }
        }

        if (item.getType() == PortalNavigationItemType.PAGE) {
            final var contentId = item.getPortalPageContentId();
            if (contentId != null) {
                final var existingPageContent = this.pageContentQueryService.findById(contentId);
                if (existingPageContent.isEmpty()) {
                    throw new PageContentNotFoundException(contentId.toString());
                }
            }
        }

        if (item.getType() == PortalNavigationItemType.API) {
            if (item.getParentId() == null) {
                throw InvalidPortalNavigationItemDataException.fieldIsEmpty("parentId");
            }

            if (item.getArea() != PortalArea.TOP_NAVBAR) {
                throw InvalidPortalNavigationItemDataException.apiMustBeInTopNavbar();
            }
            if (item.getApiId() == null || item.getApiId().isBlank()) {
                throw InvalidPortalNavigationItemDataException.fieldIsEmpty("apiId");
            }
            List<PortalNavigationItem> navigationItems = fetchAllNavigationItems(environmentId);
            if (isApiIdAlreadyUsed(item.getApiId(), navigationItems)) {
                throw InvalidPortalNavigationItemDataException.apiIdAlreadyExists(item.getApiId());
            }
            ensureNoApiInAncestors(item.getParentId(), navigationItems);
        }

        if (item.getType() == PortalNavigationItemType.LINK) {
            if (!isValidUrl(item.getUrl())) {
                throw new InvalidUrlFormatException();
            }
        }
    }

    private void validateParent(PortalNavigationItemId parentId, PortalArea itemArea, String environmentId) {
        if (parentId == null) {
            return;
        }
        final var parentItem = this.navigationItemsQueryService.findByIdAndEnvironmentId(environmentId, parentId);
        if (parentItem == null) {
            throw new ParentNotFoundException(parentId.toString());
        }
        if (!(parentItem instanceof PortalNavigationFolder)) {
            throw new ParentTypeMismatchException(parentId.toString());
        }
        if (!parentItem.getArea().equals(itemArea)) {
            throw new ParentAreaMismatchException(parentId.toString());
        }
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private List<PortalNavigationItem> fetchAllNavigationItems(String environmentId) {
        var criteria = PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).root(false).build();
        return this.navigationItemsQueryService.search(criteria);
    }

    private boolean isApiIdAlreadyUsed(String apiId, List<PortalNavigationItem> navigationItems) {
        if (apiId == null) {
            return false;
        }
        return navigationItems
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .anyMatch(apiItem -> apiId.equals(apiItem.getApiId()));
    }

    private void ensureNoApiInAncestors(PortalNavigationItemId parentId, List<PortalNavigationItem> navigationItems) {
        if (parentId == null) {
            return;
        }
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById = navigationItems
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
        var currentId = parentId;
        while (currentId != null) {
            var currentItem = itemsById.get(currentId);
            if (currentItem == null) {
                return;
            }
            if (currentItem.getType() == PortalNavigationItemType.API) {
                throw InvalidPortalNavigationItemDataException.parentHierarchyContainsApi();
            }
            currentId = currentItem.getParentId();
        }
    }

    public void validateToUpdate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        enforceTypeConsistency(existingItem, toUpdate.getType());
        var payloadParentId = toUpdate.getParentId();

        validateParent(payloadParentId, existingItem.getArea(), existingItem.getEnvironmentId());
        if (toUpdate.getTitle().isBlank()) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("title");
        }

        if (toUpdate.getType() == PortalNavigationItemType.LINK) {
            if (!isValidUrl(toUpdate.getUrl())) {
                throw new InvalidUrlFormatException();
            }
        }

        if (toUpdate.getType() == PortalNavigationItemType.API) {
            if (payloadParentId == null) {
                throw InvalidPortalNavigationItemDataException.fieldIsEmpty("parentId");
            }
        }
    }

    private void enforceTypeConsistency(PortalNavigationItem existing, PortalNavigationItemType requestedType) {
        PortalNavigationItemType existingType = switch (existing) {
            case PortalNavigationFolder ignored -> PortalNavigationItemType.FOLDER;
            case PortalNavigationPage ignored -> PortalNavigationItemType.PAGE;
            case PortalNavigationLink ignored -> PortalNavigationItemType.LINK;
            case PortalNavigationApi ignored -> PortalNavigationItemType.API;
        };

        if (existingType != requestedType) {
            throw InvalidPortalNavigationItemDataException.typeMismatch(requestedType.toString(), existingType.toString());
        }
    }
}
