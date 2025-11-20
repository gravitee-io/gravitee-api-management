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

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalPageContentAdapter {
    PortalPageContentAdapter INSTANCE = Mappers.getMapper(PortalPageContentAdapter.class);

    default PortalPageContent toEntity(io.gravitee.repository.management.model.PortalPageContent portalPageContent) {
        return switch (portalPageContent.getType()) {
            case GRAVITEE_MARKDOWN -> graviteeMarkdownPageContentFromRepository(portalPageContent);
        };
    }

    default GraviteeMarkdownPageContent graviteeMarkdownPageContentFromRepository(
        io.gravitee.repository.management.model.PortalPageContent portalPageContent
    ) {
        return new GraviteeMarkdownPageContent(PortalPageContentId.of(portalPageContent.getId()), portalPageContent.getContent());
    }

    default io.gravitee.repository.management.model.PortalPageContent toRepository(PortalPageContent portalPageContent) {
        return switch (portalPageContent) {
            case GraviteeMarkdownPageContent content -> graviteeMarkdownPageContentFromEntity(content);
        };
    }

    default io.gravitee.repository.management.model.PortalPageContent graviteeMarkdownPageContentFromEntity(
        GraviteeMarkdownPageContent portalPageContent
    ) {
        return io.gravitee.repository.management.model.PortalPageContent.builder()
            .id(portalPageContent.getId().toString())
            .type(io.gravitee.repository.management.model.PortalPageContent.Type.GRAVITEE_MARKDOWN)
            .content(portalPageContent.getContent())
            .build();
    }
}
