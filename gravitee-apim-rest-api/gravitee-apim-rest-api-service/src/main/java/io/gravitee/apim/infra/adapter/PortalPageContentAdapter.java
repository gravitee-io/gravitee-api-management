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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContent;
import io.gravitee.apim.core.open_api.OpenApiContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalPageContentAdapter {
    PortalPageContentAdapter INSTANCE = Mappers.getMapper(PortalPageContentAdapter.class);

    default PortalPageContent<?> toEntity(io.gravitee.repository.management.model.PortalPageContent portalPageContent) {
        return switch (portalPageContent.getType()) {
            case GRAVITEE_MARKDOWN -> graviteeMarkdownPageContentFromRepository(portalPageContent);
            case OPENAPI -> openApiPageContentFromRepository(portalPageContent);
        };
    }

    default GraviteeMarkdownPageContent graviteeMarkdownPageContentFromRepository(
        io.gravitee.repository.management.model.PortalPageContent portalPageContent
    ) {
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.of(portalPageContent.getId()),
            portalPageContent.getOrganizationId(),
            portalPageContent.getEnvironmentId(),
            new GraviteeMarkdownContent(portalPageContent.getContent())
        );
    }

    default OpenApiPageContent openApiPageContentFromRepository(
        io.gravitee.repository.management.model.PortalPageContent portalPageContent
    ) {
        return new OpenApiPageContent(
            PortalPageContentId.of(portalPageContent.getId()),
            portalPageContent.getOrganizationId(),
            portalPageContent.getEnvironmentId(),
            new OpenApiContent(portalPageContent.getContent())
        );
    }

    default io.gravitee.repository.management.model.PortalPageContent toRepository(PortalPageContent<?> portalPageContent) {
        String rawContent = switch (portalPageContent) {
            case GraviteeMarkdownPageContent gmd -> gmd.getGmdContent();
            case OpenApiPageContent oapi -> oapi.getOpenApiContent();
        };
        return io.gravitee.repository.management.model.PortalPageContent.builder()
            .id(portalPageContent.getId().toString())
            .organizationId(portalPageContent.getOrganizationId())
            .environmentId(portalPageContent.getEnvironmentId())
            .type(toRepositoryType(portalPageContent.getType()))
            .content(rawContent)
            .build();
    }

    default io.gravitee.repository.management.model.PortalPageContent.Type toRepositoryType(
        io.gravitee.apim.core.portal_page.model.PortalPageContentType type
    ) {
        return switch (type) {
            case GRAVITEE_MARKDOWN -> io.gravitee.repository.management.model.PortalPageContent.Type.GRAVITEE_MARKDOWN;
            case OPENAPI -> io.gravitee.repository.management.model.PortalPageContent.Type.OPENAPI;
        };
    }
}
