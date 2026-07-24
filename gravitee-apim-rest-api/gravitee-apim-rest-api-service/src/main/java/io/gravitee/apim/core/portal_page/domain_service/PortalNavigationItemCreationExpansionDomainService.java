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
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import java.util.Comparator;
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

        return new Expansion(List.copyOf(itemsToCreate), List.copyOf(requestedItemIds));
    }

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
        var visibility = root.getVisibility() != null ? root.getVisibility() : PortalVisibility.PUBLIC;

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
        public List<PortalNavigationItem> selectRequestedItems(List<PortalNavigationItem> createdItems) {
            Map<PortalNavigationItemId, PortalNavigationItem> createdItemsById = createdItems
                .stream()
                .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
            return requestedItemIds.stream().map(createdItemsById::get).toList();
        }
    }
}
