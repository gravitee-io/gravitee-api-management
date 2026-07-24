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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiProductVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalApiProductUseCaseTest {

    private static final String ENVIRONMENT_ID = PortalNavigationItemFixtures.ENV_ID;
    private static final String OTHER_ENVIRONMENT_ID = "other-environment-id";
    private static final String API_PRODUCT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String NAVIGATION_ITEM_ID = "00000000-0000-0000-0000-000000000102";
    private static final String USER_ID = "user-id";
    private static final String GROUP_ID = "group-id";

    private ApiProductQueryServiceInMemory apiProductQueryService;
    private ApiQueryServiceInMemory apiQueryService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private GetPortalApiProductUseCase useCase;

    @BeforeEach
    void setUp() {
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        apiQueryService = new ApiQueryServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();

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
        useCase = new GetPortalApiProductUseCase(
            apiProductQueryService,
            navigationItemsQueryService,
            apiProductVisibilityDomainService,
            apiVisibilityDomainService,
            apiQueryService
        );
    }

    @Test
    void should_return_public_api_product_details_with_sorted_tags_and_apis() {
        var secondApi = api("api-b", "Zebra API", "2.0");
        var firstApi = api("api-a", "Alpha API", "1.0");
        var product = apiProduct(Set.of(firstApi.getId(), secondApi.getId()));
        var navigationItem = publicApiProductNavigationItem();
        givenProductAndNavigation(product, navigationItem, publicApiNavigationItem(firstApi), publicApiNavigationItem(secondApi));
        apiQueryService.initWith(List.of(secondApi, firstApi));

        var result = useCase.execute(input(null)).apiProduct();

        assertThat(result.id()).isEqualTo(API_PRODUCT_ID);
        assertThat(result.name()).isEqualTo("AI Workspace");
        assertThat(result.description()).isEqualTo("Consumer description");
        assertThat(result.version()).isEqualTo("1.0.0");
        assertThat(result.kind()).isEqualTo(ApiProductKind.AI_WORKSPACE);
        assertThat(result.navigationItemId()).isEqualTo(NAVIGATION_ITEM_ID);
        assertThat(result.tags()).containsExactly("ai", "public");
        assertThat(result.apis())
            .extracting(apiSummary -> apiSummary.id() + ":" + apiSummary.name() + ":" + apiSummary.version())
            .containsExactly("api-a:Alpha API:1.0", "api-b:Zebra API:2.0");
    }

    @Test
    void should_return_private_api_product_to_direct_member() {
        var product = apiProduct(Set.of());
        var navigationItem = publicApiProductNavigationItem();
        navigationItem.setVisibility(PortalVisibility.PRIVATE);
        givenProductAndNavigation(product, navigationItem);
        membershipQueryService.initWith(List.of(apiProductMembership(USER_ID)));

        var result = useCase.execute(input(USER_ID));

        assertThat(result.apiProduct().id()).isEqualTo(API_PRODUCT_ID);
    }

    @Test
    void should_return_private_api_product_to_group_member() {
        var product = apiProduct(Set.of());
        product.setGroups(new HashSet<>(Set.of(GROUP_ID)));
        var navigationItem = publicApiProductNavigationItem();
        navigationItem.setVisibility(PortalVisibility.PRIVATE);
        givenProductAndNavigation(product, navigationItem);
        membershipQueryService.initWith(List.of(groupMembership(USER_ID, GROUP_ID)));

        var result = useCase.execute(input(USER_ID));

        assertThat(result.apiProduct().id()).isEqualTo(API_PRODUCT_ID);
    }

    @Test
    void should_return_not_found_for_private_api_product_without_access() {
        var navigationItem = publicApiProductNavigationItem();
        navigationItem.setVisibility(PortalVisibility.PRIVATE);
        givenProductAndNavigation(apiProduct(Set.of()), navigationItem);

        assertThatThrownBy(() -> useCase.execute(input(USER_ID))).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_api_product_does_not_exist() {
        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_api_product_belongs_to_another_environment() {
        var product = apiProduct(Set.of());
        product.setEnvironmentId(OTHER_ENVIRONMENT_ID);
        givenProductAndNavigation(product, publicApiProductNavigationItem());

        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_api_product_navigation_item_is_unpublished() {
        var navigationItem = publicApiProductNavigationItem();
        navigationItem.setPublished(false);
        givenProductAndNavigation(apiProduct(Set.of()), navigationItem);

        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_return_empty_api_list_when_api_product_has_no_apis() {
        givenProductAndNavigation(apiProduct(Set.of()), publicApiProductNavigationItem());

        var result = useCase.execute(input(null));

        assertThat(result.apiProduct().apis()).isEmpty();
    }

    @Test
    void should_only_return_included_apis_accessible_to_the_current_user() {
        var publicApi = api("public-api", "Public API", "1.0");
        var privateApi = api("private-api", "Private API", "1.0");
        var publicApiNavigation = publicApiNavigationItem(publicApi);
        var privateApiNavigation = publicApiNavigationItem(privateApi);
        privateApiNavigation.setVisibility(PortalVisibility.PRIVATE);
        givenProductAndNavigation(
            apiProduct(Set.of(publicApi.getId(), privateApi.getId())),
            publicApiProductNavigationItem(),
            publicApiNavigation,
            privateApiNavigation
        );
        apiQueryService.initWith(List.of(publicApi, privateApi));

        var result = useCase.execute(input(USER_ID));

        assertThat(result.apiProduct().apis())
            .extracting(api -> api.id())
            .containsExactly("public-api");
    }

    private void givenProductAndNavigation(
        ApiProduct apiProduct,
        PortalNavigationApiProduct apiProductNavigationItem,
        PortalNavigationItem... otherItems
    ) {
        apiProductQueryService.initWith(List.of(apiProduct));
        var navigationItems = new ArrayList<PortalNavigationItem>();
        navigationItems.add(apiProductNavigationItem);
        navigationItems.addAll(List.of(otherItems));
        navigationItemsQueryService.initWith(navigationItems);
    }

    private static ApiProduct apiProduct(Set<String> apiIds) {
        return ApiProduct.builder()
            .id(API_PRODUCT_ID)
            .environmentId(ENVIRONMENT_ID)
            .name("AI Workspace")
            .description("Consumer description")
            .version("1.0.0")
            .kind(ApiProductKind.AI_WORKSPACE)
            .tags(Set.of("public", "ai"))
            .apiIds(new HashSet<>(apiIds))
            .build();
    }

    private static PortalNavigationApiProduct publicApiProductNavigationItem() {
        return PortalNavigationItemFixtures.anApiProduct(NAVIGATION_ITEM_ID, "AI Workspace", null, API_PRODUCT_ID);
    }

    private static Api api(String id, String name, String version) {
        return Api.builder().id(id).environmentId(ENVIRONMENT_ID).name(name).version(version).build();
    }

    private static PortalNavigationApi publicApiNavigationItem(Api api) {
        return PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().toString(),
            api.getName(),
            PortalNavigationItemId.of(NAVIGATION_ITEM_ID),
            api.getId()
        );
    }

    private static Membership apiProductMembership(String userId) {
        return Membership.builder()
            .id("api-product-membership")
            .memberId(userId)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.API_PRODUCT)
            .referenceId(API_PRODUCT_ID)
            .build();
    }

    private static Membership groupMembership(String userId, String groupId) {
        return Membership.builder()
            .id("group-membership")
            .memberId(userId)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.GROUP)
            .referenceId(groupId)
            .build();
    }

    private static GetPortalApiProductUseCase.Input input(String userId) {
        return new GetPortalApiProductUseCase.Input(ENVIRONMENT_ID, API_PRODUCT_ID, PortalNavigationItemViewerContext.forPortal(userId));
    }
}
