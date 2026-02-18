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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * When parentId is set, ensures parent exists, is a folder or API, and matches the item's area (create and update).
 */
@RequiredArgsConstructor
public class ParentRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getParentId() != null;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        validateParent(
            item.getParentId(),
            item.getArea(),
            environmentId,
            item.getPublished() != null ? item.getPublished() : false,
            item.getVisibility()
        );
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return toUpdate.getParentId() != null;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        validateParent(
            toUpdate.getParentId(),
            existingItem.getArea(),
            existingItem.getEnvironmentId(),
            toUpdate.getPublished() != null ? toUpdate.getPublished() : false,
            toUpdate.getVisibility()
        );
    }

    /**
     * Shared logic for validating parent; can be used from create and update flows.
     */
    private void validateParent(
        PortalNavigationItemId parentId,
        PortalArea itemArea,
        String environmentId,
        boolean itemPublished,
        PortalVisibility itemVisibility
    ) {
        if (parentId == null) {
            return;
        }
        var parentItem = Optional.of(parentId)
            .map(parentIdVal -> navigationItemsQueryService.findByIdAndEnvironmentId(environmentId, parentIdVal))
            .orElse(null);
        if (parentItem == null) {
            throw new ParentNotFoundException(parentId.toString());
        }
        if (!(parentItem instanceof PortalNavigationItemContainer)) {
            throw new ParentTypeMismatchException(parentId.toString());
        }
        if (!parentItem.getArea().equals(itemArea)) {
            throw new ParentAreaMismatchException(parentId.toString());
        }
        if (itemPublished && !parentItem.getPublished()) {
            throw InvalidPortalNavigationItemDataException.parentMustBePublished(parentId.toString());
        }
        if (PortalVisibility.PUBLIC.equals(itemVisibility) && PortalVisibility.PRIVATE.equals(parentItem.getVisibility())) {
            throw InvalidPortalNavigationItemDataException.parentMustBePublic(parentId.toString());
        }
    }
}
