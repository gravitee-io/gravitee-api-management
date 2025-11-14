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
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.rest.api.management.v2.rest.model.BaseCreatePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.BasePortalNavigationItem;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalNavigationPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationFolder;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationLink;
import io.gravitee.rest.api.management.v2.rest.model.PortalNavigationPage;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalNavigationItemsMapper {
    PortalNavigationItemsMapper INSTANCE = Mappers.getMapper(PortalNavigationItemsMapper.class);

    default BasePortalNavigationItem map(io.gravitee.apim.core.portal_page.model.PortalNavigationItem portalNavigationItem) {
        final var id = portalNavigationItem.getId().toString();
        final var title = portalNavigationItem.getTitle();
        final var area = BasePortalNavigationItem.AreaEnum.fromValue(portalNavigationItem.getArea().name());
        final var order = portalNavigationItem.getOrder();
        final var parentId = mapParentId(portalNavigationItem.getParentId());

        return switch (portalNavigationItem) {
            case io.gravitee.apim.core.portal_page.model.PortalNavigationFolder ignored -> new PortalNavigationFolder()
                .id(id)
                .title(title)
                .area(area)
                .order(order)
                .parentId(parentId);
            case io.gravitee.apim.core.portal_page.model.PortalNavigationPage page -> new PortalNavigationPage()
                .contentId(page.getPortalPageContentId().toString())
                .id(id)
                .title(title)
                .area(area)
                .order(order)
                .parentId(parentId);
            case io.gravitee.apim.core.portal_page.model.PortalNavigationLink link -> new PortalNavigationLink()
                .url(link.getHref())
                .id(id)
                .title(title)
                .area(area)
                .order(order)
                .parentId(parentId);
        };
    }

    default io.gravitee.apim.core.portal_page.model.PortalNavigationItem map(
        String organizationId,
        String environmentId,
        BaseCreatePortalNavigationItem createPortalNavigationItem
    ) {
        final var id = PortalNavigationItemId.random();
        final var title = createPortalNavigationItem.getTitle();
        final var area = PortalArea.valueOf(createPortalNavigationItem.getArea().getValue());
        final var order = createPortalNavigationItem.getOrder() == null ? Integer.MAX_VALUE : createPortalNavigationItem.getOrder();

        final var item = switch (createPortalNavigationItem) {
            case CreatePortalNavigationFolder ignored -> new io.gravitee.apim.core.portal_page.model.PortalNavigationFolder(
                id,
                organizationId,
                environmentId,
                title,
                area,
                order
            );
            case CreatePortalNavigationPage page -> new io.gravitee.apim.core.portal_page.model.PortalNavigationPage(
                id,
                organizationId,
                environmentId,
                title,
                area,
                order,
                PortalPageContentId.of(page.getContentId())
            );
            case CreatePortalNavigationLink link -> new io.gravitee.apim.core.portal_page.model.PortalNavigationLink(
                id,
                organizationId,
                environmentId,
                title,
                area,
                order,
                link.getUrl()
            );
            default -> throw new TechnicalDomainException(
                String.format("Unknown PortalNavigationItem class %s", createPortalNavigationItem.getClass().getSimpleName())
            );
        };
        item.setParentId(mapParentId(createPortalNavigationItem.getParentId()));

        return item;
    }

    private PortalNavigationItemId mapParentId(String parentId) {
        return parentId == null ? null : PortalNavigationItemId.of(parentId);
    }

    private String mapParentId(PortalNavigationItemId parentId) {
        return parentId == null ? null : parentId.toString();
    }
}
