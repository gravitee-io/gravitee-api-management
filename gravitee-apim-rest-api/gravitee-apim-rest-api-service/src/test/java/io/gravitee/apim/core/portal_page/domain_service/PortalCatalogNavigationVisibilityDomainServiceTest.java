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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalCatalogNavigationVisibilityDomainServiceTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final PortalNavigationItemViewerContext VIEWER_CONTEXT = PortalNavigationItemViewerContext.forPortal("user-id");

    private PortalCatalogNavigationVisibilityDomainService service;

    @BeforeEach
    void set_up() {
        var apiProductVisibilityDomainService = new PortalNavigationApiProductVisibilityDomainService(
            new PortalNavigationItemsQueryServiceInMemory(),
            new ApiProductAccessibleIdsDomainService(new ApiProductQueryServiceInMemory(), new MembershipQueryServiceInMemory())
        );
        service = new PortalCatalogNavigationVisibilityDomainService(apiProductVisibilityDomainService);
    }

    @Test
    void should_filter_api_and_api_product_below_the_same_hidden_parent() {
        var parent = folder("hidden-parent", null);
        parent.setPublished(false);
        var api = api("api-item", parent.getId(), PortalVisibility.PUBLIC);
        var apiProduct = apiProduct("api-product-item", parent.getId(), PortalVisibility.PUBLIC);
        var items = List.<PortalNavigationItem>of(parent, api, apiProduct);

        var result = filter(List.of(api, apiProduct), items, Set.of(api.getId()), Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    void should_filter_an_item_below_a_hidden_multi_level_ancestor() {
        var hiddenParent = folder("hidden-parent", null);
        hiddenParent.setPublished(false);
        var visibleParent = folder("visible-parent", hiddenParent.getId());
        var apiProduct = apiProduct("api-product-item", visibleParent.getId(), PortalVisibility.PUBLIC);
        var items = List.<PortalNavigationItem>of(hiddenParent, visibleParent, apiProduct);

        var result = filter(List.of(apiProduct), items, Set.of(), Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    void should_keep_an_item_when_its_parent_is_missing() {
        var apiProduct = apiProduct("api-product-item", PortalNavigationItemId.random(), PortalVisibility.PUBLIC);

        var result = filter(List.of(apiProduct), List.of(apiProduct), Set.of(), Set.of());

        assertThat(result).containsExactly(apiProduct);
    }

    @Test
    void should_stop_traversal_when_ancestors_form_a_cycle() {
        var firstParentId = PortalNavigationItemId.random();
        var secondParentId = PortalNavigationItemId.random();
        var firstParent = folder(firstParentId, secondParentId);
        var secondParent = folder(secondParentId, firstParentId);
        var apiProduct = apiProduct("api-product-item", firstParentId, PortalVisibility.PUBLIC);
        var items = List.<PortalNavigationItem>of(firstParent, secondParent, apiProduct);

        var result = filter(List.of(apiProduct), items, Set.of(), Set.of());

        assertThat(result).containsExactly(apiProduct);
    }

    @Test
    void should_apply_type_specific_visibility_to_apis_and_api_products() {
        var api = api("api-item", null, PortalVisibility.PRIVATE);
        var apiProduct = apiProduct("api-product-item", null, PortalVisibility.PRIVATE);
        var items = List.<PortalNavigationItem>of(api, apiProduct);

        var hidden = filter(List.of(api, apiProduct), items, Set.of(), Set.of());
        var visible = filter(List.of(api, apiProduct), items, Set.of(api.getId()), Set.of(apiProduct.getApiProductId()));

        assertThat(hidden).isEmpty();
        assertThat(visible).containsExactly(api, apiProduct);
    }

    @Test
    void should_keep_only_apis_without_an_api_product_ancestor() {
        var apiProduct = apiProduct("api-product-item", null, PortalVisibility.PUBLIC);
        var folder = folder("folder-item", apiProduct.getId());
        var directApi = api("direct-api-item", apiProduct.getId(), PortalVisibility.PUBLIC);
        var nestedApi = api("nested-api-item", folder.getId(), PortalVisibility.PUBLIC);
        var standaloneApi = api("standalone-api-item", null, PortalVisibility.PUBLIC);
        var items = List.<PortalNavigationItem>of(apiProduct, folder, directApi, nestedApi, standaloneApi);

        var result = service.filterStandaloneApis(List.of(directApi, nestedApi, standaloneApi), index(items));

        assertThat(result).containsExactly(standaloneApi);
    }

    @Test
    void should_keep_standalone_apis_when_a_parent_is_missing_or_ancestors_form_a_cycle() {
        var missingParentApi = api("missing-parent-api-item", PortalNavigationItemId.random(), PortalVisibility.PUBLIC);
        var firstParentId = PortalNavigationItemId.random();
        var secondParentId = PortalNavigationItemId.random();
        var firstParent = folder(firstParentId, secondParentId);
        var secondParent = folder(secondParentId, firstParentId);
        var cycleApi = api("cycle-api-item", firstParentId, PortalVisibility.PUBLIC);
        var items = List.<PortalNavigationItem>of(missingParentApi, firstParent, secondParent, cycleApi);

        var result = service.filterStandaloneApis(List.of(missingParentApi, cycleApi), index(items));

        assertThat(result).containsExactly(missingParentApi, cycleApi);
    }

    private <T extends PortalNavigationItem> List<T> filter(
        List<T> candidates,
        List<PortalNavigationItem> allItems,
        Set<PortalNavigationItemId> accessibleApiNavigationItemIds,
        Set<String> accessibleApiProductIds
    ) {
        return service.filterVisibleItems(
            candidates,
            index(allItems),
            VIEWER_CONTEXT,
            accessibleApiNavigationItemIds,
            accessibleApiProductIds
        );
    }

    private Map<PortalNavigationItemId, PortalNavigationItem> index(List<PortalNavigationItem> items) {
        return items.stream().collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity()));
    }

    private PortalNavigationFolder folder(String id, PortalNavigationItemId parentId) {
        return folder(navigationItemId(id), parentId);
    }

    private PortalNavigationFolder folder(PortalNavigationItemId id, PortalNavigationItemId parentId) {
        return PortalNavigationFolder.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(id.json())
            .segment(id.json())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private PortalNavigationApi api(String id, PortalNavigationItemId parentId, PortalVisibility visibility) {
        return PortalNavigationApi.builder()
            .id(navigationItemId(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(id)
            .segment(id)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .apiId(id)
            .published(true)
            .visibility(visibility)
            .build();
    }

    private PortalNavigationApiProduct apiProduct(String id, PortalNavigationItemId parentId, PortalVisibility visibility) {
        return PortalNavigationApiProduct.builder()
            .id(navigationItemId(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(id)
            .segment(id)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .apiProductId(id)
            .published(true)
            .visibility(visibility)
            .build();
    }

    private PortalNavigationItemId navigationItemId(String value) {
        return PortalNavigationItemId.of(UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
