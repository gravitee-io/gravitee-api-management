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
import java.util.HashSet;
import java.util.Map;

public final class ParentHierarchyValidation {

    private ParentHierarchyValidation() {}

    public static void ensureAcyclic(
        PortalNavigationItemId parentId,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
        Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItemsById
    ) {
        ensureAcyclic(null, parentId, itemsById, pendingItemsById);
    }

    public static void ensureAcyclic(
        PortalNavigationItemId itemId,
        PortalNavigationItemId parentId,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
        Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItemsById
    ) {
        var visited = new HashSet<PortalNavigationItemId>();
        if (itemId != null) {
            visited.add(itemId);
        }

        var currentId = parentId;
        while (currentId != null) {
            if (!visited.add(currentId)) {
                throw InvalidPortalNavigationItemDataException.cyclicParentHierarchy();
            }

            var pendingItem = pendingItemsById.get(currentId);
            if (pendingItem != null) {
                currentId = pendingItem.getParentId();
                continue;
            }

            var currentItem = itemsById.get(currentId);
            if (currentItem == null) {
                return;
            }
            currentId = currentItem.getParentId();
        }
    }
}
