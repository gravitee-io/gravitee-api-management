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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import jakarta.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationAccessDomainService {

    private final PortalCrudService portalCrudService;
    private final ApiCrudService apiCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalPageContentQueryService pageContentQueryService;
    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final ApiQueryService apiQueryService;

    PortalReadAccess openPortalRead(String organizationId, String environmentId, PortalId portalId) {
        var scope = new NavigationItemReference.PortalReference(portalId);
        requireScope(organizationId, environmentId, scope);
        return new PortalReadAccess(organizationId, environmentId, scope);
    }

    final class PortalReadAccess {

        private static final int BATCH_SIZE = 500;

        private final String organizationId;
        private final String environmentId;
        private final NavigationItemReference.PortalReference scope;
        private final Map<PortalNavigationItemId, PortalNavigationItem> itemsById = new HashMap<>();
        private final Set<PortalNavigationItemId> loadedIds = new HashSet<>();
        private final Map<PortalNavigationItemId, Boolean> consistentChainById = new HashMap<>();
        private final Map<PortalNavigationItemId, Boolean> productContextById = new HashMap<>();
        private final Map<String, Boolean> apiAccessById = new HashMap<>();

        private PortalReadAccess(String organizationId, String environmentId, NavigationItemReference.PortalReference scope) {
            this.organizationId = organizationId;
            this.environmentId = environmentId;
            this.scope = scope;
        }

        void remember(Collection<PortalNavigationItem> items) {
            // Display projections may reparent a shared API root, so keep the stored relationships for authorization.
            for (var item : items) {
                itemsById.putIfAbsent(item.getId(), item);
                loadedIds.add(item.getId());
            }
        }

        PortalNavigationItem requireParent(PortalNavigationItemId parentId) {
            var parent = navigationItemsQueryService.findByIdAndEnvironmentId(environmentId, parentId);
            if (parent == null) {
                throw notFound(parentId);
            }
            remember(List.of(parent));
            requireItems(List.of(parent));
            return parent;
        }

        void requireItems(Collection<PortalNavigationItem> displayedItems) {
            var canonicalItems = displayedItems
                .stream()
                .map(item -> {
                    var canonical = itemsById.get(item.getId());
                    if (canonical == null) {
                        throw notFound(item.getId());
                    }
                    return canonical;
                })
                .toList();
            loadAncestors(canonicalItems);
            var apiIds = new HashSet<String>();
            for (var item : canonicalItems) {
                if (!hasConsistentOwnerChain(item)) {
                    throw notFound(item.getId());
                }
                if (scope.equals(item.getReference())) {
                    continue;
                }
                if (
                    item.getReference() instanceof NavigationItemReference.ApiReference api &&
                    apiOwnedNavigationDomainService.isDocumentation(item)
                ) {
                    apiIds.add(api.apiId());
                } else {
                    throw notFound(item.getId());
                }
            }
            loadApiAccess(apiIds);
            for (var item : canonicalItems) {
                if (
                    item.getReference() instanceof NavigationItemReference.ApiReference api &&
                    !Boolean.TRUE.equals(apiAccessById.get(api.apiId()))
                ) {
                    throw notFound(item.getId());
                }
            }
        }

        private void loadAncestors(Collection<PortalNavigationItem> items) {
            var pending = new ArrayDeque<>(items);
            var visited = new HashSet<PortalNavigationItemId>();
            while (!pending.isEmpty()) {
                var missingIds = new HashSet<PortalNavigationItemId>();
                while (!pending.isEmpty()) {
                    var item = pending.removeFirst();
                    if (!visited.add(item.getId()) || item.getParentId() == null) {
                        continue;
                    }
                    var parent = itemsById.get(item.getParentId());
                    if (parent != null) {
                        pending.addLast(parent);
                    } else if (!loadedIds.contains(item.getParentId())) {
                        missingIds.add(item.getParentId());
                    }
                }
                if (missingIds.isEmpty()) {
                    return;
                }
                loadedIds.addAll(missingIds);
                var ids = new ArrayList<>(missingIds);
                var ancestors = new ArrayList<PortalNavigationItem>();
                for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
                    ancestors.addAll(
                        navigationItemsQueryService.search(
                            PortalNavigationItemQueryCriteria.builder()
                                .environmentId(environmentId)
                                .ids(Set.copyOf(ids.subList(start, Math.min(start + BATCH_SIZE, ids.size()))))
                                .build()
                        )
                    );
                }
                remember(ancestors);
                pending.addAll(ancestors);
            }
        }

        private boolean hasConsistentOwnerChain(PortalNavigationItem item) {
            var path = new HashSet<PortalNavigationItemId>();
            var current = item;
            boolean valid;
            while (true) {
                var cached = consistentChainById.get(current.getId());
                if (cached != null) {
                    valid = cached;
                    break;
                }
                if (
                    !path.add(current.getId()) ||
                    !organizationId.equals(current.getOrganizationId()) ||
                    !environmentId.equals(current.getEnvironmentId())
                ) {
                    valid = false;
                    break;
                }
                if (current.getParentId() == null) {
                    valid = true;
                    break;
                }
                var parent = itemsById.get(current.getParentId());
                if (parent == null || !Objects.equals(current.getReference(), parent.getReference())) {
                    valid = false;
                    break;
                }
                current = parent;
            }
            for (var id : path) {
                consistentChainById.put(id, valid);
            }
            return valid;
        }

        private void loadApiAccess(Set<String> apiIds) {
            var pendingIds = apiIds
                .stream()
                .filter(id -> !apiAccessById.containsKey(id))
                .collect(Collectors.toSet());
            if (pendingIds.isEmpty()) {
                return;
            }
            pendingIds.forEach(id -> apiAccessById.put(id, false));
            var existingIds = new ArrayList<>(apiQueryService.findExistingIds(pendingIds, environmentId));
            var listings = new ArrayList<PortalNavigationItem>();
            for (int start = 0; start < existingIds.size(); start += BATCH_SIZE) {
                listings.addAll(
                    navigationItemsQueryService.search(
                        PortalNavigationItemQueryCriteria.builder()
                            .environmentId(environmentId)
                            .reference(scope)
                            .type(PortalNavigationItemType.API)
                            .apiIds(Set.copyOf(existingIds.subList(start, Math.min(start + BATCH_SIZE, existingIds.size()))))
                            .build()
                    )
                );
            }
            remember(listings);
            loadAncestors(listings);
            for (var listing : listings) {
                if (
                    listing instanceof PortalNavigationApi api &&
                    scope.equals(listing.getReference()) &&
                    hasConsistentOwnerChain(listing) &&
                    !isInApiProductContext(listing)
                ) {
                    apiAccessById.put(api.getApiId(), true);
                }
            }
        }

        private boolean isInApiProductContext(PortalNavigationItem item) {
            var path = new HashSet<PortalNavigationItemId>();
            var current = item;
            boolean inProduct;
            while (true) {
                var cached = productContextById.get(current.getId());
                if (cached != null) {
                    inProduct = cached;
                    break;
                }
                path.add(current.getId());
                if (current instanceof PortalNavigationApiProduct) {
                    inProduct = true;
                    break;
                }
                if (current.getParentId() == null) {
                    inProduct = false;
                    break;
                }
                current = itemsById.get(current.getParentId());
            }
            for (var id : path) {
                productContextById.put(id, inProduct);
            }
            return inProduct;
        }
    }

    public void requireScope(String organizationId, String environmentId, NavigationItemReference scope) {
        switch (Objects.requireNonNull(scope, "scope")) {
            case NavigationItemReference.PortalReference portal -> portalCrudService
                .findByIdAndEnvironmentId(portal.portalId(), environmentId)
                .filter(found -> organizationId.equals(found.getOrganizationId()))
                .orElseThrow(() -> new PortalNotFoundException(portal.portalId().toString()));
            case NavigationItemReference.ApiReference api -> apiCrudService
                .findById(api.apiId())
                .filter(found -> environmentId.equals(found.getEnvironmentId()))
                .orElseThrow(() -> new ApiNotFoundException(api.apiId()));
        }
    }

    public PortalNavigationItem requireItem(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        PortalNavigationItemId itemId
    ) {
        requireScope(organizationId, environmentId, scope);
        var item = navigationItemsQueryService.findByIdAndEnvironmentId(environmentId, itemId);
        return requireAccessibleItem(organizationId, environmentId, scope, itemId, item);
    }

    public void requireParent(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        @Nullable PortalNavigationItemId parentId
    ) {
        if (parentId != null) {
            requireItem(organizationId, environmentId, scope, parentId);
        }
    }

    public void requireCreateParents(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        List<CreatePortalNavigationItem> items
    ) {
        requireScope(organizationId, environmentId, scope);
        var pendingItems = new HashMap<PortalNavigationItemId, CreatePortalNavigationItem>();
        items
            .stream()
            .filter(item -> item.getId() != null)
            .forEach(item -> pendingItems.put(item.getId(), item));
        for (var item : items) {
            var visited = new HashSet<PortalNavigationItemId>();
            if (item.getId() != null) {
                visited.add(item.getId());
            }
            var current = item;
            while (current != null) {
                var owner = current.getReference();
                if (
                    owner != null &&
                    !scope.equals(owner) &&
                    (scope instanceof NavigationItemReference.ApiReference || owner instanceof NavigationItemReference.PortalReference)
                ) {
                    throw notFound(current.getId());
                }
                var parentId = current.getRenderedParentId() != null ? current.getRenderedParentId() : current.getParentId();
                if (parentId == null) {
                    break;
                }
                if (!visited.add(parentId)) {
                    throw InvalidPortalNavigationItemDataException.cyclicParentHierarchy();
                }
                current = pendingItems.get(parentId);
                if (current == null) {
                    requireItem(organizationId, environmentId, scope, parentId);
                }
            }
        }
    }

    public PortalPageContent<?> requireContent(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        PortalPageContentId contentId
    ) {
        return findAccessibleContent(organizationId, environmentId, scope, contentId).orElseThrow(() ->
            new PageContentNotFoundException(contentId.json())
        );
    }

    public void checkContentAccess(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        PortalPageContentId contentId
    ) {
        findAccessibleContent(organizationId, environmentId, scope, contentId);
    }

    private Optional<PortalPageContent<?>> findAccessibleContent(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        PortalPageContentId contentId
    ) {
        requireScope(organizationId, environmentId, scope);
        var pages = navigationItemsQueryService.findNavigationPagesByPortalPageContentId(environmentId, contentId);
        if (pages.isEmpty() || pages.stream().anyMatch(page -> !isAccessible(organizationId, environmentId, scope, page))) {
            throw new PageContentNotFoundException(contentId.json());
        }
        var content = pageContentQueryService.findById(contentId);
        content.ifPresent(found -> {
            if (!organizationId.equals(found.getOrganizationId()) || !environmentId.equals(found.getEnvironmentId())) {
                throw new PageContentNotFoundException(contentId.json());
            }
        });
        return content;
    }

    public List<PortalNavigationItem> requireSubtree(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        PortalNavigationItem root,
        boolean includeApiDocumentation
    ) {
        requireScope(organizationId, environmentId, scope);
        var result = new ArrayList<PortalNavigationItem>();
        var visited = new HashSet<PortalNavigationItemId>();
        var pending = new ArrayDeque<PortalNavigationItem>();
        pending.add(requireItem(organizationId, environmentId, scope, root.getId()));
        while (!pending.isEmpty()) {
            var item = pending.removeFirst();
            if (!visited.add(item.getId())) {
                continue;
            }
            result.add(requireAccessibleItem(organizationId, environmentId, scope, item.getId(), item));
            pending.addAll(navigationItemsQueryService.findByParentIdAndEnvironmentId(environmentId, item.getId()));
            if (
                includeApiDocumentation &&
                item instanceof PortalNavigationApi api &&
                !apiOwnedNavigationDomainService.isInApiProductContext(environmentId, item)
            ) {
                pending.addAll(
                    navigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
                        environmentId,
                        item.getArea(),
                        new NavigationItemReference.ApiReference(api.getApiId())
                    )
                );
            }
        }
        return List.copyOf(result);
    }

    private PortalNavigationItem requireAccessibleItem(
        String organizationId,
        String environmentId,
        NavigationItemReference scope,
        PortalNavigationItemId itemId,
        @Nullable PortalNavigationItem item
    ) {
        if (item == null || !isAccessible(organizationId, environmentId, scope, item)) {
            throw notFound(itemId);
        }
        return item;
    }

    private boolean isAccessible(String organizationId, String environmentId, NavigationItemReference scope, PortalNavigationItem item) {
        if (!hasConsistentOwnerChain(organizationId, environmentId, item)) {
            return false;
        }
        if (scope.equals(item.getReference())) {
            return true;
        }
        if (
            !(scope instanceof NavigationItemReference.PortalReference) ||
            !(item.getReference() instanceof NavigationItemReference.ApiReference api) ||
            !apiOwnedNavigationDomainService.isDocumentation(item)
        ) {
            return false;
        }
        if (
            apiCrudService
                .findById(api.apiId())
                .filter(found -> environmentId.equals(found.getEnvironmentId()))
                .isEmpty()
        ) {
            return false;
        }
        return navigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .reference(scope)
                    .type(PortalNavigationItemType.API)
                    .apiIds(Set.of(api.apiId()))
                    .build()
            )
            .stream()
            .filter(listing -> hasConsistentOwnerChain(organizationId, environmentId, listing))
            .anyMatch(listing -> !apiOwnedNavigationDomainService.isInApiProductContext(environmentId, listing));
    }

    private boolean hasConsistentOwnerChain(String organizationId, String environmentId, PortalNavigationItem item) {
        var owner = item.getReference();
        var visited = new HashSet<PortalNavigationItemId>();
        var current = item;
        while (current != null) {
            if (
                !visited.add(current.getId()) ||
                !organizationId.equals(current.getOrganizationId()) ||
                !environmentId.equals(current.getEnvironmentId()) ||
                !Objects.equals(owner, current.getReference())
            ) {
                return false;
            }
            if (current.getParentId() == null) {
                return true;
            }
            current = navigationItemsQueryService.findByIdAndEnvironmentId(environmentId, current.getParentId());
        }
        return false;
    }

    private static PortalNavigationItemNotFoundException notFound(@Nullable PortalNavigationItemId itemId) {
        return new PortalNavigationItemNotFoundException(itemId == null ? "unknown" : itemId.json());
    }
}
