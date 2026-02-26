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
    String PORTAL_PAGE_CONTENT_ID = "portalPageContentId";
    String URL = "url";

    default PortalNavigationItem toEntity(io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem.getType()) {
            case FOLDER -> portalNavigationFolderFromRepository(portalNavigationItem);
            case PAGE -> portalNavigationPageFromRepository(portalNavigationItem);
            case LINK -> portalNavigationLinkFromRepository(portalNavigationItem);
            case API -> portalNavigationApiFromRepository(portalNavigationItem);
        };
    }

    default PortalArea mapArea(io.gravitee.repository.management.model.PortalNavigationItem.Area area) {
        return switch (area) {
            case HOMEPAGE -> PortalArea.HOMEPAGE;
            case TOP_NAVBAR -> PortalArea.TOP_NAVBAR;
        };
    }

    default io.gravitee.repository.management.model.PortalNavigationItem.Area mapArea(PortalArea area) {
        return switch (area) {
            case HOMEPAGE -> io.gravitee.repository.management.model.PortalNavigationItem.Area.HOMEPAGE;
            case TOP_NAVBAR -> io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR;
        };
    }

    @Mapping(target = "url", expression = "java(parseUrl(portalNavigationItem.getConfiguration()))")
    @Mapping(target = "rootId", expression = "java(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.ZERO)")
    PortalNavigationLink portalNavigationLinkFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(target = "rootId", expression = "java(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.ZERO)")
    PortalNavigationApi portalNavigationApiFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(target = "portalPageContentId", expression = "java(parsePortalPageContentId(portalNavigationItem.getConfiguration()))")
    @Mapping(target = "rootId", expression = "java(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.ZERO)")
    PortalNavigationPage portalNavigationPageFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    default io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem) {
            case PortalNavigationApi api -> toRepository(api);
            case PortalNavigationPage page -> toRepository(page);
            case PortalNavigationLink link -> toRepository(link);
            case PortalNavigationFolder folder -> toRepository(folder);
        };
    }

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationPage portalNavigationItem);

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationFolder portalNavigationItem);

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationLink portalNavigationItem);

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationApi portalNavigationItem);

    default io.gravitee.repository.management.model.PortalNavigationItem.Type mapType(PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem) {
            case PortalNavigationFolder ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.FOLDER;
            case PortalNavigationPage ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.PAGE;
            case PortalNavigationLink ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.LINK;
            case PortalNavigationApi ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.API;
        };
    }

    default String configurationOf(PortalNavigationItem portalNavigationItem) {
        try {
            return switch (portalNavigationItem) {
                case PortalNavigationPage page -> {
                    Map<String, String> config = new HashMap<>();
                    config.put(PORTAL_PAGE_CONTENT_ID, page.getPortalPageContentId().json());
                    yield OBJECT_MAPPER.writeValueAsString(config);
                }
                case PortalNavigationLink link -> {
                    Map<String, String> config = new HashMap<>();
                    config.put(URL, link.getUrl());
                    yield OBJECT_MAPPER.writeValueAsString(config);
                }
                case PortalNavigationFolder ignored -> OBJECT_MAPPER.writeValueAsString(new HashMap<>());
                case PortalNavigationApi ignored -> OBJECT_MAPPER.writeValueAsString(new HashMap<>());
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize configuration for PortalNavigationItem", e);
        }
    }

    @Named("parsePortalPageContentId")
    default PortalPageContentId parsePortalPageContentId(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            throw new IllegalArgumentException("PortalNavigationItem configuration is missing for PAGE type");
        }
        try {
            var node = OBJECT_MAPPER.readTree(configuration);
            return PortalPageContentId.of(node.get(PORTAL_PAGE_CONTENT_ID).asText());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid configuration for PortalNavigationItem PAGE type", e);
        }
    }

    @Named("parseUrl")
    default String parseUrl(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            throw new IllegalArgumentException("PortalNavigationItem configuration is missing for LINK type");
        }
        try {
            var node = OBJECT_MAPPER.readTree(configuration);
            return node.get(URL).asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid configuration for PortalNavigationItem LINK type", e);
        }
    }

    @Mapping(target = "rootId", expression = "java(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId.ZERO)")
    PortalNavigationFolder portalNavigationFolderFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(source = "area", target = "portalArea")
    io.gravitee.repository.management.api.search.PortalNavigationItemCriteria map(PortalNavigationItemQueryCriteria criteria);

    default String mapPortalNavigationItemId(PortalNavigationItemId id) {
        return id != null ? id.json() : null;
    }

    default PortalNavigationItemId mapPortalNavigationItemId(String id) {
        return id != null ? PortalNavigationItemId.of(id) : null;
    }
}
