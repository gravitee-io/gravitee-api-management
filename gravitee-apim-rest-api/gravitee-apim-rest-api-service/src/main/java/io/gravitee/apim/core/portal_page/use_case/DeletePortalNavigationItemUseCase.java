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
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemComparator;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeletePortalNavigationItemUseCase {

    private final PortalNavigationItemCrudService portalNavigationItemCrudService;
    private final PortalPageContentCrudService portalPageContentCrudService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    public Output execute(Input input) {
        var existing = portalNavigationItemsQueryService.findByIdAndEnvironmentId(input.environmentId(), input.navigationItemId());
        if (existing == null) {
            throw new PortalNavigationItemNotFoundException(input.navigationItemId().json());
        }

        List<PortalNavigationItemId> idsToDelete = new ArrayList<>();
        List<PortalPageContentId> contentIdsToDelete = new ArrayList<>();

        idsToDelete.add(existing.getId());
        if (existing instanceof PortalNavigationPage page) {
            contentIdsToDelete.add(page.getPortalPageContentId());
        }

        Queue<PortalNavigationItemId> toProcess = new java.util.LinkedList<>();
        toProcess.add(existing.getId());

        while (!toProcess.isEmpty()) {
            var processingId = toProcess.poll();
            portalNavigationItemsQueryService
                .findByParentIdAndEnvironmentId(input.environmentId(), processingId)
                .forEach(child -> {
                    idsToDelete.add(child.getId());
                    toProcess.add(child.getId());
                    if (child instanceof PortalNavigationPage childPage) {
                        contentIdsToDelete.add(childPage.getPortalPageContentId());
                    }
                });
        }

        // Reorder siblings at the deleted item's parent level: decrement order for siblings with order > deleted order
        var parentId = existing.getParentId();
        var deletedOrder = existing.getOrder();
        var siblings = portalNavigationItemsQueryService.findByParentIdAndEnvironmentId(input.environmentId(), parentId);
        var siblingsToUpdate = siblings
            .stream()
            .sorted(PortalNavigationItemComparator.byOrder())
            .filter(sibling -> !sibling.getId().equals(existing.getId()))
            .filter(sibling -> sibling.getOrder() > deletedOrder)
            .toList();
        siblingsToUpdate.forEach(sibling -> sibling.setOrder(sibling.getOrder() - 1));

        contentIdsToDelete.forEach(portalPageContentCrudService::delete);
        idsToDelete.forEach(portalNavigationItemCrudService::delete);
        siblingsToUpdate.forEach(portalNavigationItemCrudService::update);

        return new Output();
    }

    public record Output() {}

    public record Input(String organizationId, String environmentId, PortalNavigationItemId navigationItemId) {}
}
