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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.async_api.AsyncApi;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.open_api.OpenApi;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiConfiguration;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalPageContentAdapter {
    PortalPageContentAdapter INSTANCE = Mappers.getMapper(PortalPageContentAdapter.class);
    ObjectMapper JSON = new ObjectMapper();

    default PortalPageContent<?> toEntity(io.gravitee.repository.management.model.PortalPageContent portalPageContent) {
        var repoMeta = portalPageContent.getAutomationMetadata();
        var meta = repoMeta == null
            ? null
            : new AutomationMetadata(
                AutomationMetadata.ReferenceType.valueOf(repoMeta.getReferenceType().name()),
                repoMeta.getReferenceId(),
                repoMeta.getName(),
                Optional.ofNullable(repoMeta.getLocation()),
                Optional.ofNullable(repoMeta.getOrder())
            );
        return switch (portalPageContent.getType()) {
            case GRAVITEE_MARKDOWN -> graviteeMarkdownPageContentFromRepository(portalPageContent, meta);
            case OPENAPI -> openApiPageContentFromRepository(portalPageContent, meta);
            case ASYNCAPI -> asyncApiPageContentFromRepository(portalPageContent, meta);
        };
    }

    default GraviteeMarkdownPageContent graviteeMarkdownPageContentFromRepository(
        io.gravitee.repository.management.model.PortalPageContent portalPageContent,
        @Nullable AutomationMetadata automationMetadata
    ) {
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.of(portalPageContent.getId()),
            portalPageContent.getOrganizationId(),
            portalPageContent.getEnvironmentId(),
            GraviteeMarkdown.of(portalPageContent.getContent()),
            automationMetadata
        );
    }

    default OpenApiPageContent openApiPageContentFromRepository(
        io.gravitee.repository.management.model.PortalPageContent portalPageContent,
        @Nullable AutomationMetadata automationMetadata
    ) {
        OpenApiConfiguration configuration = null;
        if (portalPageContent.getConfiguration() != null && !portalPageContent.getConfiguration().isBlank()) {
            try {
                configuration = deserializeOpenApiConfiguration(portalPageContent.getConfiguration());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format(
                        "Failed to deserialize OpenAPI configuration for page content %s: %s",
                        portalPageContent.getId(),
                        e.getMessage()
                    ),
                    e.getCause()
                );
            }
        }
        return new OpenApiPageContent(
            PortalPageContentId.of(portalPageContent.getId()),
            portalPageContent.getOrganizationId(),
            portalPageContent.getEnvironmentId(),
            OpenApi.of(portalPageContent.getContent()),
            Optional.ofNullable(configuration).orElseGet(RedocConfiguration::new),
            automationMetadata
        );
    }

    default AsyncApiPageContent asyncApiPageContentFromRepository(
        io.gravitee.repository.management.model.PortalPageContent portalPageContent,
        @Nullable AutomationMetadata automationMetadata
    ) {
        return new AsyncApiPageContent(
            PortalPageContentId.of(portalPageContent.getId()),
            portalPageContent.getOrganizationId(),
            portalPageContent.getEnvironmentId(),
            AsyncApi.of(portalPageContent.getContent()),
            automationMetadata
        );
    }

    default io.gravitee.repository.management.model.PortalPageContent toRepository(PortalPageContent<?> portalPageContent) {
        String rawContent = switch (portalPageContent) {
            case GraviteeMarkdownPageContent gmd -> gmd.getContent().value();
            case OpenApiPageContent oapi -> oapi.getContent().value();
            case AsyncApiPageContent aapi -> aapi.getContent().value();
        };
        String rawConfiguration = null;
        if (portalPageContent instanceof OpenApiPageContent oapi) {
            try {
                rawConfiguration = serializeOpenApiConfiguration(oapi.getViewerSettings());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    String.format(
                        "Failed to serialize OpenAPI configuration for page content %s: %s",
                        portalPageContent.getId(),
                        e.getMessage()
                    ),
                    e
                );
            }
        }
        var meta = portalPageContent.getAutomationMetadata();
        return io.gravitee.repository.management.model.PortalPageContent.builder()
            .id(portalPageContent.getId().toString())
            .organizationId(portalPageContent.getOrganizationId())
            .environmentId(portalPageContent.getEnvironmentId())
            .type(toRepositoryType(portalPageContent.getType()))
            .content(rawContent)
            .configuration(rawConfiguration)
            .automationMetadata(
                meta == null
                    ? null
                    : io.gravitee.repository.management.model.PortalPageContent.AutomationMetadata.builder()
                        .referenceType(AutomationTargetReferenceType.valueOf(meta.referenceType().name()))
                        .referenceId(meta.referenceId())
                        .name(meta.name())
                        .location(meta.location().orElse(null))
                        .order(meta.order().orElse(null))
                        .build()
            )
            .build();
    }

    default io.gravitee.repository.management.model.PortalPageContent.Type toRepositoryType(
        io.gravitee.apim.core.portal_page.model.PortalPageContentType type
    ) {
        return switch (type) {
            case GRAVITEE_MARKDOWN -> io.gravitee.repository.management.model.PortalPageContent.Type.GRAVITEE_MARKDOWN;
            case OPENAPI -> io.gravitee.repository.management.model.PortalPageContent.Type.OPENAPI;
            case ASYNCAPI -> io.gravitee.repository.management.model.PortalPageContent.Type.ASYNCAPI;
        };
    }

    default OpenApiConfiguration deserializeOpenApiConfiguration(String configuration) {
        try {
            var node = JSON.readTree(configuration);
            var viewer = Optional.ofNullable(node.get("viewer")).map(viewerNode ->
                OpenApiConfiguration.Viewer.valueOf(viewerNode.asText())
            );
            return switch (viewer.orElse(OpenApiConfiguration.Viewer.REDOC)) {
                case REDOC -> new RedocConfiguration();
                case SWAGGER -> new SwaggerUiConfiguration(
                    optionalBoolean(node, "displayOperationId", false),
                    optionalText(node, "docExpansion", "none"),
                    optionalBoolean(node, "enableFiltering", false),
                    optionalInt(node, "maxDisplayedTags", -1),
                    optionalBoolean(node, "showCommonExtensions", false),
                    optionalBoolean(node, "showExtensions", false),
                    optionalBoolean(node, "showUrl", optionalBoolean(node, "showURL", false)),
                    optionalBoolean(node, "tryIt", false),
                    optionalBoolean(node, "disableSyntaxHighlight", false),
                    optionalBoolean(node, "tryItAnonymous", false),
                    optionalText(node, "tryItUrl", optionalText(node, "tryItURL", "")),
                    optionalBoolean(node, "usePkce", false),
                    optionalBoolean(node, "entrypointsAsServers", false),
                    optionalBoolean(node, "entrypointAsBasePath", false)
                );
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OpenAPI configuration", e);
        }
    }

    default String serializeOpenApiConfiguration(OpenApiConfiguration configuration)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        var values = new LinkedHashMap<String, Object>();
        switch (configuration) {
            case RedocConfiguration ignored -> values.put("viewer", OpenApiConfiguration.Viewer.REDOC);
            case SwaggerUiConfiguration swagger -> {
                values.put("viewer", OpenApiConfiguration.Viewer.SWAGGER);
                values.put("displayOperationId", swagger.displayOperationId());
                values.put("docExpansion", swagger.docExpansion());
                values.put("enableFiltering", swagger.enableFiltering());
                values.put("maxDisplayedTags", swagger.maxDisplayedTags());
                values.put("showCommonExtensions", swagger.showCommonExtensions());
                values.put("showExtensions", swagger.showExtensions());
                values.put("showUrl", swagger.showUrl());
                values.put("tryIt", swagger.tryIt());
                values.put("disableSyntaxHighlight", swagger.disableSyntaxHighlight());
                values.put("tryItAnonymous", swagger.tryItAnonymous());
                values.put("tryItUrl", swagger.tryItUrl());
                values.put("usePkce", swagger.usePkce());
                values.put("entrypointsAsServers", swagger.entrypointsAsServers());
                values.put("entrypointAsBasePath", swagger.entrypointAsBasePath());
            }
        }
        return JSON.writeValueAsString(values);
    }

    private static boolean optionalBoolean(com.fasterxml.jackson.databind.JsonNode node, String field, boolean defaultValue) {
        return Optional.ofNullable(node.get(field)).map(com.fasterxml.jackson.databind.JsonNode::asBoolean).orElse(defaultValue);
    }

    private static int optionalInt(com.fasterxml.jackson.databind.JsonNode node, String field, int defaultValue) {
        return Optional.ofNullable(node.get(field)).map(com.fasterxml.jackson.databind.JsonNode::asInt).orElse(defaultValue);
    }

    private static String optionalText(com.fasterxml.jackson.databind.JsonNode node, String field, String defaultValue) {
        return Optional.ofNullable(node.get(field)).map(com.fasterxml.jackson.databind.JsonNode::asText).orElse(defaultValue);
    }
}
