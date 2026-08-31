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

import jakarta.annotation.Nullable;

/**
 * Marker interface for PortalNavigationItem subtypes that can contain children.
 * Provides access to the fields needed to resolve parent and rootId relationships.
 */
public interface PortalNavigationItemContainer {
    PortalNavigationItemId getId();

    PortalNavigationItemId getRootId();

    PortalVisibility getVisibility();

    /**
     * Placeholder parent used by materializers when the actual folder hasn't been persisted yet.
     * Returns {@code null} when the given id is {@code null}.
     */
    static @Nullable PortalNavigationItemContainer phantom(@Nullable PortalNavigationItemId id) {
        if (id == null) return null;
        return new PortalNavigationItemContainer() {
            @Override
            public PortalNavigationItemId getId() {
                return id;
            }

            @Override
            public PortalNavigationItemId getRootId() {
                return id;
            }

            @Override
            public PortalVisibility getVisibility() {
                return PortalVisibility.PUBLIC;
            }
        };
    }
}
