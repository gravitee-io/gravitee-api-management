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

import io.gravitee.apim.core.portal_page.model.*;
import java.util.HashMap;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalNavigationItemAdapter {
    PortalNavigationItemAdapter INSTANCE = Mappers.getMapper(PortalNavigationItemAdapter.class);

    com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    default PortalNavigationItem toEntity(io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem.getType()) {
            case FOLDER -> portalNavigationFolderFromRepository(portalNavigationItem);
            case PAGE -> portalNavigationPageFromRepository(portalNavigationItem);
            case LINK -> portalNavigationLinkFromRepository(portalNavigationItem);
        };
    }

    default PortalArea mapArea(io.gravitee.repository.management.model.PortalNavigationItem.Area area) {
        return switch (area) {
            case HOMEPAGE -> PortalArea.HOMEPAGE;
            case TOP_NAVBAR -> PortalArea.TOP_NAVBAR;
        };
    }

    default io.gravitee.repository.management.model.PortalNavigationItem.Area mapAreaReverse(PortalArea area) {
        return switch (area) {
            case HOMEPAGE -> io.gravitee.repository.management.model.PortalNavigationItem.Area.HOMEPAGE;
            case TOP_NAVBAR -> io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR;
        };
    }

    @Mapping(target = "id", expression = "java(PortalPageNavigationId.of(portalNavigationItem.getId()))")
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "environmentId", source = "environmentId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "area", expression = "java(mapArea(portalNavigationItem.getArea()))")
    @Mapping(
        target = "parentId",
        expression = "java(portalNavigationItem.getParentId() != null ? PortalPageNavigationId.of(portalNavigationItem.getParentId()) : null)"
    )
    @Mapping(target = "order", source = "order")
    @Mapping(target = "href", expression = "java(parseHref(portalNavigationItem.getConfiguration()))")
    PortalNavigationLink portalNavigationLinkFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(target = "id", expression = "java(PortalPageNavigationId.of(portalNavigationItem.getId()))")
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "environmentId", source = "environmentId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "area", expression = "java(mapArea(portalNavigationItem.getArea()))")
    @Mapping(
        target = "parentId",
        expression = "java(portalNavigationItem.getParentId() != null ? PortalPageNavigationId.of(portalNavigationItem.getParentId()) : null)"
    )
    @Mapping(target = "order", source = "order")
    @Mapping(target = "contentId", expression = "java(PortalPageContentId.of(parsePageId(portalNavigationItem.getConfiguration())))")
    PortalNavigationPage portalNavigationPageFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(target = "id", expression = "java(portalNavigationItem.getId().json())")
    @Mapping(
        target = "parentId",
        expression = "java(portalNavigationItem.getParentId() != null ? portalNavigationItem.getParentId().json() : null)"
    )
    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "area", expression = "java(mapAreaReverse(portalNavigationItem.getArea()))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationItem portalNavigationItem);

    default io.gravitee.repository.management.model.PortalNavigationItem.Type mapType(PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem) {
            case PortalNavigationFolder ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.FOLDER;
            case PortalNavigationPage ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.PAGE;
            case PortalNavigationLink ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.LINK;
        };
    }

    default String configurationOf(PortalNavigationItem portalNavigationItem) {
        try {
            return switch (portalNavigationItem) {
                case PortalNavigationPage page -> {
                    Map<String, String> config = new HashMap<>();
                    config.put("pageId", page.getContentId().json());
                    yield OBJECT_MAPPER.writeValueAsString(config);
                }
                case PortalNavigationLink link -> {
                    Map<String, String> config = new HashMap<>();
                    config.put("href", link.getHref());
                    yield OBJECT_MAPPER.writeValueAsString(config);
                }
                case PortalNavigationFolder ignored -> OBJECT_MAPPER.writeValueAsString(new HashMap<>());
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize configuration for PortalNavigationItem", e);
        }
    }

    @Named("parsePageId")
    default String parsePageId(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            throw new IllegalArgumentException("PortalNavigationItem configuration is missing for PAGE type");
        }
        try {
            var node = OBJECT_MAPPER.readTree(configuration);
            return node.get("pageId").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid configuration for PortalNavigationItem PAGE type", e);
        }
    }

    @Named("parseHref")
    default String parseHref(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            throw new IllegalArgumentException("PortalNavigationItem configuration is missing for LINK type");
        }
        try {
            var node = OBJECT_MAPPER.readTree(configuration);
            return node.get("href").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid configuration for PortalNavigationItem LINK type", e);
        }
    }

    @Mapping(target = "id", expression = "java(PortalPageNavigationId.of(portalNavigationItem.getId()))")
    @Mapping(target = "organizationId", source = "organizationId")
    @Mapping(target = "environmentId", source = "environmentId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "area", expression = "java(mapArea(portalNavigationItem.getArea()))")
    @Mapping(
        target = "parentId",
        expression = "java(portalNavigationItem.getParentId() != null ? PortalPageNavigationId.of(portalNavigationItem.getParentId()) : null)"
    )
    @Mapping(target = "order", source = "order")
    PortalNavigationFolder portalNavigationFolderFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );
}
