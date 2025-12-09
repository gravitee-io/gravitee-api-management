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

public class PortalNavigationItemViewerContext {

    private final boolean isPortalMode;
    private final boolean isAuthenticated;

    private PortalNavigationItemViewerContext(boolean isAuthenticated, boolean isPortalView) {
        this.isAuthenticated = isAuthenticated;
        this.isPortalMode = isPortalView;
    }

    public static PortalNavigationItemViewerContext forPortal(boolean isAuthenticated) {
        return new PortalNavigationItemViewerContext(isAuthenticated, true);
    }

    public static PortalNavigationItemViewerContext forConsole() {
        return new PortalNavigationItemViewerContext(true, false);
    }

    public boolean isPortalMode() {
        return this.isPortalMode;
    }

    public boolean isAuthenticated() {
        return this.isAuthenticated;
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
