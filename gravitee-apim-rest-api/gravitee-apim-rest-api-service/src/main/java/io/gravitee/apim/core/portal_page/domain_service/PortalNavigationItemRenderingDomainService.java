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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationItemRenderingDomainService {

    private final PortalNavigationItemsQueryService queryService;

    /** Captures existing display parents before persistence so rendering the response needs no storage reads. */
    public Map<PortalNavigationItemId, PortalNavigationItemContainer> prepareRenderingParents(
        String environmentId,
        List<CreatePortalNavigationItem> normalizedCommands
    ) {
        var pendingIds = normalizedCommands.stream().map(CreatePortalNavigationItem::getId).collect(Collectors.toSet());
        var renderedParentsById = new HashMap<PortalNavigationItemId, PortalNavigationItemContainer>();
        for (var command : normalizedCommands) {
            var parentId = command.getRenderedParentId();
            if (parentId == null || pendingIds.contains(parentId) || renderedParentsById.containsKey(parentId)) {
                continue;
            }
            var parent = Optional.ofNullable(queryService.findByIdAndEnvironmentId(environmentId, parentId)).orElseThrow(() ->
                new ParentNotFoundException(parentId.json())
            );
            if (!(parent instanceof PortalNavigationItemContainer container)) {
                throw new IllegalStateException("Rendered parent is not a container: " + parentId.json());
            }
            renderedParentsById.put(parentId, new ParentSnapshot(container.getId(), container.getRootId(), container.getVisibility()));
        }
        return Map.copyOf(renderedParentsById);
    }

    public List<PortalNavigationItem> renderCreatedItems(
        List<CreatePortalNavigationItem> normalizedCommands,
        List<PortalNavigationItem> createdItems,
        Map<PortalNavigationItemId, PortalNavigationItemContainer> existingParents
    ) {
        var commandsById = normalizedCommands.stream().collect(Collectors.toMap(CreatePortalNavigationItem::getId, Function.identity()));
        // Keep canonical parents: rendering a root must not change the stored hierarchy of its children.
        var renderedParentsById = new HashMap<>(existingParents);
        for (var item : createdItems) {
            if (item instanceof PortalNavigationItemContainer parent) {
                renderedParentsById.put(item.getId(), parent);
            }
        }

        return createdItems
            .stream()
            .map(item -> {
                var renderedParentId = commandsById.get(item.getId()).getRenderedParentId();
                if (renderedParentId == null) {
                    return item;
                }
                return renderUnder(item, renderedParentsById.get(renderedParentId));
            })
            .toList();
    }

    /** Renders a copy under the selected navigation entry without reparenting the stored API documentation root. */
    public PortalNavigationItem renderUnder(PortalNavigationItem item, PortalNavigationItemContainer parent) {
        PortalNavigationItem copy = switch (item) {
            case PortalNavigationPage page -> page.toBuilder().build();
            case PortalNavigationFolder folder -> folder.toBuilder().build();
            case PortalNavigationLink link -> link.toBuilder().build();
            default -> throw new IllegalStateException("Unexpected API subtree root type: " + item.getClass().getSimpleName());
        };
        copy.updateParent(parent);
        return copy;
    }

    private record ParentSnapshot(PortalNavigationItemId id, PortalNavigationItemId rootId, PortalVisibility visibility) implements
        PortalNavigationItemContainer {
        @Override
        public PortalNavigationItemId getId() {
            return id;
        }

        @Override
        public PortalNavigationItemId getRootId() {
            return rootId;
        }

        @Override
        public PortalVisibility getVisibility() {
            return visibility;
        }
    }
}
