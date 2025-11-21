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
import io.gravitee.apim.core.portal_page.exception.InvalidUrlFormatException;
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.net.URL;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationItemValidatorService {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalPageContentQueryService pageContentQueryService;

    public void validate(CreatePortalNavigationItem item, String environmentId) {
        validateItem(item, environmentId);
        validateParent(item, environmentId);
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

        if (item.getType() == PortalNavigationItemType.LINK) {
            if (!isValidUrl(item.getUrl())) {
                throw new InvalidUrlFormatException();
            }
        }
    }

    private void validateParent(CreatePortalNavigationItem item, String environmentId) {
        final var parentId = item.getParentId();
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
        if (!parentItem.getArea().equals(item.getArea())) {
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

    public void validateToUpdate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        enforceTypeConsistency(existingItem, toUpdate.getType());

        if (toUpdate.getTitle().isBlank()) {
            throw new InvalidDataException("Title is required for navigation items.");
        }

        if (toUpdate.getType() == PortalNavigationItemType.LINK) {
            if (!isValidUrl(toUpdate.getUrl())) {
                throw new InvalidUrlFormatException();
            }
        }
    }

    private void enforceTypeConsistency(PortalNavigationItem existing, PortalNavigationItemType requestedType) {
        PortalNavigationItemType existingType = switch (existing) {
            case PortalNavigationFolder ignored -> PortalNavigationItemType.FOLDER;
            case PortalNavigationPage ignored -> PortalNavigationItemType.PAGE;
            case PortalNavigationLink ignored -> PortalNavigationItemType.LINK;
        };

        if (existingType != requestedType) {
            throw new InvalidDataException(
                "Navigation item type cannot be changed or is mismatched (expected %s, got %s).".formatted(existingType, requestedType)
            );
        }
    }
}
