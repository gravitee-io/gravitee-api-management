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

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationApiProductVisibilityDomainServiceTest {

    private static final String ENVIRONMENT_ID = PortalNavigationItemFixtures.ENV_ID;
    private static final String USER_ID = "user-id";
    private static final String API_PRODUCT_ID = "api-product-id";

    private PortalNavigationApiProductVisibilityDomainService domainService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private ApiProductQueryServiceInMemory apiProductQueryService;
    private MembershipQueryServiceInMemory membershipQueryService;

    @BeforeEach
    void set_up() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory();
        domainService = new PortalNavigationApiProductVisibilityDomainService(
            navigationItemsQueryService,
            new ApiProductAccessibleIdsDomainService(apiProductQueryService, membershipQueryService)
        );
    }

    @Test
    void should_show_public_api_product_to_anonymous_user() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            PortalNavigationItemFixtures.API_PRODUCT_ID,
            "Public product",
            null,
            API_PRODUCT_ID
        );

        assertThat(
            domainService.isApiProductItemHidden(ENVIRONMENT_ID, apiProduct, PortalNavigationItemViewerContext.forPortal(false))
        ).isFalse();
    }

    @Test
    void should_hide_private_api_product_from_anonymous_user() {
        var apiProduct = privateApiProduct();

        assertThat(
            domainService.isApiProductItemHidden(ENVIRONMENT_ID, apiProduct, PortalNavigationItemViewerContext.forPortal(false))
        ).isTrue();
    }

    @Test
    void should_show_private_api_product_to_direct_member() {
        var apiProduct = privateApiProduct();
        membershipQueryService.initWith(List.of(apiProductMembership()));

        assertThat(
            domainService.isApiProductItemHidden(ENVIRONMENT_ID, apiProduct, PortalNavigationItemViewerContext.forPortal(USER_ID))
        ).isFalse();
    }

    @Test
    void should_show_private_api_product_to_group_member() {
        var groupId = "group-id";
        var apiProduct = privateApiProduct();
        apiProductQueryService.initWith(
            List.of(
                ApiProduct.builder()
                    .id(API_PRODUCT_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .name("Private product")
                    .groups(Set.of(groupId))
                    .build()
            )
        );
        membershipQueryService.initWith(
            List.of(
                Membership.builder()
                    .id("membership-id")
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.GROUP)
                    .referenceId(groupId)
                    .build()
            )
        );

        assertThat(
            domainService.isApiProductItemHidden(ENVIRONMENT_ID, apiProduct, PortalNavigationItemViewerContext.forPortal(USER_ID))
        ).isFalse();
    }

    @Test
    void should_hide_private_api_product_from_authenticated_non_member() {
        var apiProduct = privateApiProduct();

        assertThat(
            domainService.isApiProductItemHidden(ENVIRONMENT_ID, apiProduct, PortalNavigationItemViewerContext.forPortal(USER_ID))
        ).isTrue();
    }

    @Test
    void should_not_apply_portal_access_filter_in_console() {
        assertThat(
            domainService.isApiProductItemHidden(ENVIRONMENT_ID, privateApiProduct(), PortalNavigationItemViewerContext.forConsole())
        ).isFalse();
    }

    @Test
    void should_detect_hidden_api_product_ancestor() {
        var apiProduct = privateApiProduct();
        var childFolder = PortalNavigationItemFixtures.aFolder("Product docs", apiProduct.getId());
        childFolder.updateParent(apiProduct);
        navigationItemsQueryService.initWith(List.of(apiProduct, childFolder));

        assertThat(
            domainService.hasHiddenApiProductAncestor(ENVIRONMENT_ID, childFolder, PortalNavigationItemViewerContext.forPortal(USER_ID))
        ).isTrue();
    }

    @Test
    void should_stop_ancestor_lookup_when_hierarchy_contains_cycle() {
        var firstId = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000093");
        var secondId = PortalNavigationItemId.of("00000000-0000-0000-0000-000000000094");
        var first = PortalNavigationItemFixtures.aFolder(firstId.json(), "First", secondId);
        var second = PortalNavigationItemFixtures.aFolder(secondId.json(), "Second", firstId);
        navigationItemsQueryService.initWith(List.of(first, second));

        assertThat(
            domainService.hasHiddenApiProductAncestor(ENVIRONMENT_ID, first, PortalNavigationItemViewerContext.forPortal(USER_ID))
        ).isFalse();
    }

    private static io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct privateApiProduct() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            PortalNavigationItemFixtures.API_PRODUCT_ID,
            "Private product",
            null,
            API_PRODUCT_ID
        );
        apiProduct.setVisibility(PortalVisibility.PRIVATE);
        return apiProduct;
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
}
