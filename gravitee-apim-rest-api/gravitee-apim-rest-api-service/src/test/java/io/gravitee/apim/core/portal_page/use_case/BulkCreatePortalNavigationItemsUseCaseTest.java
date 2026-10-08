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
package io.gravitee.apim.core.portal_page.use_case;

import static fixtures.core.model.PortalNavigationItemFixtures.API1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BulkCreatePortalNavigationItemsUseCaseTest {

    private BulkCreatePortalNavigationItemUseCase useCase;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalNavigationItemValidatorService validatorService;
    private PortalNavigationItemCreationExpansionDomainService creationExpansionDomainService;
    private PortalNavigationDefaultPageDomainService defaultPageDomainService;
    private ApiProductQueryServiceInMemory apiProductQueryService;
    private ApiCrudServiceInMemory apiCrudService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();

        final var crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        final var pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        apiProductQueryService = new ApiProductQueryServiceInMemory();
        validatorService = new PortalNavigationItemValidatorService(
            queryService,
            pageContentQueryService,
            apiProductQueryService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        apiCrudService = new ApiCrudServiceInMemory();

        final var domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
            apiCrudService,
            new PortalNavigationItemSourceDomainServiceInMemory(),
            new ApiOwnedNavigationDomainService(queryService, crudService)
        );
        creationExpansionDomainService = new PortalNavigationItemCreationExpansionDomainService(
            apiProductQueryService,
            apiCrudService,
            queryService
        );
        defaultPageDomainService = new PortalNavigationDefaultPageDomainService(
            queryService,
            domainService,
            pageContentCrudService,
            apiCrudService
        );
        useCase = new BulkCreatePortalNavigationItemUseCase(
            domainService,
            validatorService,
            creationExpansionDomainService,
            defaultPageDomainService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );
        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
    }

    @Test
    void should_bootstrap_multiple_products_with_default_product_and_api_pages_and_return_only_requested_roots_in_request_order() {
        apiProductQueryService.initWith(
            List.of(
                ApiProduct.builder().id("product-1").environmentId(ENV_ID).apiIds(Set.of("shared-api")).build(),
                ApiProduct.builder().id("product-2").environmentId(ENV_ID).apiIds(Set.of("shared-api")).build()
            )
        );
        apiCrudService.initWith(List.of(Api.builder().id("shared-api").name("Shared API").environmentId(ENV_ID).build()));
        var firstProduct = productItem("product-1", "First product", 0);
        var secondProduct = productItem("product-2", "Second product", 1);

        var output = useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(firstProduct, secondProduct)));

        assertThat(output.items())
            .extracting(item -> ((io.gravitee.apim.core.portal_page.model.PortalNavigationApiProduct) item).getApiProductId())
            .containsExactly("product-1", "product-2");
        assertDefaultProductAndApiPages(output.items().get(0).getId());
        assertDefaultProductAndApiPages(output.items().get(1).getId());
    }

    @Test
    void should_create_default_product_page_when_product_has_no_apis() {
        apiProductQueryService.initWith(List.of(ApiProduct.builder().id("product-1").environmentId(ENV_ID).apiIds(Set.of()).build()));
        var product = productItem("product-1", "Product without APIs", 0);

        var output = useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(product)));

        assertThat(output.items()).singleElement();
        assertThat(queryService.findByParentIdAndEnvironmentId(ENV_ID, output.items().getFirst().getId()))
            .singleElement()
            .isInstanceOfSatisfying(PortalNavigationPage.class, page -> {
                assertThat(page.getTitle()).isEqualTo("Overview");
                assertThat(page.getOrder()).isZero();
                assertThat(page.getPublished()).isFalse();
            });
    }

    @Test
    void should_inherit_pending_api_ownership_and_visibility_through_pending_folders_in_request_order() {
        apiCrudService.initWith(List.of(Api.builder().id("pending-api").name("Pending API").environmentId(ENV_ID).build()));
        var api = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .apiId("pending-api")
            .area(PortalArea.TOP_NAVBAR)
            .parentId(PortalNavigationItemId.of(APIS_ID))
            .visibility(PortalVisibility.PRIVATE)
            .order(0)
            .build();
        var folder = documentation(PortalNavigationItemType.FOLDER, "Guides", api.getId());
        var page = documentation(PortalNavigationItemType.PAGE, "Overview", folder.getId());
        var link = documentation(PortalNavigationItemType.LINK, "Reference", api.getId()).toBuilder().order(1).build();

        var output = useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(api, folder, page, link)));

        assertThat(output.items())
            .extracting(PortalNavigationItem::getId)
            .containsExactly(api.getId(), folder.getId(), page.getId(), link.getId());
        assertThat(output.items().getFirst().getReference()).isEqualTo(NavigationItemReference.defaultReference());
        assertThat(output.items().subList(1, 4)).allSatisfy(item -> {
            assertThat(item.getReference()).isEqualTo(new NavigationItemReference.ApiReference("pending-api"));
            assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
        });
        assertThat(output.items().get(1).getParentId()).isNull();
        assertThat(output.items().get(1).getRootId()).isEqualTo(folder.getId());
        assertThat(output.items().get(2).getParentId()).isEqualTo(folder.getId());
        assertThat(output.items().get(2).getRootId()).isEqualTo(folder.getId());
        assertThat(output.items().get(3).getParentId()).isNull();
        assertThat(
            queryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
                ENV_ID,
                PortalArea.TOP_NAVBAR,
                new NavigationItemReference.ApiReference("pending-api")
            )
        )
            .extracting(PortalNavigationItem::getOrder)
            .containsExactly(0, 1);
        assertThat(pageContentCrudService.storage()).hasSize(1);
    }

    @Test
    void should_distinguish_standalone_and_product_listing_of_same_api_in_one_batch() {
        var standaloneApi = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
        var product = PortalNavigationItemFixtures.anApiProduct(PortalNavigationItemId.random().toString(), "Product", null, "product-id");
        product.markAsRoot();
        var productFolder = PortalNavigationItemFixtures.aFolder("Product APIs", product.getId());
        productFolder.updateParent(product);
        var productApi = PortalNavigationItemFixtures.anApi(
            PortalNavigationItemId.random().toString(),
            "Product API",
            productFolder.getId(),
            "api-1"
        );
        productApi.updateParent(productFolder);
        queryService.storage().addAll(List.of(product, productFolder, productApi));
        var standalonePage = documentation(PortalNavigationItemType.PAGE, "Standalone overview", standaloneApi.getId());
        var productPage = documentation(PortalNavigationItemType.PAGE, "Product overview", productApi.getId());

        var output = useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(standalonePage, productPage)));

        assertThat(output.items()).extracting(PortalNavigationItem::getId).containsExactly(standalonePage.getId(), productPage.getId());
        assertThat(output.items().getFirst().getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
        assertThat(output.items().getFirst().getParentId()).isNull();
        assertThat(output.items().getLast().getReference()).isEqualTo(NavigationItemReference.defaultReference());
        assertThat(output.items().getLast().getParentId()).isEqualTo(productApi.getId());
        assertThat(output.items().getLast().getRootId()).isEqualTo(product.getId());
    }

    @ParameterizedTest
    @EnumSource(value = PortalNavigationItemType.class, names = { "API", "API_PRODUCT" })
    void should_reject_api_or_product_under_pending_api_owned_folder_before_writes(PortalNavigationItemType type) {
        apiCrudService.initWith(
            List.of(
                Api.builder().id("pending-api").name("Pending API").environmentId(ENV_ID).build(),
                Api.builder().id("nested-api").name("Nested API").environmentId(ENV_ID).build()
            )
        );
        apiProductQueryService.initWith(List.of(ApiProduct.builder().id("product-1").environmentId(ENV_ID).apiIds(Set.of()).build()));
        var api = PortalNavigationItemFixtures.aCreatePortalNavigationApi("pending-api", PortalNavigationItemId.of(APIS_ID))
            .toBuilder()
            .id(PortalNavigationItemId.random())
            .build();
        var folder = documentation(PortalNavigationItemType.FOLDER, "Guides", api.getId());
        var nestedItem = type == PortalNavigationItemType.API
            ? PortalNavigationItemFixtures.aCreatePortalNavigationApi("nested-api", folder.getId())
            : productItem("product-1", "Nested product", 0).toBuilder().parentId(folder.getId()).build();
        var existingItems = List.copyOf(queryService.storage());

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(api, folder, nestedItem)))
        );

        assertThat(exception.getMessage()).isEqualTo(InvalidPortalNavigationItemDataException.parentHierarchyContainsApi().getMessage());
        assertThat(queryService.storage()).containsExactlyElementsOf(existingItems);
        assertThat(pageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_pending_cycle_before_canonicalization_can_hide_it_and_before_writes() {
        var api = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .apiId("pending-api")
            .area(PortalArea.TOP_NAVBAR)
            .build();
        var folder = documentation(PortalNavigationItemType.FOLDER, "Guides", api.getId());
        api.setParentId(folder.getId());
        var existingItems = List.copyOf(queryService.storage());

        var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(folder, api)))
        );

        assertThat(exception.getMessage()).isEqualTo(InvalidPortalNavigationItemDataException.cyclicParentHierarchy().getMessage());
        assertThat(queryService.storage()).containsExactlyElementsOf(existingItems);
        assertThat(pageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_duplicate_pending_ids_before_creating_items_or_page_content() {
        var first = documentation(PortalNavigationItemType.PAGE, "First page", PortalNavigationItemId.of(API1_ID));
        var duplicate = first.toBuilder().title("Second page").build();
        var existingItems = List.copyOf(queryService.storage());

        assertThrows(ItemAlreadyExistsException.class, () ->
            useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(first, duplicate)))
        );

        assertThat(queryService.storage()).containsExactlyElementsOf(existingItems);
        assertThat(pageContentCrudService.storage()).isEmpty();
    }

    @Test
    void should_create_list_of_portal_navigation_items_if_validation_succeeds() {
        // Given
        final var firstItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("Folder 1")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .build();
        firstItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        final var secondItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("Folder 2")
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .build();
        secondItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When
        final var output = useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(firstItem, secondItem)));

        // Then
        assertThat(output.items()).hasSize(2);
        assertThat(output.items()).extracting(PortalNavigationItem::getId).containsExactly(firstItem.getId(), secondItem.getId());

        final var items = queryService.findByParentIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(APIS_ID));
        final var createdFirstItem = items
            .stream()
            .filter(item -> item.getId().equals(firstItem.getId()))
            .findFirst();
        final var createdSecondItem = items
            .stream()
            .filter(item -> item.getId().equals(secondItem.getId()))
            .findFirst();

        assertThat(createdFirstItem).isPresent();
        assertThat(createdFirstItem.get()).satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo(firstItem.getTitle());
            assertThat(item.getArea()).isEqualTo(firstItem.getArea());
            assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(item.getPublished()).isFalse();
        });

        assertThat(createdSecondItem).isPresent();
        assertThat(createdSecondItem.get()).satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo(secondItem.getTitle());
            assertThat(item.getArea()).isEqualTo(secondItem.getArea());
            assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(item.getPublished()).isFalse();
        });
    }

    @Test
    void should_fail_when_at_least_one_item_is_invalid_during_bulk_validation() {
        // Given
        final var domainService = mock(PortalNavigationItemDomainService.class);
        final var useCase = new BulkCreatePortalNavigationItemUseCase(
            domainService,
            validatorService,
            creationExpansionDomainService,
            defaultPageDomainService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );

        final var validItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("Folder 1")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(PortalNavigationItemId.of(APIS_ID))
            .build();

        final var invalidApiItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .title("API without parent")
            .area(PortalArea.TOP_NAVBAR)
            .order(1)
            .apiId("api-2")
            .build();

        // When
        final var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
            useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(validItem, invalidApiItem)))
        );

        // Then
        assertThat(exception.getMessage()).isEqualTo("The parentId field is required and cannot be blank.");
        verify(domainService, never()).create(anyString(), anyString(), any(CreatePortalNavigationItem.class));
    }

    @Test
    void should_stop_after_first_persistence_failure_without_compensating_previous_writes() {
        var domainService = mock(PortalNavigationItemDomainService.class);
        var validatorService = mock(PortalNavigationItemValidatorService.class);
        var creationExpansionDomainService = mock(PortalNavigationItemCreationExpansionDomainService.class);
        var defaultPageDomainService = mock(PortalNavigationDefaultPageDomainService.class);
        var rootId = PortalNavigationItemId.random();
        var root = productItem("product-1", "Product", 0).toBuilder().id(rootId).build();
        var firstChild = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .apiId("api-1")
            .parentId(rootId)
            .build();
        var secondChild = firstChild.toBuilder().id(PortalNavigationItemId.random()).apiId("api-2").build();
        var expansion = new PortalNavigationItemCreationExpansionDomainService.Expansion(
            List.of(root, firstChild, secondChild),
            List.of(rootId)
        );
        var persistedRoot = PortalNavigationItemFixtures.anApiProduct(
            rootId.toString(),
            "Product",
            PortalNavigationItemId.of(APIS_ID),
            "product-1"
        );
        when(creationExpansionDomainService.expand(List.of(root), ENV_ID)).thenReturn(expansion);
        when(domainService.create(ORG_ID, ENV_ID, root)).thenReturn(persistedRoot);
        when(domainService.create(ORG_ID, ENV_ID, firstChild)).thenThrow(new IllegalStateException("persistence failure"));
        var useCase = new BulkCreatePortalNavigationItemUseCase(
            domainService,
            validatorService,
            creationExpansionDomainService,
            defaultPageDomainService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );

        assertThrows(IllegalStateException.class, () ->
            useCase.execute(new BulkCreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, List.of(root)))
        );

        verify(domainService).create(ORG_ID, ENV_ID, root);
        verify(domainService).create(ORG_ID, ENV_ID, firstChild);
        verify(domainService, never()).create(anyString(), anyString(), eq(secondChild));
    }

    private static CreatePortalNavigationItem productItem(String apiProductId, String title, int order) {
        return CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API_PRODUCT)
            .apiProductId(apiProductId)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .order(order)
            .parentId(PortalNavigationItemId.of(APIS_ID))
            .build();
    }

    private static CreatePortalNavigationItem documentation(PortalNavigationItemType type, String title, PortalNavigationItemId parentId) {
        return CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(type)
            .title(title)
            .area(PortalArea.TOP_NAVBAR)
            .parentId(parentId)
            .order(0)
            .url(type == PortalNavigationItemType.LINK ? "https://gravitee.io" : null)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
    }

    private void assertDefaultProductAndApiPages(PortalNavigationItemId productNavigationItemId) {
        var productChildren = queryService.findByParentIdAndEnvironmentId(ENV_ID, productNavigationItemId);
        assertThat(productChildren)
            .filteredOn(PortalNavigationPage.class::isInstance)
            .singleElement()
            .satisfies(page -> {
                assertThat(page.getTitle()).isEqualTo("Overview");
                assertThat(page.getOrder()).isZero();
                assertThat(page.getPublished()).isFalse();
            });
        assertThat(productChildren)
            .filteredOn(PortalNavigationApi.class::isInstance)
            .singleElement()
            .satisfies(apiItem -> {
                assertThat(apiItem.getOrder()).isEqualTo(1);
                assertThat(queryService.findByParentIdAndEnvironmentId(ENV_ID, apiItem.getId()))
                    .singleElement()
                    .isInstanceOfSatisfying(PortalNavigationPage.class, page -> assertThat(page.getTitle()).isEqualTo("Overview"));
            });
    }
}
