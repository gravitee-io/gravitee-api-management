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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal_page.domain_service.CheckTypoToleranceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalCatalogNavigationVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiProductVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalCatalogApiProductSummary;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSearchInclude;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetVisiblePortalCatalogItemsUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";

    private GetVisiblePortalCatalogItemsUseCase useCase;
    private CountingPortalNavigationItemsQueryService navigationItemsQueryService;
    private ApiPortalSearchQueryServiceInMemory apiPortalSearchQueryService;
    private ApiQueryServiceInMemory apiQueryService;
    private ApiProductQueryServiceInMemory apiProductQueryService;

    @BeforeEach
    void set_up() {
        navigationItemsQueryService = new CountingPortalNavigationItemsQueryService();
        apiPortalSearchQueryService = new ApiPortalSearchQueryServiceInMemory();
        apiQueryService = new ApiQueryServiceInMemory();
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        var membershipQueryService = new MembershipQueryServiceInMemory();
        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            new SubscriptionQueryServiceInMemory(),
            apiQueryService
        );
        var apiVisibilityDomainService = new PortalNavigationApiVisibilityDomainService(
            navigationItemsQueryService,
            apiMembershipDomainService
        );
        var apiProductVisibilityDomainService = new PortalNavigationApiProductVisibilityDomainService(
            navigationItemsQueryService,
            new ApiProductAccessibleIdsDomainService(apiProductQueryService, membershipQueryService)
        );
        var catalogNavigationVisibilityDomainService = new PortalCatalogNavigationVisibilityDomainService(
            apiProductVisibilityDomainService
        );
        useCase = new GetVisiblePortalCatalogItemsUseCase(
            navigationItemsQueryService,
            apiVisibilityDomainService,
            apiProductVisibilityDomainService,
            catalogNavigationVisibilityDomainService,
            apiPortalSearchQueryService,
            apiQueryService,
            apiProductQueryService,
            new CheckTypoToleranceDomainService(new ParametersQueryServiceInMemory())
        );
    }

    @Test
    void should_return_api_and_api_product_in_one_deterministic_page() {
        var folder = folder("folder-id", "Catalog", null);
        var apiItem = apiItem("api-item-id", "api-id", folder.getId(), PortalVisibility.PUBLIC);
        var apiProductItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(folder, apiItem, apiProductItem));
        initApis(List.of(api("api-id", "Zebra API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Apple Product", Set.of())));

        var output = useCase.execute(input(Optional.empty(), Set.of(), 1, 10));

        assertThat(output.items().getTotalElements()).isEqualTo(2);
        assertThat(output.items().getContent())
            .extracting(PortalNavigationItem::getType)
            .containsExactly(PortalNavigationItemType.API_PRODUCT, PortalNavigationItemType.API);
        assertThat(output.includedApis()).isEmpty();
        assertThat(output.includedApiProducts()).isEmpty();
    }

    @Test
    void should_exclude_direct_and_nested_product_apis_while_preserving_the_product_summary() {
        var productItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        var folder = folder("folder-id", "Product APIs", productItem.getId());
        var directApiItem = apiItem("direct-api-item-id", "direct-api-id", productItem.getId(), PortalVisibility.PUBLIC);
        var nestedApiItem = apiItem("nested-api-item-id", "nested-api-id", folder.getId(), PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(productItem, folder, directApiItem, nestedApiItem));
        initApis(List.of(api("direct-api-id", "Direct API", "1.0.0"), api("nested-api-id", "Nested API", "2.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "API Product", Set.of("direct-api-id", "nested-api-id"))));

        var output = useCase.execute(input(Optional.empty(), Set.of(PortalNavigationSearchInclude.API_PRODUCT), 1, 10));

        assertThat(output.items().getTotalElements()).isEqualTo(1);
        assertThat(output.items().getContent()).containsExactly(productItem);
        assertThat(output.includedApiProducts())
            .singleElement()
            .satisfies(summary ->
                assertThat(summary.apis())
                    .extracting(PortalCatalogApiProductSummary.ApiSummary::id)
                    .containsExactly("direct-api-id", "nested-api-id")
            );
    }

    @Test
    void should_return_a_standalone_api_once_when_the_same_api_is_also_product_scoped() {
        var productItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        var productScopedApiItem = apiItem("product-api-item-id", "api-id", productItem.getId(), PortalVisibility.PUBLIC);
        var standaloneApiItem = apiItem("standalone-api-item-id", "api-id", null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(productItem, productScopedApiItem, standaloneApiItem));
        initApis(List.of(api("api-id", "Zebra API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Apple Product", Set.of("api-id"))));

        var output = useCase.execute(input(Optional.empty(), Set.of(PortalNavigationSearchInclude.API), 1, 10));

        assertThat(output.items().getTotalElements()).isEqualTo(2);
        assertThat(output.items().getContent()).containsExactly(productItem, standaloneApiItem).doesNotContain(productScopedApiItem);
        assertThat(output.includedApis()).singleElement().extracting(Api::getId).isEqualTo("api-id");
    }

    @Test
    void should_not_match_a_product_scoped_api_as_an_independent_catalog_result() {
        var productItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        var productScopedApiItem = apiItem("product-api-item-id", "api-id", productItem.getId(), PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(productItem, productScopedApiItem));
        initApis(List.of(api("api-id", "Payments API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Commerce Product", Set.of("api-id"))));

        var output = useCase.execute(input(Optional.of("payments"), Set.of(), 1, 10));

        assertThat(output.items().getContent()).isEmpty();
        assertThat(output.items().getTotalElements()).isZero();
    }

    @Test
    void should_paginate_after_excluding_product_scoped_apis() {
        var productItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        var productScopedApiItem = apiItem("product-api-item-id", "product-api-id", productItem.getId(), PortalVisibility.PUBLIC);
        var standaloneApiItem = apiItem("standalone-api-item-id", "standalone-api-id", null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(productItem, productScopedApiItem, standaloneApiItem));
        initApis(List.of(api("product-api-id", "Middle API", "1.0.0"), api("standalone-api-id", "Zebra API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Apple Product", Set.of("product-api-id"))));

        var output = useCase.execute(input(Optional.empty(), Set.of(), 2, 1));

        assertThat(output.items().getTotalElements()).isEqualTo(2);
        assertThat(output.items().getContent()).containsExactly(standaloneApiItem);
    }

    @Test
    void should_resolve_ancestors_with_a_bounded_number_of_queries() {
        var apiFolder = folder("api-folder-id", "APIs", null);
        var productFolder = folder("product-folder-id", "Products", null);
        var apiItem = apiItem("api-item-id", "api-id", apiFolder.getId(), PortalVisibility.PUBLIC);
        var apiProductItem = apiProductItem("product-item-id", "product-id", productFolder.getId(), PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(apiFolder, productFolder, apiItem, apiProductItem));
        initApis(List.of(api("api-id", "API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Product", Set.of())));

        var output = useCase.execute(input(Optional.empty(), Set.of(), 1, 10));

        assertThat(output.items().getContent()).hasSize(2);
        assertThat(navigationItemsQueryService.searchCalls).isEqualTo(2);
        assertThat(navigationItemsQueryService.singleItemLookupCalls).isZero();
    }

    @Test
    void should_match_api_and_api_product_names() {
        var folder = folder("folder-id", "Catalog", null);
        var apiItem = apiItem("api-item-id", "api-id", folder.getId(), PortalVisibility.PUBLIC);
        var apiProductItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(folder, apiItem, apiProductItem));
        initApis(List.of(api("api-id", "Payments API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Payments Suite", Set.of())));

        var output = useCase.execute(input(Optional.of("payments"), Set.of(), 1, 10));

        assertThat(output.items().getContent())
            .extracting(PortalNavigationItem::getType)
            .containsExactly(PortalNavigationItemType.API, PortalNavigationItemType.API_PRODUCT);
    }

    @Test
    void should_paginate_the_combined_result_after_sorting() {
        var folder = folder("folder-id", "Catalog", null);
        var apiItem = apiItem("api-item-id", "api-id", folder.getId(), PortalVisibility.PUBLIC);
        var apiProductItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(folder, apiItem, apiProductItem));
        initApis(List.of(api("api-id", "Zebra API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Apple Product", Set.of())));

        var output = useCase.execute(input(Optional.empty(), Set.of(), 1, 1));

        assertThat(output.items().getTotalElements()).isEqualTo(2);
        assertThat(output.items().getContent())
            .singleElement()
            .extracting(PortalNavigationItem::getType)
            .isEqualTo(PortalNavigationItemType.API_PRODUCT);
    }

    @Test
    void should_return_all_sorted_entries_when_pagination_is_disabled() {
        var folder = folder("folder-id", "Catalog", null);
        var apiItem = apiItem("api-item-id", "api-id", folder.getId(), PortalVisibility.PUBLIC);
        var firstProductItem = apiProductItem("first-product-item-id", "first-product-id", null, PortalVisibility.PUBLIC);
        var secondProductItem = apiProductItem("second-product-item-id", "second-product-id", null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(folder, apiItem, firstProductItem, secondProductItem));
        initApis(List.of(api("api-id", "Zebra API", "1.0.0")));
        apiProductQueryService.initWith(
            List.of(apiProduct("first-product-id", "Apple Product", Set.of()), apiProduct("second-product-id", "Middle Product", Set.of()))
        );

        var output = useCase.execute(input(Optional.empty(), Set.of(), 1, -1));

        assertThat(output.items().getContent())
            .extracting(PortalNavigationItem::getId)
            .containsExactly(firstProductItem.getId(), secondProductItem.getId(), apiItem.getId());
        assertThat(output.items().getTotalElements()).isEqualTo(3);
    }

    @Test
    void should_return_an_empty_result_when_pagination_is_disabled() {
        var output = useCase.execute(input(Optional.empty(), Set.of(), 1, -1));

        assertThat(output.items().getContent()).isEmpty();
        assertThat(output.items().getTotalElements()).isZero();
    }

    @Test
    void should_include_only_visible_apis_in_api_product_summary() {
        var productItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PUBLIC);
        var publicApiItem = apiItem("public-api-item-id", "public-api-id", productItem.getId(), PortalVisibility.PUBLIC);
        var privateApiItem = apiItem("private-api-item-id", "private-api-id", productItem.getId(), PortalVisibility.PRIVATE);
        navigationItemsQueryService.initWith(List.of(productItem, publicApiItem, privateApiItem));
        initApis(List.of(api("public-api-id", "Public API", "1.0.0"), api("private-api-id", "Private API", "2.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Product", Set.of("public-api-id", "private-api-id"))));

        var output = useCase.execute(input(Optional.empty(), Set.of(PortalNavigationSearchInclude.API_PRODUCT), 1, 10));

        assertThat(output.includedApiProducts())
            .singleElement()
            .satisfies(summary -> {
                assertThat(summary.id()).isEqualTo("product-id");
                assertThat(summary.navigationItemId()).isEqualTo(productItem.getId().json());
                assertThat(summary.apis())
                    .singleElement()
                    .satisfies(api -> {
                        assertThat(api.id()).isEqualTo("public-api-id");
                        assertThat(api.name()).isEqualTo("Public API");
                    });
            });
    }

    @Test
    void should_not_return_items_below_a_hidden_api_product() {
        var productItem = apiProductItem("product-item-id", "product-id", null, PortalVisibility.PRIVATE);
        var apiItem = apiItem("api-item-id", "api-id", productItem.getId(), PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(productItem, apiItem));
        initApis(List.of(api("api-id", "API", "1.0.0")));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Product", Set.of("api-id"))));

        var output = useCase.execute(input(Optional.empty(), Set.of(), 1, 10));

        assertThat(output.items().getContent()).isEmpty();
        assertThat(output.items().getTotalElements()).isZero();
    }

    @Test
    void should_not_return_api_product_below_an_unpublished_parent() {
        var folder = folder("folder-id", "Hidden", null);
        folder.setPublished(false);
        var apiProductItem = apiProductItem("product-item-id", "product-id", folder.getId(), PortalVisibility.PUBLIC);
        navigationItemsQueryService.initWith(List.of(folder, apiProductItem));
        apiProductQueryService.initWith(List.of(apiProduct("product-id", "Product", Set.of())));

        var output = useCase.execute(input(Optional.empty(), Set.of(), 1, 10));

        assertThat(output.items().getContent()).isEmpty();
    }

    private GetVisiblePortalCatalogItemsUseCase.Input input(
        Optional<String> query,
        Set<PortalNavigationSearchInclude> includes,
        int page,
        int size
    ) {
        return new GetVisiblePortalCatalogItemsUseCase.Input(
            ENV_ID,
            ORG_ID,
            PortalNavigationItemViewerContext.forPortal((String) null),
            new PageableImpl(page, size),
            query,
            includes
        );
    }

    private void initApis(List<Api> apis) {
        apiPortalSearchQueryService.initWith(apis);
        apiQueryService.initWith(apis);
    }

    private Api api(String id, String name, String version) {
        return Api.builder().id(id).environmentId(ENV_ID).name(name).version(version).build();
    }

    private ApiProduct apiProduct(String id, String name, Set<String> apiIds) {
        return ApiProduct.builder()
            .id(id)
            .environmentId(ENV_ID)
            .name(name)
            .description("Description")
            .version("1.0.0")
            .apiIds(apiIds)
            .build();
    }

    private PortalNavigationFolder folder(String id, String title, PortalNavigationItemId parentId) {
        return PortalNavigationFolder.builder()
            .id(navigationItemId(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private PortalNavigationApi apiItem(String id, String apiId, PortalNavigationItemId parentId, PortalVisibility visibility) {
        return PortalNavigationApi.builder()
            .id(navigationItemId(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(apiId)
            .segment(apiId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .apiId(apiId)
            .published(true)
            .visibility(visibility)
            .build();
    }

    private PortalNavigationApiProduct apiProductItem(
        String id,
        String apiProductId,
        PortalNavigationItemId parentId,
        PortalVisibility visibility
    ) {
        return PortalNavigationApiProduct.builder()
            .id(navigationItemId(id))
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(apiProductId)
            .segment(apiProductId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(parentId)
            .apiProductId(apiProductId)
            .published(true)
            .visibility(visibility)
            .build();
    }

    private PortalNavigationItemId navigationItemId(String value) {
        return PortalNavigationItemId.of(UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString());
    }

    private static class CountingPortalNavigationItemsQueryService extends PortalNavigationItemsQueryServiceInMemory {

        private int searchCalls;
        private int singleItemLookupCalls;

        @Override
        public List<PortalNavigationItem> search(PortalNavigationItemQueryCriteria criteria) {
            searchCalls++;
            return super.search(criteria);
        }

        @Override
        public PortalNavigationItem findByIdAndEnvironmentId(String environmentId, PortalNavigationItemId id) {
            singleItemLookupCalls++;
            return super.findByIdAndEnvironmentId(environmentId, id);
        }
    }
}
