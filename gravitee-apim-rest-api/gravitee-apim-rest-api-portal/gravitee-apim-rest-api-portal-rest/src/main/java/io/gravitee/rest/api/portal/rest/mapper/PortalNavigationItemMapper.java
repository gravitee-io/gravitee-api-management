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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.portal.rest.model.PortalPageContentType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalNavigationItemMapper {
    PortalNavigationItemMapper INSTANCE = Mappers.getMapper(PortalNavigationItemMapper.class);

    PortalArea map(io.gravitee.rest.api.portal.rest.model.PortalArea area);

    default List<io.gravitee.rest.api.portal.rest.model.PortalNavigationItem> map(@Nonnull List<PortalNavigationItem> items) {
        return items.stream().map(this::getBasePortalNavigationItem).toList();
    }

    default io.gravitee.rest.api.portal.rest.model.PortalNavigationItem getBasePortalNavigationItem(PortalNavigationItem item) {
        var baseItem = switch (item) {
            case PortalNavigationFolder folder -> map(folder);
            case PortalNavigationLink link -> map(link);
            case PortalNavigationPage page -> map(page);
            case PortalNavigationApi api -> map(api);
        };
        var wrappedItem = new io.gravitee.rest.api.portal.rest.model.PortalNavigationItem();
        wrappedItem.setActualInstance(baseItem);
        return wrappedItem;
    }

    io.gravitee.rest.api.portal.rest.model.PortalNavigationFolder map(PortalNavigationFolder folder);
    io.gravitee.rest.api.portal.rest.model.PortalNavigationLink map(PortalNavigationLink link);
    io.gravitee.rest.api.portal.rest.model.PortalNavigationPage map(PortalNavigationPage page);
    io.gravitee.rest.api.portal.rest.model.PortalNavigationApi map(PortalNavigationApi api);

    default String extractContent(PortalPageContent<?> content) {
        return switch (content) {
            case GraviteeMarkdownPageContent gmd -> gmd.getGmdContent();
            case OpenApiPageContent oapi -> oapi.getOpenApiContent();
        };
    }

    @Mapping(target = "content", expression = "java(extractContent(content))")
    @Mapping(source = "type", target = "type")
    io.gravitee.rest.api.portal.rest.model.PortalPageContent map(PortalPageContent<?> content);

    default String map(@Nullable PortalNavigationItemId portalNavigationItemId) {
        if (portalNavigationItemId == null) {
            return null;
        }

        return portalNavigationItemId.json();
    }

    default String map(PortalPageContentId portalPageContentId) {
        return portalPageContentId.json();
    }
}
