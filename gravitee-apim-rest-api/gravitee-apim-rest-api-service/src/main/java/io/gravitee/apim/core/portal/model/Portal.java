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
package io.gravitee.apim.core.portal.model;

import jakarta.annotation.Nonnull;
import java.net.URL;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Portal {

    @Nonnull
    private final PortalId id;

    @Nonnull
    private final String environmentId;

    @Nonnull
    private final String organizationId;

    @Nonnull
    private String name;

    @Nonnull
    private PortalNavigationStructure navigationStructure;

    private String activeThemeId;

    // The update method for the default marker is added with the portal update stories.
    @Getter(AccessLevel.NONE)
    private boolean defaultPortal;

    @Nonnull
    private URL url;

    private Portal(
        PortalId id,
        String environmentId,
        String organizationId,
        String name,
        PortalNavigationStructure navigationStructure,
        String activeThemeId,
        boolean defaultPortal,
        URL url
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.organizationId = organizationId;
        this.name = name;
        this.navigationStructure = orEmpty(navigationStructure);
        this.activeThemeId = activeThemeId;
        this.defaultPortal = defaultPortal;
        this.url = url;
    }

    /** New portal served at {@code url}, with a random id and no navigation. */
    public static Portal create(String environmentId, String organizationId, String name, @Nonnull URL url) {
        return newPortal(environmentId, organizationId, name, false, url);
    }

    /** New default portal of the environment, served at {@code url}, with a random id and no navigation. */
    public static Portal createDefault(String environmentId, String organizationId, String name, @Nonnull URL url) {
        return newPortal(environmentId, organizationId, name, true, url);
    }

    /** New portal with a known id (HRID-derived), served at {@code url}, with no navigation. */
    public static Portal create(PortalId id, String environmentId, String organizationId, String name, @Nonnull URL url) {
        return newPortal(id, environmentId, organizationId, name, false, url);
    }

    private static Portal newPortal(String environmentId, String organizationId, String name, boolean defaultPortal, URL url) {
        return newPortal(PortalId.random(), environmentId, organizationId, name, defaultPortal, url);
    }

    private static Portal newPortal(PortalId id, String environmentId, String organizationId, String name, boolean defaultPortal, URL url) {
        return new Portal(
            id,
            environmentId,
            organizationId,
            name,
            PortalNavigationStructure.empty(),
            null,
            defaultPortal,
            Objects.requireNonNull(url, "url")
        );
    }

    /** Reconstitute a persisted portal with all its fields. */
    public static Portal of(
        PortalId id,
        String environmentId,
        String organizationId,
        String name,
        PortalNavigationStructure navigationStructure,
        String activeThemeId,
        boolean defaultPortal,
        URL url
    ) {
        return new Portal(id, environmentId, organizationId, name, navigationStructure, activeThemeId, defaultPortal, url);
    }

    public boolean isDefault() {
        return defaultPortal;
    }

    public void update(String name, PortalNavigationStructure navigationStructure, String activeThemeId) {
        this.name = name;
        this.navigationStructure = orEmpty(navigationStructure);
        this.activeThemeId = activeThemeId;
    }

    public void changeUrl(@Nonnull URL url) {
        this.url = Objects.requireNonNull(url, "url");
    }

    /** @deprecated Portal is a mutable entity: use {@link #update(String, PortalNavigationStructure, String)}. */
    @Deprecated
    public Portal withNavigationStructure(PortalNavigationStructure navigationStructure) {
        return new Portal(
            this.id,
            this.environmentId,
            this.organizationId,
            this.name,
            navigationStructure,
            this.activeThemeId,
            this.defaultPortal,
            this.url
        );
    }

    /** @deprecated Portal is a mutable entity: use {@link #update(String, PortalNavigationStructure, String)}. */
    @Deprecated
    public Portal withActiveThemeId(String activeThemeId) {
        return new Portal(
            this.id,
            this.environmentId,
            this.organizationId,
            this.name,
            this.navigationStructure,
            activeThemeId,
            this.defaultPortal,
            this.url
        );
    }

    private static PortalNavigationStructure orEmpty(PortalNavigationStructure navigationStructure) {
        return navigationStructure == null ? PortalNavigationStructure.empty() : navigationStructure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Portal that = (Portal) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
