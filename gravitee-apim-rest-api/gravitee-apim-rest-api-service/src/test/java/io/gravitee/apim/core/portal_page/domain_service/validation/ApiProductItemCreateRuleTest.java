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
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal_page.exception.ApiProductNavigationItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductItemCreateRuleTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_PRODUCT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String PARENT_ID = "00000000-0000-0000-0000-000000000102";

    private ApiProductQueryServiceInMemory apiProductQueryService;
    private ApiProductItemCreateRule rule;

    @BeforeEach
    void setUp() {
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).apiIds(Set.of()).build())
        );
        rule = new ApiProductItemCreateRule(apiProductQueryService);
    }

    @Test
    void should_accept_product_below_non_product_container() {
        assertThatCode(() -> rule.validate(apiProductItem(), ENVIRONMENT_ID, CreateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void should_reject_product_without_parent() {
        var item = apiProductItem().toBuilder().parentId(null).build();

        assertThatThrownBy(() -> rule.validate(item, ENVIRONMENT_ID, CreateValidationContext.empty()))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessage("The parentId field is required and cannot be blank.");
    }

    @Test
    void should_reject_product_outside_top_navbar() {
        var item = apiProductItem().toBuilder().area(PortalArea.HOMEPAGE).build();

        assertThatThrownBy(() -> rule.validate(item, ENVIRONMENT_ID, CreateValidationContext.empty())).isInstanceOf(
            InvalidPortalNavigationItemDataException.class
        );
    }

    @Test
    void should_reject_product_from_another_environment() {
        assertThatThrownBy(() -> rule.validate(apiProductItem(), "other-environment", CreateValidationContext.empty())).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_reject_nested_product_in_pending_payload() {
        var parent = apiProductItem().toBuilder().id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000103")).build();
        var nested = apiProductItem().toBuilder().parentId(parent.getId()).build();
        var ctx = new CreateValidationContext(List.of(), Map.of(), Map.of(parent.getId(), parent));

        assertThatThrownBy(() -> rule.validate(nested, ENVIRONMENT_ID, ctx)).isInstanceOf(InvalidPortalNavigationItemDataException.class);
    }

    @Test
    void should_reject_existing_product_reference() {
        var existing = PortalNavigationItemFixtures.anApiProduct("00000000-0000-0000-0000-000000000104", "Product", null, API_PRODUCT_ID);
        var ctx = new CreateValidationContext(List.of(existing), Map.of(existing.getId(), existing), Map.of());

        assertThatThrownBy(() -> rule.validate(apiProductItem(), ENVIRONMENT_ID, ctx)).isInstanceOf(
            ApiProductNavigationItemAlreadyExistsException.class
        );
    }

    @Test
    void should_reject_existing_product_reference_before_validating_ancestors() {
        var existing = PortalNavigationItemFixtures.anApiProduct("00000000-0000-0000-0000-000000000104", "Product", null, API_PRODUCT_ID);
        var parent = apiProductItem().toBuilder().id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000105")).build();
        var ctx = new CreateValidationContext(List.of(existing), Map.of(existing.getId(), existing), Map.of(parent.getId(), parent));

        assertThatThrownBy(() ->
            rule.validate(apiProductItem().toBuilder().parentId(parent.getId()).build(), ENVIRONMENT_ID, ctx)
        ).isInstanceOf(ApiProductNavigationItemAlreadyExistsException.class);
    }

    @Test
    void should_reject_duplicate_product_references_in_bulk_payload() {
        var first = apiProductItem();
        var second = apiProductItem().toBuilder().id(PortalNavigationItemId.random()).build();

        assertThatThrownBy(() ->
            new DuplicateApiProductIdsInPayloadRule().validate(List.of(first, second), ENVIRONMENT_ID, CreateValidationContext.empty())
        ).isInstanceOf(ApiProductNavigationItemAlreadyExistsException.class);
    }

    private static CreatePortalNavigationItem apiProductItem() {
        return CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .title("Product")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .type(PortalNavigationItemType.API_PRODUCT)
            .parentId(PortalNavigationItemId.of(PARENT_ID))
            .apiProductId(API_PRODUCT_ID)
            .build();
    }
}
