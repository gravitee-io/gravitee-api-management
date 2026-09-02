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
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemVisibilityEvaluatorTest {

    private PortalNavigationItemsQueryServiceInMemory queryService;

    @BeforeEach
    void set_up() {
        queryService = new PortalNavigationItemsQueryServiceInMemory();
    }

    @Test
    void should_use_matching_visibility_service() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            PortalNavigationItemFixtures.API_PRODUCT_ID,
            "API Product",
            null,
            "api-product-id"
        );
        var evaluator = evaluatorWith(new HiddenApiProductVisibilityService());

        assertThat(evaluator.isVisible(apiProduct)).isFalse();
    }

    @Test
    void should_default_to_visible_when_no_service_applies() {
        var page = PortalNavigationItemFixtures.aPage("Page", null);
        var evaluator = evaluatorWith(new HiddenApiProductVisibilityService());

        assertThat(evaluator.isVisible(page)).isTrue();
    }

    @Test
    void should_detect_hidden_ancestor_using_matching_visibility_service() {
        var apiProduct = PortalNavigationItemFixtures.anApiProduct(
            PortalNavigationItemFixtures.API_PRODUCT_ID,
            "API Product",
            null,
            "api-product-id"
        );
        var page = PortalNavigationItemFixtures.aPage("Page", apiProduct.getId());
        page.updateParent(apiProduct);
        queryService.initWith(List.of(apiProduct, page));
        var evaluator = evaluatorWith(new HiddenApiProductVisibilityService());

        assertThat(evaluator.hasHiddenAncestor(page)).isTrue();
    }

    @Test
    void should_detect_hidden_ancestor_when_the_enclosing_api_of_a_spliced_subtree_root_is_hidden() {
        var privateApiRow = PortalNavigationItemFixtures.anApi("00000000-0000-0000-0000-00000000b001", "Private API", null, "api-x");
        privateApiRow.markAsRoot();
        privateApiRow.setVisibility(PortalVisibility.PRIVATE);
        queryService.initWith(List.of(privateApiRow));

        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            new MembershipQueryServiceInMemory(),
            new SubscriptionQueryServiceInMemory(),
            new ApiQueryServiceInMemory()
        );
        var apiVisibilityDomainService = new PortalNavigationApiVisibilityDomainService(queryService, apiMembershipDomainService);

        var splicedRoot = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(PortalNavigationItemFixtures.ORG_ID)
            .environmentId(PortalNavigationItemFixtures.ENV_ID)
            .title("Doc")
            .segment("doc")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(PortalPageContentId.random())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .reference(new NavigationItemReference.ApiReference("api-x"))
            .build();
        splicedRoot.markAsRoot();

        var evaluator = new PortalNavigationItemVisibilityEvaluator(
            PortalNavigationItemFixtures.ENV_ID,
            PortalNavigationItemViewerContext.forPortal(false),
            queryService,
            List.of(apiVisibilityDomainService)
        );

        assertThat(evaluator.hasHiddenAncestor(splicedRoot)).isTrue();
    }

    private PortalNavigationItemVisibilityEvaluator evaluatorWith(PortalNavigationItemVisibilityService visibilityService) {
        return new PortalNavigationItemVisibilityEvaluator(
            PortalNavigationItemFixtures.ENV_ID,
            PortalNavigationItemViewerContext.forPortal("user-id"),
            queryService,
            List.of(visibilityService)
        );
    }

    private static class HiddenApiProductVisibilityService implements PortalNavigationItemVisibilityService {

        @Override
        public boolean appliesTo(PortalNavigationItem item) {
            return item instanceof PortalNavigationApiProduct;
        }

        @Override
        public Predicate<PortalNavigationItem> prepareVisibilityPredicate(
            String environmentId,
            PortalNavigationItemViewerContext viewerContext
        ) {
            return item -> false;
        }
    }
}
