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

import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface PortalPageAdapter {
    PortalPageAdapter INSTANCE = org.mapstruct.factory.Mappers.getMapper(PortalPageAdapter.class);

    default io.gravitee.apim.core.portal_page.model.PortalPage toEntity(io.gravitee.repository.management.model.PortalPage portalPage) {
        if (portalPage == null) {
            return null;
        }

        return new io.gravitee.apim.core.portal_page.model.PortalPage(mapId(portalPage.getId()), mapContent(portalPage.getContent()));
    }

    @Mapping(target = "content", source = "pageContent.content")
    io.gravitee.repository.management.model.PortalPage toRepository(io.gravitee.apim.core.portal_page.model.PortalPage portalPage);

    default io.gravitee.apim.core.portal_page.model.PageId mapId(String value) {
        return value == null ? null : io.gravitee.apim.core.portal_page.model.PageId.of(value);
    }

    default String map(io.gravitee.apim.core.portal_page.model.PageId value) {
        return value == null ? null : value.toString();
    }

    default io.gravitee.apim.core.portal_page.model.GraviteeMarkdown mapContent(String value) {
        return value == null ? null : new io.gravitee.apim.core.portal_page.model.GraviteeMarkdown(value);
    }

    default PortalViewContext map(PortalPageContextType pageContext) {
        if (pageContext == null) {
            return null;
        }
        return PortalViewContext.valueOf(pageContext.name());
    }

    @Mapping(target = "context", source = "contextType")
    PortalPageView map(PortalPageContext pageContext);

    default io.gravitee.apim.core.portal_page.model.PortalPage map(PortalPage value) {
        if (value == null) {
            return null;
        }
        return new io.gravitee.apim.core.portal_page.model.PortalPage(mapId(value.getId()), mapContent(value.getContent()));
    }
}
