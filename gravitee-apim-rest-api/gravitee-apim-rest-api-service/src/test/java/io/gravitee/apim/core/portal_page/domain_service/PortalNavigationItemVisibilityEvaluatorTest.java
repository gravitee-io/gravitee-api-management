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
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
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
