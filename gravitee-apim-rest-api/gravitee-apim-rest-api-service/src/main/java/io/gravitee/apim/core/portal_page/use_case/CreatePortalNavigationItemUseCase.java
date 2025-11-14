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
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePortalNavigationItemUseCase {

    private final PortalNavigationItemCrudService portalNavigationItemCrudService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    public Output execute(Input input) {
        final PortalNavigationItem itemToCreate = input.item();

        validate(itemToCreate);

        final var maxOrder = this.retrieveSiblingItems(itemToCreate).stream().mapToInt(PortalNavigationItem::getOrder).max().orElse(-1);
        // Limit the new item's order to at most one position after the current maximum
        itemToCreate.setOrder(Math.min(itemToCreate.getOrder(), maxOrder + 1));

        final var output = new Output(this.portalNavigationItemCrudService.create(itemToCreate));

        // Update orders of all following items
        this.retrieveSiblingItems(itemToCreate)
            .stream()
            .filter(item -> !Objects.equals(item.getId(), itemToCreate.getId()))
            .filter(item -> item.getOrder() >= itemToCreate.getOrder())
            .forEach(item -> {
                item.setOrder(item.getOrder() + 1);
                this.portalNavigationItemCrudService.update(item);
            });

        return output;
    }

    @Builder
    public record Input(PortalNavigationItem item) {}

    public record Output(PortalNavigationItem item) {}

    private void validate(PortalNavigationItem item) {
        final var parentId = item.getParentId();
        if (parentId != null) {
            final var parentItem = portalNavigationItemsQueryService.findByIdAndEnvironmentId(item.getEnvironmentId(), parentId);
            if (parentItem == null) {
                throw new ParentNotFoundException(String.format("Parent item with id %s does not exist", parentId));
            }
            if (!parentItem.getClass().equals(PortalNavigationFolder.class)) {
                throw new ParentTypeMismatchException(String.format("Parent item with id %s is not a folder", parentId));
            }
            if (!parentItem.getArea().equals(item.getArea())) {
                throw new ParentAreaMismatchException(
                    String.format("Parent item with id %s belongs to a different area than the child item", parentId)
                );
            }
        }

        if (item.getArea().equals(PortalArea.HOMEPAGE)) {
            final var existingHomepage = portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(
                item.getEnvironmentId(),
                item.getArea()
            );
            if (!existingHomepage.isEmpty()) {
                throw new HomepageAlreadyExistsException("Homepage already exists");
            }
        }

        if (item instanceof PortalNavigationPage page) {
            final var contentId = page.getPortalPageContentId();
            // TODO check that content with provided id exists, when portal page content dev is ready
        }
    }

    private List<PortalNavigationItem> retrieveSiblingItems(PortalNavigationItem item) {
        return item.getParentId() != null
            ? portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(item.getEnvironmentId(), item.getParentId())
            : portalNavigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalArea(item.getEnvironmentId(), item.getArea());
    }
}
