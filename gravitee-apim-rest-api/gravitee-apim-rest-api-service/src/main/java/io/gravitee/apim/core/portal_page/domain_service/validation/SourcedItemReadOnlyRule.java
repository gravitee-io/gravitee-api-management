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
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Sourced items cannot be renamed or moved while their source is kept, and every item below a
 * sourced FOLDER is fully read-only. Removing the source in the same update lifts the restriction.
 * <p>
 * The subtree is closed on the way in as well — create below, or move below — including against a
 * sourced FOLDER declared earlier in the same bulk payload.
 */
@RequiredArgsConstructor
public class SourcedItemReadOnlyRule implements CreatePortalNavigationItemValidationRule, UpdatePortalNavigationItemValidationRule {

    private final SourcedAncestorFinder sourcedAncestorFinder;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getParentId() != null;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        // Walk the pending part of the chain first: the parent may only exist in the same payload
        var parentId = item.getParentId();
        int depth = 0;
        while (parentId != null) {
            if (ValidationDepth.exceeded(depth++, parentId, "the read-only restriction is not enforced above")) {
                return;
            }
            var pendingParent = ctx.pendingItemsById().get(parentId);
            if (pendingParent == null) {
                break;
            }
            if (pendingParent.getSource() != null) {
                throw InvalidPortalNavigationItemDataException.cannotCreateBelowSourcedItem(parentId.json());
            }
            parentId = pendingParent.getParentId();
        }
        sourcedAncestorFinder
            .findSourcedAncestorFrom(environmentId, parentId)
            .ifPresent(ancestor -> {
                throw InvalidPortalNavigationItemDataException.cannotCreateBelowSourcedItem(ancestor.getId().json());
            });
    }

    @Override
    public boolean appliesTo(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem) {
        return existingItem.getSource() != null || existingItem.getParentId() != null || toUpdate.getParentId() != null;
    }

    @Override
    public void validate(UpdatePortalNavigationItem toUpdate, PortalNavigationItem existingItem, UpdateValidationContext ctx) {
        if (
            existingItem.getParentId() != null &&
            sourcedAncestorFinder.findSourcedAncestor(existingItem.getEnvironmentId(), existingItem).isPresent()
        ) {
            throw InvalidPortalNavigationItemDataException.childOfSourcedItemIsReadOnly(existingItem.getId().json());
        }

        // The target position matters as much as the current one
        if (toUpdate.getParentId() != null && !Objects.equals(toUpdate.getParentId(), existingItem.getParentId())) {
            sourcedAncestorFinder
                .findSourcedAncestorFrom(existingItem.getEnvironmentId(), toUpdate.getParentId())
                .ifPresent(ancestor -> {
                    throw InvalidPortalNavigationItemDataException.cannotMoveBelowSourcedItem(ancestor.getId().json());
                });
        }

        if (existingItem.getSource() == null || toUpdate.getSource() == null) {
            return;
        }

        if (
            !Objects.equals(toUpdate.getTitle(), existingItem.getTitle()) ||
            !Objects.equals(toUpdate.getParentId(), existingItem.getParentId())
        ) {
            throw InvalidPortalNavigationItemDataException.sourcedItemCannotBeRenamedOrMoved(existingItem.getId().json());
        }
    }
}
