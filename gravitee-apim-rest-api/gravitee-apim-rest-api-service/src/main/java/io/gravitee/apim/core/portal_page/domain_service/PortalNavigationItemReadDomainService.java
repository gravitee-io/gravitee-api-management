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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiPortalSearchQueryService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService.PortalReadAccess;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemComparator;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemContainer;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import jakarta.annotation.Nullable;
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

@DomainService
@RequiredArgsConstructor
public class PortalNavigationItemReadDomainService {

    private final PortalNavigationItemsQueryService queryService;
    private final List<PortalNavigationItemVisibilityService> visibilityServices;
    private final PortalNavigationItemSourceDomainService sourceDomainService;
    private final ApiPortalSearchQueryService apiPortalSearchQueryService;
    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final PortalNavigationItemRenderingDomainService renderingDomainService;
    private final PortalNavigationAccessDomainService accessService;
    private static final Predicate<PortalNavigationItem> IS_CONTAINER_PREDICATE = i -> i instanceof PortalNavigationItemContainer;

    /** Reads a displayed tree by environment, area, parent and optional owner, preserving the caller's criteria. */
    public List<PortalNavigationItem> read(
        PortalNavigationItemQueryCriteria criteria,
        PortalNavigationItemViewerContext viewerContext,
        boolean loadChildren
    ) {
        return read(criteria, viewerContext, loadChildren, null);
    }

    public List<PortalNavigationItem> readForPortal(
        String organizationId,
        String environmentId,
        PortalId portalId,
        PortalArea area,
        Optional<PortalNavigationItemId> parentId,
        boolean loadChildren
    ) {
        var access = accessService.openPortalRead(organizationId, environmentId, portalId);
        var criteria = PortalNavigationItemQueryCriteria.builder()
            .environmentId(environmentId)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .area(area)
            .parentId(parentId.orElse(null))
            .build();
        return read(criteria, PortalNavigationItemViewerContext.forConsole(), loadChildren, access);
    }

    private List<PortalNavigationItem> read(
        PortalNavigationItemQueryCriteria criteria,
        PortalNavigationItemViewerContext viewerContext,
        boolean loadChildren,
        @Nullable PortalReadAccess access
    ) {
        Map<PortalNavigationItemId, Boolean> productContextById = new HashMap<>();
        var visibilityEvaluator = new PortalNavigationItemVisibilityEvaluator(
            criteria.getEnvironmentId(),
            viewerContext,
            queryService,
            visibilityServices
        );

        List<PortalNavigationItem> rootItems;
        if (criteria.getParentId() != null) {
            var parentItem = findAndValidateParent(criteria, viewerContext, visibilityEvaluator, access);
            if (parentItem == null) {
                return List.of();
            }
            rootItems = childrenOf(parentItem, criteria, viewerContext, visibilityEvaluator, productContextById, access);
        } else {
            rootItems = searchItems(criteria, null, viewerContext, visibilityEvaluator, access);
        }

        List<PortalNavigationItem> allItems = new ArrayList<>(rootItems);
        if (loadChildren) {
            allItems.addAll(loadDescendants(rootItems, criteria, viewerContext, visibilityEvaluator, productContextById, access));
        }
        if (access != null) {
            access.requireItems(allItems);
        }
        allItems.stream().map(PortalNavigationItem::getSource).filter(Objects::nonNull).forEach(sourceDomainService::removeSensitiveData);

        return allItems.stream().sorted(PortalNavigationItemComparator.byNullableParentIdThenNullableOrder()).toList();
    }

    public Map<PortalNavigationItemId, Api> resolveApis(String organizationId, String environmentId, List<PortalNavigationItem> items) {
        List<PortalNavigationApi> apiItems = items
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();
        if (apiItems.isEmpty()) {
            return Map.of();
        }

        Set<String> apiIds = apiItems.stream().map(PortalNavigationApi::getApiId).collect(Collectors.toSet());
        Map<String, Api> apisByApiId = apiPortalSearchQueryService
            .search(environmentId, organizationId, apiIds)
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

    private PortalNavigationItem findAndValidateParent(
        PortalNavigationItemQueryCriteria criteria,
        PortalNavigationItemViewerContext viewerContext,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator,
        @Nullable PortalReadAccess access
    ) {
        var parent = access == null
            ? queryService.findByIdAndEnvironmentId(criteria.getEnvironmentId(), criteria.getParentId())
            : access.requireParent(criteria.getParentId());
        if (parent == null || viewerContext.shouldNotShow(parent)) {
            return null;
        }
        if (!visibilityEvaluator.isVisible(parent) || visibilityEvaluator.hasHiddenAncestor(parent)) {
            return null;
        }
        return parent;
    }

    /** Prunes hidden containers before traversing their children. */
    private List<PortalNavigationItem> loadDescendants(
        List<PortalNavigationItem> initialItems,
        PortalNavigationItemQueryCriteria criteria,
        PortalNavigationItemViewerContext viewerContext,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator,
        Map<PortalNavigationItemId, Boolean> productContextById,
        @Nullable PortalReadAccess access
    ) {
        List<PortalNavigationItem> childrenAccumulator = new ArrayList<>();
        LinkedList<PortalNavigationItem> queue = new LinkedList<>();
        initialItems.stream().filter(IS_CONTAINER_PREDICATE).forEach(queue::add);

        while (!queue.isEmpty()) {
            var currentFolder = queue.removeFirst();
            var foundChildren = childrenOf(currentFolder, criteria, viewerContext, visibilityEvaluator, productContextById, access);
            if (!foundChildren.isEmpty()) {
                childrenAccumulator.addAll(foundChildren);
                foundChildren.stream().filter(IS_CONTAINER_PREDICATE).forEach(queue::add);
            }
        }
        return childrenAccumulator;
    }

    private List<PortalNavigationItem> searchItems(
        PortalNavigationItemQueryCriteria criteria,
        @Nullable PortalNavigationItem parent,
        PortalNavigationItemViewerContext viewerContext,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator,
        @Nullable PortalReadAccess access
    ) {
        var builder = PortalNavigationItemQueryCriteria.builder()
            .environmentId(criteria.getEnvironmentId())
            .area(criteria.getArea())
            .parentId(parent == null ? null : parent.getId())
            .root(parent == null);

        if (criteria.getReference() != null) {
            builder.reference(parent == null ? criteria.getReference() : parent.getReference());
        }
        if (viewerContext.isPortalMode()) {
            builder.published(true);
            if (!viewerContext.isAuthenticated()) {
                builder.visibility(PortalVisibility.PUBLIC);
            }
        }

        List<PortalNavigationItem> items = queryService.search(builder.build());
        if (access != null) {
            access.remember(items);
        }
        if (parent == null) {
            // API-owned roots are displayed under each standalone API listing, rather than at the portal's top level.
            items = items
                .stream()
                .filter(item -> !(item.getReference() instanceof NavigationItemReference.ApiReference))
                .toList();
        }
        return filterHiddenItems(items, viewerContext, visibilityEvaluator);
    }

    /** Standalone API listings combine physical children with shared API-owned roots; product listings keep physical children. */
    private List<PortalNavigationItem> childrenOf(
        PortalNavigationItem parent,
        PortalNavigationItemQueryCriteria criteria,
        PortalNavigationItemViewerContext viewerContext,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator,
        Map<PortalNavigationItemId, Boolean> productContextById,
        @Nullable PortalReadAccess access
    ) {
        if (parent instanceof PortalNavigationApi navApi) {
            var physicalChildren = searchItems(criteria, navApi, viewerContext, visibilityEvaluator, access);
            if (apiOwnedNavigationDomainService.isInApiProductContext(criteria.getEnvironmentId(), navApi, productContextById)) {
                return physicalChildren;
            }
            var splicedRoots = queryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
                criteria.getEnvironmentId(),
                criteria.getArea(),
                new NavigationItemReference.ApiReference(navApi.getApiId())
            );
            if (access != null) {
                access.remember(splicedRoots);
            }
            var combined = new ArrayList<>(physicalChildren);
            combined.addAll(
                filterForViewer(splicedRoots, viewerContext, visibilityEvaluator)
                    .stream()
                    .map(root -> renderingDomainService.renderUnder(root, navApi))
                    .toList()
            );
            return combined;
        }
        return searchItems(criteria, parent, viewerContext, visibilityEvaluator, access);
    }

    /** API-owned roots are queried without visibility criteria, so apply the portal viewer's narrowing before membership filtering. */
    private List<PortalNavigationItem> filterForViewer(
        List<PortalNavigationItem> items,
        PortalNavigationItemViewerContext viewerContext,
        PortalNavigationItemVisibilityEvaluator visibilityEvaluator
    ) {
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
}
