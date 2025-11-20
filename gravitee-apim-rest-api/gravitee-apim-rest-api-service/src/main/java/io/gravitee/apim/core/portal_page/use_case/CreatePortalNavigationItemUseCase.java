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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.CreatePortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePortalNavigationItemUseCase {

    private final PortalNavigationItemCrudService crudService;
    private final PortalNavigationItemsQueryService queryService;
    private final CreatePortalNavigationItemValidatorService validatorService;
    private final PortalPageContentCrudService pageContentCrudService;

    public Output execute(Input input) {
        final CreatePortalNavigationItem itemToCreate = input.item();
        final var organizationId = input.organizationId();
        final var environmentId = input.environmentId();

        validatorService.validate(itemToCreate, environmentId);

        final var order = itemToCreate.getOrder();
        // Order is zero based, so new max order == size()
        final var newMaxOrder = this.retrieveSiblingItems(itemToCreate.getParentId(), environmentId, itemToCreate.getArea()).size();
        // Limit the new item's order to at most new max order
        itemToCreate.setOrder(order == null ? newMaxOrder : Math.min(order, newMaxOrder));

        if (itemToCreate.getType() == PortalNavigationItemType.PAGE && itemToCreate.getPortalPageContentId() == null) {
            final var defaultPageContent = pageContentCrudService.createDefault();
            itemToCreate.setPortalPageContentId(defaultPageContent.getId());
        }

        final var newItem = PortalNavigationItem.from(itemToCreate, organizationId, environmentId);
        final var output = new Output(this.crudService.create(newItem));

        // Update orders of all following sibling items
        this.retrieveSiblingItems(newItem.getParentId(), newItem.getEnvironmentId(), itemToCreate.getArea())
            .stream()
            .filter(item -> !Objects.equals(item.getId(), newItem.getId()))
            .filter(sibling -> sibling.getOrder() >= newItem.getOrder())
            .forEach(followingSibling -> {
                followingSibling.setOrder(followingSibling.getOrder() + 1);
                this.crudService.update(followingSibling);
            });

        return output;
    }

    @Builder
    public record Input(String organizationId, String environmentId, CreatePortalNavigationItem item) {}

    public record Output(PortalNavigationItem item) {}

    private List<PortalNavigationItem> retrieveSiblingItems(PortalNavigationItemId parentId, String environmentId, PortalArea area) {
        return parentId != null
            ? queryService.findByParentIdAndEnvironmentId(environmentId, parentId)
            : queryService.findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, area);
    }
}
