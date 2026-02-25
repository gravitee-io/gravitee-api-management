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
import static fixtures.core.model.PortalNavigationItemFixtures.CATEGORY1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePortalNavigationItemUseCaseTest {

    private CreatePortalNavigationItemUseCase useCase;
    private PortalNavigationItemDomainService domainService;
    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();

        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        PortalPageContentQueryServiceInMemory pageContentQueryService = new PortalPageContentQueryServiceInMemory(
            pageContentCrudService.storage()
        );
        PortalNavigationItemValidatorService validatorService = new PortalNavigationItemValidatorService(
            queryService,
            pageContentQueryService
        );
        domainService = new PortalNavigationItemDomainService(crudService, queryService, pageContentCrudService, apiCrudService);
        useCase = new CreatePortalNavigationItemUseCase(domainService, validatorService);
        queryService.initWith(PortalNavigationItemFixtures.sampleNavigationItems());
        apiCrudService.initWith(List.of(Api.builder().id("apiId").name("apiIdName").build()));
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

        final ThrowingRunnable throwing = () ->
            useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

        // Then
        Exception exception = assertThrows(ApiNotFoundException.class, throwing);
        assertThat(exception.getMessage()).isEqualTo("Api not found.");
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
        final ThrowingRunnable throwing = () ->
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
        void should_create_page_with_parent_id_set_to_portal_navigation_item_type_api() {
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
            assertThat(output.item().getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
        }

        @Test
        void should_create_link_with_parent_id_set_to_portal_navigation_item_type_api() {
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
            assertThat(output.item().getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
        }

        @Test
        void should_create_folder_with_parent_id_set_to_portal_navigation_item_type_api() {
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
            assertThat(output.item().getParentId()).isEqualTo(PortalNavigationItemId.of(API1_ID));
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
            final ThrowingRunnable throwing = () ->
                useCase.execute(new CreatePortalNavigationItemUseCase.Input(ORG_ID, ENV_ID, createPortalNavigationItem));

            // Then
            Exception exception = assertThrows(InvalidPortalNavigationItemDataException.class, throwing);
            assertThat(exception.getMessage()).isEqualTo("Parent hierarchy cannot include API items.");
        }
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
