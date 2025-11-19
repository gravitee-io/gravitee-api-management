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
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CreatePortalNavigationItemValidatorService {

    private final PortalNavigationItemsQueryService queryService;

    public void validate(CreatePortalNavigationItem item, String environmentId) {
        validateItem(item, environmentId);
        validateParent(item, environmentId);
    }

    private void validateItem(CreatePortalNavigationItem item, String environmentId) {
        final var itemId = item.getId();
        if (itemId != null) {
            final var existingItem = this.queryService.findByIdAndEnvironmentId(environmentId, itemId);
            if (existingItem != null) {
                throw new ItemAlreadyExistsException(itemId.toString());
            }
        }

        if (item.getArea().equals(PortalArea.HOMEPAGE)) {
            final var existingHomepage = this.queryService.findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, item.getArea());
            if (!existingHomepage.isEmpty()) {
                throw new HomepageAlreadyExistsException();
            }
        }

        if (item.getType() == PortalNavigationItemType.PAGE) {
            final var contentId = item.getPortalPageContentId();
            // TODO check if content exists and create one otherwise, currently assigning a random id to avoid repo level errors
            if (contentId == null) {
                item.setPortalPageContentId(PortalPageContentId.random());
            }
        }
    }

    private void validateParent(CreatePortalNavigationItem item, String environmentId) {
        final var parentId = item.getParentId();
        if (parentId == null) {
            return;
        }

        final var parentItem = this.queryService.findByIdAndEnvironmentId(environmentId, parentId);
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
}
