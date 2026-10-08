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

import static fixtures.core.model.PortalNavigationItemFixtures.API1_FOLDER_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.API1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.APIS_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.CATEGORY1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiProductQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationDefaultPageDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemCreationExpansionDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePortalNavigationItemUseCaseTest {

    private CreatePortalNavigationItemUseCase useCase;
    private PortalNavigationItemDomainService domainService;
    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private PortalNavigationItemValidatorService validatorService;
    private PortalNavigationItemCreationExpansionDomainService creationExpansionDomainService;
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();

        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        PortalPageContentQueryServiceInMemory pageContentQueryService = new PortalPageContentQueryServiceInMemory(
            pageContentCrudService.storage()
        );
        validatorService = new PortalNavigationItemValidatorService(
            queryService,
            pageContentQueryService,
            apiProductQueryService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );
        domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
            apiCrudService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );
        creationExpansionDomainService = new PortalNavigationItemCreationExpansionDomainService(
            apiProductQueryService,
            apiCrudService,
            queryService
        );
        var defaultPageDomainService = new PortalNavigationDefaultPageDomainService(
            queryService,
            domainService,
            pageContentCrudService,
            apiCrudService
        );
        useCase = new CreatePortalNavigationItemUseCase(
            domainService,
            validatorService,
            creationExpansionDomainService,
            defaultPageDomainService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );
        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
        apiCrudService.initWith(List.of(Api.builder().id("apiId").name("apiIdName").build()));
    }

    @Test
    void should_create_product_and_its_api_children_with_default_pages_but_return_only_product() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id("product-id").environmentId(ENV_ID).apiIds(Set.of("api-1", "api-2")).build())
        );
        apiCrudService.initWith(
            List.of(
                Api.builder().id("api-1").name("Alpha").environmentId(ENV_ID).build(),
                Api.builder().id("api-2").name("Beta").environmentId(ENV_ID).build()
            )
        );
        var toCreate = CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API_PRODUCT)
            .apiProductId("product-id")
            .title("Product")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(PortalNavigationItemId.of(APIS_ID))
            .build();

        var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate));

        assertThat(output.item().getType()).isEqualTo(PortalNavigationItemType.API_PRODUCT);
        var children = queryService.findByParentIdAndEnvironmentId(ENV_ID, output.item().getId());
        assertThat(children)
            .filteredOn(PortalNavigationApi.class::isInstance)
            .extracting(item -> ((PortalNavigationApi) item).getApiId())
            .containsExactly("api-1", "api-2");
        assertThat(children)
            .filteredOn(PortalNavigationPage.class::isInstance)
            .singleElement()
            .satisfies(page -> {
                assertThat(page.getTitle()).isEqualTo("Overview");
                assertThat(page.getPublished()).isFalse();
                assertThat(page.getParentId()).isEqualTo(output.item().getId());
            });
        assertThat(children)
            .filteredOn(PortalNavigationApi.class::isInstance)
            .allSatisfy(apiItem ->
                assertThat(queryService.findByParentIdAndEnvironmentId(ENV_ID, apiItem.getId()))
                    .singleElement()
                    .isInstanceOfSatisfying(PortalNavigationPage.class, page -> {
                        assertThat(page.getTitle()).isEqualTo("Overview");
                        assertThat(page.getPublished()).isFalse();
                        assertThat(page.getParentId()).isEqualTo(apiItem.getId());
                    })
            );
        assertThat(pageContentCrudService.storage())
            .hasSize(3)
            .allSatisfy(content -> assertThat(content).isInstanceOf(GraviteeMarkdownPageContent.class));
    }

    @Test
    void should_keep_created_product_and_api_children_when_default_page_seeding_fails() {
        apiProductQueryService.initWith(
            List.of(ApiProduct.builder().id("product-id").environmentId(ENV_ID).apiIds(Set.of("api-1")).build())
        );
        apiCrudService.initWith(List.of(Api.builder().id("api-1").name("Alpha").environmentId(ENV_ID).build()));
        var failingPageContentCrudService = new PortalPageContentCrudServiceInMemory() {
            @Override
            public PortalPageContent<?> create(PortalPageContent<?> content) {
                throw new IllegalStateException("page content persistence failure");
            }
        };
        var failingDefaultPageDomainService = new PortalNavigationDefaultPageDomainService(
            queryService,
            domainService,
            failingPageContentCrudService,
            apiCrudService
        );
        var failingSeedUseCase = new CreatePortalNavigationItemUseCase(
            domainService,
            validatorService,
            creationExpansionDomainService,
            failingDefaultPageDomainService,
            new PortalNavigationItemSourceDomainServiceInMemory()
        );
        var toCreate = CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.API_PRODUCT)
            .apiProductId("product-id")
            .title("Product")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(PortalNavigationItemId.of(APIS_ID))
            .build();

        var output = failingSeedUseCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate));

        assertThat(output.item().getType()).isEqualTo(PortalNavigationItemType.API_PRODUCT);
        assertThat(queryService.findByParentIdAndEnvironmentId(ENV_ID, output.item().getId()))
            .singleElement()
            .satisfies(apiItem -> assertThat(queryService.findByParentIdAndEnvironmentId(ENV_ID, apiItem.getId())).isEmpty());
    }

    @Test
    void should_create_api_item_if_validation_succeeds() {
        // Given
        final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .apiId("apiId")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
        createPortalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When
        useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        final var items = queryService.findByParentIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(APIS_ID));
        final var createdItem = items
            .stream()
            .filter(item -> item.getId().equals(createPortalNavigationItem.getId()))
            .findFirst();
        assertThat(createdItem).isPresent();
        assertThat(createdItem.get()).satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo("apiIdName");
            assertThat(item.getArea()).isEqualTo(createPortalNavigationItem.getArea());
            assertThat(item.getOrder()).isEqualTo(createPortalNavigationItem.getOrder());
            assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(item.getPublished()).isFalse();
        });
    }

    @Test
    void should_not_create_api_item_if_validation_fails() {
        // Given
        final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .apiId("wrongApiId")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
        createPortalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When

        final Executable throwing = () ->
            useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        Exception exception = assertThrows(ApiNotFoundException.class, throwing);
        assertThat(exception.getMessage()).isEqualTo("Api not found.");
    }

    @Test
    void should_store_an_item_with_no_parent_as_top_level_documentation_of_the_api_that_owns_it() {
        var apiReference = new NavigationItemReference.ApiReference("apiId");
        var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("Guides")
            .area(PortalArea.TOP_NAVBAR)
            .reference(apiReference)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();

        useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        var created = queryService.findByIdAndEnvironmentId(ENV_ID, createPortalNavigationItem.getId());
        assertThat(created.getReference()).isEqualTo(apiReference);
        assertThat(created.getParentId()).isNull();
        assertThat(queryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(ENV_ID, PortalArea.TOP_NAVBAR, apiReference))
            .extracting(PortalNavigationItem::getId)
            .containsExactly(created.getId());
    }

    @Test
    void should_create_item_if_validation_succeeds() {
        // Given
        final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("title")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
        createPortalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When
        useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        final var items = queryService.findByParentIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(APIS_ID));
        final var createdItem = items
            .stream()
            .filter(item -> item.getId().equals(createPortalNavigationItem.getId()))
            .findFirst();
        assertThat(createdItem).isPresent();
        assertThat(createdItem.get()).satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo(createPortalNavigationItem.getTitle());
            assertThat(item.getArea()).isEqualTo(createPortalNavigationItem.getArea());
            assertThat(item.getOrder()).isEqualTo(createPortalNavigationItem.getOrder());
            assertThat(item.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
            assertThat(item.getPublished()).isFalse();
        });
    }

    @Test
    void should_not_create_item_if_validation_fails() {
        // Given
        final var numberOfItems = queryService.storage().size();

        final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.LINK)
            .title("title")
            .url("invalid url")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
        createPortalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

        // When
        final Executable throwing = () ->
            useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        Exception exception = assertThrows(RuntimeException.class, throwing);
        assertThat(exception.getMessage()).isEqualTo("Provided url is invalid");
        assertThat(queryService.storage()).hasSize(numberOfItems);
    }

    @Test
    void should_create_default_page_content_when_content_id_is_null() {
        // Given
        final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.PAGE)
            .title("title")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
        final var numberOfContents = pageContentCrudService.storage().size();

        // When
        final var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        final var contentId = ((PortalNavigationPage) output.item()).getPortalPageContentId();
        final var contents = pageContentCrudService.storage();
        assertThat(contents)
            .hasSize(numberOfContents + 1)
            .anySatisfy(content -> {
                assertThat(content.getId()).isEqualTo(contentId);
                assertThat(content.getOrganizationId()).isEqualTo(ORG_ID);
                assertThat(content.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(content).isInstanceOf(GraviteeMarkdownPageContent.class);
                assertThat(((GraviteeMarkdownPageContent) content).getContent().value()).isEqualTo("default page content");
            });
    }

    @Test
    void should_create_openapi_page_content_when_content_type_is_openapi() {
        // Given
        final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.PAGE)
            .title("title")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.OPENAPI)
            .build();
        final var numberOfContents = pageContentCrudService.storage().size();

        // When
        final var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        final var contentId = ((PortalNavigationPage) output.item()).getPortalPageContentId();
        final var contents = pageContentCrudService.storage();
        assertThat(contents)
            .hasSize(numberOfContents + 1)
            .anySatisfy(content -> {
                assertThat(content.getId()).isEqualTo(contentId);
                assertThat(content.getOrganizationId()).isEqualTo(ORG_ID);
                assertThat(content.getEnvironmentId()).isEqualTo(ENV_ID);
                assertThat(content).isInstanceOf(OpenApiPageContent.class);
                assertThat(((OpenApiPageContent) content).getContent().value()).isEqualTo("openapi: 3.0.3");
            });
    }

    @Test
    void should_set_portal_navigation_item_to_not_published() {
        // Given
        final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.FOLDER)
            .title("title")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();

        // When
        final var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        assertThat(output.item().getPublished()).isFalse();
    }

    @Nested
    class CreateItemWithApiAsParent {

        @Test
        void should_create_api_owned_page_at_canonical_root_under_standalone_api() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.PAGE)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .parentId(PortalNavigationItemId.of(API1_ID))
                .order(0)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .build();

            // When
            final var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

            // Then
            assertThat(output.item().getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
            assertThat(output.item().getParentId()).isNull();
            assertThat(output.item().getRootId()).isEqualTo(output.item().getId());
        }

        @Test
        void should_create_api_owned_link_at_canonical_root_under_standalone_api() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.LINK)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .parentId(PortalNavigationItemId.of(API1_ID))
                .url("https://gravitee.io")
                .order(0)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .build();

            // When
            final var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

            // Then
            assertThat(output.item().getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
            assertThat(output.item().getParentId()).isNull();
            assertThat(output.item().getRootId()).isEqualTo(output.item().getId());
        }

        @Test
        void should_create_api_owned_folder_at_canonical_root_under_standalone_api() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.FOLDER)
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .parentId(PortalNavigationItemId.of(API1_ID))
                .order(0)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .build();

            // When
            final var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

            // Then
            assertThat(output.item().getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
            assertThat(output.item().getParentId()).isNull();
            assertThat(output.item().getRootId()).isEqualTo(output.item().getId());
        }

        @Test
        void should_not_create_api_item_with_parent_id_set_to_portal_navigation_item_type_api() {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.API)
                .area(PortalArea.TOP_NAVBAR)
                .apiId("apiId")
                .parentId(PortalNavigationItemId.of(API1_ID))
                .order(0)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .build();

            // When
            final Executable throwing = () ->
                useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Parent hierarchy cannot include API items.");
        }
    }

    @Nested
    class DocumentationOwnership {

        @ParameterizedTest
        @CsvSource({ "API, 0", "API, 2", "API_PRODUCT, 0", "API_PRODUCT, 2" })
        void should_reject_api_or_api_product_creation_under_persisted_api_owned_folder(
            PortalNavigationItemType type,
            int nestedFolderCount
        ) {
            var parent = PortalNavigationItemFixtures.aFolder("API documentation")
                .toBuilder()
                .reference(new NavigationItemReference.ApiReference("api-1"))
                .build();
            parent.markAsRoot();
            crudService.create(parent);
            for (int depth = 0; depth < nestedFolderCount; depth++) {
                var nestedFolder = PortalNavigationItemFixtures.aFolder("Section " + depth)
                    .toBuilder()
                    .reference(parent.getReference())
                    .build();
                nestedFolder.updateParent(parent);
                crudService.create(nestedFolder);
                parent = nestedFolder;
            }
            apiProductQueryService.initWith(List.of(ApiProduct.builder().id("product-id").environmentId(ENV_ID).apiIds(Set.of()).build()));
            var toCreate = CreatePortalNavigationItem.builder()
                .type(type)
                .title("Nested item")
                .apiId(type == PortalNavigationItemType.API ? "apiId" : null)
                .apiProductId(type == PortalNavigationItemType.API_PRODUCT ? "product-id" : null)
                .area(PortalArea.TOP_NAVBAR)
                .parentId(parent.getId())
                .order(0)
                .build();
            var existingItems = List.copyOf(queryService.storage());

            var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
                useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate))
            );

            assertThat(exception.getMessage()).isEqualTo(
                InvalidPortalNavigationItemDataException.parentHierarchyContainsApi().getMessage()
            );
            assertThat(queryService.storage()).containsExactlyElementsOf(existingItems);
            assertThat(pageContentCrudService.storage()).isEmpty();
        }

        @Test
        void should_reject_segment_collision_in_canonical_api_root_namespace_before_writing() {
            var existing = PortalNavigationItemFixtures.aFolder("Guides")
                .toBuilder()
                .reference(new NavigationItemReference.ApiReference("api-1"))
                .segment("guides")
                .build();
            existing.markAsRoot();
            queryService.storage().add(existing);
            var toCreate = documentation(PortalNavigationItemType.FOLDER, PortalNavigationItemId.of(API1_ID))
                .toBuilder()
                .segment("guides")
                .build();
            var existingItems = List.copyOf(queryService.storage());

            assertThrows(PathConflictException.class, () ->
                useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate))
            );

            assertThat(queryService.storage()).containsExactlyElementsOf(existingItems);
            assertThat(pageContentCrudService.storage()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "FOLDER", "LINK" })
        void should_inherit_api_ownership_and_keep_physical_parent_under_api_owned_folder(PortalNavigationItemType type) {
            var folder = PortalNavigationItemFixtures.aFolder("API documentation")
                .toBuilder()
                .reference(new NavigationItemReference.ApiReference("api-1"))
                .build();
            folder.markAsRoot();
            queryService.initWith(List.of(folder));

            var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, documentation(type, folder.getId())));

            assertThat(output.item().getReference()).isEqualTo(folder.getReference());
            assertThat(output.item().getParentId()).isEqualTo(folder.getId());
            assertThat(output.item().getRootId()).isEqualTo(folder.getId());
        }

        @ParameterizedTest
        @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "FOLDER", "LINK" })
        void should_keep_portal_ownership_when_api_has_product_ancestor_above_a_folder(PortalNavigationItemType type) {
            var product = PortalNavigationItemFixtures.anApiProduct(
                PortalNavigationItemId.random().toString(),
                "Product",
                null,
                "product-id"
            );
            product.markAsRoot();
            var folder = PortalNavigationItemFixtures.aFolder("Product section", product.getId());
            folder.updateParent(product);
            var api = PortalNavigationItemFixtures.anApi(API1_ID, "API", folder.getId(), "api-1");
            api.updateParent(folder);
            queryService.initWith(List.of(product, folder, api));

            var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, documentation(type, api.getId())));

            assertThat(output.item().getReference()).isEqualTo(NavigationItemReference.defaultReference());
            assertThat(output.item().getParentId()).isEqualTo(api.getId());
            assertThat(output.item().getRootId()).isEqualTo(product.getId());
        }

        @ParameterizedTest
        @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "FOLDER", "LINK" })
        void should_keep_portal_ownership_under_legacy_portal_owned_folder_below_api(PortalNavigationItemType type) {
            var parentId = PortalNavigationItemId.of(API1_FOLDER_ID);

            var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, documentation(type, parentId)));

            assertThat(output.item().getReference()).isEqualTo(NavigationItemReference.defaultReference());
            assertThat(output.item().getParentId()).isEqualTo(parentId);
            assertThat(output.item().getRootId()).isEqualTo(PortalNavigationItemId.of(APIS_ID));
        }

        @Test
        void should_inherit_private_visibility_from_rendered_api_parent_after_canonicalizing_parent() {
            var api = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
            api.setVisibility(PortalVisibility.PRIVATE);

            var output = useCase.execute(
                new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, documentation(PortalNavigationItemType.PAGE, api.getId()))
            );

            assertThat(output.item().getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
            assertThat(output.item().getParentId()).isNull();
            assertThat(output.item().getVisibility()).isEqualTo(PortalVisibility.PRIVATE);
        }

        @Test
        void should_reject_explicit_public_visibility_under_private_rendered_api_parent_before_writing() {
            var api = queryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(API1_ID));
            api.setVisibility(PortalVisibility.PRIVATE);
            var toCreate = documentation(PortalNavigationItemType.PAGE, api.getId())
                .toBuilder()
                .visibility(PortalVisibility.PUBLIC)
                .build();
            var existingItems = List.copyOf(queryService.storage());

            var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
                useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate))
            );

            assertThat(exception.getMessage()).isEqualTo(
                InvalidPortalNavigationItemDataException.parentMustBePublic(api.getId().toString()).getMessage()
            );
            assertThat(queryService.storage()).containsExactlyElementsOf(existingItems);
            assertThat(pageContentCrudService.storage()).isEmpty();
        }

        @Test
        void should_reject_creation_below_api_owned_sourced_folder_before_writing() {
            var folder = PortalNavigationItemFixtures.aFolder("Sourced API documentation")
                .toBuilder()
                .reference(new NavigationItemReference.ApiReference("api-1"))
                .source(PortalNavigationItemSource.builder().sourceType("http-fetcher").sourceConfiguration("{}").build())
                .build();
            folder.markAsRoot();
            queryService.initWith(List.of(folder));

            var exception = assertThrows(InvalidPortalNavigationItemDataException.class, () ->
                useCase.execute(
                    new CreatePortalNavigationItemUseCase.Input(
                        ORG_ID,
                        ENV_ID,
                        documentation(PortalNavigationItemType.PAGE, folder.getId())
                    )
                )
            );

            assertThat(exception.getMessage()).isEqualTo(
                InvalidPortalNavigationItemDataException.cannotCreateBelowSourcedItem(folder.getId().json()).getMessage()
            );
            assertThat(queryService.storage()).containsExactly(folder);
            assertThat(pageContentCrudService.storage()).isEmpty();
        }

        @Test
        void should_not_apply_portal_sourced_ancestor_to_canonical_api_documentation_root() {
            var portalFolder = PortalNavigationItemFixtures.aFolder(APIS_ID, "APIs")
                .toBuilder()
                .source(PortalNavigationItemSource.builder().sourceType("http-fetcher").sourceConfiguration("{}").build())
                .build();
            portalFolder.markAsRoot();
            var api = PortalNavigationItemFixtures.anApi(API1_ID, "API", portalFolder.getId(), "api-1");
            api.updateParent(portalFolder);
            queryService.initWith(List.of(portalFolder, api));

            var output = useCase.execute(
                new CreatePortalNavigationItemUseCase.Input(
                    ORG_ID,
                    ENV_ID,
                    documentation(PortalNavigationItemType.PAGE, PortalNavigationItemId.of(API1_ID))
                )
            );

            assertThat(output.item().getReference()).isEqualTo(new NavigationItemReference.ApiReference("api-1"));
            assertThat(output.item().getParentId()).isNull();
        }
    }

    private static CreatePortalNavigationItem documentation(PortalNavigationItemType type, PortalNavigationItemId parentId) {
        return CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(type)
            .title("Documentation")
            .area(PortalArea.TOP_NAVBAR)
            .parentId(parentId)
            .order(0)
            .url(type == PortalNavigationItemType.LINK ? "https://gravitee.io" : null)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
    }

    @Nested
    class RootId {

        @Test
        void should_set_root_id_to_self_when_creating_root_item() {
            // Given — no parentId → root item
            var toCreate = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.FOLDER)
                .title("New Root Folder")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .build();

            // When
            var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate));

            // Then — rootId must equal the item's own id
            assertThat(output.item().getRootId()).isEqualTo(output.item().getId());
            assertThat(output.item().getParentId()).isNull();
        }

        @Test
        void should_set_parentId_and_rootId_correctly_when_creating_child_under_non_root_parent() {
            // Given — APIS (root) → CATEGORY1 (child, rootId = APIS_ID) in sampleNavigationItems
            // Create a new item under CATEGORY1
            var toCreate = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.FOLDER)
                .title("New Sub Folder")
                .area(PortalArea.TOP_NAVBAR)
                .parentId(PortalNavigationItemId.of(CATEGORY1_ID))
                .order(0)
                .build();

            // When
            var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate));

            // Then — parentId = CATEGORY1_ID, rootId = APIS_ID (inherited from parent's rootId)
            assertThat(output.item().getParentId()).isEqualTo(PortalNavigationItemId.of(CATEGORY1_ID));
            assertThat(output.item().getRootId()).isEqualTo(PortalNavigationItemId.of(APIS_ID));
        }

        @Test
        void should_create_homepage_item_as_root_element_with_root_id_equal_to_self() {
            // HOMEPAGE items are always root elements — the validator rejects a second one,
            // but each individual creation must produce rootId = self.id
            var toCreate = CreatePortalNavigationItem.builder()
                .id(PortalNavigationItemId.random())
                .type(PortalNavigationItemType.FOLDER)
                .title("Homepage")
                .area(PortalArea.HOMEPAGE)
                .order(0)
                .build(); // no parentId — homepages are always root

            // When
            var output = useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, toCreate));

            // Then — item is a root element
            assertThat(output.item().getParentId()).isNull();
            assertThat(output.item().getRootId()).isEqualTo(output.item().getId());
        }
    }

    @Nested
    class UpdateSiblingsOrder {

        @ParameterizedTest(name = "Order = {1} ({0})")
        @CsvSource(
            {
                "New item at the beginning -> update all siblings, 0",
                "New item in the middle -> update some siblings, 1",
                "New item at the end -> update no siblings, 2",
                "New item at the end -> update no siblings and limit order to max possible, 99999999",
            }
        )
        void should_update_order_of_siblings_top_level(String label, Integer order) {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .id(PortalNavigationItemId.random())
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(order)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .build();

            final var existingSiblingOrdersById = queryService
                .findTopLevelItemsByEnvironmentIdAndPortalArea(ENV_ID, PortalArea.TOP_NAVBAR)
                .stream()
                .collect(Collectors.toMap(PortalNavigationItem::getId, PortalNavigationItem::getOrder));

            // When
            useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

            // Then
            final var items = queryService.findTopLevelItemsByEnvironmentIdAndPortalArea(ENV_ID, PortalArea.TOP_NAVBAR);

            final var createdItem = items
                .stream()
                .filter(item -> item.getId().equals(createPortalNavigationItem.getId()))
                .findFirst()
                .orElse(null);
            assertThat(createdItem).isNotNull();
            final var targetOrder = Math.min(order, items.size() - 1);
            assertThat(createdItem.getOrder()).isEqualTo(targetOrder);

            final var updatedSiblings = items.stream().filter(item -> existingSiblingOrdersById.keySet().contains(item.getId()));
            assertThat(updatedSiblings).allSatisfy(item -> {
                final var oldSiblingOrder = existingSiblingOrdersById.get(item.getId());
                final var updatedSiblingOrder = item.getOrder();
                if (existingSiblingOrdersById.get(item.getId()) < order) {
                    assertThat(updatedSiblingOrder).isEqualTo(oldSiblingOrder);
                } else {
                    assertThat(updatedSiblingOrder).isEqualTo(oldSiblingOrder + 1);
                }
            });
        }

        @ParameterizedTest(name = "Order = {1} ({0})")
        @CsvSource(
            {
                "New item at the beginning -> update all siblings, 0",
                "New item in the middle -> update some siblings, 1",
                "New item at the end -> update no siblings, 2",
                "New item at the end -> update no siblings and limit order to max possible, 99999999",
            }
        )
        void should_update_order_of_siblings_mid_level(String label, Integer order) {
            // Given
            final var createPortalNavigationItem = CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.FOLDER)
                .id(PortalNavigationItemId.random())
                .title("title")
                .area(PortalArea.TOP_NAVBAR)
                .order(order)
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .build();
            createPortalNavigationItem.setParentId(PortalNavigationItemId.of(APIS_ID));

            final var existingSiblingOrdersById = queryService
                .findByParentIdAndEnvironmentId(ENV_ID, createPortalNavigationItem.getParentId())
                .stream()
                .collect(Collectors.toMap(PortalNavigationItem::getId, PortalNavigationItem::getOrder));

            // When
            useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

            // Then
            final var items = queryService.findByParentIdAndEnvironmentId(ENV_ID, createPortalNavigationItem.getParentId());

            final var createdItem = items
                .stream()
                .filter(item -> item.getId().equals(createPortalNavigationItem.getId()))
                .findFirst()
                .orElse(null);
            assertThat(createdItem).isNotNull();
            final var targetOrder = Math.min(order, items.size() - 1);
            assertThat(createdItem.getOrder()).isEqualTo(targetOrder);

            final var updatedSiblings = items.stream().filter(item -> existingSiblingOrdersById.keySet().contains(item.getId()));
            assertThat(updatedSiblings).allSatisfy(item -> {
                final var oldSiblingOrder = existingSiblingOrdersById.get(item.getId());
                final var updatedSiblingOrder = item.getOrder();
                if (existingSiblingOrdersById.get(item.getId()) < order) {
                    assertThat(updatedSiblingOrder).isEqualTo(oldSiblingOrder);
                } else {
                    assertThat(updatedSiblingOrder).isEqualTo(oldSiblingOrder + 1);
                }
            });
        }
    }
}
