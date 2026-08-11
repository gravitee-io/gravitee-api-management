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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalApiProductAccessDomainServiceTest {

    private static final String ENVIRONMENT_ID = PortalNavigationItemFixtures.ENV_ID;
    private static final String API_PRODUCT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String NAVIGATION_ITEM_ID = "00000000-0000-0000-0000-000000000102";
    private static final String USER_ID = "user-id";

    private ApiProductQueryServiceInMemory apiProductQueryService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private PortalApiProductAccessDomainService service;

    @BeforeEach
    void setUp() {
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();

        var apiQueryService = new ApiQueryServiceInMemory();
        var apiVisibilityDomainService = new PortalNavigationApiVisibilityDomainService(
            navigationItemsQueryService,
            new ApiPortalMembershipDomainService(membershipQueryService, new SubscriptionQueryServiceInMemory(), apiQueryService)
        );
        var apiProductVisibilityDomainService = new PortalNavigationApiProductVisibilityDomainService(
            navigationItemsQueryService,
            new ApiProductAccessibleIdsDomainService(apiProductQueryService, membershipQueryService)
        );
        service = new PortalApiProductAccessDomainService(
            apiProductQueryService,
            navigationItemsQueryService,
            apiProductVisibilityDomainService,
            apiVisibilityDomainService
        );
    }

    @Test
    void should_return_public_product_and_navigation_item_for_anonymous_viewer() {
        givenProductAndNavigation(apiProduct(), apiProductNavigation());

        var result = service.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, viewer(null));

        assertThat(result.apiProduct().getId()).isEqualTo(API_PRODUCT_ID);
        assertThat(result.navigationItem().getId()).isEqualTo(PortalNavigationItemId.of(NAVIGATION_ITEM_ID));
    }

    @Test
    void should_return_private_product_for_direct_member() {
        var navigationItem = apiProductNavigation();
        navigationItem.setVisibility(PortalVisibility.PRIVATE);
        givenProductAndNavigation(apiProduct(), navigationItem);
        membershipQueryService.initWith(List.of(apiProductMembership()));

        var result = service.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, viewer(USER_ID));

        assertThat(result.apiProduct().getId()).isEqualTo(API_PRODUCT_ID);
    }

    @Test
    void should_reject_private_product_for_anonymous_viewer() {
        var navigationItem = apiProductNavigation();
        navigationItem.setVisibility(PortalVisibility.PRIVATE);
        givenProductAndNavigation(apiProduct(), navigationItem);

        assertThatThrownBy(() -> service.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, viewer(null))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_reject_product_from_another_environment() {
        var apiProduct = apiProduct();
        apiProduct.setEnvironmentId("other-environment");
        givenProductAndNavigation(apiProduct, apiProductNavigation());

        assertThatThrownBy(() -> service.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, viewer(null))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_reject_product_without_published_navigation_item() {
        var navigationItem = apiProductNavigation();
        navigationItem.setPublished(false);
        givenProductAndNavigation(apiProduct(), navigationItem);

        assertThatThrownBy(() -> service.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, viewer(null))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_reject_product_below_hidden_api_product_ancestor() {
        var parent = PortalNavigationItemFixtures.anApiProduct(
            "00000000-0000-0000-0000-000000000201",
            "Parent product",
            null,
            "00000000-0000-0000-0000-000000000202"
        );
        parent.setVisibility(PortalVisibility.PRIVATE);
        var navigationItem = apiProductNavigation(parent.getId());
        apiProductQueryService.initWith(List.of(apiProduct()));
        navigationItemsQueryService.initWith(List.of(parent, navigationItem));

        assertThatThrownBy(() -> service.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, viewer(USER_ID))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_reject_product_below_hidden_api_ancestor() {
        var parent = PortalNavigationItemFixtures.anApi("00000000-0000-0000-0000-000000000301", "Parent API", null, "api-id");
        parent.setVisibility(PortalVisibility.PRIVATE);
        var navigationItem = apiProductNavigation(parent.getId());
        apiProductQueryService.initWith(List.of(apiProduct()));
        navigationItemsQueryService.initWith(List.of(parent, navigationItem));

        assertThatThrownBy(() -> service.findAccessible(ENVIRONMENT_ID, API_PRODUCT_ID, viewer(USER_ID))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    private void givenProductAndNavigation(ApiProduct apiProduct, PortalNavigationApiProduct navigationItem) {
        apiProductQueryService.initWith(List.of(apiProduct));
        navigationItemsQueryService.initWith(List.of(navigationItem));
    }

    private static ApiProduct apiProduct() {
        return ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).name("Product").build();
    }

    private static PortalNavigationApiProduct apiProductNavigation() {
        return apiProductNavigation(null);
    }

    private static PortalNavigationApiProduct apiProductNavigation(PortalNavigationItemId parentId) {
        return PortalNavigationItemFixtures.anApiProduct(NAVIGATION_ITEM_ID, "Product", parentId, API_PRODUCT_ID);
    }

    private static Membership apiProductMembership() {
        return Membership.builder()
            .id("membership-id")
            .memberId(USER_ID)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.API_PRODUCT)
            .referenceId(API_PRODUCT_ID)
            .build();
    }

    private static PortalNavigationItemViewerContext viewer(String userId) {
        return PortalNavigationItemViewerContext.forPortal(userId);
    }
}
