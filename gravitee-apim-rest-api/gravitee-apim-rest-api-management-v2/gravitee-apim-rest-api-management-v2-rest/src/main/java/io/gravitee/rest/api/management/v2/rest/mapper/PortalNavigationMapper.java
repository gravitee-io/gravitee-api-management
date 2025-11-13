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

import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalNavigationMapper {
    PortalNavigationMapper INSTANCE = Mappers.getMapper(PortalNavigationMapper.class);

    default List<io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItem> map(List<PortalNavigationItem> items) {
        return items
            .stream()
            .map(item ->
                switch (item) {
                    case PortalNavigationPage page -> new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItem(map(page));
                    case PortalNavigationFolder folder -> new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItem(
                        map(folder)
                    );
                    case PortalNavigationLink link -> new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItem(map(link));
                }
            )
            .toList();
    }

    default String map(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId id) {
        return id != null ? id.json() : null;
    }

    @Mapping(target = "type", constant = "PAGE")
    @Mapping(
        target = "configuration",
        expression = "java(new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPageAllOfConfiguration().portalPageContentId(page.getPortalPageContentId().toString()))"
    )
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage map(PortalNavigationPage page);

    @Mapping(target = "type", constant = "FOLDER")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder map(PortalNavigationFolder folder);

    @Mapping(target = "type", constant = "LINK")
    @Mapping(
        target = "configuration",
        expression = "java(new io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLinkAllOfConfiguration().url(link.getUrl()))"
    )
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink map(PortalNavigationLink link);
}
