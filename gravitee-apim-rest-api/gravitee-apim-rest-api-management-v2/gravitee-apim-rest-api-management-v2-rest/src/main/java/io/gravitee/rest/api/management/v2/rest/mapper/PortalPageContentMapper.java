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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiConfiguration;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.RedocConfiguration;
import io.gravitee.apim.core.portal_page.model.SwaggerUiConfiguration;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.rest.api.management.v2.rest.model.BasePortalPageOpenApiConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageOpenApiConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageRedocConfiguration;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageSwaggerConfiguration;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalPageContentMapper {
    PortalPageContentMapper INSTANCE = Mappers.getMapper(PortalPageContentMapper.class);

    default io.gravitee.rest.api.management.v2.rest.model.PortalPageContent map(PortalPageContent<?> portalPageContent) {
        return switch (portalPageContent) {
            case GraviteeMarkdownPageContent markdownContent -> map(markdownContent);
            case OpenApiPageContent openApiContent -> map(openApiContent);
            case AsyncApiPageContent asyncApiContent -> map(asyncApiContent);
        };
    }

    default String map(io.gravitee.apim.core.portal_page.model.PortalPageContentId id) {
        return id.json();
    }

    @Mapping(target = "type", constant = "GRAVITEE_MARKDOWN")
    @Mapping(target = "content", expression = "java(markdownContent.getContent().value())")
    @Mapping(target = "configuration", ignore = true)
    io.gravitee.rest.api.management.v2.rest.model.PortalPageContent map(GraviteeMarkdownPageContent markdownContent);

    @Mapping(target = "type", constant = "OPENAPI")
    @Mapping(target = "content", expression = "java(openApiContent.getContent().value())")
    @Mapping(target = "configuration", expression = "java(configurationToDto(openApiContent.getViewerSettings()))")
    io.gravitee.rest.api.management.v2.rest.model.PortalPageContent map(OpenApiPageContent openApiContent);

    @Mapping(target = "type", constant = "ASYNCAPI")
    @Mapping(target = "content", expression = "java(asyncApiContent.getContent().value())")
    @Mapping(target = "configuration", ignore = true)
    io.gravitee.rest.api.management.v2.rest.model.PortalPageContent map(AsyncApiPageContent asyncApiContent);

    @Mapping(target = "configuration", expression = "java(mapToConfiguration(portalPageContent.getConfiguration()))")
    UpdatePortalPageContent map(io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent portalPageContent);

    default PortalPageOpenApiConfiguration configurationToDto(OpenApiConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        return switch (configuration) {
            case SwaggerUiConfiguration cfg -> new PortalPageOpenApiConfiguration(mapToDto(cfg));
            case RedocConfiguration ignored -> new PortalPageOpenApiConfiguration(mapRedocToDto());
        };
    }

    default OpenApiConfiguration mapToConfiguration(PortalPageOpenApiConfiguration configuration) {
        if (configuration == null || configuration.getActualInstance() == null) {
            return null;
        }
        return switch (configuration.getActualInstance()) {
            case PortalPageRedocConfiguration ignored -> new RedocConfiguration();
            case PortalPageSwaggerConfiguration swagger -> new SwaggerUiConfiguration(
                swagger.getDisplayOperationId(),
                mapDocExpansion(swagger.getDocExpansion()),
                swagger.getEnableFiltering(),
                swagger.getMaxDisplayedTags(),
                swagger.getShowCommonExtensions(),
                swagger.getShowExtensions(),
                swagger.getShowURL(),
                swagger.getTryIt(),
                swagger.getDisableSyntaxHighlight(),
                swagger.getTryItAnonymous(),
                swagger.getTryItURL(),
                swagger.getUsePkce(),
                swagger.getEntrypointsAsServers(),
                swagger.getEntrypointAsBasePath()
            );
            default -> throw new IllegalStateException("Cannot map configuration to OpenApi configuration");
        };
    }

    private static PortalPageSwaggerConfiguration.DocExpansionEnum mapDocExpansion(@NotNull String value) {
        return PortalPageSwaggerConfiguration.DocExpansionEnum.fromValue(value);
    }

    private static String mapDocExpansion(@NotNull PortalPageSwaggerConfiguration.DocExpansionEnum value) {
        return value.getValue();
    }

    private static PortalPageSwaggerConfiguration mapToDto(SwaggerUiConfiguration cfg) {
        var configuration = new PortalPageSwaggerConfiguration()
            .displayOperationId(cfg.displayOperationId())
            .docExpansion(mapDocExpansion(cfg.docExpansion()))
            .enableFiltering(cfg.enableFiltering())
            .maxDisplayedTags(cfg.maxDisplayedTags())
            .showCommonExtensions(cfg.showCommonExtensions())
            .showExtensions(cfg.showExtensions())
            .showURL(cfg.showUrl())
            .tryIt(cfg.tryIt())
            .disableSyntaxHighlight(cfg.disableSyntaxHighlight())
            .tryItAnonymous(cfg.tryItAnonymous())
            .tryItURL(cfg.tryItUrl())
            .usePkce(cfg.usePkce())
            .entrypointsAsServers(cfg.entrypointsAsServers())
            .entrypointAsBasePath(cfg.entrypointAsBasePath());
        configuration.setViewer(BasePortalPageOpenApiConfiguration.ViewerEnum.SWAGGER);
        return configuration;
    }

    private static PortalPageRedocConfiguration mapRedocToDto() {
        var configuration = new PortalPageRedocConfiguration();
        configuration.setViewer(BasePortalPageOpenApiConfiguration.ViewerEnum.REDOC);
        return configuration;
    }
}
