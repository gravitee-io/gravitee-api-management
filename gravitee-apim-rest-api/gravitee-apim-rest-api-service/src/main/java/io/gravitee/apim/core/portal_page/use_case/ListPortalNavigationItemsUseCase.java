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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiPortalSearchQueryService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemVisibilityEvaluator;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemVisibilityService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemComparator;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPortalNavigationItemsUseCase {

    private final PortalNavigationItemsQueryService queryService;
    private final List<PortalNavigationItemVisibilityService> visibilityServices;
    private final PortalNavigationItemSourceDomainService sourceDomainService;
    private final ApiPortalSearchQueryService apiPortalSearchQueryService;
    private static final Predicate<PortalNavigationItem> IS_CONTAINER_PREDICATE = i -> i instanceof PortalNavigationItemContainer;

    public Output execute(Input input) {
        var visibilityEvaluator = new PortalNavigationItemVisibilityEvaluator(
            input.environmentId(),
            input.viewerContext(),
            queryService,
            visibilityServices
        );

        List<PortalNavigationItem> rootItems;
        if (input.parentId().isPresent()) {
            var parentItem = findAndValidateParent(input, visibilityEvaluator);
            if (parentItem == null) {
                return new Output(List.of(), Map.of());
            }
            rootItems = childrenOf(parentItem, input, visibilityEvaluator);
        } else {
            rootItems = searchItems(input, null, true, visibilityEvaluator);
        }

        List<PortalNavigationItem> allItems = new ArrayList<>(rootItems);

        if (input.loadChildren()) {
            List<PortalNavigationItem> descendants = loadDescendants(rootItems, input, visibilityEvaluator);
            allItems.addAll(descendants);
        }

        allItems.stream().map(PortalNavigationItem::getSource).filter(Objects::nonNull).forEach(sourceDomainService::removeSensitiveData);

        return new Output(sortItems(allItems), resolveApis(input, allItems));
    }

    private Map<PortalNavigationItemId, Api> resolveApis(Input input, List<PortalNavigationItem> allItems) {
        if (!input.includeApis()) {
            return Map.of();
        }

        List<PortalNavigationApi> apiItems = allItems
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();

        if (apiItems.isEmpty()) {
            return Map.of();
        }

        Set<String> apiIds = apiItems.stream().map(PortalNavigationApi::getApiId).collect(Collectors.toSet());
        Map<String, Api> apisByApiId = apiPortalSearchQueryService
            .search(input.environmentId(), input.organizationId(), apiIds)
            .stream()
            .collect(Collectors.toMap(Api::getId, api -> api));

        Map<PortalNavigationItemId, Api> apisByNavigationItemId = new HashMap<>();
        for (PortalNavigationApi navItem : apiItems) {
            Api api = apisByApiId.get(navItem.getApiId());
            if (api != null) {
                apisByNavigationItemId.put(navItem.getId(), api);
            }
        }
        return apisByNavigationItemId;
    }

    private PortalNavigationItem findAndValidateParent(Input input, PortalNavigationItemVisibilityEvaluator visibilityEvaluator) {
        var parent = queryService.findByIdAndEnvironmentId(input.environmentId(), input.parentId().get());

        if (parent == null) {
            return null;
        }

        if (input.viewerContext().shouldNotShow(parent)) {
            return null;
        }

        if (!visibilityEvaluator.isVisible(parent) || visibilityEvaluator.hasHiddenAncestor(parent)) {
            return null;
        }

        return parent;
    }

    /**
     * Loads children recursively.
     * Prunes children by discarding if a child is found but deemed "not visible".
     */
    private List<PortalNavigationItem> loadDescendants(
        List<PortalNavigationItem> initialItems,
        Input input,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
        List<PortalNavigationItem> childrenAccumulator = new ArrayList<>();
        LinkedList<PortalNavigationItem> queue = new LinkedList<>();

        initialItems.stream().filter(IS_CONTAINER_PREDICATE).forEach(queue::add);

        while (!queue.isEmpty()) {
            var currentFolder = queue.removeFirst();

            var foundChildren = childrenOf(currentFolder, input, visibilityEvaluator);

            if (!foundChildren.isEmpty()) {
                childrenAccumulator.addAll(foundChildren);

                foundChildren.stream().filter(IS_CONTAINER_PREDICATE).forEach(queue::add);
            }
        }
        return childrenAccumulator;
    }

    private List<PortalNavigationItem> searchItems(
        Input input,
        PortalNavigationItemId parentId,
        boolean isRootSearch,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
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

        List<PortalNavigationItem> items = queryService.search(builder.build());
        if (isRootSearch) {
            // An API's subtree is materialized once, as a root, and spliced in under every portal's
            // nav-api row by childrenOf(...) — it must never also surface at the portal's own top level.
            items = items
                .stream()
                .filter(item -> !(item.getReference() instanceof NavigationItemReference.ApiReference))
                .toList();
        }
        return filterHiddenItems(items, input.viewerContext(), visibilityEvaluator);
    }

    /**
     * A {@link PortalNavigationApi} row can have two kinds of children: ordinary console-authored
     * sub-navigation, still physically parented under the row and found the usual way, and the API's
     * own automation-owned subtree — materialized once, keyed on the API — reached here by reference
     * rather than by the row's own parentId, so it renders identically under every portal row that
     * lists the API. Both are real; this returns their union.
     */
    private List<PortalNavigationItem> childrenOf(
        PortalNavigationItem parent,
        Input input,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
        if (parent instanceof PortalNavigationApi navApi) {
            var physicalChildren = searchItems(input, navApi.getId(), false, visibilityEvaluator);
            var splicedRoots = queryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
                input.environmentId(),
                input.portalArea(),
                new NavigationItemReference.ApiReference(navApi.getApiId())
            );
            var combined = new ArrayList<>(physicalChildren);
            combined.addAll(renderUnder(navApi, filterForViewer(splicedRoots, input, visibilityEvaluator)));
            return combined;
        }
        return searchItems(input, parent.getId(), false, visibilityEvaluator);
    }

    /**
     * {@code findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference} has no published/visibility
     * criteria of its own — unlike {@code searchItems}'s query builder — so portal-mode narrowing is
     * applied here in Java before the shared {@link #filterHiddenItems} pass.
     */
    private List<PortalNavigationItem> filterForViewer(
        List<PortalNavigationItem> items,
        Input input,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
        var viewerContext = input.viewerContext();
        var narrowed = items;
        if (viewerContext.isPortalMode()) {
            narrowed = narrowed
                .stream()
                .filter(item -> Boolean.TRUE.equals(item.getPublished()))
                .toList();
            if (!viewerContext.isAuthenticated()) {
                narrowed = narrowed
                    .stream()
                    .filter(item -> item.getVisibility() == PortalVisibility.PUBLIC)
                    .toList();
            }
        }
        return filterHiddenItems(narrowed, viewerContext, visibilityEvaluator);
    }

    /**
     * The returned model deliberately does not mirror storage: each spliced root is a copy with its
     * {@code parentId} rewritten to the nav-api row it renders under here, so every client keeps
     * rebuilding the tree from {@code parentId} unchanged. The original stored item — root, with no
     * parent — is never mutated; the same root renders differently under each portal that lists the API.
     */
    private List<PortalNavigationItem> renderUnder(PortalNavigationItemContainer navApiRow, List<PortalNavigationItem> roots) {
        return roots
            .stream()
            .map(root -> renderedCopy(root, navApiRow))
            .toList();
    }

    private PortalNavigationItem renderedCopy(PortalNavigationItem item, PortalNavigationItemContainer parent) {
        PortalNavigationItem copy = switch (item) {
            case PortalNavigationPage page -> PortalNavigationPage.builder()
                .id(page.getId())
                .organizationId(page.getOrganizationId())
                .environmentId(page.getEnvironmentId())
                .reference(page.getReference())
                .title(page.getTitle())
                .segment(page.getSegment())
                .area(page.getArea())
                .order(page.getOrder())
                .published(page.getPublished())
                .visibility(page.getVisibility())
                .source(page.getSource())
                .automationMetadata(page.getAutomationMetadata())
                .portalPageContentId(page.getPortalPageContentId())
                .build();
            case PortalNavigationFolder folder -> PortalNavigationFolder.builder()
                .id(folder.getId())
                .organizationId(folder.getOrganizationId())
                .environmentId(folder.getEnvironmentId())
                .reference(folder.getReference())
                .title(folder.getTitle())
                .segment(folder.getSegment())
                .area(folder.getArea())
                .order(folder.getOrder())
                .published(folder.getPublished())
                .visibility(folder.getVisibility())
                .source(folder.getSource())
                .automationMetadata(folder.getAutomationMetadata())
                .build();
            case PortalNavigationLink link -> PortalNavigationLink.builder()
                .id(link.getId())
                .organizationId(link.getOrganizationId())
                .environmentId(link.getEnvironmentId())
                .reference(link.getReference())
                .title(link.getTitle())
                .segment(link.getSegment())
                .area(link.getArea())
                .order(link.getOrder())
                .published(link.getPublished())
                .visibility(link.getVisibility())
                .source(link.getSource())
                .automationMetadata(link.getAutomationMetadata())
                .url(link.getUrl())
                .build();
            default -> throw new IllegalStateException("Unexpected API subtree root type: " + item.getClass().getSimpleName());
        };
        copy.updateParent(parent);
        return copy;
    }

    /**
     * Drops API and API product navigation items the viewer must not see. When a container is
     * dropped here it is also not enqueued by the BFS in {@link #loadDescendants}, so its
     * descendants are naturally excluded from the result.
     */
    private List<PortalNavigationItem> filterHiddenItems(
        List<PortalNavigationItem> items,
        PortalNavigationItemViewerContext viewerContext,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
        if (!viewerContext.isPortalMode()) {
            return items;
        }
        return items.stream().filter(visibilityEvaluator::isVisible).toList();
    }

    private List<PortalNavigationItem> sortItems(List<PortalNavigationItem> items) {
        return items.stream().sorted(PortalNavigationItemComparator.byNullableParentIdThenNullableOrder()).toList();
    }

    public record Output(List<PortalNavigationItem> items, Map<PortalNavigationItemId, Api> apis) {}

    public record Input(
        String environmentId,
        String organizationId,
        PortalArea portalArea,
        Optional<PortalNavigationItemId> parentId,
        boolean loadChildren,
        PortalNavigationItemViewerContext viewerContext,
        boolean includeApis
    ) {}
}
