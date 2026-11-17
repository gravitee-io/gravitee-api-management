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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.exception.ApiProductNavigationItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PortalListingUniquenessTest {

    private static final String ENV_ID = "env-id";
    private static final String API_ID = "api-id";
    private static final String PRODUCT_ID = "product-id";
    private static final PortalNavigationItemId PARENT_ID = PortalNavigationItemId.random();
    private static final NavigationItemReference PORTAL_A = PortalNavigationItemFixtures.PORTAL_REFERENCE;
    private static final NavigationItemReference PORTAL_B = new NavigationItemReference.PortalReference(
        PortalId.of("22222222-2222-2222-2222-222222222222")
    );

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_limit_api_create_uniqueness_to_its_portal(boolean samePortal) {
        var existing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "API", PARENT_ID, API_ID);
        existing.changeOwner(samePortal ? PORTAL_A : PORTAL_B);
        var ctx = createContext(existing);
        var rule = new ApiItemCreateRule(new ApiProductQueryServiceInMemory());

        if (samePortal) {
            assertThatThrownBy(() -> rule.validate(createApi(), ENV_ID, ctx)).isInstanceOf(InvalidPortalNavigationItemDataException.class);
        } else {
            assertThatCode(() -> rule.validate(createApi(), ENV_ID, ctx)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_limit_api_update_uniqueness_to_its_portal(boolean samePortal) {
        var existing = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "API", PARENT_ID, API_ID);
        var collision = PortalNavigationItemFixtures.anApi(PortalNavigationItemId.random().json(), "Other", PARENT_ID, API_ID);
        collision.changeOwner(samePortal ? PORTAL_A : PORTAL_B);
        var ctx = new UpdateValidationContext(List.of(existing, collision), Map.of(), Map.of(), Map.of(), List.of());
        var update = UpdatePortalNavigationItem.builder().parentId(PARENT_ID).build();
        var rule = new ApiItemUpdateRule(new ApiProductQueryServiceInMemory());

        if (samePortal) {
            assertThatThrownBy(() -> rule.validate(update, existing, ctx)).isInstanceOf(InvalidPortalNavigationItemDataException.class);
        } else {
            assertThatCode(() -> rule.validate(update, existing, ctx)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_limit_product_create_uniqueness_to_its_portal(boolean samePortal) {
        var products = new ApiProductQueryServiceInMemory();
        products.initWith(List.of(ApiProduct.builder().id(PRODUCT_ID).environmentId(ENV_ID).apiIds(Set.of()).build()));
        var existing = PortalNavigationItemFixtures.anApiProduct(PortalNavigationItemId.random().json(), "Product", PARENT_ID, PRODUCT_ID);
        existing.changeOwner(samePortal ? PORTAL_A : PORTAL_B);
        var item = createApi().toBuilder().type(PortalNavigationItemType.API_PRODUCT).apiProductId(PRODUCT_ID).build();
        var rule = new ApiProductItemCreateRule(products);

        if (samePortal) {
            assertThatThrownBy(() -> rule.validate(item, ENV_ID, createContext(existing))).isInstanceOf(
                ApiProductNavigationItemAlreadyExistsException.class
            );
        } else {
            assertThatCode(() -> rule.validate(item, ENV_ID, createContext(existing))).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_limit_bulk_api_uniqueness_to_its_portal(boolean samePortal) {
        var first = createApi();
        var second = createApi().toBuilder().reference(samePortal ? PORTAL_A : PORTAL_B).build();
        var rule = new DuplicateApiIdsInPayloadRule();

        if (samePortal) {
            assertThatThrownBy(() -> rule.validate(List.of(first, second), ENV_ID, CreateValidationContext.empty())).isInstanceOf(
                InvalidPortalNavigationItemDataException.class
            );
        } else {
            assertThatCode(() -> rule.validate(List.of(first, second), ENV_ID, CreateValidationContext.empty())).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_limit_bulk_product_uniqueness_to_its_portal(boolean samePortal) {
        var first = createApi().toBuilder().type(PortalNavigationItemType.API_PRODUCT).apiProductId(PRODUCT_ID).build();
        var second = first.toBuilder().id(PortalNavigationItemId.random()).reference(samePortal ? PORTAL_A : PORTAL_B).build();
        var rule = new DuplicateApiProductIdsInPayloadRule();

        if (samePortal) {
            assertThatThrownBy(() -> rule.validate(List.of(first, second), ENV_ID, CreateValidationContext.empty())).isInstanceOf(
                ApiProductNavigationItemAlreadyExistsException.class
            );
        } else {
            assertThatCode(() -> rule.validate(List.of(first, second), ENV_ID, CreateValidationContext.empty())).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void should_reject_two_pending_homepages_only_in_the_same_portal(boolean samePortal) {
        var first = createApi().toBuilder().type(PortalNavigationItemType.PAGE).area(PortalArea.HOMEPAGE).parentId(null).build();
        var second = first.toBuilder().id(PortalNavigationItemId.random()).reference(samePortal ? PORTAL_A : PORTAL_B).build();
        var ctx = new CreateValidationContext(
            List.of(),
            Map.of(),
            Map.of(first.getId(), first, second.getId(), second),
            Map.of(),
            List.of()
        );
        var rule = new HomepageUniquenessRule(new PortalNavigationItemsQueryServiceInMemory());

        if (samePortal) {
            assertThatThrownBy(() -> rule.validate(first, ENV_ID, ctx)).isInstanceOf(HomepageAlreadyExistsException.class);
        } else {
            assertThatCode(() -> rule.validate(first, ENV_ID, ctx)).doesNotThrowAnyException();
        }
    }

    private static CreateValidationContext createContext(PortalNavigationItem existing) {
        return new CreateValidationContext(List.of(existing), Map.of(existing.getId(), existing), Map.of(), Map.of(), List.of());
    }

    private static CreatePortalNavigationItem createApi() {
        return CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .reference(PORTAL_A)
            .area(PortalArea.TOP_NAVBAR)
            .type(PortalNavigationItemType.API)
            .apiId(API_ID)
            .parentId(PARENT_ID)
            .build();
    }
}
