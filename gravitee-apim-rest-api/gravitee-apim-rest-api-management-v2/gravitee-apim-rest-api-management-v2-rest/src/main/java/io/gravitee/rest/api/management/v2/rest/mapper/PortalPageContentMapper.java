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

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
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
        };
    }

    default String map(io.gravitee.apim.core.portal_page.model.PortalPageContentId id) {
        return id.json();
    }

    @Mapping(target = "type", constant = "GRAVITEE_MARKDOWN")
    @Mapping(target = "content", source = "content.raw")
    io.gravitee.rest.api.management.v2.rest.model.PortalPageContent map(GraviteeMarkdownPageContent markdownContent);

    @Mapping(target = "type", constant = "OPENAPI")
    @Mapping(target = "content", source = "content.raw")
    io.gravitee.rest.api.management.v2.rest.model.PortalPageContent map(OpenApiPageContent openApiContent);

    UpdatePortalPageContent map(io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent portalPageContent);
}
