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

import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.ApiReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import jakarta.annotation.Nullable;

/**
 * Where a page, folder or link ends up when it is placed under a parent, and whether an API owns it there.
 * The documentation of an API is stored against the API, so an item placed under the API's listing takes no
 * stored parent: the listing only displays it.
 *
 * This is the one statement of that rule, for an item being created as for one being moved.
 *
 * @param apiOwner the API that owns an item placed there, null when the place belongs to the portal
 * @param renderedParentId the listing the item is displayed under when that is not its stored parent
 */
public record DocumentationDestination(
    @Nullable ApiReference apiOwner,
    @Nullable PortalNavigationItemId storedParentId,
    @Nullable PortalNavigationItemId renderedParentId
) {
    /**
     * What the rule needs to know about a parent, whether it is stored already or about to be created.
     *
     * @param apiId the API a listing lists, null for any other type
     * @param inApiProduct the parent is an API product or sits below one
     */
    public record Parent(
        PortalNavigationItemId id,
        PortalNavigationItemType type,
        NavigationItemReference owner,
        @Nullable String apiId,
        boolean inApiProduct
    ) {}

    public static DocumentationDestination under(@Nullable Parent parent) {
        if (parent == null) {
            return new DocumentationDestination(null, null, null);
        }
        // A listing below an API product shows the API as a member of the product, not its documentation
        if (!parent.inApiProduct()) {
            if (parent.type() == PortalNavigationItemType.API && parent.apiId() != null) {
                return new DocumentationDestination(new ApiReference(parent.apiId()), null, parent.id());
            }
            if (parent.type() == PortalNavigationItemType.FOLDER && parent.owner() instanceof ApiReference api) {
                return new DocumentationDestination(api, parent.id(), null);
            }
        }
        return new DocumentationDestination(null, parent.id(), null);
    }
}
