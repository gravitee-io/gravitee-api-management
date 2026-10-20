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
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.ArrayList;
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
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalNavigationItemCreationExpansionDomainService service;

    @BeforeEach
    void setUp() {
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        apiCrudService = new ApiCrudServiceInMemory();
        queryService = new PortalNavigationItemsQueryServiceInMemory();
        service = new PortalNavigationItemCreationExpansionDomainService(apiProductQueryService, apiCrudService, queryService);
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
        assertThat(expansion.generatedApiNavigationItemIds()).containsExactlyElementsOf(
            children.stream().map(CreatePortalNavigationItem::getId).toList()
        );
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
    void should_not_consider_requested_api_items_as_generated() {
        var apiItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .title("API")
            .area(PortalArea.TOP_NAVBAR)
            .type(PortalNavigationItemType.API)
            .parentId(PortalNavigationItemId.of(PARENT_ID))
            .apiId("api-1")
            .build();

        var expansion = service.expand(List.of(apiItem), ENVIRONMENT_ID);

        assertThat(expansion.generatedApiNavigationItemIds()).isEmpty();
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

    @Test
    void should_infer_pending_ownership_from_original_graph_without_reordering_or_mutating_requests() {
        var api = pendingApi().toBuilder().visibility(PortalVisibility.PRIVATE).build();
        var folder = documentation(PortalNavigationItemType.FOLDER, api.getId());
        var page = documentation(PortalNavigationItemType.PAGE, folder.getId());
        var requested = List.of(page, folder, api);

        var expansion = service.expand(requested, ENVIRONMENT_ID);

        assertThat(expansion.requestedItemIds()).containsExactly(page.getId(), folder.getId(), api.getId());
        assertThat(expansion.itemsToCreate())
            .extracting(CreatePortalNavigationItem::getId)
            .containsExactly(page.getId(), folder.getId(), api.getId());
        var normalizedPage = expansion.itemsToCreate().getFirst();
        assertThat(normalizedPage.getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
        assertThat(normalizedPage.getParentId()).isEqualTo(folder.getId());
        assertThat(normalizedPage.getVisibility()).isNull();
        var normalizedFolder = expansion.itemsToCreate().get(1);
        assertThat(normalizedFolder.getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
        assertThat(normalizedFolder.getParentId()).isNull();
        assertThat(normalizedFolder.getRenderedParentId()).isEqualTo(api.getId());
        assertThat(normalizedFolder.getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
        assertThat(expansion.itemsToCreate().getLast().getReference()).isEqualTo(NavigationItemReference.defaultReference());
        assertThat(requested).allSatisfy(item -> assertThat(item.getReference()).isEqualTo(NavigationItemReference.defaultReference()));
        assertThat(folder.getParentId()).isEqualTo(api.getId());
        assertThat(folder.getVisibility()).isNull();
        assertThat(page.getParentId()).isEqualTo(folder.getId());
        assertThat(page.getVisibility()).isNull();
    }

    @Test
    void should_keep_pending_product_descendants_portal_owned_using_the_full_original_ancestor_chain() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENVIRONMENT_ID).apiIds(Set.of()).build())
        );
        var product = apiProductItem().toBuilder().id(PortalNavigationItemId.random()).parentId(null).build();
        var folder = documentation(PortalNavigationItemType.FOLDER, product.getId());
        var api = pendingApi().toBuilder().parentId(folder.getId()).build();
        var page = documentation(PortalNavigationItemType.PAGE, api.getId());

        var expansion = service.expand(List.of(page, api, folder, product), ENVIRONMENT_ID);

        assertThat(expansion.itemsToCreate())
            .extracting(CreatePortalNavigationItem::getId)
            .containsExactly(page.getId(), api.getId(), folder.getId(), product.getId());
        assertThat(expansion.itemsToCreate()).allSatisfy(item ->
            assertThat(item.getReference()).isEqualTo(NavigationItemReference.defaultReference())
        );
        assertThat(expansion.itemsToCreate().getFirst().getParentId()).isEqualTo(api.getId());
    }

    @Test
    void should_reject_original_graph_cycle_that_canonicalization_would_hide() {
        var api = pendingApi();
        var folder = documentation(PortalNavigationItemType.FOLDER, api.getId());
        api.setParentId(folder.getId());

        assertThatThrownBy(() -> service.expand(List.of(folder, api), ENVIRONMENT_ID))
            .isInstanceOf(InvalidPortalNavigationItemDataException.class)
            .hasMessage(InvalidPortalNavigationItemDataException.cyclicParentHierarchy().getMessage());
    }

    @Test
    void should_reject_duplicate_pending_ids_before_resolving_ownership() {
        var first = documentation(PortalNavigationItemType.FOLDER, null);
        var duplicate = first.toBuilder().title("Another folder").build();

        assertThatThrownBy(() -> service.expand(List.of(first, duplicate), ENVIRONMENT_ID)).isInstanceOf(ItemAlreadyExistsException.class);
    }

    @Test
    void should_not_classify_api_as_standalone_when_an_ancestor_is_missing() {
        var api = PortalNavigationItemFixtures.anApi(PARENT_ID, "API", PortalNavigationItemId.random(), "api-1");
        api.setEnvironmentId(ENVIRONMENT_ID);
        queryService.initWith(List.of(api));
        var page = documentation(PortalNavigationItemType.PAGE, api.getId());

        assertThatThrownBy(() -> service.expand(List.of(page), ENVIRONMENT_ID)).isInstanceOf(ParentNotFoundException.class);
    }

    @Test
    void should_not_resolve_a_parent_from_another_environment() {
        var api = PortalNavigationItemFixtures.anApi(PARENT_ID, "API", null, "api-1");
        api.setEnvironmentId("other-environment");
        queryService.initWith(List.of(api));
        var page = documentation(PortalNavigationItemType.PAGE, api.getId());

        assertThatThrownBy(() -> service.expand(List.of(page), ENVIRONMENT_ID)).isInstanceOf(ParentNotFoundException.class);
    }

    @Test
    void should_keep_portal_ownership_when_product_ancestor_is_more_than_fifty_levels_above_api() {
        var product = PortalNavigationItemFixtures.anApiProduct(
            PortalNavigationItemId.random().toString(),
            "Product",
            null,
            API_PRODUCT_ID
        );
        product.setEnvironmentId(ENVIRONMENT_ID);
        var hierarchy = new ArrayList<PortalNavigationItem>();
        hierarchy.add(product);
        var parentId = product.getId();
        for (int depth = 0; depth < 60; depth++) {
            var folder = PortalNavigationItemFixtures.aFolder("Section " + depth, parentId);
            folder.setEnvironmentId(ENVIRONMENT_ID);
            hierarchy.add(folder);
            parentId = folder.getId();
        }
        var api = PortalNavigationItemFixtures.anApi(PARENT_ID, "API", parentId, "api-1");
        api.setEnvironmentId(ENVIRONMENT_ID);
        hierarchy.add(api);
        queryService.initWith(hierarchy);
        var page = documentation(PortalNavigationItemType.PAGE, api.getId());

        var expansion = service.expand(List.of(page), ENVIRONMENT_ID);

        assertThat(expansion.itemsToCreate())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getReference()).isEqualTo(NavigationItemReference.defaultReference());
                assertThat(item.getParentId()).isEqualTo(api.getId());
                assertThat(item.getRenderedParentId()).isNull();
            });
    }

    @Test
    void should_keep_explicit_api_reference_and_canonical_parent_unchanged() {
        var reference = new NavigationItemReference.ApiReference("gamma-api");
        var folder = documentation(PortalNavigationItemType.FOLDER, null).toBuilder().reference(reference).build();
        var page = documentation(PortalNavigationItemType.PAGE, folder.getId()).toBuilder().reference(reference).build();

        var expansion = service.expand(List.of(folder, page), ENVIRONMENT_ID);

        assertThat(expansion.itemsToCreate()).allSatisfy(item -> assertThat(item.getReference()).isEqualTo(reference));
        assertThat(expansion.itemsToCreate().getFirst().getParentId()).isNull();
        assertThat(expansion.itemsToCreate().getLast().getParentId()).isEqualTo(folder.getId());
        assertThat(expansion.itemsToCreate()).allSatisfy(item -> assertThat(item.getRenderedParentId()).isNull());
    }

    private static CreatePortalNavigationItem pendingApi() {
        return CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .title("API")
            .area(PortalArea.TOP_NAVBAR)
            .type(PortalNavigationItemType.API)
            .apiId("api-1")
            .build();
    }

    private static CreatePortalNavigationItem documentation(PortalNavigationItemType type, PortalNavigationItemId parentId) {
        return CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .title("Documentation")
            .area(PortalArea.TOP_NAVBAR)
            .type(type)
            .parentId(parentId)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
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
