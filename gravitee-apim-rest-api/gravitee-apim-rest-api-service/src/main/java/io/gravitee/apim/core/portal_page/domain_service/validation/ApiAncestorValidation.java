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
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.Map;

/**
 * Shared validation: ensures no API item appears in the parent hierarchy (used by create and update API rules).
 */
public final class ApiAncestorValidation {

    private ApiAncestorValidation() {}

    public static void ensureNoApiInAncestors(
        PortalNavigationItemId parentId,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById
    ) {
        ensureNoApiInAncestors(parentId, itemsById, Map.of());
    }

    public static void ensureNoApiInAncestors(PortalNavigationItemId parentId, CreateValidationContext ctx) {
        ensureNoApiInAncestors(parentId, ctx.itemsById(), ctx.pendingItemsById());
    }

    private static void ensureNoApiInAncestors(
        PortalNavigationItemId parentId,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
        Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItemsById
    ) {
        if (parentId == null) {
            return;
        }
        ParentHierarchyValidation.ensureAcyclic(parentId, itemsById, pendingItemsById);

        var currentId = parentId;
        while (currentId != null) {
            var pendingItem = pendingItemsById.get(currentId);
            if (pendingItem != null) {
                if (pendingItem.getType() == PortalNavigationItemType.API) {
                    throw InvalidPortalNavigationItemDataException.parentHierarchyContainsApi();
                }
                currentId = pendingItem.getParentId();
                continue;
            }
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
}
