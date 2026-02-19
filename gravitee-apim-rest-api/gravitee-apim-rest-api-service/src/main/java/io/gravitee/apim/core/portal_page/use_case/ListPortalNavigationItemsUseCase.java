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
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemComparator;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPortalNavigationItemsUseCase {

    private final PortalNavigationItemsQueryService queryService;
    private static final Predicate<PortalNavigationItem> IS_CONTAINER_PREDICATE = i -> i instanceof PortalNavigationItemContainer;

    public Output execute(Input input) {
        PortalNavigationItem parentItem;
        if (input.parentId().isPresent()) {
            parentItem = findAndValidateParent(input);
            if (parentItem == null) {
                return new Output(List.of());
            }
        }

        List<PortalNavigationItem> rootItems = searchItems(input, input.parentId().orElse(null), input.parentId().isEmpty());

        List<PortalNavigationItem> allItems = new ArrayList<>(rootItems);

        if (input.loadChildren()) {
            List<PortalNavigationItem> descendants = loadDescendants(rootItems, input);
            allItems.addAll(descendants);
        }

        return new Output(sortItems(allItems));
    }

    private PortalNavigationItem findAndValidateParent(Input input) {
        var parent = queryService.findByIdAndEnvironmentId(input.environmentId(), input.parentId().get());

        if (parent == null) {
            return null;
        }

        if (input.viewerContext().shouldNotShow(parent)) {
            return null;
        }

        return parent;
    }

    /**
     * Loads children recursively.
     * Prunes children by discarding if a child is found but deemed "not visible".
     */
    private List<PortalNavigationItem> loadDescendants(List<PortalNavigationItem> initialItems, Input input) {
        List<PortalNavigationItem> childrenAccumulator = new ArrayList<>();
        LinkedList<PortalNavigationItem> queue = new LinkedList<>();

        initialItems.stream().filter(IS_CONTAINER_PREDICATE).forEach(queue::add);

        while (!queue.isEmpty()) {
            var currentFolder = queue.removeFirst();

            var foundChildren = searchItems(input, currentFolder.getId(), false);

            if (!foundChildren.isEmpty()) {
                childrenAccumulator.addAll(foundChildren);

                foundChildren.stream().filter(IS_CONTAINER_PREDICATE).forEach(queue::add);
            }
        }
        return childrenAccumulator;
    }

    private List<PortalNavigationItem> searchItems(Input input, PortalNavigationItemId parentId, boolean isRootSearch) {
        var builder = PortalNavigationItemQueryCriteria.builder()
            .environmentId(input.environmentId())
            .area(input.portalArea())
            .parentId(parentId)
            .root(isRootSearch);

        if (input.viewerContext().isPortalMode()) {
            builder.published(true);

            if (!input.viewerContext().isAuthenticated()) {
                builder.visibility(PortalVisibility.PUBLIC);
            }
        }

        return queryService.search(builder.build());
    }

    private List<PortalNavigationItem> sortItems(List<PortalNavigationItem> items) {
        return items.stream().sorted(PortalNavigationItemComparator.byNullableParentIdThenNullableOrder()).toList();
    }

    public record Output(List<PortalNavigationItem> items) {}

    public record Input(
        String environmentId,
        PortalArea portalArea,
        Optional<PortalNavigationItemId> parentId,
        boolean loadChildren,
        PortalNavigationItemViewerContext viewerContext
    ) {}
}
