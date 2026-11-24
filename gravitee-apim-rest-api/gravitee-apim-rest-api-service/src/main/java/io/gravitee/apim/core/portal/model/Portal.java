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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Portal {

    /** The order the environment's portals are offered in. */
    public static final Comparator<Portal> DEFAULT_FIRST_THEN_BY_NAME = Comparator.comparing((Portal portal) -> !portal.isDefault())
        .thenComparing(Portal::getName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(portal -> portal.getId().toString());

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

    // Before the console could create portals, every portal that is not the default one came from the Automation API.
    private final boolean createdFromConsole;

    /** The access point this portal is served at; null while its environment has no usable portal URL. */
    private String accessPointId;

    private Portal(
        PortalId id,
        String environmentId,
        String organizationId,
        String name,
        PortalNavigationStructure navigationStructure,
        String activeThemeId,
        boolean defaultPortal,
        boolean createdFromConsole,
        String accessPointId
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.organizationId = organizationId;
        this.name = name;
        this.navigationStructure = orEmpty(navigationStructure);
        this.activeThemeId = activeThemeId;
        this.defaultPortal = defaultPortal;
        this.createdFromConsole = createdFromConsole;
        this.accessPointId = accessPointId;
    }

    /** New portal served at this access point, with a random id and no navigation. */
    public static Portal create(String environmentId, String organizationId, String name, @Nonnull String accessPointId) {
        return newPortal(environmentId, organizationId, name, false, accessPointId);
    }

    /** New default portal of the environment, served at this access point, with a random id and no navigation. */
    public static Portal createDefault(String environmentId, String organizationId, String name, @Nonnull String accessPointId) {
        return newPortal(environmentId, organizationId, name, true, accessPointId);
    }

    /** New portal with a known id (HRID-derived), served at this access point, with no navigation. */
    public static Portal create(PortalId id, String environmentId, String organizationId, String name, @Nonnull String accessPointId) {
        return newPortal(id, environmentId, organizationId, name, false, false, accessPointId);
    }

    /** New portal created from the console, served at this access point, with a random id and no navigation: the console manages it. */
    public static Portal createFromConsole(String environmentId, String organizationId, String name, @Nonnull String accessPointId) {
        return newPortal(PortalId.random(), environmentId, organizationId, name, false, true, accessPointId);
    }

    private static Portal newPortal(String environmentId, String organizationId, String name, boolean defaultPortal, String accessPointId) {
        return newPortal(PortalId.random(), environmentId, organizationId, name, defaultPortal, false, accessPointId);
    }

    private static Portal newPortal(
        PortalId id,
        String environmentId,
        String organizationId,
        String name,
        boolean defaultPortal,
        boolean createdFromConsole,
        String accessPointId
    ) {
        return new Portal(
            id,
            environmentId,
            organizationId,
            name,
            PortalNavigationStructure.empty(),
            null,
            defaultPortal,
            createdFromConsole,
            Objects.requireNonNull(accessPointId, "accessPointId")
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
        String accessPointId
    ) {
        return of(id, environmentId, organizationId, name, navigationStructure, activeThemeId, defaultPortal, false, accessPointId);
    }

    /** Reconstitute a persisted portal with all its fields, including whether the console created it. */
    public static Portal of(
        PortalId id,
        String environmentId,
        String organizationId,
        String name,
        PortalNavigationStructure navigationStructure,
        String activeThemeId,
        boolean defaultPortal,
        boolean createdFromConsole,
        String accessPointId
    ) {
        return new Portal(
            id,
            environmentId,
            organizationId,
            name,
            navigationStructure,
            activeThemeId,
            defaultPortal,
            createdFromConsole,
            accessPointId
        );
    }

    /** Keeps the portals whose name contains {@code query}, ignoring case; null or blank keeps them all. */
    public static Predicate<Portal> nameFilter(String query) {
        if (query == null || query.isBlank()) {
            return portal -> true;
        }
        var needle = query.strip().toLowerCase(Locale.ROOT);
        return portal -> portal.getName().toLowerCase(Locale.ROOT).contains(needle);
    }

    /**
     * @param environmentPortals all the portals of one environment
     * @return its default portal, else its only portal: an environment managed by automation has no default one
     */
    public static Optional<Portal> defaultOrOnly(List<Portal> environmentPortals) {
        return environmentPortals
            .stream()
            .filter(Portal::isDefault)
            .findFirst()
            .or(() -> environmentPortals.size() == 1 ? Optional.of(environmentPortals.getFirst()) : Optional.empty());
    }

    public boolean isDefault() {
        return defaultPortal;
    }

    public void update(String name, PortalNavigationStructure navigationStructure, String activeThemeId) {
        this.name = name;
        this.navigationStructure = orEmpty(navigationStructure);
        this.activeThemeId = activeThemeId;
    }

    public void linkAccessPoint(@Nonnull String accessPointId) {
        this.accessPointId = Objects.requireNonNull(accessPointId, "accessPointId");
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
            this.createdFromConsole,
            this.accessPointId
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
            this.createdFromConsole,
            this.accessPointId
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
