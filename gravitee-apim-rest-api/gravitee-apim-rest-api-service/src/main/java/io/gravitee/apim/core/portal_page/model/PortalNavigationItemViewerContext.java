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
import jakarta.annotation.Nullable;
import java.util.Optional;

public class PortalNavigationItemViewerContext {

    private final boolean isPortalMode;
    private final boolean isAuthenticated;

    @Nullable
    private final String userId;

    private PortalNavigationItemViewerContext(boolean isAuthenticated, boolean isPortalView, @Nullable String userId) {
        this.isAuthenticated = isAuthenticated;
        this.isPortalMode = isPortalView;
        this.userId = userId;
    }

    public static PortalNavigationItemViewerContext forPortal(boolean isAuthenticated) {
        return new PortalNavigationItemViewerContext(isAuthenticated, true, null);
    }

    public static PortalNavigationItemViewerContext forPortal(@Nullable String userId) {
        return new PortalNavigationItemViewerContext(userId != null, true, userId);
    }

    public static PortalNavigationItemViewerContext forConsole() {
        return new PortalNavigationItemViewerContext(true, false, null);
    }

    public boolean isPortalMode() {
        return this.isPortalMode;
    }

    public boolean isAuthenticated() {
        return this.isAuthenticated;
    }

    public Optional<String> userId() {
        return Optional.ofNullable(this.userId);
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
