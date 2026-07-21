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
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.Map;
import java.util.Optional;

public final class ApiProductAncestorValidation {

    private ApiProductAncestorValidation() {}

    public static void ensureNoApiProductInAncestors(PortalNavigationItemId parentId, CreateValidationContext ctx) {
        ensureNoApiProductInAncestors(findApiProductId(parentId, ctx));
    }

    public static void ensureNoApiProductInAncestors(PortalNavigationItemId parentId, UpdateValidationContext ctx) {
        ensureNoApiProductInAncestors(findApiProductId(parentId, ctx));
    }

    private static void ensureNoApiProductInAncestors(Optional<String> apiProductId) {
        if (apiProductId.isPresent()) {
            throw InvalidPortalNavigationItemDataException.parentHierarchyContainsApiProduct();
        }
    }

    public static Optional<String> findApiProductId(PortalNavigationItemId parentId, CreateValidationContext ctx) {
        return findApiProductId(parentId, ctx.itemsById(), ctx.pendingItemsById());
    }

    public static Optional<String> findApiProductId(PortalNavigationItemId parentId, UpdateValidationContext ctx) {
        return findApiProductId(parentId, ctx.itemsById(), Map.of());
    }

    public static Optional<String> findApiProductId(
        PortalNavigationItemId parentId,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById
    ) {
        return findApiProductId(parentId, itemsById, Map.of());
    }

    private static Optional<String> findApiProductId(
        PortalNavigationItemId parentId,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
        Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItemsById
    ) {
        ParentHierarchyValidation.ensureAcyclic(parentId, itemsById, pendingItemsById);

        var currentId = parentId;
        while (currentId != null) {
            var pendingItem = pendingItemsById.get(currentId);
            if (pendingItem != null) {
                if (pendingItem.getType() == PortalNavigationItemType.API_PRODUCT) {
                    return Optional.ofNullable(pendingItem.getApiProductId());
                }
                currentId = pendingItem.getParentId();
                continue;
            }

            var currentItem = itemsById.get(currentId);
            if (currentItem == null) {
                return Optional.empty();
            }
            if (currentItem instanceof PortalNavigationApiProduct apiProduct) {
                return Optional.of(apiProduct.getApiProductId());
            }
            currentId = currentItem.getParentId();
        }
        return Optional.empty();
    }
}
