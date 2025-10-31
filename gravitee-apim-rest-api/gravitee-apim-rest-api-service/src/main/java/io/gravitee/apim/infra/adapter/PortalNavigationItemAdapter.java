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
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalNavigationItemAdapter {
    PortalNavigationItemAdapter INSTANCE = Mappers.getMapper(PortalNavigationItemAdapter.class);

    default PortalNavigationItem toEntity(io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem) {
        var id = PortalPageNavigationId.of(portalNavigationItem.getId());
        var parentId = portalNavigationItem.getParentId() != null ? PortalPageNavigationId.of(portalNavigationItem.getParentId()) : null;
        var area = switch (portalNavigationItem.getArea()) {
            case HOMEPAGE -> PortalArea.HOMEPAGE;
            case TOP_NAVBAR -> PortalArea.TOP_NAVBAR;
        };
        var item = switch (portalNavigationItem.getType()) {
            case FOLDER -> new PortalNavigationFolder(
                id,
                portalNavigationItem.getOrganizationId(),
                portalNavigationItem.getEnvironmentId(),
                portalNavigationItem.getTitle(),
                area
            );
            case PAGE -> portalNavigationPageFromRepository(portalNavigationItem, id, area);
            case LINK -> portalNavigationLinkFromRepository(portalNavigationItem, id, area);
        };
        item.setParentId(parentId);
        item.setOrder(portalNavigationItem.getOrder());
        return item;
    }

    default PortalNavigationLink portalNavigationLinkFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem,
        PortalPageNavigationId id,
        PortalArea area
    ) {
        var configurationStr = portalNavigationItem.getConfiguration();
        if (configurationStr == null || configurationStr.isEmpty()) {
            throw new IllegalArgumentException("PortalNavigationItem configuration is missing for LINK type");
        }

        String href;
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = objectMapper.readTree(configurationStr);
            href = node.get("href").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid configuration for PortalNavigationItem LINK type", e);
        }

        return new PortalNavigationLink(
            id,
            portalNavigationItem.getOrganizationId(),
            portalNavigationItem.getEnvironmentId(),
            portalNavigationItem.getTitle(),
            area,
            href
        );
    }

    default PortalNavigationPage portalNavigationPageFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem,
        PortalPageNavigationId id,
        PortalArea area
    ) {
        var configurationStr = portalNavigationItem.getConfiguration();
        if (configurationStr == null || configurationStr.isEmpty()) {
            throw new IllegalArgumentException("PortalNavigationItem configuration is missing for PAGE type");
        }

        String pageId;
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = objectMapper.readTree(configurationStr);
            pageId = node.get("pageId").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid configuration for PortalNavigationItem PAGE type", e);
        }

        return new PortalNavigationPage(
            id,
            portalNavigationItem.getOrganizationId(),
            portalNavigationItem.getEnvironmentId(),
            portalNavigationItem.getTitle(),
            area,
            PortalPageContentId.of(pageId)
        );
    }

    default io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationItem portalNavigationItem) {
        var type = switch (portalNavigationItem) {
            case PortalNavigationFolder ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.FOLDER;
            case PortalNavigationPage ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.PAGE;
            case PortalNavigationLink ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.LINK;
        };
        var area = switch (portalNavigationItem.getArea()) {
            case HOMEPAGE -> io.gravitee.repository.management.model.PortalNavigationItem.Area.HOMEPAGE;
            case TOP_NAVBAR -> io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR;
        };
        return io.gravitee.repository.management.model.PortalNavigationItem.builder()
            .id(portalNavigationItem.getId().json())
            .organizationId(portalNavigationItem.getOrganizationId())
            .environmentId(portalNavigationItem.getEnvironmentId())
            .title(portalNavigationItem.getTitle())
            .type(type)
            .area(area)
            .parentId(portalNavigationItem.getParentId() != null ? portalNavigationItem.getParentId().json() : null)
            .order(portalNavigationItem.getOrder())
            .configuration(configurationOf(portalNavigationItem))
            .build();
    }

    default String configurationOf(PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem) {
            case PortalNavigationPage page -> String.format("{\"pageId\":\"%s\"}", page.getContentId().json());
            case PortalNavigationLink link -> String.format("{\"href\":\"%s\"}", link.getHref());
            case PortalNavigationFolder ignored -> "{}";
        };
    }
}
