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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalAdapter {
    PortalAdapter INSTANCE = Mappers.getMapper(PortalAdapter.class);

    default Portal toEntity(io.gravitee.repository.management.model.Portal portal) {
        if (portal == null) {
            return null;
        }
        return Portal.of(
            PortalId.of(portal.getId()),
            portal.getEnvironmentId(),
            portal.getOrganizationId(),
            portal.getName(),
            deserializePortalNavigation(portal.getPortalNavigation())
        ).withActiveThemeId(portal.getActiveThemeId());
    }

    default io.gravitee.repository.management.model.Portal toRepository(Portal portal) {
        if (portal == null) {
            return null;
        }
        return io.gravitee.repository.management.model.Portal.builder()
            .id(portal.getId().toString())
            .environmentId(portal.getEnvironmentId())
            .organizationId(portal.getOrganizationId())
            .name(portal.getName())
            .portalNavigation(serializePortalNavigation(portal.getNavigationStructure()))
            .activeThemeId(portal.getActiveThemeId())
            .build();
    }

    TypeReference<Map<PortalArea, List<NavigationPath>>> NAVIGATION_STRUCTURE_MAP = new TypeReference<>() {};
    TypeReference<List<NavigationPath>> NAVIGATION_PATH_LIST = new TypeReference<>() {};

    default String serializePortalNavigation(PortalNavigationStructure structure) {
        if (structure == null || structure.isEmpty()) {
            return null;
        }
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(structure.areas());
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Unexpected error while serializing portal navigation", ioe);
        }
    }

    default PortalNavigationStructure deserializePortalNavigation(String json) {
        if (json == null || json.isBlank()) {
            return PortalNavigationStructure.empty();
        }
        try {
            JsonNode root = GraviteeJacksonMapper.getInstance().readTree(json);
            if (root.isArray()) {
                // Legacy shape: bare array of navigation paths, treated as TOP_NAVBAR entries.
                var legacy = GraviteeJacksonMapper.getInstance().treeToValue(root, NAVIGATION_PATH_LIST);
                return PortalNavigationStructure.ofTopNavbar(legacy);
            }
            Map<PortalArea, List<NavigationPath>> areas = GraviteeJacksonMapper.getInstance().treeToValue(root, NAVIGATION_STRUCTURE_MAP);
            return areas.isEmpty() ? PortalNavigationStructure.empty() : new PortalNavigationStructure(new EnumMap<>(areas));
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Invalid portal navigation JSON: " + json, ioe);
        }
    }
}
