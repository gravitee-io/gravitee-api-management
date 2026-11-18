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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;

/** The portal a Portal API request is scoped to, named by the {@code portalId} path parameter of {@link PortalResource}. */
final class PortalScope {

    static final String PORTAL_ID_PARAM = "portalId";

    private PortalScope() {}

    /**
     * @return empty on the unscoped paths, which keep reading the whole environment
     * @throws PortalNotFoundException when the id is not a UUID: no portal can have it, so it is answered like any unknown portal
     */
    static Optional<PortalId> fromPath(UriInfo uriInfo) {
        return Optional.ofNullable(uriInfo.getPathParameters().getFirst(PORTAL_ID_PARAM)).map(PortalScope::parse);
    }

    /**
     * @throws PortalNavigationItemNotFoundException on a portal-scoped path when the item id is not a UUID: the portal cannot have
     *     such an item. The unscoped paths keep their existing answer.
     */
    static void requireWellFormedItemId(Optional<PortalId> scope, String itemId) {
        if (scope.isEmpty()) {
            return;
        }
        try {
            PortalNavigationItemId.of(itemId);
        } catch (IllegalArgumentException e) {
            throw new PortalNavigationItemNotFoundException(itemId);
        }
    }

    private static PortalId parse(String portalId) {
        try {
            return PortalId.of(portalId);
        } catch (IllegalArgumentException e) {
            throw new PortalNotFoundException(portalId);
        }
    }
}
