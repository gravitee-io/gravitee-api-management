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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.ApiReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationItemCreationExpansionDomainService {

    private static final Comparator<Api> API_COMPARATOR = Comparator.comparing(
        Api::getName,
        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
    ).thenComparing(Api::getId);

    private final ApiProductQueryService apiProductQueryService;
    private final ApiCrudService apiCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    public Expansion expand(List<CreatePortalNavigationItem> requestedItems, String environmentId) {
        var itemsToCreate = new ArrayList<CreatePortalNavigationItem>();
        var requestedItemIds = new ArrayList<PortalNavigationItemId>();

        for (var requestedItem : requestedItems) {
            var itemWithId = ensureId(requestedItem);
            itemsToCreate.add(itemWithId);
            requestedItemIds.add(itemWithId.getId());

            if (itemWithId.getType() == PortalNavigationItemType.API_PRODUCT) {
                itemsToCreate.addAll(createApiChildren(itemWithId, environmentId));
            }
        }

        var ownershipResolver = new OwnershipResolver(itemsToCreate, environmentId);
        return new Expansion(itemsToCreate.stream().map(ownershipResolver::normalize).toList(), List.copyOf(requestedItemIds));
    }

    private static boolean isDocumentation(PortalNavigationItemType type) {
        return type == PortalNavigationItemType.PAGE || type == PortalNavigationItemType.FOLDER || type == PortalNavigationItemType.LINK;
    }

    private static NavigationItemReference inheritReference(NavigationItemReference reference, ParentContext parent) {
        if (reference instanceof ApiReference || parent == null || parent.productScoped()) {
            return reference;
        }
        if (parent.type() == PortalNavigationItemType.API && parent.apiId() != null) {
            return new ApiReference(parent.apiId());
        }
        if (parent.type() == PortalNavigationItemType.FOLDER && parent.reference() instanceof ApiReference) {
            return parent.reference();
        }
        return reference;
    }

    private final class OwnershipResolver {

        private final String environmentId;
        private final Map<PortalNavigationItemId, CreatePortalNavigationItem> pendingItems = new HashMap<>();
        private final Map<PortalNavigationItemId, ParentContext> resolvedParents = new HashMap<>();

        private OwnershipResolver(List<CreatePortalNavigationItem> items, String environmentId) {
            this.environmentId = environmentId;
            for (var item : items) {
                if (pendingItems.putIfAbsent(item.getId(), item) != null) {
                    throw new ItemAlreadyExistsException(item.getId().json());
                }
            }
        }

        private CreatePortalNavigationItem normalize(CreatePortalNavigationItem item) {
            var builder = item.toBuilder().renderedParentId(null);
            if (!isDocumentation(item.getType()) || item.getReference() instanceof ApiReference || item.getParentId() == null) {
                return builder.build();
            }

            var parent = resolveParent(item.getParentId(), item.getId());
            var reference = inheritReference(item.getReference(), parent);
            builder.reference(reference);
            if (reference instanceof ApiReference && parent.type() == PortalNavigationItemType.API) {
                builder
                    .parentId(null)
                    .renderedParentId(item.getParentId())
                    .visibility(PortalVisibility.resolve(item.getVisibility(), parent.visibility()));
            }
            return builder.build();
        }

        private ParentContext resolveParent(PortalNavigationItemId parentId, PortalNavigationItemId itemId) {
            // Resolve the original graph before detaching API-owned roots, including parents from the same batch.
            var ancestors = new ArrayList<ParentNode>();
            var visited = new HashSet<PortalNavigationItemId>();
            visited.add(itemId);
            var currentId = parentId;
            while (currentId != null && !resolvedParents.containsKey(currentId)) {
                if (!visited.add(currentId)) {
                    throw InvalidPortalNavigationItemDataException.cyclicParentHierarchy();
                }
                var node = findParent(currentId);
                ancestors.add(node);
                currentId = node.parentId();
            }

            var parent = resolvedParents.get(currentId);
            for (var node : ancestors.reversed()) {
                var reference = node.pending() && isDocumentation(node.type())
                    ? inheritReference(node.reference(), parent)
                    : node.reference();
                parent = new ParentContext(
                    node.type(),
                    reference,
                    node.apiId(),
                    PortalVisibility.resolve(node.visibility(), parent == null ? null : parent.visibility()),
                    node.type() == PortalNavigationItemType.API_PRODUCT || (parent != null && parent.productScoped())
                );
                resolvedParents.put(node.id(), parent);
            }
            return parent;
        }

        private ParentNode findParent(PortalNavigationItemId id) {
            var pending = pendingItems.get(id);
            if (pending != null) {
                return new ParentNode(
                    id,
                    pending.getParentId(),
                    pending.getType(),
                    pending.getReference(),
                    pending.getApiId(),
                    pending.getVisibility(),
                    true
                );
            }
            var persisted = navigationItemsQueryService.findByIdAndEnvironmentId(environmentId, id);
            if (persisted == null) {
                throw new ParentNotFoundException(id.json());
            }
            return new ParentNode(
                id,
                persisted.getParentId(),
                persisted.getType(),
                persisted.getReference(),
                persisted instanceof PortalNavigationApi api ? api.getApiId() : null,
                persisted.getVisibility(),
                false
            );
        }
    }

    private record ParentNode(
        PortalNavigationItemId id,
        PortalNavigationItemId parentId,
        PortalNavigationItemType type,
        NavigationItemReference reference,
        String apiId,
        PortalVisibility visibility,
        boolean pending
    ) {}

    private record ParentContext(
        PortalNavigationItemType type,
        NavigationItemReference reference,
        String apiId,
        PortalVisibility visibility,
        boolean productScoped
    ) {}

    private List<CreatePortalNavigationItem> createApiChildren(CreatePortalNavigationItem root, String environmentId) {
        var apiProductId = root.getApiProductId();
        if (apiProductId == null || apiProductId.isBlank()) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("apiProductId");
        }

        var apiProduct = apiProductQueryService
            .findById(apiProductId)
            .filter(product -> environmentId.equals(product.getEnvironmentId()))
            .orElseThrow(() -> new ApiProductNotFoundException(apiProductId));
        var apis = resolveApis(apiProduct, environmentId);
        var visibility = PortalVisibility.resolve(root.getVisibility(), PortalVisibility.PUBLIC);

        var children = new ArrayList<CreatePortalNavigationItem>(apis.size());
        for (int order = 0; order < apis.size(); order++) {
            var api = apis.get(order);
            children.add(
                CreatePortalNavigationItem.builder()
                    .id(PortalNavigationItemId.random())
                    .title(api.getName())
                    .area(root.getArea())
                    .order(order)
                    .type(PortalNavigationItemType.API)
                    .parentId(root.getId())
                    .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                    .apiId(api.getId())
                    .visibility(visibility)
                    .published(false)
                    .build()
            );
        }
        return children;
    }

    private List<Api> resolveApis(ApiProduct apiProduct, String environmentId) {
        Set<String> apiIds = apiProduct.getApiIds() != null ? apiProduct.getApiIds() : Set.of();
        if (apiIds.isEmpty()) {
            return List.of();
        }

        var apis = apiCrudService.findByIds(List.copyOf(apiIds));
        var validApisById = apis
            .stream()
            .filter(api -> environmentId.equals(api.getEnvironmentId()))
            .collect(Collectors.toMap(Api::getId, Function.identity(), (existing, duplicate) -> existing));

        apiIds
            .stream()
            .filter(apiId -> !validApisById.containsKey(apiId))
            .findFirst()
            .ifPresent(apiId -> {
                throw new ApiNotFoundException(apiId);
            });

        return validApisById.values().stream().sorted(API_COMPARATOR).toList();
    }

    private CreatePortalNavigationItem ensureId(CreatePortalNavigationItem item) {
        return item.getId() != null ? item : item.toBuilder().id(PortalNavigationItemId.random()).build();
    }

    public record Expansion(List<CreatePortalNavigationItem> itemsToCreate, List<PortalNavigationItemId> requestedItemIds) {
        public List<PortalNavigationItemId> generatedApiNavigationItemIds() {
            return itemsToCreate
                .stream()
                .filter(item -> item.getType() == PortalNavigationItemType.API)
                .filter(item -> !requestedItemIds.contains(item.getId()))
                .map(CreatePortalNavigationItem::getId)
                .toList();
        }

        public List<PortalNavigationItem> selectRequestedItems(List<PortalNavigationItem> createdItems) {
            Map<PortalNavigationItemId, PortalNavigationItem> createdItemsById = createdItems
                .stream()
                .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
            return requestedItemIds.stream().map(createdItemsById::get).toList();
        }
    }
}
