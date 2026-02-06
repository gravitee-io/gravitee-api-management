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

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.BaseUpdatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationPage;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalNavigationItemsMapper {
    PortalNavigationItemsMapper INSTANCE = Mappers.getMapper(PortalNavigationItemsMapper.class);

    @Mapping(target = "type", constant = "PAGE")
    @Mapping(target = "portalPageContentId", expression = "java(page.getPortalPageContentId().id())")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationPage page
    );

    @Mapping(target = "type", constant = "FOLDER")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationFolder folder
    );

    @Mapping(target = "type", constant = "LINK")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink map(
        io.gravitee.apim.core.portal_page.model.PortalNavigationLink link
    );

    @Mapping(target = "type", constant = "API")
    io.gravitee.rest.api.management.v2.rest.model.PortalNavigationApi map(io.gravitee.apim.core.portal_page.model.PortalNavigationApi api);

    default List<PortalNavigationItem> map(List<io.gravitee.apim.core.portal_page.model.PortalNavigationItem> items) {
        return items.stream().map(this::map).toList();
    }

    default PortalNavigationItem map(io.gravitee.apim.core.portal_page.model.PortalNavigationItem portalNavigationItem) {
        return switch (portalNavigationItem) {
            case io.gravitee.apim.core.portal_page.model.PortalNavigationFolder folder -> new PortalNavigationItem(map(folder));
            case io.gravitee.apim.core.portal_page.model.PortalNavigationPage page -> new PortalNavigationItem(map(page));
            case io.gravitee.apim.core.portal_page.model.PortalNavigationLink link -> new PortalNavigationItem(map(link));
            case io.gravitee.apim.core.portal_page.model.PortalNavigationApi api -> new PortalNavigationItem(map(api));
        };
    }

    @Mapping(
        target = "portalPageContentId",
        expression = "java(page.getPortalPageContentId() == null ? null : io.gravitee.apim.core.portal_page.model.PortalPageContentId.of(page.getPortalPageContentId().toString()))"
    )
    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage page
    );

    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder folder
    );

    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink link
    );
    io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationApi api
    );

    default io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem map(
        BaseCreatePortalNavigationItem createPortalNavigationItem
    ) {
        return switch (createPortalNavigationItem) {
            case CreatePortalNavigationFolder folder -> map(folder);
            case CreatePortalNavigationPage page -> map(page);
            case CreatePortalNavigationLink link -> map(link);
            case CreatePortalNavigationApi api -> map(api);
            default -> throw new TechnicalDomainException(
                String.format("Unknown PortalNavigationItem class %s", createPortalNavigationItem.getClass().getSimpleName())
            );
        };
    }

    default PortalNavigationItemId map(UUID id) {
        return id == null ? null : PortalNavigationItemId.of(id.toString());
    }

    default String map(io.gravitee.apim.core.portal_page.model.PortalNavigationItemId id) {
        return id != null ? id.json() : null;
    }

    default io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        BaseUpdatePortalNavigationItem updatePortalNavigationItem
    ) {
        return switch (updatePortalNavigationItem) {
            case UpdatePortalNavigationFolder folder -> map(folder);
            case UpdatePortalNavigationPage page -> map(page);
            case UpdatePortalNavigationLink link -> map(link);
            case UpdatePortalNavigationApi api -> map(api);
            default -> throw new TechnicalDomainException(
                String.format("Unknown PortalNavigationItem class %s", updatePortalNavigationItem.getClass().getSimpleName())
            );
        };
    }

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationPage page
    );

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationFolder folder
    );

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationLink link
    );

    io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem map(
        io.gravitee.rest.api.management.v2.rest.model.UpdatePortalNavigationApi api
    );
}
