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

import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.model.*;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.util.HashMap;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

@Mapper
public interface PortalNavigationItemAdapter {
    Logger log = NodeLoggerFactory.getLogger(PortalNavigationItemAdapter.class);
    PortalNavigationItemAdapter INSTANCE = Mappers.getMapper(PortalNavigationItemAdapter.class);

    com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();
    String PORTAL_PAGE_CONTENT_ID = "portalPageContentId";
    String URL = "url";
    String SOURCE = "source";
    String SOURCE_TYPE = "type";
    String SOURCE_CONFIGURATION = "configuration";
    String FETCH_CRON = "fetchCron";
    String LAST_FETCHED_AT = "lastFetchedAt";
    String LAST_FETCH_ERROR = "lastFetchError";

    default PortalNavigationItem toEntity(io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem.getType()) {
            case FOLDER -> portalNavigationFolderFromRepository(portalNavigationItem);
            case PAGE -> portalNavigationPageFromRepository(portalNavigationItem);
            case LINK -> portalNavigationLinkFromRepository(portalNavigationItem);
            case API -> portalNavigationApiFromRepository(portalNavigationItem);
            case API_PRODUCT -> portalNavigationApiProductFromRepository(portalNavigationItem);
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
    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "repositoryRootIdToDomain")
    @Mapping(target = "reference", expression = "java(referenceFromRepository(portalNavigationItem))")
    PortalNavigationLink portalNavigationLinkFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "repositoryRootIdToDomain")
    @Mapping(target = "reference", expression = "java(referenceFromRepository(portalNavigationItem))")
    PortalNavigationApi portalNavigationApiFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "repositoryRootIdToDomain")
    @Mapping(target = "reference", expression = "java(referenceFromRepository(portalNavigationItem))")
    PortalNavigationApiProduct portalNavigationApiProductFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    @Mapping(target = "portalPageContentId", expression = "java(parsePortalPageContentId(portalNavigationItem.getConfiguration()))")
    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "repositoryRootIdToDomain")
    @Mapping(target = "source", expression = "java(sourceFromRepository(portalNavigationItem))")
    @Mapping(target = "reference", expression = "java(referenceFromRepository(portalNavigationItem))")
    PortalNavigationPage portalNavigationPageFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    );

    default io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem) {
            case PortalNavigationApi api -> toRepository(api);
            case PortalNavigationApiProduct apiProduct -> toRepository(apiProduct);
            case PortalNavigationPage page -> toRepository(page);
            case PortalNavigationLink link -> toRepository(link);
            case PortalNavigationFolder folder -> toRepository(folder);
        };
    }

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    @Mapping(target = "useAutoFetch", source = "source.useAutoFetch")
    @Mapping(target = "referenceType", expression = "java(referenceTypeToRepository(portalNavigationItem.getReference()))")
    @Mapping(target = "referenceId", expression = "java(referenceIdToRepository(portalNavigationItem.getReference()))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationPage portalNavigationItem);

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    @Mapping(target = "useAutoFetch", source = "source.useAutoFetch")
    @Mapping(target = "referenceType", expression = "java(referenceTypeToRepository(portalNavigationItem.getReference()))")
    @Mapping(target = "referenceId", expression = "java(referenceIdToRepository(portalNavigationItem.getReference()))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationFolder portalNavigationItem);

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    @Mapping(target = "referenceType", expression = "java(referenceTypeToRepository(portalNavigationItem.getReference()))")
    @Mapping(target = "referenceId", expression = "java(referenceIdToRepository(portalNavigationItem.getReference()))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationLink portalNavigationItem);

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    @Mapping(target = "referenceType", expression = "java(referenceTypeToRepository(portalNavigationItem.getReference()))")
    @Mapping(target = "referenceId", expression = "java(referenceIdToRepository(portalNavigationItem.getReference()))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationApi portalNavigationItem);

    @Mapping(target = "type", expression = "java(mapType(portalNavigationItem))")
    @Mapping(target = "configuration", expression = "java(configurationOf(portalNavigationItem))")
    @Mapping(target = "referenceType", expression = "java(referenceTypeToRepository(portalNavigationItem.getReference()))")
    @Mapping(target = "referenceId", expression = "java(referenceIdToRepository(portalNavigationItem.getReference()))")
    io.gravitee.repository.management.model.PortalNavigationItem toRepository(PortalNavigationApiProduct portalNavigationItem);

    default io.gravitee.repository.management.model.PortalNavigationItem.Type mapType(PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem) {
            case PortalNavigationFolder ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.FOLDER;
            case PortalNavigationPage ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.PAGE;
            case PortalNavigationLink ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.LINK;
            case PortalNavigationApi ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.API;
            case PortalNavigationApiProduct ignored -> io.gravitee.repository.management.model.PortalNavigationItem.Type.API_PRODUCT;
        };
    }

    default String configurationOf(PortalNavigationItem portalNavigationItem) {
        try {
            var config = OBJECT_MAPPER.createObjectNode();
            switch (portalNavigationItem) {
                case PortalNavigationPage page -> {
                    config.put(PORTAL_PAGE_CONTENT_ID, page.getPortalPageContentId().json());
                    writeSource(config, page.getSource());
                }
                case PortalNavigationLink link -> config.put(URL, link.getUrl());
                case PortalNavigationFolder folder -> writeSource(config, folder.getSource());
                case PortalNavigationApi ignored -> {}
                case PortalNavigationApiProduct ignored -> {}
            }
            return OBJECT_MAPPER.writeValueAsString(config);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize configuration for PortalNavigationItem", e);
        }
    }

    private static void writeSource(com.fasterxml.jackson.databind.node.ObjectNode config, PortalNavigationItemSource source) {
        if (source == null) {
            return;
        }
        var sourceNode = config.putObject(SOURCE);
        sourceNode.put(SOURCE_TYPE, source.getSourceType());
        sourceNode.put(SOURCE_CONFIGURATION, source.getSourceConfiguration());
        if (source.getFetchCron() != null) {
            sourceNode.put(FETCH_CRON, source.getFetchCron());
        }
        if (source.getLastFetchedAt() != null) {
            sourceNode.put(LAST_FETCHED_AT, source.getLastFetchedAt().toString());
        }
        if (source.getLastFetchError() != null) {
            sourceNode.put(LAST_FETCH_ERROR, source.getLastFetchError());
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

    default PortalNavigationItemSource sourceFromRepository(
        io.gravitee.repository.management.model.PortalNavigationItem portalNavigationItem
    ) {
        var configuration = portalNavigationItem.getConfiguration();
        if (configuration == null || configuration.isEmpty()) {
            return null;
        }
        try {
            var sourceNode = OBJECT_MAPPER.readTree(configuration).get(SOURCE);
            if (sourceNode == null || sourceNode.isNull()) {
                return null;
            }
            return PortalNavigationItemSource.builder()
                .sourceType(sourceNode.get(SOURCE_TYPE).asText())
                .sourceConfiguration(sourceNode.get(SOURCE_CONFIGURATION).asText())
                .useAutoFetch(portalNavigationItem.isUseAutoFetch())
                .fetchCron(sourceNode.hasNonNull(FETCH_CRON) ? sourceNode.get(FETCH_CRON).asText() : null)
                .lastFetchedAt(
                    sourceNode.hasNonNull(LAST_FETCHED_AT) ? java.time.Instant.parse(sourceNode.get(LAST_FETCHED_AT).asText()) : null
                )
                .lastFetchError(sourceNode.hasNonNull(LAST_FETCH_ERROR) ? sourceNode.get(LAST_FETCH_ERROR).asText() : null)
                .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid source in configuration for PortalNavigationItem", e);
        }
    }

    default NavigationItemReference referenceFromRepository(io.gravitee.repository.management.model.PortalNavigationItem item) {
        return switch (item.getReferenceType()) {
            case PORTAL -> io.gravitee.apim.core.portal.model.PortalId.of(item.getReferenceId());
            case API -> ApiId.of(item.getReferenceId());
        };
    }

    default io.gravitee.repository.management.model.PortalNavigationReferenceType referenceTypeToRepository(
        NavigationItemReference reference
    ) {
        return switch (reference) {
            case io.gravitee.apim.core.portal.model.PortalId ignored -> io.gravitee.repository.management.model.PortalNavigationReferenceType.PORTAL;
            case ApiId ignored -> io.gravitee.repository.management.model.PortalNavigationReferenceType.API;
            default -> throw new IllegalStateException("Unknown NavigationItemReference type: " + reference.getClass());
        };
    }

    default String referenceIdToRepository(NavigationItemReference reference) {
        return switch (reference) {
            case io.gravitee.apim.core.portal.model.PortalId p -> p.id().toString();
            case ApiId a -> a.id().toString();
            default -> throw new IllegalStateException("Unknown NavigationItemReference type: " + reference.getClass());
        };
    }

    @Named("repositoryRootIdToDomain")
    default PortalNavigationItemId repositoryRootIdToDomain(String rootId) {
        if (StringUtils.hasText(rootId)) {
            return PortalNavigationItemId.of(rootId);
        }
        log.warn("Portal navigation item has null or blank rootId; mapping to zero. This is unexpected after rootId backfill.");
        return PortalNavigationItemId.zero();
    }

    @Mapping(target = "rootId", source = "rootId", qualifiedByName = "repositoryRootIdToDomain")
    @Mapping(target = "source", expression = "java(sourceFromRepository(portalNavigationItem))")
    @Mapping(target = "reference", expression = "java(referenceFromRepository(portalNavigationItem))")
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

    default PortalCategoryId mapCategoryId(String id) {
        return id != null ? PortalCategoryId.of(id) : null;
    }

    default String mapCategoryId(PortalCategoryId id) {
        return id != null ? id.toString() : null;
    }
}
