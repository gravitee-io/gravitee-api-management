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
package io.gravitee.apim.core.portal_page.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemTest {

    @Test
    void should_create_unpublished_api_product_container() {
        var create = CreatePortalNavigationItem.builder()
            .title("Product documentation")
            .segment("product-documentation")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .type(PortalNavigationItemType.API_PRODUCT)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .apiProductId("00000000-0000-0000-0000-000000000020")
            .build();

        var item = PortalNavigationItem.from(create, "organization-id", "environment-id", null);

        assertThat(item).isInstanceOf(PortalNavigationApiProduct.class).isInstanceOf(PortalNavigationItemContainer.class);
        var apiProduct = (PortalNavigationApiProduct) item;
        assertThat(apiProduct.getApiProductId()).isEqualTo("00000000-0000-0000-0000-000000000020");
        assertThat(apiProduct.getPublished()).isFalse();
        assertThat(apiProduct.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(apiProduct.isRoot()).isTrue();
    }

    @Test
    void should_preserve_api_product_id_during_update() {
        var apiProduct = fixtures.core.model.PortalNavigationItemFixtures.anApiProduct();
        var originalApiProductId = apiProduct.getApiProductId();
        var update = UpdatePortalNavigationItem.builder()
            .title("Updated product documentation")
            .order(1)
            .type(PortalNavigationItemType.API_PRODUCT)
            .published(false)
            .visibility(PortalVisibility.PRIVATE)
            .build();

        apiProduct.update(update);

        assertThat(apiProduct.getApiProductId()).isEqualTo(originalApiProductId);
        assertThat(apiProduct.getTitle()).isEqualTo("Updated product documentation");
    }
}
