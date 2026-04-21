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
package io.gravitee.apim.core.portal_page.model;

import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import java.util.Objects;

public record PortalNavigationItemViewerContext(boolean isPortalMode, boolean isAuthenticated, String userId) {
    public PortalNavigationItemViewerContext {
        if (isPortalMode && isAuthenticated) {
            Objects.requireNonNull(userId, "userId must be set for authenticated portal users");
        }
    }

    public static PortalNavigationItemViewerContext forPortal(boolean isAuthenticated, String userId) {
        return new PortalNavigationItemViewerContext(true, isAuthenticated, userId);
    }

    public static PortalNavigationItemViewerContext forConsole() {
        return new PortalNavigationItemViewerContext(false, true, null);
    }

    public boolean shouldNotShow(PortalNavigationItem item) {
        if (!this.isPortalMode) {
            return false;
        }

        if (Boolean.FALSE.equals(item.getPublished())) {
            return true;
        }

        return PortalVisibility.PRIVATE.equals(item.getVisibility()) && !this.isAuthenticated;
    }

    public void validateAccess(PortalNavigationItem item) {
        if (shouldNotShow(item)) {
            throw new PortalNavigationItemNotFoundException(item.getId().json());
        }
    }
}
