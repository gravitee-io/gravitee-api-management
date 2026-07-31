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

import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemCreationExpansionDomainServiceTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_PRODUCT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String PARENT_ID = "00000000-0000-0000-0000-000000000102";

    private ApiProductQueryServiceInMemory apiProductQueryService;
    private ApiCrudServiceInMemory apiCrudService;
    private PortalNavigationItemCreationExpansionDomainService service;

    @BeforeEach
    void setUp() {
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        apiCrudService = new ApiCrudServiceInMemory();
        service = new PortalNavigationItemCreationExpansionDomainService(apiProductQueryService, apiCrudService);
    }

    @Test
    void should_create_only_product_root_when_product_has_no_apis() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).apiIds(Set.of()).build())
        );

        var expansion = service.expand(List.of(apiProductItem()), ENVIRONMENT_ID);

        assertThat(expansion.itemsToCreate())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getId()).isNotNull();
                assertThat(item.getType()).isEqualTo(PortalNavigationItemType.API_PRODUCT);
            });
        assertThat(expansion.requestedItemIds()).containsExactly(expansion.itemsToCreate().getFirst().getId());
    }

    @Test
    void should_create_api_children_in_stable_name_and_id_order() {
        apiProductQueryService.initWith(
            List.of(
                ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).apiIds(Set.of("api-z", "api-a-2", "api-a-1")).build()
            )
        );
        apiCrudService.initWith(
            List.of(api("api-z", "Zulu", ENVIRONMENT_ID), api("api-a-2", "Alpha", ENVIRONMENT_ID), api("api-a-1", "alpha", ENVIRONMENT_ID))
        );
        var root = apiProductItem().toBuilder().visibility(PortalVisibility.PRIVATE).published(true).build();

        var expansion = service.expand(List.of(root), ENVIRONMENT_ID);

        var createdRoot = expansion.itemsToCreate().getFirst();
        var children = expansion.itemsToCreate().subList(1, expansion.itemsToCreate().size());
        assertThat(children).extracting(CreatePortalNavigationItem::getApiId).containsExactly("api-a-1", "api-a-2", "api-z");
        assertThat(children).extracting(CreatePortalNavigationItem::getOrder).containsExactly(0, 1, 2);
        assertThat(children).allSatisfy(child -> {
            assertThat(child.getId()).isNotNull();
            assertThat(child.getType()).isEqualTo(PortalNavigationItemType.API);
            assertThat(child.getParentId()).isEqualTo(createdRoot.getId());
            assertThat(child.getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
            assertThat(child.getPublished()).isFalse();
            assertThat(child.getContentType()).isEqualTo(PortalPageContentType.GRAVITEE_MARKDOWN);
        });
    }

    @Test
    void should_create_one_child_when_api_lookup_returns_duplicate_records() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).apiIds(Set.of("api-1")).build())
        );
        apiCrudService.initWith(List.of(api("api-1", "First result", ENVIRONMENT_ID), api("api-1", "Duplicate result", ENVIRONMENT_ID)));

        var expansion = service.expand(List.of(apiProductItem()), ENVIRONMENT_ID);

        assertThat(expansion.itemsToCreate().subList(1, expansion.itemsToCreate().size()))
            .singleElement()
            .satisfies(child -> {
                assertThat(child.getApiId()).isEqualTo("api-1");
                assertThat(child.getTitle()).isEqualTo("First result");
            });
    }

    @Test
    void should_reject_product_from_another_environment() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId("other-environment").apiIds(Set.of()).build())
        );

        assertThatThrownBy(() -> service.expand(List.of(apiProductItem()), ENVIRONMENT_ID)).isInstanceOf(ApiProductNotFoundException.class);
    }

    @Test
    void should_reject_missing_api_before_returning_expansion() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).apiIds(Set.of("missing-api")).build())
        );

        assertThatThrownBy(() -> service.expand(List.of(apiProductItem()), ENVIRONMENT_ID)).isInstanceOf(ApiNotFoundException.class);
    }

    private static CreatePortalNavigationItem apiProductItem() {
        return CreatePortalNavigationItem.builder()
            .title("Product")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .type(PortalNavigationItemType.API_PRODUCT)
            .parentId(PortalNavigationItemId.of(PARENT_ID))
            .apiProductId(API_PRODUCT_ID)
            .build();
    }

    private static Api api(String id, String name, String environmentId) {
        return Api.builder().id(id).name(name).environmentId(environmentId).build();
    }
}
